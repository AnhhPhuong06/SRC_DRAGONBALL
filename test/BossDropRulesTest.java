package services;

import java.nio.file.*;
import java.lang.reflect.*;
import java.util.*;
import models.Template.*;
import models.boss.*;
import models.item.Item;
import models.map.*;
import models.player.*;
import server.Manager;
import sun.misc.Unsafe;

public final class BossDropRulesTest {
    static int checks;
    static BossDropRules.Rule rule(int id,String key,int map,double chance,boolean enabled) {
        return new BossDropRules.Rule(id,"Boss drop",enabled,map,key,568,chance,2,3,List.of(new MobDropRules.Option(50,12),new MobDropRules.Option(205,20)));
    }
    public static final class FixtureBoss extends Boss {
        int calls;boolean fail,clearZone;
        FixtureBoss()throws Exception{super(-99999,BossesData.FIDE_DAI_CA_1);}
        @Override public void reward(Player killer){calls++;if(clearZone)zone=null;if(fail)throw new IllegalStateException("Original reward failure");}
        @Override public void initSkill(){}
        @Override protected void resetBase(){}
    }
    public static void main(String[] args)throws Exception {
        var path=Path.of("data/config/test_boss_drop.json");var store=new BossDropRules(path);
        check(!Files.exists(path)&&store.state().rules().isEmpty(),"Default read creates no config and drops nothing");
        store.save(rule(0,"FIDE_DAI_CA_1",1,50,true),0);
        check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->499999,(a,b)->b).get(0).quantity()==3,"50% includes last successful value and quantity max");
        check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->500000,(a,b)->a).isEmpty(),"50% excludes first failed value");
        check(store.roll(2,List.of("FIDE_DAI_CA_1"),()->0,(a,b)->a).isEmpty(),"Map restriction");
        check(store.roll(1,List.of("FIDE_DAI_CA_2"),()->0,(a,b)->a).isEmpty(),"Exact stage, not every stage of same boss");
        reject(ConcurrentModificationException.class,()->store.save(rule(1,"FIDE_DAI_CA_1",1,1,true),0));
        store.save(rule(1,"FIDE_DAI_CA_1",-1,0.0001,true),1);
        check(store.roll(2,List.of("FIDE_DAI_CA_1"),()->0,(a,b)->a).size()==1&&store.roll(2,List.of("FIDE_DAI_CA_1"),()->1,(a,b)->a).isEmpty(),"Minimum nonzero chance and all-map rule");
        store.save(rule(1,"FIDE_DAI_CA_1",-1,100,true),2);
        check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->{throw new AssertionError();},(a,b)->a).size()==1,"100% always drops without RNG");
        store.save(rule(1,"FIDE_DAI_CA_1",-1,0,true),3);
        check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->{throw new AssertionError();},(a,b)->a).isEmpty(),"Zero never rolls");
        store.save(rule(1,"FIDE_DAI_CA_1",-1,100,false),4);
        check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->0,(a,b)->a).isEmpty(),"Disabled rule excluded");
        check(new BossDropRules(path).state().equals(store.state()),"Exact persistence across restart");
        store.delete(1,5);check(store.save(rule(0,"FIDE_DAI_CA_1",-1,100,true),6).id()==2,"IDs not reused after delete");
        for(double chance:new double[]{-1,101,Double.NaN,Double.POSITIVE_INFINITY,0.00001})reject(IllegalArgumentException.class,()->rule(0,"FIDE_DAI_CA_1",1,chance,true));
        reject(IllegalArgumentException.class,()->rule(0,"../outside",1,1,true));
        reject(IllegalArgumentException.class,()->new BossDropRules.Rule(0,"Bad",true,-1,"FIDE_DAI_CA_1",568,1,4,3,List.of()));
        reject(IllegalArgumentException.class,()->new BossDropRules.Rule(0,"Bad",true,-1,"FIDE_DAI_CA_1",568,1,1,1,List.of(new MobDropRules.Option(50,1),new MobDropRules.Option(50,2))));
        Path bad=path.resolveSibling("broken.json");Files.writeString(bad,"bad JSON");var broken=new BossDropRules(bad);
        check(broken.loadError()!=null,"Malformed saved config reported");reject(java.io.IOException.class,()->broken.save(rule(0,"FIDE_DAI_CA_1",1,1,true),0));check(Files.readString(bad).equals("bad JSON"),"Malformed file not overwritten");
        Path blocked=path.resolveSibling("blocked");Files.writeString(blocked,"file");var failed=new BossDropRules(blocked.resolve("rules.json"));
        reject(java.io.IOException.class,()->failed.save(rule(0,"FIDE_DAI_CA_1",1,1,true),0));check(failed.state().revision()==0&&failed.state().rules().isEmpty(),"Failed persistence does not publish");

        for(int id=0;id<=674;id++){ItemTemplate t=new ItemTemplate();t.id=(short)id;t.type=27;t.name="Item "+id;Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"option #",0));
        var unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);var unsafe=(Unsafe)unsafeField.get(null);
        var map=(models.map.Map)unsafe.allocateInstance(models.map.Map.class);map.mapId=1;var zone=new Zone(map,0,20);
        Player owner=new Player();owner.id=123;owner.isPlayer=true;
        BossDropRules.get().save(rule(0,"FIDE_DAI_CA_1",1,100,true),0);
        var boss=boss(unsafe,zone);boss.rewardWithConfiguredDrops(owner);
        check(boss.calls==1&&zone.items.size()==1,"Real wrapper calls original subclass reward and custom drops");
        var item=zone.items.get(0);check(item.playerId==123&&item.x==100&&item.y==200&&item.quantity>=2&&item.quantity<=3,"Drop owner, quantity and death position");
        Item picked=ItemService.gI().createItemFromItemMap(item);check(picked.getOptionParam(50)==12&&picked.getOptionParam(205)==20,"Options survive to picked-up item");
        boss.rewardWithConfiguredDrops(owner);check(zone.items.size()==1&&boss.calls==1,"Duplicate death reward repeats neither original nor custom rewards");
        boss.initBase();boss.rewardWithConfiguredDrops(owner);check(zone.items.size()==2,"Respawn/initBase resets custom reward guard");
        var stage2=boss(unsafe,zone);stage2.currentLevel=1;stage2.rewardWithConfiguredDrops(owner);check(zone.items.size()==2,"Other stage does not inherit first-stage reward");
        var dying=boss(unsafe,zone);dying.clearZone=true;dying.fail=true;reject(IllegalStateException.class,()->dying.rewardWithConfiguredDrops(owner));
        check(zone.items.size()==3&&dying.calls==1,"Captured death map survives original reward exception/map exit");
        dying.rewardWithConfiguredDrops(owner);check(dying.calls==1&&zone.items.size()==3,"Retry after partial reward failure does not duplicate rewards");
        var pet=(Pet)unsafe.allocateInstance(Pet.class);pet.master=owner;boss(unsafe,zone).rewardWithConfiguredDrops(pet);check(zone.items.size()==4&&zone.items.get(0).playerId==123,"Pet kill belongs to master");
        Player bot=new Player();bot.isBot=true;boss(unsafe,zone).rewardWithConfiguredDrops(bot);check(zone.items.size()==4,"Bot does not get custom boss rewards");
        BossDropRules.get().save(rule(1,"FIDE_DAI_CA_1",-1,100,true),1);
        map.tileMap=new int[20][20];map.tileTop=new int[]{1};map.tileMap[12][4]=1;
        boss(unsafe,zone).rewardWithConfiguredDrops(owner);check(zone.items.get(0).y==288,"Flying boss drops reach the ground tile");
        map.mapId=131;var actualDeath=boss(unsafe,zone);actualDeath.die(owner);
        check(actualDeath.calls==1&&zone.items.size()==6&&actualDeath.bossStatus==consts.BossStatus.DIE,"Actual inherited Boss.die invokes both original reward and configured drop");
        var concurrent=boss(unsafe,zone);List<Thread> workers=new ArrayList<>();
        for(int i=0;i<8;i++){Thread thread=new Thread(()->concurrent.rewardWithConfiguredDrops(owner));workers.add(thread);thread.start();}
        for(Thread thread:workers)thread.join();
        check(concurrent.calls==1&&zone.items.size()==7,"Concurrent death callbacks award original/custom rewards once");
        System.out.println("PASS: "+checks+" boss drop probability, stage/map, persistence, rollback and real reward integration checks");
    }
    static FixtureBoss boss(Unsafe unsafe,Zone zone)throws Exception {
        var boss=(FixtureBoss)unsafe.allocateInstance(FixtureBoss.class);var base=new Player();
        for(var field:Player.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(boss,field.get(base));}
        var data=Boss.class.getDeclaredField("data");data.setAccessible(true);data.set(boss,new BossData[]{BossesData.FIDE_DAI_CA_1,BossesData.FIDE_DAI_CA_2});
        boss.isBoss=true;boss.zone=zone;boss.currentLevel=0;boss.location.x=100;boss.location.y=200;return boss;
    }
    interface Task{void run()throws Exception;}
    static void reject(Class<? extends Throwable> type,Task task)throws Exception{try{task.run();throw new AssertionError("Expected "+type);}catch(Exception e){if(!type.isInstance(e))throw e;checks++;}}
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
