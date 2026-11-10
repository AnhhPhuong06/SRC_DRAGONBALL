package models.combine.list;

import consts.ConstItem;
import consts.ConstNpc;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.IntSupplier;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;
import utils.Util;

public class SieuHoaCaiTrang {
    public static final int MAX_SUCCESSES = 20;
    private static final int[] OPTIONS = {50, 77, 103, 5};
    private static final String REQUIREMENTS = "Cần chọn 1 cải trang, 2 Đá ngũ sắc và 1 Quả Trứng Mabư.";

    private record Materials(Item costume, List<Item> stones, List<Item> eggs) {}

    private static int successCount(Item costume) {
        if (costume.itemOptions == null) return -1;
        int count = 0;
        boolean found = false;
        for (Item.ItemOption option : costume.itemOptions) {
            if (option == null || option.optionTemplate == null) return -1;
            if (option.optionTemplate.id == ItemService.SUPER_COSTUME_SUCCESS_COUNT) {
                if (found || option.param < 0) return -1;
                found = true;
                count = option.param;
            }
        }
        return count;
    }

    private static String limitProblem(Item costume) {
        int count = successCount(costume);
        if (count < 0) return "Dữ liệu số lần siêu hóa của cải trang không hợp lệ, vui lòng báo quản trị viên.";
        if (count >= MAX_SUCCESSES) return "Cải trang đã siêu hóa thành công đủ 20 lần, không thể nâng thêm.";
        return null;
    }

    private static Materials findMaterials(Player player) {
        Item costume = null;
        List<Item> stones = new ArrayList<>();
        List<Item> eggs = new ArrayList<>();
        Set<Item> selected = new HashSet<>();
        long stoneCount = 0;
        long eggCount = 0;
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity <= 0
                    || !player.inventory.itemsBag.contains(item) || !selected.add(item)) {
                return null;
            }
            if (item.template.type == 5) {
                if (costume != null || item.quantity != 1) return null;
                costume = item;
            } else if (item.template.id == ConstItem.DA_NGU_SAC) {
                stones.add(item);
                stoneCount += item.quantity;
            } else if (item.template.id == ConstItem.QUA_TRUNG) {
                eggs.add(item);
                eggCount += item.quantity;
            } else {
                return null;
            }
        }
        return costume != null && stoneCount >= 2 && eggCount >= 1
                ? new Materials(costume, stones, eggs) : null;
    }

    public static void showInfoCombine(Player player) {
        if (!checkLocation(player)) return;
        synchronized (player.inventory.itemsBag) {
            Materials materials = findMaterials(player);
            if (materials == null) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        REQUIREMENTS, "Đóng");
                return;
            }
            String problem = limitProblem(materials.costume());
            if (problem != null) {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, problem, "Đóng");
                return;
            }
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE,
                    "|2|Siêu hóa " + materials.costume().template.name
                    + "\nĐã siêu hóa thành công: " + successCount(materials.costume()) + "/" + MAX_SUCCESSES + " lần"
                    + "\n|1|Nguyên liệu: 2 Đá ngũ sắc + 1 Quả Trứng Mabư"
                    + "\n|7|Tỉ lệ thành công: 10%"
                    + "\n|1|Thành công: cộng thêm ngẫu nhiên 1–5% cho từng chỉ số SĐ, HP, KI, SĐ chí mạng."
                    + "\nChọn 10 lần: thử từng lượt, dừng khi thiếu nguyên liệu hoặc đủ 20 lần thành công."
                    + "\nMỗi cải trang tối đa 20 lần thành công. Thất bại không tính số lần.",
                    "Siêu hóa\n1 lần", "Nâng cấp\n10 lần", "Từ chối");
        }
    }

    public static void nangCap(Player player) {
        nangCap(player, 1);
    }

    public static void nangCap(Player player, int attempts) {
        nangCap(player, attempts, () -> Util.isTrue(5, 100), () -> Util.nextInt(1, 5));
    }

    static void nangCap(Player player, BooleanSupplier successRoll, IntSupplier bonusRoll) {
        nangCap(player, 1, successRoll, bonusRoll);
    }

    static void nangCap(Player player, int attempts, BooleanSupplier successRoll, IntSupplier bonusRoll) {
        if (!checkLocation(player)) return;
        CombineBatch.run(player, attempts, false, () -> {
            Materials materials = findMaterials(player);
            if (materials == null) {
                return CombineBatch.Result.blocked(REQUIREMENTS);
            }
            String problem = limitProblem(materials.costume());
            if (problem != null) {
                return CombineBatch.Result.blocked(problem);
            }
            boolean success = successRoll.getAsBoolean();
            consume(player, materials.stones(), 2);
            consume(player, materials.eggs(), 1);
            String result;
            if (success) {
                Item costume = materials.costume();
                int[] bonuses = new int[OPTIONS.length];
                for (int i = 0; i < OPTIONS.length; i++) {
                    bonuses[i] = bonusRoll.getAsInt();
                    costume.addOptionParam(OPTIONS[i], bonuses[i]);
                }
                costume.addOptionParam(ItemService.SUPER_COSTUME_SUCCESS_COUNT, 1);
                costume.info = costume.getInfo();
                costume.content = costume.getContent();
                result = "Siêu hóa thành công: +" + bonuses[0]
                        + "% SĐ, +" + bonuses[1] + "% HP, +" + bonuses[2] + "% KI, +"
                        + bonuses[3] + "% SĐ chí mạng. Số lần thành công: "
                        + successCount(costume) + "/" + MAX_SUCCESSES + ".";
            } else {
                result = "Siêu hóa thất bại, mất 2 Đá ngũ sắc và 1 Quả Trứng Mabư. Cải trang được giữ nguyên.";
            }
            return new CombineBatch.Result(success, result);
        });
    }

    public static boolean checkLocation(Player player) {
        if (player.zone != null && player.zone.map != null && player.zone.map.mapId == 5) return true;
        Service.gI().sendThongBao(player, "Chỉ có thể siêu hóa cải trang tại Bà Hạt Mít đảo Kame.");
        return false;
    }

    private static void consume(Player player, List<Item> stacks, int count) {
        for (Item stack : stacks) {
            int quantity = Math.min(count, stack.quantity);
            InventoryService.gI().subQuantityItemsBag(player, stack, quantity);
            count -= quantity;
            if (count == 0) return;
        }
    }
}
