import consts.ConstNpc;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.Arrays;
import models.map.Zone;
import models.npc.npc_list.ToriBot;
import models.player.Player;
import models.shop.Shop;
import models.shop.TabShop;
import network.io.Message;
import server.Manager;
import sun.misc.Unsafe;

/** Exercises NPC menus and the real shop packet without a database or game server. */
public class ToriBotShopTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (int id = 0; id <= 674; id++) {
            var template = new models.Template.ItemTemplate(); template.id = (short) id; template.gender = 3; template.type = 27;
            template.name = "Item " + id; if (id == 674) template.iconID = 6467; Manager.ITEM_TEMPLATES.add(template);
        }
        Shop shop = new Shop(); shop.id = 49; shop.npcId = ConstNpc.TORI_BOT;
        shop.tagName = ToriBot.ITEM_SHOP_TAG; shop.typeShop = 0;
        TabShop tab = new TabShop(); tab.id = 64; tab.shop = shop; tab.name = "Vật phẩm";
        shop.tabShops.add(tab); Manager.SHOPS.add(shop);

        ToriBot tori = new ToriBot(5, 1, 900, 408, ConstNpc.TORI_BOT, 10477);
        TestPlayer player = player(5);
        tori.openBaseMenu(player);
        try (DataInputStream reader = read(player.menu)) {
            check(reader.readUnsignedShort() == ConstNpc.TORI_BOT, "Tori-bot menu avatar");
            reader.readUTF(); check(reader.readUnsignedByte() == 2, "Shop and close buttons");
            check(reader.readUTF().equals("Shop\nvật phẩm") && reader.readUTF().equals("Đóng"), "Item shop menu labels");
        }
        tori.confirmMenu(player, 1);
        check(player.shop == null, "Close does not open shop");
        tori.confirmMenu(player, 0);
        check(ToriBot.ITEM_SHOP_TAG.equals(player.idMark.getTagNameShop()), "Correct shop tag");
        check(player.idMark.getShopOpen().npcId == ConstNpc.TORI_BOT, "Shop associated with Tori-bot");
        try (DataInputStream reader = read(player.shop)) {
            check(reader.readUnsignedByte() == 3 && reader.readUnsignedByte() == 1, "Standard item shop with one tab");
            check(reader.readUTF().equals("Vật phẩm") && reader.readUnsignedByte() == 0, "Empty editable item tab opens normally");
        }
        check(player.inventory.gold == 1234 && player.inventory.gem == 567, "Opening shop has no charge");
        int shopPackets = player.shopPackets;
        player.idMark.setIndexMenu(ConstNpc.BUY_VIP);
        tori.confirmMenu(player, 0);
        check(player.shopPackets == shopPackets, "Kame Tori does not process VIP menu actions");

        for (int mapId : new int[]{0, 7, 14}) {
            ToriBot original = new ToriBot(mapId, 1, 900, 408, ConstNpc.TORI_BOT, 10477);
            player = player(mapId); original.openBaseMenu(player);
            try (DataInputStream reader = read(player.menu)) {
                reader.readUnsignedShort(); reader.readUTF(); reader.readUnsignedByte();
                check(reader.readUTF().equals("Mua VIP"), "Existing village VIP menu retained");
            }
            original.confirmMenu(player, 0);
            check(player.idMark.getIndexMenu() == ConstNpc.BUY_VIP && player.shopPackets == 0, "Village VIP handler retained");
        }
        player = player(42);
        tori.openBaseMenu(player); tori.confirmMenu(player, 0);
        check(player.menu == null && player.shop == null, "Cannot open Kame NPC from another map");
        System.out.println("PASS: " + checks + " Tori-bot menu, shop-packet and village VIP checks");
    }

    private static DataInputStream read(byte[] bytes) { return new DataInputStream(new ByteArrayInputStream(bytes)); }
    private static TestPlayer player(int mapId) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        models.map.Map map = (models.map.Map) ((Unsafe) f.get(null)).allocateInstance(models.map.Map.class);
        map.mapId = mapId;
        TestPlayer player = new TestPlayer(); player.zone = new Zone(map, 0, 20);
        player.location.x = 900; player.location.y = 408;
        player.inventory.gold = 1234; player.inventory.gem = 567;
        return player;
    }
    private static void check(boolean value, String message) { checks++; if (!value) throw new AssertionError(message); }
    private static class TestPlayer extends Player {
        byte[] menu; byte[] shop; int shopPackets;
        @Override public void sendMessage(Message message) {
            if (message.command == 32) menu = Arrays.copyOf(message.getData(), message.getData().length);
            if (message.command == -44) { shop = Arrays.copyOf(message.getData(), message.getData().length); shopPackets++; }
        }
    }
}
