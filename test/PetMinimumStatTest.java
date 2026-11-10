package models.combine.list;

import consts.ConstNpc;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import models.Template.ItemOptionTemplate;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import server.Manager;
import services.CombineService;
import sun.misc.Unsafe;

/** Real recipe, batch fees/selection and NPC confirmation using isolated synthetic inventories. */
public final class PetMinimumStatTest {
    static int checks;static final int[] STATS={50,77,103,5};
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static CombineTenAttemptsTest.TestPlayer recipe(int stat,int value){var p=CombineTenAttemptsTest.recipe(CombineTenAttemptsTest.PET,10);p.gear.setOptionParam(stat,value);return p;}
    public static void main(String[] args)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var map=(models.map.Map)((Unsafe)f.get(null)).allocateInstance(models.map.Map.class);map.mapId=5;CombineTenAttemptsTest.kame=map;
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"option #",0));var npc=new TestNpc();
        for(int stat:STATS)for(int value=0;value<5;value++)for(int attempts:new int[]{1,10}) {
            var p=recipe(stat,value);var before=p.gear.itemOptions.stream().map(o->o.optionTemplate.id+":"+o.param).toList();
            VuotGioiHanLinhThu.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU&&npc.text.contains("5%"),"Preview refuses each existing target option below five");
            VuotGioiHanLinhThu.nangCap(p,attempts,()->{throw new AssertionError("Low stat must be checked before RNG");});
            check(p.gear.itemOptions.stream().map(o->o.optionTemplate.id+":"+o.param).toList().equals(before),"Rejected upgrade preserves every option");
            check(CombineTenAttemptsTest.count(p,14)==10&&p.bags==0&&p.reopens==0&&p.gear.quantity==1,"No fee, split, animation or bag refresh on initial rejection");
            check(p.notice.contains("5%"),"Direct upgrade explains threshold");
        }
        for(int stat:STATS)for(boolean success:new boolean[]{false,true}) {
            var p=recipe(stat,5);VuotGioiHanLinhThu.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"Exactly five is allowed");
            VuotGioiHanLinhThu.nangCap(p,1,()->success);check(p.gear.getOptionParam(stat)==(success?7:3)&&CombineTenAttemptsTest.count(p,14)==9,"Boundary pays one normal attempt and retains +/-2 rule");
            if(!success){int bags=p.bags;VuotGioiHanLinhThu.nangCap(p,10,()->{throw new AssertionError("Failed boundary cannot roll again");});check(p.bags==bags&&CombineTenAttemptsTest.count(p,14)==9,"Below-five result blocks follow-up batch without charging");}
        }
        for(int stat:STATS)for(int start:new int[]{5,6,7,8,10,20,24,25,30}) {
            var p=recipe(stat,start);var rolls=new AtomicInteger();VuotGioiHanLinhThu.nangCap(p,10,()->{rolls.incrementAndGet();return false;});
            int paid=Math.min(10,(start-5)/2+1);check(rolls.get()==paid&&CombineTenAttemptsTest.count(p,14)==10-paid,"Ten-attempt batch rechecks each time and charges exactly paid attempts");
            check(p.gear.getOptionParam(stat)==start-paid*2&&p.bags==1&&p.reopens==1,"Final stat and one retained-selection refresh are correct");
            if(paid<10)check(p.notice.contains(paid+"/10")&&p.notice.contains("5%"),"Early-stop summary identifies actual attempts and threshold");
        }
        var p=recipe(50,5);var rolls=new AtomicInteger();VuotGioiHanLinhThu.nangCap(p,10,()->rolls.getAndIncrement()==0);
        check(rolls.get()==3&&p.gear.getOptionParam(50)==3&&CombineTenAttemptsTest.count(p,14)==7&&p.notice.contains("3/10"),"Mixed success/failures re-evaluate current stats, not initial stats");
        p=recipe(50,4);p.gear.quantity=4;VuotGioiHanLinhThu.nangCap(p,10,()->{throw new AssertionError("Low stack must not roll");});check(p.gear.quantity==4&&CombineTenAttemptsTest.count(p,14)==10&&p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.type==21).count()==1,"Rejected stacked Pet remains intact");
        p=recipe(50,20);p.gear.addOptionParam(93,0);p.gear.addOptionParam(200,1);p.gear.addOptionParam(201,2);VuotGioiHanLinhThu.nangCap(p,1,()->true);
        check(p.gear.getOptionParam(50)==22&&p.gear.getOptionParam(30)==0&&p.gear.getOptionParam(93)==0&&p.gear.getOptionParam(201)==2,"Low metadata/mage options do not block or change");
        p=recipe(50,25);p.gear.itemOptions.removeIf(o->o.optionTemplate.id==5);VuotGioiHanLinhThu.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE,"An absent line is not an existing below-five stat");
        VuotGioiHanLinhThu.nangCap(p,10,()->true);check(p.gear.getOptionParam(5)==2&&p.gear.getOptionParam(50)==27&&CombineTenAttemptsTest.count(p,14)==9,"New two-percent line makes batch stop before its next paid attempt");
        p=recipe(50,20);VuotGioiHanLinhThu.showInfoCombine(p);p.gear.setOptionParam(50,4);npc.confirmMenu(p,1);check(CombineTenAttemptsTest.count(p,14)==10&&p.gear.getOptionParam(50)==4,"NPC confirmation revalidates after preview");
        check(VuotGioiHanLinhThu.SUCCESS_RATE==10&&VuotGioiHanLinhThu.MIN_STAT_PERCENT==5,"Production success rate unchanged and threshold exact");
        System.out.println("PASS: "+checks+" all-four-option lower bounds, exact-five eligibility, pre-RNG/pre-fee rejection, stacked Pet, batch stops, retained selection, unrelated/missing options and stale NPC confirmation checks");
    }
    static final class TestNpc extends BaHatMit {String text;TestNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}@Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}@Override public void createOtherMenu(Player p,int index,String text,String...buttons){this.text=text;p.idMark.setIndexMenu(index);}}
}
