import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.ItemService;
import services.player.InventoryService;

/** Verify existing scrambled items, sparse legacy lines and actual inventory packets. */
public class MageOptionOrderTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (int id = 0; id < 252; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,
                    ItemService.resolveOptionName(id, "Option #"), 0));
        }
        permutations(new int[]{200,201,202,203,204}, 0);
        for (int mask = 0; mask < 32; mask++) {
            Item item = gear();
            item.itemOptions.add(new ItemOption(0,4075));
            List<Integer> expected = new ArrayList<>(List.of(0));
            for (int id = 204; id >= 200; id--) {
                if ((mask & (1 << (id - 200))) != 0) item.itemOptions.add(new ItemOption(id, id - 190));
            }
            for (int id = 200; id <= 204; id++) {
                if ((mask & (1 << (id - 200))) != 0) expected.add(id);
            }
            item.itemOptions.add(new ItemOption(30,0)); expected.add(30);
            verify(item, expected);
        }
        Item screenshot = gear();
        for (int id : new int[]{0,14,127,30,204,200,201,203}) screenshot.itemOptions.add(new ItemOption(id,id == 200 ? 8 : 6));
        List<Integer> expected = List.of(0,14,127,30,200,201,203,204);
        verify(screenshot, expected);
        check(!ids(screenshot.getDisplayOptions()).contains(202), "Missing HP remains missing; display never fabricates stats");
        Capture player = new Capture();
        player.isPet = true;
        player.inventory.itemsBag.add(screenshot);
        player.inventory.itemsBody.add(screenshot);
        for (int slot = 1; slot < 14; slot++) player.inventory.itemsBody.add(new Item());
        player.inventory.itemsBox.add(screenshot);
        InventoryService.gI().sendItemBags(player);
        InventoryService.gI().sendItemBody(player);
        InventoryService.gI().sendItemBox(player);
        for (int command : new int[]{-36,-37,-35}) verifyPacket(player.packets.get(command),command,screenshot,expected);
        verify(screenshot, expected);
        Item malformed = gear();
        malformed.itemOptions.add(null); malformed.itemOptions.add(new ItemOption());
        malformed.itemOptions.add(new ItemOption(204,9)); malformed.itemOptions.add(new ItemOption(200,3));
        check(ids(malformed.getDisplayOptions()).equals(List.of(200,204)), "Invalid entries skipped consistently with existing inventory packets");
        malformed.itemOptions = null;
        check(malformed.getDisplayOptions().isEmpty(), "Null option list displays no options");
        System.out.println("PASS: " + checks + " legacy/new mage display ordering, preserved values and bag/body/box packet checks");
    }

    private static void permutations(int[] values, int start) {
        if (start == values.length) {
            Item item = gear();
            item.itemOptions.add(new ItemOption(0,4075));
            for (int i = 0; i < values.length; i++) {
                item.itemOptions.add(new ItemOption(values[i], values[i] == 200 ? 8 : values[i] - 190));
                if (i == 0) item.itemOptions.add(new ItemOption(30,0));
                if (i == 2) item.itemOptions.add(new ItemOption(93,10));
            }
            verify(item,List.of(0,200,201,202,203,204,30,93));
            return;
        }
        for (int i = start; i < values.length; i++) {
            int temp = values[start]; values[start] = values[i]; values[i] = temp;
            permutations(values,start+1);
            temp = values[start]; values[start] = values[i]; values[i] = temp;
        }
    }

    private static void verify(Item item,List<Integer> expected) {
        List<ItemOption> stored = new ArrayList<>(item.itemOptions);
        List<Integer> params = stored.stream().map(o -> o.param).toList();
        List<ItemOption> display = item.getDisplayOptions();
        check(ids(display).equals(expected), "Counter then SD, HP, KI, SDCM; other options retain their order");
        check(item.getInfo().equals(String.join("", display.stream().map(ItemOption::getOptionString).toList())), "Info text matches transmitted order");
        String info = "|1|" + item.template.name + "\n|0|";
        for (ItemOption option : display) info += option.getOptionString() + "\n";
        check(item.getInfoItem().equals(info + "|2|" + item.template.description), "Detailed item text uses same order");
        check(item.itemOptions.equals(stored) && item.itemOptions.stream().map(o -> o.param).toList().equals(params), "Original order, option objects and parameters unchanged");
        display.clear();
        check(item.itemOptions.equals(stored), "Display list is independent of stored options");
    }

    private static void verifyPacket(byte[] bytes,int command,Item item,List<Integer> expected) throws Exception {
        check(bytes != null,"Inventory packet sent: " + command);
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(bytes))) {
            check(reader.readUnsignedByte() == 0,"Correct packet subtype");
            if (command == -37) reader.readShort();
            check(reader.readUnsignedByte() == (command == -37 ? 14 : 1)
                    && reader.readShort() == item.template.id, "Correct slot and item ID");
            check(reader.readInt() == item.quantity, "Quantity unchanged");
            check(reader.readUTF().equals(item.getInfo()) && reader.readUTF().equals(item.getContent()), "Info and content unchanged");
            int size = reader.readUnsignedByte();
            check(size == expected.size(), "Packet contains exactly the existing option count");
            for (int id : expected) {
                check(reader.readUnsignedByte() == id, "Correct wire option order");
                check(reader.readUnsignedShort() == item.getOptionParam(id), "Correct wire parameter");
            }
            if (command == -37) for (int slot = 1; slot < 14; slot++) check(reader.readShort() == -1, "Empty body slot preserved");
            check(reader.available() == 0,"No malformed packet bytes");
        }
    }

    private static Item gear() {
        Item item = new Item(); item.quantity = 1;
        item.template = new ItemTemplate(); item.template.id = 562; item.template.type = 2;
        item.template.name = "Găng Thần Xayda"; item.template.description = "Giúp bạn tăng sức đánh";
        return item;
    }
    private static List<Integer> ids(List<ItemOption> options) {return options.stream().map(o -> o.optionTemplate.id).toList();}
    private static void check(boolean value,String message) {checks++;if (!value) throw new AssertionError(message);}
    private static final class Capture extends Player {
        final Map<Integer,byte[]> packets = new HashMap<>();
        @Override public void sendMessage(Message message) {
            if (message.command == -36 || message.command == -37 || (message.command == -35 && message.getData()[0] == 0)) {
                packets.put((int)message.command,Arrays.copyOf(message.getData(),message.getData().length));
            }
        }
    }
}
