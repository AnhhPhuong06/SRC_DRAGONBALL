package services.func;

import database.daos.GiftDataDAO.Achievement;
import database.daos.GiftDataDAO.GiftCode;
import database.daos.GiftDataDAO.Quest;
import database.daos.GiftDataDAO.QuestTask;
import database.daos.GiftDataDAO.Snapshot;
import database.daos.GiftDataDAO.VipPack;
import database.daos.PlayerGiftDAO;
import database.daos.PlayerGiftDAO.GiftState;
import models.player.Player;

public final class GiftRules {

    private GiftRules() {
    }

    public static int checkInStatus(GiftState state, int dayNumber) {
        if (state == null) {
            return 0;
        }
        return state.statusOf(PlayerGiftDAO.KIND_CHECKIN_MILESTONE, dayNumber,
                state.getCheckInDays() >= dayNumber);
    }

    public static String milestoneKind(int sub) {
        return sub == GiftSystemService.SUB_ONLINE_GIFT
                ? PlayerGiftDAO.KIND_ONLINE_GIFT
                : PlayerGiftDAO.KIND_LEVEL_GIFT;
    }

    public static long milestoneCurrent(Player player, GiftState state, int sub) {
        if (sub == GiftSystemService.SUB_ONLINE_GIFT) {
            return state == null ? 0L : state.getDailyOnlineMinutes();
        }
        if (player == null || player.nPoint == null) {
            return 0L;
        }
        return player.nPoint.power;
    }

    public static int milestoneStatus(GiftState state, int sub, long threshold, long current) {
        if (state == null) {
            return 0;
        }
        return state.statusOf(milestoneKind(sub), threshold, current >= threshold);
    }
    public static String giftCodeKey(String code) {
        return code == null ? "" : code.trim().toUpperCase(java.util.Locale.ROOT);
    }

    public static boolean giftCodeRedeemed(GiftState state, String code) {
        return state != null && state.isClaimed(PlayerGiftDAO.KIND_GIFTCODE, giftCodeKey(code));
    }

    public static boolean giftCodeUsed(GiftState state, GiftCode code) {
        if (code == null) {
            return true;
        }
        return giftCodeRedeemed(state, code.code) || !code.hasStock();
    }

    public static boolean isDailyPackBought(GiftState state, int packageLevel) {
        return state != null && state.isClaimed(PlayerGiftDAO.KIND_DAILY_PACK, packageLevel);
    }

    public static boolean isVipPackOwned(GiftState state, int vipLevel) {
        return state != null && state.isClaimed(PlayerGiftDAO.KIND_VIP_PACK, vipLevel);
    }

    public static int vipPackStatus(GiftState state, VipPack pack) {
        if (state == null || pack == null) {
            return 0;
        }
        if (isVipPackOwned(state, pack.vipLevel)) {
            return 1;
        }
        if (pack.requiredVipLevel > 0 && !isVipPackOwned(state, pack.requiredVipLevel)) {
            return 2;
        }
        return 0;
    }

    public static long achievementProgress(GiftState state, Achievement row) {
        if (state == null || row == null) {
            return 0L;
        }
        return state.getCounter(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key());
    }

    public static int achievementProgressInt(GiftState state, Achievement row) {
        return toInt(achievementProgress(state, row));
    }

    public static boolean isAchievementReached(GiftState state, Achievement row) {
        if (row == null) {
            return false;
        }
        return row.target <= 0 || achievementProgress(state, row) >= row.target;
    }

    public static int achievementStatus(GiftState state, Achievement row) {
        if (state == null || row == null) {
            return 0;
        }
        if (state.isClaimed(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key())) {
            return 2;
        }
        if (row.group == 1 && !state.isPrivilegeUnlocked()) {
            return 0;
        }
        return isAchievementReached(state, row) ? 1 : 0;
    }

    public static String questTaskKey(int questId, int taskIndex) {
        return questId + ":" + taskIndex;
    }

    public static long questTaskCount(GiftState state, int questId, int taskIndex) {
        if (state == null) {
            return 0L;
        }
        return state.getCounter(PlayerGiftDAO.KIND_QUEST, questTaskKey(questId, taskIndex));
    }

    public static int questTaskProgress(GiftState state, Quest quest, QuestTask task) {
        if (quest == null || task == null) {
            return 0;
        }
        return toInt(questTaskCount(state, quest.id, task.index));
    }

    public static Quest activeQuest(Snapshot data, GiftState state) {
        if (data == null || state == null || state.isQuestAllCompleted()) {
            return null;
        }
        Quest quest = (state.getQuestId() >= 0) ? data.questById(state.getQuestId()) : null;
        if (quest == null) {
            quest = data.questByStep(state.getQuestStep());
        }
        if (quest == null && !data.quests.isEmpty()) {
            quest = data.quests.get(0);
        }
        return quest;
    }

    public static boolean isQuestAllCompleted(GiftState state) {
        return state != null && state.isQuestAllCompleted();
    }

    public static boolean isQuestClaimable(GiftState state, Quest quest) {
        if (state == null || quest == null || state.isQuestAllCompleted()) {
            return false;
        }
        if (state.isClaimed(PlayerGiftDAO.KIND_QUEST, quest.id)) {
            return false;
        }
        for (QuestTask task : quest.tasks) {
            if (task == null || task.target <= 0) {
                continue;
            }
            if (questTaskCount(state, quest.id, task.index) < task.target) {
                return false;
            }
        }
        return true;
    }

    public static int chargeStatus(GiftState state, long amount) {
        if (state == null) {
            return 0;
        }
        return state.statusOf(PlayerGiftDAO.KIND_CHARGE, amount, state.getTotalCharged() >= amount);
    }

    public static int toInt(long value) {
        if (value > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        if (value < Integer.MIN_VALUE) {
            return Integer.MIN_VALUE;
        }
        return (int) value;
    }
}
