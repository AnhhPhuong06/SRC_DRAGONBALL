package services;

import consts.ConstNpc;
import java.util.ArrayList;
import java.util.List;
import java.util.function.IntBinaryOperator;
import models.item.Item;
import models.npc.NpcFactory;
import models.player.Player;
import services.map.NpcService;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

/** SKH boxes: full sets or one random activated piece after choosing a planet. */
public final class ThienSuSKHBox {
    private static final int FULL_BOX = 1477, RANDOM_BOX = 1478;
    private static final int HUY_DIET_VIP_BOX = 1501, THIEN_SU_VIP_BOX = 1502;
    private static final int[][] EQUIPMENT = {
        {1048,1051,1054,1057,1060}, {1049,1052,1055,1058,1061}, {1050,1053,1056,1059,1062}
    };
    private static final int[][] DESTRUCTION_EQUIPMENT = {
        {650,651,657,658,656}, {652,653,659,660,656}, {654,655,661,662,656}
    };
    private static final int[][] SETS = {{127,128,129,246},{130,131,132},{133,134,135}};
    private static final String[][] NAMES = {
        {"Thiên Xin Hăng","Kirin","Sôngôku","Kaiôken"}, {"Picolo","Ốc tiêu","Pikkoro Daimao"}, {"Kakarot","Ca Đíc","Nappa"}
    };
    private record Selection(Item box, int boxId, int planet) {}

    private ThienSuSKHBox() {}

    public static void open(Player player, Item box) {
        if (player == null || !player.isPl() || box == null || !box.isNotNullItem()
                || !isSupportedBox(box.template.id)) return;
        synchronized (player.inventory.itemsBag) {
            Selection selection = new Selection(box, box.template.id, -1);
            if (!owned(player, selection) || !room(player, selection.boxId == FULL_BOX ? 5 : 1)) return;
            String name = boxName(selection.boxId);
            NpcFactory.PLAYERID_OBJECT.put(player.id, selection);
            NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_HOP_SKH_THIEN_SU_PLANET, -1,
                    "|0|" + name + "\n|7|Chọn hành tinh nhận đồ:\n"
                            + (selection.boxId == FULL_BOX
                                    ? "Sau đó chọn set kích hoạt, nhận đủ 5 món Thiên Sứ cùng set.\n"
                                            + vipOptionInfo(selection.boxId) + "\nCần 5 ô hành trang trống."
                                    : "Nhận ngẫu nhiên 1 món " + (selection.boxId == RANDOM_BOX || selection.boxId == THIEN_SU_VIP_BOX ? "Thiên Sứ" : "Hủy Diệt")
                                            + " và set kích hoạt thuộc hành tinh đã chọn.\n" + vipOptionInfo(selection.boxId)
                                            + "\nCần 1 ô hành trang trống."),
                    "Trái Đất", "Namếc", "Xayda", "Từ chối");
        }
    }

    public static void selectPlanet(Player player, int select) {
        selectPlanet(player, select, Util::nextInt);
    }

    static void selectPlanet(Player player, int select, IntBinaryOperator random) {
        synchronized (player.inventory.itemsBag) {
            if (player.idMark.getIndexMenu() != ConstNpc.MENU_HOP_SKH_THIEN_SU_PLANET) return;
            Selection selection = pending(player);
            if (selection == null) return;
            if (select < 0 || select > 2) { close(player, selection); return; }
            if (selection.boxId != FULL_BOX) {
                if (!room(player, 1)) return;
                int slot = random.applyAsInt(0, 4);
                int set = random.applyAsInt(0, SETS[select].length - 1);
                if (slot < 0 || slot > 4 || set < 0 || set >= SETS[select].length) return;
                grant(player, selection, select, set, slot, random);
            } else {
                Selection next = new Selection(selection.box, selection.boxId, select);
                NpcFactory.PLAYERID_OBJECT.put(player.id, next);
                String[] choices = new String[NAMES[select].length + 1];
                System.arraycopy(NAMES[select], 0, choices, 0, NAMES[select].length);
                choices[choices.length - 1] = "Từ chối";
                NpcService.gI().createMenuConMeo(player, ConstNpc.MENU_HOP_SKH_THIEN_SU_SET, -1,
                        "|0|Hộp SKH Thiên Sứ\n|7|Chọn set kích hoạt:\nNhận đủ áo, quần, găng, giày, rada Thiên Sứ cùng set.\n"
                                + vipOptionInfo(selection.boxId), choices);
            }
        }
    }

    public static void selectSet(Player player, int select) {
        synchronized (player.inventory.itemsBag) {
            if (player.idMark.getIndexMenu() != ConstNpc.MENU_HOP_SKH_THIEN_SU_SET) return;
            Selection selection = pending(player);
            if (selection == null) return;
            if (selection.boxId != FULL_BOX || selection.planet < 0 || selection.planet > 2
                    || select < 0 || select >= SETS[selection.planet].length) { close(player, selection); return; }
            grant(player, selection, selection.planet, select, -1, Util::nextInt);
        }
    }

    private static boolean owned(Player player, Selection selection) {
        return selection.box.isNotNullItem() && selection.box.quantity > 0 && selection.box.template.id == selection.boxId
                && player.inventory.itemsBag.stream().anyMatch(item -> item == selection.box);
    }

    private static Selection pending(Player player) {
        Object value = NpcFactory.PLAYERID_OBJECT.get(player.id);
        if (value instanceof Selection selection && owned(player, selection)) return selection;
        player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        if (value instanceof Selection) NpcFactory.PLAYERID_OBJECT.remove(player.id, value);
        Service.gI().sendThongBao(player, "Hộp không còn trong hành trang. Hãy dùng lại hộp để chọn quà.");
        return null;
    }

    private static boolean room(Player player, int required) {
        if (InventoryService.gI().getCountEmptyBag(player) >= required) return true;
        Service.gI().sendThongBao(player, "Cần ít nhất " + required + " ô hành trang trống để nhận quà.");
        return false;
    }

    private static void close(Player player, Selection selection) {
        player.idMark.setIndexMenu(ConstNpc.IGNORE_MENU);
        NpcFactory.PLAYERID_OBJECT.remove(player.id, selection);
    }

    private static void grant(Player player, Selection selection, int planet, int setIndex, int slot,
            IntBinaryOperator random) {
        int count = slot < 0 ? 5 : 1;
        if (!owned(player, selection) || !room(player, count)) return;
        ItemService items = ItemService.gI();
        List<Item> rewards = new ArrayList<>(count);
        try {
            // Create and validate every reward before changing any bag slot or consuming the box.
            for (int i = 0; i < count; i++) {
                int gearSlot = slot < 0 ? i : slot;
                int id = equipment(selection.boxId, planet, gearSlot);
                var template = items.getTemplate(id);
                if (template == null || template.type != gearSlot || (template.gender != planet && template.gender != 3)) {
                    throw new IllegalStateException("Invalid SKH equipment template " + id);
                }
                Item reward = items.randomCS(id, SETS[planet][setIndex]);
                if (reward == null || !reward.isNotNullItem() || reward.quantity != 1) {
                    throw new IllegalStateException("Could not create SKH equipment " + id);
                }
                if (gearSlot == 2 && isPercentOptionBox(selection.boxId)) {
                    int minAttack = selection.boxId == HUY_DIET_VIP_BOX ? 8000 : 11000;
                    int maxAttack = selection.boxId == HUY_DIET_VIP_BOX ? 10000 : 15000;
                    int attack = random.applyAsInt(minAttack, maxAttack);
                    if (attack < minAttack || attack > maxAttack) {
                        throw new IllegalStateException("Glove attack out of range: " + attack);
                    }
                    reward.itemOptions.removeIf(option -> option.optionTemplate.id == 0);
                    reward.itemOptions.add(new Item.ItemOption(0, attack));
                    long attackOptions = reward.itemOptions.stream().filter(option -> option.optionTemplate.id == 0).count();
                    if (attackOptions != 1 || reward.getOptionParam(0) < minAttack || reward.getOptionParam(0) > maxAttack) {
                        throw new IllegalStateException("Invalid VIP glove attack option");
                    }
                }
                boolean correctTier = selection.boxId == HUY_DIET_VIP_BOX ? reward != null && reward.isDHD()
                        : reward != null && reward.isDTS();
                if (reward == null || !reward.isNotNullItem() || reward.quantity != 1 || !correctTier
                        || reward.itemOptions.stream().anyMatch(option -> option == null || option.optionTemplate == null)) {
                    throw new IllegalStateException("Invalid SKH equipment options " + id);
                }
                if (gearSlot == 4 && isPercentOptionBox(selection.boxId)) {
                    reward.itemOptions.removeIf(option -> option.optionTemplate.id == 5);
                }
                if (gearSlot == 2 && isPercentOptionBox(selection.boxId)) {
                    reward.itemOptions.removeIf(option -> option.optionTemplate.id == 50
                            || option.optionTemplate.id == 77 || option.optionTemplate.id == 103);
                }
                if (isPercentOptionBox(selection.boxId)) {
                    int percent = randomVipPercent(selection.boxId, random);
                    int min = 1;
                    int max = selection.boxId == HUY_DIET_VIP_BOX ? 5 : 10;
                    if (percent < min || percent > max) throw new IllegalStateException("Invalid option 199 value");
                    var optionTemplate = items.getItemOptionTemplate(ItemService.PERCENT_SD_HP_KI);
                    if (optionTemplate == null || optionTemplate.id != ItemService.PERCENT_SD_HP_KI) {
                        throw new IllegalStateException("Missing option template 199");
                    }
                    reward.itemOptions.removeIf(option -> option.optionTemplate.id == ItemService.PERCENT_SD_HP_KI);
                    reward.itemOptions.add(new Item.ItemOption(optionTemplate, percent));
                }
                rewards.add(reward);
            }
        } catch (RuntimeException e) {
            Logger.logException(ThienSuSKHBox.class, e, "Lỗi tạo quà hộp SKH Thiên Sứ");
            Service.gI().sendThongBao(player, "Chưa tạo được trang bị Thiên Sứ. Hộp của bạn chưa bị trừ.");
            return;
        }
        List<Integer> positions = new ArrayList<>(count);
        for (int i = 0; i < player.inventory.itemsBag.size() && positions.size() < count; i++) {
            if (!player.inventory.itemsBag.get(i).isNotNullItem()) positions.add(i);
        }
        if (positions.size() < count) return;
        // Heavenly equipment has one piece per slot. Publish the complete prepared bundle under the bag lock.
        for (int i = 0; i < count; i++) player.inventory.itemsBag.set(positions.get(i), rewards.get(i));
        InventoryService.gI().subQuantityItemsBag(player, selection.box, 1);
        close(player, selection);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, count == 5
                ? "Bạn nhận được 5 món Thiên Sứ - Set " + NAMES[planet][setIndex] + "."
                : "Bạn nhận được " + rewards.get(0).template.name + " - Set " + NAMES[planet][setIndex] + ".");
    }

    private static boolean isSupportedBox(int id) {
        return id == FULL_BOX || id == RANDOM_BOX || id == HUY_DIET_VIP_BOX || id == THIEN_SU_VIP_BOX;
    }

    private static boolean isPercentOptionBox(int id) {
        return isSupportedBox(id);
    }

    private static String boxName(int id) {
        return switch (id) {
            case HUY_DIET_VIP_BOX -> "Hộp SKH Hủy diệt Vip";
            case THIEN_SU_VIP_BOX -> "Hộp SKH Thiên sứ Vip";
            default -> "Hộp SKH Thiên Sứ";
        };
    }

    private static String vipOptionInfo(int id) {
        return switch (id) {
            case HUY_DIET_VIP_BOX -> "Có thêm +1–5% SĐ, HP, KI.";
            case FULL_BOX, RANDOM_BOX, THIEN_SU_VIP_BOX -> "Option 199: 1–2% hay gặp; 3–4% may mắn; 5–6% cực may; 7–8% cực may mắn; 9–10% siêu may mắn.";
            default -> "";
        };
    }

    private static int equipment(int boxId, int planet, int slot) {
        return boxId == HUY_DIET_VIP_BOX ? DESTRUCTION_EQUIPMENT[planet][slot] : EQUIPMENT[planet][slot];
    }

    private static int randomVipPercent(int boxId, IntBinaryOperator random) {
        int roll = random.applyAsInt(0, 99);
        if (roll < 0 || roll > 99) return -1;
        if (boxId == HUY_DIET_VIP_BOX) {
            // 1%, 2%, 3%, 4%, 5% have respective chances 35%, 35%, 15%, 10%, 5%.
            return roll < 35 ? 1 : roll < 70 ? 2 : roll < 85 ? 3 : roll < 95 ? 4 : 5;
        }
        // Per-level chances decrease from 1% through 10%, grouped by the requested luck tiers.
        return roll < 30 ? 1 : roll < 55 ? 2 : roll < 68 ? 3 : roll < 79 ? 4
                : roll < 85 ? 5 : roll < 90 ? 6 : roll < 94 ? 7 : roll < 97 ? 8
                : roll < 99 ? 9 : 10;
    }
}
