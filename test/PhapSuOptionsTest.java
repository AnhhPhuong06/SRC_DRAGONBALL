import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.Template.SkillTemplate;
import models.intrinsic.Intrinsic;
import models.item.Item;
import models.player.Player;
import models.skill.Skill;
import server.Manager;
import services.ItemService;

/** Check actual equipped stats, option text, and recalculation without a database. */
public class PhapSuOptionsTest {
    private static int checks;

    public static void main(String[] args) {
        for (int id = 0; id < 252; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,
                    ItemService.resolveOptionName(id, "Option #"), 0));
        }
        ItemTemplate template = new ItemTemplate();
        template.id = 0;
        template.type = 0;
        template.gender = 3;
        template.name = "Test gear";
        Manager.ITEM_TEMPLATES.add(template);

        String[] expected = {"Trang Bị Pháp Sư 7 lần", "SĐ Pháp Sư 7%", "HP Pháp Sư 7%", "KI Pháp Sư 7%",
                "SĐCM Pháp Sư 7%"};
        for (int i = 0; i < expected.length; i++) {
            check(new Item.ItemOption(200 + i, 7).getOptionString().equals(expected[i]),
                    "Display parameter for option " + (200 + i));
        }
        Player baseline = equipped(new int[0], new int[0]);
        sameStats(equipped(new int[]{200}, new int[]{500}), baseline, "Counter has no stat effect");
        int[] regularIds = {50, 77, 103, 5};
        for (int i = 0; i < regularIds.length; i++) {
            for (int amount : new int[]{0, 1, 20, 100, 300}) {
                sameStats(equipped(new int[]{201 + i}, new int[]{amount}),
                        equipped(new int[]{regularIds[i]}, new int[]{amount}),
                        "Mage stat matches standard stat: " + (201 + i) + "/" + amount);
            }
        }
        Player mage = equipped(new int[]{200, 201, 202, 203}, new int[]{9, 15, 25, 35});
        Player normal = equipped(new int[]{50, 77, 103}, new int[]{15, 25, 35});
        sameStats(mage, normal, "All mage stats applied together");
        check(mage.nPoint.dame > baseline.nPoint.dame && mage.nPoint.hpMax > baseline.nPoint.hpMax
                && mage.nPoint.mpMax > baseline.nPoint.mpMax, "Actual stats increase");
        mage.nPoint.calPoint();
        sameStats(mage, normal, "Recalculation does not compound bonuses");
        sameStats(equipped(new int[]{201, 202, 203, 50, 77, 103}, new int[]{15, 25, 35, 10, 10, 10}),
                equipped(new int[]{50, 77, 103, 50, 77, 103}, new int[]{15, 25, 35, 10, 10, 10}),
                "Mage stats combine with existing percentage options");
        Player critical = equipped(new int[]{204, 5}, new int[]{25, 10});
        sameStats(critical, equipped(new int[]{5, 5}, new int[]{25, 10}),
                "Mage critical bonus combines with existing critical bonus");
        check(critical.nPoint.tlSDCM == 35 && critical.nPoint.crit == baseline.nPoint.crit,
                "Option 204 increases critical damage, preserving critical chance");
        critical.nPoint.calPoint();
        check(critical.nPoint.tlSDCM == 35, "Recalculation does not compound critical damage");
        critical.playerSkill.skillSelect = new Skill();
        critical.playerSkill.skillSelect.template = new SkillTemplate();
        critical.playerSkill.skillSelect.template.id = Skill.DRAGON;
        critical.playerSkill.skillSelect.damage = 100;
        critical.nPoint.dame = 100_000;
        critical.nPoint.crit = 100;
        long expectedCritical = 200_000L * 135 / 100;
        for (int attempt = 0; attempt < 50; attempt++) {
            int damage = critical.nPoint.getDameAttack(false);
            check(critical.nPoint.isCrit && damage >= expectedCritical * 95 / 100 - 2
                    && damage <= expectedCritical * 105 / 100 + 2, "Critical hits use mage bonus");
        }
        critical.nPoint.crit = 0;
        int damage = critical.nPoint.getDameAttack(false);
        check(!critical.nPoint.isCrit && damage >= 95_000 && damage <= 105_002,
                "Normal hits do not gain mage critical damage bonus");
        for (int[] equipment : new int[][]{{5,5}, {21,7}, {11,8}, {23,9}, {24,9}}) {
            int type=equipment[0], slot=equipment[1];
            Player accessoryMage=equipped(new int[]{200,201,202,203,204},new int[]{8,6,9,12,15},type,slot);
            Player accessoryNormal=equipped(new int[]{50,77,103,5},new int[]{6,9,12,15},type,slot);
            Player accessoryBase=equipped(new int[0],new int[0],type,slot);
            sameStats(accessoryMage,accessoryNormal,"New equipment mage stats match normal bonuses: "+type);
            check(accessoryMage.nPoint.dame>accessoryBase.nPoint.dame && accessoryMage.nPoint.hpMax>accessoryBase.nPoint.hpMax
                    && accessoryMage.nPoint.mpMax>accessoryBase.nPoint.mpMax && accessoryMage.nPoint.tlSDCM==15,
                    "New equipment receives actual combat bonuses in correct body slot: "+type);
            accessoryMage.nPoint.calPoint();
            sameStats(accessoryMage,accessoryNormal,"Recalculation does not compound accessory mage stats: "+type);
        }
        System.out.println("PASS: " + checks + " mage option display and equipped-stat checks");
    }

    private static Player equipped(int[] ids, int[] amounts) {
        return equipped(ids, amounts, 0, 0);
    }

    private static Player equipped(int[] ids, int[] amounts, int type, int slot) {
        Player player = new Player();
        player.isPlayer = true;
        player.nPoint.hpg = player.nPoint.mpg = player.nPoint.dameg = 10_000;
        player.nPoint.power = 1_000_000;
        player.playerIntrinsic.intrinsic = new Intrinsic();
        for (int i = 0; i < 14; i++) player.inventory.itemsBody.add(new Item());
        Item item = ItemService.gI().createNewItem((short) 0);
        ItemTemplate template = new ItemTemplate();
        template.id = 0; template.type = (byte) type; template.gender = 3; template.name = "Test equipment";
        item.template = template;
        for (int i = 0; i < ids.length; i++) item.itemOptions.add(new Item.ItemOption(ids[i], amounts[i]));
        player.inventory.itemsBody.set(slot, item);
        player.nPoint.calPoint();
        return player;
    }

    private static void sameStats(Player actual, Player expected, String message) {
        check(actual.nPoint.hpMax == expected.nPoint.hpMax && actual.nPoint.mpMax == expected.nPoint.mpMax
                && actual.nPoint.dame == expected.nPoint.dame && actual.nPoint.tlSDCM == expected.nPoint.tlSDCM, message);
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
