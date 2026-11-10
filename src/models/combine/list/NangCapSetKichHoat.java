package models.combine.list;

import consts.ConstItem;
import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 * Nâng SKH Thường: 1 đồ Thần Linh -> 1 món SKH cùng loại ngẫu nhiên dưới Thần Linh
 */
public class NangCapSetKichHoat {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item itemTL = player.combineNew.itemsCombine.get(0);
            if (itemTL == null || !itemTL.isDTL()) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 món Thần Linh", "Đóng");
                return;
            }

            if (player.inventory.gold < 200_000_000 || player.inventory.ruby < 500) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Không đủ 200 triệu vàng và 500 hồng ngọc", "Đóng");
                return;
            }

            String npcSay = "|2|Con có muốn nâng cấp " + itemTL.template.name + " thành Set Kích Hoạt thường?\n|7|"
                    + "Sẽ nhận được 1 món kích hoạt cùng loại ngẫu nhiên dưới Thần Linh\n"
                    + "|1|Cần 200 triệu vàng và 500 Hồng ngọc";

            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    npcSay, "Nâng cấp\n200Tr vàng + 500 Hồng ngọc", "Từ chối");
        } else {
            if (player.combineNew.itemsCombine.size() > 1) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Nguyên liệu không phù hợp", "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Còn thiếu nguyên liệu để nâng cấp, cần 1 món Thần Linh", "Đóng");
        }
    }

    public static void nangCapSetKichHoat(Player player, int... numm) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBao(player, "Cần 1 món Thần Linh");
            return;
        }

        Item itemTL = player.combineNew.itemsCombine.get(0);
        if (itemTL == null || !itemTL.isDTL()) {
            Service.gI().sendThongBao(player, "Cần 1 món Thần Linh");
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }

        if (player.inventory.gold < 200_000_000) {
            Service.gI().sendThongBao(player, "Con cần 200Tr vàng để đổi...");
            return;
        }
        if (player.inventory.ruby < 500) {
            Service.gI().sendThongBao(player, "Con cần 500 hồng ngọc để đổi...");
            return;
        }

        if (!player.inventory.itemsBag.contains(itemTL)) {
            Service.gI().sendThongBao(player, "Vật phẩm nguyên liệu không còn trong hành trang!");
            player.combineNew.itemsCombine.clear();
            CombineService.gI().reOpenItemCombine(player);
            return;
        }

        int type = itemTL.template.type;
        if (type < 0 || type > 4) {
            type = 0;
        }

        int gender = (itemTL.template.gender >= 0 && itemTL.template.gender <= 2) ? itemTL.template.gender : player.gender;
        if (gender < 0 || gender > 2) {
            gender = 0;
        }

        int[] clothes = ConstItem.LIST_ITEM_CLOTHES[gender][type];
        // clothes.length - 1 là đồ Thần Linh, nên từ 0 đến clothes.length - 2 là đồ dưới Thần Linh
        int rewardTempId = clothes[Util.nextInt(0, clothes.length - 2)];

        int skhOptionId = ItemService.gI().randomSKHId((byte) gender);
        Item item = ItemService.gI().itemSKH(rewardTempId, skhOptionId, itemTL.itemOptions);

        if (item != null) {
            player.inventory.gold -= 200_000_000;
            player.inventory.ruby -= 500;
            CombineService.gI().sendEffectSuccessCombine(player);
            InventoryService.gI().subQuantityItemsBag(player, itemTL, 1);
            InventoryService.gI().addItemBag(player, item);
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + item.template.name);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Có lỗi xảy ra khi tạo vật phẩm kích hoạt");
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }

}
