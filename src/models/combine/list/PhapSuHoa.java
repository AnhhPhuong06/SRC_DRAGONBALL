package models.combine.list;

import consts.ConstNpc;
import java.util.function.BooleanSupplier;
import java.util.function.IntUnaryOperator;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public final class PhapSuHoa {
    public static final int DA_PHAP_SU = 1455;
    public static final int BUA_TAY_PHAP_SU = 1456;
    public static final int GEM_COST = 200;
    public static final int MAX_TIMES = 8;
    public static final int BONUS = 3;
    public static final int MAX_STAT = 9;
    public static final int SUCCESS_RATE = 80;
    private static final String EQUIPMENT_TYPES = "";
    private static final int[] STAT_OPTIONS = {
        ItemService.MAGE_DAMAGE_PERCENT, ItemService.MAGE_HP_PERCENT,
        ItemService.MAGE_KI_PERCENT, ItemService.MAGE_CRITICAL_DAMAGE_PERCENT
    };

    private record Materials(Item equipment, Item material) {}

    private PhapSuHoa() {}

    public static String getInstructions(boolean reset) {
        if (reset) {
            return "Vào hành trang:\nChọn 1 món đã pháp sư hóa + 1 Bùa tẩy pháp sư\n"
                    + "Nhận " + EQUIPMENT_TYPES + "\n"
                    + "Xóa số lần và toàn bộ dòng pháp sư, giữ nguyên chỉ số khác\n"
                    + "Giữ vật phẩm và Bùa tẩy còn lại trong ô sau khi thực hiện\n"
                    + "Tỉ lệ thành công: 100%\nChi phí: 200 ngọc xanh mỗi lần";
        }
        return "Vào hành trang:\nChọn 1 trang bị + 1 Đá pháp sư\n"
                + "Nhận " + EQUIPMENT_TYPES + "\n"
                + "Thành công: ngẫu nhiên 1 dòng SĐ, HP, KI hoặc SĐCM Pháp Sư +3%\n"
                + "Thất bại giữ nguyên chỉ số và số lần nâng\n"
                + "Giữ vật phẩm và đá còn lại trong ô để nâng tiếp\n"
                + "Tỉ lệ thành công: " + SUCCESS_RATE + "%\nChi phí: 200 ngọc xanh và 1 đá mỗi lần thử";
    }

    public static boolean checkLocation(Player player) {
        if (player.zone != null && player.zone.map != null && player.zone.map.mapId == 5) return true;
        Service.gI().sendThongBao(player, "Chỉ có thể pháp sư hóa hoặc tẩy pháp sư tại Bà Hạt Mít đảo Kame.");
        return false;
    }

    private static boolean isMageOption(int id) {
        return id >= ItemService.MAGE_EQUIPMENT_COUNT && id <= ItemService.MAGE_CRITICAL_DAMAGE_PERCENT;
    }

    private static boolean isEquipmentType(int type) {
        return switch (type) {
            case 5, 11, 21, 23, 24 -> true;
            default -> false;
        };
    }

    private static Materials findMaterials(Player player, boolean reset) {
        if (player.combineNew.itemsCombine.size() != 2) return null;
        Item equipment = null;
        Item material = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity <= 0
                    || !player.inventory.itemsBag.contains(item)) return null;
            if (isEquipmentType(item.template.type)) {
                if (equipment != null || item.quantity != 1 || item.itemOptions == null) return null;
                equipment = item;
            } else if (item.template.id == (reset ? BUA_TAY_PHAP_SU : DA_PHAP_SU)) {
                if (material != null) return null;
                material = item;
            } else {
                return null;
            }
        }
        return equipment == null || material == null ? null : new Materials(equipment, material);
    }

    private static String validate(Player player, Materials materials, boolean reset) {
        if (materials == null) {
            return "Chọn 1 món: " + EQUIPMENT_TYPES + "\n"
                    + (reset ? "đã pháp sư hóa và 1 Bùa tẩy pháp sư." : "và 1 Đá pháp sư.");
        }
        Item equipment = materials.equipment();
        boolean hasMage = false;
        boolean[] seen = new boolean[5];
        for (ItemOption option : equipment.itemOptions) {
            if (option == null || option.optionTemplate == null) return "Dữ liệu trang bị không hợp lệ.";
            int id = option.optionTemplate.id;
            if (!isMageOption(id)) continue;
            hasMage = true;
            if (!reset) {
                int index = id - ItemService.MAGE_EQUIPMENT_COUNT;
                if (seen[index] || option.param < 0 || option.param > 0xffff) {
                    return "Dòng pháp sư trên trang bị không hợp lệ, hãy tẩy pháp sư trước.";
                }
                if (index != 0 && option.param > MAX_STAT) {
                    return " Hãy tẩy pháp sư trước khi nâng lại.";
                }
                seen[index] = true;
            }
        }
        if (reset && !hasMage) return "Món đồ này chưa được pháp sư hóa.";
        if (!reset && equipment.getOptionParam(ItemService.MAGE_EQUIPMENT_COUNT) >= MAX_TIMES) {
            return "Trang bị đã pháp sư hóa đủ 8 lần. Có thể tẩy pháp sư để nâng lại.";
        }
        if (!reset) {
            if (availableStats(equipment).length == 0) return "không thể nâng thêm.";
            int additional = seen[0] ? 0 : 1;
            for (int i = 1; i < seen.length; i++) {
                if (!seen[i]) { additional++; break; }
            }
            if (equipment.itemOptions.size() + additional > 255) return "Trang bị có quá nhiều dòng option, không thể thêm dòng pháp sư.";
        }
        if (player.inventory.gem < GEM_COST) return "Cần đủ 200 ngọc xanh để thực hiện.";
        return null;
    }

    private static int[] availableStats(Item equipment) {
        int[] available = new int[STAT_OPTIONS.length];
        int count = 0;
        for (int id : STAT_OPTIONS) {
            if (equipment.getOptionParam(id) < MAX_STAT) available[count++] = id;
        }
        return java.util.Arrays.copyOf(available, count);
    }

    public static void showInfoCombine(Player player) {
        if (!checkLocation(player)) return;
        boolean reset = player.combineNew.typeCombine == CombineService.TAY_PHAP_SU;
        synchronized (player.inventory.itemsBag) {
            Materials materials = findMaterials(player, reset);
            String problem = validate(player, materials, reset);
            if (problem != null) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, problem, "Đóng");
                return;
            }
            player.combineNew.gemCombine = GEM_COST;
            player.combineNew.ratioCombine = reset ? 100 : SUCCESS_RATE;
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "|2|" + (reset ? "Tẩy pháp sư: " : "Pháp sư hóa: ") + materials.equipment().template.name
                    + "\nSố lần đã pháp sư hóa: " + materials.equipment().getOptionParam(ItemService.MAGE_EQUIPMENT_COUNT) + "/8"
                    + "\n|1|" + (reset ? "Dùng 1 Bùa tẩy pháp sư, xóa các dòng pháp sư và số lần nâng.\nGiữ nguyên toàn bộ chỉ số khác."
                            : "Thành công ngẫu nhiên 1 dòng SĐ, HP, KI hoặc SĐCM Pháp Sư +3%"
                            + "\nThất bại giữ nguyên chỉ số và số lần nâng; vẫn mất đá và ngọc.")
                            + "\nGiữ vật phẩm và nguyên liệu còn lại trong ô sau khi thực hiện."
                    + "\n|7|Chi phí: 200 ngọc xanh\n|2|Tỉ lệ thành công: " + (reset ? 100 : SUCCESS_RATE) + "%",
                    reset ? "Tẩy pháp sư\n1 lần" : "Pháp sư hóa\n1 lần", "Nâng cấp\n10 lần", "Từ chối");
        }
    }

    public static void combine(Player player) {
        combine(player, 1);
    }

    public static void combine(Player player, int attempts) {
        combine(player, attempts, () -> Util.isTrue(SUCCESS_RATE, 100));
    }

    // Reset always succeeds; upgrades roll only after all recipe/cost/limit checks pass.
    static void combine(Player player, BooleanSupplier successRoll) {
        combine(player, 1, successRoll);
    }

    static void combine(Player player, int attempts, BooleanSupplier successRoll) {
        combine(player, attempts, successRoll, bound -> Util.nextInt(bound));
    }

    static void combine(Player player, int attempts, BooleanSupplier successRoll, IntUnaryOperator statRoll) {
        if (!checkLocation(player)) return;
        int type = player.combineNew.typeCombine;
        if (type != CombineService.PHAP_SU_HOA && type != CombineService.TAY_PHAP_SU) return;
        boolean reset = type == CombineService.TAY_PHAP_SU;
        CombineBatch.run(player, attempts, true, () -> {
            Materials materials = findMaterials(player, reset);
            String problem = validate(player, materials, reset);
            if (problem != null) {
                return CombineBatch.Result.blocked(problem);
            }
            Item equipment = materials.equipment();
            boolean success = reset || successRoll.getAsBoolean();
            String result;
            if (reset) {
                equipment.itemOptions.removeIf(option -> isMageOption(option.optionTemplate.id));
                result = "Tẩy pháp sư thành công. Đã xóa các dòng pháp sư và số lần nâng, giữ nguyên chỉ số khác.";
            } else if (success) {
                int[] available = availableStats(equipment);
                int optionId = available[statRoll.applyAsInt(available.length)];
                int bonus = Math.min(BONUS, MAX_STAT - equipment.getOptionParam(optionId));
                equipment.addOptionParam(optionId, bonus);
                equipment.addOptionParam(ItemService.MAGE_EQUIPMENT_COUNT, 1);
                String statName = switch (optionId) {
                    case ItemService.MAGE_DAMAGE_PERCENT -> "SĐ";
                    case ItemService.MAGE_HP_PERCENT -> "HP";
                    case ItemService.MAGE_KI_PERCENT -> "KI";
                    default -> "SĐCM";
                };
                result = "Pháp sư hóa thành công: +" + bonus + "% " + statName + " Pháp Sư"
                        + ". Số lần: " + equipment.getOptionParam(ItemService.MAGE_EQUIPMENT_COUNT) + "/8.";
            } else {
                result = "Pháp sư hóa thất bại.";
            }
            player.inventory.gem -= GEM_COST;
            InventoryService.gI().subQuantityItemsBag(player, materials.material(), 1);
            equipment.info = equipment.getInfo();
            equipment.content = equipment.getContent();
            return new CombineBatch.Result(success, result);
        });
    }
}
