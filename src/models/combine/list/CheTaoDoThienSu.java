/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import java.util.ArrayList;
import java.util.Arrays;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class CheTaoDoThienSu {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() < 4 || player.combineNew.itemsCombine.size() > 4) {
            Service.gI().sendThongBao(player, "Thiếu vật phẩm, vui lòng thêm vào");
            return;
        } else if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isCongThucVip()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Công Thức Vip");
            return;
        } else if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isManhTS() && item.quantity >= 999).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Mảnh Thiên Sứ");
            return;
        } else if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDaNangCap1()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Đá Nâng Cấp");
            return;
        } else if (player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDaMayMan()).count() != 1) {
            Service.gI().sendThongBao(player, "Thiếu Đá May Mắn");
            return;
        }
        // BUG cũ: toàn bộ thực thi (trừ 1 tỷ vàng, roll, tạo đồ) bị đặt nhầm trong showInfoCombine -
        // chỉ CHỌN item là bị trừ tiền, không có bước xác nhận. Giờ chỉ hiển thị menu;
        // thực thi duy nhất nằm ở CheTaoDoThiensu().
        String npcSay = "|2|Chế tạo đồ Thần Linh VIP\n"
                + "|7|Cần: Công Thức VIP + 999 Mảnh Thiên Sứ + Đá Nâng Cấp + Đá May Mắn\n"
                + "|5|Chi phí: 1 tỷ vàng";
        CombineService.gI().baHatMit.createOtherMenu(player, consts.ConstNpc.MENU_START_COMBINE, npcSay, "Chế Tạo", "Từ Chối");
    }


    public static void CheTaoDoThiensu(Player player) {
        if (player.combineNew.itemsCombine.size() != 4) {
            Service.gI().sendThongBao(player, "Thiếu đồ");
            return;
        }
        if (player.inventory.gold < 1000000000) {
            Service.gI().sendThongBao(player, "Trong người không đủ 1 tỷ vàng mà định làm gì đấy con");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 1 ô trống hành trang");
            return;
        }
        Item itemTL = player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isDHD()).findFirst().get();
        Item itemManh = player.combineNew.itemsCombine.stream().filter(item -> item.isNotNullItem() && item.isManhTS() && item.quantity >= 99).findFirst().get();

        player.inventory.gold -= 1000000000;
        CombineService.gI().sendEffectSuccessCombine(player);
        short[][] itemIds = {{1048, 1051, 1054, 1057, 1060}, {1049, 1052, 1055, 1058, 1061}, {1050, 1053, 1056, 1059, 1062}}; // thứ tự td - 0,nm - 1, xd - 2
        Item itemTS = ItemService.gI().DoThienSu(itemIds[itemTL.template.gender > 2 ? player.gender : itemTL.template.gender][itemManh.typeIdManh()], itemTL.template.gender);
        InventoryService.gI().addItemBag(player, itemTS);
        InventoryService.gI().subQuantityItemsBag(player, itemTL, 1);
        InventoryService.gI().subQuantityItemsBag(player, itemManh, 99);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        Service.gI().sendThongBao(player, "Bạn đã nhận được " + itemTS.template.name);
        player.combineNew.itemsCombine.clear();
        CombineService.gI().reOpenItemCombine(player);
    }
}
