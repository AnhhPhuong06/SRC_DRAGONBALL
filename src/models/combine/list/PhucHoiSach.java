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
public class PhucHoiSach {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item sachTuyetKy = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item)) {
                    sachTuyetKy = item;
                }
            }
            if (sachTuyetKy != null) {
                String npcSay = "|2|Phục hồi " + sachTuyetKy.getName() + "\n"
                        + "Cần 10 cuốn sách cũ\n"
                        + "Phí phục hồi 500 triệu vàng";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                        "Đồng ý", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Không tìm thấy vật phẩm");
                return;
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Không tìm thấy vật phẩm");
            return;
        }
    }

    public static void PhucHoisach(Player player) {
        if (player.combineNew.itemsCombine.size() == 1) {
            Item cuonSachCu = InventoryService.gI().findItemBagByTemp(player, (short) 1187);
            int goldPhanra = 500_000_000;
            Item sachTuyetKy = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item)) {
                    sachTuyetKy = item;
                }
            }
            if (sachTuyetKy != null) {
                int doBen = 0;
                ItemOption optionLevel = null;
                for (ItemOption io : sachTuyetKy.itemOptions) {
                    if (io.optionTemplate.id == 226) {
                        doBen = io.param;
                        optionLevel = io;
                        break;
                    }
                }
                if (cuonSachCu == null) {
                    Service.gI().sendThongBaoOK(player, "Cần sách tuyệt kỹ và 10 cuốn sách cũ");
                    return;
                }
                if (cuonSachCu.quantity < 10) {
                    Service.gI().sendThongBaoOK(player, "Cần sách tuyệt kỹ và 10 cuốn sách cũ");
                    return;
                }
                if (player.inventory.gold < goldPhanra) {
                    Service.gI().sendThongBao(player, "Không đủ 500tr vàng");
                    return;
                }
                if (doBen != 1000) {
                    for (int i = 0; i < sachTuyetKy.itemOptions.size(); i++) {
                        if (sachTuyetKy.itemOptions.get(i).optionTemplate.id == 226) {
                            sachTuyetKy.itemOptions.get(i).param = 1000;
                            break;
                        }
                    }
                    player.inventory.gold -= 500_000_000;
                    InventoryService.gI().subQuantityItemsBag(player, cuonSachCu, 10);
                    InventoryService.gI().sendItemBags(player);
                    Service.gI().sendMoney(player);
                    CombineService.gI().sendEffectSuccessCombine(player);
                    CombineService.gI().reOpenItemCombine(player);
                } else {
                    Service.gI().sendThongBao(player, "Sách còn dùng được");
                    return;
                }
            }
        }
    }
}
