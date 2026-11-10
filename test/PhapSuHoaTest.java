package models.combine.list;

import consts.ConstNpc;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.map.Zone;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.CombineService;
import services.ItemService;
import sun.misc.Unsafe;

/** Real recipe, fees, NPC routing and item packets without a DB or live accounts. */
public class PhapSuHoaTest {
    private static int checks;
    private static models.map.Map kame;
    private static final int[][] BASE_OPTIONS = {{0, 150}, {50, 10}, {77, 20}, {103, 30}, {5, 17},
        {30, 0}, {93, 7}, {72, 4}, {107, 8}, {21, 80}, {127, 1}};

    public static void main(String[] args) throws Exception {
        kame = map(5);
        for (int id = 0; id < 252; id++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,
                ItemService.resolveOptionName(id, "option #"), 0));
        TestNpc npc = new TestNpc();
        check(PhapSuHoa.SUCCESS_RATE == 80, "Upgrade chance is exactly eighty percent");

        for (int type : new int[]{0, 1, 2, 3, 4, 5, 11, 21, 23, 24}) {
            for (int gender = 0; gender < 3; gender++) {
                    TestPlayer player = recipe(false);
                    Item gear = player.inventory.itemsBag.get(0);
                    gear.template.type = (byte) type;
                    gear.template.gender = (byte) gender;
                    long created = gear.createTime;
                    for (int attempt = 1; attempt <= 8; attempt++) {
                        int choice = (attempt - 1) % 4;
                        PhapSuHoa.combine(player, 1, () -> true, bound -> choice);
                        check(gear.getOptionParam(200) == attempt && mageTotal(gear) == 3 * attempt,
                                "All equipment slots and planets gain exactly one +3 bonus per success");
                        for (int i = 0; i < 4; i++) check(gear.getOptionParam(201 + i) == Math.max(0, (attempt + 3 - i) / 4) * 3,
                                "Only selected stat changes; repeated selections accumulate");
                        check(gear.itemOptions.size() == BASE_OPTIONS.length + 1 + Math.min(attempt, 4),
                                "Only rolled mage stats and one counter are added");
                        check(player.inventory.gem == 4000 - attempt * 200 && count(player, 1455) == 10 - attempt,
                                "Exactly one stone and 200 green gems per success");
                        check(player.inventory.gold == 1234 && player.inventory.ruby == 567,
                                "Other currencies preserved");
                        check(player.inventory.itemsBag.get(0) == gear && gear.createTime == created && gear.quantity == 1,
                                "Original equipment preserved");
                        preserveBase(gear);
                        check(player.combineNew.itemsCombine.equals(List.of(gear,findMaterial(player,1455))) && player.effects.contains(2) && !player.effects.contains(3),
                                "Successful roll keeps equipment and remaining material selected");
                        check(readBagOption(player.bagPacket, 200) == attempt, "Client packet carries success count");
                        for (int id = 201; id <= 204; id++) if (gear.hasOption(id)) {
                            check(readBagOption(player.bagPacket, id) == gear.getOptionParam(id),
                                    "Client packet carries actual random mage percentages");
                        }
                        check(gear.itemOptions.stream().filter(o -> o.optionTemplate.id == 200).findFirst().orElseThrow()
                                .getOptionString().equals("Trang Bị Pháp Sư " + attempt + " lần"), "Counter text updates");
                    }
                    select(player, false);
                    PhapSuHoa.combine(player, () -> { throw new AssertionError("Limit must block before rolling"); });
                    check(gear.getOptionParam(200) == 8 && mageTotal(gear) == 24
                            && player.inventory.gem == 2400 && count(player, 1455) == 2, "Ninth upgrade has no cost");
            }
        }

        TestPlayer player = recipe(false);
        Item gear = player.inventory.itemsBag.get(0);
        for (int choice = 0; choice < 4; choice++) {
            select(player, false);
            int stat = choice;
            PhapSuHoa.combine(player, 1, () -> true, bound -> stat);
        }
        check(gear.getOptionParam(200) == 4 && mageTotal(gear) == 12
                && gear.getOptionParam(201) == 3 && gear.getOptionParam(204) == 3, "All four random outcomes are supported");

        for (int type : new int[]{5, 11, 21, 23, 24}) {
            player = recipe(true); gear = player.inventory.itemsBag.get(0);
            gear.template.type = (byte) type;
            gear.addOptionParam(200, 8);
            for (int id = 201; id <= 204; id++) gear.addOptionParam(id, 6);
            PhapSuHoa.showInfoCombine(player);
            check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE,
                    "New equipment type can preview reset: " + type);
            PhapSuHoa.combine(player, () -> { throw new AssertionError("Reset must not roll stats"); });
            check(gear.itemOptions.size() == BASE_OPTIONS.length && mageTotal(gear) == 0 && !gear.hasOption(200),
                    "Reset removes only mage lines on added equipment type: " + type);
            preserveBase(gear);
            check(player.inventory.gem == 3800 && count(player, 1456) == 9,
                    "New equipment reset uses exactly one charm and 200 green gems");
            check(player.inventory.itemsBag.get(0) == gear && readBagOption(player.bagPacket, 50) == 10,
                    "Original item and unrelated packet stats preserved after reset");
            player.inventory.itemsBag.add(item(1455, 27, 1));
            select(player, false);
            PhapSuHoa.showInfoCombine(player);
            check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE,
                    "New equipment type can preview upgrade after reset");
            PhapSuHoa.combine(player, () -> true);
            check(gear.getOptionParam(200) == 1 && mageTotal(gear) == 3 && player.inventory.gem == 3600,
                    "Added equipment can start a fresh upgrade cycle after reset");

            for (boolean reset : new boolean[]{false, true}) {
                player = recipe(reset); gear = player.inventory.itemsBag.get(0);
                gear.template.type = (byte) type; gear.quantity = 2;
                if (reset) gear.addOptionParam(200, 1);
                PhapSuHoa.combine(player, () -> { throw new AssertionError("Stack must not roll stats"); });
                check(player.inventory.gem == 4000 && count(player, reset ? 1456 : 1455) == 10 && player.bagPackets == 0,
                        "Stacked accessory cannot apply one fee to multiple items");
            }
        }

        player = recipe(true); gear = player.inventory.itemsBag.get(0);
        gear.addOptionParam(200, 8);
        for (int id = 201; id <= 204; id++) gear.addOptionParam(id, 6);
        PhapSuHoa.combine(player, () -> { throw new AssertionError("Reset must not roll stats"); });
        check(gear.itemOptions.size() == BASE_OPTIONS.length && !gear.hasOption(200)
                && !gear.hasOption(201) && !gear.hasOption(202) && !gear.hasOption(203) && !gear.hasOption(204),
                "Reset removes every mage line including count");
        preserveBase(gear);
        check(player.inventory.gem == 3800 && count(player, 1456) == 9 && player.inventory.ruby == 567,
                "Reset costs exactly one charm and 200 green gems");
        player.combineNew.typeCombine = CombineService.PHAP_SU_HOA;
        player.inventory.itemsBag.add(item(1455, 27, 1));
        select(player, false);
        PhapSuHoa.combine(player, () -> true);
        check(gear.getOptionParam(200) == 1 && mageTotal(gear) == 3 && player.inventory.gem == 3600,
                "Reset allows a fresh upgrade cycle");

        // Stale selections, wrong currency and malformed recipes may not consume anything.
        for (boolean reset : new boolean[]{false, true}) {
            for (int invalid = 0; invalid < 18; invalid++) {
                player = recipe(reset); gear = player.inventory.itemsBag.get(0);
                if (reset) { gear.addOptionParam(200, 2); gear.addOptionParam(202, 6); }
                switch (invalid) {
                    case 0 -> player.combineNew.itemsCombine.remove(1);
                    case 1 -> player.inventory.itemsBag.get(1).quantity = 0;
                    case 2 -> gear.template.type = 9;
                    case 3 -> gear.template.type = 27;
                    case 4 -> player.inventory.itemsBag.get(1).template.id = 14;
                    case 5 -> player.inventory.itemsBag.remove(gear);
                    case 6 -> player.combineNew.itemsCombine.set(1, gear);
                    case 7 -> gear.quantity = 2;
                    case 8 -> { Item extra = item(457, 9, 1); player.inventory.itemsBag.add(extra); player.combineNew.itemsCombine.add(extra); }
                    case 9 -> player.inventory.gem = 199;
                    case 10 -> { player.inventory.gem = 0; player.inventory.ruby = 1_000_000; }
                    case 11 -> player.zone = new Zone(map(42), 0, 20);
                    case 12 -> player.zone = null;
                    case 13 -> player.combineNew.itemsCombine.set(0, null);
                    case 14 -> gear.template.type = -1;
                    case 15 -> player.inventory.itemsBag.remove(1);
                    case 16 -> gear.itemOptions = null;
                    case 17 -> player.combineNew.typeCombine = -1;
                }
                int beforeGems = player.inventory.gem;
                int beforeMaterial = count(player, reset ? 1456 : 1455);
                PhapSuHoa.combine(player, () -> { throw new AssertionError("Invalid recipe must not roll"); });
                check(player.inventory.gem == beforeGems && count(player, reset ? 1456 : 1455) == beforeMaterial,
                        "Invalid/stale recipe has no charge: " + reset + "/" + invalid);
                check(player.bagPackets == 0, "Invalid recipe does not mutate inventory");
            }
        }
        for (int invalid = 0; invalid < 6; invalid++) {
            player = recipe(false); gear = player.inventory.itemsBag.get(0);
            switch (invalid) {
                case 0 -> gear.addOptionParam(200, -1);
                case 1 -> gear.addOptionParam(204, 65534);
                case 2 -> { gear.itemOptions.add(new Item.ItemOption(200, 1)); gear.itemOptions.add(new Item.ItemOption(200, 1)); }
                case 3 -> { gear.itemOptions.add(new Item.ItemOption(201, 3)); gear.itemOptions.add(new Item.ItemOption(201, 3)); }
                case 4 -> gear.itemOptions.add(null);
                case 5 -> {
                    gear.itemOptions.clear();
                    for (int i = 0; i < 254; i++) gear.itemOptions.add(new Item.ItemOption(30, 0));
                }
            }
            PhapSuHoa.combine(player, () -> { throw new AssertionError("Invalid option data must not roll"); });
            check(player.inventory.gem == 4000 && count(player, 1455) == 10, "Corrupt option/overflow has no charge");
        }
        player = recipe(true);
        PhapSuHoa.combine(player);
        check(player.inventory.gem == 4000 && count(player, 1456) == 10, "Cannot reset unenchanted equipment");
        player = recipe(true); gear = player.inventory.itemsBag.get(0);
        gear.itemOptions.add(new Item.ItemOption(204, 3));
        gear.itemOptions.add(new Item.ItemOption(204, 6));
        PhapSuHoa.combine(player);
        check(!gear.hasOption(204), "Reset removes duplicate mage lines even without a counter");
        preserveBase(gear);

        for (boolean reset : new boolean[]{false, true}) {
            player = recipe(reset); gear = player.inventory.itemsBag.get(0);
            if (reset) gear.addOptionParam(200, 1);
            player.inventory.gem = 200;
            player.inventory.itemsBag.get(1).quantity = 1;
            for (int i = 2; i < 100; i++) player.inventory.itemsBag.add(item(457, 9, 1));
            java.util.Collections.reverse(player.combineNew.itemsCombine);
            CombineService.gI().startCombine(player);
            check(player.inventory.gem == 0 && count(player, reset ? 1456 : 1455) == 0
                    && player.inventory.itemsBag.get(0) == gear, "Full bag, exact fee, last material and reversed input order");
            CombineService.gI().startCombine(player);
            check(player.inventory.gem == 0 && player.bagPackets == 1, "Confirmation replay has no cost");
        }

        // Exercise real NPC menu, open-tab packet, preview, cancellation and confirmation.
        for (boolean reset : new boolean[]{false, true}) {
            player = recipe(reset); gear = player.inventory.itemsBag.get(0);
            if (reset) { gear.addOptionParam(200, 3); gear.addOptionParam(201, 9); }
            npc.openBaseMenu(player);
            check(npc.buttons.length == 9 && npc.buttons[6].contains("giới hạn")
                    && npc.buttons[7].contains("Pháp sư") && npc.buttons[8].contains("pháp sư"), "Append new Kame menu entries");
            npc.confirmMenu(player, reset ? 8 : 7);
            check(player.combineNew.typeCombine == (reset ? CombineService.TAY_PHAP_SU : CombineService.PHAP_SU_HOA),
                    "NPC opens correct combine type");
            try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(player.openPacket))) {
                check(reader.readUnsignedByte() == 0, "Standard open-combine packet");
                String info = reader.readUTF(); String title = reader.readUTF();
                check(info.contains("200 ngọc xanh") && info.contains("Tỉ lệ thành công: " + (reset ? 100 : 80) + "%")
                        && info.contains(reset ? "Bùa tẩy pháp sư" : "Đá pháp sư"), "Correct recipe and cost in client instructions");
                check(info.contains("cải trang") && info.contains("pet") && info.contains("cánh") && info.contains("thú cưỡi"),
                        "Both instructions list newly supported equipment");
                check(title.equals(reset ? "Tẩy pháp sư" : "Pháp sư hóa trang bị"), "Correct combine title");
            }
            CombineService.gI().showInfoCombine(player, new int[]{0, 1});
            check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE
                    && npc.text.contains("Tỉ lệ thành công: " + (reset ? 100 : 80) + "%") && npc.text.contains("200 ngọc xanh"), "Preview routed with real cost and odds");
            npc.confirmMenu(player, 2);
            check(player.inventory.gem == 4000, "Cancel consumes nothing");
            npc.confirmMenu(player, 0);
            check(player.inventory.gem == 3800 && count(player, reset ? 1456 : 1455) == 9,
                    "NPC confirmation performs exactly one operation");
            check(reset ? !gear.hasOption(200) :
                    (gear.getOptionParam(200) == 1 && mageTotal(gear) == 3 || !gear.hasOption(200) && mageTotal(gear) == 0),
                    "NPC applies reset or real probabilistic upgrade");
            npc.confirmMenu(player, 0);
            check(player.inventory.gem == 3800, "Repeated NPC confirmation cannot charge again");
        }
        player = recipe(false);
        CombineService.gI().showInfoCombine(player, new int[]{0, 0});
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Duplicate input indices cannot preview");
        for (int mapId : new int[]{42, 43, 44, 112}) {
            for (int type : new int[]{CombineService.PHAP_SU_HOA, CombineService.TAY_PHAP_SU}) {
                player = recipe(false);
                player.zone = new Zone(map(mapId), 0, 20);
                player.combineNew.typeCombine = -1;
                CombineService.gI().openTabCombine(player, type);
                check(player.combineNew.typeCombine == -1 && player.openPacket == null, "Cannot open outside Kame");
            }
        }
        player = recipe(false);
        CombineService.gI().showInfoCombine(player, new int[]{0, 1});
        player.zone = new Zone(map(42), 0, 20);
        npc.confirmMenu(player, 0);
        check(player.inventory.gem == 4000 && count(player, 1455) == 10, "Moving after preview cannot upgrade");

        // Existing equipment may have any subset of mage stats, including old four-stat upgrades.
        for (int mask = 0; mask < 16; mask++) for (int selected = 0; selected < 4; selected++) {
            player = recipe(false); gear = player.inventory.itemsBag.get(0);
            gear.addOptionParam(200, 2);
            for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) gear.addOptionParam(201 + i, 6);
            int stat = selected;
            PhapSuHoa.combine(player, 1, () -> true, bound -> stat);
            for (int i = 0; i < 4; i++) check(gear.getOptionParam(201 + i) == (((mask & (1 << i)) != 0 ? 6 : 0) + (i == selected ? 3 : 0)),
                    "Legacy stats change only when selected");
            check(gear.getOptionParam(200) == 3, "Random bonus counts as one success");
            preserveBase(gear);
        }
        for (int equipmentType : new int[]{0,1,2,3,4,5,11,21,23,24}) {
            for (int progress : new int[]{0,7}) {
                player = recipe(false); gear = player.inventory.itemsBag.get(0);
                gear.template.type = (byte) equipmentType;
                if (progress > 0) {gear.addOptionParam(200, progress);gear.addOptionParam(201, 6);gear.addOptionParam(203,6);}
                String original = gear.getInfo();int originalSize = gear.itemOptions.size();
                PhapSuHoa.combine(player, 1, () -> false, bound -> {throw new AssertionError("Failure must not roll a stat");});
                check(gear.getInfo().equals(original) && gear.itemOptions.size() == originalSize && gear.getOptionParam(200) == progress,
                        "Failure preserves every stat and success counter: " + equipmentType);
                check(player.inventory.gem == 3800 && count(player,1455) == 9 && player.effects.contains(3) && !player.effects.contains(2),
                        "Failure costs exactly one stone and 200 green gems and displays failure effect");
                check(player.combineNew.itemsCombine.equals(List.of(gear,findMaterial(player,1455))) && player.bagPackets == 1, "Failure retains selection and refreshes inventory");
                PhapSuHoa.combine(player, () -> true);
                check(gear.getOptionParam(200) == progress + 1 && player.inventory.gem == 3600,
                        "Failed attempts do not consume the eight-success limit");
            }
        }
        player = recipe(false); gear = player.inventory.itemsBag.get(0);
        gear.addOptionParam(204,6);
        PhapSuHoa.combine(player, 1, () -> true, bound -> 3);
        check(readBagOption(player.bagPacket,204) == 9 && gear.getOptionParam(200) == 1,
                "Six percent upgrades to exactly nine in client packet");

        for (int mask = 0; mask < 15; mask++) {
            List<Integer> available = new ArrayList<>();
            for (int i = 0; i < 4; i++) if ((mask & (1 << i)) == 0) available.add(201 + i);
            for (int selected = 0; selected < available.size(); selected++) {
                player = recipe(false); gear = player.inventory.itemsBag.get(0);
                for (int i = 0; i < 4; i++) if ((mask & (1 << i)) != 0) gear.addOptionParam(201 + i, 9);
                int choice = selected;
                PhapSuHoa.combine(player, 1, () -> true, bound -> {
                    check(bound == available.size(), "Random draw is uniform over only uncapped options");
                    return choice;
                });
                for (int i = 0; i < 4; i++) check(gear.getOptionParam(201 + i)
                        == ((mask & (1 << i)) != 0 ? 9 : (201 + i == available.get(selected) ? 3 : 0)),
                        "Nine percent lines never receive another bonus");
                check(gear.getOptionParam(200) == 1 && player.inventory.gem == 3800 && count(player,1455) == 9,
                        "Selecting another eligible line still costs one attempt");
            }
        }
        for (int value : new int[]{7,8}) {
            player = recipe(false); gear = player.inventory.itemsBag.get(0); gear.addOptionParam(201, value);
            PhapSuHoa.combine(player, 1, () -> true, bound -> 0);
            check(gear.getOptionParam(201) == 9 && readBagOption(player.bagPacket,201) == 9,
                    "Non-multiple legacy values stop at nine rather than exceeding cap");
        }
        for (int value : new int[]{9,12}) {
            player = recipe(false); gear = player.inventory.itemsBag.get(0);
            for (int id = 201; id <= 204; id++) gear.addOptionParam(id, value);
            PhapSuHoa.showInfoCombine(player);
            check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Full cap or over-cap legacy gear cannot preview upgrade");
            PhapSuHoa.combine(player, 10, () -> {throw new AssertionError("Blocked gear must not roll");});
            check(player.inventory.gem == 4000 && count(player,1455) == 10 && !gear.hasOption(200),
                    "Full cap and legacy over-cap gear blocked before costs or counter changes");
        }
        player = recipe(true); gear = player.inventory.itemsBag.get(0); gear.addOptionParam(200, 8);
        for (int id = 201; id <= 204; id++) gear.addOptionParam(id, 24);
        PhapSuHoa.combine(player, 1, () -> {throw new AssertionError("Reset must not roll success");},
                bound -> {throw new AssertionError("Reset must not roll a stat");});
        check(!gear.hasOption(200) && mageTotal(gear) == 0 && player.inventory.gem == 3800 && count(player,1456) == 9,
                "Reset still cleans legacy over-cap gear for the normal cost");
        preserveBase(gear);

        player = recipe(false); gear = player.inventory.itemsBag.get(0); gear.itemOptions.clear();
        for (int i = 0; i < 253; i++) gear.itemOptions.add(new Item.ItemOption(30, 0));
        PhapSuHoa.combine(player, 1, () -> true, bound -> 0);
        check(gear.itemOptions.size() == 255 && gear.getOptionParam(200) == 1 && gear.getOptionParam(201) == 3,
                "Random upgrade reserves only counter plus one stat, within client packet limit");

        int successes=0;
        int[] statSelections = new int[4];
        for (int attempt = 0; attempt < 2000; attempt++) {
            player = recipe(false);
            player.combineNew.ratioCombine = attempt % 2 == 0 ? 0 : 100;
            CombineService.gI().startCombine(player);
            gear = player.inventory.itemsBag.get(0);
            if (gear.hasOption(200)) {
                successes++;
                check(gear.getOptionParam(200) == 1 && mageTotal(gear) == 3
                                && gear.itemOptions.size() == BASE_OPTIONS.length + 2,
                        "Production success adds exactly one random stat");
                for (int i = 0; i < 4; i++) if (gear.hasOption(201 + i)) statSelections[i]++;
            } else check(mageTotal(gear) == 0 && gear.itemOptions.size() == BASE_OPTIONS.length,
                    "Production failure adds no stats or counter");
            check(player.inventory.gem == 3800 && count(player,1455) == 9,
                    "Each production attempt charges exactly once regardless of outcome");
        }
        check(successes >= 1450 && successes <= 1750, "Production chance is near eighty percent independently of shared ratio: " + successes);
        for (int count : statSelections) check(count > 0, "Production random selection can reach all four stats");
        check(PhapSuHoa.getInstructions(false).contains("ngẫu nhiên 1 dòng") && !PhapSuHoa.getInstructions(false).contains("cả 4 dòng"),
                "Instructions describe the actual one-stat random outcome");
        System.out.println("PASS: " + checks + " mage recipes, random stat bonuses, failure fees, eight-success limit, reset, packets and NPC checks; successes " + successes + "/2000; stats " + Arrays.toString(statSelections));
    }

    private static void preserveBase(Item gear) {
        for (int[] option : BASE_OPTIONS) check(gear.getOptionParam(option[0]) == option[1], "Preserve unrelated option " + option[0]);
    }
    private static int mageTotal(Item item) {
        return item.itemOptions.stream().filter(o -> o.optionTemplate.id >= 201 && o.optionTemplate.id <= 204).mapToInt(o -> o.param).sum();
    }
    private static TestPlayer recipe(boolean reset) {
        TestPlayer player = new TestPlayer();
        player.zone = new Zone(kame, 0, 20);
        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
        player.inventory.gem = 4000; player.inventory.gold = 1234; player.inventory.ruby = 567;
        Item gear = item(0, 0, 1);
        for (int[] option : BASE_OPTIONS) gear.addOptionParam(option[0], option[1]);
        player.inventory.itemsBag.addAll(List.of(gear, item(reset ? 1456 : 1455, 27, 10)));
        select(player, reset);
        return player;
    }
    private static void select(Player player, boolean reset) {
        player.combineNew.typeCombine = reset ? CombineService.TAY_PHAP_SU : CombineService.PHAP_SU_HOA;
        player.combineNew.itemsCombine.clear();
        player.combineNew.itemsCombine.add(player.inventory.itemsBag.get(0));
        player.combineNew.itemsCombine.add(player.inventory.itemsBag.stream()
                .filter(i -> i.isNotNullItem() && i.template.id == (reset ? 1456 : 1455)).findFirst().orElseThrow());
    }
    private static Item item(int id, int type, int quantity) {
        Item item = new Item(); item.template = new ItemTemplate();
        item.template.id = (short) id; item.template.type = (byte) type;
        item.template.name = "Test item " + id; item.quantity = quantity; return item;
    }
    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).mapToInt(i -> i.quantity).sum();
    }
    private static Item findMaterial(Player player,int id){return player.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).findFirst().orElseThrow();}
    private static models.map.Map map(int id) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe"); field.setAccessible(true);
        models.map.Map map = (models.map.Map) ((Unsafe) field.get(null)).allocateInstance(models.map.Map.class);
        map.mapId = id; return map;
    }
    private static int readBagOption(byte[] packet, int optionId) throws Exception {
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(packet))) {
            reader.readUnsignedByte();
            int count = reader.readUnsignedByte();
            for (int i = 0; i < count; i++) {
                int template = reader.readShort(); if (template == -1) continue;
                reader.readInt(); reader.readUTF(); reader.readUTF();
                int options = reader.readUnsignedByte();
                for (int j = 0; j < options; j++) {
                    int id = reader.readUnsignedByte(); int param = reader.readUnsignedShort();
                    if (template == 0 && id == optionId) return param;
                }
            }
        }
        throw new AssertionError("Missing gear option in inventory packet");
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static class TestPlayer extends Player {
        final List<Integer> effects = new ArrayList<>(); byte[] openPacket; byte[] bagPacket; int bagPackets;
        @Override public void sendMessage(Message msg) {
            byte[] data = msg.getData();
            if (msg.command == -81 && data.length > 0) {
                effects.add((int) data[0]);
                if (data[0] == 0) openPacket = Arrays.copyOf(data, data.length);
            }
            if (msg.command == -36) { bagPackets++; bagPacket = Arrays.copyOf(data, data.length); }
        }
    }
    private static class TestNpc extends BaHatMit {
        String[] buttons; String text;
        TestNpc() { super(5, 1, 0, 0, ConstNpc.BA_HAT_MIT, 0); }
        @Override public boolean canOpenNpc(Player player) { player.idMark.setNpcChose(this); return true; }
        @Override public void createOtherMenu(Player player, int index, String text, String... buttons) {
            this.buttons = buttons; this.text = text; super.createOtherMenu(player, index, text, buttons);
        }
    }
}
