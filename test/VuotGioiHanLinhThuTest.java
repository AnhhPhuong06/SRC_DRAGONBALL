package models.combine.list;

import consts.ConstNpc;
import java.io.DataInputStream;
import java.io.ByteArrayInputStream;
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
import sun.misc.Unsafe;

/** No database, live server, account or game transaction required. */
public class VuotGioiHanLinhThuTest {
    private static int checks;
    private static models.map.Map kame;
    private static final int[] STATS = {50, 77, 103, 5};
    private static final int[][] PRESERVED = {{1,60}, {20,10}, {21,80}, {30,0}, {72,3}, {93,7},
        {102,7}, {106,1}, {107,5}, {118,1000}, {119,5}, {127,1}, {174,2026}, {185,12},
        {194,1}, {197,20}, {198,10}, {200,8}, {205,20}, {207,1}, {213,5}, {221,10},
        {224,1}, {225,4}, {226,1000}, {227,1}, {228,50}, {247,2}, {248,1}, {249,1}, {250,1}, {251,1},
        {0,1000}, {2,20}, {4,20}, {6,20}, {7,20}, {10,20}, {14,20}, {15,20}, {17,20},
        {22,20}, {23,20}, {27,20}, {28,20}, {47,20}, {48,20}, {62,20}, {78,20}, {79,20},
        {94,20}, {108,20}, {157,20}, {159,20}, {165,20}, {178,20}, {183,20}, {187,20},
        {190,20}, {199,20}, {201,20}, {202,20}, {203,20}, {204,20}, {217,20}, {222,20},
        {223,20}, {229,20}, {231,20}, {232,20}, {233,20}};

    public static void main(String[] args) throws Exception {
        kame = map(5);
        for (int i = 0; i < 252; i++) Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(i, "option #", 0));
        check(VuotGioiHanLinhThu.SUCCESS_RATE == 10, "Configured production success rate is 10 percent");
        TestNpc npc = new TestNpc();

        for (boolean success : new boolean[]{true, false}) {
            TestPlayer player = recipe();
            Item pet = player.inventory.itemsBag.get(0);
            VuotGioiHanLinhThu.nangCap(player, () -> success);
            for (int id : STATS) check(pet.getOptionParam(id) == (success ? 22 : 18),
                    "Only SD, HP, KI and critical damage percentages change by two: " + id);
            check(pet.getOptionParam(93) == 7 && pet.getOptionParam(30) == 0 && pet.getOptionParam(72) == 3
                    && pet.getOptionParam(21) == 80 && pet.getOptionParam(106) == 1, "Preserve expiry, lock, level, requirement and ability flags");
            for (int[] option : PRESERVED) check(pet.getOptionParam(option[0]) == option[1], "Preserve unrelated option: " + option[0]);
            check(pet.itemOptions.size() == STATS.length + PRESERVED.length, "Do not invent or remove option lines");
            check(readStatFromBag(player.bagPacket, 1522, 50) == (success ? 22 : 18)
                    && readStatFromBag(player.bagPacket, 1522, 5) == (success ? 22 : 18)
                    && readStatFromBag(player.bagPacket, 1522, 0) == 1000
                    && readStatFromBag(player.bagPacket, 1522, 204) == 20, "Client receives four-stat change with flat and mage options preserved");
            check(player.inventory.itemsBag.contains(pet) && pet.template.id == 1522 && pet.quantity == 1, "Keep original pet");
            check(count(player, 14) == 2 && player.inventory.gold == 1234 && player.inventory.ruby == 567, "Exactly one dragon ball; no extra fees");
            check(player.effects.contains(success ? 2 : 3), "Correct result animation");
            check(player.combineNew.itemsCombine.equals(List.of(pet, find(player,14))) && player.bagPackets == 1, "Pet and remaining dragon balls stay selected after inventory refresh");
            VuotGioiHanLinhThu.nangCap(player, () -> success);
            check(pet.getOptionParam(50) == (success ? 24 : 16) && count(player, 14) == 1, "Repeat upgrades accumulate");
        }

        TestPlayer player = recipe();
        Item pet = player.inventory.itemsBag.get(0);
        pet.setOptionParam(5, 1);
        pet.setOptionParam(103, 0);
        pet.setOptionParam(0, 1);
        pet.setOptionParam(6, 0);
        VuotGioiHanLinhThu.nangCap(player, () -> {throw new AssertionError("Below-five Pet must not roll");});
        check(pet.getOptionParam(5) == 1 && pet.getOptionParam(103) == 0
                && pet.getOptionParam(0) == 1 && pet.getOptionParam(6) == 0 && count(player,14)==3, "Below-five target stat blocks without changing any option or charging materials");

        // Every combination of present/missing target lines, on both outcomes.
        for (boolean success : new boolean[]{true, false}) {
            for (int mask = 0; mask < 16; mask++) {
                player = recipe(); pet = player.inventory.itemsBag.get(0);
                for (int i = 0; i < STATS.length; i++) if ((mask & (1 << i)) == 0) {
                    final int id = STATS[i]; pet.itemOptions.removeIf(option -> option.optionTemplate.id == id);
                }
                VuotGioiHanLinhThu.nangCap(player, () -> success);
                for (int i = 0; i < STATS.length; i++) {
                    boolean present = (mask & (1 << i)) != 0;
                    check(pet.hasOption(STATS[i]) == (present || success), "Only success adds missing target lines");
                    if (present || success) check(pet.getOptionParam(STATS[i]) == (present ? (success ? 22 : 18) : 2),
                            "Missing stats start at two, existing stats accumulate or decrease");
                }
                check(pet.itemOptions.size() == PRESERVED.length + (success ? 4 : Integer.bitCount(mask)) && count(player,14) == 2,
                        "Missing-stat recipe consumes one material and changes no unrelated line");
            }
        }
        player = recipe(); pet = player.inventory.itemsBag.get(0); pet.itemOptions.clear();
        VuotGioiHanLinhThu.nangCap(player, () -> true);
        byte[] addedPacket = player.bagPacket;
        check(pet.itemOptions.size() == 4 && Arrays.stream(STATS).allMatch(id ->
                findStat(addedPacket, id) == 2), "Pet with no options receives all four client-visible stats");

        // Screenshot regression: Pet Cuu Vi (1962), four in a stack and ten dragon balls.
        for (boolean success : new boolean[]{true, false}) {
            player = recipe(); pet = player.inventory.itemsBag.get(0);
            pet.template.id = 1962; pet.template.name = "Pet Cửu Vĩ"; pet.template.isUpToUp = true; pet.quantity = 4;
            pet.createTime = 123456789; pet.message = "Keep pet metadata";
            pet.itemOptions.removeIf(option -> option.optionTemplate.id == 5);
            for (int id : new int[]{50,77,103}) pet.setOptionParam(id,25);
            player.inventory.itemsBag.get(1).quantity = 10;
            player.inventory.itemsBag.add(new Item());
            CombineService.gI().showInfoCombine(player,new int[]{0,1});
            check(player.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE && npc.text.contains("xếp chồng"), "Stacked screenshot pet previews with split explanation");
            VuotGioiHanLinhThu.nangCap(player,()->success);
            Item selectedPet=pet;
            Item rest=player.inventory.itemsBag.stream().filter(i->i!=selectedPet && i.isNotNullItem() && i.template.id==1962).findFirst().orElseThrow();
            check(pet.quantity==1 && rest.quantity==3 && count(player,1962)==4 && count(player,14)==9, "One attempt splits one of four pets and consumes one of ten balls");
            for(int id:new int[]{50,77,103})check(pet.getOptionParam(id)==(success?27:23) && rest.getOptionParam(id)==25,"Only selected pet changes; remaining stack keeps original percentage");
            check(pet.hasOption(5)==success && rest.hasOption(5)==false && (!success || pet.getOptionParam(5)==2),"New critical damage line belongs only to successful selected pet");
            for(int[] option:PRESERVED)check(rest.getOptionParam(option[0])==option[1] && pet.getOptionParam(option[0])==option[1],"Both split items preserve unrelated option "+option[0]);
            check(rest.createTime==pet.createTime && rest.message.equals(pet.message) && rest.itemOptions!=pet.itemOptions && rest.itemOptions.get(0)!=pet.itemOptions.get(0),"Original metadata preserved with independent option copies");
            check(readStatFromBag(player.bagPacket,1962,50)==(success?27:23) && countPetRows(player.bagPacket,1962)==2,"Client receives distinct selected pet and unchanged remainder");
            check(player.combineNew.itemsCombine.equals(List.of(pet,find(player,14))),"Only selected single pet and remaining balls stay in combine");
            VuotGioiHanLinhThu.nangCap(player,()->success);
            check(pet.getOptionParam(50)==(success?27:21)&&rest.quantity==3&&rest.getOptionParam(50)==25&&count(player,14)==(success?9:8),"New two-percent critical line stops further upgrades; failure can continue on eligible selected pet");
        }
        player=recipe();pet=player.inventory.itemsBag.get(0);pet.quantity=4;
        VuotGioiHanLinhThu.showInfoCombine(player);
        check(player.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU && npc.text.contains("1 ô hành trang"),"Full bag explains need for split slot");
        VuotGioiHanLinhThu.nangCap(player,()->{throw new AssertionError("No-space recipe must not roll");});
        check(pet.quantity==4 && pet.getOptionParam(50)==20 && count(player,14)==3,"Full bag preserves entire stack and material");
        player=recipe();pet=player.inventory.itemsBag.get(0);pet.quantity=4;player.inventory.itemsBag.get(1).quantity=1;
        VuotGioiHanLinhThu.showInfoCombine(player);check(player.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"Last ball slot can hold remainder even in full bag");
        VuotGioiHanLinhThu.nangCap(player,()->true);
        check(pet.quantity==1 && count(player,1522)==4 && count(player,14)==0 && player.inventory.itemsBag.size()==2,"Reuses consumed ball slot without dropping any pet");
        player=recipe();pet=player.inventory.itemsBag.get(0);pet.quantity=4;player.inventory.itemsBag.add(new Item());
        VuotGioiHanLinhThu.showInfoCombine(player);player.inventory.itemsBag.set(2,item(457,9,1));
        VuotGioiHanLinhThu.nangCap(player,()->{throw new AssertionError("Stale free slot must not roll");});
        check(pet.quantity==4 && count(player,14)==3 && count(player,457)==1,"Free space checked again after preview");
        player=recipe();pet=player.inventory.itemsBag.get(0);pet.quantity=4;player.inventory.itemsBag.add(new Item());pet.setOptionParam(50,65534);
        VuotGioiHanLinhThu.nangCap(player,()->true);
        check(pet.quantity==4 && count(player,14)==3 && !player.inventory.itemsBag.get(2).isNotNullItem(),"Overflow does not split stack or consume material");
        player=recipe();pet=player.inventory.itemsBag.get(0);pet.quantity=4;player.inventory.itemsBag.add(new Item());
        for(int id:STATS)pet.setOptionParam(id,0);
        VuotGioiHanLinhThu.nangCap(player,()->{throw new AssertionError("Zero-stat Pet must not roll");});
        check(pet.quantity==4 && count(player,1522)==4 && count(player,14)==3 && player.bagPacket==null,"Zero-stat stack is not split or charged");
        final TestPlayer stackedPlayer=recipe();stackedPlayer.inventory.itemsBag.get(0).quantity=4;stackedPlayer.inventory.itemsBag.add(new Item());
        CombineService.gI().showInfoCombine(stackedPlayer,new int[]{0,1});npc.confirmMenu(stackedPlayer,0);npc.confirmMenu(stackedPlayer,0);
        check(count(stackedPlayer,1522)==4&&count(stackedPlayer,14)==2,"Repeated old NPC confirmation charges only once while selection is retained");
        int[] retainedSlots=stackedPlayer.combineNew.itemsCombine.stream().mapToInt(stackedPlayer.inventory.itemsBag::indexOf).toArray();
        CombineService.gI().showInfoCombine(stackedPlayer,retainedSlots);npc.confirmMenu(stackedPlayer,0);
        check(count(stackedPlayer,1522)==4&&count(stackedPlayer,14)==1,"Retained stacked-pet selection permits the next deliberate NPC upgrade");

        for (int invalid = 0; invalid < 17; invalid++) {
            player = recipe();
            pet = player.inventory.itemsBag.get(0);
            switch (invalid) {
                case 0 -> player.combineNew.itemsCombine.remove(1);
                case 1 -> player.inventory.itemsBag.get(1).quantity = 0;
                case 2 -> pet.template.type = 5;
                case 3 -> player.inventory.itemsBag.get(1).template.id = 15;
                case 4 -> player.inventory.itemsBag.remove(pet);
                case 5 -> player.combineNew.itemsCombine.set(1, pet);
                case 6 -> pet.quantity = -1;
                case 7 -> { Item extra = item(457, 9, 1); player.inventory.itemsBag.add(extra); player.combineNew.itemsCombine.add(extra); }
                case 8 -> pet.itemOptions = null;
                case 9 -> pet.template.type = 35;
                case 10 -> player.combineNew.itemsCombine.set(0, null);
                case 11 -> pet.setOptionParam(50, -1);
                case 12 -> pet.setOptionParam(50, 65536);
                case 13 -> pet.itemOptions.add(new Item.ItemOption(50, 20));
                case 14 -> pet.itemOptions.add(null);
                case 15 -> pet.itemOptions.add(new Item.ItemOption());
                case 16 -> {
                    pet.itemOptions.clear();
                    for (int i = 0; i < 252; i++) pet.itemOptions.add(new Item.ItemOption(30, 0));
                    pet.addOptionParam(50, 20);
                }
            }
            int before = count(player, 14);
            VuotGioiHanLinhThu.nangCap(player, () -> { throw new AssertionError("Invalid recipe must not roll"); });
            check(count(player, 14) == before, "Invalid/stale recipe has no material cost: " + invalid);
        }

        player = recipe(); pet = player.inventory.itemsBag.get(0); pet.setOptionParam(50, 65534);
        VuotGioiHanLinhThu.nangCap(player, () -> true);
        check(pet.getOptionParam(50) == 65534 && count(player, 14) == 3, "No packet overflow or material loss");
        pet.setOptionParam(50, 65533);
        VuotGioiHanLinhThu.nangCap(player, () -> true);
        check(pet.getOptionParam(50) == 65535 && readStatFromBag(player.bagPacket, 1522, 50) == 65535,
                "Upper unsigned-short boundary survives serialization");

        player = recipe();
        npc.openBaseMenu(player);
        check(npc.buttons.length == 9 && npc.buttons[6].contains("giới hạn"), "Append NPC entry without shifting existing actions");
        npc.confirmMenu(player, 6);
        check(player.combineNew.typeCombine == CombineService.VUOT_GIOI_HAN_LINH_THU, "NPC opens correct combine type");
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(player.openPacket))) {
            check(reader.readUnsignedByte() == 0, "Standard combine packet");
            String instructions = reader.readUTF(); String title = reader.readUTF();
            check(instructions.contains("Ngọc Rồng 1 sao") && instructions.contains("10%")
                    && instructions.contains("-2% SĐ, HP, KI, SĐCM") && instructions.contains("thêm dòng")
                    && instructions.contains(VuotGioiHanLinhThu.MIN_STAT_RULE), "Recipe, four options, threshold and actual odds in client instructions");
            check(title.equals("Vượt giới hạn:\nPet\nTiến hóa Mega"), "Title uses Pet label");
        }
        CombineService.gI().showInfoCombine(player, new int[]{0, 1});
        check(player.idMark.getIndexMenu() == ConstNpc.MENU_START_COMBINE && npc.text.contains("10%")
                && npc.text.contains("+2% SĐ, HP, KI") && npc.text.contains("-2% SĐ, HP, KI"), "Preview routed with real outcomes");
        npc.confirmMenu(player, 2);
        check(count(player, 14) == 3, "Cancel consumes nothing");
        npc.confirmMenu(player, 0);
        check(count(player, 14) == 2, "NPC confirm executes upgrade");

        player = recipe();
        CombineService.gI().showInfoCombine(player, new int[]{0, 0});
        check(player.idMark.getIndexMenu() == ConstNpc.IGNORE_MENU, "Duplicate indices cannot preview");
        player = recipe(); player.inventory.itemsBag.get(1).quantity = 1;
        VuotGioiHanLinhThu.nangCap(player, () -> false);
        check(count(player, 14) == 0 && player.inventory.itemsBag.stream().anyMatch(i -> i.isNotNullItem() && i.template.type == 21), "Last dragon ball consumed, pet remains");

        for (int mapId : new int[]{42, 43, 44, 84, 112}) {
            npc.mapId = mapId;
            player = recipe(); player.zone = new Zone(map(mapId), 0, 20);
            player.combineNew.typeCombine = -1;
            CombineService.gI().openTabCombine(player, CombineService.VUOT_GIOI_HAN_LINH_THU);
            check(player.combineNew.typeCombine == -1, "Cannot open away from Kame: " + mapId);
            player.combineNew.typeCombine = CombineService.VUOT_GIOI_HAN_LINH_THU;
            CombineService.gI().showInfoCombine(player, new int[]{0, 1});
            check(player.idMark.isBaseMenu(), "No preview away from Kame");
            VuotGioiHanLinhThu.nangCap(player, () -> { throw new AssertionError("Wrong map must not roll"); });
            player.idMark.setIndexMenu(ConstNpc.MENU_START_COMBINE);
            npc.confirmMenu(player, 0);
            check(count(player, 14) == 3, "Wrong-map direct/NPC calls have no cost");
        }
        npc.mapId = 5;
        player = recipe(); CombineService.gI().showInfoCombine(player, new int[]{0, 1});
        player.zone = new Zone(map(42), 0, 20); npc.confirmMenu(player, 0);
        check(count(player, 14) == 3, "Moving after preview cannot upgrade");
        player.zone = null;
        CombineService.gI().startCombine(player);
        check(count(player, 14) == 3, "Missing map cannot upgrade");

        int successes = 0;
        for (int attempt = 0; attempt < 2000; attempt++) {
            player = recipe(); pet = player.inventory.itemsBag.get(0);
            player.combineNew.ratioCombine = 100;
            CombineService.gI().startCombine(player);
            if (pet.getOptionParam(50) == 22) successes++;
            else check(pet.getOptionParam(50) == 18, "Production failure subtracts two points");
            check(count(player, 14) == 2, "Production recipe cost");
        }
        check(successes >= 100 && successes <= 300, "Production rate near 10%, independent of shared ratio: " + successes);
        System.out.println("PASS: " + checks + " checks; production successes " + successes + "/2000");
    }

    private static TestPlayer recipe() {
        TestPlayer player = new TestPlayer();
        player.zone = new Zone(kame, 0, 20);
        player.idMark.setIndexMenu(ConstNpc.BASE_MENU);
        player.inventory.gold = 1234; player.inventory.ruby = 567;
        Item pet = item(1522, 21, 1);
        for (int id : STATS) pet.addOptionParam(id, 20);
        for (int[] option : PRESERVED) pet.addOptionParam(option[0], option[1]);
        player.inventory.itemsBag.addAll(List.of(pet, item(14, 12, 3)));
        player.combineNew.itemsCombine.addAll(player.inventory.itemsBag);
        player.combineNew.typeCombine = CombineService.VUOT_GIOI_HAN_LINH_THU;
        return player;
    }

    private static models.map.Map map(int id) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        models.map.Map map = (models.map.Map) ((Unsafe) f.get(null)).allocateInstance(models.map.Map.class);
        map.mapId = id; return map;
    }
    private static Item item(int id, int type, int quantity) {
        Item item = new Item(); item.template = new ItemTemplate();
        item.template.id = (short) id; item.template.type = (byte) type;
        item.template.name = "Test item " + id; item.quantity = quantity; return item;
    }
    private static int count(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).mapToInt(i -> i.quantity).sum();
    }
    private static Item find(Player player, int id) {
        return player.inventory.itemsBag.stream().filter(i -> i.isNotNullItem() && i.template.id == id).findFirst().orElseThrow();
    }
    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }
    private static int findStat(byte[] packet, int id) {
        try { return readStatFromBag(packet, 1522, id); }
        catch (Exception error) { throw new AssertionError(error); }
    }
    private static int countPetRows(byte[] packet,int itemId)throws Exception {
        try(var reader=new DataInputStream(new ByteArrayInputStream(packet))) {
            reader.readUnsignedByte();int slots=reader.readUnsignedByte(),count=0;
            for(int i=0;i<slots;i++) {int template=reader.readShort();if(template==-1)continue;reader.readInt();reader.readUTF();reader.readUTF();int options=reader.readUnsignedByte();for(int j=0;j<options;j++){reader.readUnsignedByte();reader.readUnsignedShort();}if(template==itemId)count++;}
            return count;
        }
    }
    private static int readStatFromBag(byte[] packet, int itemId, int optionId) throws Exception {
        try (DataInputStream reader = new DataInputStream(new ByteArrayInputStream(packet))) {
            reader.readUnsignedByte();
            int count = reader.readUnsignedByte();
            for (int i = 0; i < count; i++) {
                int template = reader.readShort();
                if (template == -1) continue;
                reader.readInt(); reader.readUTF(); reader.readUTF();
                int options = reader.readUnsignedByte();
                for (int j = 0; j < options; j++) {
                    int id = reader.readUnsignedByte(); int param = reader.readUnsignedShort();
                    if (template == itemId && id == optionId) return param;
                }
            }
        }
        throw new AssertionError("Missing item option in inventory packet");
    }
    private static class TestPlayer extends Player {
        final List<Integer> effects = new ArrayList<>(); byte[] openPacket; byte[] bagPacket; int bagPackets;
        @Override public void sendMessage(Message msg) {
            if (msg.command == -81 && msg.getData().length > 0) {
                effects.add((int) msg.getData()[0]);
                if (msg.getData()[0] == 0) openPacket = Arrays.copyOf(msg.getData(), msg.getData().length);
            }
            if (msg.command == -36) { bagPackets++; bagPacket = Arrays.copyOf(msg.getData(), msg.getData().length); }
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
