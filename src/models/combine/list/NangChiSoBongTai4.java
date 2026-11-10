package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import services.CombineService;
import models.player.Player;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class NangChiSoBongTai4 {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 3) {
            Item bongTai4 = null;
            Item hon = null;
            Item da = null;
            for (Item item : player.combineNew.itemsCombine) {
                switch (item.template.id) {
                    case 1624 -> bongTai4 = item;
                    case 1623 -> hon = item;
                    case 935 -> da = item;
                    default -> {
                    }
                }
            }
            if (bongTai4 != null && hon != null && da != null) {
                player.combineNew.gemCombine = 500;
                player.combineNew.ratioCombine = 100;

                String npcSay = "|2|Bông tai Porata [+4]\n\n"
                        + "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n"
                        + ((hon.quantity >= 50) ? "|2|" : "|7|") + "Cần 199 " + hon.template.name + "\n"
                        + ((da.quantity >= 1) ? "|2|" : "|7|") + "Cần 1 " + da.template.name + "\n"
                        + ((player.inventory.getGemAndRuby() >= 500) ? "|2|" : "|7|") + "Cần: 500 ngọc\n"
                        + "|1|+1 Chỉ số ngẫu nhiên";
                if (da.quantity < 1) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (1 - da.quantity) + " " + da.template.name, "Đóng");
                } else if (hon.quantity < 199) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (199 - hon.quantity) + " " + hon.template.name, "Đóng");
                } else if (player.inventory.getGemAndRuby() < 500) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (500 - player.inventory.getGemAndRuby()) + " ngọc", "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n500 ngọc", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Bông tai Porata cấp 4, X199 Mảnh hồn bông tai và 1 Đá xanh lam", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Bông tai Porata cấp 4, X199 Mảnh hồn bông tai và 1 Đá xanh lam", "Đóng");
        }
    }

    public static void nangChiSoBongTai4(Player player) {
        if (player.combineNew.itemsCombine.size() == 3) {
            Item bongTai4 = null;
            Item hon = null;
            Item da = null;
            for (Item item : player.combineNew.itemsCombine) {
                switch (item.template.id) {
                    case 1624 -> bongTai4 = item;
                    case 1623 -> hon = item;
                    case 935 -> da = item;
                    default -> {
                    }
                }
            }
            if (bongTai4 != null && hon != null && da != null) {
                if (hon.quantity >= 199 && da.quantity >= 1 && player.inventory.getGemAndRuby() >= 500) {
                    if (player.inventory.gem >= 500) {
                        player.inventory.gem -= 500;
                    } else {
                        int rubyNeed = 500 - player.inventory.gem;
                        player.inventory.gem = 0;
                        player.inventory.ruby -= rubyNeed;
                    }

            if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    // Xóa toàn bộ option cũ, bao gồm cả option gốc
                    bongTai4.itemOptions.clear();

                    int[] listId = {77, 80, 81, 103, 50, 94, 5, 14};

                    // Random 2 dòng độc lập, cho phép trùng option
                    for (int i = 0; i < 2; i++) {
                        int idOption = listId[Util.nextInt(listId.length)];
                        int param = Util.nextInt(1, 25);

                        bongTai4.itemOptions.add(new Item.ItemOption(idOption, param));
                    }

                    // Giữ option đánh dấu Bông tai cấp 3
                    bongTai4.itemOptions.add(new Item.ItemOption(38, 0));
                    bongTai4.itemOptions.add(new Item.ItemOption(72, 4));

                    bongTai4.info = bongTai4.getInfo();
                    bongTai4.content = bongTai4.getContent();

                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }

                    InventoryService.gI().subQuantityItemsBag(player, hon, 199);
                    InventoryService.gI().subQuantityItemsBag(player, da, 1);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendMoney(player);
                    CombineService.gI().reOpenItemCombine(player);
                }
            }
        }
    }
}
