package models.combine.list;

import consts.ConstNpc;
import java.util.ArrayList;
import java.util.List;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.CombineService;
import services.ItemService;

/** Standalone regression check without a database/server. */
public class PhanRaDoThanLinhTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (int id = 0; id < 250; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, "option #", 0));
        }
        for (short id = 0; id <= 674; id++) {
            ItemTemplate template = new ItemTemplate();
            template.id = id;
            template.type = (byte) (id == 674 ? 27 : 0);
            template.isUpToUp = id == 674;
            template.name = "Item " + id;
            Manager.ITEM_TEMPLATES.add(template);
        }
        TestNpc npc = new TestNpc();

        // Accept one divine item from every equipment type and planet.
        for (int id = 555; id <= 567; id++) {
            TestPlayer player = recipe(id);
            List<Item> originals = new ArrayList<>(player.combineNew.itemsCombine);
            check(player.inventory.itemsBag.stream().noneMatch(i -> !i.isNotNullItem()), "Initially full bag");
            PhanRaDoThanLinh.phanRa(player, () -> true);
            check(count(player, 674) == 1 && divineCount(player) == 0, "Success reward for template " + id);
            check(originals.stream().noneMatch(player.inventory.itemsBag::contains), "Consume exactly one item");
            check(player.effects.contains(2) && player.combineNew.itemsCombine.isEmpty(), "Success effect and clear selection");
            check(player.inventory.gold == 1234 && player.inventory.ruby == 567, "No extra currency cost");
            PhanRaDoThanLinh.phanRa(player, () -> { throw new AssertionError("Replay must not roll"); });
            check(count(player, 674) == 1, "No replay reward");
        }
        TestPlayer player = recipe(555);
        PhanRaDoThanLinh.phanRa(player, () -> false);
        check(divineCount(player) == 0 && count(player, 674) == 0, "Failure consumes one item without reward");
        check(player.effects.contains(3) && player.combineNew.itemsCombine.isEmpty(), "Failure effect and clear selection");

        player = recipe(555);
        Item existingStone = ItemService.gI().createNewItem((short) 674, 5);
        existingStone.addOptionParam(73, 0);
        player.inventory.itemsBag.add(existingStone);
        PhanRaDoThanLinh.phanRa(player, () -> true);
        check(count(player, 674) == 6, "Exactly one stone added to an existing stack");

        for (int invalid = 0; invalid < 9; invalid++) {
            player = recipe(555);
            switch (invalid) {
                case 0 -> player.combineNew.itemsCombine.remove(0);
                case 1 -> {
                    Item extra = ItemService.gI().createNewItem((short) 561);
                    player.inventory.itemsBag.add(extra);
                    player.combineNew.itemsCombine.add(extra);
                }
                case 2 -> player.combineNew.itemsCombine.add(player.combineNew.itemsCombine.get(0));
                case 3 -> player.combineNew.itemsCombine.set(0, ItemService.gI().createNewItem((short) 567));
                case 4 -> player.inventory.itemsBag.remove(0);
                case 5 -> player.combineNew.itemsCombine.get(0).quantity = 0;
                case 6 -> player.combineNew.itemsCombine.get(0).quantity = 2;
                case 7 -> player.combineNew.itemsCombine.set(0, null);
                case 8 -> {
                    Item wrong = ItemService.gI().createNewItem((short) 674);
                    player.inventory.itemsBag.set(0, wrong);
                    player.combineNew.itemsCombine.set(0, wrong);
                }
            }
            int countBefore = divineCount(player);
            int stonesBefore = count(player, 674);
            PhanRaDoThanLinh.phanRa(player, () -> { throw new AssertionError("Invalid recipe must not roll"); });
            check(divineCount(player) == countBefore && count(player, 674) == stonesBefore,
                    "Invalid/stale selection has no effect: " + invalid);
        }

        player = recipe(555);
        npc.openBaseMenu(player);
        check(npc.buttons.length == 9 && npc.buttons[5].contains("Phân rã"), "Kame NPC menu entry");
        npc.confirmMenu(player, 5);
        check(player.combineNew.typeCombine == CombineService.PHAN_RA_DO_THAN_LINH, "NPC opens correct tab");
        try (var reader = new java.io.DataInputStream(new java.io.ByteArrayInputStream(player.openPacket))) {
            check(reader.readUnsignedByte() == 0, "Standard combine open packet");
            String instructions = reader.readUTF();
            String title = reader.readUTF();
            check(instructions.contains("Chọn 1 món") && instructions.contains("80%")
                    && instructions.contains("mất 1 món") && !instructions.contains("36%") && !instructions.contains("3 món"),
                    "Empty-panel instructions display one item and 80 percent");
            check(title.contains("Phân rã 1 món") && !title.contains("3 món"), "Panel title displays one item");
        }
        CombineService.gI().showInfoCombine(player, new int[]{0});
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE && npc.text.contains("80%"), "Preview odds");
        check(npc.text.contains("mất 1 món") && npc.text.contains("1 Đá ngũ sắc") && !npc.text.contains("3 món"), "Preview cost and reward");
        npc.confirmMenu(player, 1);
        check(divineCount(player) == 1 && count(player, 674) == 0, "Cancel has no cost");
        npc.confirmMenu(player, 0);
        check(divineCount(player) == 0 && count(player, 674) <= 1, "NPC confirmation executes dismantling");
        player = recipe(555);
        CombineService.gI().showInfoCombine(player, new int[]{0, 0});
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Duplicate preview blocked");
        CombineService.gI().showInfoCombine(player, new int[]{0});
        player.inventory.itemsBag.remove(0);
        npc.confirmMenu(player, 0);
        check(divineCount(player) == 0 && count(player, 674) == 0, "Revalidate bag after preview");

        for (int quantity : new int[]{2, 3}) {
            player = quantity == 2 ? recipe(555, 558) : recipe(555, 558, 567);
            PhanRaDoThanLinh.phanRa(player, () -> { throw new AssertionError("Multiple items must not roll"); });
            check(divineCount(player) == quantity && count(player, 674) == 0, "Reject multiple selected items without loss");
        }

        int successes = 0;
        for (int attempt = 0; attempt < 1000; attempt++) {
            player = recipe(555);
            player.combineNew.ratioCombine = 100;
            CombineService.gI().startCombine(player);
            successes += count(player, 674);
            check(divineCount(player) == 0 && count(player, 674) <= 1, "Production cost and reward");
        }
        check(successes >= 400 && successes <= 600, "Actual success rate near 50%: " + successes + "/1000");
        System.out.println("PASS: " + checks + " checks; production successes " + successes + "/1000");
    }

    private static TestPlayer recipe(int... ids) {
        TestPlayer player = new TestPlayer();
        player.inventory.gold = 1234;
        player.inventory.ruby = 567;
        for (int id : ids) player.inventory.itemsBag.add(ItemService.gI().createNewItem((short) id));
        player.combineNew.itemsCombine.addAll(player.inventory.itemsBag);
        player.combineNew.typeCombine = CombineService.PHAN_RA_DO_THAN_LINH;
        return player;
    }

    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id)
                .mapToInt(i -> i.quantity).sum();
    }

    private static int divineCount(Player player) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.isDTL())
                .mapToInt(i -> i.quantity).sum();
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static class TestPlayer extends Player {
        final List<Integer> effects = new ArrayList<>();
        byte[] openPacket;
        @Override public void sendMessage(Message message) {
            if (message.command == -81 && message.getData().length > 0) {
                effects.add((int) message.getData()[0]);
                if (message.getData()[0] == 0) openPacket = java.util.Arrays.copyOf(message.getData(), message.getData().length);
            }
        }
    }

    private static class TestNpc extends BaHatMit {
        String[] buttons;
        String text;
        TestNpc() { super(5, 1, 0, 0, ConstNpc.BA_HAT_MIT, 0); }
        @Override public boolean canOpenNpc(Player player) {
            player.idMark.setNpcChose(this);
            return true;
        }
        @Override public void createOtherMenu(Player player, int index, String text, String... buttons) {
            this.buttons = buttons;
            this.text = text;
            super.createOtherMenu(player, index, text, buttons);
        }
    }
}
