package models.combine.list;

import consts.ConstNpc;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
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

/** Run as models.combine.list.SieuHoaCaiTrangTest; no database or server required. */
public class SieuHoaCaiTrangTest {
    private static final int[] OPTIONS = {50, 77, 103, 5};
    private static int checks;
    private static models.map.Map kameMap;

    public static void main(String[] args) throws Exception {
        kameMap = mapWithId(5);
        for (int id = 0; id < 250; id++) {
            Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id, ItemService.resolveOptionName(id, "option #"), 0));
        }
        TestNpc npc = new TestNpc();
        TestPlayer player = recipe(5, 2, 568);
        Item costume = player.inventory.itemsBag.get(0);
        costume.addOptionParam(50, 30);
        costume.addOptionParam(77, 20);
        costume.addOptionParam(93, 7);
        AtomicInteger index = new AtomicInteger();
        int[] bonus = {1, 5, 2, 4};
        SieuHoaCaiTrang.nangCap(player, () -> true, () -> bonus[index.getAndIncrement()]);
        check(costume.getOptionParam(50) == 31 && costume.getOptionParam(77) == 25, "Add to existing stats");
        check(costume.getOptionParam(103) == 2 && costume.getOptionParam(5) == 4, "Add missing stats");
        check(costume.getOptionParam(93) == 7, "Preserve expiry and other options");
        check(costume.getOptionParam(205) == 1, "First successful super upgrade starts count at one");
        check(player.inventory.itemsBag.contains(costume) && costume.quantity == 1, "Keep original costume");
        check(count(player, 674) == 0 && count(player, 568) == 1, "Consume exactly two stones and one egg");
        check(player.combineNew.itemsCombine.equals(List.of(costume,find(player,568))), "Keep costume and remaining egg; remove only consumed stone stack");
        checkReopenSelection(player);
        check(player.effects.contains(2), "Success animation");
        check(player.inventory.gold == 1234 && player.inventory.ruby == 567, "No unrequested currency cost");
        SieuHoaCaiTrang.nangCap(player, () -> { throw new AssertionError("Replay must not roll"); }, () -> 5);
        check(costume.getOptionParam(50) == 31 && count(player, 568) == 1, "No repeated upgrade when retained selection lacks stones");

        Item stones = item(674, 27, 2);
        player.inventory.itemsBag.add(stones);
        reselect(player,costume);
        SieuHoaCaiTrang.nangCap(player, () -> true, () -> 5);
        check(costume.getOptionParam(50) == 36 && costume.getOptionParam(103) == 7, "Repeat upgrades accumulate");
        check(costume.getOptionParam(205) == 2, "Success count accumulates on the same costume");

        player = recipe(5, 2, 568);
        costume = player.inventory.itemsBag.get(0);
        costume.addOptionParam(50, 30);
        SieuHoaCaiTrang.nangCap(player, () -> false, () -> { throw new AssertionError("Failed upgrades must not add stats"); });
        check(costume.getOptionParam(50) == 30 && costume.itemOptions.size() == 1, "Failure preserves all costume stats");
        check(player.inventory.itemsBag.contains(costume) && count(player, 674) == 0 && count(player, 568) == 1,
                "Failure keeps costume and consumes materials");
        check(player.effects.contains(3), "Failure animation");

        for (int invalid = 0; invalid < 8; invalid++) {
            player = recipe(5, 2, 568);
            switch (invalid) {
                case 0 -> player.combineNew.itemsCombine.remove(2);
                case 1 -> player.inventory.itemsBag.get(1).quantity = 1;
                case 2 -> player.inventory.itemsBag.get(0).template.type = 0;
                case 3 -> player.inventory.itemsBag.get(2).template.id = 1396;
                case 4 -> player.inventory.itemsBag.remove(0);
                case 5 -> player.combineNew.itemsCombine.add(player.combineNew.itemsCombine.get(1));
                case 6 -> player.inventory.itemsBag.get(0).quantity = 2;
                case 7 -> {
                    Item extra = item(457, 9, 1);
                    player.inventory.itemsBag.add(extra);
                    player.combineNew.itemsCombine.add(extra);
                }
            }
            int stonesBefore = count(player, 674);
            int eggsBefore = count(player, 568);
            SieuHoaCaiTrang.nangCap(player, () -> { throw new AssertionError("Invalid recipe must not roll"); }, () -> 5);
            check(count(player, 674) == stonesBefore && count(player, 568) == eggsBefore,
                    "Invalid/stale recipe has no material cost: " + invalid);
        }

        player = recipe(5, 1, 568);
        Item secondStone = item(674, 27, 1);
        player.inventory.itemsBag.add(secondStone);
        player.combineNew.itemsCombine.add(secondStone);
        SieuHoaCaiTrang.nangCap(player, () -> true, () -> 1);
        check(count(player, 674) == 0 && player.inventory.itemsBag.get(0).getOptionParam(50) == 1,
                "Accept stones split across selected stacks");

        // Select once and perform consecutive attempts, on both success and failure.
        player=recipe(5,10,568);costume=player.inventory.itemsBag.get(0);player.inventory.itemsBag.get(2).quantity=10;
        for(int attempt=1;attempt<=5;attempt++) {
            final boolean success=attempt==1||attempt==3;
            SieuHoaCaiTrang.nangCap(player,()->success,()->2);
            check(count(player,674)==10-2*attempt&&count(player,568)==10-attempt,"Every consecutive attempt charges exactly the original materials");
            check(player.combineNew.itemsCombine.contains(costume),"Costume stays selected on success and failure");
            check(player.combineNew.itemsCombine.size()==(attempt<5?3:2),"Only exhausted material stack leaves selection");
            checkReopenSelection(player);
        }
        check(costume.getOptionParam(205)==2&&costume.getOptionParam(50)==4,"Continuous attempts preserve success counter and bonus rules");
        int eggsBefore=count(player,568);
        SieuHoaCaiTrang.nangCap(player,()->{throw new AssertionError("Missing stones must not roll");},()->5);
        check(count(player,568)==eggsBefore,"Retained costume cannot consume eggs without enough stones");

        player=recipe(5,1,568);costume=player.inventory.itemsBag.get(0);
        Item anotherStone=item(674,27,5);player.inventory.itemsBag.add(anotherStone);player.combineNew.itemsCombine.add(anotherStone);
        SieuHoaCaiTrang.nangCap(player,()->false,()->5);
        check(player.combineNew.itemsCombine.equals(List.of(costume,find(player,568),anotherStone))&&anotherStone.quantity==4,"Exhausted split stack removed while remaining selected stack stays");
        checkReopenSelection(player);
        SieuHoaCaiTrang.nangCap(player,()->true,()->1);
        check(count(player,674)==2&&count(player,568)==0&&player.combineNew.itemsCombine.equals(List.of(costume,anotherStone)),"Can upgrade again without reinserting surviving split stack");
        checkReopenSelection(player);

        player=recipe(5,3,568);costume=player.inventory.itemsBag.get(0);
        SieuHoaCaiTrang.nangCap(player,()->false,()->5);
        check(player.combineNew.itemsCombine.size()==3&&count(player,674)==1,"Keep a partially used stack even below next recipe requirement");
        SieuHoaCaiTrang.nangCap(player,()->{throw new AssertionError("One stone is insufficient");},()->5);
        check(count(player,674)==1&&count(player,568)==1,"Insufficient retained stack consumes nothing");

        player = recipe(5, 2, 568);
        npc.openBaseMenu(player);
        check(npc.buttons.length >= 5 && npc.buttons[4].contains("Siêu hóa"), "NPC menu entry");
        npc.confirmMenu(player, 4);
        check(player.combineNew.typeCombine == CombineService.SIEU_HOA_CAI_TRANG, "NPC opens correct tab");
        CombineService.gI().showInfoCombine(player, new int[]{0, 1, 2});
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE, "Recipe preview routes to confirmation");
        check(npc.text.contains("10%") && npc.text.contains("1–5%"), "Preview shows actual odds and bonus");
        npc.confirmMenu(player, 2);
        check(count(player, 674) == 2 && count(player, 568) == 2, "Cancel has no cost");
        npc.confirmMenu(player, 0);
        check(count(player, 674) == 0 && count(player, 568) == 1, "NPC confirmation executes upgrade");
        CombineService.gI().showInfoCombine(player, new int[]{0});
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Invalid preview cannot start combine");

        player=recipe(5,8,568);player.inventory.itemsBag.get(2).quantity=4;
        for(int attempt=1;attempt<=4;attempt++) {
            int[] selectedIndices=player.combineNew.itemsCombine.stream().mapToInt(player.inventory.itemsBag::indexOf).toArray();
            CombineService.gI().showInfoCombine(player,selectedIndices);
            check(player.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"Retained client selection can open the next confirmation");
            npc.confirmMenu(player,0);
            check(count(player,674)==8-2*attempt&&count(player,568)==4-attempt,"Repeated real NPC flow consumes one recipe each time without reinserting");
            checkReopenSelection(player);
            npc.confirmMenu(player,0);
            check(count(player,674)==8-2*attempt&&count(player,568)==4-attempt,"Old popup confirmation cannot charge a second time");
        }
        check(player.combineNew.itemsCombine.size()==1&&player.combineNew.itemsCombine.get(0).template.type==5,"Keep costume selected after last recipe consumes both materials");

        for (int mapId : new int[]{42, 43, 44, 84}) {
            npc.mapId = mapId;
            models.map.Map otherMap = mapWithId(mapId);
            for (int variation = 0; variation < 8; variation++) {
                player = recipe(5, 2, 568);
                player.zone = new Zone(otherMap, 0, 20);
                player.luotNhanBuaMienPhi = variation & 1;
                if ((variation & 2) != 0) player.inventory.itemsBag.add(item(1210, 27, 1));
                if ((variation & 4) != 0) player.inventory.itemsBag.add(item(1211, 27, 1));
                npc.openBaseMenu(player);
                check(java.util.Arrays.stream(npc.buttons).noneMatch(b -> b.contains("Siêu hóa")),
                        "No super costume entry on map " + mapId);
                check(npc.buttons.length == 4, "Compact cliff menu stays stable with charm/event items");
                player.combineNew.typeCombine = -1;
                CombineService.gI().openTabCombine(player, CombineService.SIEU_HOA_CAI_TRANG);
                check(player.combineNew.typeCombine == -1, "Cannot open super costume tab outside Kame");
                player.combineNew.typeCombine = CombineService.SIEU_HOA_CAI_TRANG;
                CombineService.gI().showInfoCombine(player, new int[]{0, 1, 2});
                check(player.idMark.isBaseMenu(), "Cannot preview outside Kame");
                SieuHoaCaiTrang.nangCap(player, () -> { throw new AssertionError("Wrong map must not roll"); }, () -> 5);
                player.idMark.setIndexMenu(ConstNpc.MENU_START_COMBINE);
                npc.confirmMenu(player, 0);
                check(count(player, 674) == 2 && count(player, 568) == 2,
                        "Direct upgrade and NPC confirmation outside Kame have no cost");
            }
        }

        player = recipe(5, 2, 568);
        npc.mapId = 5;
        CombineService.gI().showInfoCombine(player, new int[]{0, 1, 2});
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE, "Kame preview still works");
        player.zone = new Zone(mapWithId(42), 0, 20);
        CombineService.gI().startCombine(player);
        check(count(player, 674) == 2 && count(player, 568) == 2, "Moving away after preview cannot consume materials");
        player.zone = null;
        CombineService.gI().startCombine(player);
        check(count(player, 674) == 2 && count(player, 568) == 2, "Missing map cannot upgrade");

        // Twenty successes are allowed regardless of the number of failed attempts.
        player = recipe(5, 120, 568);
        costume = player.inventory.itemsBag.get(0);
        player.inventory.itemsBag.get(2).quantity = 120;
        costume.addOptionParam(93, 7);
        for (int expectedCount = 1; expectedCount <= 20; expectedCount++) {
            SieuHoaCaiTrang.nangCap(player, () -> false, () -> { throw new AssertionError("Failure must not roll bonuses"); });
            check(costume.getOptionParam(205) == expectedCount - 1, "Failure never increments successful-upgrade count");
            SieuHoaCaiTrang.nangCap(player, () -> true, () -> 1);
            check(costume.getOptionParam(205) == expectedCount && costume.getOptionParam(50) == expectedCount,
                    "Every success increments counter once");
            check(count(player, 674) == 120 - expectedCount * 4 && count(player, 568) == 120 - expectedCount * 2,
                    "All attempts below the cap keep the existing material costs");
        }
        check(costume.getOptionParam(93) == 7 && costume.getOptionParam(205) == 20, "Twentieth success is accepted and preserves expiry");
        check(ItemService.gI().copyItem(costume).getOptionParam(205) == 20, "Persistent item options carry counter through copies");
        check(new Item.ItemOption(205, 20).getOptionString().equals("Siêu hóa thành công 20/20 lần"), "Counter text shows successful upgrades out of twenty");
        check(packetCount(player.bagPacket) == 20, "Client receives persistent success counter in inventory packet");
        reselect(player, costume);
        for (int attempt = 0; attempt < 2; attempt++) {
            SieuHoaCaiTrang.nangCap(player, () -> { throw new AssertionError("At cap must not roll success or failure"); }, () -> 5);
        }
        check(count(player, 674) == 40 && count(player, 568) == 80 && costume.getOptionParam(50) == 20,
                "Further attempts at twenty do not change stats or consume materials");
        SieuHoaCaiTrang.showInfoCombine(player);
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU && npc.text.contains("20 lần"), "Preview blocks capped costume");

        Item secondCostume = item(101, 5, 1);
        player.inventory.itemsBag.add(secondCostume);
        reselect(player, secondCostume);
        SieuHoaCaiTrang.nangCap(player, () -> true, () -> 1);
        check(secondCostume.getOptionParam(205) == 1 && costume.getOptionParam(205) == 20,
                "Limit belongs to each costume independently");

        player = recipe(5, 2, 568);
        costume = player.inventory.itemsBag.get(0);
        costume.addOptionParam(205, 19);
        CombineService.gI().showInfoCombine(player, new int[]{0, 1, 2});
        check(npc.text.contains("19/20") && player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE,
                "Preview displays nineteen successes and permits the final upgrade");
        costume.setOptionParam(205, 20);
        npc.confirmMenu(player, 0);
        check(count(player, 674) == 2 && count(player, 568) == 2, "Stale preview cannot bypass the cap");

        for (int invalid = 0; invalid < 4; invalid++) {
            player = recipe(5, 2, 568);
            costume = player.inventory.itemsBag.get(0);
            if (invalid == 0) costume.addOptionParam(205, -1);
            if (invalid == 1) costume.addOptionParam(205, 21);
            if (invalid == 2) {
                costume.itemOptions.add(new Item.ItemOption(205, 1));
                costume.itemOptions.add(new Item.ItemOption(205, 1));
            }
            if (invalid == 3) costume.itemOptions = null;
            SieuHoaCaiTrang.nangCap(player, () -> { throw new AssertionError("Invalid counter must not roll"); }, () -> 5);
            check(count(player, 674) == 2 && count(player, 568) == 2, "Invalid count does not consume materials");
        }

        // Check the public path ignores a stale/tampered shared combine ratio.
        int successes = 0;
        for (int attempt = 0; attempt < 2000; attempt++) {
            player = recipe(5, 2, 568);
            player.combineNew.ratioCombine = 100;
            costume = player.inventory.itemsBag.get(0);
            CombineService.gI().startCombine(player);
            if (costume.hasOption(50)) {
                successes++;
                for (int option : OPTIONS) {
                    check(costume.getOptionParam(option) >= 1 && costume.getOptionParam(option) <= 5,
                            "Production bonus range");
                }
            }
        }
        check(successes >= 100 && successes <= 300, "Production success rate near 10%: " + successes + "/2000");
        System.out.println("PASS: " + checks + " checks; production successes " + successes + "/2000");
    }

    private static TestPlayer recipe(int type, int stoneCount, int eggId) {
        TestPlayer player = new TestPlayer();
        player.zone = new Zone(kameMap, 0, 20);
        player.inventory.gold = 1234;
        player.inventory.ruby = 567;
        player.inventory.itemsBag.addAll(List.of(item(100, type, 1), item(674, 27, stoneCount), item(eggId, 27, 2)));
        player.combineNew.itemsCombine.addAll(player.inventory.itemsBag);
        player.combineNew.typeCombine = CombineService.SIEU_HOA_CAI_TRANG;
        return player;
    }

    private static models.map.Map mapWithId(int id) throws Exception {
        Field field = Unsafe.class.getDeclaredField("theUnsafe");
        field.setAccessible(true);
        models.map.Map map = (models.map.Map) ((Unsafe) field.get(null)).allocateInstance(models.map.Map.class);
        map.mapId = id;
        return map;
    }

    private static Item item(int id, int type, int quantity) {
        Item item = new Item();
        item.template = new ItemTemplate();
        item.template.id = (short) id;
        item.template.type = (byte) type;
        item.template.name = "Test item " + id;
        item.quantity = quantity;
        return item;
    }

    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id)
                .mapToInt(i -> i.quantity).sum();
    }

    private static Item find(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).findFirst().orElseThrow();
    }

    private static void reselect(Player player, Item costume) {
        player.combineNew.clearItemCombine();
        player.combineNew.itemsCombine.addAll(List.of(costume, find(player, 674), find(player, 568)));
    }

    private static int packetCount(byte[] packet) throws Exception {
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(packet))) {
            reader.readUnsignedByte(); int slots = reader.readUnsignedByte();
            for (int slot = 0; slot < slots; slot++) {
                int itemId = reader.readShort(); if (itemId == -1) continue;
                reader.readInt(); reader.readUTF(); reader.readUTF();
                int options = reader.readUnsignedByte();
                for (int i = 0; i < options; i++) {
                    int id = reader.readUnsignedByte(); int param = reader.readUnsignedShort();
                    if (itemId == 100 && id == 205) return param;
                }
            }
        }
        throw new AssertionError("Missing success counter in inventory packet");
    }

    private static void checkReopenSelection(TestPlayer player)throws Exception {
        try(var reader=new DataInputStream(new ByteArrayInputStream(player.reopenPacket))) {
            check(reader.readUnsignedByte()==1,"Standard reopen packet is used");
            int selected=reader.readUnsignedByte();check(selected==player.combineNew.itemsCombine.size(),"Packet selected count matches retained items");
            for(Item item:player.combineNew.itemsCombine) {
                int slot=reader.readUnsignedByte();check(slot<player.inventory.itemsBag.size()&&player.inventory.itemsBag.get(slot)==item&&item.quantity>0,"Client receives current bag index after sorting");
            }
            check(reader.available()==0,"Reopen packet contains no stale or missing indices");
        }
        check(player.packetOrder.get(player.packetOrder.size()-2)==-36&&player.packetOrder.get(player.packetOrder.size()-1)==1,"Inventory refresh precedes restored selection packet");
    }

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static class TestPlayer extends Player {
        final List<Integer> effects = new ArrayList<>();
        final List<Integer> packetOrder=new ArrayList<>();
        byte[] bagPacket,reopenPacket;
        @Override public void sendMessage(Message message) {
            if (message.command == -81 && message.getData().length > 0) effects.add((int) message.getData()[0]);
            if (message.command == -36) {bagPacket = message.getData().clone();packetOrder.add(-36);}
            if (message.command == -81&&message.getData().length>0&&message.getData()[0]==1){reopenPacket=message.getData().clone();packetOrder.add(1);}
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
