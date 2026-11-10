import data.DataGame;
import data.ItemData;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.Template.SkillTemplate;
import models.intrinsic.Intrinsic;
import models.item.Item;
import models.player.NPoint;
import models.player.Player;
import models.skill.Skill;
import network.io.Message;
import network.session.MySession;
import server.Manager;
import services.ItemService;
import services.player.InventoryService;
import sun.misc.Unsafe;

/** Checks real hidden-stat conversion, combat effect, and client option data without DB/network. */
public class RandomCriticalDamageOptionTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        for (int id = 0; id < 252; id++) {
            String name = id == 5 ? "+#% sức đánh chí mạng" : "Option #";
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, ItemService.resolveOptionName(id, name), 0));
        }
        ItemTemplate gear = new ItemTemplate();
        gear.id = 0;
        gear.type = 0;
        gear.gender = 3;
        gear.name = "Test gear";
        Manager.ITEM_TEMPLATES.add(gear);
        Player player = new Player();
        int min = 201;
        int max = 0;
        for (int attempt = 0; attempt < 5000; attempt++) {
            Item item = item(player, 194);
            check(item.getInfo().equals("Random +50-200% sức đánh chí mạng"), "Hidden stat label before reveal");
            check(InventoryService.gI().revealHiddenStats(player, item), "Reveal option 194");
            int bonus = item.getOptionParam(5);
            check(!item.hasOption(194) && item.itemOptions.size() == 1, "Replace marker with one real critical damage option");
            check(bonus >= 50 && bonus <= 200, "Bonus stays within 50-200 inclusive");
            min = Math.min(min, bonus);
            max = Math.max(max, bonus);
            check(!InventoryService.gI().revealHiddenStats(player, item) && item.getOptionParam(5) == bonus,
                    "Resolved item does not reroll or gain duplicate stats");
        }
        check(min == 50 && max == 200, "Both requested endpoints are reachable");
        int[] legacyOptions = {248, 249, 250, 251};
        int[] realOptions = {50, 77, 103, 5};
        for (int i = 0; i < legacyOptions.length; i++) {
            for (int attempt = 0; attempt < 100; attempt++) {
                Item item = item(player, legacyOptions[i]);
                InventoryService.gI().revealHiddenStats(player, item);
                int bonus = item.getOptionParam(realOptions[i]);
                check(bonus >= 1 && bonus <= 50 && !item.hasOption(legacyOptions[i]), "Preserve existing random options");
            }
        }
        Item combined = item(player, 194);
        combined.addOptionParam(5, 30);
        combined.addOptionParam(251, 0);
        combined.addOptionParam(93, 7);
        InventoryService.gI().revealHiddenStats(player, combined);
        int totalCriticalBonus = combined.itemOptions.stream().filter(o -> o.optionTemplate.id == 5).mapToInt(o -> o.param).sum();
        check(totalCriticalBonus >= 81 && totalCriticalBonus <= 280, "New and existing critical damage bonuses combine");
        check(combined.getOptionParam(93) == 7 && !combined.hasOption(194) && !combined.hasOption(251),
                "Keep unrelated item options");

        Method addOption = NPoint.class.getDeclaredMethod("addOption", Item.ItemOption.class);
        addOption.setAccessible(true);
        for (Item.ItemOption option : combined.itemOptions) addOption.invoke(player.nPoint, option);
        check(player.nPoint.tlSDCM == totalCriticalBonus, "Resolved bonus contributes to actual character stats");
        player.playerIntrinsic.intrinsic = new Intrinsic();
        player.playerSkill.skillSelect = new Skill();
        player.playerSkill.skillSelect.template = new SkillTemplate();
        player.playerSkill.skillSelect.template.id = Skill.DRAGON;
        player.playerSkill.skillSelect.damage = 100;
        player.nPoint.dame = 100_000;
        player.nPoint.crit = 100;
        long expectedCriticalDamage = 200_000L * (100 + totalCriticalBonus) / 100;
        for (int attempt = 0; attempt < 100; attempt++) {
            int damage = player.nPoint.getDameAttack(false);
            check(player.nPoint.isCrit && damage >= expectedCriticalDamage * 95 / 100 - 2
                    && damage <= expectedCriticalDamage * 105 / 100 + 2, "Critical hits use revealed damage bonus");
        }
        player.nPoint.crit = 0;
        int damage = player.nPoint.getDameAttack(false);
        check(!player.nPoint.isCrit && damage >= 95_000 && damage <= 105_002, "Normal hits do not gain critical damage bonus");

        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        FakeSession session = (FakeSession) ((Unsafe) field.get(null)).allocateInstance(FakeSession.class);
        Method sendOptions = ItemData.class.getDeclaredMethod("updateItemOptionItemplate", MySession.class);
        sendOptions.setAccessible(true);
        sendOptions.invoke(null, session);
        DataInputStream packet = new DataInputStream(new ByteArrayInputStream(session.data));
        check(packet.readUnsignedByte() == 8 && packet.readByte() == DataGame.vsItem && packet.readByte() == 0,
                "Client receives item option template update");
        int count = packet.readUnsignedByte();
        String name194 = null;
        for (int id = 0; id < count; id++) {
            String name = packet.readUTF();
            packet.readByte();
            if (id == 194) name194 = name;
        }
        check("Random +50-200% sức đánh chí mạng".equals(name194), "Client receives correct option 194 label");
        System.out.println("PASS: " + checks + " reveal, range, combat and client-template checks; observed range " + min + "-" + max);
    }

    private static Item item(Player player, int optionId) {
        Item item = ItemService.gI().createNewItem((short) 0);
        item.addOptionParam(optionId, 0);
        player.inventory.itemsBag.clear();
        player.inventory.itemsBag.add(item);
        return item;
    }
    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
    private static class FakeSession extends MySession {
        byte[] data;
        private FakeSession() { super(null); }
        @Override public void doSendMessage(Message message) { data = message.getData(); }
    }
}
