package services.func;

import java.util.ArrayList;
import java.util.List;
import database.daos.DonatePackageDAO;
import database.daos.GiftDataDAO;
import database.daos.GiftDataDAO.Achievement;
import database.daos.GiftDataDAO.ChargeMilestone;
import database.daos.GiftDataDAO.CheckInMilestone;
import database.daos.GiftDataDAO.DailyPack;
import database.daos.GiftDataDAO.GiftCode;
import database.daos.GiftDataDAO.Mail;
import database.daos.GiftDataDAO.MilestoneEntry;
import database.daos.GiftDataDAO.Quest;
import database.daos.GiftDataDAO.Reward;
import database.daos.GiftDataDAO.Snapshot;
import database.daos.GiftDataDAO.VipPack;
import database.daos.PlayerGiftDAO;
import database.daos.PlayerGiftDAO.GiftState;
import models.Template;
import models.item.Item;
import models.player.Player;
import network.io.Message;
import services.ItemService;
import services.player.InventoryService;
import utils.Logger;

public final class GiftClaimService {

    public static final int SUB_CHECKIN = 3;
    public static final int SUB_LEVEL_GIFT = 4;
    public static final int SUB_ONLINE_GIFT = 5;
    public static final int SUB_GIFTCODE = 6;
    public static final int SUB_PACK = 7;
    public static final int SUB_MAIL = 9;
    public static final int SUB_ACHIEVEMENT = 10;
    public static final int SUB_QUEST = 11;
    public static final int SUB_CHARGE_PROMO = 13;
    private static final int TYPE_GOLD = 9;
    private static final int TYPE_GEM = 10;
    private static final int TYPE_RUBY = 34;
    private static GiftClaimService instance;

    private static final ThreadLocal<Boolean> EMITTING_STATE = new ThreadLocal<Boolean>() {
        @Override
        protected Boolean initialValue() {
            return Boolean.FALSE;
        }
    };

    public static GiftClaimService gI() {
        if (instance == null) {
            instance = new GiftClaimService();
        }
        return instance;
    }

    private GiftClaimService() {
    }

    public void handle(Player player, int sub, int cmd, Message msg) {
        if (player == null || msg == null) {
            return;
        }
        if (Boolean.TRUE.equals(EMITTING_STATE.get())) {
            return; 
        }
        try {
            switch (sub) {
                case SUB_CHECKIN:
                    handleCheckIn(player, cmd, msg);
                    break;
                case SUB_LEVEL_GIFT:
                case SUB_ONLINE_GIFT:
                    handleMilestoneGift(player, sub, cmd, msg);
                    break;
                case SUB_GIFTCODE:
                    handleGiftCode(player, cmd, msg);
                    break;
                case SUB_PACK:
                    handlePack(player, cmd, msg);
                    break;
                case SUB_MAIL:
                    handleMail(player, cmd, msg);
                    break;
                case SUB_ACHIEVEMENT:
                    handleAchievement(player, cmd, msg);
                    break;
                case SUB_QUEST:
                    handleQuest(player, cmd, msg);
                    break;
                case SUB_CHARGE_PROMO:
                    handleChargePromo(player, cmd, msg);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Logger.logException(GiftClaimService.class, e,
                    "gift99 claim sub=" + sub + " cmd=" + cmd + " player=" + player.name);
        }
    }

    private void handleCheckIn(Player player, int cmd, Message msg) throws Exception {
        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);

        if (cmd == 1) {
            state.rolloverDailyPacksIfNewDay();
            if (state.isCheckedInToday()) {
                notifyPlayer(player, "Hôm nay bạn đã điểm danh rồi!");
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            Reward rolled = snap.rollCheckInPool();
            if (rolled == null) {
                notifyPlayer(player, "Chưa có phần quà điểm danh nào được cấu hình.");
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            List<Reward> wanted = one(rolled);
            if (!hasBagRoom(player, wanted)) {
                notifyBagFull(player);
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            if (!state.markCheckedInToday()) {
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            List<Reward> granted = grant(player, wanted);
            GiftProgressService.gI().onCheckIn(player);
            PlayerGiftDAO.save(player.id);
            GiftSystemService.gI().sendCheckInClaim(player, granted); // sub 3 claim is FLAT
            sendState(player, SUB_CHECKIN, 0);
            return;
        }

        if (cmd == 2) {
            int day = readI32(msg);
            CheckInMilestone milestone = snap.checkInMilestone(day);
            if (milestone == null) {
                notifyPlayer(player, "Mốc điểm danh không tồn tại.");
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            if (state.isClaimed(PlayerGiftDAO.KIND_CHECKIN_MILESTONE, day)) {
                notifyPlayer(player, "Bạn đã nhận mốc này rồi!");
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            if (state.getCheckInDays() < day) {
                notifyPlayer(player, "Bạn cần điểm danh đủ " + day + " ngày để nhận mốc này.");
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            if (!hasBagRoom(player, milestone.rewards)) {
                notifyBagFull(player);
                sendState(player, SUB_CHECKIN, 0);
                return;
            }
            if (!state.markClaimed(PlayerGiftDAO.KIND_CHECKIN_MILESTONE, day)) {
                sendState(player, SUB_CHECKIN, 0);
                return; // replayed packet
            }
            List<Reward> granted = grant(player, milestone.rewards);
            PlayerGiftDAO.save(player.id);
            GiftSystemService.gI().sendCheckInClaim(player, granted);
            sendState(player, SUB_CHECKIN, 0);
            return;
        }

        sendState(player, SUB_CHECKIN, 0);
    }

    private void handleMilestoneGift(Player player, int sub, int cmd, Message msg) throws Exception {
        if (cmd != 2) {
            sendState(player, sub, 1);
            return;
        }
        long threshold = (sub == SUB_ONLINE_GIFT) ? readI32(msg) : readI64(msg);

        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);
        if (sub == SUB_ONLINE_GIFT) {
            state.rolloverOnlineGiftsIfNewDay(); 
        }
        MilestoneEntry entry = snap.milestone(sub, threshold);
        if (entry == null) {
            notifyPlayer(player, "Mốc quà không tồn tại.");
            sendState(player, sub, 1);
            return;
        }
        String kind = GiftRules.milestoneKind(sub);
        if (state.isClaimed(kind, threshold)) {
            notifyPlayer(player, "Bạn đã nhận mốc này rồi!");
            sendState(player, sub, 1);
            return;
        }
        long current = GiftRules.milestoneCurrent(player, state, sub);
        if (current < threshold) {
            notifyPlayer(player, sub == SUB_ONLINE_GIFT
                    ? "Bạn chưa online đủ thời gian để nhận mốc này."
                    : "Bạn chưa đạt mốc này.");
            sendState(player, sub, 1);
            return;
        }
        if (!hasBagRoom(player, entry.rewards)) {
            notifyBagFull(player);
            sendState(player, sub, 1);
            return;
        }
        if (!state.markClaimed(kind, threshold)) {
            sendState(player, sub, 1);
            return; // replayed packet
        }
        List<Reward> granted = grant(player, entry.rewards);
        PlayerGiftDAO.save(player.id);
        GiftSystemService.gI().sendMilestoneClaim(player, sub, granted); 
        sendState(player, sub, 1);
    }

    private void handleGiftCode(Player player, int cmd, Message msg) throws Exception {
        if (cmd != 1) {
            sendState(player, SUB_GIFTCODE, -1);
            return;
        }
        String raw = readUTF(msg);
        String code = raw == null ? "" : raw.trim();
        if (code.isEmpty()) {
            notifyPlayer(player, "Bạn chưa nhập giftcode!");
            return;
        }

        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);
        GiftCode entry = snap.giftCode(code); 
        if (entry == null) {
            notifyPlayer(player, "Giftcode không tồn tại!");
            return;
        }
        long now = System.currentTimeMillis();
        if (entry.isExpired(now)) {
            notifyPlayer(player, "Giftcode đã hết hạn!");
            return;
        }
        if (!entry.isTargeted(player.name)) {
            notifyPlayer(player, "Giftcode này không dành cho bạn!");
            return;
        }
        String codeKey = GiftRules.giftCodeKey(code);
        if (state.isClaimed(PlayerGiftDAO.KIND_GIFTCODE, codeKey)) {
            notifyPlayer(player, "Giftcode đã được sử dụng!");
            return;
        }

        if (entry.scope == 2 && entry.usedCount > 0) {
            notifyPlayer(player, "Giftcode này đã có người sử dụng!");
            return;
        }
        if (!entry.hasStock()) {
            notifyPlayer(player, "Giftcode đã hết lượt sử dụng!");
            return;
        }
        if (!hasBagRoom(player, entry.rewards)) {
            notifyBagFull(player);
            return;
        }
        if (!state.markClaimed(PlayerGiftDAO.KIND_GIFTCODE, codeKey)) {
            return; // replayed packet
        }

        if (!GiftDataDAO.tryConsumeGiftCode(entry.id)) {
            state.unmarkClaimed(PlayerGiftDAO.KIND_GIFTCODE, codeKey);
            notifyPlayer(player, "Giftcode đã hết lượt sử dụng!");
            return;
        }
        List<Reward> granted = grant(player, entry.rewards);
        PlayerGiftDAO.save(player.id);
        GiftSystemService.gI().sendRewardPopup(player, granted); 
        sendState(player, SUB_GIFTCODE, -1); 
    }

    private void handlePack(Player player, int cmd, Message msg) throws Exception {
        if (cmd != 1 && cmd != 3) {
            sendState(player, SUB_PACK, cmd == 2 ? 2 : 0);
            return;
        }
        int id = readI32(msg);
        int listCmd = (cmd == 1) ? 0 : 2;

        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);
        state.rolloverDailyPacksIfNewDay();
        String claimKind;
        int cost;
        List<Reward> rewards;
        String label;

        if (cmd == 1) {
            DailyPack pack = snap.dailyPack(id);
            if (pack == null) {
                notifyPlayer(player, "Gói quà không tồn tại.");
                sendState(player, SUB_PACK, listCmd);
                return;
            }
            if (state.isClaimed(PlayerGiftDAO.KIND_DAILY_PACK, id)) {
                notifyPlayer(player, "Bạn đã mua gói này rồi!");
                sendState(player, SUB_PACK, listCmd);
                return;
            }
            claimKind = PlayerGiftDAO.KIND_DAILY_PACK;
            cost = pack.vndCost;
            rewards = pack.rewards;
            label = pack.label;
        } else {
            VipPack pack = snap.vipPack(id);
            if (pack == null) {
                notifyPlayer(player, "Gói VIP không tồn tại.");
                sendState(player, SUB_PACK, listCmd);
                return;
            }
            if (state.isClaimed(PlayerGiftDAO.KIND_VIP_PACK, id)) {
                notifyPlayer(player, "Bạn đã sở hữu gói này!");
                sendState(player, SUB_PACK, listCmd);
                return;
            }

            if (pack.requiredVipLevel > 0
                    && !state.isClaimed(PlayerGiftDAO.KIND_VIP_PACK, pack.requiredVipLevel)) {
                notifyPlayer(player, "Cần mở gói cấp " + pack.requiredVipLevel + " trước!");
                sendState(player, SUB_PACK, listCmd);
                return;
            }
            claimKind = PlayerGiftDAO.KIND_VIP_PACK;
            cost = pack.vndCost;
            rewards = pack.rewards;
            label = pack.label;
        }

        if (!hasBagRoom(player, rewards)) {
            notifyBagFull(player);
            sendState(player, SUB_PACK, listCmd);
            return;
        }
        if (player.getSession() == null) {
            sendState(player, SUB_PACK, listCmd);
            return;
        }

        if (!state.markClaimed(claimKind, id)) {
            sendState(player, SUB_PACK, listCmd);
            return; // replayed packet
        }
        if (cost > 0 && !DonatePackageDAO.chargeVnd(player, cost)) {
            state.unmarkClaimed(claimKind, id);
            notifyPlayer(player, "Số dư của bạn không đủ để mua " + (label == null || label.isEmpty() ? "gói này" : label) + "!");
            sendState(player, SUB_PACK, listCmd);
            return;
        }
        List<Reward> granted = grant(player, rewards);
        PlayerGiftDAO.save(player.id);
        GiftSystemService.gI().sendRewardPopup(player, granted);
        sendState(player, SUB_PACK, listCmd);
    }

    private void handleMail(Player player, int cmd, Message msg) throws Exception {
        if (cmd != 1 && cmd != 2 && cmd != 4) {
            sendState(player, SUB_MAIL, 0);
            return;
        }
        int mailKind = readU8(msg);
        int mailId = readI32(msg);

        GiftState state = PlayerGiftDAO.load(player.id);
        Mail mail = GiftDataDAO.findMail(mailKind, mailId, player.id);
        if (mail != null && state.isMailDeleted(mailKind, mailId)) {
            mail = null; // soft-deleted for this player
        }

        switch (cmd) {
            case 1: {
                if (mail == null) {
                    notifyPlayer(player, "Thư không tồn tại hoặc đã bị xóa.");
                    sendState(player, SUB_MAIL, 0);
                    return;
                }
                if (state.setMailFlag(mailKind, mailId, PlayerGiftDAO.MAIL_READ)) {
                    PlayerGiftDAO.save(player.id);
                }
                sendState(player, SUB_MAIL, 0);
                GiftSystemService.gI().sendMailDetail(player, mailKind, mailId, mail);
                return;
            }
            case 2: {
                if (mail == null) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    notifyPlayer(player, "Thư không tồn tại hoặc đã bị xóa.");
                    return;
                }
                if (mail.isExpired(System.currentTimeMillis())) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    notifyPlayer(player, "Thư đã hết hạn!");
                    return;
                }
                if (state.isMailClaimed(mailKind, mailId)) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    notifyPlayer(player, "Bạn đã nhận quà của thư này rồi!");
                    return;
                }
                if (mail.rewards == null || mail.rewards.isEmpty()) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    notifyPlayer(player, "Thư này không có vật phẩm đính kèm.");
                    return;
                }
                if (!hasBagRoom(player, mail.rewards)) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    notifyBagFull(player);
                    return;
                }
                if (!state.setMailFlag(mailKind, mailId,
                        PlayerGiftDAO.MAIL_CLAIMED | PlayerGiftDAO.MAIL_READ)) {
                    GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, false, null);
                    return; // replayed packet: flags were already exactly these bits
                }
                List<Reward> granted = grant(player, mail.rewards);
                PlayerGiftDAO.save(player.id);
                GiftSystemService.gI().sendMailClaim(player, mailKind, mailId, true, granted);
                sendState(player, SUB_MAIL, 0);
                return;
            }
            default: { // cmd 4
                if (mailKind != 0) {
                    notifyPlayer(player, "Không thể xóa thư toàn server.");
                    sendState(player, SUB_MAIL, 0);
                    return;
                }
                if (mail == null) {
                    sendState(player, SUB_MAIL, 0);
                    return;
                }
                if (GiftDataDAO.deletePersonalMail(mailId, player.id)) {
                    state.forgetMail(mailKind, mailId);
                } else {
                    state.setMailFlag(mailKind, mailId, PlayerGiftDAO.MAIL_DELETED);
                }
                PlayerGiftDAO.save(player.id);
                sendState(player, SUB_MAIL, 0);
                return;
            }
        }
    }

    private void handleAchievement(Player player, int cmd, Message msg) throws Exception {
        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);

        if (cmd == 1) {
            int group = readU8(msg);
            int id = readI32(msg);
            Achievement row = snap.achievement(group, id);
            if (row == null) {
                notifyPlayer(player, "Thành tựu không tồn tại.");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            if (row.group == 1 && !state.isPrivilegeUnlocked()) {
                notifyPlayer(player, "Bạn chưa mở khóa phần thưởng đặc quyền!");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            if (state.isClaimed(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key())) {
                notifyPlayer(player, "Bạn đã nhận thành tựu này rồi!");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            if (!GiftRules.isAchievementReached(state, row)) {
                notifyPlayer(player, "Bạn chưa hoàn thành thành tựu này.");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            if (!hasBagRoom(player, row.rewards)) {
                notifyBagFull(player);
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            if (!state.markClaimed(PlayerGiftDAO.KIND_ACHIEVEMENT, row.key())) {
                sendState(player, SUB_ACHIEVEMENT, 0);
                return; // replayed packet
            }
            List<Reward> granted = grant(player, row.rewards);
            PlayerGiftDAO.save(player.id);
            GiftSystemService.gI().sendAchievementClaim(player, granted); // FLAT
            sendState(player, SUB_ACHIEVEMENT, 0);
            return;
        }

        if (cmd == 2) {
            if (state.isPrivilegeUnlocked()) {
                notifyPlayer(player, "Bạn đã mở khóa phần thưởng đặc quyền rồi!");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            int price = snap.achievementUnlockPrice();
            if (price > 0 && player.getSession() == null) {
                notifyPlayer(player, "Số dư của bạn không đủ để mở khóa đặc quyền!");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }

            if (!state.setPrivilegeUnlocked(true)) {
                sendState(player, SUB_ACHIEVEMENT, 0);
                return; // replayed packet
            }
            if (price > 0 && !DonatePackageDAO.chargeVnd(player, price)) {
                state.setPrivilegeUnlocked(false);
                notifyPlayer(player, "Số dư của bạn không đủ để mở khóa đặc quyền!");
                sendState(player, SUB_ACHIEVEMENT, 0);
                return;
            }
            PlayerGiftDAO.save(player.id);
            notifyPlayer(player, "Mở khóa phần thưởng đặc quyền thành công!");          
            sendState(player, SUB_ACHIEVEMENT, 0);
            return;
        }

        sendState(player, SUB_ACHIEVEMENT, 0);
    }

    private void handleQuest(Player player, int cmd, Message msg) throws Exception {
        if (cmd != 1) {
            sendState(player, SUB_QUEST, 0);
            return;
        }
        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);

        Quest quest = GiftRules.activeQuest(snap, state);
        if (quest == null) {
            notifyPlayer(player, "Bạn không có nhiệm vụ nào để nhận.");
            sendState(player, SUB_QUEST, 0);
            return;
        }
        if (state.isClaimed(PlayerGiftDAO.KIND_QUEST, quest.id)) {
            notifyPlayer(player, "Bạn đã nhận thưởng nhiệm vụ này rồi!");
            sendState(player, SUB_QUEST, 0);
            return;
        }
        if (!GiftRules.isQuestClaimable(state, quest)) {
            notifyPlayer(player, "Bạn chưa hoàn thành nhiệm vụ này.");
            sendState(player, SUB_QUEST, 0);
            return;
        }
        if (!hasBagRoom(player, quest.rewards)) {
            notifyBagFull(player);
            sendState(player, SUB_QUEST, 0);
            return;
        }
        if (!state.markClaimed(PlayerGiftDAO.KIND_QUEST, quest.id)) {
            sendState(player, SUB_QUEST, 0);
            return; // replayed packet
        }
        List<Reward> granted = grant(player, quest.rewards);
        Quest next = snap.questByStep(quest.step + 1);
        if (next != null) {
            state.setQuest(next.id, next.step, false);
        } else {
            state.setQuest(quest.id, quest.step, true);
        }
        PlayerGiftDAO.save(player.id);
        GiftSystemService.gI().sendQuestClaim(player, granted); // FLAT
        sendState(player, SUB_QUEST, 0);
    }

    private void handleChargePromo(Player player, int cmd, Message msg) throws Exception {
        if (cmd != 1) {
            sendState(player, SUB_CHARGE_PROMO, 0);
            return;
        }
        long amount = readI64(msg);

        Snapshot snap = GiftDataDAO.snapshot();
        GiftState state = PlayerGiftDAO.load(player.id);
        ChargeMilestone milestone = snap.chargeMilestone(amount);
        if (milestone == null) {
            notifyPlayer(player, "Mốc nạp không tồn tại.");
            sendState(player, SUB_CHARGE_PROMO, 0);
            return;
        }
        if (state.isClaimed(PlayerGiftDAO.KIND_CHARGE, amount)) {
            notifyPlayer(player, "Bạn đã nhận mốc nạp này rồi!");
            sendState(player, SUB_CHARGE_PROMO, 0);
            return;
        }
        if (state.getTotalCharged() < amount) {
            notifyPlayer(player, "Bạn chưa nạp đủ để nhận mốc này.");
            sendState(player, SUB_CHARGE_PROMO, 0);
            return;
        }
        if (!hasBagRoom(player, milestone.rewards)) {
            notifyBagFull(player);
            sendState(player, SUB_CHARGE_PROMO, 0);
            return;
        }
        if (!state.markClaimed(PlayerGiftDAO.KIND_CHARGE, amount)) {
            sendState(player, SUB_CHARGE_PROMO, 0);
            return; // replayed packet
        }
        List<Reward> granted = grant(player, milestone.rewards);
        PlayerGiftDAO.save(player.id);
        GiftSystemService.gI().sendChargePromoClaim(player, granted);
        sendState(player, SUB_CHARGE_PROMO, 0);
    }

    private boolean hasBagRoom(Player player, List<Reward> rewards) {
        if (rewards == null || rewards.isEmpty()) {
            return true;
        }
        int need = 0;
        for (Reward reward : rewards) {
            if (reward == null || reward.amount <= 0) {
                continue;
            }
            Template.ItemTemplate template = ItemService.gI().getTemplate(reward.itemId);
            if (template == null) {
                continue; 
            }
            if (template.type == TYPE_GOLD || template.type == TYPE_GEM || template.type == TYPE_RUBY) {
                continue;
            }
            need++;
        }
        return need <= InventoryService.gI().getCountEmptyBag(player);
    }

    private List<Reward> grant(Player player, List<Reward> rewards) {
        List<Reward> granted = new ArrayList<>();
        if (rewards == null || rewards.isEmpty()) {
            return granted;
        }
        for (Reward reward : rewards) {
            if (reward == null || reward.amount <= 0) {
                continue;
            }
            try {
                Template.ItemTemplate template = ItemService.gI().getTemplate(reward.itemId);
                if (template == null) {
                    Logger.error("gift99: itemId " + reward.itemId + " khong co template, bo qua\n");
                    continue;
                }
                Item item = ItemService.gI().createNewItem((short) reward.itemId, reward.amount);
                if (item == null || item.template == null) {
                    continue;
                }
                applyOptions(item, reward);
                if (InventoryService.gI().addItemBag(player, item)) {
                    granted.add(reward);
                } else {
                    notifyPlayer(player, "Không nhận được vật phẩm, hành trang đã đầy.");
                    break;
                }
            } catch (Exception e) {
                Logger.logException(GiftClaimService.class, e,
                        "gift99 grant itemId=" + reward.itemId + " x" + reward.amount);
            }
        }
        if (!granted.isEmpty()) {
            InventoryService.gI().sendItemBags(player);
        }
        return granted;
    }

    private void applyOptions(Item item, Reward reward) {
        if (reward.options == null || reward.options.isEmpty()) {
            return;
        }
        boolean added = false;
        for (int[] option : reward.options) {
            if (option == null || option.length < 2) {
                continue;
            }
            try {
                Template.ItemOptionTemplate optionTemplate =
                        ItemService.gI().getItemOptionTemplate(option[0]);
                if (optionTemplate != null) {
                    item.itemOptions.add(new Item.ItemOption(optionTemplate, option[1]));
                    added = true;
                }
            } catch (Exception ignored) {

            }
        }
        if (added) {
            item.info = item.getInfo();
        }
    }

    private static List<Reward> one(Reward reward) {
        List<Reward> list = new ArrayList<>(1);
        list.add(reward);
        return list;
    }

    public void notifyPlayer(Player player, String text) {
        GiftSystemService.gI().sendToast(player, text);
    }

    private void notifyBagFull(Player player) {
        notifyPlayer(player, "Hành trang không đủ chỗ trống, hãy dọn bớt rồi nhận lại!");
    }

    public void sendState(Player player, int sub, int stateCmd) {
        if (player == null) {
            return;
        }
        if (Boolean.TRUE.equals(EMITTING_STATE.get())) {
            return; 
        }
        EMITTING_STATE.set(Boolean.TRUE);
        try {
            GiftSystemService wire = GiftSystemService.gI();
            switch (sub) {
                case SUB_CHECKIN:
                    wire.sendCheckInState(player);
                    break;
                case SUB_LEVEL_GIFT:
                case SUB_ONLINE_GIFT:
                    wire.sendMilestoneState(player, sub);
                    break;
                case SUB_GIFTCODE:
                    wire.sendGiftCodeList(player);
                    break;
                case SUB_PACK:
                    if (stateCmd == 2) {
                        wire.sendVipPacks(player);
                    } else {
                        wire.sendDailyPacks(player);
                    }
                    break;
                case SUB_MAIL:
                    wire.sendMailList(player);
                    break;
                case SUB_ACHIEVEMENT:
                    wire.sendAchievementState(player);
                    break;
                case SUB_QUEST:
                    wire.sendQuestState(player);
                    break;
                case SUB_CHARGE_PROMO:
                    wire.sendChargePromoState(player);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            Logger.logException(GiftClaimService.class, e,
                    "gift99 sendState sub=" + sub + " cmd=" + stateCmd);
        } finally {
            EMITTING_STATE.set(Boolean.FALSE);
        }
    }

    private int readU8(Message msg) throws Exception {
        return msg.reader().available() >= 1 ? msg.reader().readUnsignedByte() : 0;
    }

    private int readI32(Message msg) throws Exception {
        return msg.reader().available() >= 4 ? msg.reader().readInt() : 0;
    }

    private long readI64(Message msg) throws Exception {
        return msg.reader().available() >= 8 ? msg.reader().readLong() : 0L;
    }

    private String readUTF(Message msg) throws Exception {
        return msg.reader().available() >= 2 ? msg.reader().readUTF() : "";
    }
}
