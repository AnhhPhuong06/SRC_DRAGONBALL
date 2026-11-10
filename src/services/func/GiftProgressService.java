package services.func;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import database.daos.GiftDataDAO;
import database.daos.GiftDataDAO.Achievement;
import database.daos.GiftDataDAO.Quest;
import database.daos.GiftDataDAO.QuestTask;
import database.daos.GiftDataDAO.Snapshot;
import database.daos.PlayerDAO;
import database.daos.PlayerGiftDAO;
import database.daos.PlayerGiftDAO.GiftState;
import models.map.ItemMap;
import models.mob.Mob;
import models.player.Player;
import server.Client;
import services.Service;
import utils.Logger;
import models.task.Task_Kol;

public final class GiftProgressService {

    public static final String TYPE_REACH_LEVEL = "REACH_LEVEL";
    public static final String TYPE_KILL_MOB = "KILL_MOB";
    public static final String TYPE_COLLECT_DRAGON_BALL = "COLLECT_DRAGON_BALL";
    public static final String TYPE_CHECKIN_TOTAL = "CHECKIN_TOTAL";
    public static final String TYPE_SOCKET_CRYSTAL = "SOCKET_CRYSTAL";
    public static final String TYPE_PICK_ITEM = "PICK_ITEM";
    public static final String TYPE_ONLINE_MINUTE = "ONLINE_MINUTE";

    private static final int PARAM_NONE = 0;
    private static final int PLANET_NAMEK = 1;
    private static final int EARTH_BALL_MIN = 14;
    private static final int EARTH_BALL_MAX = 20;
    private static GiftProgressService instance;
    private GiftProgressService() {
    }

    public static GiftProgressService gI() {
        if (instance == null) {
            instance = new GiftProgressService();
        }
        return instance;
    }

    public void onLogin(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        try {
            GiftState state = PlayerGiftDAO.load(player.id);
            if (state == null) {
                return;
            }
            syncAbsolutes(player, state);
            PlayerGiftDAO.saveIfDirty(player.id);
            onChargedSynced(player);
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onLogin");
        }
    }

    public void onLogout(Player player) {
        if (player == null) {
            return;
        }
        LAST_CHARGED_READ.remove(player.id); 
        try {
            GiftState state = PlayerGiftDAO.peek(player.id);
            if (state == null) {
                return; 
            }
            syncAbsolutes(player, state);
            PlayerGiftDAO.unload(player.id); 
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onLogout");
        }
    }

    private void syncAbsolutes(Player player, GiftState state) {
        syncCharged(player, state);
        set(state, TYPE_REACH_LEVEL, PARAM_NONE, Service.gI().getCurrLevel(player));
        set(state, TYPE_CHECKIN_TOTAL, PARAM_NONE, state.getCheckInDays());
        set(state, TYPE_ONLINE_MINUTE, PARAM_NONE, state.getOnlineMinutes());
    }
    private static final long CHARGED_REREAD_INTERVAL = 3_000L;
    private static final ConcurrentHashMap<Long, Long> LAST_CHARGED_READ = new ConcurrentHashMap<>();
    public void syncCharged(Player player) {
        refreshCharged(player, false);
    }
    public void onChargedExternally(String username, long delta) {
        if (username == null || username.trim().isEmpty()) {
            return;
        }
        try {
            Player target = Client.gI().getPlayerByUserName(username.trim());
            if (target == null || target.getSession() == null) {
                return; 
            }
            if (!refreshCharged(target, true) && delta > 0) {
                target.getSession().danap += (int) Math.min(delta, Integer.MAX_VALUE);
                mirrorCharged(target);
                Task_Kol.checkThresholds(target); // KOL 30 (nạp 50k) tự hoàn thành ngay khi nạp đủ
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onChargedExternally " + username);
        }
    }

    private boolean refreshCharged(Player player, boolean force) {
        if (player == null || player.getSession() == null) {
            return false;
        }
        boolean applied = false;
        try {
            if (force || shouldReread(player.id)) {
                LAST_CHARGED_READ.put(player.id, System.currentTimeMillis());
                long fresh = PlayerDAO.getTotalDanap(player.getSession().userId);
                if (fresh >= 0) {
                    player.getSession().danap = (int) Math.min(fresh, Integer.MAX_VALUE);
                    applied = true;
                }
            }
            mirrorCharged(player);
            onChargedSynced(player);
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress refreshCharged");
        }
        return applied;
    }

    private static boolean shouldReread(long playerId) {
        long now = System.currentTimeMillis();
        Long last = LAST_CHARGED_READ.get(playerId);
        if (last != null && now - last < CHARGED_REREAD_INTERVAL) {
            return false;
        }
        LAST_CHARGED_READ.put(playerId, now);
        return true;
    }

    private void mirrorCharged(Player player) {
        GiftState state = PlayerGiftDAO.peek(player.id);
        if (state != null) {
            syncCharged(player, state);
        }
    }

    private void syncCharged(Player player, GiftState state) {
        if (player.getSession() == null) {
            return;
        }
        long danap = player.getSession().danap;
        if (danap > state.getTotalCharged()) {
            state.setTotalCharged(danap); 
        }
    }

    public void onChargedSynced(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        Task_Kol.checkThresholds(player); // KOL 30 (nạp 50k) tự hoàn thành khi danap đã sync đủ
        try {
            GiftState state = PlayerGiftDAO.peek(player.id);
            if (state == null || state.getTotalCharged() <= 0L) {
                return; // never topped up — the first-charge mail has nothing to celebrate yet
            }
            GiftMailService.gI().deliverFirstChargeMail(player);
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onChargedSynced");
        }
    }

    public void onOnlineTick(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        try {
            GiftState state = PlayerGiftDAO.peek(player.id);
            if (state == null) {
                return;
            }
            long before = state.getOnlineMinutes();
            state.addOnlineSeconds(1);
            long after = state.getOnlineMinutes();
            if (after != before) {
                set(state, TYPE_ONLINE_MINUTE, PARAM_NONE, after);
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onOnlineTick");
        }
    }

    public void onKillMob(Player player, Mob mob) {
        if (player == null || mob == null) {
            return;
        }
        try {
            add(player, TYPE_KILL_MOB, PARAM_NONE, 1);
            if (mob.zone != null && mob.zone.map != null && mob.zone.map.planetId == PLANET_NAMEK) {
                add(player, TYPE_KILL_MOB, PLANET_NAMEK, 1);
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onKillMob");
        }
    }

    public void onPickItem(Player player, ItemMap item) {
        if (player == null || item == null) {
            return;
        }
        try {
            add(player, TYPE_PICK_ITEM, PARAM_NONE, 1);
            if (item.itemTemplate == null) {
                return;
            }
            int tempId = item.itemTemplate.id;
            if (item.isNamecBall) {
                add(player, TYPE_COLLECT_DRAGON_BALL, PLANET_NAMEK, 1);
            } else if (tempId >= EARTH_BALL_MIN && tempId <= EARTH_BALL_MAX) {
                add(player, TYPE_COLLECT_DRAGON_BALL, PARAM_NONE, 1);
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onPickItem");
        }
    }

    public void onCrystalSocketed(Player player) {
        if (player == null) {
            return;
        }
        try {
            add(player, TYPE_SOCKET_CRYSTAL, PARAM_NONE, 1);
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onCrystalSocketed");
        }
    }

    public void onCheckIn(Player player) {
        if (player == null) {
            return;
        }
        try {
            GiftState state = PlayerGiftDAO.peek(player.id);
            if (state != null) {
                set(state, TYPE_CHECKIN_TOTAL, PARAM_NONE, state.getCheckInDays());
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress onCheckIn");
        }
    }

    public void syncLevel(Player player) {
        if (player == null || !player.isPl()) {
            return;
        }
        try {
            GiftState state = PlayerGiftDAO.peek(player.id);
            if (state != null) {
                set(state, TYPE_REACH_LEVEL, PARAM_NONE, Service.gI().getCurrLevel(player));
            }
        } catch (Exception e) {
            Logger.logException(GiftProgressService.class, e, "gift progress syncLevel");
        }
    }

    public void add(Player player, String type, int param, long delta) {
        if (player == null || delta == 0) {
            return;
        }
        GiftState state = PlayerGiftDAO.peek(player.id);
        if (state != null) {
            apply(state, type, param, delta, false);
        }
    }

    public void set(Player player, String type, int param, long value) {
        if (player == null) {
            return;
        }
        GiftState state = PlayerGiftDAO.peek(player.id);
        if (state != null) {
            apply(state, type, param, value, true);
        }
    }

    private void set(GiftState state, String type, int param, long value) {
        apply(state, type, param, value, true);
    }

    private void apply(GiftState state, String type, int param, long value, boolean absolute) {
        if (state == null || type == null || type.isEmpty()) {
            return;
        }
        Snapshot data = GiftDataDAO.snapshot();
        if (data == null) {
            return;
        }

        applyAchievements(state, data.basicAchievements, type, param, value, absolute);
        applyAchievements(state, data.privilegeAchievements, type, param, value, absolute);

        Quest quest = GiftRules.activeQuest(data, state);
        if (quest != null && quest.tasks != null) {
            for (QuestTask task : quest.tasks) {
                if (task == null || !matches(task.type, task.param, type, param)) {
                    continue;
                }
                String key = GiftRules.questTaskKey(quest.id, task.index);
                if (absolute) {
                    if (value > state.getCounter(PlayerGiftDAO.KIND_QUEST, key)) {
                        state.setCounter(PlayerGiftDAO.KIND_QUEST, key, value);
                    }
                } else {
                    state.addCounter(PlayerGiftDAO.KIND_QUEST, key, value);
                }
            }
        }
    }

    private void applyAchievements(GiftState state, List<Achievement> rows, String type, int param,
            long value, boolean absolute) {
        if (rows == null) {
            return;
        }
        for (Achievement row : rows) {
            if (row == null || !matches(row.type, row.param, type, param)) {
                continue;
            }
            if (absolute) {
                if (value > state.getCounter(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key())) {
                    state.setCounter(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key(), value);
                }
            } else {
                state.addCounter(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key(), value);
            }
        }
    }

    private static boolean matches(String rowType, int rowParam, String type, int param) {
        return rowType != null && rowParam == param && rowType.equalsIgnoreCase(type);
    }
}
