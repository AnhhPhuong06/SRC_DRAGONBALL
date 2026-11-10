package models.combine.list;

import java.util.function.Supplier;
import models.player.Player;
import services.CombineService;
import services.Service;
import services.player.InventoryService;

/** Each attempt validates and pays independently; publish the final inventory once. */
final class CombineBatch {
    record Result(Boolean success, String message) {
        static Result blocked(String message) { return new Result(null, message); }
    }

    private CombineBatch() {}

    static void run(Player player, int requested, boolean usesGems, Supplier<Result> attempt) {
        if (requested != 1 && requested != 10) {
            Service.gI().sendThongBao(player, "Chỉ có thể chọn nâng cấp 1 lần hoặc 10 lần.");
            return;
        }
        synchronized (player.inventory.itemsBag) {
            int completed = 0;
            int successes = 0;
            Result last = null;
            String stop = null;
            for (int i = 0; i < requested; i++) {
                Result result = attempt.get();
                if (result.success() == null) {
                    stop = result.message();
                    break;
                }
                completed++;
                if (result.success()) successes++;
                last = result;
                // Depleted selected stacks must not invalidate the next paid attempt.
                retainItems(player);
            }
            if (completed == 0) {
                Service.gI().sendThongBao(player, stop);
                return;
            }
            if (successes > 0) CombineService.gI().sendEffectSuccessCombine(player);
            else CombineService.gI().sendEffectFailCombine(player);
            String result = requested == 1 ? last.message()
                    : "Đã thực hiện " + completed + "/" + requested + " lần: " + successes
                            + " thành công, " + (completed - successes) + " thất bại.\n"
                            + "Chỉ trừ chi phí của " + completed + " lượt đã thực hiện.\n"
                            + last.message();
            if (stop != null) result += "\nDừng: " + stop;
            Service.gI().sendThongBao(player, result);
            if (usesGems) Service.gI().sendMoney(player);
            // The client rebuilds bag objects before it receives retained selection indices.
            InventoryService.gI().sendItemBags(player);
            retainItems(player);
            CombineService.gI().reOpenItemCombine(player);
        }
    }

    private static void retainItems(Player player) {
        player.combineNew.itemsCombine.removeIf(item -> item == null || !item.isNotNullItem()
                || item.quantity <= 0 || !player.inventory.itemsBag.contains(item));
    }
}
