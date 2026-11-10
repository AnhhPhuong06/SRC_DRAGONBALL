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
public class TaySach {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item sachTuyetKy = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item)) {
                    sachTuyetKy = item;
                }
            }
            if (sachTuyetKy != null) {
                String npcSay = "|3|Tẩy Sách Tuyệt Kỹ\n";
                npcSay += "|5|Tẩy sách loại bỏ các option HP,KI,SD";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                        "Đồng ý", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ để tẩy");
                return;
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ để tẩy");
            return;
        }
    }

    public static void taySach(Player player) {
        if (player.combineNew.itemsCombine.size() != 1) {
            Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ để tẩy");
            return;
        }
        Item sachTuyetKy = player.combineNew.itemsCombine.get(0);
        if (sachTuyetKy == null || !CombineService.gI().issachTuyetKy(sachTuyetKy)) {
            return;
        }
        if (sachTuyetKy.getOptionParam(225) <= 0 || sachTuyetKy.isHaveOption(224)) {
            Service.gI().sendServerMessage(player, "Không thể thực hiện");
            return;
        }
        for (int i = 0; i < sachTuyetKy.itemOptions.size(); i++) {
            Item.ItemOption io = sachTuyetKy.itemOptions.get(i);
            if (io.optionTemplate.id == 21) {
                break;
            }
            sachTuyetKy.itemOptions.set(i, new Item.ItemOption(224, 0));
        }
        sachTuyetKy.subOptionParam(225, 1);
        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
