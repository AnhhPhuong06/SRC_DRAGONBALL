package models.combine.list;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import models.item.Item;
import models.player.Player;
import services.player.InventoryService;

/** Extra materials are required when filling the eighth existing crystal socket. */
record EighthStarMaterials(Item hematite, Item chisel, String error) {
    static final int HEMATITE_ID = 1019;
    static final int CHISEL_ID = 1020;
    static final String REQUIREMENTS = "Ép viên sao thứ 8 cần thêm 1 Dùi đục và 1 Đá Hematit.";

    static EighthStarMaterials check(Player player, Item equipment, Item stone) {
        if (equipment.quantity != 1 || equipment.itemOptions == null || stone.itemOptions == null)
            return invalid("Trang bị hoặc đá pha lê không hợp lệ.");
        int used = 0, slots = 0;
        for (var option : equipment.itemOptions) {
            if (option == null || option.optionTemplate == null) return invalid("Dữ liệu trang bị không hợp lệ.");
            if (option.optionTemplate.id == 102 && (++used > 1 || option.param < 0)) return invalid("Số sao đã ép không hợp lệ.");
            if (option.optionTemplate.id == 107 && (++slots > 1 || option.param < 0)) return invalid("Số ô sao không hợp lệ.");
        }
        boolean eighth = equipment.getOptionParam(102) == 7;
        Item hematite = null, chisel = null;
        Set<Item> seen = Collections.newSetFromMap(new IdentityHashMap<>());
        for (Item item : player.combineNew.itemsCombine) {
            if (item == null || !item.isNotNullItem() || item.quantity < 1 || !seen.add(item)
                    || !player.inventory.itemsBag.contains(item)) return invalid("Nguyên liệu không còn trong hành trang hoặc đã chọn trùng.");
            if (item == equipment || item == stone) continue;
            if (item.template.id == HEMATITE_ID && hematite == null) hematite = item;
            else if (item.template.id == CHISEL_ID && chisel == null) chisel = item;
            else return invalid("Nguyên liệu ép sao không phù hợp.");
        }
        if (eighth && (hematite == null || chisel == null)) return invalid(REQUIREMENTS);
        if (!eighth && (hematite != null || chisel != null)) return invalid("Chỉ viên sao thứ 8 cần Dùi đục và Đá Hematit.");
        return new EighthStarMaterials(hematite, chisel, null);
    }
    private static EighthStarMaterials invalid(String error) { return new EighthStarMaterials(null, null, error); }
    String instructions() { return hematite == null ? "" : "\n|1|Cần thêm 1 Dùi đục + 1 Đá Hematit\nÉp thành công sẽ trừ mỗi loại 1 món."; }
    void consume(Player player) {
        if (hematite == null) return;
        InventoryService.gI().subQuantityItemsBag(player, hematite, 1);
        InventoryService.gI().subQuantityItemsBag(player, chisel, 1);
        player.combineNew.itemsCombine.clear();
    }
}
