/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.combine.list;

import consts.ConstFont;
import consts.ConstNpc;
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
public class NangCapChanMenh {

    private static final int STONES_PER_ATTEMPT = 20; // số đá tốn mỗi lần thử (thành công hoặc thất bại)
    private static final int[] successRate = {20, 10, 5, 3, 2, 1, 1}; // tỉ lệ thành công %
    private static final int thoiVangRequired = 5;
    private static final int rubyRequired = 20;
    private static final short THOI_VANG_ID = 457;
    private static final short MAX_ITEM_ID = 1559;

    public static void showInfoCombine(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            Service.gI().sendDialogMessage(player, "Cần 1 Chân Thiên Tử và đá hoàng kim.");
            return;
        }

        Item chanMenh = null; // item nâng cấp
        Item daHoangKim = null; // đá hoàng kim

        for (Item item : player.combineNew.itemsCombine) {
            if (item.template.id >= 1551 && item.template.id <= 1559) {
                chanMenh = item;
            } else if (item.template.id == 1560) {
                daHoangKim = item;
            }
        }

        if (chanMenh == null || daHoangKim == null) {
            Service.gI().sendDialogMessage(player, "Cần 1 Chân Thiên Tử và đá hoàng kim.");
            return;
        }

        int currentLevel = chanMenh.template.level;
        if (chanMenh.template.id >= MAX_ITEM_ID) {
            Service.gI().sendDialogMessage(player, "Chân Thiên Tử đã đạt cấp tối đa.");
            return;
        }

        int rate = successRate[currentLevel - 1];

        Item thoiVang = InventoryService.gI().findItemBagByTemp(player, THOI_VANG_ID);
        int thoiVangHave = thoiVang != null ? thoiVang.quantity : 0;

        StringBuilder text = new StringBuilder();
        text.append(ConstFont.BOLD_BLUE).append("Chân Thiên Tử [+").append(currentLevel).append("]\n\n");
        text.append(ConstFont.BOLD_BLUE).append("Tỉ lệ thành công: ").append(rate).append("%\n");
        text.append(ConstFont.BOLD_BLUE).append("Khi thành công: +1% đến +3% các chỉ số (sđ , hp , ki , sđcm , chí mạng)\n\n");

        text.append(daHoangKim.quantity >= STONES_PER_ATTEMPT ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần ").append(STONES_PER_ATTEMPT).append(" đá hoàng kim\n");
        text.append(thoiVangHave >= thoiVangRequired ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần ").append(thoiVangRequired).append(" thỏi vàng\n");
        text.append(player.inventory.ruby >= rubyRequired ? ConstFont.BOLD_BLUE : ConstFont.BOLD_RED)
                .append("Cần ").append(rubyRequired).append(" hồng ngọc\n");
        text.append(ConstFont.BOLD_RED).append("Thất bại mất ").append(STONES_PER_ATTEMPT).append(" đá hoàng kim\n");

        if (player.inventory.ruby < rubyRequired) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu " + (rubyRequired - player.inventory.ruby) + " hồng ngọc");
            return;
        }
        if (thoiVangHave < thoiVangRequired) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu " + (thoiVangRequired - thoiVangHave) + " thỏi vàng");
            return;
        }
        if (daHoangKim.quantity < STONES_PER_ATTEMPT) {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, text.toString(),
                    "Còn thiếu " + (STONES_PER_ATTEMPT - daHoangKim.quantity) + " đá hoàng kim");
            return;
        }

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, text.toString(),
                "Nâng cấp\n" + thoiVangRequired + " thỏi vàng\n" + rubyRequired + " hồng ngọc", "Từ chối");
    }

    public static void nangCap(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) {
            return;
        }

        Item chanMenh = null;
        Item daHoangKim = null;

        for (Item item : player.combineNew.itemsCombine) {
            if (item.template.id >= 1551 && item.template.id <= MAX_ITEM_ID) {
                chanMenh = item;
            } else if (item.template.id == 1560) {
                daHoangKim = item;
            }
        }

        if (chanMenh == null || daHoangKim == null) {
            return;
        }
        if (chanMenh.template.id >= MAX_ITEM_ID) {
            return;
        }

        int currentLevel = chanMenh.template.level;
        int rate = successRate[currentLevel - 1];

        Item thoiVang = InventoryService.gI().findItemBagByTemp(player, THOI_VANG_ID);
        int thoiVangHave = thoiVang != null ? thoiVang.quantity : 0;

        if (daHoangKim.quantity < STONES_PER_ATTEMPT || thoiVangHave < thoiVangRequired || player.inventory.ruby < rubyRequired) {
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, thoiVang, thoiVangRequired);
        player.inventory.ruby -= rubyRequired;
        InventoryService.gI().subQuantityItemsBag(player, daHoangKim, STONES_PER_ATTEMPT);

        if (Util.isTrue(rate, 100)) {
            short newItemId = (short) (chanMenh.template.id + 1);
            if (newItemId > MAX_ITEM_ID) {
                newItemId = MAX_ITEM_ID;
            }
            Item newChanMenh = ItemService.gI().createNewItem(newItemId);
            for (Item.ItemOption io : chanMenh.itemOptions) {
                newChanMenh.itemOptions.add(new Item.ItemOption(io.optionTemplate.id, io.param));
            }
            int[] upgradeOptions = {50, 77, 103, 5, 14, 30};
            for (int optionId : upgradeOptions) {
                int bonus = Util.nextInt(1, 3); // random 1–3%
                boolean found = false;

                for (Item.ItemOption io : newChanMenh.itemOptions) {
                    if (io.optionTemplate.id == optionId) {
                        io.param += bonus; // cộng dồn param cũ
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    newChanMenh.itemOptions.add(new Item.ItemOption(optionId, bonus));
                }
            }
            InventoryService.gI().subQuantityItemsBag(player, chanMenh, 1);
            InventoryService.gI().addItemBag(player, newChanMenh);
            CombineService.gI().sendEffectSuccessCombine(player);

        } else {
            CombineService.gI().sendEffectFailCombine(player);
        }

        InventoryService.gI().sendItemBags(player);
        Service.gI().sendMoney(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
