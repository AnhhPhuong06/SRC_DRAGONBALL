import consts.ConstNpc;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.npc.npc_list.OngGohan;
import models.player.Player;
import models.task.TaskMain;
import network.io.Message;
import network.session.MySession;
import server.Manager;
import services.ItemService;
import utils.Util;
import sun.misc.Unsafe;

/** Exercise real Gohan menus and inventory stacking; billing is simulated, with no account writes. */
public class GohanExchangeTest {
    private static int checks;
    private static Unsafe unsafe;
    private static final int[] PRICES = {10_000, 20_000, 50_000, 100_000, 200_000, 500_000, 1_000_000};
    private static final int[] IDS = {457, 568, 674};
    private static final int[] MENUS = {ConstNpc.MENU_THOI_VANG, ConstNpc.MENU_GOHAN_DOI_TRUNG, ConstNpc.MENU_GOHAN_DOI_DA_NGU_SAC};
    private static final int[][] QUANTITIES = {{2000, 4000, 10000, 20000, 40000, 100000, 200000},
        {100, 200, 500, 1000, 2000, 5000, 10000}, {200, 400, 1000, 2000, 4000, 10000, 20000}};

    public static void main(String[] args) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        unsafe = (Unsafe) field.get(null);
        for (int id = 0; id < 252; id++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, "option #", 0));
        for (int id = 0; id <= 674; id++) {
            ItemTemplate template = new ItemTemplate(); template.id = (short) id;
            template.type = 27; template.name = "Item " + id; template.description = "Test item";
            template.isUpToUp = true;
            Manager.ITEM_TEMPLATES.add(template);
        }
        TestNpc npc = new TestNpc();
        TestPlayer player = player();
        npc.openBaseMenu(player);
        check(npc.buttons.length == 7 && npc.buttons[2].contains("Quy đổi"), "Base menu keeps existing positions");
        npc.confirmMenu(player, 2);
        check(npc.buttons.length == 4 && player.idMark.getIndexMenu() == ConstNpc.MENU_OPEN_VND,
                "Category menu exposes gold, eggs, stones and close");
        check(npc.buttons[1].contains("568") && npc.buttons[2].contains("ngũ sắc"), "Correct new reward categories");

        for (int kind = 0; kind < 3; kind++) {
            for (int tier = 0; tier < 7; tier++) {
                player = player(); npc.reset();
                open(npc, player, kind);
                check(player.idMark.getIndexMenu() == MENUS[kind] && npc.buttons.length == 8, "Correct tier menu");
                for (int i = 0; i < 7; i++) {
                    check(npc.buttons[i].contains(Util.format(PRICES[i])) && npc.buttons[i].contains(Util.format(QUANTITIES[kind][i])),
                            "Displayed prices and quantities agree with requested rates");
                }
                npc.confirmMenu(player, tier);
                check(npc.charges == 1 && npc.lastPrice == PRICES[tier] && player.getSession().sotien == 2_000_000 - PRICES[tier],
                        "Exactly one charge at correct tier");
                check(count(player, IDS[kind]) == QUANTITIES[kind][tier], "Exact reward quantity through one million VNĐ");
                check(player.playerTask.taskdh.Nap == PRICES[tier], "Existing task progress maintained");
                check(player.inventory.gem == 123 && player.inventory.ruby == 456 && player.inventory.gold == 789,
                        "Inventory currencies preserved");
                check(player.bagPackets == 1 && readPacketQuantity(player.bagPacket, IDS[kind]) == QUANTITIES[kind][tier],
                        "Client inventory packet carries full reward quantity");
                if (kind == 0) {
                    Item gold = find(player, 457);
                    check(gold.getOptionParam(100) == 1 && gold.hasOption(86) && !gold.hasOption(30),
                            "Gold keeps existing options and tradability");
                }
                check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU && npc.text.contains("Số dư còn lại"),
                        "Receipt shows remaining balance and closes payment selection");
                npc.confirmMenu(player, tier);
                check(npc.charges == 1 && count(player, IDS[kind]) == QUANTITIES[kind][tier], "Replayed confirmation cannot charge twice");
            }
        }

        for (int kind = 0; kind < 3; kind++) {
            // Full bag can still receive a compatible stack.
            player = player(); npc.reset();
            Item existing = reward(kind, 100);
            player.inventory.itemsBag.set(0, existing);
            player.inventory.itemsBag.set(1, ItemService.gI().createNewItem((short) 20));
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(npc.charges == 1 && count(player, IDS[kind]) == 100 + QUANTITIES[kind][0], "Full bag merges compatible reward");
            check(player.inventory.itemsBag.contains(existing), "Existing stack object preserved");

            // Partial capacity must not mutate a real stack or debit the account.
            player = player(); npc.reset();
            int maximum = kind == 0 ? 500_000_000 : 99999;
            existing = reward(kind, maximum - 1);
            player.inventory.itemsBag.set(0, existing);
            player.inventory.itemsBag.set(1, ItemService.gI().createNewItem((short) 20));
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(npc.charges == 0 && existing.quantity == maximum - 1 && player.getSession().sotien == 2_000_000,
                    "Partial capacity never consumes funds or changes existing quantity");
            check(player.bagPackets == 0, "Failed capacity check does not publish partial inventory");

            player = player(); npc.reset();
            existing = reward(kind, maximum - 1);
            player.inventory.itemsBag.set(0, existing);
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(npc.charges == 1 && count(player, IDS[kind]) == maximum - 1 + QUANTITIES[kind][0],
                    "Overflow into empty slot preserves every reward");

            player = player(); npc.reset();
            player.inventory.itemsBag.set(0, ItemService.gI().createNewItem((short) 20));
            player.inventory.itemsBag.set(1, ItemService.gI().createNewItem((short) 21));
            open(npc, player, kind); npc.confirmMenu(player, 6);
            check(npc.charges == 0 && count(player, IDS[kind]) == 0, "Full incompatible bag rejects without charging");

            player = player(); npc.reset();
            player.getSession().sotien = 9999;
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(npc.charges == 0 && count(player, IDS[kind]) == 0 && player.getSession().sotien == 9999,
                    "Insufficient balance blocks before DB debit");

            player = player(); npc.reset(); npc.debitSucceeds = false;
            player.inventory.itemsBag.set(0, reward(kind, 100));
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(npc.charges == 1 && player.getSession().sotien == 2_000_000 && count(player, IDS[kind]) == 100,
                    "DB debit rejection leaves inventory and cached balance unchanged");
            check(player.playerTask.taskdh.Nap == 0 && player.bagPackets == 0, "Rejected debit has no task or bag changes");

            player = player(); npc.reset(); player.getSession().sotien = 10_000;
            open(npc, player, kind); npc.confirmMenu(player, 0);
            check(player.getSession().sotien == 0 && count(player, IDS[kind]) == QUANTITIES[kind][0], "Exact balance supported");

            for (int invalid : new int[]{-1, 7, 8, 100, Integer.MAX_VALUE}) {
                player = player(); npc.reset();
                open(npc, player, kind); npc.confirmMenu(player, invalid);
                check(npc.charges == 0 && player.getSession().sotien == 2_000_000 && count(player, IDS[kind]) == 0,
                        "Close/out-of-range selection has no cost");
            }

            player = player(); npc.reset();
            open(npc, player, kind);
            player.session = null;
            npc.confirmMenu(player, 0);
            check(npc.charges == 0 && count(player, IDS[kind]) == 0, "Missing session cannot exchange");

            player = player(); npc.reset();
            open(npc, player, kind);
            final TestPlayer shared = player;
            Thread first = new Thread(() -> npc.confirmMenu(shared, 0));
            Thread second = new Thread(() -> npc.confirmMenu(shared, 0));
            first.start(); second.start(); first.join(); second.join();
            check(npc.charges == 1 && player.getSession().sotien == 1_990_000 && count(player, IDS[kind]) == QUANTITIES[kind][0],
                    "Concurrent confirmations charge and grant once");
        }
        player = player(); npc.reset();
        player.idMark.setIndexMenu(ConstNpc.MENU_OPEN_VND); npc.confirmMenu(player, 3);
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU && npc.charges == 0, "Category close does not buy a reward");

        System.out.println("PASS: " + checks + " Gohan exchange tiers, billing outcomes, bag capacity, quantities and replay checks");
    }

    private static void open(TestNpc npc, Player player, int kind) {
        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
        npc.confirmMenu(player, 2);
        npc.confirmMenu(player, kind);
    }
    private static TestPlayer player() throws Exception {
        TestPlayer player = new TestPlayer();
        MySession session = (MySession) unsafe.allocateInstance(MySession.class);
        session.userId = 1; session.sotien = 2_000_000; session.version = 222; session.player = player;
        player.session = session;
        player.inventory.itemsBag.add(new Item()); player.inventory.itemsBag.add(new Item());
        player.inventory.gem = 123; player.inventory.ruby = 456; player.inventory.gold = 789;
        player.playerTask.taskMain = new TaskMain(); player.playerTask.taskMain.id = 999;
        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
        return player;
    }
    private static Item reward(int kind, int quantity) {
        Item item = ItemService.gI().createNewItem((short) IDS[kind], quantity);
        if (kind == 0) { item.addOptionParam(100, 1); item.addOptionParam(86, 0); }
        return item;
    }
    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).mapToInt(i -> i.quantity).sum();
    }
    private static Item find(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).findFirst().orElseThrow();
    }
    private static int readPacketQuantity(byte[] packet, int itemId) throws Exception {
        int total = 0;
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(packet))) {
            reader.readUnsignedByte(); int count = reader.readUnsignedByte();
            for (int slot = 0; slot < count; slot++) {
                int id = reader.readShort(); if (id == -1) continue;
                int quantity = reader.readInt(); reader.readUTF(); reader.readUTF();
                int options = reader.readUnsignedByte();
                for (int i = 0; i < options; i++) { reader.readUnsignedByte(); reader.readUnsignedShort(); }
                if (id == itemId) total += quantity;
            }
        }
        return total;
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static class TestPlayer extends Player {
        int bagPackets; byte[] bagPacket;
        @Override public void sendMessage(Message msg) {
            if (msg.command == -36) { bagPackets++; bagPacket = Arrays.copyOf(msg.getData(), msg.getData().length); }
        }
    }
    private static class TestNpc extends OngGohan {
        String[] buttons; String text; int charges; int lastPrice; boolean debitSucceeds = true;
        TestNpc() { super(21, 1, 0, 0, ConstNpc.ONG_GOHAN, 0); }
        void reset() { charges = 0; lastPrice = 0; debitSucceeds = true; }
        @Override public boolean canOpenNpc(Player player) { player.idMark.setNpcChose(this); return true; }
        @Override public void createOtherMenu(Player player, int index, String text, String... buttons) {
            this.text = text; this.buttons = buttons; super.createOtherMenu(player, index, text, buttons);
        }
        @Override protected boolean chargeExchange(Player player, int amount) {
            charges++; lastPrice = amount;
            if (!debitSucceeds) return false;
            player.getSession().sotien -= amount; return true;
        }
    }
}
