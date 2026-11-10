/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import consts.ConstNpc;
import java.util.function.BooleanSupplier;
import models.item.Item;
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
public class PhanRaDoThan {

    private static final int GOLD_COST = 50_000_000;
    private static final int DISPLAY_SUCCESS_RATE = 80;
    private static final int ACTUAL_SUCCESS_RATE = 50;

    private static boolean validMaterial(Player player) {
        if (player == null || player.combineNew == null
                || player.combineNew.itemsCombine.size() != 1) {
            return false;
        }
        Item item = player.combineNew.itemsCombine.get(0);
        return item != null && item.isNotNullItem() && item.isDTL()
                && item.quantity >= 1
                && player.inventory.itemsBag.contains(item)
                && player.inventory.gold >= GOLD_COST;
    }

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() == 0) {
            CombineService.gI().shopNew.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Con hãy đưa ta đồ thần linh để phân rã", "Đóng");
            return;
        }

        if (player.combineNew.itemsCombine.size() == 1) {
            Item item = player.combineNew.itemsCombine.get(0);

            if (item != null && item.isNotNullItem() && item.isDTL()) {
                boolean isCoin10k = item.template.id == 561
                        || item.template.id == 562
                        || item.template.id == 564
                        || item.template.id == 566;

                String npcSay = "|2|Sau khi bán đồ thần linh này\n|7|"
                        + "Tỉ lệ thành công: " + DISPLAY_SUCCESS_RATE + "%\n"
                        + "Thành công nhận: 1 Coin "
                        + (isCoin10k ? "10K" : "8K") + " VNĐ\n"
                        + "Thất bại mất món đồ và vàng\n"
                        + (player.inventory.gold < 50000000 ? "|7|" : "|1|")
                        + "Cần " + Util.numberToMoney(GOLD_COST) + " vàng";

                if (player.inventory.gold < GOLD_COST) {
                    CombineService.gI().shopNew.npcChat(player, "Hết vàng rồi\nẢo ít thôi con");
                    return;
                }

                CombineService.gI().shopNew.createOtherMenu(player, ConstNpc.MENU_PHAN_RA_DO_THAN_LINH,
                        npcSay, "Phân rã\n" + Util.numberToMoney(GOLD_COST) + " vàng", "Từ chối");
            } else {
                CombineService.gI().shopNew.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Ta chỉ có thể phân rã đồ thần linh thôi", "Đóng");
            }
        } else {
            CombineService.gI().shopNew.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Ta chỉ có thể giúp con bán 1 lần 1 món đồ thần linh", "Đóng");
        }
    }

    public static void PhanRaDoThan(Player player) {
        PhanRaDoThan(player, () -> Util.isTrue(ACTUAL_SUCCESS_RATE, 100));
    }

    static void PhanRaDoThan(Player player, BooleanSupplier successRoll) {
        if (!validMaterial(player)) {
            Service.gI().sendThongBao(player, "Không đủ điều kiện phân rã đồ thần linh");
            return;
        }

        Item item = player.combineNew.itemsCombine.get(0);
        boolean success = successRoll.getAsBoolean();
        player.inventory.gold -= GOLD_COST;
        InventoryService.gI().subQuantityItemsBag(player, item, 1);

        if (success) {
            if (item.template.id == 561 || item.template.id == 562 || item.template.id == 564 || item.template.id == 566) {
                Item coin10k = ItemService.gI().createNewItem((short) 1340);
                coin10k.itemOptions.add(new Item.ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, coin10k);
                Service.gI().sendThongBaoOK(player, "Bạn nhận được Coin 10.000 VNĐ");
            } else {
                Item coin8k = ItemService.gI().createNewItem((short) 1341);
                coin8k.itemOptions.add(new Item.ItemOption(30, 0));
                InventoryService.gI().addItemBag(player, coin8k);
                Service.gI().sendThongBaoOK(player, "Bạn nhận được Coin 8.000 VNĐ");
            }
            CombineService.gI().sendEffectSuccessCombine(player);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Phân rã thất bại, mất món đồ thần linh và vàng.");
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
