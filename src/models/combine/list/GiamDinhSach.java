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
public class GiamDinhSach {

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item sachTuyetKy = null;
            Item buaGiamDinh = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item)) {
                    sachTuyetKy = item;
                } else if (item.template.id == 1152) {
                    buaGiamDinh = item;
                }
            }
            if (sachTuyetKy != null && buaGiamDinh != null) {

                String npcSay = "|1|" + sachTuyetKy.getName() + "\n";
                npcSay += "|2|" + buaGiamDinh.getName() + " " + buaGiamDinh.quantity + "/1";
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                        "Giám định", "Từ chối");
            } else {
                Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ và bùa giám định");
                return;
            }
        } else {
            Service.gI().sendThongBaoOK(player, "Cần Sách Tuyệt Kỹ và bùa giám định");
            return;
        }
    }

    public static void GiamDinhsach(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {

            Item sachTuyetKy = null;
            Item buaGiamDinh = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (CombineService.gI().issachTuyetKy(item)) {
                    sachTuyetKy = item;
                } else if (item.template.id == 1152) {
                    buaGiamDinh = item;
                }
            }
            if (sachTuyetKy != null && buaGiamDinh != null) {
                Item sachTuyetKy_2 = ItemService.gI().createNewItem((short) sachTuyetKy.template.id);
                if (CombineService.gI().checkHaveOption(sachTuyetKy, 0, 224)) {
                    int tyle = Util.nextInt(100);
                    if (tyle >= 0 && tyle <= 33) {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(50, Util.nextInt(5, 15)));
                    } else if (tyle > 33 && tyle <= 66) {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(77, Util.nextInt(5, 15)));
                    } else {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(103, Util.nextInt(5, 15)));
                    }
                    for (int i = 1; i < sachTuyetKy.itemOptions.size(); i++) {
                        sachTuyetKy_2.itemOptions.add(new ItemOption(sachTuyetKy.itemOptions.get(i).optionTemplate.id, sachTuyetKy.itemOptions.get(i).param));
                    }
                    CombineService.gI().sendEffectSuccessCombine(player);
                    InventoryService.gI().addItemBag(player, sachTuyetKy_2);
                    InventoryService.gI().subQuantityItemsBag(player, sachTuyetKy, 1);
                    InventoryService.gI().subQuantityItemsBag(player, buaGiamDinh, 1);
                    InventoryService.gI().sendItemBags(player);
                    CombineService.gI().reOpenItemCombine(player);
                } else {
                    Service.gI().sendThongBao(player, "Sách đã được giám định");
                    return;
                }
            }
        }
    }
}
