package models.combine.list;

import consts.ConstNpc;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;
import models.Template.*;
import models.item.Item;
import models.map.Zone;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.CombineService;
import sun.misc.Unsafe;

/** Batch costs, per-attempt randomness, early stops and real NPC/client routing. */
public final class CombineTenAttemptsTest {
    static final int PET=CombineService.VUOT_GIOI_HAN_LINH_THU, MAGE=CombineService.PHAP_SU_HOA,
            RESET=CombineService.TAY_PHAP_SU, COSTUME=CombineService.SIEU_HOA_CAI_TRANG;
    static final int[] MODES={PET,MAGE,RESET,COSTUME};
    static int checks;
    static models.map.Map kame;
    static TestNpc npc;

    public static void main(String[] args)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        kame=(models.map.Map)((Unsafe)f.get(null)).allocateInstance(models.map.Map.class);kame.mapId=5;
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"option #",0));
        npc=new TestNpc();
        for(int mode:MODES) {
            var p=recipe(mode,12);var rolls=new AtomicInteger();
            apply(p,10,()->rolls.getAndIncrement()%3==0);
            int attempts=mode==RESET?1:10;
            fees(p,attempts);check(rolls.get()==(mode==RESET?0:10),"Independent roll for each paid attempt");
            check(p.bags==1&&p.reopens==1,"One final inventory/selection refresh per batch");packet(p);
            check(p.notice.contains(attempts+"/10")&&p.notice.contains("Chỉ trừ chi phí"),"Summary reports actual paid attempts");
            if(mode==PET)for(int id:new int[]{50,77,103,5})check(p.gear.getOptionParam(id)==26,"Mixed Pet outcomes applied sequentially above the minimum");
            if(mode==MAGE){check(p.gear.getOptionParam(200)==4,"Only successes advance mage counter");int total=0;for(int id=201;id<=204;id++)total+=p.gear.getOptionParam(id);check(total==12,"Four successful attempts add four random bonuses of three percent");}
            if(mode==COSTUME){check(p.gear.getOptionParam(205)==4,"Only successes advance costume counter");for(int id:new int[]{50,77,103,5})check(p.gear.getOptionParam(id)==14,"Each costume success receives its bonus");}
            if(mode==RESET){for(int id=200;id<=204;id++)check(!p.gear.hasOption(id),"Reset removes only mage options");check(p.gear.getOptionParam(50)==10,"Reset preserves base stats");}
        }
        var randomMage=recipe(MAGE,10);var statRolls=new AtomicInteger();
        PhapSuHoa.combine(randomMage,10,()->true,bound->statRolls.getAndIncrement()%bound);
        fees(randomMage,8);check(statRolls.get()==8&&randomMage.gear.getOptionParam(200)==8,"Each successful batch attempt rolls its own stat and stops at cap");
        for(int id=201;id<=204;id++)check(randomMage.gear.getOptionParam(id)==6,"Eight random successful rolls may repeat and accumulate independently");
        var cappedMage=recipe(MAGE,10);
        PhapSuHoa.combine(cappedMage,10,()->true,bound->0);
        fees(cappedMage,8);check(cappedMage.gear.getOptionParam(200)==8,"Repeated first-choice draws still reach eight-success cap");
        check(cappedMage.gear.getOptionParam(201)==9&&cappedMage.gear.getOptionParam(202)==9
                &&cappedMage.gear.getOptionParam(203)==6&&!cappedMage.gear.hasOption(204),"Batch skips each stat as soon as it reaches nine percent");
        for(int mode:MODES)for(int available=0;available<=12;available++) {
            var p=recipe(mode,available);var rolls=new AtomicInteger();
            apply(p,10,()->{rolls.incrementAndGet();return false;});
            int expected=Math.min(mode==RESET?1:10,available);
            fees(p,expected);check(rolls.get()==(mode==RESET?0:expected),"No rolls or costs after material runs out");
            check(p.bags==(expected>0?1:0),"Invalid/empty batch does not refresh inventory");
            if(expected>0)packet(p);
        }
        for(int funds=0;funds<=12;funds++) {
            var p=recipe(MAGE,20);p.initialGems=p.inventory.gem=funds*200+199;
            var rolls=new AtomicInteger();apply(p,10,()->{rolls.incrementAndGet();return false;});
            fees(p,Math.min(10,funds));check(p.inventory.gem>=199&&rolls.get()==Math.min(10,funds),"Stop before unaffordable green gem fee");
        }
        for(int mode:new int[]{MAGE,COSTUME}) {
            var p=recipe(mode,20);int counter=mode==MAGE?200:205,cap=mode==MAGE?8:20;
            p.gear.addOptionParam(counter,cap-1);var rolls=new AtomicInteger();
            apply(p,10,()->{rolls.incrementAndGet();return true;});fees(p,1);
            check(p.gear.getOptionParam(counter)==cap&&rolls.get()==1,"Stop immediately at successful-upgrade cap");
            apply(p,10,()->{throw new AssertionError("Cap must block RNG");});fees(p,1);
        }
        var p=recipe(MAGE,20);p.gear.addOptionParam(200,6);var rolls=new AtomicInteger();
        apply(p,10,()->{int i=rolls.incrementAndGet();return i==3||i==5;});fees(p,5);
        check(rolls.get()==5&&p.gear.getOptionParam(200)==8,"Failure fees count before reaching mage success cap");

        p=recipe(COSTUME,20);p.combineNew.itemsCombine.remove(p.material);p.inventory.itemsBag.remove(p.material);
        for(int quantity:new int[]{1,2,3,4,10}){Item stack=item(674,27,quantity);p.combineNew.itemsCombine.add(stack);p.inventory.itemsBag.add(stack);}
        p.initialMaterial=20;apply(p,10,()->false);fees(p,10);packet(p);
        check(p.combineNew.itemsCombine.stream().noneMatch(i->i.template.id==674),"Depleted split material stacks are removed during the batch");

        p=recipe(PET,10);p.gear.quantity=4;p.gear.template.id=1962;
        apply(p,10,()->true);fees(p,10);packet(p);
        Item remainder=p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==1962&&i.quantity==3).findFirst().orElseThrow();
        check(p.gear.quantity==1&&p.gear.getOptionParam(50)==50&&remainder.getOptionParam(50)==30,"All ten attempts affect only chosen single Pet");
        check(p.combineNew.itemsCombine.equals(List.of(p.gear)),"Untouched Pet stack is never selected automatically");
        p=recipe(PET,10);p.gear.quantity=4;p.inventory.itemsBag.removeIf(i->!i.isNotNullItem());
        apply(p,10,()->{throw new AssertionError("Full bag must block RNG");});fees(p,0);
        p=recipe(PET,1);p.gear.quantity=4;p.inventory.itemsBag.removeIf(i->!i.isNotNullItem());
        apply(p,10,()->false);fees(p,1);check(p.gear.quantity==1,"Final ball slot can hold untouched Pet stack");packet(p);

        for(int mode:MODES) {
            for(int invalid:new int[]{-1,0,2,11,100,Integer.MAX_VALUE}) {
                p=recipe(mode,20);apply(p,invalid,()->{throw new AssertionError("Invalid count must block RNG");});fees(p,0);
            }
            p=recipe(mode,20);p.zone=null;apply(p,10,()->{throw new AssertionError("Wrong map must block RNG");});fees(p,0);
            p=recipe(mode,20);p.inventory.itemsBag.remove(p.gear);apply(p,10,()->{throw new AssertionError("Foreign item must block RNG");});fees(p,0);
            p=recipe(mode,2);preview(p);
            check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE&&npc.buttons.length==3&&npc.buttons[1].contains("10 lần"),"NPC offers 1 / 10 / cancel in order");
            npc.confirmMenu(p,2);fees(p,0);
            npc.confirmMenu(p,1);fees(p,mode==RESET?1:2);packet(p);
            check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU,"Batch confirmation expires after use");
            npc.confirmMenu(p,1);fees(p,mode==RESET?1:2);
            p=recipe(mode,2);preview(p);npc.confirmMenu(p,0);fees(p,1);
            if(mode!=RESET){preview(p);npc.confirmMenu(p,1);fees(p,2);packet(p);}
            p=recipe(mode,12);preview(p);npc.confirmMenu(p,1);
            int paid=p.initialMaterial-count(p,p.materialId);
            if(mode==COSTUME)paid/=2;
            check(paid==(mode==RESET?1:mode==MAGE&&p.gear.getOptionParam(200)==8?paid:10),"Real NPC ten-button runs exactly ten except early reset/mage cap");
            check(paid>=1&&paid<=10,"NPC ten-button cannot exceed ten attempts");fees(p,paid);packet(p);
        }
        p=recipe(RESET,20);p.gear.itemOptions.removeIf(i->i.optionTemplate.id>=200&&i.optionTemplate.id<=204);
        apply(p,10,()->{throw new AssertionError("Reset never rolls");});fees(p,0);
        System.out.println("PASS: "+checks+" batch cost, RNG, material/gem/cap stop, retained packet and NPC routing checks");
    }

    static void apply(TestPlayer p,int attempts,BooleanSupplier roll){switch(p.combineNew.typeCombine){
        case PET->VuotGioiHanLinhThu.nangCap(p,attempts,roll);
        case MAGE,RESET->PhapSuHoa.combine(p,attempts,roll);
        case COSTUME->SieuHoaCaiTrang.nangCap(p,attempts,roll,()->1);
        default->throw new AssertionError();
    }}
    static void preview(TestPlayer p){CombineService.gI().showInfoCombine(p,p.combineNew.itemsCombine.stream().mapToInt(p.inventory.itemsBag::indexOf).toArray());}
    static TestPlayer recipe(int mode,int attempts){
        var p=new TestPlayer();p.zone=new Zone(kame,0,20);p.inventory.gem=p.initialGems=10000;p.combineNew.typeCombine=mode;
        p.materialId=mode==PET?14:mode==MAGE?1455:mode==RESET?1456:674;
        p.initialMaterial=attempts*(mode==COSTUME?2:1);p.initialEggs=attempts;
        p.gear=item(100,mode==PET?21:mode==COSTUME?5:0,1);
        for(int id:new int[]{50,77,103,5})p.gear.addOptionParam(id,mode==PET?30:10);p.gear.addOptionParam(30,0);
        if(mode==RESET){p.gear.addOptionParam(200,3);for(int id=201;id<=204;id++)p.gear.addOptionParam(id,9);}
        p.material=item(p.materialId,27,p.initialMaterial);
        p.inventory.itemsBag.addAll(List.of(p.material,new Item(),p.gear));p.combineNew.itemsCombine.addAll(List.of(p.gear,p.material));
        if(mode==COSTUME){Item eggs=item(568,27,attempts);p.inventory.itemsBag.add(eggs);p.combineNew.itemsCombine.add(eggs);}
        return p;
    }
    static void fees(TestPlayer p,int attempts){
        check(count(p,p.materialId)==p.initialMaterial-attempts*(p.combineNew.typeCombine==COSTUME?2:1),"Exact primary material fee");
        if(p.combineNew.typeCombine==COSTUME)check(count(p,568)==p.initialEggs-attempts,"Exact egg fee");
        check(p.inventory.gem==p.initialGems-((p.combineNew.typeCombine==MAGE||p.combineNew.typeCombine==RESET)?attempts*200:0),"Exact green gem fee");
    }
    static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static Item item(int id,int type,int quantity){var i=new Item();i.template=new ItemTemplate();i.template.id=(short)id;i.template.type=(byte)type;i.template.name="Item "+id;i.quantity=quantity;return i;}
    static void packet(TestPlayer p)throws Exception {
        try(var r=new DataInputStream(new ByteArrayInputStream(p.reopen))){check(r.readUnsignedByte()==1,"Reopen action");check(r.readUnsignedByte()==p.combineNew.itemsCombine.size(),"Retained selection count");for(Item i:p.combineNew.itemsCombine){int slot=r.readUnsignedByte();check(p.inventory.itemsBag.get(slot)==i&&i.quantity>0,"Fresh bag index and positive retained quantity");}check(r.available()==0,"No extra stale indices");}
        check(p.events.get(p.events.size()-2)==-36&&p.events.get(p.events.size()-1)==1,"Final bag arrives before selection");
    }
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class TestPlayer extends Player {
        Item gear,material;int materialId,initialMaterial,initialEggs,initialGems,bags,reopens;byte[] reopen;String notice="";List<Integer> events=new ArrayList<>();
        @Override public void sendMessage(Message m){
            if(m.command==-36){bags++;events.add(-36);}
            if(m.command==-81&&m.getData().length>0&&m.getData()[0]==1){reopens++;reopen=m.getData().clone();events.add(1);}
            if(m.command==-25)try(var r=new DataInputStream(new ByteArrayInputStream(m.getData()))){notice=r.readUTF();}catch(IOException e){throw new AssertionError(e);}
        }
    }
    static final class TestNpc extends BaHatMit {
        String[] buttons;
        TestNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}
        @Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}
        @Override public void createOtherMenu(Player p,int index,String text,String...buttons){this.buttons=buttons;p.idMark.setIndexMenu(index);}
    }
}
