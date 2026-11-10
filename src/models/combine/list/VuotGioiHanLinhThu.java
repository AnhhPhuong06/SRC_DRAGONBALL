package models.combine.list;

import consts.ConstItem;
import consts.ConstNpc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public final class VuotGioiHanLinhThu {
    public static final int SUCCESS_RATE = 10;
    public static final int MIN_STAT_PERCENT = 5;
    public static final String MIN_STAT_RULE = "Các dòng SĐ, HP, KI, SĐCM đang có trên pet phải từ 5% trở lên.";
    private static final int BONUS = 2;
    private static final int[] STAT_OPTIONS = {50, 77, 103, 5};
    private static final String REQUIREMENTS = "Chọn 1 pet và 1 Ngọc Rồng 1 sao trong hành trang.";
    private static final String BAG_SPACE = "Pet đang xếp chồng. Cần trống 1 ô hành trang để tách riêng 1 pet vượt giới hạn.";

    private record Materials(Item linhThu, Item dragonBall, List<ItemOption> stats) {}

    // Only the four standard percentage bonuses: damage, HP, KI and critical damage.
    private static boolean isStatOption(int id) {
        return switch (id) {
            case 50, 77, 103, 5 -> true;
            default -> false;
        };
    }

    private static Materials findMaterials(Player player) {
        if (player.combineNew.itemsCombine.size() != 2) return null;
        Item linhThu = null;
        Item dragonBall = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity <= 0
                    || !player.inventory.itemsBag.contains(item)) return null;
            if (item.template.type == 21) {
                if (linhThu != null) return null;
                linhThu = item;
            } else if (item.template.id == ConstItem.NGOC_RONG_1_SAO) {
                if (dragonBall != null) return null;
                dragonBall = item;
            } else {
                return null;
            }
        }
        if (linhThu == null || dragonBall == null || linhThu.itemOptions == null) return null;
        List<ItemOption> stats = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        for (ItemOption option : linhThu.itemOptions) {
            if (option == null || option.optionTemplate == null) return null;
            if (isStatOption(option.optionTemplate.id)) {
                if (option.param < 0 || option.param > 0xffff || !seen.add(option.optionTemplate.id)) return null;
                stats.add(option);
            }
        }
        if (linhThu.itemOptions.size() + STAT_OPTIONS.length - stats.size() > 255) return null;
        return new Materials(linhThu, dragonBall, stats);
    }

    private static int remainderSlot(Player player, Materials materials) {
        for (int i = 0; i < player.inventory.itemsBag.size(); i++) {
            Item item = player.inventory.itemsBag.get(i);
            if (item == null || !item.isNotNullItem()) return i;
        }
        // The final dragon ball frees its own slot during this same transaction.
        return materials.dragonBall().quantity == 1 ? player.inventory.itemsBag.indexOf(materials.dragonBall()) : -1;
    }

    private static boolean hasLowStat(Materials materials) {
        return materials.stats().stream().anyMatch(option -> option.param < MIN_STAT_PERCENT);
    }

    public static boolean checkLocation(Player player) {
        if (player.zone != null && player.zone.map != null && player.zone.map.mapId == 5) return true;
        Service.gI().sendThongBao(player, "Chỉ có thể vượt giới hạn pet tại Bà Hạt Mít đảo Kame.");
        return false;
    }

    public static void showInfoCombine(Player player) {
        if (!checkLocation(player)) return;
        synchronized (player.inventory.itemsBag) {
            Materials materials = findMaterials(player);
            if (materials == null) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, REQUIREMENTS, "Đóng");
                return;
            }
            if (hasLowStat(materials)) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Không thể vượt giới hạn. " + MIN_STAT_RULE, "Đóng");
                return;
            }
            if (materials.linhThu().quantity > 1 && remainderSlot(player, materials) < 0) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, BAG_SPACE, "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "|2|Vượt giới hạn: " + materials.linhThu().template.name
                    + "\n|1|Nguyên liệu: 1 Ngọc Rồng 1 sao mỗi lần thử"
                    + "\n|7|Tỉ lệ thành công: " + SUCCESS_RATE + "%"
                    + "\n|7|" + MIN_STAT_RULE
                    + "\n|1|Thành công: +2% SĐ, HP, KI và SĐ chí mạng"
                    + "\nChưa có dòng nào sẽ thêm dòng đó với 2%."
                    + "\n|7|Thất bại: -2% SĐ, HP, KI và SĐ chí mạng"
                    + "\nGiữ nguyên các option khác."
                    + "\nGiữ nguyên pet, hạn sử dụng và khóa vật phẩm."
                    + "\nGiữ pet và Ngọc Rồng còn lại trong ô để nâng tiếp."
                    + "\nChọn 10 lần: xét tỉ lệ và trừ 1 Ngọc Rồng riêng mỗi lượt, dừng khi thiếu nguyên liệu hoặc có chỉ số dưới 5%."
                    + (materials.linhThu().quantity > 1 ? "\nPet đang xếp chồng: chỉ tách 1 con để nâng, các con còn lại giữ nguyên." : "")
                    + "\nCó thể nâng cấp nhiều lần, không giới hạn số lần nâng.",
                    "Nâng cấp\n1 lần", "Nâng cấp\n10 lần", "Từ chối");
        }
    }

    public static void nangCap(Player player) {
        nangCap(player, 1);
    }

    public static void nangCap(Player player, int attempts) {
        nangCap(player, attempts, () -> Util.isTrue(SUCCESS_RATE, 100));
    }

    static void nangCap(Player player, BooleanSupplier successRoll) {
        nangCap(player, 1, successRoll);
    }

    static void nangCap(Player player, int attempts, BooleanSupplier successRoll) {
        if (!checkLocation(player)) return;
        CombineBatch.run(player, attempts, false, () -> {
            Materials materials = findMaterials(player);
            if (materials == null) {
                return CombineBatch.Result.blocked(REQUIREMENTS);
            }
            if (hasLowStat(materials)) {
                return CombineBatch.Result.blocked("Không thể vượt giới hạn tiếp. " + MIN_STAT_RULE);
            }
            int remainderIndex = materials.linhThu().quantity > 1 ? remainderSlot(player, materials) : -1;
            if (materials.linhThu().quantity > 1 && remainderIndex < 0) {
                return CombineBatch.Result.blocked(BAG_SPACE);
            }
            boolean success = successRoll.getAsBoolean();
            // Item options are unsigned 16-bit values on the existing client protocol.
            if (success && materials.stats().stream().anyMatch(option -> option.param > 0xffff - BONUS)) {
                return CombineBatch.Result.blocked("Chỉ số pet quá lớn, không thể nâng thêm.");
            }
            Item remainder = null;
            if (materials.linhThu().quantity > 1) {
                remainder = ItemService.gI().copyItem(materials.linhThu());
                remainder.quantity--;
                remainder.message = materials.linhThu().message;
            }
            if (success) {
                for (int id : STAT_OPTIONS) materials.linhThu().addOptionParam(id, BONUS);
            } else {
                for (ItemOption option : materials.stats()) option.param = Math.max(0, option.param - BONUS);
            }
            if (remainder != null) materials.linhThu().quantity = 1;
            InventoryService.gI().subQuantityItemsBag(player, materials.dragonBall(), 1);
            // Place directly so an unchanged failure cannot merge the selected pet back into its stack.
            if (remainder != null) player.inventory.itemsBag.set(remainderIndex, remainder);
            Item linhThu = materials.linhThu();
            linhThu.info = linhThu.getInfo();
            linhThu.content = linhThu.getContent();
            return new CombineBatch.Result(success, success
                    ? "Vượt giới hạn thành công: +2% SĐ, HP, KI và SĐ chí mạng của pet."
                    : "Vượt giới hạn thất bại: -2% SĐ, HP, KI và SĐ chí mạng của pet.");
        });
    }
}
