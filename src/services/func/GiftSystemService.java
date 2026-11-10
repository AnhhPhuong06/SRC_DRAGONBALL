package services.func;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import database.daos.GiftDataDAO;
import database.daos.GiftDataDAO.Achievement;
import database.daos.GiftDataDAO.ChargeMilestone;
import database.daos.GiftDataDAO.CheckInMilestone;
import database.daos.GiftDataDAO.DailyPack;
import database.daos.GiftDataDAO.GiftCode;
import database.daos.GiftDataDAO.Mail;
import database.daos.GiftDataDAO.MilestoneEntry;
import database.daos.GiftDataDAO.PoolEntry;
import database.daos.GiftDataDAO.Quest;
import database.daos.GiftDataDAO.QuestTask;
import database.daos.GiftDataDAO.Reward;
import database.daos.GiftDataDAO.Snapshot;
import database.daos.GiftDataDAO.VipPack;
import database.daos.PlayerGiftDAO;
import database.daos.PlayerGiftDAO.GiftState;
import models.player.Player;
import network.io.Message;
import utils.Logger;

public class GiftSystemService {

    public static final int SUB_CHECKIN = 3;
    public static final int SUB_LEVEL_GIFT = 4;
    public static final int SUB_ONLINE_GIFT = 5;
    public static final int SUB_GIFTCODE = 6;
    public static final int SUB_PACK = 7;
    public static final int SUB_TOAST = 8;
    public static final int SUB_MAIL = 9;
    public static final int SUB_ACHIEVEMENT = 10;
    public static final int SUB_QUEST = 11;
    public static final int SUB_CHARGE_PROMO = 13;

    private static final int MAX_U8 = 255;

    private static final int MAX_UTF_CHARS = 5000;

    private static GiftSystemService instance;

    public static GiftSystemService gI() {
        if (instance == null) {
            instance = new GiftSystemService();
        }
        return instance;
    }

    public void handle(Player player, Message msg) {
        if (player == null || msg == null || msg.reader() == null) {
            return;
        }
        try {
            int sub = msg.reader().readUnsignedByte();
            int cmd = (sub == SUB_GIFTCODE && msg.reader().available() == 0)
                    ? 0
                    : msg.reader().readUnsignedByte();

            switch (sub) {
                case SUB_CHECKIN:
                    // cmd 0 = state, cmd 1 = check in, cmd 2 = claim milestone (i32 day)
                    if (cmd == 0) {
                        sendCheckInState(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                case SUB_LEVEL_GIFT:
                case SUB_ONLINE_GIFT:
                    if (cmd == 2) {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    } else {
                        sendMilestoneState(player, sub);
                    }
                    break;

                case SUB_GIFTCODE:
                    // cmd 0 (synthesised: no command byte at all) = list, cmd 1 = redeem (UTF code)
                    if (cmd == 1) {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    } else {
                        sendGiftCodeList(player);
                    }
                    break;

                case SUB_PACK:
                    if (cmd == 0) {
                        sendDailyPacks(player);
                    } else if (cmd == 2) {
                        sendVipPacks(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                case SUB_MAIL:
                    if (cmd == 0) {
                        sendMailList(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                case SUB_ACHIEVEMENT:
                    if (cmd == 0) {
                        GiftProgressService.gI().syncLevel(player);
                        sendAchievementState(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                case SUB_QUEST:
                    if (cmd == 0) {
                        GiftProgressService.gI().syncLevel(player);
                        sendQuestState(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                case SUB_CHARGE_PROMO:
                    if (cmd == 0) {
                        GiftProgressService.gI().syncCharged(player);
                        sendChargePromoState(player);
                    } else {
                        GiftClaimService.gI().handle(player, sub, cmd, msg);
                    }
                    break;

                default:
                    break;
            }
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 handle");
        }
    }

    public void sendCheckInState(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);

            List<CheckInMilestone> milestones = new ArrayList<>();
            for (CheckInMilestone milestone : data.checkInMilestones) {
                if (milestone != null) {
                    milestones.add(milestone);
                }
            }
            clamp(milestones);

            List<PoolEntry> pool = new ArrayList<>();
            for (PoolEntry entry : data.checkInPool) {
                if (entry != null && entry.reward != null) {
                    pool.add(entry);
                }
            }
            clamp(pool);

            m.writer().writeByte(SUB_CHECKIN);
            m.writer().writeByte(0);
            m.writer().writeByte(state.isCheckedInToday() ? 1 : 0);
            m.writer().writeInt(state.getCheckInDays());

            m.writer().writeByte(milestones.size());
            for (CheckInMilestone milestone : milestones) {
                m.writer().writeInt(milestone.dayNumber);
                m.writer().writeByte(GiftRules.checkInStatus(state, milestone.dayNumber));
                writeRewardListFlat(m, milestone.rewards);
            }

            m.writer().writeByte(pool.size());
            for (PoolEntry entry : pool) {
                writeRewardPool(m, entry.reward);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub3 kind0 state");
        } finally {
            m.cleanup();
        }
    }

    public void sendCheckInClaim(Player player, List<Reward> rewards) {
        writeClaimFlat(player, SUB_CHECKIN, 1, rewards, "gift99 sub3 kind1 claim");
    }

    public void sendMilestoneState(Player player, int sub) {
        if (player == null) {
            return;
        }
        boolean asInt = (sub == SUB_ONLINE_GIFT);
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);
            if (sub == SUB_ONLINE_GIFT) {
                state.rolloverOnlineGiftsIfNewDay(); // làm mới Quà Online lúc 00:00
            }
            long current = GiftRules.milestoneCurrent(player, state, sub);

            List<MilestoneEntry> entries = new ArrayList<>();
            for (MilestoneEntry entry : data.milestones(sub)) {
                if (entry != null) {
                    entries.add(entry);
                }
            }
            clamp(entries);

            m.writer().writeByte(sub);
            m.writer().writeByte(0);
            writeScalar(m, current, asInt);
            m.writer().writeByte(entries.size());
            for (MilestoneEntry entry : entries) {
                writeScalar(m, entry.threshold, asInt);
                m.writer().writeByte(GiftRules.milestoneStatus(state, sub, entry.threshold, current));
                writeRewardListFull(m, entry.rewards);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub" + sub + " kind0 state");
        } finally {
            m.cleanup();
        }
    }

    public void sendMilestoneClaim(Player player, int sub, List<Reward> rewards) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            List<Reward> list = sanitize(rewards);
            m.writer().writeByte(sub);
            m.writer().writeByte(1);
            m.writer().writeByte(list.size());
            for (Reward reward : list) {
                writeRewardFull(m, reward);
            }
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub" + sub + " kind1 claim");
        } finally {
            m.cleanup();
        }
    }

    public void sendGiftCodeList(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);
            long now = System.currentTimeMillis();

            List<GiftCode> codes = new ArrayList<>();
            for (GiftCode code : data.giftCodes) {
                if (code == null || code.code == null || code.code.isEmpty()) {
                    continue;
                }
                if (code.isExpired(now) || !code.isTargeted(player.name)) {
                    continue;
                }
                codes.add(code);
            }
            clamp(codes);

            m.writer().writeByte(SUB_GIFTCODE);
            m.writer().writeByte(0);
            m.writer().writeByte(codes.size());
            for (GiftCode code : codes) {
                m.writer().writeUTF(safe(code.code));
                m.writer().writeByte(code.scope);
                m.writer().writeByte(GiftRules.giftCodeUsed(state, code) ? 1 : 0);
                m.writer().writeLong(code.remainSeconds(now));
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub6 kind0 list");
        } finally {
            m.cleanup();
        }
    }

    public void sendRewardPopup(Player player, List<Reward> rewards) {
        writeClaimFlat(player, SUB_GIFTCODE, 1, rewards, "gift99 sub6 kind1 use");
    }

    public void sendDailyPacks(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);
            state.rolloverDailyPacksIfNewDay(); // làm mới lượt mua gói ngày lúc 00:00

            List<DailyPack> packs = new ArrayList<>();
            for (DailyPack pack : data.dailyPacks) {
                if (pack != null) {
                    packs.add(pack);
                }
            }
            clamp(packs);

            m.writer().writeByte(SUB_PACK);
            m.writer().writeByte(0);
            m.writer().writeByte(packs.size());
            for (DailyPack pack : packs) {
                m.writer().writeInt(pack.packageLevel);
                m.writer().writeUTF(safe(pack.label));
                m.writer().writeInt(pack.vndCost);
                m.writer().writeByte(GiftRules.isDailyPackBought(state, pack.packageLevel) ? 1 : 0);
                writeRewardListFull(m, pack.rewards);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub7 kind0 daily");
        } finally {
            m.cleanup();
        }
    }

    public void sendVipPacks(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);

            List<VipPack> packs = new ArrayList<>();
            for (VipPack pack : data.vipPacks) {
                if (pack != null) {
                    packs.add(pack);
                }
            }
            clamp(packs);

            m.writer().writeByte(SUB_PACK);
            m.writer().writeByte(1);
            m.writer().writeByte(packs.size());
            for (VipPack pack : packs) {
                m.writer().writeInt(pack.vipLevel);
                m.writer().writeUTF(safe(pack.label));
                m.writer().writeUTF(safe(pack.description));
                m.writer().writeInt(pack.vndCost);
                m.writer().writeInt(pack.requiredVipLevel);
                m.writer().writeByte(GiftRules.vipPackStatus(state, pack));
                writeRewardListFull(m, pack.rewards);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub7 kind1 vip");
        } finally {
            m.cleanup();
        }
    }

    public void broadcastGiftUpdate() {
        java.util.List<Player> players;
        try {
            players = new ArrayList<>(server.Client.gI().getPlayers());
        } catch (Exception e) {
            return;
        }
        for (Player pl : players) {
            if (pl == null || pl.session == null) {
                continue;
            }
            try {
                sendCheckInState(pl);
                sendMilestoneState(pl, SUB_LEVEL_GIFT);
                sendMilestoneState(pl, SUB_ONLINE_GIFT);
                sendDailyPacks(pl);
                sendVipPacks(pl);
            } catch (Exception ignore) {
            }
        }
    }

    public void sendMailList(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            GiftState state = PlayerGiftDAO.load(player.id);
            List<Mail> rows = visibleMails(player, state);

            int unread = 0;
            for (Mail mail : rows) {
                if (!state.isMailRead(mail.mailKind, mail.id)) {
                    unread++;
                }
            }

            m.writer().writeByte(SUB_MAIL);
            m.writer().writeByte(0);
            m.writer().writeInt(unread);
            m.writer().writeByte(rows.size());
            for (Mail mail : rows) {
                m.writer().writeByte(mail.mailKind);
                m.writer().writeInt(mail.id);
                m.writer().writeByte(PlayerGiftDAO.wireFlags(state.getMailFlags(mail.mailKind, mail.id)));
                m.writer().writeUTF(safe(mail.sender));
                m.writer().writeUTF(safe(mail.title));
                m.writer().writeLong(mail.sentTime);
                m.writer().writeLong(mail.expireTime);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub9 kind0 list");
        } finally {
            m.cleanup();
        }
    }

    public void sendMailDetail(Player player, int mailKind, int mailId) {
        if (player == null) {
            return;
        }
        sendMailDetail(player, mailKind, mailId, GiftDataDAO.findMail(mailKind, mailId, player.id));
    }

    public void sendMailDetail(Player player, int mailKind, int mailId, Mail mail) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            GiftState state = PlayerGiftDAO.load(player.id);
            if (mail != null && state.isMailDeleted(mailKind, mailId)) {
                mail = null;
            }

            m.writer().writeByte(SUB_MAIL);
            m.writer().writeByte(1);
            m.writer().writeByte(mailKind);
            m.writer().writeInt(mailId);
            if (mail == null) {
                m.writer().writeUTF("");
                m.writer().writeByte(0);
            } else {
                m.writer().writeUTF(safe(mail.body));
                boolean claimable = !state.isMailClaimed(mailKind, mailId)
                        && !mail.isExpired(System.currentTimeMillis());
                if (claimable) {
                    writeRewardListFull(m, mail.rewards);
                } else {
                    m.writer().writeByte(0);
                }
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub9 kind1 detail");
        } finally {
            m.cleanup();
        }
    }

    public void sendMailClaim(Player player, int mailKind, int mailId, boolean ok, List<Reward> rewards) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            List<Reward> list = ok ? sanitize(rewards) : new ArrayList<Reward>();
            m.writer().writeByte(SUB_MAIL);
            m.writer().writeByte(2);
            m.writer().writeByte(mailKind);
            m.writer().writeInt(mailId);
            m.writer().writeByte(ok ? 1 : 0);
            m.writer().writeByte(list.size());
            for (Reward reward : list) {
                writeRewardFlat(m, reward);
            }
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub9 kind2 claim");
        } finally {
            m.cleanup();
        }
    }

    public void sendMailUnreadCount(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            GiftState state = PlayerGiftDAO.load(player.id);
            int unread = 0;
            for (Mail mail : visibleMails(player, state)) {
                if (!state.isMailRead(mail.mailKind, mail.id)) {
                    unread++;
                }
            }
            m.writer().writeByte(SUB_MAIL);
            m.writer().writeByte(3);
            m.writer().writeInt(unread);
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub9 kind3 unread");
        } finally {
            m.cleanup();
        }
    }

    public static List<Mail> visibleMails(Player player, GiftState state) {
        long now = System.currentTimeMillis();
        List<Mail> all = new ArrayList<>(GiftDataDAO.loadPersonalMails(player.id));
        all.addAll(GiftDataDAO.snapshot().broadcastMails);

        List<Mail> kept = new ArrayList<>();
        for (Mail mail : all) {
            if (mail == null || mail.isExpired(now) || state.isMailDeleted(mail.mailKind, mail.id)) {
                continue;
            }
            kept.add(mail);
        }
        Collections.sort(kept, new Comparator<Mail>() {
            @Override
            public int compare(Mail a, Mail b) {
                int byTime = Long.compare(b.sentTime, a.sentTime);
                return byTime != 0 ? byTime : Integer.compare(b.id, a.id);
            }
        });
        clamp(kept);
        return kept;
    }

    public void sendAchievementState(Player player) {
        sendAchievementState(player, false);
    }

    public void sendAchievementState(Player player, boolean autoOpen) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);

            List<Achievement> basic = liveAchievements(data.achievements(0));
            List<Achievement> privilege = liveAchievements(data.achievements(1));

            m.writer().writeByte(SUB_ACHIEVEMENT);
            m.writer().writeByte(0);
            m.writer().writeByte(autoOpen ? 1 : 0);
            m.writer().writeInt(data.achievementUnlockPrice());
            m.writer().writeByte(state.isPrivilegeUnlocked() ? 1 : 0);
            writeAchievementRows(m, state, basic);
            writeAchievementRows(m, state, privilege);

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub10 kind0 state");
        } finally {
            m.cleanup();
        }
    }

    public void sendAchievementClaim(Player player, List<Reward> rewards) {
        writeClaimFlat(player, SUB_ACHIEVEMENT, 1, rewards, "gift99 sub10 kind1 claim");
    }

    private void writeAchievementRows(Message m, GiftState state, List<Achievement> rows) throws Exception {
        m.writer().writeByte(rows.size());
        for (Achievement row : rows) {
            m.writer().writeInt(row.id);
            m.writer().writeUTF(safe(row.name));
            m.writer().writeUTF(safe(row.description));
            m.writer().writeInt(row.target);                              // target FIRST
            m.writer().writeInt(GiftRules.achievementProgressInt(state, row)); // then progress
            m.writer().writeByte(GiftRules.achievementStatus(state, row));
            writeRewardListFull(m, row.rewards);
        }
    }

    private static List<Achievement> liveAchievements(List<Achievement> source) {
        List<Achievement> list = new ArrayList<>();
        for (Achievement row : source) {
            if (row != null) {
                list.add(row);
            }
        }
        clamp(list);
        return list;
    }

    public void sendQuestState(Player player) {
        sendQuestState(player, false);
    }


    public void sendQuestState(Player player, boolean autoOpen) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);
            Quest quest = GiftRules.activeQuest(data, state);

            List<QuestTask> tasks = new ArrayList<>();
            if (quest != null) {
                for (QuestTask task : quest.tasks) {
                    if (task != null) {
                        tasks.add(task);
                    }
                }
                clamp(tasks);
            }

            m.writer().writeByte(SUB_QUEST);
            m.writer().writeByte(0);
            m.writer().writeByte(autoOpen ? 1 : 0);
            m.writer().writeByte(GiftRules.isQuestAllCompleted(state) ? 1 : 0);
            m.writer().writeByte(GiftRules.isQuestClaimable(state, quest) ? 1 : 0);
            m.writer().writeInt(quest == null ? -1 : quest.id);
            m.writer().writeUTF(quest == null ? "" : safe(quest.title));
            m.writer().writeInt(quest == null ? state.getQuestStep() : quest.step);

            m.writer().writeByte(tasks.size());
            for (QuestTask task : tasks) {
                m.writer().writeInt(task.index);
                m.writer().writeUTF(safe(task.text));
                m.writer().writeInt(task.target);                          // target FIRST
                m.writer().writeInt(GiftRules.questTaskProgress(state, quest, task)); // then count
            }

            if (quest == null) {
                m.writer().writeByte(0);
            } else {
                writeRewardListFull(m, quest.rewards);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub11 kind0 state");
        } finally {
            m.cleanup();
        }
    }

    public void sendQuestClaim(Player player, List<Reward> rewards) {
        writeClaimFlat(player, SUB_QUEST, 1, rewards, "gift99 sub11 kind1 claim");
    }

    public void sendChargePromoState(Player player) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            Snapshot data = GiftDataDAO.snapshot();
            GiftState state = PlayerGiftDAO.load(player.id);

            List<ChargeMilestone> milestones = new ArrayList<>();
            for (ChargeMilestone milestone : data.chargeMilestones) {
                if (milestone != null) {
                    milestones.add(milestone);
                }
            }
            clamp(milestones);

            m.writer().writeByte(SUB_CHARGE_PROMO);
            m.writer().writeByte(0);
            m.writer().writeLong(state.getTotalCharged());
            m.writer().writeLong(data.chargeTarget());
            m.writer().writeByte(milestones.size());
            for (ChargeMilestone milestone : milestones) {
                m.writer().writeLong(milestone.amount);
                m.writer().writeByte(GiftRules.chargeStatus(state, milestone.amount));
                m.writer().writeUTF(safe(milestone.iconPath));
                writeRewardListFull(m, milestone.rewards);
            }

            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub13 kind0 state");
        } finally {
            m.cleanup();
        }
    }

    public void sendChargePromoClaim(Player player, List<Reward> rewards) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            List<Reward> list = sanitize(rewards);
            m.writer().writeByte(SUB_CHARGE_PROMO);
            m.writer().writeByte(1);
            m.writer().writeByte(list.size());
            for (Reward reward : list) {
                writeRewardFull(m, reward);
            }
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub13 kind1 claim");
        } finally {
            m.cleanup();
        }
    }

    public void sendChargePromoActive(Player player, boolean active) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            m.writer().writeByte(SUB_CHARGE_PROMO);
            m.writer().writeByte(2);
            m.writer().writeByte(active ? 1 : 0);
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub13 kind2 active");
        } finally {
            m.cleanup();
        }
    }

    public void sendChargePromoActive(Player player) {
        sendChargePromoActive(player, GiftDataDAO.snapshot().chargePromoActive());
    }

    public void sendToast(Player player, String message) {
        if (player == null || message == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            m.writer().writeByte(SUB_TOAST);
            m.writer().writeUTF(safe(message));
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, "gift99 sub8 toast");
        } finally {
            m.cleanup();
        }
    }
 
    public void writeRewardFull(Message m, Reward reward) throws Exception {
        m.writer().writeInt(reward.itemId);
        m.writer().writeInt(reward.amount);
        List<int[]> options = validOptions(reward);
        m.writer().writeByte(options.size());
        for (int[] pair : options) {
            m.writer().writeInt(pair[0]);
            m.writer().writeInt(pair[1]);
        }
    }

    public void writeRewardFlat(Message m, Reward reward) throws Exception {
        m.writer().writeInt(reward.itemId);
        m.writer().writeInt(reward.amount);
    }

    public void writeRewardPool(Message m, Reward reward) throws Exception {
        m.writer().writeInt(reward.itemId);
        m.writer().writeInt(0);
        m.writer().writeInt(reward.amount);
        m.writer().writeInt(0);
        List<int[]> options = validOptions(reward);
        m.writer().writeByte(options.size());
        for (int[] pair : options) {
            m.writer().writeInt(pair[0]);
            m.writer().writeInt(pair[1]);
        }
    }

    /** {@code u8 count} then FULL records. Sanitised and clamped before the count is written. */
    public void writeRewardListFull(Message m, List<Reward> rewards) throws Exception {
        List<Reward> list = sanitize(rewards);
        m.writer().writeByte(list.size());
        for (Reward reward : list) {
            writeRewardFull(m, reward);
        }
    }

    /** {@code u8 count} then FLAT records. */
    public void writeRewardListFlat(Message m, List<Reward> rewards) throws Exception {
        List<Reward> list = sanitize(rewards);
        m.writer().writeByte(list.size());
        for (Reward reward : list) {
            writeRewardFlat(m, reward);
        }
    }

    private void writeScalar(Message m, long value, boolean asInt) throws Exception {
        if (asInt) {
            m.writer().writeInt(GiftRules.toInt(value));
        } else {
            m.writer().writeLong(value);
        }
    }

    /** Shared body of the six {@code u8 count + FLAT} claim packets. */
    private void writeClaimFlat(Player player, int sub, int kind, List<Reward> rewards, String context) {
        if (player == null) {
            return;
        }
        Message m = new Message((byte) 99);
        try {
            List<Reward> list = sanitize(rewards);
            m.writer().writeByte(sub);
            m.writer().writeByte(kind);
            m.writer().writeByte(list.size());
            for (Reward reward : list) {
                writeRewardFlat(m, reward);
            }
            player.sendMessage(m);
        } catch (Exception e) {
            Logger.logException(GiftSystemService.class, e, context);
        } finally {
            m.cleanup();
        }
    }

    public static List<Reward> sanitize(List<Reward> rewards) {
        List<Reward> list = new ArrayList<>();
        if (rewards == null) {
            return list;
        }
        for (Reward reward : rewards) {
            if (reward != null) {
                list.add(reward);
            }
            if (list.size() >= MAX_U8) {
                break;
            }
        }
        return list;
    }

    /** Option pairs that are actually pairs; anything shorter is a data error and is skipped. */
    private static List<int[]> validOptions(Reward reward) {
        List<int[]> options = new ArrayList<>();
        if (reward.options == null) {
            return options;
        }
        for (int[] pair : reward.options) {
            if (pair != null && pair.length >= 2) {
                options.add(pair);
            }
            if (options.size() >= MAX_U8) {
                break;
            }
        }
        return options;
    }

    private static void clamp(List<?> list) {
        while (list.size() > MAX_U8) {
            list.remove(list.size() - 1);
        }
    }

    private static String safe(String s) {
        if (s == null || s.isEmpty()) {
            return "";
        }
        String out = (s.indexOf('\0') >= 0) ? s.replace('\0', ' ') : s;
        return (out.length() > MAX_UTF_CHARS) ? out.substring(0, MAX_UTF_CHARS) : out;
    }
}
