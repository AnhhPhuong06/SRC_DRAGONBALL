import consts.ConstNpc;
import java.util.Arrays;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import server.Manager;
import services.CombineService;
import services.ItemService;

/** Standalone menu navigation and reward regression checks without a running server. */
public class BaHatMitCliffMenuTest {
    private static int checks;

    public static void main(String[] args) {
        for (int id = 0; id < 250; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, "option #", 0));
        }
        for (short id = 0; id <= 1492; id++) {
            ItemTemplate template = new ItemTemplate();
            template.id = id;
            template.type = 27;
            template.gender = 3;
            template.name = "Item " + id;
            Manager.ITEM_TEMPLATES.add(template);
        }
        TestNpc npc = new TestNpc(42);
        String[] root = {"Bùa\nHỗ trợ", "Nâng cấp", "Sách\nTuyệt kỹ", "Sự kiện"};
        for (int mapId : new int[]{42, 43, 44, 84}) {
            npc.mapId = mapId;
            for (int variation = 0; variation < 8; variation++) {
                Player player = new Player();
                player.luotNhanBuaMienPhi = variation & 1;
                if ((variation & 2) != 0) player.inventory.itemsBag.add(item(1210));
                if ((variation & 4) != 0) player.inventory.itemsBag.add(item(1211));
                npc.openBaseMenu(player);
                check(Arrays.equals(npc.buttons, root), "Stable root on map " + mapId);
                for (int selection = 0; selection < 4; selection++) {
                    npc.openBaseMenu(player);
                    npc.confirmMenu(player, selection);
                    int expected = new int[]{ConstNpc.MENU_BHM_BUA, ConstNpc.MENU_BHM_NANG_CAP,
                        ConstNpc.SACH_TUYET_KY, ConstNpc.MENU_BHM_SU_KIEN}[selection];
                    check(player.idMark.getIndexMenu() == expected, "Correct submenu routing");
                    check(Arrays.stream(npc.buttons).allMatch(b -> b.split("\n").length <= 2), "Short button labels");
                    check(npc.buttons[npc.buttons.length - 1].equals("Quay lại"), "Submenu has back button");
                    npc.confirmMenu(player, npc.buttons.length - 1);
                    check(player.idMark.isBaseMenu() && Arrays.equals(npc.buttons, root), "Back to root");
                }
            }
        }

        npc.mapId = 42;
        Player player = new Player();
        int[] upgrades = {CombineService.NANG_CAP_VAT_PHAM, CombineService.NANG_CAP_BONG_TAI,
            CombineService.NANG_CAP_BONG_TAI_CAP3, CombineService.LAM_PHEP_NHAP_DA, CombineService.NHAP_NGOC_RONG};
        for (int selection = 0; selection < upgrades.length; selection++) {
            npc.openBaseMenu(player);
            npc.confirmMenu(player, 1);
            npc.confirmMenu(player, selection);
            check(player.combineNew.typeCombine == upgrades[selection], "Preserve upgrade action " + selection);
        }
        player.inventory.itemsBag.add(item(921));
        player.inventory.itemsBox.add(item(1492));
        npc.openBaseMenu(player);
        npc.confirmMenu(player, 1);
        check(npc.buttons[1].equals("Mở chỉ số\nPorata 2") && npc.buttons[2].equals("Mở chỉ số\nPorata 3"),
                "Porata labels follow owned accessories");
        npc.confirmMenu(player, 1);
        check(player.combineNew.typeCombine == CombineService.NANG_CHI_SO_BONG_TAI, "Existing Porata 2 stat unlock");
        npc.openBaseMenu(player);
        npc.confirmMenu(player, 1);
        npc.confirmMenu(player, 2);
        check(player.combineNew.typeCombine == CombineService.NANG_CHI_SO_BONG_TAI_CAP3, "Existing Porata 3 stat unlock");

        int[] books = {CombineService.GIAM_DINH_SACH, CombineService.TAY_SACH, CombineService.NANG_CAP_SACH_TUYET_KY,
            CombineService.PHUC_HOI_SACH, CombineService.PHAN_RA_SACH};
        for (int selection = 2; selection <= 6; selection++) {
            npc.openBaseMenu(player);
            npc.confirmMenu(player, 2);
            npc.confirmMenu(player, selection);
            check(player.combineNew.typeCombine == books[selection - 2], "Preserve book action " + selection);
        }
        npc.openBaseMenu(player);
        npc.confirmMenu(player, 0);
        npc.confirmMenu(player, 0);
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_OPTION_SHOP_BUA && npc.buttons.length == 4,
                "Charm duration selector");
        npc.confirmMenu(player, 3);
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_BHM_BUA, "Shop back to charms");
        player.luotNhanBuaMienPhi = 1;
        npc.confirmMenu(player, 1);
        check(player.luotNhanBuaMienPhi == 0 && charmTime(player) > 0, "Claim a free one-hour charm");
        long charmTime = charmTime(player);
        npc.confirmMenu(player, 1);
        check(charmTime(player) == charmTime, "Cannot claim twice");
        check(npc.buttons[1].contains("Đã nhận") && npc.buttons.length == 3, "Claimed state keeps menu positions");

        player = new Player();
        player.inventory.itemsBag.addAll(Arrays.asList(item(1210), item(1211)));
        npc.openBaseMenu(player);
        npc.confirmMenu(player, 3);
        npc.confirmMenu(player, 0);
        check(count(player, 1210) == 1 && count(player, 1211) == 1, "Full bag keeps event materials");
        player.inventory.itemsBag.add(new Item());
        npc.confirmMenu(player, 0);
        check(count(player, 1210) == 0 && count(player, 1211) == 1, "Exchange correct snowman");
        player.inventory.itemsBag.add(new Item());
        npc.confirmMenu(player, 1);
        check(count(player, 1211) == 0, "Exchange ice snowman");
        int held = (int) player.inventory.itemsBag.stream().filter(Item::isNotNullItem).count();
        npc.confirmMenu(player, 0);
        check(player.inventory.itemsBag.stream().filter(Item::isNotNullItem).count() == held,
                "Missing materials cannot exchange");

        npc.mapId = 5;
        npc.openBaseMenu(player);
        check(npc.buttons.length == 9 && npc.buttons[4].contains("Siêu hóa") && npc.buttons[5].contains("Phân rã")
                        && npc.buttons[6].contains("giới hạn"),
                "Kame menu preserves existing actions and appends limit break");
        npc.confirmMenu(player, 1);
        check(npc.buttons.length == 1 && npc.buttons[0].contains("Thường"), "VIP SKH stays removed");
        System.out.println("PASS: " + checks + " cliff menu, navigation and reward checks");
    }

    private static long charmTime(Player player) {
        return player.charms.tdTriTue + player.charms.tdManhMe + player.charms.tdDaTrau + player.charms.tdOaiHung
                + player.charms.tdBatTu + player.charms.tdDeoDai + player.charms.tdThuHut;
    }

    private static Item item(int id) { return ItemService.gI().createNewItem((short) id); }

    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id)
                .mapToInt(i -> i.quantity).sum();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static class TestNpc extends BaHatMit {
        String[] buttons;
        TestNpc(int mapId) { super(mapId, 1, 0, 0, ConstNpc.BA_HAT_MIT, 0); }
        @Override public boolean canOpenNpc(Player player) {
            player.idMark.setNpcChose(this);
            return true;
        }
        @Override public void createOtherMenu(Player player, int index, String text, String... buttons) {
            this.buttons = buttons;
            super.createOtherMenu(player, index, text, buttons);
        }
    }
}
