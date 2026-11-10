package managers.boss;

import consts.BossID;
import consts.BossStatus;
import consts.ConstItem;
import consts.ConstNpc;
import event.EventManager;
import event.event_list.Halloween;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.boss.Boss;
import models.boss.BossData;
import models.item.Item;
import models.map.Zone;
import models.npc.npc_list.QuyLaoKame;
import models.player.Player;
import server.Manager;
import server.ui.EventPanel;
import services.ItemService;
import sun.misc.Unsafe;

/** Run in an isolated fixture directory; no DB, server threads or restart. */
public class HalloweenEventStateTest {
    private static final int[] HALLOWEEN_BOSSES = {BossID.MATROI, BossID.DOI, BossID.BIMA, BossID.JANEMBA};
    private static int checks;

    public static void main(String[] args) throws Exception {
        check(!Files.isDirectory(Path.of("src")), "Use isolated fixture working directory");
        for (String selection : new String[]{"7", "1-7", "11-7", "", " 1 - 3 - 7 "}) {
            Files.writeString(Path.of("active_event.txt"), selection + "\n");
            EventManager.loadHalloweenConfig();
            check(EventManager.HALLOWEEN == (selection.equals("1-7") || selection.startsWith(" 1 ")),
                    "Load Halloween selection: " + selection);
        }
        Files.delete(Path.of("active_event.txt"));
        EventManager.loadHalloweenConfig();
        check(!EventManager.HALLOWEEN, "Missing selection defaults to Halloween off");
        BossManager realManager = BossManager.gI();
        for (int id : HALLOWEEN_BOSSES) check(realManager.createBoss(id) == null, "Disabled factory rejects boss " + id);

        RecordingHalloween event = new RecordingHalloween();
        event.boss();
        check(event.counts.isEmpty(), "Disabled event cannot spawn bosses");
        EventManager.HALLOWEEN = true;
        event.boss();
        check(event.counts.equals(Map.of(BossID.MATROI, 3, BossID.DOI, 3, BossID.BIMA, 3, BossID.JANEMBA, 1)),
                "Enabled event creates intended boss counts");

        RecordingManager manager = new RecordingManager();
        Field singleton = BossManager.class.getDeclaredField("instance");
        singleton.setAccessible(true);
        singleton.set(null, manager);
        Zone zone = zone();
        for (int id : HALLOWEEN_BOSSES) {
            FakeBoss boss = fakeBoss(id);
            boss.zone = zone;
            zone.getBosses().add(boss);
            zone.getHumanoids().add(boss);
            manager.addBoss(boss);
        }
        FakeBoss normalBoss = fakeBoss(BossID.FROST);
        normalBoss.zone = zone;
        zone.getBosses().add(normalBoss);
        manager.addBoss(normalBoss);
        EventManager.setHalloweenEnabled(false);
        check(manager.getBosses().equals(List.of(normalBoss)), "Disable removes only Halloween boss registrations");
        check(zone.getBosses().equals(List.of(normalBoss)), "Disable removes live Halloween bosses from zones");
        check(normalBoss.zone == zone && normalBoss.bossStatus == BossStatus.ACTIVE, "Other bosses stay active");
        EventManager.setHalloweenEnabled(true);
        check(manager.created == 10 && manager.getBosses().size() == 11, "Enable creates event bosses once");
        EventManager.setHalloweenEnabled(true);
        check(manager.created == 10, "Repeated enable cannot duplicate bosses");
        EventManager.HALLOWEEN = false;
        manager.updateBosses();
        check(manager.getBosses().equals(List.of(normalBoss)) && normalBoss.ticks == 1,
                "Boss tick cleans up direct flag changes and continues normal bosses");

        for (int id = 0; id < 250; id++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, "option #", 0));
        for (short id = 0; id <= 1497; id++) {
            ItemTemplate template = new ItemTemplate();
            template.id = id;
            template.type = 27;
            template.isUpToUp = true;
            template.name = "Item " + id;
            Manager.ITEM_TEMPLATES.add(template);
        }
        TestNpc npc = new TestNpc();
        Player player = new Player();
        npc.openBaseMenu(player);
        check(npc.buttons.length == 2, "No event exchange entry when disabled");
        npc.confirmMenu(player, 2);
        check(player.idMark.isBaseMenu(), "Old root selection cannot reopen event exchange");

        EventManager.HALLOWEEN = true;
        npc.openBaseMenu(player);
        check(npc.buttons.length == 3 && npc.buttons[2].contains("Sự kiện"), "Exchange entry appears when enabled");
        npc.confirmMenu(player, 2);
        check(player.idMark.getIndexMenu() == ConstNpc.DOI_QUA_SU_KIEN_BI_NGO, "Open enabled event exchange");
        Item pumpkins = ItemService.gI().createNewItem((short) ConstItem.BI_NGO, 200);
        Item candy = ItemService.gI().createNewItem((short) ConstItem.KEO_BAN_TAY, 200);
        player.inventory.itemsBag.addAll(List.of(pumpkins, candy, new Item()));
        EventManager.setHalloweenEnabled(false);
        for (int selection = 0; selection < 4; selection++) {
            npc.confirmMenu(player, selection);
            check(pumpkins.quantity == 200 && candy.quantity == 200,
                    "Stale exchange selection cannot consume materials: " + selection);
            check(player.inventory.itemsBag.stream().filter(Item::isNotNullItem).count() == 2,
                    "Stale exchange selection cannot grant rewards");
        }
        EventManager.HALLOWEEN = true;
        npc.confirmMenu(player, 1);
        check(pumpkins.quantity == 150 && count(player, ConstItem.GIO_KEO_BI_NGO) == 1,
                "Existing exchange recipe works after re-enabling");
        EventManager.HALLOWEEN = false;
        player.canReward = true;
        npc.openBaseMenu(player);
        check(npc.buttons.length == 1 && npc.buttons[0].contains("Giao"), "Unrelated reward menu preserved");
        EventPanel panel = new EventPanel();
        Method save = EventPanel.class.getDeclaredMethod("saveEventConfig", List.class);
        save.setAccessible(true);
        check((boolean) save.invoke(panel, List.of(1, 7)), "Panel saves event configuration");
        check(Files.readString(Path.of("active_event.txt")).trim().equals("1-7") && EventManager.HALLOWEEN,
                "Panel enables persisted Halloween state");
        int created = manager.created;
        save.invoke(panel, List.of(1, 7));
        check(manager.created == created, "Saving the same panel selection does not duplicate bosses");
        save.invoke(panel, List.of(7));
        check(!EventManager.HALLOWEEN && manager.getBosses().equals(List.of(normalBoss)),
                "Panel disable immediately removes event bosses without a test restart");
        Files.writeString(Path.of("active_event.txt"), "7\n");
        System.out.println("PASS: " + checks + " Halloween config, spawn, cleanup and exchange checks");
    }

    private static Zone zone() throws Exception {
        models.map.Map map = (models.map.Map) unsafe().allocateInstance(models.map.Map.class);
        map.mapId = 5;
        return new Zone(map, 0, 20);
    }

    private static FakeBoss fakeBoss(int id) throws Exception {
        FakeBoss boss = (FakeBoss) unsafe().allocateInstance(FakeBoss.class);
        Player defaults = new Player();
        for (Field field : Player.class.getFields()) {
            if (!Modifier.isStatic(field.getModifiers()) && !Modifier.isFinal(field.getModifiers())) field.set(boss, field.get(defaults));
        }
        boss.id = id;
        boss.isBoss = true;
        boss.bossStatus = BossStatus.ACTIVE;
        return boss;
    }

    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).mapToInt(i -> i.quantity).sum();
    }
    private static Unsafe unsafe() throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        return (Unsafe) field.get(null);
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static class FakeBoss extends Boss {
        int ticks;
        private FakeBoss() throws Exception { super(0, (BossData[]) null); }
        @Override public void update() { ticks++; }
    }
    private static class RecordingManager extends BossManager {
        int created;
        @Override public Boss createBoss(int id) {
            try {
                FakeBoss boss = fakeBoss(id);
                addBoss(boss);
                created++;
                return boss;
            } catch (Exception e) { throw new RuntimeException(e); }
        }
    }
    private static class RecordingHalloween extends Halloween {
        final Map<Integer, Integer> counts = new HashMap<>();
        @Override public void createBoss(int id, int... total) { counts.merge(id, total.length == 0 ? 1 : total[0], Integer::sum); }
    }
    private static class TestNpc extends QuyLaoKame {
        String[] buttons;
        TestNpc() { super(5, 1, 0, 0, ConstNpc.QUY_LAO_KAME, 0); }
        @Override public boolean canOpenNpc(Player player) { return true; }
        @Override public void createOtherMenu(Player player, int index, String text, String... buttons) {
            this.buttons = buttons;
            super.createOtherMenu(player, index, text, buttons);
        }
    }
}
