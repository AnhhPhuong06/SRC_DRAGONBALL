package models.npc.npc_list;

import java.util.ArrayList;
import java.util.List;
import database.daos.DonatePackageDAO;

import consts.ConstTask;
import consts.ConstNpc;
import database.daos.PlayerDAO;
import models.item.Item;
import models.npc.Npc;
import models.player.Player;
import models.task.TaskMain;
import server.Manager;
import services.PetService;
import services.player.InventoryService;
import utils.Util;
import services.ItemService;
import services.Service;
import services.TaskService;
import services.func.Input;

public class OngGohan extends Npc {

    public OngGohan(int mapId, int status, int cx, int cy, int tempId, int avartar) {
        super(mapId, status, cx, cy, tempId, avartar);
    }

    @Override
    public void openBaseMenu(Player player) {
        if (canOpenNpc(player)) {
            if (!TaskService.gI().checkDoneTaskTalkNpc(player, this)) {
                this.createOtherMenu(player, ConstNpc.BASE_MENU,
                        "|0|Chào con , Đến với thế giới Ngọc Rồng Online\n|5|Tải game và nạp thẻ tự động tại : nroha.fun",
                        "Kích hoạt\nThành viên", "Nhập\nGiftcode", "Quy đổi\nVNĐ", "Đổi Mật\nKhẩu", "Hỗ Trợ\nNhiệm Vụ", "Nhận\nĐệ Tử", "Ngoại Hình\nPorata");

            }
        }
    }

    @Override
    public void confirmMenu(Player player, int select) {
        if (canOpenNpc(player)) {
            if (player.idMark.isBaseMenu()) {
                switch (select) {
                    case 3 ->
                        Input.gI().createFormChangePassword(player);
                    case 1 -> {
                        Input.gI().createFormGiftCode(player);
                    }
                    case 2 -> openExchangeMenu(player);
                    case 0 ->
                        this.createOtherMenu(player, ConstNpc.MENU_MTV,
                                "Mở thành viên:\n"
                                + "|7|Free: khi hoàn thành nhiệm vụ Fide\n",
                                 "Mở Free\nXong Fide");
                    case 4 -> {
                        StringBuilder text = new StringBuilder();
                        for (short i = 0; i < Manager.supportTaskId.length; i++) {
                            TaskMain task = TaskService.gI().getTaskMain(Manager.supportTaskId[i][0]);
                            text.append(task.name).append(" (").append(task.subTasks.get(Manager.supportTaskId[i][1]).name).append(")\n");
                        }
                        this.createOtherMenu(player, ConstNpc.MENU_SUPPORT_TASK,
                                "|7|Nhiệm vụ hiện tại: " + player.playerTask.taskMain.subTasks.get(player.playerTask.taskMain.index).name
                                + "\n|-1|Danh sách nhiệm vụ được hỗ trợ\n|3|" + text,
                                "Nhận\nHỗ Trợ", "Đóng");
                    }
                    case 5 -> {
                        if (player.pet != null) {
                            this.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                                    "|0|Con đã có đệ tử đi theo rồi, hãy chăm sóc và rèn luyện cho nó thật tốt nhé!",
                                    "Đóng");
                            return;
                        }
                        this.createOtherMenu(player, ConstNpc.MENU_NHAN_DE_TU,
                                "|0|Chào con, ta sẽ trao cho con một đứa đệ tử trung thành để cùng con tu luyện!\n"
                                + "|7|Hãy chọn hành tinh cho đệ tử của con:",
                                "Trái Đất", "Namếc", "Xayda", "Ngẫu Nhiên", "Từ Chối");
                    }
                    case 6 -> {
                        String status = player.isOffPorataOutfit ? "|7|Đang TẮT (Hiện cải trang / đồ thường)" : "|2|Đang BẬT (Hiện đồ Porata)";
                        this.createOtherMenu(player, ConstNpc.MENU_BAT_TAT_PORATA,
                                "|0|Chào con, con có muốn bật hoặc tắt ngoại hình khi hợp thể bông tai Porata không?\n"
                                + "|1|Trạng thái hiện tại: " + status,
                                "Bật Ngoại\nHình Porata", "Tắt Ngoại\nHình Porata", "Đóng");
                    }

                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_NHAN_DE_TU) {
                if (player.pet != null) {
                    Service.gI().sendThongBao(player, "Bạn đã có đệ tử rồi!");
                    return;
                }
                switch (select) {
                    case 0 -> { // Trái Đất
                        PetService.gI().createNormalPet(player, 0);
                        Service.gI().sendThongBao(player, "Nhận đệ tử Trái Đất thành công!");
                    }
                    case 1 -> { // Namếc
                        PetService.gI().createNormalPet(player, 1);
                        Service.gI().sendThongBao(player, "Nhận đệ tử Namếc thành công!");
                    }
                    case 2 -> { // Xayda
                        PetService.gI().createNormalPet(player, 2);
                        Service.gI().sendThongBao(player, "Nhận đệ tử Xayda thành công!");
                    }
                    case 3 -> { // Ngẫu Nhiên
                        PetService.gI().createNormalPet(player);
                        Service.gI().sendThongBao(player, "Nhận đệ tử thành công!");
                    }
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_SUPPORT_TASK) {
                if (select == 0) {
                    TaskService.gI().supportTask(player);
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPEN_VND) {
                if (select >= 0 && select < ExchangeReward.values().length) {
                    openExchangeTiers(player, ExchangeReward.values()[select]);
                } else {
                    player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_MTV) {
                if (player.getSession() == null) {
                    return;
                }
                if (player.getSession().actived) {
                    Service.gI().sendThongBao(player, "Bạn đã mở thành viên rồi");
                    return;
                }
                if (TaskService.gI().getIdTask(player) < ConstTask.TASK_24_0) {
                    Service.gI().sendThongBao(player, "Cần hoàn thành nhiệm vụ Fide mới có thể mở thành viên");
                    return;
                }
                switch (select) {
                    // case 0 -> { // Mở bằng 10K VNĐ
                    //     if (player.getSession().sotien < 10000) {
                    //         Service.gI().sendThongBao(player, "Không đủ 10K");
                    //     } else if (PlayerDAO.subVND2(player, 10000)) {
                    //         Item itemqua = ItemService.gI().createNewItem((short) 77, 500000);
                    //         InventoryService.gI().addItemBag(player, itemqua);
                    //         Service.gI().sendThongBao(player, "Bạn Đã Nhận Được " + itemqua.template.name + "Thoát ra vào lại để xem nhé");
                    //         Service.gI().sendThongBao(player, "Đã mở thành viên thành công");
                    //     } else {
                    //         this.npcChat(player, "Lỗi vui lòng báo admin...");
                    //     }
                    // }
                    // case 1 -> { // Mở bằng 50K VNĐ: x3 Ngọc Rồng 3 sao (16) + x150 Thỏi Vàng (457)
                    //     if (player.getSession().sotien < 50000) {
                    //         Service.gI().sendThongBao(player, "Không đủ 50K");
                    //     } else if (InventoryService.gI().getCountEmptyBag(player) < 2) {
                    //         Service.gI().sendThongBao(player, "Cần ít nhất 2 ô trống hành trang");
                    //     } else if (PlayerDAO.subVND2(player, 50000)) {
                    //         Item ngoc = ItemService.gI().createNewItem((short) 16, 3);
                    //         ngoc.itemOptions.add(new Item.ItemOption(30, 0));
                    //         InventoryService.gI().addItemBag(player, ngoc);
                    //         Item thoiVang = ItemService.gI().createNewItem((short) 457);
                    //         thoiVang.quantity = 150;
                    //         thoiVang.itemOptions.add(new Item.ItemOption(100, 1));
                    //         thoiVang.itemOptions.add(new Item.ItemOption(86, 0));
                    //         thoiVang.itemOptions.add(new Item.ItemOption(30, 0));
                    //         InventoryService.gI().addItemBag(player, thoiVang);
                    //         InventoryService.gI().sendItemBags(player);
                    //         Service.gI().sendThongBao(player, "Đã mở thành viên (50K)! Nhận x3 " + ngoc.template.name + " và x150 Thỏi Vàng");
                    //     } else {
                    //         this.npcChat(player, "Lỗi vui lòng báo admin...");
                    //     }
                    // }
                    case 0 -> { // Mở miễn phí khi hoàn thành nhiệm vụ Fide
                        if (TaskService.gI().getIdTask(player) < ConstTask.TASK_24_0) {
                            Service.gI().sendThongBao(player, "Cần hoàn thành nhiệm vụ Fide mới có thể mở thành viên");
                        } else if (PlayerDAO.subVND2(player, 0)) {
                            Service.gI().sendThongBao(player, "Đã mở thành viên miễn phí (hoàn thành Fide) thành công");
                        } else {
                            this.npcChat(player, "Lỗi vui lòng báo admin...");
                        }
                    }
                }
            // } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_OPEN_HONG_NGOC) {
            //     if (InventoryService.gI().getCountEmptyBag(player) < 2) {
            //         Service.gI().sendThongBaoOK(player, "Cần ít nhất 2 ô trống hành trang!");
            //         return;
            //     }
            //     switch (select) {
            //         case 0, 1, 2, 3, 4, 5 -> {
            //             _HandlerChangeRuby(player, select);
            //         }
            //     }
            } else if (exchangeRewardForMenu(player.idMark.getIndexMenu()) != null) {
                synchronized (player.inventory.itemsBag) {
                    ExchangeReward reward = exchangeRewardForMenu(player.idMark.getIndexMenu());
                    if (reward == null) return;
                    // Consume the menu before billing, so a replay cannot buy a second pack.
                    player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
                    if (select >= 0 && select < EXCHANGE_VND.length) exchangeVnd(player, reward, select);
                }
            } else if (player.idMark.getIndexMenu() == ConstNpc.MENU_BAT_TAT_PORATA) {
                switch (select) {
                    case 0 -> { // Bật
                        player.isOffPorataOutfit = false;
                        Service.gI().Send_Caitrang(player);
                        Service.gI().sendThongBao(player, "Đã BẬT ngoại hình hợp thể Porata thành công!");
                    }
                    case 1 -> { // Tắt
                        player.isOffPorataOutfit = true;
                        Service.gI().Send_Caitrang(player);
                        Service.gI().sendThongBao(player, "Đã TẮT ngoại hình hợp thể Porata thành công!\n(Khi hợp thể sẽ hiển thị cải trang hoặc trang bị bình thường)");
                    }
                }
            }
        }
    }

    private static final int[] EXCHANGE_VND = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, 1_000_000};

    private enum ExchangeReward {
        GOLD(457, 2000, "Thỏi vàng", ConstNpc.MENU_THOI_VANG),
        EGG(568, 100, "Quả trứng", ConstNpc.MENU_GOHAN_DOI_TRUNG),
        STONE(674, 200, "Đá ngũ sắc", ConstNpc.MENU_GOHAN_DOI_DA_NGU_SAC);

        final short itemId;
        final int per10k;
        final String label;
        final int menu;

        ExchangeReward(int itemId, int per10k, String label, int menu) {
            this.itemId = (short) itemId;
            this.per10k = per10k;
            this.label = label;
            this.menu = menu;
        }

        int quantity(int price) {
            return price / 10_000 * per10k;
        }
    }

    private static ExchangeReward exchangeRewardForMenu(int menu) {
        for (ExchangeReward reward : ExchangeReward.values()) if (reward.menu == menu) return reward;
        return null;
    }

    private void openExchangeMenu(Player player) {
        if (player.getSession() == null) return;
        createOtherMenu(player, ConstNpc.MENU_OPEN_VND,
                "|7|Số dư: " + Util.format(player.getSession().sotien) + " VNĐ\n"
                + "|1|10.000 VNĐ = 2.000 thỏi vàng hoặc 100 quả trứng hoặc 200 đá ngũ sắc\n"
                + "Chọn vật phẩm muốn quy đổi:",
                "Thỏi\nvàng", "Quả trứng\n(ID 568)", "Đá\nngũ sắc", "Đóng");
    }

    private void openExchangeTiers(Player player, ExchangeReward reward) {
        if (player.getSession() == null) return;
        String[] buttons = new String[EXCHANGE_VND.length + 1];
        for (int i = 0; i < EXCHANGE_VND.length; i++) {
            int price = EXCHANGE_VND[i];
            buttons[i] = Util.format(price) + " VNĐ\n" + Util.format(reward.quantity(price)) + "\n" + reward.label;
        }
        buttons[EXCHANGE_VND.length] = "Đóng";
        createOtherMenu(player, reward.menu,
                "|7|Số dư: " + Util.format(player.getSession().sotien) + " VNĐ\n"
                + "|1|10.000 VNĐ = " + Util.format(reward.per10k) + " " + reward.label
                + "\nChọn mức quy đổi từ 10.000 đến 1.000.000 VNĐ:", buttons);
    }

    protected boolean chargeExchange(Player player, int amount) {
        // Check the actual DB balance in the UPDATE, rather than trusting the session cache alone.
        return DonatePackageDAO.chargeVnd(player, amount);
    }

    private void exchangeVnd(Player player, ExchangeReward reward, int tier) {
        if (player.getSession() == null) return;
        int price = EXCHANGE_VND[tier];
        int quantity = reward.quantity(price);
        synchronized (player.inventory.itemsBag) {
            if (player.getSession().sotien < price) {
                Service.gI().sendThongBao(player, "Không đủ số dư. Cần " + Util.format(price) + " VNĐ để quy đổi.");
                return;
            }
            Item item = ItemService.gI().createNewItem(reward.itemId, quantity);
            if (!item.isNotNullItem()) {
                Service.gI().sendThongBao(player, "Vật phẩm quy đổi chưa được cấu hình, vui lòng báo quản trị viên.");
                return;
            }
            if (reward == ExchangeReward.GOLD) {
                item.addOptionParam(100, 1);
                item.addOptionParam(86, 0);
            }
            // Run the existing stacking logic on a copy before taking money. A failed insert can
            // partially fill stacks, so never use the live bag for this capacity check.
            List<Item> staged = new ArrayList<>(player.inventory.itemsBag.size());
            for (Item current : player.inventory.itemsBag) staged.add(ItemService.gI().copyItem(current));
            if (!InventoryService.gI().addItemList(staged, item)) {
                Service.gI().sendThongBao(player, "Hành trang không đủ chỗ để nhận toàn bộ vật phẩm quy đổi.");
                return;
            }
            if (!chargeExchange(player, price)) {
                Service.gI().sendThongBao(player, "Không thể trừ VNĐ. Hãy kiểm tra số dư hoặc thử lại sau.");
                return;
            }
            // Apply the validated result without another fallible inventory insert. Preserve object
            // identity for existing stacks, which may still be referenced by another game panel.
            for (int slot = 0; slot < staged.size(); slot++) {
                Item current = player.inventory.itemsBag.get(slot);
                Item updated = staged.get(slot);
                if (current.isNotNullItem()) {
                    if (current.quantity != updated.quantity) {
                        current.quantity = updated.quantity;
                        current.itemOptions = updated.itemOptions;
                    }
                } else if (updated.isNotNullItem()) {
                    player.inventory.itemsBag.set(slot, updated);
                }
            }
            if (player.playerTask != null && player.playerTask.taskdh != null) player.playerTask.taskdh.Nap += price;
            Service.gI().sendMoney(player);
            InventoryService.gI().sendItemBags(player);
            createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "|2|Quy đổi thành công " + Util.format(price) + " VNĐ thành " + Util.format(quantity) + " " + reward.label
                    + "\n|7|Số dư còn lại: " + Util.format(player.getSession().sotien) + " VNĐ", "Đóng");
        }
    }

}
