/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import consts.ConstNpc;
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
public class NangCapSachTuyetKi {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item sachTuyetKy = null;
            Item kimBamGiay = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item) && (item.template.id == 1181 || item.template.id == 1183 || item.template.id == 1185)) {
                    sachTuyetKy = item;
                } else if (item.template.id == 1153) {
                    kimBamGiay = item;
                }
            }
            if (sachTuyetKy != null && kimBamGiay != null) {
                if (kimBamGiay.quantity < 10) {
                    Service.gI().sendThongBaoOK(player, "Cần ít nhất 10 Kìm bấm giấy mới có thể nâng cấp.");
                    return;
                }
                String npcSay = "|2|Nâng cấp " + sachTuyetKy.template.name + "\n";
                npcSay += "Cần 10 Kìm bấm giấy (Hiện có: " + kimBamGiay.quantity + "/10)\n"
                        + "Tỉ lệ thành công: 10%\n"
                        + "Nâng cấp thất bại sẽ mất 10 Kìm bấm giấy";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                        "Nâng cấp", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
                return;
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ 1 và 10 Kìm bấm giấy.");
            return;
        }
    }

    public static void NangCapSachTuyetki(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item sachTuyetKy = null;
            Item kimBamGiay = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item) && (item.template.id == 1181 || item.template.id == 1183 || item.template.id == 1185)) {
                    sachTuyetKy = item;
                } else if (item.template.id == 1153) {
                    kimBamGiay = item;
                }
            }
            if (sachTuyetKy != null && kimBamGiay != null) {
                if (kimBamGiay.quantity < 10) {
                    Service.gI().sendThongBao(player, "Không đủ Kìm bấm giấy");
                    return;
                }
                if (!Util.isTrue(10, 100)) {
                    // Thất bại theo tỉ lệ 10% hiển thị trên menu: mất 10 kìm bấm giấy
                    InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, 10);
                    CombineService.gI().sendEffectFailCombine(player);
                    InventoryService.gI().sendItemBags(player);
                    CombineService.gI().reOpenItemCombine(player);
                    Service.gI().sendThongBao(player, "Nâng cấp thất bại, mất 10 Kìm bấm giấy");
                    return;
                }
                Item sachTuyetKy_2 = ItemService.gI().createNewItem((short) (sachTuyetKy.template.id + 1));
                boolean coOption225 = false;
                for (int i = 0; i < sachTuyetKy.itemOptions.size(); i++) {
                    ItemOption io = sachTuyetKy.itemOptions.get(i);
                    if (io.optionTemplate.id == 225) {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(225, 10)); // Sách cấp 2 có 10 lượt tẩy
                        coOption225 = true;
                    } else {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(io.optionTemplate.id, io.param));
                    }
                }
                if (!coOption225) {
                    sachTuyetKy_2.itemOptions.add(new ItemOption(225, 10));
                }
                CombineService.gI().sendEffectSuccessCombine(player);
                InventoryService.gI().addItemBag(player, sachTuyetKy_2);
                InventoryService.gI().subQuantityItemsBag(player, sachTuyetKy, 1);
                InventoryService.gI().subQuantityItemsBag(player, kimBamGiay, 10);
                InventoryService.gI().sendItemBags(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }
}
