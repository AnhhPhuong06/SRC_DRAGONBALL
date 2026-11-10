import java.util.HashSet;
import java.util.List;
import java.util.Set;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.intrinsic.Intrinsic;
import models.item.Item;
import models.player.NPoint;
import models.player.Player;
import server.Manager;
import services.ItemService;
import services.player.InventoryService;

/** Hidden main/secondary lines and combined percentage stats, without a DB/server. */
public class RandomStatOptionsTest {
    private static int checks;

    public static void main(String[] args) throws Exception {
        NPoint.class.getMethod("setPlayer", Player.class); // Preserve the generated API used by SuperRank bosses.
        for (int id = 0; id < 252; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, ItemService.resolveOptionName(id, "Option #"), 0));
        }
        ItemTemplate template = new ItemTemplate();
        template.id = 0;
        template.type = 0;
        template.gender = 3;
        template.name = "Test gear";
        Manager.ITEM_TEMPLATES.add(template);
        Player player = new Player();

        for (int hiddenId : new int[]{195, 196}) {
            int lower = hiddenId == 195 ? 1 : 20;
            int upper = hiddenId == 195 ? 30 : 50;
            Set<Integer> seen = new HashSet<>();
            for (int attempt = 0; attempt < 1500; attempt++) {
                Item item = hidden(player, hiddenId, 0);
                check(InventoryService.gI().revealHiddenStats(player, item), "Random group option reveals");
                check(!item.hasOption(hiddenId) && item.itemOptions.size() == 1 && item.hasOption(199),
                        "Random group becomes one combined stat line");
                int amount = item.getOptionParam(199);
                check(amount >= lower && amount <= upper, "Requested random range: " + hiddenId);
                seen.add(amount);
                check(!InventoryService.gI().revealHiddenStats(player, item) && item.getOptionParam(199) == amount,
                        "Combined random stat remains fixed after reveal");
            }
            check(seen.contains(lower) && seen.contains(upper), "Both endpoints reachable: " + hiddenId);
        }

        for (int hiddenId : new int[]{197, 198}) {
            Set<Integer> allowed = hiddenId == 197 ? Set.of(50, 77, 103) : Set.of(5, 94, 108, 95, 96);
            Set<Integer> seen = new HashSet<>();
            for (int attempt = 0; attempt < 600; attempt++) {
                Item item = hidden(player, hiddenId, 37);
                check(item.getInfo().startsWith("37%"), "Hidden line displays percentage parameter");
                InventoryService.gI().revealHiddenStats(player, item);
                check(item.itemOptions.size() == 1 && !item.hasOption(hiddenId), "One hidden marker opens exactly one line");
                Item.ItemOption option = item.itemOptions.get(0);
                check(allowed.contains(option.optionTemplate.id) && option.param == 37, "Correct stat pool and exact percentage");
                seen.add(option.optionTemplate.id);
                check(!InventoryService.gI().revealHiddenStats(player, item), "Hidden main/secondary line cannot reroll");
            }
            check(seen.equals(allowed), "All stats in pool can be chosen");
        }

        Item mixed = hidden(player, 197, 12);
        mixed.addOptionParam(198, 25);
        mixed.addOptionParam(195, 0);
        mixed.addOptionParam(196, 0);
        mixed.addOptionParam(194, 0);
        mixed.addOptionParam(199, 10);
        mixed.addOptionParam(93, 7);
        InventoryService.gI().revealHiddenStats(player, mixed);
        check(mixed.itemOptions.size() == 7 && mixed.getOptionParam(93) == 7, "Mixed hidden options preserve unrelated stats");
        check(mixed.itemOptions.stream().noneMatch(o -> o.optionTemplate.id >= 194 && o.optionTemplate.id <= 198),
                "All hidden markers removed together");
        check(!InventoryService.gI().revealHiddenStats(player, mixed), "Mixed item resolves once");

        Player baseline = equipped(new int[0], new int[0]);
        Player combined = equipped(new int[]{199}, new int[]{30});
        Player separate = equipped(new int[]{50, 77, 103}, new int[]{30, 30, 30});
        check(combined.nPoint.hpMax == separate.nPoint.hpMax && combined.nPoint.mpMax == separate.nPoint.mpMax
                && combined.nPoint.dame == separate.nPoint.dame, "Option 199 matches the three normal percentage stats");
        check(combined.nPoint.hpMax > baseline.nPoint.hpMax && combined.nPoint.mpMax > baseline.nPoint.mpMax
                && combined.nPoint.dame > baseline.nPoint.dame, "Option 199 increases actual HP, KI and damage");
        combined.nPoint.calPoint();
        check(combined.nPoint.hpMax == separate.nPoint.hpMax && combined.nPoint.dame == separate.nPoint.dame,
                "Recalculating stats does not stack or reroll option 199");
        Item fixed = hidden(player, 199, 22);
        check(fixed.getInfo().equals("+22% SĐ, HP, KI") && !InventoryService.gI().revealHiddenStats(player, fixed),
                "Option 199 displays and keeps its entered percentage");
        System.out.println("PASS: " + checks + " random ranges, hidden pools and actual combined-stat checks");
    }

    private static Item hidden(Player player, int id, int amount) {
        Item item = ItemService.gI().createNewItem((short) 0);
        item.addOptionParam(id, amount);
        player.inventory.itemsBag.clear();
        player.inventory.itemsBag.add(item);
        return item;
    }

    private static Player equipped(int[] ids, int[] amounts) {
        Player player = new Player();
        player.isPlayer = true;
        player.nPoint.hpg = player.nPoint.mpg = player.nPoint.dameg = 10_000;
        player.nPoint.power = 1_000_000;
        player.playerIntrinsic.intrinsic = new Intrinsic();
        for (int slot = 0; slot < 14; slot++) player.inventory.itemsBody.add(new Item());
        Item item = ItemService.gI().createNewItem((short) 0);
        for (int i = 0; i < ids.length; i++) item.addOptionParam(ids[i], amounts[i]);
        player.inventory.itemsBody.set(0, item);
        player.nPoint.calPoint();
        return player;
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }
}
