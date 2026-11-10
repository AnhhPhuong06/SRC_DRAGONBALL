package models.combine.list;

import consts.ConstNpc;
import java.lang.reflect.Field;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import models.Template.*;
import models.item.Item;
import models.map.Zone;
import models.npc.npc_list.BaHatMit;
import models.player.Player;
import network.io.Message;
import server.Manager;
import services.CombineService;
import utils.CombineUtil;
import sun.misc.Unsafe;

/** Exercise preview, NPC dispatch and actual inventory mutations without a DB. */
public final class EighthCrystalSocketTest {
    static int checks;static TestNpc npc;static models.map.Map kame;
    public static void main(String[] args)throws Exception {
        var unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        kame=(models.map.Map)((Unsafe)unsafeField.get(null)).allocateInstance(models.map.Map.class);kame.mapId=5;
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        npc=new TestNpc();
        for(int stoneId:new int[]{14,15,16,17,18,19,20,441,442,443,444,445,446,447,1359}) {
            TestPlayer p=recipe(7,8,stoneId,0,true,10);Item gear=p.inventory.itemsBag.get(0),stone=p.inventory.itemsBag.get(1);
            int option=CombineUtil.getOptionDaPhaLe(stone),bonus=CombineUtil.getParamDaPhaLe(stone);
            EpSaoTrangBi.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE&&npc.text.contains("Dùi đục")&&npc.text.contains("Hematit"),"Preview lists eighth-star materials");
            EpSaoTrangBi.epSaoTrangBi(p);success(p,gear,option,bonus,9);
            String after=snapshot(p);EpSaoTrangBi.epSaoTrangBi(p);check(snapshot(p).equals(after),"Replay costs nothing after eighth socket");
        }
        for(int type:new int[]{5,11,21,23,24})for(int stoneId=1577;stoneId<=1581;stoneId++) {
            var p=recipe(7,8,stoneId,type,true,1);Item gear=p.inventory.itemsBag.get(0),stone=p.inventory.itemsBag.get(1);
            EpSaoTrangBi.epSaoTrangBi(p);success(p,gear,CombineUtil.getOptionDaPhaLe(stone),CombineUtil.getParamDaPhaLe(stone),0);
        }
        for(int stoneId=1364;stoneId<=1368;stoneId++) {
            var p=recipe(7,8,stoneId,0,true,3);Item gear=p.inventory.itemsBag.get(0);
            EpSaoTrangBi.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.MENU_START_COMBINE&&npc.text.contains("Dùi đục"),"Special stat stones show same eighth socket requirement");
            EpSaoTrangBi.epSaoTrangBi(p);check(gear.getOptionParam(102)==8&&count(p,stoneId)==2&&count(p,1019)==2&&count(p,1020)==2,"Special stone delegation consumes eighth-star materials");
            p=recipe(7,8,stoneId,0,false,3);rejectNoChange(p,()->EpDaTangChiSo.epDa(current),"Direct special stone route cannot bypass materials");
        }
        for(int stars=0;stars<7;stars++) {
            var p=recipe(stars,8,441,0,false,10);Item gear=p.inventory.itemsBag.get(0);
            EpSaoTrangBi.showInfoCombine(p);EpSaoTrangBi.epSaoTrangBi(p);
            check(gear.getOptionParam(102)==stars+1&&count(p,441)==9&&p.inventory.gem==1000-CombineUtil.getGemEpSao(stars),"Earlier stars keep two-item recipe and existing gem fee");
            p=recipe(stars,8,1366,0,false,10);gear=p.inventory.itemsBag.get(0);EpDaTangChiSo.epDa(p);
            check(gear.getOptionParam(102)==stars+1&&gear.getOptionParam(50)==5&&p.inventory.gem==1000,"Earlier special stones remain unchanged");
        }
        for(boolean extra:new boolean[]{false,true})for(int slots:new int[]{0,7}) {
            var p=recipe(7,slots,441,0,extra,2);rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"No eighth empty socket cannot consume items");
        }
        var p=recipe(7,8,441,0,false,2);EpSaoTrangBi.showInfoCombine(p);check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU&&npc.text.contains("thứ 8"),"Missing materials blocked in preview");
        rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Missing both materials rejected on actual execution");
        for(int missing:new int[]{2,3}) {
            p=recipe(7,8,441,0,true,2);p.combineNew.itemsCombine.remove(missing);
            rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Missing one material rejected");
        }
        for(int role=0;role<4;role++) {
            p=recipe(7,8,441,0,true,2);p.inventory.itemsBag.get(role).quantity=0;rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Empty stack rejected");
            p=recipe(7,8,441,0,true,2);p.inventory.itemsBag.remove(role);rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Off-bag selection rejected");
        }
        p=recipe(7,8,441,0,true,2);p.combineNew.itemsCombine.set(3,p.combineNew.itemsCombine.get(2));rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Duplicate selected object rejected");
        p=recipe(7,8,441,0,true,2);p.inventory.itemsBag.get(0).addOptionParam(102,0);p.inventory.itemsBag.get(0).itemOptions.add(new Item.ItemOption(102,7));rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Duplicate star counter rejected");
        p=recipe(6,8,441,0,true,2);rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Extra materials only accepted for eighth star");
        p=recipe(7,8,441,0,true,2);EpSaoTrangBi.showInfoCombine(p);p.inventory.itemsBag.remove(3);rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Revalidate inventory after preview");
        p=recipe(6,8,441,0,false,2);p.inventory.gem=0;p.combineNew.gemCombine=0;rejectNoChange(p,()->EpSaoTrangBi.epSaoTrangBi(current),"Stale zero fee cannot bypass actual gem fee");

        for(int shift=0;shift<4;shift++) {
            p=recipe(7,8,441,0,true,1);Collections.rotate(p.combineNew.itemsCombine,shift);EpSaoTrangBi.epSaoTrangBi(p);check(p.inventory.itemsBag.get(0).getOptionParam(102)==8,"Material selection order independent");
        }
        p=recipe(7,8,441,0,true,1);Item gear=p.inventory.itemsBag.get(0);p.combineNew.itemsCombine.clear();
        CombineService.gI().showInfoCombine(p,new int[]{0,1,2,3});npc.confirmMenu(p,0);check(gear.getOptionParam(102)==8&&count(p,1019)==0&&count(p,1020)==0,"Actual NPC accepts four items and dispatches eighth socket");
        p=recipe(7,8,441,0,true,3);final TestPlayer parallel=p;CountDownLatch start=new CountDownLatch(1);AtomicReference<Throwable> failure=new AtomicReference<>();
        Runnable work=()->{try{start.await();EpSaoTrangBi.epSaoTrangBi(parallel);}catch(Throwable error){failure.set(error);}};
        var first=new Thread(work);var second=new Thread(work);first.start();second.start();start.countDown();first.join();second.join();
        check(failure.get()==null&&p.inventory.itemsBag.get(0).getOptionParam(102)==8&&count(p,441)==2&&count(p,1019)==2&&count(p,1020)==2,"Concurrent duplicate execution charges once");
        System.out.println("PASS: "+checks+" eighth crystal socket recipe, NPC, material fees, rejection, replay and earlier-star regression checks");
    }
    static TestPlayer current;
    static void rejectNoChange(TestPlayer player,Runnable action,String message){current=player;String before=snapshot(player);action.run();check(snapshot(player).equals(before),message);}
    static void success(TestPlayer p,Item gear,int option,int bonus,int remaining){
        check(gear.getOptionParam(102)==8&&gear.getOptionParam(107)==8&&gear.getOptionParam(option)==bonus,"Eighth socket adds original stone stat without changing socket count");
        check(count(p,1019)==remaining&&count(p,1020)==remaining&&count(p,p.stoneId)==remaining,"Exactly one of each material consumed");
        check(p.inventory.itemsBag.get(0)==gear&&gear.getOptionParam(30)==0&&gear.hasOption(30)&&gear.getOptionParam(200)==2,"Original gear and unrelated options preserved");
        check(p.inventory.gem==1000&&p.inventory.gold==12345&&p.inventory.ruby==99&&p.combineNew.itemsCombine.isEmpty()&&p.effects.contains(2),"Existing eighth fee and success effect preserved; selection cleared");
    }
    static TestPlayer recipe(int used,int slots,int stoneId,int type,boolean extras,int quantity){
        var p=new TestPlayer();p.zone=new Zone(kame,0,20);p.inventory.gem=1000;p.inventory.gold=12345;p.inventory.ruby=99;p.stoneId=stoneId;
        var gear=item(0,type,1);gear.addOptionParam(102,used);gear.addOptionParam(107,slots);gear.addOptionParam(30,0);gear.addOptionParam(200,2);
        var stone=item(stoneId,stoneId>=441&&stoneId<=447||stoneId==1359?30:27,quantity);if(stoneId==1359)stone.addOptionParam(77,7);
        p.inventory.itemsBag.addAll(List.of(gear,stone));if(extras)p.inventory.itemsBag.addAll(List.of(item(1019,27,quantity),item(1020,27,quantity)));
        p.combineNew.typeCombine=CombineService.EP_SAO_TRANG_BI;p.combineNew.itemsCombine.addAll(p.inventory.itemsBag);return p;
    }
    static Item item(int id,int type,int quantity){var item=new Item();item.template=new ItemTemplate();item.template.id=(short)id;item.template.type=(byte)type;item.template.name="Item "+id;item.quantity=quantity;return item;}
    static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static String snapshot(Player p){StringBuilder b=new StringBuilder().append(p.inventory.gem).append(':').append(p.inventory.gold).append(':').append(p.inventory.ruby);for(var i:p.inventory.itemsBag){b.append('|').append(System.identityHashCode(i)).append(':').append(i.quantity).append(':').append(i.template==null?-1:i.template.id);for(var o:i.itemOptions)b.append('/').append(o.optionTemplate.id).append('=').append(o.param);}return b.toString();}
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static final class TestPlayer extends Player {int stoneId;final List<Integer> effects=new ArrayList<>();@Override public void sendMessage(Message message){if(message.command==-81&&message.getData().length>0)effects.add((int)message.getData()[0]);}}
    static final class TestNpc extends BaHatMit {String text;TestNpc(){super(5,1,0,0,ConstNpc.BA_HAT_MIT,0);}@Override public boolean canOpenNpc(Player p){p.idMark.setNpcChose(this);return true;}@Override public void createOtherMenu(Player p,int index,String text,String...buttons){this.text=text;p.idMark.setIndexMenu(index);}}
}
