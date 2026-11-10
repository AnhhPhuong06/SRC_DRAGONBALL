package models.combine.list;

import consts.ConstNpc;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import models.Template.*;
import models.item.Item;
import models.map.Zone;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.CombineService;
import sun.misc.Unsafe;

/** Verify retained selections reach the real client packet and NPC confirmation paths. */
public final class CombineRetainedSelectionTest {
    static int checks;static models.map.Map kame;static TestNpc npc;
    static final int PET=CombineService.VUOT_GIOI_HAN_LINH_THU,MAGE=CombineService.PHAP_SU_HOA,RESET=CombineService.TAY_PHAP_SU,COSTUME=CombineService.SIEU_HOA_CAI_TRANG;
    public static void main(String[] args)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);kame=(models.map.Map)((Unsafe)f.get(null)).allocateInstance(models.map.Map.class);kame.mapId=5;
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"option #",0));npc=new TestNpc();
        for(int mode:new int[]{PET,MAGE,RESET,COSTUME})for(boolean success:new boolean[]{true,false}) {
            if(mode==RESET&&!success)continue;
            var p=recipe(mode,4);Item gear=p.gear;List<Item> original=List.copyOf(p.combineNew.itemsCombine);
            apply(p,success);check(p.combineNew.itemsCombine.equals(original),"Keep the same equipment and material objects on success/failure: "+mode);packet(p);
            check(p.inventory.itemsBag.contains(gear)&&gear.quantity==1,"Keep original equipment");checkFees(p,1);
            if(mode==RESET){int gems=p.inventory.gem;int charms=count(p,1456);PhapSuHoa.combine(p,()->{throw new AssertionError("Reset must not roll");});check(p.inventory.gem==gems&&count(p,1456)==charms,"Reset of already-clean equipment cannot charge again");}
            else{apply(p,success);checkFees(p,2);check(p.combineNew.itemsCombine.equals(original),"Next attempt uses retained selection");packet(p);}
        }
        for(int mode:new int[]{PET,MAGE,RESET,COSTUME}) {
            var p=recipe(mode,1);apply(p,true);
            check(p.combineNew.itemsCombine.equals(List.of(p.gear)),"Only equipment remains after final material is consumed: "+mode);packet(p);
            check(count(p,p.material)==0,"Final material consumed exactly once");int gems=p.inventory.gem;
            apply(p,false);check(p.inventory.gem==gems&&p.combineNew.itemsCombine.equals(List.of(p.gear)),"Insufficient retained recipe consumes nothing");
        }
        for(int mode:new int[]{PET,MAGE,RESET,COSTUME}) {
            var p=recipe(mode,4);preview(p);check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"First NPC preview valid");
            npc.confirmMenu(p,0);checkFees(p,1);packet(p);
            npc.confirmMenu(p,0);checkFees(p,1);
            preview(p);
            if(mode==RESET){check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU,"Clean equipment is rejected by subsequent reset preview");npc.confirmMenu(p,0);checkFees(p,1);}
            else{check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"Retained indices open next NPC confirmation");npc.confirmMenu(p,0);checkFees(p,2);packet(p);}
        }
        var p=recipe(MAGE,4);p.inventory.gem=200;apply(p,false);int stones=count(p,1455);apply(p,false);check(p.inventory.gem==0&&count(p,1455)==stones,"Insufficient green gems blocks next retained attempt");
        p=recipe(MAGE,4);p.gear.addOptionParam(200,7);apply(p,true);stones=count(p,1455);int gems=p.inventory.gem;apply(p,true);
        check(p.gear.getOptionParam(200)==8&&count(p,1455)==stones&&p.inventory.gem==gems,"Retained selection still enforces eight successful mage upgrades");
        p=recipe(COSTUME,4);p.gear.addOptionParam(205,19);apply(p,true);stones=count(p,674);apply(p,true);check(p.gear.getOptionParam(205)==20&&count(p,674)==stones,"Retained selection still enforces costume success limit");
        p=recipe(PET,4);p.gear.quantity=4;p.gear.template.id=1962;apply(p,true);
        Item rest=p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==1962&&i.quantity==3).findFirst().orElseThrow();
        check(p.combineNew.itemsCombine.contains(p.gear)&&!p.combineNew.itemsCombine.contains(rest),"Stacked Pet retains only the chosen single Pet");packet(p);
        apply(p,true);check(p.gear.getOptionParam(50)==14&&rest.getOptionParam(50)==10&&rest.quantity==3,"Next retained attempt never upgrades the untouched Pet stack");packet(p);
        System.out.println("PASS: "+checks+" retained item/packet/NPC checks for Pet, mage upgrade, mage reset and costume; fees and limits preserved");
    }
    static void preview(TestPlayer p){int[] slots=p.combineNew.itemsCombine.stream().mapToInt(p.inventory.itemsBag::indexOf).toArray();CombineService.gI().showInfoCombine(p,slots);}
    static void apply(TestPlayer p,boolean success){
        switch(p.combineNew.typeCombine){case PET->VuotGioiHanLinhThu.nangCap(p,()->success);case MAGE,RESET->PhapSuHoa.combine(p,()->success);case COSTUME->SieuHoaCaiTrang.nangCap(p,()->success,()->1);default->throw new AssertionError();}
    }
    static void checkFees(TestPlayer p,int attempts){
        check(count(p,p.material)==p.initialMaterial-attempts*(p.combineNew.typeCombine==COSTUME?2:1),"Exact primary material fee");
        if(p.combineNew.typeCombine==COSTUME)check(count(p,568)==p.initialEggs-attempts,"Exact egg fee");
        check(p.inventory.gem==4000-((p.combineNew.typeCombine==MAGE||p.combineNew.typeCombine==RESET)?200*attempts:0),"Exact green gem fee");
    }
    static TestPlayer recipe(int mode,int attempts){
        var p=new TestPlayer();p.zone=new Zone(kame,0,20);p.inventory.gem=4000;p.combineNew.typeCombine=mode;
        p.material=mode==PET?14:mode==MAGE?1455:mode==RESET?1456:674;p.initialMaterial=attempts*(mode==COSTUME?2:1);p.initialEggs=attempts;
        p.gear=item(100,mode==PET?21:mode==COSTUME?5:0,1);for(int id:new int[]{50,77,103,5})p.gear.addOptionParam(id,10);p.gear.addOptionParam(30,0);
        if(mode==RESET){p.gear.addOptionParam(200,3);p.gear.addOptionParam(201,9);}
        Item material=item(p.material,27,p.initialMaterial);
        // A hole before equipment ensures the reopen packet must use compacted inventory indices.
        p.inventory.itemsBag.addAll(List.of(material,new Item(),p.gear));p.combineNew.itemsCombine.addAll(List.of(p.gear,material));
        if(mode==COSTUME){Item eggs=item(568,27,attempts);p.inventory.itemsBag.add(eggs);p.combineNew.itemsCombine.add(eggs);}return p;
    }
    static Item item(int id,int type,int quantity){var i=new Item();i.template=new ItemTemplate();i.template.id=(short)id;i.template.type=(byte)type;i.template.name="Item "+id;i.quantity=quantity;return i;}
    static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static void packet(TestPlayer p)throws Exception {
        try(var r=new DataInputStream(new ByteArrayInputStream(p.reopen))){check(r.readUnsignedByte()==1,"Standard reopen action");check(r.readUnsignedByte()==p.combineNew.itemsCombine.size(),"Accurate retained item count");for(Item i:p.combineNew.itemsCombine){int slot=r.readUnsignedByte();check(slot<p.inventory.itemsBag.size()&&p.inventory.itemsBag.get(slot)==i&&i.quantity>0,"Retained item points to fresh inventory slot");}check(r.available()==0,"No stale packet indices");}
        check(p.events.get(p.events.size()-2)==-36&&p.events.get(p.events.size()-1)==1,"Bag refresh precedes retained selection");
    }
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static final class TestPlayer extends Player{Item gear;int material,initialMaterial,initialEggs;byte[] reopen;final List<Integer> events=new ArrayList<>();@Override public void sendMessage(Message m){if(m.command==-36)events.add(-36);if(m.command==-81&&m.getData().length>0&&m.getData()[0]==1){reopen=m.getData().clone();events.add(1);}}}
    static final class TestNpc extends BaHatMit{TestNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}@Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}@Override public void createOtherMenu(Player p,int index,String text,String...buttons){p.idMark.setIndexMenu(index);}}
}
