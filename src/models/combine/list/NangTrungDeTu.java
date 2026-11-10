package models.combine.list;

import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public class NangTrungDeTu {

    private static final int THOI_VANG = 457;

    private record Recipe(int matId, String matName, int matQty, int goldQty, int resultId, int rate) {
    }

    private static final Recipe[] RECIPES = {
        new Recipe(568, "Quả Trứng (đệ Mabư)", 5, 50, 1521, 80),
        new Recipe(1521, "Trứng Dái Ubu", 5, 50, 1519, 70),
        new Recipe(1519, "Trứng Dái Black", 5, 50, 1475, 50),
    };

    private static Recipe findRecipe(Player player) {
        for (Recipe r : RECIPES) {
            for (Item it : player.combineNew.itemsCombine) {
                if (it != null && it.isNotNullItem() && it.template.id == r.matId()) {
                    return r;
                }
            }
        }
        return null;
    }

    private static int quantityInBag(Player player, int tempId) {
        return InventoryService.gI().getNumItemBag(player, tempId);
    }

    private static String reqLine(String name, int have, int need) {
        return name + " " + have + "/" + need + "\n";
    }

    public static void showInfoCombine(Player player) {
        Recipe r = findRecipe(player);
        if (r == null) {
            Service.gI().sendThongBaoOK(player,
                    "Hãy đặt trứng đệ tử (Quả Trứng, Trứng Ubu...) vào ô để nâng cấp.");
            return;
        }
        int rate = services.CombineRateConfig.getRateTrungDeTu(r.matId());
        String npcSay = "Nâng cấp thành " + ItemService.gI().getTemplate(r.resultId()).name + "\n"
                + reqLine(r.matName(), quantityInBag(player, r.matId()), r.matQty())
                + reqLine("Thỏi vàng", quantityInBag(player, THOI_VANG), r.goldQty())
                + "Tỉ lệ thành công: " + rate + "%\n"
                + "Thất bại sẽ mất toàn bộ nguyên liệu";

        CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Nâng cấp", "Từ chối");
    }

    public static void NangCap(Player player) {
        Recipe r = findRecipe(player);
        if (r == null) {
            Service.gI().sendThongBaoOK(player, "Hãy đặt trứng đệ tử vào ô để nâng cấp.");
            return;
        }
        int eggTotal = InventoryService.gI().getNumItemBag(player, r.matId());
        int goldTotal = InventoryService.gI().getNumItemBag(player, THOI_VANG);
        if (eggTotal < r.matQty()) {
            Service.gI().sendThongBaoOK(player, "Ngươi không đủ trứng nguyên liệu để nâng cấp");
            return;
        }
        if (goldTotal < r.goldQty()) {
            Service.gI().sendThongBaoOK(player, "Ngươi không đủ " + r.goldQty() + " thỏi vàng để nâng cấp");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) <= 0) {
            Service.gI().sendThongBaoOK(player, "Hành trang đã đầy, hãy dọn bớt trước khi nâng cấp");
            return;
        }

        InventoryService.gI().subQuantityItemsBag(player, r.matId(), r.matQty());
        InventoryService.gI().subQuantityItemsBag(player, THOI_VANG, r.goldQty());

        int rate = services.CombineRateConfig.getRateTrungDeTu(r.matId());
        if (Util.isTrue(rate, 100)) {
            Item result = ItemService.gI().createNewItem((short) r.resultId(), 1);
            InventoryService.gI().addItemBag(player, result);
            CombineService.gI().sendEffectSuccessCombine(player);
            Service.gI().sendThongBao(player, "Nâng cấp thành công, ngươi nhận được " + result.template.name);
        } else {
            CombineService.gI().sendEffectFailCombine(player);
            Service.gI().sendThongBao(player, "Nâng cấp thất bại, ngươi đã mất toàn bộ nguyên liệu");
        }
        player.combineNew.itemsCombine.clear();
        InventoryService.gI().sendItemBags(player);
        CombineService.gI().reOpenItemCombine(player);
    }
}
