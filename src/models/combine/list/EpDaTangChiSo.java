package models.combine.list;


import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import services.CombineService;
import services.ItemService;
import services.Service;
import services.player.InventoryService;

public class EpDaTangChiSo {

    private static final int[][] DA_OPTIONS = {
        {1364, 103, 7}, // Đá KI: +7% KI
        {1365, 101, 7}, // Đá SMTN: +7% TN,SM
        {1366, 50, 5},  // Đá SD: +5% sức đánh
        {1367, 5, 3},   // Đá SDCM: +3% sức đánh chí mạng
        {1368, 77, 7}   // Đá HP: +7% HP
    };

    private static int[] getDaOption(Item item) {
        if (item != null && item.isNotNullItem()) {
            for (int[] da : DA_OPTIONS) {
                if (item.template.id == da[0]) {
                    return da;
                }
            }
        }
        return null;
    }

    private static boolean isTrangBi(Item item) {
        return item != null && item.isNotNullItem()
                && (item.template.type <= 5 || item.template.type == 32);
    }

    public static boolean hasDaTangChiSo(Player player) {
        for (Item item : player.combineNew.itemsCombine) {
            if (getDaOption(item) != null) {
                return true;
            }
        }
        return false;
    }

    public static void showInfoCombine(Player player) {
        synchronized (player.inventory.itemsBag) { showInfoLocked(player); }
    }

    private static void showInfoLocked(Player player) {
        if (player.combineNew.itemsCombine.size() == 2 || player.combineNew.itemsCombine.size() == 4) {
            Item trangBi = null;
            Item daItem = null;
            int[] da = null;
            for (Item item : player.combineNew.itemsCombine) {
                if (isTrangBi(item)) {
                    trangBi = item;
                } else if (getDaOption(item) != null) {
                    daItem = item;
                    da = getDaOption(item);
                }
            }
            if (trangBi != null && da != null) {
                EighthStarMaterials extra = EighthStarMaterials.check(player, trangBi, daItem);
                if (extra.error() != null) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU, extra.error(), "Đóng");
                    return;
                }
                int star = 0;
                int starEmpty = 0;
                for (Item.ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id == 102) {
                        star = io.param;
                    } else if (io.optionTemplate.id == 107) {
                        starEmpty = io.param;
                    }
                }
                if (starEmpty == 0) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Trang bị chưa có lỗ, hãy pha lê hóa trang bị trước khi ép đá", "Đóng");
                    return;
                }
                if (star >= starEmpty) {
                    CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                            "Trang bị đã ép đủ " + starEmpty + " lỗ, không thể ép thêm", "Đóng");
                    return;
                }
                String npcSay = trangBi.template.name + "\n|2|";
                for (Item.ItemOption io : trangBi.itemOptions) {
                    if (io.optionTemplate.id != 102) {
                        npcSay += io.getOptionString() + "\n";
                    }
                }
                npcSay += "|7|Ép thêm: " + ItemService.gI().getItemOptionTemplate(da[1]).name.replaceAll("#", "" + da[2]) + "\n";
                npcSay += "|1|Lỗ đã dùng: " + star + "/" + starEmpty + extra.instructions();
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.MENU_START_COMBINE, npcSay, "Ép đá");
            } else {
                CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                        "Cần 1 trang bị (áo, quần, găng, giày, rađa) và 1 viên đá tăng chỉ số", "Đóng");
            }
        } else {
            CombineService.gI().baHatMit.createOtherMenu(player, ConstNpc.IGNORE_MENU,
                    "Cần 1 trang bị (áo, quần, găng, giày, rađa) và 1 viên đá tăng chỉ số", "Đóng");
        }
    }

    public static void epDa(Player player) {
        synchronized (player.inventory.itemsBag) { epDaLocked(player); }
    }

    private static void epDaLocked(Player player) {
        if (player.combineNew.itemsCombine.size() != 2 && player.combineNew.itemsCombine.size() != 4) {
            Service.gI().sendThongBao(player, "Thiếu nguyên liệu");
            return;
        }
        Item trangBi = null;
        Item daItem = null;
        int[] da = null;
        for (Item item : player.combineNew.itemsCombine) {
            if (isTrangBi(item)) {
                trangBi = item;
            } else if (getDaOption(item) != null) {
                daItem = item;
                da = getDaOption(item);
            }
        }
        if (trangBi == null || da == null || daItem.quantity < 1) {
            Service.gI().sendThongBao(player, "Cần 1 trang bị và 1 viên đá tăng chỉ số");
            return;
        }
        EighthStarMaterials extra = EighthStarMaterials.check(player, trangBi, daItem);
        if (extra.error() != null) {
            Service.gI().sendThongBao(player, extra.error());
            return;
        }
        boolean hadOption30 = trangBi.isHaveOption(30);
        int star = 0;
        int starEmpty = 0;
        Item.ItemOption optionStar = null;
        for (Item.ItemOption io : trangBi.itemOptions) {
            if (io.optionTemplate.id == 102) {
                star = io.param;
                optionStar = io;
            } else if (io.optionTemplate.id == 107) {
                starEmpty = io.param;
            }
        }
        if (starEmpty == 0) {
            Service.gI().sendThongBao(player, "Trang bị chưa có lỗ, hãy pha lê hóa trang bị trước khi ép đá");
            return;
        }
        if (star >= starEmpty) {
            Service.gI().sendThongBao(player, "Trang bị đã ép đủ " + starEmpty + " lỗ, không thể ép thêm");
            return;
        }
        int optionId = da[1];
        int param = da[2];
        Item.ItemOption option = null;
        for (Item.ItemOption io : trangBi.itemOptions) {
            if (io.optionTemplate.id == optionId) {
                option = io;
                break;
            }
        }
        if (option != null) {
            option.param += param;
        } else {
            trangBi.itemOptions.add(new Item.ItemOption(optionId, param));
        }
        if (optionStar != null) {
            optionStar.param++;
        } else {
            trangBi.itemOptions.add(new Item.ItemOption(102, 1));
        }
        if (hadOption30 && !trangBi.isHaveOption(30)) {
            trangBi.itemOptions.add(new Item.ItemOption(30, 0));
        }
        extra.consume(player);
        InventoryService.gI().subQuantityItemsBag(player, daItem, 1);
        CombineService.gI().sendEffectSuccessCombine(player);
        InventoryService.gI().sendItemBags(player);
        Service.gI().sendThongBao(player, "Ép thành công! " + trangBi.template.name + " được cộng thêm "
                + ItemService.gI().getItemOptionTemplate(optionId).name.replaceAll("#", "" + param));
        CombineService.gI().reOpenItemCombine(player);
    }
}
