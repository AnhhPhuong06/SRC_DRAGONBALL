package models.combine.list;

import consts.ConstItem;
import consts.ConstNpc;
import java.util.HashSet;
import java.util.List;
import java.util.function.BooleanSupplier;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public class PhanRaDoThanLinh {
    public static final int DISPLAY_SUCCESS_RATE = 80;
    private static final int ACTUAL_SUCCESS_RATE = 50;
    private static final String REQUIREMENTS = "Hãy chọn đúng 1 món đồ thần linh bất kỳ trong hành trang để phân rã.";

    private static boolean validMaterials(Player player) {
        List<Item> items = player.combineNew.itemsCombine;
        if (items.size() != 1 || new HashSet<>(items).size() != 1) return false;
        for (Item item : items) {
            if (item == null || !item.isNotNullItem() || !item.isDTL() || item.quantity != 1
                    || !player.inventory.itemsBag.contains(item)) return false;
        }
        return true;
    }

    public static void showInfoCombine(Player player) {
        synchronized (player.inventory.itemsBag) {
            if (!validMaterials(player)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        REQUIREMENTS, "Đóng");
                return;
            }
            String names = "";
            for (Item item : player.combineNew.itemsCombine) names += "\n" + item.template.name;
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "|2|Phân rã 1 món đồ thần linh" + names
                    + "\n|7|Tỉ lệ thành công: " + DISPLAY_SUCCESS_RATE + "%"
                    + "\n|1|Thành công nhận 1 Đá ngũ sắc."
                    + "\nMỗi lần phân rã đều mất 1 món, kể cả khi thất bại.",
                    "Phân rã", "Từ chối");
        }
    }

    public static void phanRa(Player player) {
        phanRa(player, () -> Util.isTrue(ACTUAL_SUCCESS_RATE, 100));
    }

    static void phanRa(Player player, BooleanSupplier successRoll) {
        synchronized (player.inventory.itemsBag) {
            if (!validMaterials(player)) {
                Service.gI().sendThongBao(player, REQUIREMENTS);
                return;
            }
            boolean success = successRoll.getAsBoolean();
            Item reward = success ? ItemService.gI().createNewItem((short) ConstItem.DA_NGU_SAC, 1) : null;
            for (Item item : player.combineNew.itemsCombine) {
                InventoryService.gI().subQuantityItemsBag(player, item, 1);
            }
            // Món bị trừ giải phóng ô cho đá thưởng, kể cả khi hành trang ban đầu đầy.
            if (success) {
                InventoryService.gI().addItemBag(player, reward);
                CombineService.gI().sendEffectSuccessCombine(player);
                Service.gI().sendThongBao(player, "Phân rã thành công, nhận được 1 Đá ngũ sắc.");
            } else {
                CombineService.gI().sendEffectFailCombine(player);
                Service.gI().sendThongBao(player, "Phân rã thất bại, mất 1 món đồ thần linh.");
            }
            player.combineNew.clearItemCombine();
            InventoryService.gI().sendItemBags(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }
}
