package services.func;

import consts.ConstItem;
import consts.ConstNpc;
import database.daos.PlayerDAO;
import java.util.ArrayList;
import java.util.List;
import models.Template.ItemTemplate;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import services.VongQuayThoiVangConfig;
import utils.Util;

/**
 * Hệ thống mốc vòng quay Thượng Đế.
 * Tích lũy số lượt quay (Player.lixi) để nhận các mốc phần thưởng giá trị.
 * Trạng thái nhận thưởng được lưu bằng bitmask trong Player.mocVongQuay và cột dien_sukien.
 */
public class MocVongQuayService {

    private static MocVongQuayService instance;

    public static MocVongQuayService gI() {
        if (instance == null) {
            instance = new MocVongQuayService();
        }
        return instance;
    }

    public static class RewardItem {
        public int tempId;
        public int quantity;
        public int[][] options;

        public RewardItem(int tempId, int quantity) {
            this.tempId = tempId;
            this.quantity = quantity;
            this.options = null;
        }

        public RewardItem(int tempId, int quantity, int[][] options) {
            this.tempId = tempId;
            this.quantity = quantity;
            this.options = options;
        }
    }

    public static class MocVongQuay {
        public int id;
        public int spinsRequired;
        public String name;
        public List<RewardItem> rewards;

        public MocVongQuay(int id, int spinsRequired, String name, List<RewardItem> rewards) {
            this.id = id;
            this.spinsRequired = spinsRequired;
            this.name = name;
            this.rewards = rewards;
        }
    }

    public static volatile List<MocVongQuay> MOCS = List.of();

    static {
        reloadMocFromConfig();
    }

    public static synchronized void reloadMocFromConfig() {
        List<MocVongQuay> staged = new ArrayList<>();

        List<RewardItem> r1 = VongQuayThoiVangConfig.parseRewards(VongQuayThoiVangConfig.moc1RewardsStr);
        if (r1.isEmpty()) {
            r1 = new ArrayList<>(List.of(
                    new RewardItem(ConstItem.THOI_VANG, VongQuayThoiVangConfig.moc1ThoiVang),
                    new RewardItem(VongQuayThoiVangConfig.moc1ItemId, VongQuayThoiVangConfig.moc1ItemQuantity)
            ));
        }
        staged.add(new MocVongQuay(0, VongQuayThoiVangConfig.moc1Spins, "Mốc " + Util.numberToMoney(VongQuayThoiVangConfig.moc1Spins) + " Lượt", r1));

        List<RewardItem> r2 = VongQuayThoiVangConfig.parseRewards(VongQuayThoiVangConfig.moc2RewardsStr);
        if (r2.isEmpty()) {
            r2 = new ArrayList<>(List.of(
                    new RewardItem(ConstItem.THOI_VANG, VongQuayThoiVangConfig.moc2ThoiVang),
                    new RewardItem(VongQuayThoiVangConfig.moc2ItemId, VongQuayThoiVangConfig.moc2ItemQuantity, new int[][]{{30, 0}})
            ));
        }
        staged.add(new MocVongQuay(1, VongQuayThoiVangConfig.moc2Spins, "Mốc " + Util.numberToMoney(VongQuayThoiVangConfig.moc2Spins) + " Lượt", r2));

        List<RewardItem> r3 = VongQuayThoiVangConfig.parseRewards(VongQuayThoiVangConfig.moc3RewardsStr);
        if (r3.isEmpty()) {
            r3 = new ArrayList<>(List.of(
                    new RewardItem(ConstItem.THOI_VANG, VongQuayThoiVangConfig.moc3ThoiVang),
                    new RewardItem(987, VongQuayThoiVangConfig.moc3DaBaoVe),
                    new RewardItem(1277, VongQuayThoiVangConfig.moc3ManhKhi),
                    new RewardItem(1177, VongQuayThoiVangConfig.moc3RuongMTS),
                    new RewardItem(VongQuayThoiVangConfig.moc3CaiTrangId, 1, new int[][]{
                            {50, 30}, {77, 30}, {103, 30}, {5, 25}, {14, 15}, {95, 12}, {96, 12}, {97, 15}, {30, 0}
                    })
            ));
        }
        staged.add(new MocVongQuay(2, VongQuayThoiVangConfig.moc3Spins, "Mốc " + Util.numberToMoney(VongQuayThoiVangConfig.moc3Spins) + " Lượt", r3));

        List<RewardItem> r4 = VongQuayThoiVangConfig.parseRewards(VongQuayThoiVangConfig.moc4RewardsStr);
        if (r4.isEmpty()) {
            r4 = new ArrayList<>(List.of(
                    new RewardItem(ConstItem.THOI_VANG, VongQuayThoiVangConfig.moc4ThoiVang),
                    new RewardItem(987, VongQuayThoiVangConfig.moc4DaBaoVe),
                    new RewardItem(1277, VongQuayThoiVangConfig.moc4ManhKhi),
                    new RewardItem(1177, VongQuayThoiVangConfig.moc4RuongMTS),
                    new RewardItem(VongQuayThoiVangConfig.moc4CaiTrangId, 1, new int[][]{
                            {50, 35}, {77, 35}, {103, 35}, {5, 35}, {14, 20}, {95, 18}, {96, 18}, {97, 25}, {108, 20}, {106, 0}, {30, 0}
                    })
            ));
        }
        staged.add(new MocVongQuay(3, VongQuayThoiVangConfig.moc4Spins, "Mốc " + Util.numberToMoney(VongQuayThoiVangConfig.moc4Spins) + " Lượt", r4));
        MOCS = List.copyOf(staged);
    }

    public void openMenuMoc(Npc npc, Player player) {
        StringBuilder say = new StringBuilder();
        say.append("|7|MỐC SIÊU CAO VÒNG QUAY THƯỢNG ĐẾ\n")
                .append("|2|Tổng số lượt quay của con: ").append(Util.numberToMoney(player.lixi)).append(" lượt\n");
        for (MocVongQuay m : MOCS) {
            say.append("|1|• ").append(m.name).append(": ");
            List<String> rws = new ArrayList<>();
            for (RewardItem rw : m.rewards) {
                ItemTemplate t = ItemService.gI().getTemplate(rw.tempId);
                String nm = (t != null && t.name != null && !t.name.trim().isEmpty()) ? t.name : VongQuayThoiVangConfig.getItemName(rw.tempId);
                rws.add(Util.numberToMoney(rw.quantity) + " " + nm);
            }
            say.append(String.join(", ", rws)).append("\n");
        }
        say.append("|0|Con hãy chọn mốc muốn xem hoặc nhận:");

        String[] options = new String[MOCS.size() + 2];
        for (int i = 0; i < MOCS.size(); i++) {
            MocVongQuay m = MOCS.get(i);
            boolean daNhan = player.daNhanMocVongQuay(i);
            boolean duDieuKien = player.lixi >= m.spinsRequired;
            String status = daNhan ? "(Đã nhận)" : (duDieuKien ? "(Có quà!)" : "");
            options[i] = "Mốc " + Util.numberToMoney(m.spinsRequired) + "\nlượt" + (status.isEmpty() ? "" : "\n" + status);
        }
        options[MOCS.size()] = "Nhận hết\nquà đủ mốc";
        options[MOCS.size() + 1] = "Đóng";

        npc.createOtherMenu(player, ConstNpc.MENU_MOC_VONG_QUAY, say.toString(), options);
    }

    public void confirmMenuMoc(Npc npc, Player player, int select) {
        if (select >= 0 && select < MOCS.size()) {
            openDetailMoc(npc, player, select);
        } else if (select == MOCS.size()) {
            claimAllEligibleMoc(npc, player);
        }
    }

    public void openDetailMoc(Npc npc, Player player, int index) {
        if (index < 0 || index >= MOCS.size()) {
            return;
        }
        player.idMark.setIndexMocVongQuay(index);
        MocVongQuay moc = MOCS.get(index);
        boolean daNhan = player.daNhanMocVongQuay(index);
        boolean duDieuKien = player.lixi >= moc.spinsRequired;

        StringBuilder text = new StringBuilder();
        text.append("|7|PHẦN THƯỞNG MỐC ").append(Util.numberToMoney(moc.spinsRequired)).append(" LƯỢT QUAY\n");
        text.append("|2|Tổng lượt quay hiện tại: ").append(Util.numberToMoney(player.lixi))
                .append(" / ").append(Util.numberToMoney(moc.spinsRequired)).append(" lượt\n");
        if (daNhan) {
            text.append("|4|Trạng thái: [ĐÃ NHẬN THƯỞNG]\n");
        } else if (duDieuKien) {
            text.append("|2|Trạng thái: [ĐỦ ĐIỀU KIỆN NHẬN THƯỞNG!]\n");
        } else {
            text.append("|1|Trạng thái: [CHƯA ĐẠT - Còn thiếu ")
                    .append(Util.numberToMoney(moc.spinsRequired - player.lixi)).append(" lượt]\n");
        }
        text.append("|1|Danh sách phần thưởng:\n");
        for (RewardItem rw : moc.rewards) {
            ItemTemplate temp = ItemService.gI().getTemplate(rw.tempId);
            String name = (temp != null && temp.name != null && !temp.name.trim().isEmpty())
                    ? temp.name : VongQuayThoiVangConfig.getItemName(rw.tempId);
            boolean isKhoa = false;
            if (rw.options != null) {
                for (int[] opt : rw.options) {
                    if (opt[0] == 30) {
                        isKhoa = true;
                        break;
                    }
                }
            }
            text.append("• x").append(rw.quantity).append(" ").append(name).append(isKhoa ? " (Khóa)" : "").append("\n");
        }

        if (!daNhan && duDieuKien) {
            text.append("|0|Con có muốn nhận thưởng mốc này ngay không?");
            npc.createOtherMenu(player, ConstNpc.CONFIRM_RECEIVE_MOC_VONG_QUAY, text.toString(),
                    "Nhận Quà", "Quay lại", "Đóng");
        } else {
            npc.createOtherMenu(player, ConstNpc.CONFIRM_RECEIVE_MOC_VONG_QUAY, text.toString(),
                    "Quay lại", "Đóng");
        }
    }

    public void confirmDetailMoc(Npc npc, Player player, int select) {
        int index = player.idMark.getIndexMocVongQuay();
        if (index < 0 || index >= MOCS.size()) {
            openMenuMoc(npc, player);
            return;
        }
        MocVongQuay moc = MOCS.get(index);
        boolean daNhan = player.daNhanMocVongQuay(index);
        boolean duDieuKien = player.lixi >= moc.spinsRequired;

        if (!daNhan && duDieuKien) {
            switch (select) {
                case 0 -> claimMoc(npc, player, index);
                case 1 -> openMenuMoc(npc, player);
                default -> {}
            }
        } else {
            switch (select) {
                case 0 -> openMenuMoc(npc, player);
                default -> {}
            }
        }
    }

    public void claimMoc(Npc npc, Player player, int index) {
        if (index < 0 || index >= MOCS.size()) {
            return;
        }
        MocVongQuay moc = MOCS.get(index);
        if (player.lixi < moc.spinsRequired) {
            Service.gI().sendThongBao(player, "Con chưa đạt mốc " + Util.numberToMoney(moc.spinsRequired) + " lượt quay!");
            return;
        }
        if (player.daNhanMocVongQuay(index)) {
            Service.gI().sendThongBao(player, "Con đã nhận phần thưởng mốc này rồi!");
            return;
        }

        int requiredSlots = moc.rewards.size();
        if (InventoryService.gI().getCountEmptyBag(player) < requiredSlots) {
            Service.gI().sendThongBao(player, "Hành trang của con cần tối thiểu " + requiredSlots + " ô trống để nhận quà!");
            return;
        }

        player.setNhanMocVongQuay(index);
        PlayerDAO.updatePlayer(player);

        for (RewardItem rw : moc.rewards) {
            Item it = createRewardItem(rw);
            if (it != null) {
                InventoryService.gI().addItemBag(player, it);
            }
        }
        InventoryService.gI().sendItemBags(player);

        Service.gI().sendThongBao(player, "Chúc mừng con đã nhận thành công quà mốc "
                + Util.numberToMoney(moc.spinsRequired) + " lượt quay!");

        if (moc.spinsRequired >= 1000) {
            Service.gI().sendThongBaoAllPlayer("Chúc mừng " + player.name
                    + " vừa nhận quà mốc " + Util.numberToMoney(moc.spinsRequired)
                    + " lượt Vòng Quay Thượng Đế!");
        }

        openMenuMoc(npc, player);
    }

    public void claimAllEligibleMoc(Npc npc, Player player) {
        List<MocVongQuay> canClaim = new ArrayList<>();
        int totalSlots = 0;
        for (int i = 0; i < MOCS.size(); i++) {
            MocVongQuay m = MOCS.get(i);
            if (player.lixi >= m.spinsRequired && !player.daNhanMocVongQuay(i)) {
                canClaim.add(m);
                totalSlots += m.rewards.size();
            }
        }

        if (canClaim.isEmpty()) {
            Service.gI().sendThongBao(player, "Không có mốc thưởng nào đủ điều kiện nhận!");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < totalSlots) {
            Service.gI().sendThongBao(player, "Hành trang của con cần tối thiểu " + totalSlots
                    + " ô trống để nhận tất cả các mốc!");
            return;
        }

        for (MocVongQuay m : canClaim) {
            player.setNhanMocVongQuay(m.id);
            for (RewardItem rw : m.rewards) {
                Item it = createRewardItem(rw);
                if (it != null) {
                    InventoryService.gI().addItemBag(player, it);
                }
            }
        }
        PlayerDAO.updatePlayer(player);
        InventoryService.gI().sendItemBags(player);

        Service.gI().sendThongBao(player, "Đã nhận thành công " + canClaim.size() + " mốc phần thưởng!");
        openMenuMoc(npc, player);
    }

    private Item createRewardItem(RewardItem rw) {
        if (ItemService.gI().getTemplate(rw.tempId) == null) {
            return ItemService.gI().createNewItem((short) ConstItem.THOI_VANG, rw.quantity);
        }
        Item item = ItemService.gI().createNewItem((short) rw.tempId, rw.quantity);
        if (rw.options != null && rw.options.length > 0) {
            item.itemOptions.clear();
            for (int[] opt : rw.options) {
                item.itemOptions.add(new Item.ItemOption(opt[0], opt[1]));
            }
        }
        return item;
    }
}
