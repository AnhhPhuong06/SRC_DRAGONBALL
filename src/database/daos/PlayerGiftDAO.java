package database.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import database.AlyraManager;
import utils.Logger;


public final class PlayerGiftDAO {

    public static final String KIND_CHECKIN_MILESTONE = "checkin";
    /** key = the entry's {@code threshold} (a character level). Sub 4 cmd 2. */
    public static final String KIND_LEVEL_GIFT = "level";
    /** key = the entry's {@code threshold} (online minutes). Sub 5 cmd 2. */
    public static final String KIND_ONLINE_GIFT = "online";
    /** key = the normalised code text (trimmed, upper-cased). Sub 6 cmd 1. */
    public static final String KIND_GIFTCODE = "giftcode";
    /** key = the pack's {@code packageLevel}. Sub 7 cmd 1. */
    public static final String KIND_DAILY_PACK = "dailypack";
    /** key = the pack's {@code vipLevel}. Sub 7 cmd 3. */
    public static final String KIND_VIP_PACK = "vippack";
    /** key = {@code "group:id"} (see {@code GiftDataDAO.Achievement.key()}). Sub 10 cmd 1. */
    public static final String KIND_ACHIEVEMENT = "achievement";
    /** key = the quest's {@code id}. Sub 11 cmd 1. */
    public static final String KIND_QUEST = "quest";
    /** key = the milestone's {@code amount} in VND. Sub 13 cmd 1. */
    public static final String KIND_CHARGE = "charge";
    public static final String KIND_MAIL_SENT = "mailsent";

    public static final int MAIL_READ = 1;
    public static final int MAIL_CLAIMED = 2;

    public static final int MAIL_DELETED = 4;

    /** Bits that may go on the wire. Anything above this is server bookkeeping. */
    private static final int MAIL_WIRE_MASK = MAIL_READ | MAIL_CLAIMED;

    private static final ConcurrentHashMap<Long, GiftState> CACHE = new ConcurrentHashMap<>();

    static {
        ensureTables();
    }

    private PlayerGiftDAO() {
    }

    public static final class GiftState {

        private long playerId;

        // ---- check-in (sub 3)
        private int checkInDays;
        private long lastCheckInTime;
        private long lastDailyPackReset;

        private long onlineSeconds;             
        private long onlineDayBaseSeconds;     
        private long lastOnlineReset; 

        // ---- achievements (sub 10)
        private boolean privilegeUnlocked;

        // ---- charge promo (sub 13)
        private long totalCharged;

        // ---- quest (sub 11): which quest of the chain this player is on
        private int questId = -1;
        private int questStep;
        private boolean questAllCompleted;

        // ---- generic stores
        /** namespace -> set of claimed keys. */
        private final Map<String, Set<String>> claims = new HashMap<>();
        /** "namespace:key" -> counter (achievement progress, quest task counts). */
        private final Map<String, Long> counters = new HashMap<>();
        /** "mailKind:mailId" -> flag bits. */
        private final Map<String, Integer> mailFlags = new HashMap<>();

        private boolean dirty;

        GiftState(long playerId) {
            this.playerId = playerId;
        }

        public long getPlayerId() {
            return playerId;
        }


        public synchronized boolean isClaimed(String kind, String key) {
            Set<String> set = claims.get(kind);
            return set != null && set.contains(normalize(key));
        }

        public boolean isClaimed(String kind, long key) {
            return isClaimed(kind, Long.toString(key));
        }

        public synchronized boolean markClaimed(String kind, String key) {
            Set<String> set = claims.get(kind);
            if (set == null) {
                set = new LinkedHashSet<>();
                claims.put(kind, set);
            }
            if (set.add(normalize(key))) {
                dirty = true;
                return true;
            }
            return false;
        }

        public boolean markClaimed(String kind, long key) {
            return markClaimed(kind, Long.toString(key));
        }

        /** Undo a claim — for admin resets and for rolling back a grant that failed halfway. */
        public synchronized boolean unmarkClaimed(String kind, String key) {
            Set<String> set = claims.get(kind);
            if (set != null && set.remove(normalize(key))) {
                dirty = true;
                return true;
            }
            return false;
        }

        public boolean unmarkClaimed(String kind, long key) {
            return unmarkClaimed(kind, Long.toString(key));
        }

        /** Snapshot copy of one namespace; safe to iterate without holding the lock. */
        public synchronized Set<String> claimedKeys(String kind) {
            Set<String> set = claims.get(kind);
            return set == null ? new LinkedHashSet<String>() : new LinkedHashSet<>(set);
        }

        /** Drops a whole namespace (e.g. a daily reset of the pack purchases). */
        public synchronized boolean clearClaims(String kind) {
            Set<String> removed = claims.remove(kind);
            if (removed != null && !removed.isEmpty()) {
                dirty = true;
                return true;
            }
            return false;
        }

        /** Làm mới lượt mua Gói Ngày lúc 00:00: xoá record "đã mua" nếu đã qua ngày mới. */
        public synchronized void rolloverDailyPacksIfNewDay() {
            long now = System.currentTimeMillis();
            if (!isSameDay(lastDailyPackReset, now)) {
                clearClaims(KIND_DAILY_PACK);
                lastDailyPackReset = now;
                dirty = true;
            }
        }

        public int statusOf(String kind, long key, boolean reached) {
            if (isClaimed(kind, key)) {
                return 2;
            }
            return reached ? 1 : 0;
        }

        public int statusOf(String kind, String key, boolean reached) {
            if (isClaimed(kind, key)) {
                return 2;
            }
            return reached ? 1 : 0;
        }

        // ---------------------------------------------------------------- counters

        /** Progress counter, e.g. {@code getCounter(KIND_ACHIEVEMENT, "0:1")}. */
        public synchronized long getCounter(String kind, String key) {
            Long v = counters.get(counterKey(kind, key));
            return v == null ? 0L : v;
        }

        public long getCounter(String kind, long key) {
            return getCounter(kind, Long.toString(key));
        }

        /** @return true when the stored value actually changed. */
        public synchronized boolean setCounter(String kind, String key, long value) {
            String k = counterKey(kind, key);
            Long previous = counters.get(k);
            if (previous != null && previous == value) {
                return false;
            }
            counters.put(k, value);
            dirty = true;
            return true;
        }

        public boolean setCounter(String kind, long key, long value) {
            return setCounter(kind, Long.toString(key), value);
        }

        /** Adds {@code delta} and returns the new total. */
        public synchronized long addCounter(String kind, String key, long delta) {
            String k = counterKey(kind, key);
            Long previous = counters.get(k);
            long value = (previous == null ? 0L : previous) + delta;
            counters.put(k, value);
            dirty = true;
            return value;
        }

        public long addCounter(String kind, long key, long delta) {
            return addCounter(kind, Long.toString(key), delta);
        }

        public synchronized boolean clearCounters(String kind) {
            String prefix = kind + ":";
            boolean changed = counters.keySet().removeIf(k -> k.startsWith(prefix));
            if (changed) {
                dirty = true;
            }
            return changed;
        }

        private static String counterKey(String kind, String key) {
            return kind + ":" + normalize(key);
        }

        // ---------------------------------------------------------------- mail flags

        /** Raw stored flags including {@link #MAIL_DELETED}. */
        public synchronized int getMailFlags(int mailKind, int mailId) {
            Integer v = mailFlags.get(mailKey(mailKind, mailId));
            return v == null ? 0 : v;
        }

        /** ORs {@code bits} into this mail's flags. @return true when something changed. */
        public synchronized boolean setMailFlag(int mailKind, int mailId, int bits) {
            String k = mailKey(mailKind, mailId);
            Integer previous = mailFlags.get(k);
            int before = previous == null ? 0 : previous;
            int after = before | bits;
            if (after == before) {
                return false;
            }
            mailFlags.put(k, after);
            dirty = true;
            return true;
        }

        /** Clears {@code bits} from this mail's flags. */
        public synchronized boolean clearMailFlag(int mailKind, int mailId, int bits) {
            String k = mailKey(mailKind, mailId);
            Integer previous = mailFlags.get(k);
            if (previous == null) {
                return false;
            }
            int after = previous & ~bits;
            if (after == previous) {
                return false;
            }
            if (after == 0) {
                mailFlags.remove(k);
            } else {
                mailFlags.put(k, after);
            }
            dirty = true;
            return true;
        }

        public boolean isMailRead(int mailKind, int mailId) {
            return (getMailFlags(mailKind, mailId) & MAIL_READ) != 0;
        }

        public boolean isMailClaimed(int mailKind, int mailId) {
            return (getMailFlags(mailKind, mailId) & MAIL_CLAIMED) != 0;
        }

        public boolean isMailDeleted(int mailKind, int mailId) {
            return (getMailFlags(mailKind, mailId) & MAIL_DELETED) != 0;
        }

        /** Forgets a mail entirely — use after the row itself is gone from {@code gift_mail}. */
        public synchronized boolean forgetMail(int mailKind, int mailId) {
            if (mailFlags.remove(mailKey(mailKind, mailId)) != null) {
                dirty = true;
                return true;
            }
            return false;
        }

        private static String mailKey(int mailKind, int mailId) {
            return mailKind + ":" + mailId;
        }

        public synchronized int getCheckInDays() {
            return checkInDays;
        }

        public synchronized long getLastCheckInTime() {
            return lastCheckInTime;
        }

        /** True when {@link #lastCheckInTime} falls on today's calendar date (server local time). */
        public synchronized boolean isCheckedInToday() {
            return isSameDay(lastCheckInTime, System.currentTimeMillis());
        }

        public synchronized boolean markCheckedInToday() {
            long now = System.currentTimeMillis();
            if (isSameDay(lastCheckInTime, now)) {
                return false;
            }
            lastCheckInTime = now;
            checkInDays++;
            dirty = true;
            return true;
        }

        /** Admin/debug override. */
        public synchronized void setCheckIn(int days, long lastTime) {
            this.checkInDays = days;
            this.lastCheckInTime = lastTime;
            this.dirty = true;
        }

        public synchronized long getOnlineSeconds() {
            return onlineSeconds;
        }

        /** Accumulates play time; the sub 5 state packet's {@code current} is derived from this. */
        public synchronized long addOnlineSeconds(long seconds) {
            if (seconds <= 0) {
                return onlineSeconds;
            }
            onlineSeconds += seconds;
            dirty = true;
            return onlineSeconds;
        }

        public synchronized void setOnlineSeconds(long seconds) {
            this.onlineSeconds = Math.max(0L, seconds);
            this.dirty = true;
        }

        public long getOnlineMinutes() {
            return getOnlineSeconds() / 60L;
        }

        public synchronized long getDailyOnlineMinutes() {
            return Math.max(0L, onlineSeconds - onlineDayBaseSeconds) / 60L;
        }

        public synchronized void rolloverOnlineGiftsIfNewDay() {
            long now = System.currentTimeMillis();
            if (!isSameDay(lastOnlineReset, now)) {
                clearClaims(KIND_ONLINE_GIFT);
                onlineDayBaseSeconds = onlineSeconds;
                lastOnlineReset = now;
                dirty = true;
            }
        }

        public synchronized boolean isPrivilegeUnlocked() {
            return privilegeUnlocked;
        }

        /** @return true when this call actually flipped it (i.e. it was not already unlocked). */
        public synchronized boolean setPrivilegeUnlocked(boolean unlocked) {
            if (privilegeUnlocked == unlocked) {
                return false;
            }
            privilegeUnlocked = unlocked;
            dirty = true;
            return true;
        }

        public synchronized long getTotalCharged() {
            return totalCharged;
        }

        public synchronized void setTotalCharged(long amount) {
            this.totalCharged = Math.max(0L, amount);
            this.dirty = true;
        }

        public synchronized int getQuestId() {
            return questId;
        }

        public synchronized int getQuestStep() {
            return questStep;
        }

        public synchronized boolean isQuestAllCompleted() {
            return questAllCompleted;
        }

        /** Moves the player onto a quest. Pass {@code allCompleted} when the chain is exhausted. */
        public synchronized void setQuest(int questId, int questStep, boolean allCompleted) {
            this.questId = questId;
            this.questStep = questStep;
            this.questAllCompleted = allCompleted;
            this.dirty = true;
        }

        // ---------------------------------------------------------------- persistence plumbing

        public synchronized boolean isDirty() {
            return dirty;
        }

        synchronized void setDirty(boolean value) {
            this.dirty = value;
        }

        /** Serialises to the canonical blob. Callers hold the monitor via {@link #snapshotJson()}. */
        @SuppressWarnings("unchecked")
        private JSONObject toJson() {
            JSONObject root = new JSONObject();
            root.put("checkInDays", (long) checkInDays);
            root.put("lastCheckIn", lastCheckInTime);
            root.put("lastDailyPackReset", lastDailyPackReset);
            root.put("onlineSeconds", onlineSeconds);
            root.put("onlineDayBase", onlineDayBaseSeconds);
            root.put("lastOnlineReset", lastOnlineReset);
            root.put("privilegeUnlocked", privilegeUnlocked);
            root.put("totalCharged", totalCharged);
            root.put("questId", (long) questId);
            root.put("questStep", (long) questStep);
            root.put("questAllCompleted", questAllCompleted);

            JSONObject claimsObj = new JSONObject();
            for (Map.Entry<String, Set<String>> e : claims.entrySet()) {
                if (e.getValue() == null || e.getValue().isEmpty()) {
                    continue;
                }
                JSONArray arr = new JSONArray();
                arr.addAll(e.getValue());
                claimsObj.put(e.getKey(), arr);
            }
            root.put("claims", claimsObj);

            JSONObject countersObj = new JSONObject();
            for (Map.Entry<String, Long> e : counters.entrySet()) {
                countersObj.put(e.getKey(), e.getValue());
            }
            root.put("counters", countersObj);

            JSONObject mailObj = new JSONObject();
            for (Map.Entry<String, Integer> e : mailFlags.entrySet()) {
                mailObj.put(e.getKey(), (long) e.getValue());
            }
            root.put("mail", mailObj);

            return root;
        }

        /** JSON text of the current state; taken under the state lock so it is always consistent. */
        public synchronized String snapshotJson() {
            return toJson().toJSONString();
        }

        private synchronized void loadFrom(JSONObject root) {
            checkInDays = (int) GiftDataDAO.asLong(root.get("checkInDays"), 0L);
            lastCheckInTime = GiftDataDAO.asLong(root.get("lastCheckIn"), 0L);
            lastDailyPackReset = GiftDataDAO.asLong(root.get("lastDailyPackReset"), 0L);
            onlineSeconds = GiftDataDAO.asLong(root.get("onlineSeconds"), 0L);
            onlineDayBaseSeconds = GiftDataDAO.asLong(root.get("onlineDayBase"), 0L);
            lastOnlineReset = GiftDataDAO.asLong(root.get("lastOnlineReset"), 0L);
            privilegeUnlocked = asBool(root.get("privilegeUnlocked"));
            totalCharged = GiftDataDAO.asLong(root.get("totalCharged"), 0L);
            questId = (int) GiftDataDAO.asLong(root.get("questId"), -1L);
            questStep = (int) GiftDataDAO.asLong(root.get("questStep"), 0L);
            questAllCompleted = asBool(root.get("questAllCompleted"));

            claims.clear();
            Object claimsNode = root.get("claims");
            if (claimsNode instanceof JSONObject) {
                JSONObject claimsObj = (JSONObject) claimsNode;
                for (Object keyObj : claimsObj.keySet()) {
                    String kind = String.valueOf(keyObj);
                    Object listNode = claimsObj.get(keyObj);
                    if (!(listNode instanceof JSONArray)) {
                        continue;
                    }
                    Set<String> set = new LinkedHashSet<>();
                    for (Object v : (JSONArray) listNode) {
                        if (v != null) {
                            set.add(normalize(String.valueOf(v)));
                        }
                    }
                    if (!set.isEmpty()) {
                        claims.put(kind, set);
                    }
                }
            }

            counters.clear();
            Object countersNode = root.get("counters");
            if (countersNode instanceof JSONObject) {
                JSONObject countersObj = (JSONObject) countersNode;
                for (Object keyObj : countersObj.keySet()) {
                    counters.put(String.valueOf(keyObj), GiftDataDAO.asLong(countersObj.get(keyObj), 0L));
                }
            }

            mailFlags.clear();
            Object mailNode = root.get("mail");
            if (mailNode instanceof JSONObject) {
                JSONObject mailObj = (JSONObject) mailNode;
                for (Object keyObj : mailObj.keySet()) {
                    int bits = (int) GiftDataDAO.asLong(mailObj.get(keyObj), 0L);
                    if (bits != 0) {
                        mailFlags.put(String.valueOf(keyObj), bits);
                    }
                }
            }

            dirty = false;
        }

        private static boolean asBool(Object o) {
            if (o instanceof Boolean) {
                return (Boolean) o;
            }
            if (o instanceof Number) {
                return ((Number) o).longValue() != 0L;
            }
            return o != null && "true".equalsIgnoreCase(String.valueOf(o).trim());
        }

        /** Keys are compared case-insensitively so giftcode text round-trips reliably. */
        private static String normalize(String key) {
            return key == null ? "" : key.trim().toUpperCase(Locale.ROOT);
        }
    }

   public static void ensureTables() {
        try (Connection con = AlyraManager.getConnection();
                Statement st = con.createStatement()) {
            st.executeUpdate("CREATE TABLE IF NOT EXISTS player_gift_data ("
                    + "player_id BIGINT NOT NULL PRIMARY KEY,"
                    + "data_gift MEDIUMTEXT NOT NULL,"
                    + "update_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP "
                    + "ON UPDATE CURRENT_TIMESTAMP"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        } catch (Exception e) {
            Logger.logException(PlayerGiftDAO.class, e, "Lỗi tạo bảng player_gift_data");
        }
    }

    public static GiftState load(long playerId) {
        GiftState cached = CACHE.get(playerId);
        if (cached != null) {
            return cached;
        }
        GiftState loaded = readFromDb(playerId);
        GiftState raced = CACHE.putIfAbsent(playerId, loaded);
        return raced != null ? raced : loaded;
    }

    /** The cached state without touching the DB, or null when this player is not loaded. */
    public static GiftState peek(long playerId) {
        return CACHE.get(playerId);
    }

    private static GiftState readFromDb(long playerId) {
        GiftState state = new GiftState(playerId);
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT data_gift FROM player_gift_data WHERE player_id = ?")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String raw = rs.getString("data_gift");
                    if (raw != null && !raw.trim().isEmpty()) {
                        Object root = JSONValue.parse(raw);
                        if (root instanceof JSONObject) {
                            state.loadFrom((JSONObject) root);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(PlayerGiftDAO.class, e, "Lỗi load player_gift_data id=" + playerId);
        }
        return state;
    }

    /** Persists the cached state for this player. No-op when nothing is cached. */
    public static boolean save(long playerId) {
        return save(playerId, CACHE.get(playerId));
    }

    public static boolean save(long playerId, GiftState state) {
        if (state == null) {
            return false;
        }
        String json;
        synchronized (state) {
            json = state.snapshotJson();
            state.setDirty(false);
        }
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO player_gift_data (player_id, data_gift) VALUES (?, ?) "
                        + "ON DUPLICATE KEY UPDATE data_gift = VALUES(data_gift)")) {
            ps.setLong(1, playerId);
            ps.setString(2, json);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            state.setDirty(true); // retry on the next flush instead of dropping the claim
            Logger.logException(PlayerGiftDAO.class, e, "Lỗi lưu player_gift_data id=" + playerId);
            return false;
        }
    }

    /** Saves only when something changed — the normal call at the end of a claim handler. */
    public static boolean saveIfDirty(long playerId) {
        GiftState state = CACHE.get(playerId);
        if (state == null || !state.isDirty()) {
            return false;
        }
        return save(playerId, state);
    }

    /** Saves and drops the cache entry. Call on logout so an offline player holds no memory. */
    public static void unload(long playerId) {
        GiftState state = CACHE.remove(playerId);
        if (state != null && state.isDirty()) {
            save(playerId, state);
        }
    }

    public static int flushDirty() {
        int saved = 0;
        List<Map.Entry<Long, GiftState>> entries = new ArrayList<>(CACHE.entrySet());
        for (Map.Entry<Long, GiftState> entry : entries) {
            GiftState state = entry.getValue();
            if (state != null && state.isDirty() && save(entry.getKey(), state)) {
                saved++;
            }
        }
        return saved;
    }

    /** Number of players currently held in memory — for the admin panel. */
    public static int cachedCount() {
        return CACHE.size();
    }

    public static int wireFlags(int storedFlags) {
        return storedFlags & MAIL_WIRE_MASK;
    }

    /** True when both epoch-millis timestamps fall on the same server-local calendar day. */
    public static boolean isSameDay(long a, long b) {
        if (a <= 0 || b <= 0) {
            return false;
        }
        Calendar ca = Calendar.getInstance();
        ca.setTimeInMillis(a);
        Calendar cb = Calendar.getInstance();
        cb.setTimeInMillis(b);
        return ca.get(Calendar.YEAR) == cb.get(Calendar.YEAR)
                && ca.get(Calendar.DAY_OF_YEAR) == cb.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean reset(long playerId) {
        CACHE.remove(playerId);
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM player_gift_data WHERE player_id = ?")) {
            ps.setLong(1, playerId);
            ps.executeUpdate();
            return true;
        } catch (Exception e) {
            Logger.logException(PlayerGiftDAO.class, e, "Lỗi reset player_gift_data id=" + playerId);
            return false;
        }
    }
}
