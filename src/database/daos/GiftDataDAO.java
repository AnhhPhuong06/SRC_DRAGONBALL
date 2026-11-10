package database.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import database.AlyraManager;
import utils.Logger;


public final class GiftDataDAO {

    /** Reward list JSON column value used when a row leaves its rewards blank. */
    private static final String EMPTY_JSON = "[]";

    private static final long DAY_MILLIS = 86400000L;

    /** Sanity ceiling for {@code checkin.pool_roll_count} — the wire count is one unsigned byte. */
    private static final int MAX_POOL_ROLLS = 255;

    // ---- gift_config keys. The seed uses DOTS; lookups also accept the underscore spelling. -----
    public static final String CFG_ACHIEVEMENT_UNLOCK_PRICE = "achievement.unlock_price";
    public static final String CFG_CHARGE_PROMO_TARGET = "charge_promo.target";
    public static final String CFG_CHARGE_PROMO_ACTIVE = "charge_promo.active";
    public static final String CFG_CHECKIN_POOL_ROLL_COUNT = "checkin.pool_roll_count";
    /** {@code gift_mail_template} id delivered once on a player's first top-up; 0 = feature off. */
    public static final String CFG_MAIL_FIRST_CHARGE_TEMPLATE = "mail.first_charge_template";

    /** Accepted {@code expireAt} spellings; the seed uses the first. */
    private static final String[] TIMESTAMP_PATTERNS = {
        "yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd'T'HH:mm:ss", "yyyy-MM-dd HH:mm", "yyyy-MM-dd"
    };

    private static final Object RELOAD_LOCK = new Object();

    private static volatile Snapshot cache;

    private static final ConcurrentHashMap<Integer, Long> BROADCAST_FIRST_SEEN =
            new ConcurrentHashMap<>();

    static {
        ensureTables();
    }

    private GiftDataDAO() {
    }

    public static final class Reward {

        public int itemId;
        public int amount = 1;
        /** Each entry is {optionId, optionParam}. Never null; empty means "no option block". */
        public List<int[]> options = new ArrayList<>();

        public Reward() {
        }

        public Reward(int itemId, int amount) {
            this.itemId = itemId;
            this.amount = amount;
        }

        /** Convenience for the {@code rewardFull(Message, int, int, int[][])} writer signature. */
        public int[][] optionArray() {
            if (options == null || options.isEmpty()) {
                return null;
            }
            return options.toArray(new int[0][]);
        }
    }

    /** A weighted entry of the check-in random pool ({@code gift_checkin.kind = 'POOL'}). */
    public static final class PoolEntry {

        public Reward reward;
        public int weight = 1;
    }

    /** Accumulated check-in milestone ({@code gift_checkin.kind = 'MILESTONE'}; rewards go out FLAT). */
    public static final class CheckInMilestone {

        public int dayNumber;
        public List<Reward> rewards = new ArrayList<>();
    }

    /** One level-gift (sub 4) or online-gift (sub 5) entry; {@code giftType} is the sub-type. */
    public static final class MilestoneEntry {

        public int giftType;
        public long threshold;
        public List<Reward> rewards = new ArrayList<>();
    }

    /** A giftcode row (sub 6). {@code usedCount}/{@code stock} are server-wide, not per player. */
    public static final class GiftCode {

        public long id;
        public String code = "";
        /** 1 = once per player, 2 = once server-wide, 3 = restricted to {@link #targetPlayers}. */
        public int scope = 1;
        /** Epoch millis, resolved from the {@code expireAt} string; {@code <= 0} means never. */
        public long expireTime;
        /** Total redemptions allowed server-wide; {@code < 0} means unlimited. */
        public int stock = -1;
        public int usedCount;
        /** Lower-cased player names allowed to redeem when {@code scope == 3}. */
        public Set<String> targetPlayers = new HashSet<>();
        public List<Reward> rewards = new ArrayList<>();

        public boolean isExpired(long now) {
            return expireTime > 0 && now >= expireTime;
        }

        /** Countdown the client expects (sub 6 kind 0), NOT an absolute time. Negative = permanent. */
        public long remainSeconds(long now) {
            if (expireTime <= 0) {
                return -1L;
            }
            long remain = (expireTime - now) / 1000L;
            return remain > 0 ? remain : 0L;
        }

        public boolean hasStock() {
            return stock < 0 || usedCount < stock;
        }

        public boolean isTargeted(String playerName) {
            if (scope != 3) {
                return true;
            }
            return playerName != null && targetPlayers.contains(playerName.toLowerCase(Locale.ROOT));
        }
    }

    /** Daily pack (sub 7 kind 0). {@code packageLevel} is the id the client sends back on buy. */
    public static final class DailyPack {

        public int packageLevel;
        public String label = "";
        public int vndCost;
        public List<Reward> rewards = new ArrayList<>();
    }

    /** VIP pack (sub 7 kind 1). {@code vipLevel} is the id the client sends back on buy. */
    public static final class VipPack {

        public int vipLevel;
        public String label = "";
        public String description = "";
        public int vndCost;
        public int requiredVipLevel;
        public List<Reward> rewards = new ArrayList<>();
    }

    public static final class Mail {

        public int id;
        public int mailKind;
        /** Recipient for {@code mailKind == 0}; {@code <= 0} for broadcasts. */
        public long playerId;
        public String sender = "";
        public String title = "";
        public String body = "";
        public long sentTime;
        /** Epoch millis; {@code <= 0} means the mail never expires. */
        public long expireTime;
        /** Lifetime in days RELATIVE to delivery, as seeded. {@code <= 0} means never expires. */
        public int expireDays;
        public List<Reward> rewards = new ArrayList<>();

        public boolean isExpired(long now) {
            return expireTime > 0 && now >= expireTime;
        }
    }

    public static final class Achievement {

        public int id;
        public int group;
        public String name = "";
        public String description = "";
        public int target;
        /** Tracked-stat selector, e.g. {@code REACH_LEVEL}, {@code KILL_MOB}. May be empty. */
        public String type = "";
        /** Extra qualifier for {@link #type} (map id, tier, ...); 0 when unused. */
        public int param;
        public List<Reward> rewards = new ArrayList<>();

        /** Composite key used for {@link PlayerGiftDAO} claim/counter lookups: {@code "group:id"}. */
        public String key() {
            return group + ":" + id;
        }
    }

    /** One task line of a quest (sub 11 kind 0). */
    public static final class QuestTask {

        public int index;
        public String text = "";
        public int target;
        /** Tracked-stat selector, same vocabulary as {@link Achievement#type}. May be empty. */
        public String type = "";
        /** Extra qualifier for {@link #type}; 0 when unused. */
        public int param;
    }

    public static final class Quest {

        public int id;
        public String title = "";
        public int step;
        public List<QuestTask> tasks = new ArrayList<>();
        public List<Reward> rewards = new ArrayList<>();
    }

    public static final class ChargeMilestone {

        public long amount;
        public String iconPath = "";
        public List<Reward> rewards = new ArrayList<>();
    }

    public static final class Snapshot {

        public final List<CheckInMilestone> checkInMilestones;
        public final List<PoolEntry> checkInPool;
        public final List<MilestoneEntry> levelGifts;
        public final List<MilestoneEntry> onlineGifts;
        public final List<GiftCode> giftCodes;
        public final List<DailyPack> dailyPacks;
        public final List<VipPack> vipPacks;
        /** Every {@code gift_mail_template} row, both kinds — the catalogue admins send from. */
        public final List<Mail> mailTemplates;
        /** Broadcast mail only ({@code mailKind == 1}); personal mail is loaded per player. */
        public final List<Mail> broadcastMails;
        public final List<Achievement> basicAchievements;
        public final List<Achievement> privilegeAchievements;
        public final List<Quest> quests;
        public final List<ChargeMilestone> chargeMilestones;
        public final Map<String, String> config;

        private final Map<Integer, CheckInMilestone> checkInByDay = new HashMap<>();
        private final Map<Long, MilestoneEntry> levelByThreshold = new HashMap<>();
        private final Map<Long, MilestoneEntry> onlineByThreshold = new HashMap<>();
        private final Map<String, GiftCode> codeByText = new HashMap<>();
        private final Map<Integer, DailyPack> dailyByLevel = new HashMap<>();
        private final Map<Integer, VipPack> vipByLevel = new HashMap<>();
        private final Map<Integer, Mail> templateById = new HashMap<>();
        private final Map<Integer, Mail> broadcastById = new HashMap<>();
        private final Map<String, Achievement> achievementByKey = new HashMap<>();
        private final Map<Integer, Quest> questStepIndex = new HashMap<>();
        private final Map<Integer, Quest> questIdIndex = new HashMap<>();
        private final Map<Long, ChargeMilestone> chargeByAmount = new HashMap<>();

        private final int totalPoolWeight;

        Snapshot(List<CheckInMilestone> checkInMilestones,
                List<PoolEntry> checkInPool,
                List<MilestoneEntry> levelGifts,
                List<MilestoneEntry> onlineGifts,
                List<GiftCode> giftCodes,
                List<DailyPack> dailyPacks,
                List<VipPack> vipPacks,
                List<Mail> mailTemplates,
                List<Achievement> basicAchievements,
                List<Achievement> privilegeAchievements,
                List<Quest> quests,
                List<ChargeMilestone> chargeMilestones,
                Map<String, String> config) {
            List<Mail> broadcasts = new ArrayList<>();
            for (Mail mail : mailTemplates) {
                if (mail != null && mail.mailKind == 1) {
                    broadcasts.add(mail);
                }
            }

            this.checkInMilestones = Collections.unmodifiableList(checkInMilestones);
            this.checkInPool = Collections.unmodifiableList(checkInPool);
            this.levelGifts = Collections.unmodifiableList(levelGifts);
            this.onlineGifts = Collections.unmodifiableList(onlineGifts);
            this.giftCodes = Collections.unmodifiableList(giftCodes);
            this.dailyPacks = Collections.unmodifiableList(dailyPacks);
            this.vipPacks = Collections.unmodifiableList(vipPacks);
            this.mailTemplates = Collections.unmodifiableList(mailTemplates);
            this.broadcastMails = Collections.unmodifiableList(broadcasts);
            this.basicAchievements = Collections.unmodifiableList(basicAchievements);
            this.privilegeAchievements = Collections.unmodifiableList(privilegeAchievements);
            this.quests = Collections.unmodifiableList(quests);
            this.chargeMilestones = Collections.unmodifiableList(chargeMilestones);
            this.config = Collections.unmodifiableMap(config);

            for (CheckInMilestone m : this.checkInMilestones) {
                this.checkInByDay.put(m.dayNumber, m);
            }
            for (MilestoneEntry e : this.levelGifts) {
                this.levelByThreshold.put(e.threshold, e);
            }
            for (MilestoneEntry e : this.onlineGifts) {
                this.onlineByThreshold.put(e.threshold, e);
            }
            for (GiftCode c : this.giftCodes) {
                this.codeByText.put(normalizeCode(c.code), c);
            }
            for (DailyPack p : this.dailyPacks) {
                this.dailyByLevel.put(p.packageLevel, p);
            }
            for (VipPack p : this.vipPacks) {
                this.vipByLevel.put(p.vipLevel, p);
            }
            for (Mail m : this.mailTemplates) {
                this.templateById.put(m.id, m);
            }
            for (Mail m : this.broadcastMails) {
                this.broadcastById.put(m.id, m);
            }
            for (Achievement a : this.basicAchievements) {
                this.achievementByKey.put(a.key(), a);
            }
            for (Achievement a : this.privilegeAchievements) {
                this.achievementByKey.put(a.key(), a);
            }
            for (Quest q : this.quests) {
                this.questStepIndex.put(q.step, q);
                this.questIdIndex.put(q.id, q);
            }
            for (ChargeMilestone c : this.chargeMilestones) {
                this.chargeByAmount.put(c.amount, c);
            }

            int weight = 0;
            for (PoolEntry e : this.checkInPool) {
                weight += Math.max(1, e.weight);
            }
            this.totalPoolWeight = weight;
        }

        public CheckInMilestone checkInMilestone(int dayNumber) {
            return checkInByDay.get(dayNumber);
        }

        /** @param giftType 4 = level gift, 5 = online gift. */
        public MilestoneEntry milestone(int giftType, long threshold) {
            return giftType == 5 ? onlineByThreshold.get(threshold) : levelByThreshold.get(threshold);
        }

        /** @param giftType 4 = level gift, 5 = online gift. */
        public List<MilestoneEntry> milestones(int giftType) {
            return giftType == 5 ? onlineGifts : levelGifts;
        }

        /** Case-insensitive and whitespace-tolerant, matching the client's own comparison. */
        public GiftCode giftCode(String code) {
            return code == null ? null : codeByText.get(normalizeCode(code));
        }

        public DailyPack dailyPack(int packageLevel) {
            return dailyByLevel.get(packageLevel);
        }

        public VipPack vipPack(int vipLevel) {
            return vipByLevel.get(vipLevel);
        }

        public Mail broadcastMail(int mailId) {
            return broadcastById.get(mailId);
        }

        /** Any {@code gift_mail_template} row by id, personal templates included. */
        public Mail mailTemplate(int templateId) {
            return templateById.get(templateId);
        }

        public Achievement achievement(int group, int id) {
            return achievementByKey.get(group + ":" + id);
        }

        /** @param group 0 = basic, 1 = privilege. */
        public List<Achievement> achievements(int group) {
            return group == 1 ? privilegeAchievements : basicAchievements;
        }

        public Quest questByStep(int step) {
            return questStepIndex.get(step);
        }

        public Quest questById(int id) {
            return questIdIndex.get(id);
        }

        /** Highest {@code step} present, so the service can tell when the chain is finished. */
        public int lastQuestStep() {
            int max = -1;
            for (Quest q : quests) {
                if (q.step > max) {
                    max = q.step;
                }
            }
            return max;
        }

        public ChargeMilestone chargeMilestone(long amount) {
            return chargeByAmount.get(amount);
        }


        public long chargeTarget() {
            long configured = configLong(CFG_CHARGE_PROMO_TARGET, 0L);
            if (configured > 0) {
                return configured;
            }
            long max = 0;
            for (ChargeMilestone m : chargeMilestones) {
                if (m.amount > max) {
                    max = m.amount;
                }
            }
            return max > 0 ? max : 1L;
        }

        /** Cost of the achievement privilege track (sub 10 kind 0 {@code unlockPrice}). */
        public int achievementUnlockPrice() {
            long price = configLong(CFG_ACHIEVEMENT_UNLOCK_PRICE, 0L);
            if (price < 0) {
                return 0;
            }
            return price > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) price;
        }

        /** Backs the sub 13 kind 2 "promotion active" push. The client tests {@code == 1} strictly. */
        public boolean chargePromoActive() {
            return configLong(CFG_CHARGE_PROMO_ACTIVE, 0L) == 1L;
        }

        /** How many pool items one check-in rolls ({@code checkin.pool_roll_count}); never below 1. */
        public int checkInPoolRollCount() {
            long count = configLong(CFG_CHECKIN_POOL_ROLL_COUNT, 1L);
            if (count < 1L) {
                return 1;
            }
            return count > MAX_POOL_ROLLS ? MAX_POOL_ROLLS : (int) count;
        }

        public int firstChargeMailTemplate() {
            long id = configLong(CFG_MAIL_FIRST_CHARGE_TEMPLATE, 0L);
            if (id <= 0L || id > Integer.MAX_VALUE) {
                return 0;
            }
            return (int) id;
        }

        /** Weighted random pick for the daily check-in roll; null when the pool is empty. */
        public Reward rollCheckInPool() {
            if (checkInPool.isEmpty()) {
                return null;
            }
            int roll = ThreadLocalRandom.current().nextInt(Math.max(1, totalPoolWeight));
            for (PoolEntry e : checkInPool) {
                roll -= Math.max(1, e.weight);
                if (roll < 0) {
                    return e.reward;
                }
            }
            return checkInPool.get(checkInPool.size() - 1).reward;
        }

        public String configString(String key, String def) {
            if (key == null) {
                return def;
            }
            String v = config.get(key);
            if (v == null) {
                v = config.get(key.replace('.', '_'));
            }
            if (v == null) {
                v = config.get(key.replace('_', '.'));
            }
            return v == null ? def : v;
        }

        public long configLong(String key, long def) {
            return asLong(configString(key, null), def);
        }
    }

    public static Snapshot snapshot() {
        startAutoReload();
        Snapshot local = cache;
        if (local == null) {
            reload();
            local = cache;
        }
        return local;
    }

    public static void reload() {
        synchronized (RELOAD_LOCK) {
            Snapshot built = loadAll();
            if (built != null) {
                cache = built;
            } else if (cache == null) {
                cache = emptySnapshot();
            }
        }
    }

    private static final String[] GIFT_TEMPLATE_TABLES = {
        "gift_checkin", "gift_level_milestone", "gift_online_milestone",
        "gift_code", "gift_daily_pack", "gift_vip_pack", "gift_mail_template",
        "gift_achievement", "gift_quest", "gift_charge_milestone", "gift_config"};

    private static long giftHash() {
        long h = 1125899906842597L;
        boolean any = false;
        try (Connection con = AlyraManager.getConnection_Data()) {
            for (String tb : GIFT_TEMPLATE_TABLES) {

                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT COALESCE(SUM(CRC32(CONCAT_WS('#', `id`, `name`, `data`,"
                        + " `sort_order`, `active`)) * `id`), 0) + COUNT(*) AS chk FROM `" + tb + "`");
                        ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        h = h * 31 + rs.getLong("chk");
                    }
                    any = true;
                } catch (Exception ex) {
                    utils.Logger.error("giftHash " + tb + ": " + ex + "\n");
                }
            }
        } catch (Exception e) {
            utils.Logger.error("giftHash conn: " + e + "\n");
            return Long.MIN_VALUE;
        }
        return any ? h : Long.MIN_VALUE;
    }
    private static volatile boolean autoReloadStarted = false;

    public static void startAutoReload() {
        if (autoReloadStarted) {
            return;
        }
        synchronized (RELOAD_LOCK) {
            if (autoReloadStarted) {
                return;
            }
            autoReloadStarted = true;
        }
        Thread t = new Thread(() -> {
            long last = giftHash();
            utils.Logger.success("GiftAutoReload hash ban dau = " + last
                    + (last == Long.MIN_VALUE ? " (LOI: giftHash fail, xem dong giftHash <bang>: ... phia tren)" : " (OK)") + "\n");
            while (true) {
                try {
                    Thread.sleep(3000);
                    long cur = giftHash();
                    if (cur != Long.MIN_VALUE && cur != last) {
                        reload();
                        last = cur;
                        services.func.GiftSystemService.gI().broadcastGiftUpdate();
                        utils.Logger.success("Auto-reload gift (du lieu thay doi)\n");
                    }
                } catch (Exception e) {
                }
            }
        }, "GiftAutoReload");
        t.setDaemon(true);
        t.start();
        utils.Logger.success("GiftAutoReload da khoi dong\n");
    }

    private static Snapshot emptySnapshot() {
        return new Snapshot(new ArrayList<CheckInMilestone>(), new ArrayList<PoolEntry>(),
                new ArrayList<MilestoneEntry>(), new ArrayList<MilestoneEntry>(),
                new ArrayList<GiftCode>(), new ArrayList<DailyPack>(), new ArrayList<VipPack>(),
                new ArrayList<Mail>(), new ArrayList<Achievement>(), new ArrayList<Achievement>(),
                new ArrayList<Quest>(), new ArrayList<ChargeMilestone>(), new HashMap<String, String>());
    }

    public static void ensureTables() {
        try (Connection con = AlyraManager.getConnection_Data();
                Statement st = con.createStatement()) {

            st.executeUpdate("CREATE TABLE IF NOT EXISTS `gift_checkin` ("
                    + "`id` INT(11) NOT NULL,"
                    + "`kind` VARCHAR(16) NOT NULL DEFAULT 'MILESTONE',"
                    + "`name` VARCHAR(150) NOT NULL DEFAULT '',"
                    + "`data` TEXT NOT NULL,"
                    + "`sort_order` INT(11) NOT NULL DEFAULT 0,"
                    + "`active` TINYINT(1) NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`id`),"
                    + "KEY `idx_checkin_kind` (`kind`, `sort_order`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            st.executeUpdate(contentTable("gift_level_milestone"));
            st.executeUpdate(contentTable("gift_online_milestone"));
            st.executeUpdate(contentTable("gift_code"));
            st.executeUpdate(contentTable("gift_daily_pack"));
            st.executeUpdate(contentTable("gift_vip_pack"));
            st.executeUpdate(contentTable("gift_mail_template"));
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `gift_achievement` ("
                    + "`id` INT(11) NOT NULL,"
                    + "`grp` TINYINT(4) NOT NULL DEFAULT 0,"
                    + "`name` VARCHAR(150) NOT NULL DEFAULT '',"
                    + "`data` TEXT NOT NULL,"
                    + "`sort_order` INT(11) NOT NULL DEFAULT 0,"
                    + "`active` TINYINT(1) NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`id`),"
                    + "KEY `idx_achievement_grp` (`grp`, `sort_order`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            st.executeUpdate(contentTable("gift_quest"));
            st.executeUpdate(contentTable("gift_charge_milestone"));
            st.executeUpdate("CREATE TABLE IF NOT EXISTS `gift_config` ("
                    + "`id` INT(11) NOT NULL,"
                    + "`name` VARCHAR(150) NOT NULL DEFAULT '',"
                    + "`data` TEXT NOT NULL,"
                    + "`sort_order` INT(11) NOT NULL DEFAULT 0,"
                    + "`active` TINYINT(1) NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`id`),"
                    + "UNIQUE KEY `uk_config_name` (`name`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS `gift_mail_player` ("
                    + "`id` INT(11) NOT NULL AUTO_INCREMENT,"
                    + "`player_id` BIGINT(20) NOT NULL DEFAULT 0,"
                    + "`template_id` INT(11) NOT NULL DEFAULT 0,"
                    + "`sender` VARCHAR(64) NOT NULL DEFAULT '',"
                    + "`title` VARCHAR(255) NOT NULL DEFAULT '',"
                    + "`body` TEXT NOT NULL,"
                    + "`rewards` TEXT NOT NULL,"
                    + "`sent_time` BIGINT(20) NOT NULL DEFAULT 0,"
                    + "`expire_time` BIGINT(20) NOT NULL DEFAULT 0,"
                    + "`active` TINYINT(1) NOT NULL DEFAULT 1,"
                    + "PRIMARY KEY (`id`),"
                    + "KEY `idx_mail_player` (`player_id`, `active`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

            st.executeUpdate("CREATE TABLE IF NOT EXISTS `gift_code_usage` ("
                    + "`code_id` INT(11) NOT NULL,"
                    + "`used_count` INT(11) NOT NULL DEFAULT 0,"
                    + "PRIMARY KEY (`code_id`)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi tạo bảng gift system (nro_data)");
        }
    }

    private static String contentTable(String table) {
        return "CREATE TABLE IF NOT EXISTS `" + table + "` ("
                + "`id` INT(11) NOT NULL,"
                + "`name` VARCHAR(150) NOT NULL DEFAULT '',"
                + "`data` TEXT NOT NULL,"
                + "`sort_order` INT(11) NOT NULL DEFAULT 0,"
                + "`active` TINYINT(1) NOT NULL DEFAULT 1,"
                + "PRIMARY KEY (`id`)"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4";
    }

    private static String contentSelect(String table) {
        return "SELECT `id`, `name`, `data` FROM `" + table + "` "
                + "WHERE `active` = 1 ORDER BY `sort_order`, `id`";
    }

    private static Snapshot loadAll() {
        try (Connection con = AlyraManager.getConnection_Data()) {
            CheckInContent checkIn = loadCheckIn(con);
            return new Snapshot(
                    checkIn.milestones,
                    checkIn.pool,
                    loadMilestones(con, "gift_level_milestone", 4),
                    loadMilestones(con, "gift_online_milestone", 5),
                    loadGiftCodes(con),
                    loadDailyPacks(con),
                    loadVipPacks(con),
                    loadMailTemplates(con),
                    loadAchievements(con, 0),
                    loadAchievements(con, 1),
                    loadQuests(con),
                    loadChargeMilestones(con),
                    loadConfig(con));
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi load gift templates");
            return null;
        }
    }

    private static Map<String, String> loadConfig(Connection con) throws Exception {
        Map<String, String> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_config"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String key = nullToEmpty(rs.getString("name")).trim();
                if (key.isEmpty()) {
                    Logger.error("gift99: gift_config id=" + id + " khong co name, bo qua\n");
                    continue;
                }
                String raw = rs.getString("data");
                Object node = (raw == null) ? null : JSONValue.parse(raw);
                Object value = (node instanceof JSONObject)
                        ? firstNonNull(((JSONObject) node).get("value"), ((JSONObject) node).get("val"))
                        : node;
                if (value == null) {
                    Logger.error("gift99: gift_config \"" + key + "\" thieu truong value, bo qua\n");
                    continue;
                }
                map.put(key, String.valueOf(value));
            }
        }
        return map;
    }

    private static final class CheckInContent {

        final List<CheckInMilestone> milestones = new ArrayList<>();
        final List<PoolEntry> pool = new ArrayList<>();
    }

    private static CheckInContent loadCheckIn(Connection con) throws Exception {
        CheckInContent out = new CheckInContent();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT `id`, `kind`, `name`, `data` FROM `gift_checkin` "
                + "WHERE `active` = 1 ORDER BY `sort_order`, `id`");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_checkin", id);
                    if (data == null) {
                        continue;
                    }
                    String kind = nullToEmpty(rs.getString("kind")).trim().toUpperCase(Locale.ROOT);
                    if ("POOL".equals(kind)) {
                        PoolEntry entry = readPoolEntry(data);
                        if (entry == null) {
                            Logger.error("gift99: gift_checkin id=" + id
                                    + " kind=POOL khong co vat pham hop le, bo qua\n");
                            continue;
                        }
                        out.pool.add(entry);
                    } else if ("MILESTONE".equals(kind) || kind.isEmpty()) {
                        CheckInMilestone m = new CheckInMilestone();
                        m.dayNumber = (int) asLong(firstNonNull(data.get("dayNumber"),
                                data.get("day_number"), data.get("day")), 0L);
                        m.rewards = parseRewardNode(data.get("rewards"));
                        out.milestones.add(m);
                    } else {
                        Logger.error("gift99: gift_checkin id=" + id + " kind=\"" + kind
                                + "\" khong hop le, bo qua\n");
                    }
                } catch (Exception e) {
                    Logger.logException(GiftDataDAO.class, e, "Lỗi đọc gift_checkin id=" + id);
                }
            }
        }
        return out;
    }

    private static PoolEntry readPoolEntry(JSONObject data) {
        Reward reward = null;
        Object rewardsNode = data.get("rewards");
        if (rewardsNode != null) {
            List<Reward> list = parseRewardNode(rewardsNode);
            if (!list.isEmpty()) {
                reward = list.get(0);
            }
        }
        if (reward == null) {
            reward = parseReward(data);
        }
        if (reward == null || reward.amount == 0) {
            return null;
        }
        PoolEntry entry = new PoolEntry();
        entry.reward = reward;
        entry.weight = Math.max(1, (int) asLong(data.get("weight"), 1L));
        return entry;
    }

    private static List<MilestoneEntry> loadMilestones(Connection con, String table, int giftType)
            throws Exception {
        List<MilestoneEntry> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect(table));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), table, id);
                    if (data == null) {
                        continue;
                    }
                    MilestoneEntry e = new MilestoneEntry();
                    e.giftType = giftType;
                    e.threshold = asLong(data.get("threshold"), 0L);
                    e.rewards = parseRewardNode(data.get("rewards"));
                    list.add(e);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc " + table + " id=" + id);
                }
            }
        }
        return list;
    }

    private static List<GiftCode> loadGiftCodes(Connection con) throws Exception {
        Map<Long, Integer> usage = loadGiftCodeUsage(con);
        List<GiftCode> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_code"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                long id = rs.getLong("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_code", id);
                    if (data == null) {
                        continue;
                    }
                    GiftCode c = new GiftCode();
                    c.id = id;
                    c.code = asString(data.get("code"), "").trim();
                    if (c.code.isEmpty()) {
                        Logger.error("gift99: gift_code id=" + id + " thieu truong code, bo qua\n");
                        continue;
                    }
                    c.scope = (int) asLong(data.get("scope"), 1L);
                    c.stock = (int) asLong(data.get("stock"), -1L);
                    c.expireTime = parseTimestamp(data.get("expireAt"));
                    c.targetPlayers = parseNameSet(firstNonNull(data.get("players"),
                            data.get("targetPlayers"), data.get("target_players")));
                    c.rewards = parseRewardNode(data.get("rewards"));
                    Integer used = usage.get(id);
                    c.usedCount = (used == null) ? 0 : used.intValue();
                    list.add(c);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc gift_code id=" + id);
                }
            }
        }
        return list;
    }

    private static Map<Long, Integer> loadGiftCodeUsage(Connection con) {
        Map<Long, Integer> map = new HashMap<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT `code_id`, `used_count` FROM `gift_code_usage`");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                map.put(rs.getLong("code_id"), rs.getInt("used_count"));
            }
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi load gift_code_usage");
        }
        return map;
    }

    private static List<DailyPack> loadDailyPacks(Connection con) throws Exception {
        List<DailyPack> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_daily_pack"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_daily_pack", id);
                    if (data == null) {
                        continue;
                    }
                    DailyPack p = new DailyPack();
                    p.packageLevel = (int) asLong(data.get("packageLevel"), id);
                    p.label = asString(firstNonNull(data.get("label"), rs.getString("name")), "");
                    p.vndCost = (int) asLong(data.get("vndCost"), 0L);
                    p.rewards = parseRewardNode(data.get("rewards"));
                    list.add(p);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc gift_daily_pack id=" + id);
                }
            }
        }
        return list;
    }

    private static List<VipPack> loadVipPacks(Connection con) throws Exception {
        List<VipPack> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_vip_pack"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_vip_pack", id);
                    if (data == null) {
                        continue;
                    }
                    VipPack p = new VipPack();
                    p.vipLevel = (int) asLong(data.get("vipLevel"), id);
                    p.label = asString(firstNonNull(data.get("label"), rs.getString("name")), "");
                    p.description = asString(data.get("description"), "");
                    p.vndCost = (int) asLong(data.get("vndCost"), 0L);
                    p.requiredVipLevel = (int) asLong(data.get("requiredVipLevel"), 0L);
                    p.rewards = parseRewardNode(data.get("rewards"));
                    list.add(p);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc gift_vip_pack id=" + id);
                }
            }
        }
        return list;
    }

    private static List<Mail> loadMailTemplates(Connection con) throws Exception {
        List<Mail> list = new ArrayList<>();
        long now = System.currentTimeMillis();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_mail_template"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_mail_template", id);
                    if (data == null) {
                        continue;
                    }
                    Mail m = new Mail();
                    m.id = id;
                    m.mailKind = (int) asLong(data.get("mailKind"), 1L);
                    m.playerId = 0L;
                    m.sender = asString(data.get("sender"), "");
                    m.title = asString(firstNonNull(data.get("title"), rs.getString("name")), "");
                    m.body = asString(data.get("body"), "");
                    m.expireDays = (int) asLong(data.get("expireDays"), 0L);
                    m.rewards = parseRewardNode(data.get("rewards"));
                    if (m.mailKind == 1) {
                        Long seen = BROADCAST_FIRST_SEEN.putIfAbsent(id, now);
                        m.sentTime = (seen == null) ? now : seen.longValue();
                        m.expireTime = relativeExpiry(m.sentTime, m.expireDays);
                    } else {
                        m.sentTime = 0L;
                        m.expireTime = 0L;
                    }
                    list.add(m);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc gift_mail_template id=" + id);
                }
            }
        }
        return list;
    }

    private static List<Achievement> loadAchievements(Connection con, int group) throws Exception {
        List<Achievement> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(
                "SELECT `id`, `grp`, `name`, `data` FROM `gift_achievement` "
                + "WHERE `active` = 1 AND `grp` = ? ORDER BY `sort_order`, `id`")) {
            ps.setInt(1, group);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    try {
                        JSONObject data = rowData(rs.getString("data"), "gift_achievement", id);
                        if (data == null) {
                            continue;
                        }
                        Achievement a = new Achievement();
                        a.id = id;
                        a.group = rs.getInt("grp");
                        a.name = nullToEmpty(rs.getString("name"));
                        a.description = asString(data.get("description"), "");
                        a.target = (int) asLong(data.get("target"), 0L);
                        a.type = asString(data.get("type"), "").trim();
                        a.param = (int) asLong(data.get("param"), 0L);
                        a.rewards = parseRewardNode(data.get("rewards"));
                        list.add(a);
                    } catch (Exception ex) {
                        Logger.logException(GiftDataDAO.class, ex,
                                "Lỗi đọc gift_achievement id=" + id);
                    }
                }
            }
        }
        return list;
    }

    private static List<Quest> loadQuests(Connection con) throws Exception {
        List<Quest> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_quest"));
                ResultSet rs = ps.executeQuery()) {
            int step = 0;
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_quest", id);
                    if (data == null) {
                        continue; // a skipped row must not burn a step number
                    }
                    Quest q = new Quest();
                    q.id = id;
                    q.title = asString(firstNonNull(data.get("title"), rs.getString("name")), "");
                    q.step = step;
                    q.tasks = parseTaskNode(data.get("tasks"));
                    q.rewards = parseRewardNode(data.get("rewards"));
                    list.add(q);
                    step++;
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex, "Lỗi đọc gift_quest id=" + id);
                }
            }
        }
        return list;
    }

    private static List<ChargeMilestone> loadChargeMilestones(Connection con) throws Exception {
        List<ChargeMilestone> list = new ArrayList<>();
        try (PreparedStatement ps = con.prepareStatement(contentSelect("gift_charge_milestone"));
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                int id = rs.getInt("id");
                try {
                    JSONObject data = rowData(rs.getString("data"), "gift_charge_milestone", id);
                    if (data == null) {
                        continue;
                    }
                    ChargeMilestone c = new ChargeMilestone();
                    c.amount = asLong(data.get("amount"), 0L);
                    c.iconPath = asString(data.get("iconPath"), "");
                    c.rewards = parseRewardNode(data.get("rewards"));
                    list.add(c);
                } catch (Exception ex) {
                    Logger.logException(GiftDataDAO.class, ex,
                            "Lỗi đọc gift_charge_milestone id=" + id);
                }
            }
        }
        return list;
    }

    private static final String MAIL_PLAYER_COLUMNS =
            "`id`, `player_id`, `sender`, `title`, `body`, `rewards`, `sent_time`, `expire_time`";

    public static List<Mail> loadPersonalMails(long playerId) {
        List<Mail> list = new ArrayList<>();
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT " + MAIL_PLAYER_COLUMNS + " FROM `gift_mail_player` "
                        + "WHERE `active` = 1 AND `player_id` = ? ORDER BY `sent_time` DESC, `id` DESC")) {
            ps.setLong(1, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(readPersonalMail(rs));
                }
            }
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi load gift_mail_player");
        }
        return list;
    }

    public static Mail findMail(int mailKind, int mailId, long playerId) {
        if (mailKind == 1) {
            return snapshot().broadcastMail(mailId);
        }
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT " + MAIL_PLAYER_COLUMNS + " FROM `gift_mail_player` "
                        + "WHERE `active` = 1 AND `id` = ? AND `player_id` = ?")) {
            ps.setInt(1, mailId);
            ps.setLong(2, playerId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return readPersonalMail(rs);
                }
            }
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi tìm gift_mail_player");
        }
        return null;
    }

    private static Mail readPersonalMail(ResultSet rs) throws Exception {
        Mail m = new Mail();
        m.id = rs.getInt("id");
        m.mailKind = 0;
        m.playerId = rs.getLong("player_id");
        m.sender = nullToEmpty(rs.getString("sender"));
        m.title = nullToEmpty(rs.getString("title"));
        m.body = nullToEmpty(rs.getString("body"));
        m.rewards = parseRewards(rs.getString("rewards"));
        m.sentTime = rs.getLong("sent_time");
        if (m.sentTime <= 0) {
            m.sentTime = System.currentTimeMillis();
        }
        m.expireTime = rs.getLong("expire_time");
        return m;
    }


    public static int insertPersonalMail(long playerId, String sender, String title, String body,
            List<Reward> rewards, long expireTime) {
        return insertPersonalMail(playerId, 0, sender, title, body, rewards, expireTime);
    }

    private static int insertPersonalMail(long playerId, int templateId, String sender, String title,
            String body, List<Reward> rewards, long expireTime) {
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT INTO `gift_mail_player` (`player_id`, `template_id`, `sender`, `title`, "
                        + "`body`, `rewards`, `sent_time`, `expire_time`, `active`) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1)",
                        Statement.RETURN_GENERATED_KEYS)) {
            ps.setLong(1, playerId);
            ps.setInt(2, templateId);
            ps.setString(3, nullToEmpty(sender));
            ps.setString(4, nullToEmpty(title));
            ps.setString(5, nullToEmpty(body));
            ps.setString(6, writeRewards(rewards));
            ps.setLong(7, System.currentTimeMillis());
            ps.setLong(8, expireTime);
            if (ps.executeUpdate() == 1) {
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        return keys.getInt(1);
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi gửi gift_mail_player");
        }
        return -1;
    }

    public static int sendTemplateMail(long playerId, int templateId) {
        Mail template = snapshot().mailTemplate(templateId);
        if (template == null) {
            Logger.error("gift99: gift_mail_template id=" + templateId + " khong ton tai\n");
            return -1;
        }
        long expireTime = relativeExpiry(System.currentTimeMillis(), template.expireDays);
        return insertPersonalMail(playerId, templateId, template.sender, template.title,
                template.body, template.rewards, expireTime);
    }

    public static boolean deletePersonalMail(int mailId, long playerId) {
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM `gift_mail_player` WHERE `id` = ? AND `player_id` = ?")) {
            ps.setInt(1, mailId);
            ps.setLong(2, playerId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi xóa gift_mail_player");
            return false;
        }
    }

    public static boolean tryConsumeGiftCode(long codeId) {
        GiftCode cached = null;
        Snapshot s = snapshot();
        if (s != null) {
            for (GiftCode c : s.giftCodes) {
                if (c.id == codeId) {
                    cached = c;
                    break;
                }
            }
        }
        if (cached == null) {
            Logger.error("gift99: gift_code id=" + codeId + " khong con trong snapshot, tu choi\n");
            return false;
        }
        int stock = cached.stock;

        try (Connection con = AlyraManager.getConnection_Data()) {
            try (PreparedStatement seed = con.prepareStatement(
                    "INSERT IGNORE INTO `gift_code_usage` (`code_id`, `used_count`) VALUES (?, 0)")) {
                seed.setLong(1, codeId);
                seed.executeUpdate();
            }
            try (PreparedStatement ps = con.prepareStatement(
                    "UPDATE `gift_code_usage` SET `used_count` = `used_count` + 1 "
                    + "WHERE `code_id` = ? AND (? < 0 OR `used_count` < ?)")) {
                ps.setLong(1, codeId);
                ps.setInt(2, stock);
                ps.setInt(3, stock);
                if (ps.executeUpdate() == 1) {
                    cached.usedCount++; // keep the cache roughly honest; the DB stays the authority
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi trừ lượt gift_code");
            return false; // DB lỗi thì từ chối — thà chặn nhầm còn hơn phát trùng
        }
    }

    public static boolean setConfig(String key, String value) {
        if (key == null || key.trim().isEmpty()) {
            return false;
        }
        String name = key.trim();
        String blob = writeConfigValue(value);
        synchronized (RELOAD_LOCK) {
            try (Connection con = AlyraManager.getConnection_Data()) {
                try (PreparedStatement update = con.prepareStatement(
                        "UPDATE `gift_config` SET `data` = ?, `active` = 1 WHERE `name` = ?")) {
                    update.setString(1, blob);
                    update.setString(2, name);
                    if (update.executeUpdate() >= 1) {
                        return true;
                    }
                }

                int nextId = 1;
                try (PreparedStatement max = con.prepareStatement(
                        "SELECT COALESCE(MAX(`id`), 0) + 1 FROM `gift_config`");
                        ResultSet rs = max.executeQuery()) {
                    if (rs.next()) {
                        nextId = rs.getInt(1);
                    }
                }
                try (PreparedStatement insert = con.prepareStatement(
                        "INSERT INTO `gift_config` (`id`, `name`, `data`, `sort_order`, `active`) "
                        + "VALUES (?, ?, ?, ?, 1) ON DUPLICATE KEY UPDATE `data` = VALUES(`data`), "
                        + "`active` = 1")) {
                    insert.setInt(1, nextId);
                    insert.setString(2, name);
                    insert.setString(3, blob);
                    insert.setInt(4, nextId);
                    return insert.executeUpdate() >= 1;
                }
            } catch (Exception e) {
                Logger.logException(GiftDataDAO.class, e, "Lỗi ghi gift_config");
                return false;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static String writeConfigValue(String value) {
        JSONObject o = new JSONObject();
        String raw = (value == null) ? "" : value.trim();
        try {
            o.put("value", Long.valueOf(Long.parseLong(raw)));
        } catch (Exception notANumber) {
            o.put("value", raw);
        }
        return o.toJSONString();
    }

    private static JSONObject rowData(String raw, String table, Object id) {
        if (raw == null || raw.trim().isEmpty()) {
            Logger.error("gift99: " + table + " id=" + id + " co cot data rong, bo qua\n");
            return null;
        }
        try {
            Object root = JSONValue.parse(raw);
            if (root instanceof JSONObject) {
                return (JSONObject) root;
            }
            Logger.error("gift99: " + table + " id=" + id
                    + " co cot data khong phai JSON object, bo qua\n");
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi parse JSON " + table + " id=" + id);
        }
        return null;
    }

    public static List<Reward> parseRewards(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return parseRewardNode(JSONValue.parse(raw));
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi parse rewards JSON: " + raw);
            return new ArrayList<>();
        }
    }

    private static List<Reward> parseRewardNode(Object node) {
        List<Reward> list = new ArrayList<>();
        if (node == null) {
            return list;
        }
        if (node instanceof String) {
            Object reparsed = JSONValue.parse((String) node);
            return (reparsed instanceof String) ? list : parseRewardNode(reparsed);
        }
        if (!(node instanceof JSONArray)) {
            return list;
        }
        for (Object element : (JSONArray) node) {
            Reward r = parseReward(element);
            // amount 0 is a data error; itemId 0 is a legal template id so it is NOT filtered.
            if (r != null && r.amount != 0) {
                list.add(r);
            }
        }
        return list;
    }

    private static Reward parseReward(Object element) {
        if (element == null) {
            return null;
        }
        // A nested JSON string (this codebase sometimes stores arrays-of-strings) — unwrap once.
        if (element instanceof String) {
            Object reparsed = JSONValue.parse((String) element);
            if (reparsed == null || reparsed instanceof String) {
                return null;
            }
            return parseReward(reparsed);
        }
        if (element instanceof JSONObject) {
            JSONObject o = (JSONObject) element;
            Reward r = new Reward();
            r.itemId = (int) asLong(firstNonNull(o.get("itemId"), o.get("id"), o.get("item_id")), 0L);
            r.amount = (int) asLong(firstNonNull(o.get("amount"), o.get("quantity"), o.get("qty")), 1L);
            r.options = parseOptions(firstNonNull(o.get("options"), o.get("opt"), o.get("opts")));
            return r;
        }
        if (element instanceof JSONArray) {
            JSONArray a = (JSONArray) element;
            if (a.isEmpty()) {
                return null;
            }
            Reward r = new Reward();
            r.itemId = (int) asLong(a.get(0), 0L);
            r.amount = a.size() > 1 ? (int) asLong(a.get(1), 1L) : 1;
            r.options = a.size() > 2 ? parseOptions(a.get(2)) : new ArrayList<int[]>();
            return r;
        }
        return null;
    }

    /** Parses an option block: {@code [[optionId, optionParam], ...]}. Never returns null. */
    private static List<int[]> parseOptions(Object node) {
        List<int[]> options = new ArrayList<>();
        if (node == null) {
            return options;
        }
        if (node instanceof String) {
            Object reparsed = JSONValue.parse((String) node);
            return reparsed instanceof String ? options : parseOptions(reparsed);
        }
        if (!(node instanceof JSONArray)) {
            return options;
        }
        for (Object element : (JSONArray) node) {
            if (element instanceof JSONArray) {
                JSONArray pair = (JSONArray) element;
                if (pair.size() >= 2) {
                    options.add(new int[]{(int) asLong(pair.get(0), 0L), (int) asLong(pair.get(1), 0L)});
                }
            } else if (element instanceof JSONObject) {
                JSONObject o = (JSONObject) element;
                options.add(new int[]{
                    (int) asLong(firstNonNull(o.get("id"), o.get("optionId")), 0L),
                    (int) asLong(firstNonNull(o.get("param"), o.get("optionParam"), o.get("value")), 0L)});
            }
        }
        return options;
    }

    /** Serialises a reward list back to the canonical seeded form (used by {@link #insertPersonalMail}). */
    @SuppressWarnings("unchecked")
    public static String writeRewards(List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return EMPTY_JSON;
        }
        JSONArray arr = new JSONArray();
        for (Reward r : rewards) {
            if (r == null) {
                continue;
            }
            JSONObject o = new JSONObject();
            o.put("itemId", (long) r.itemId);
            o.put("amount", (long) r.amount);
            JSONArray opts = new JSONArray();
            if (r.options != null) {
                for (int[] pair : r.options) {
                    if (pair == null || pair.length < 2) {
                        continue;
                    }
                    JSONArray p = new JSONArray();
                    p.add((long) pair[0]);
                    p.add((long) pair[1]);
                    opts.add(p);
                }
            }
            o.put("options", opts);
            arr.add(o);
        }
        return arr.toJSONString();
    }

    public static List<QuestTask> parseTasks(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return parseTaskNode(JSONValue.parse(raw));
        } catch (Exception e) {
            Logger.logException(GiftDataDAO.class, e, "Lỗi parse quest tasks JSON: " + raw);
            return new ArrayList<>();
        }
    }

    /** The already-parsed form of {@link #parseTasks(String)}. */
    private static List<QuestTask> parseTaskNode(Object node) {
        List<QuestTask> list = new ArrayList<>();
        if (node == null) {
            return list;
        }
        if (node instanceof String) {
            Object reparsed = JSONValue.parse((String) node);
            return (reparsed instanceof String) ? list : parseTaskNode(reparsed);
        }
        if (!(node instanceof JSONArray)) {
            return list;
        }
        int position = 0;
        for (Object element : (JSONArray) node) {
            if (element instanceof String) {
                element = JSONValue.parse((String) element);
            }
            if (!(element instanceof JSONObject)) {
                position++;
                continue;
            }
            JSONObject o = (JSONObject) element;
            QuestTask t = new QuestTask();
            t.index = (int) asLong(firstNonNull(o.get("index"), o.get("idx")), position);
            t.text = asString(firstNonNull(o.get("text"), o.get("title")), "");
            t.target = (int) asLong(firstNonNull(o.get("target"), o.get("max")), 0L);
            t.type = asString(o.get("type"), "").trim();
            t.param = (int) asLong(o.get("param"), 0L);
            list.add(t);
            position++;
        }
        return list;
    }

    /** Parses the giftcode {@code players} array into a lower-cased name set. */
    private static Set<String> parseNameSet(Object node) {
        Set<String> set = new HashSet<>();
        if (node == null) {
            return set;
        }
        try {
            if (node instanceof JSONArray) {
                for (Object element : (JSONArray) node) {
                    addName(set, element);
                }
                return set;
            }
            String raw = String.valueOf(node).trim();
            if (raw.isEmpty()) {
                return set;
            }
            Object reparsed = JSONValue.parse(raw);
            if (reparsed instanceof JSONArray) {
                for (Object element : (JSONArray) reparsed) {
                    addName(set, element);
                }
                return set;
            }
            // fall back to a plain comma-separated list
            for (String name : raw.split("[,;\r\n]+")) {
                addName(set, name);
            }
        } catch (Exception ignored) {
        }
        return set;
    }

    private static void addName(Set<String> set, Object value) {
        if (value == null) {
            return;
        }
        String name = String.valueOf(value).trim();
        if (!name.isEmpty()) {
            set.add(name.toLowerCase(Locale.ROOT));
        }
    }

    private static long relativeExpiry(long from, int days) {
        return days > 0 ? from + (days * DAY_MILLIS) : 0L;
    }

    private static long parseTimestamp(Object node) {
        if (node == null) {
            return 0L;
        }
        if (node instanceof Number) {
            return ((Number) node).longValue();
        }
        String s = String.valueOf(node).trim();
        if (s.isEmpty() || "null".equalsIgnoreCase(s)) {
            return 0L;
        }
        if (s.matches("-?\\d+")) {
            try {
                return Long.parseLong(s);
            } catch (Exception e) {
                return 0L;
            }
        }
        for (String pattern : TIMESTAMP_PATTERNS) {
            try {
                SimpleDateFormat fmt = new SimpleDateFormat(pattern, Locale.ROOT);
                fmt.setLenient(false);
                return fmt.parse(s).getTime();
            } catch (Exception ignored) {
                // try the next pattern
            }
        }
        Logger.error("gift99: khong doc duoc moc thoi gian \"" + s + "\", coi nhu vinh vien\n");
        return 0L;
    }

    static String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase(Locale.ROOT);
    }

    private static Object firstNonNull(Object... values) {
        for (Object v : values) {
            if (v != null) {
                return v;
            }
        }
        return null;
    }

    static long asLong(Object o, long def) {
        if (o == null) {
            return def;
        }
        if (o instanceof Number) {
            return ((Number) o).longValue();
        }
        try {
            return Long.parseLong(String.valueOf(o).trim());
        } catch (Exception e) {
            return def;
        }
    }

    private static String asString(Object o, String def) {
        return o == null ? def : String.valueOf(o);
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }
}
