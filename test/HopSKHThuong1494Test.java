package services;

import consts.ConstNpc;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.npc.Npc;
import models.npc.NpcFactory;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.func.UseItem;

/** Standalone check using in-memory data; no database, account or server is started. */
public class HopSKHThuong1494Test {
    private static int checks;
    private static final int[][] SETS = {{127, 128, 129, 246}, {130, 131, 132}, {133, 134, 135}};

    public static void main(String[] args) throws Exception {
        for (int id = 0; id <= 250; id++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, "option #", 0));
        for (short id = 0; id <= 1495; id++) {
            ItemTemplate template = new ItemTemplate();
            template.id = id; template.type = 27; template.gender = 3; template.name = "item " + id;
            Manager.ITEM_TEMPLATES.add(template);
        }
        for (int planet = 0; planet < 3; planet++) {
            for (int slot = 0; slot < 5; slot++) {
                for (int id : UseItem.LIST_ITEM_CLOTHES[planet][slot]) {
                    ItemTemplate template = Manager.ITEM_TEMPLATES.get(id);
                    template.type = (byte) slot; template.gender = (byte) (slot == 4 ? 3 : planet);
                }
            }
        }
        NpcFactory.createNpcConMeo();
        Npc cat = Manager.NPCS.stream().filter(n -> n.tempId == ConstNpc.CON_MEO).findFirst().orElseThrow();

        // Exercise every slot, equipment tier and activation set for all three planets.
        for (int planet = 0; planet < 3; planet++) {
            for (int slot = 0; slot < 5; slot++) {
                int[] tiers = UseItem.LIST_ITEM_CLOTHES[planet][slot];
                check(tiers.length == 13 && tiers[tiers.length - 1] >= 555, "Full cloth-to-divine pool");
                for (int tier = 0; tier < tiers.length; tier++) {
                    for (int set = 0; set < SETS[planet].length; set++) {
                        TestPlayer player = player(2, true);
                        player.gender = (byte) ((planet + 1) % 3);
                        // Legacy selections must have no influence on the reward.
                        player.idMark.setPlanetSkh((planet + 2) % 3); player.idMark.setSkhOptionId(243);
                        int[] rolls = {slot, tier, set}; int[] cursor = {0};
                        ItemService.gI().openHopSKHThuong(player, planet, (min, max) -> {
                            int value = rolls[cursor[0]++];
                            check(value >= min && value <= max, "Random bounds"); return value;
                        });
                        List<Item> rewards = rewards(player);
                        check(rewards.size() == 1 && rewards.get(0).quantity == 1, "Exactly one equipment reward");
                        Item reward = rewards.get(0);
                        check(reward.template.id == tiers[tier] && reward.template.type == slot, "Chosen planet, slot and tier respected");
                        check(reward.template.gender == 3 || reward.template.gender == planet, "Reward gender follows chosen planet");
                        int option = SETS[planet][set];
                        check(reward.hasOption(option) && reward.hasOption(ItemService.gI().optionIdSKH(option)), "Complete activation pair");
                        check(reward.hasOption(30), "Normal activation-item lock retained");
                        int baseOption = new int[]{47, 6, 0, 7, 14}[slot];
                        check(reward.hasOption(baseOption) || reward.hasOption(slot == 1 ? 22 : slot == 3 ? 23 : baseOption), "Base stats exist for all tiers");
                        if (reward.isDTL()) check(reward.getOptionParam(21) == 18, "Divine equipment has divine power requirement");
                        check(boxes(player) == 1 && player.bagPackets == 1, "Consume one box and refresh bag once");
                        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Close selection after reward");
                        check(player.inventory.gold == 1234 && player.inventory.ruby == 567, "No unrequested fees");
                    }
                }
            }
        }

        for (int planet = 0; planet < 3; planet++) {
            TestPlayer player = player(2, true);
            UseItem.gI().openHopSKHThuong(player, box(player));
            check(player.idMark.getIndexMenu() == ConstNpc.MENU_OPEN_HOP_SKH_THUONG_PLANET && boxes(player) == 2,
                    "Using 1494 opens planet selector without consuming");
            try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(player.menuPacket))) {
                check(reader.readShort() == ConstNpc.CON_MEO, "Standard choice menu packet");
                String text = reader.readUTF();
                check(text.contains("ngẫu nhiên") && text.contains("thần linh"), "Menu describes random tier range");
                check(reader.readUnsignedByte() == 4, "Three planets and cancel only");
                check(reader.readUTF().equals("Trái Đất") && reader.readUTF().equals("Namếc")
                        && reader.readUTF().equals("Xayda") && reader.readUTF().equals("Từ chối"), "All planet buttons");
            }
            player.gender = (byte) ((planet + 1) % 3);
            cat.confirmMenu(player, planet);
            check(boxes(player) == 1 && rewards(player).size() == 1, "Planet click immediately opens one box");
            Item reward = rewards(player).get(0);
            check(reward.template.gender == 3 || reward.template.gender == planet, "Real random reward matches selected planet");
            check(Arrays.stream(SETS[planet]).anyMatch(reward::hasOption), "Real random set matches planet");
            cat.confirmMenu(player, planet);
            check(boxes(player) == 1 && rewards(player).size() == 1, "Repeated stale confirmation does not consume another box");
        }

        TestPlayer player = player(2, true);
        UseItem.gI().openHopSKHThuong(player, box(player)); cat.confirmMenu(player, 3);
        check(boxes(player) == 2 && rewards(player).isEmpty(), "Cancel has no cost");
        for (int planet : new int[]{-1, 3, 4, 255}) {
            ItemService.gI().openHopSKHThuong(player, planet, (min, max) -> { throw new AssertionError("Invalid planet must not roll"); });
            check(boxes(player) == 2 && rewards(player).isEmpty(), "Invalid selection has no effect");
        }
        player = player(2, false);
        ItemService.gI().openHopSKHThuong(player, 0, (min, max) -> { throw new AssertionError("Full bag must not roll"); });
        check(boxes(player) == 2 && rewards(player).isEmpty(), "Full bag never consumes box");
        player = player(2, true); player.inventory.itemsBag.clear(); player.inventory.itemsBag.add(new Item());
        ItemService.gI().openHopSKHThuong(player, 0, (min, max) -> { throw new AssertionError("Missing box must not roll"); });
        check(rewards(player).isEmpty(), "Missing box cannot grant reward");
        player = player(1, true);
        ItemService.gI().openHopSKHThuong(player, 2, (min, max) -> min);
        check(boxes(player) == 0 && rewards(player).size() == 1, "Last box exchanged for one reward");

        // A missing template must not eat a box before creating its reward.
        player = player(2, true);
        ItemTemplate original = Manager.ITEM_TEMPLATES.set(0, null);
        ItemService.gI().openHopSKHThuong(player, 0, (min, max) -> min);
        Manager.ITEM_TEMPLATES.set(0, original);
        check(boxes(player) == 2 && rewards(player).isEmpty(), "Missing reward template preserves box");
        System.out.println("PASS: " + checks + " reward-pool, activation, planet-menu and inventory checks");
    }

    private static TestPlayer player(int quantity, boolean space) {
        TestPlayer player = new TestPlayer(); player.isPlayer = true;
        player.inventory.gold = 1234; player.inventory.ruby = 567;
        player.inventory.itemsBag.add(ItemService.gI().createNewItem((short) 1494, quantity));
        if (space) player.inventory.itemsBag.add(new Item());
        return player;
    }
    private static Item box(Player p) { return p.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == 1494).findFirst().orElseThrow(); }
    private static int boxes(Player p) { return p.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == 1494).mapToInt(i -> i.quantity).sum(); }
    private static List<Item> rewards(Player p) { return p.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id != 1494).toList(); }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static class TestPlayer extends Player {
        byte[] menuPacket; int bagPackets;
        @Override public void sendMessage(Message message) {
            if (message.command == 32) menuPacket = Arrays.copyOf(message.getData(), message.getData().length);
            if (message.command == -36) bagPackets++;
        }
    }
}
