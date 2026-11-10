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
import services.Service;
import services.player.InventoryService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class TinhAnTrangBi {

    private static final int COST = 500000000;
    private static final int RUBY = 20000;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item cailoz = null;
            Item concak = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.type >= 0 && item.template.type <= 4) {
                    cailoz = item;
                } else if (item.template.id == 674) {
                    concak = item;
                }
            }
            if (cailoz != null && concak != null && concak.quantity >= 1) {
                player.combineNew.ratioCombine = 30;

                String npcSay = "|7|Ép ấn trang bị " + cailoz.template.name + "\n" + "Tỉ Lệ Thành Công: " + player.combineNew.ratioCombine + "%\n" + "Cần 20.000 hồng ngọc";
                if (player.inventory.ruby >= RUBY) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay,
                            "Nâng Cấp");
                } else {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Cần 1 món trang bị áo quần găng giầy rada và x1 đá ngũ sắc", "Đóng");
                }
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 món trang bị áo quần găng giầy rada và x1 đá ngũ sắc", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 món trang bị áo quần găng giầy rada và x1 đá ngũ sắc", "Đóng");
        }
    }

    public static void TinhAnTrangBi(Player player) {
        if (player.combineNew.itemsCombine.size() == 2) {
            Item cailoz = null;
            Item concac = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (item.template.type >= 0 && item.template.type <= 4) {
                    cailoz = item;
                } else if (item.template.id == 674) {
                    concac = item;
                }
            }
            if (cailoz != null && concac != null && concac.quantity >= 1) {
                if (player.inventory.ruby < RUBY) {
                    Service.gI().sendThongBao(player, "Bạn không đủ 20.000 hồng ngọc để ép ấn");
                    return;
                }
                player.inventory.ruby -= RUBY;
                InventoryService.gI().subQuantityItemsBag(player, concac, 1);
                if (Util.isTrue(player.combineNew.ratioCombine, 100)) {
                    int rdUp = Util.nextInt(0, 2);
                    cailoz.itemOptions.removeIf(opt -> opt.optionTemplate.id == 34 || opt.optionTemplate.id == 35 || opt.optionTemplate.id == 36);
                    if (rdUp == 0) {
                        cailoz.itemOptions.add(new Item.ItemOption(34, 1));
                    } else if (rdUp == 1) {
                        cailoz.itemOptions.add(new Item.ItemOption(35, 1));
                    } else if (rdUp == 2) {
                        cailoz.itemOptions.add(new Item.ItemOption(36, 1));
                    }
                    CombineService.gI().sendEffectSuccessCombine(player);
                } else {
                    CombineService.gI().sendEffectFailCombine(player);
                }
                InventoryService.gI().sendItemBags(player);
                Service.gI().sendMoney(player);
                CombineService.gI().reOpenItemCombine(player);
            }
        }
    }
}
