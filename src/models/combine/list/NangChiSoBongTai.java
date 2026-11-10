package models.combine.list;

/*
 * @Zalo : 0372.665.345
 */


import consts.ConstNpc;
import models.item.Item;
import services.CombineService;
import models.player.Player;
import services.player.InventoryService;
import services.Service;
import utils.Util;

public class NangChiSoBongTai {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 3) {
            Item bongTai = null;
            Item honBongTai = null;
            Item daXanhLam = null;
            for (Item item : player.combineNew.itemsCombine) {
                switch (item.template.id) {
                    case 921 ->
                        bongTai = item;
                    case 934 ->
                        honBongTai = item;
                    case 935 ->
                        daXanhLam = item;
                    default -> {
                    }
                }
            }
            if (bongTai != null && honBongTai != null && daXanhLam != null) {
                player.combineNew.gemCombine = 100;
                player.combineNew.ratioCombine = 70;
                String npcSay = "|2|Bông tai Porata [+2]\n\n"
                        + "|2|Tỉ lệ thành công: " + player.combineNew.ratioCombine + "%\n"
                        + ((honBongTai.quantity >= 50) ? "|2|" : "|7|") + "Cần 50 " + honBongTai.template.name + "\n"
                        + ((daXanhLam.quantity >= 1) ? "|2|" : "|7|") + "Cần 1 " + daXanhLam.template.name + "\n"
                        + ((player.inventory.getGemAndRuby() >= 100) ? "|2|" : "|7|") + "Cần: 100 ngọc\n"
                        + "|1|+1 Chỉ số ngẫu nhiên";

                if (daXanhLam.quantity < 1) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (1 - daXanhLam.quantity) + " " + daXanhLam.template.name, "Đóng");
                } else if (honBongTai.quantity < 50) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (50 - honBongTai.quantity) + " " + honBongTai.template.name, "Đóng");
                } else if (player.inventory.getGemAndRuby() < 100) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            npcSay + "\nCòn thiếu " + (100 - player.inventory.getGemAndRuby()) + " ngọc", "Đóng");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng cấp\n100 ngọc/ruby", "Từ chối");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 Bông tai Porata cấp 2, X50 Mảnh hồn bông tai và 1 Đá xanh lam", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 Bông tai Porata cấp 2, X50 Mảnh hồn bông tai và 1 Đá xanh lam", "Đóng");
        }
    }

    public static void nangChiSoBongTai(Player player) {
        if (player.combineNew.itemsCombine.size() == 3) {
            Item bongTai = null;
            Item honBongTai = null;
            Item daXanhLam = null;
            for (Item item : player.combineNew.itemsCombine) {
                switch (item.template.id) {
                    case 921 ->
                        bongTai = item;
                    case 934 ->
                        honBongTai = item;
                    case 935 ->
                        daXanhLam = item;
                    default -> {
                    }
                }
            }
            if (bongTai != null && honBongTai != null && daXanhLam != null) {
                if (honBongTai.quantity >= 50 && daXanhLam.quantity >= 1 && player.inventory.getGemAndRuby() >= 100) {
                    if (player.inventory.gem >= 100) {
                        player.inventory.gem -= 100;
                    } else {
                        int rubyNeed = 100 - player.inventory.gem;
                        player.inventory.gem = 0;
                        player.inventory.ruby -= rubyNeed;
                    }

                    if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                        int[] options = {77, 80, 81, 103, 50, 94, 5, 14};
                        int optionId = options[Util.nextInt(0, options.length - 1)];
                        int param = Util.nextInt(3, 10);
                        bongTai.itemOptions.clear();
                        bongTai.itemOptions.add(new Item.ItemOption(optionId, param));
                        bongTai.itemOptions.add(new Item.ItemOption(38, 0));
                        bongTai.itemOptions.add(new Item.ItemOption(72, 2));
                        CombineService.gI().sendEffectSuccessCombine(player);
                    } else {
                        CombineService.gI().sendEffectFailCombine(player);
                    }

                    InventoryService.gI().subQuantityItemsBag(player, honBongTai, 50);
                    InventoryService.gI().subQuantityItemsBag(player, daXanhLam, 1);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendMoney(player);
                    CombineService.gI().reOpenItemCombine(player);
                }
            }
        }
    }
}
