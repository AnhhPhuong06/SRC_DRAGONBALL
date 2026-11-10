import consts.BossStatus;
import models.boss.*;
import models.item.Item;
import models.player.Inventory;
import models.player.Player;
import models.shop.*;
import server.Manager;
import services.*;
import services.player.InventoryService;
import java.lang.reflect.Field;
import sun.misc.Unsafe;

/** In-memory regressions only: no server initialization, live accounts or database calls. */
public final class StabilityRegressionTest {
    static int checks;
    public static void main(String[] args)throws Exception{
        try {
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
            BossRespawnAuditTest.unsafe=(Unsafe)f.get(null);
            for(int id=0;id<=1449;id++){
                var t=new models.Template.ItemTemplate();t.id=(short)id;t.gender=3;t.type=(byte)(id==1449?21:27);
                t.name="Item "+id;t.iconID=100;Manager.ITEM_TEMPLATES.add(t);
            }
            for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new models.Template.ItemOptionTemplate(id,"Option #",0));
            String group=args.length==0?"all":args[0];
            if(!java.util.Set.of("all","cooldown","attacker","shop","gold").contains(group))throw new IllegalArgumentException("Unknown test group");
            if(group.equals("all")||group.equals("cooldown"))cooldowns();
            if(group.equals("all")||group.equals("attacker"))nullAttackers();
            if(group.equals("all")||group.equals("shop"))shops();
            if(group.equals("all")||group.equals("gold"))gold();
            System.out.println("PASS: "+checks+" stability checks: "+group);
            System.exit(0);
        }catch(Throwable e){e.printStackTrace();System.exit(1);}
    }
    static BossData data(int seconds){return new BossData("Fixture",(byte)0,new short[]{1,2,3,-1,-1,-1},1,new long[]{100},new int[]{100},new int[0][],new String[0],new String[0],new String[0],seconds);}
    static void cooldowns()throws Exception{
        Boss base=BossRespawnAuditTest.fixture(BossRespawnAuditTest.Fixture.class,data(0));
        base.currentLevel=0;base.secondsRest=300;base.lastTimeRest=System.currentTimeMillis()-1;base.rest();
        check(base.bossStatus==BossStatus.RESPAWN,"Panel zero cooldown must not fall back to old runtime delay");
        for(int seconds:new int[]{0,1,2,3,4,300,3_000_000}){
            var m=BossRespawnAuditTest.fixture(models.boss.boss_list.MajinBuu12H.Mabu.class,data(seconds));
            m.currentLevel=0;m.secondsRest=seconds;m.lastTimeRest=System.currentTimeMillis()-1;m.rest();
            check(m.bossStatus==(seconds==0?BossStatus.RESPAWN:BossStatus.REST),"Mabu respects current configured delay without division by zero or int overflow: "+seconds);
            Field pct=m.getClass().getDeclaredField("percent");pct.setAccessible(true);
            check(pct.getInt(m)>=0&&pct.getInt(m)<=100,"Mabu progress remains in 0..100");
            m.lastTimeRest=System.currentTimeMillis()-(seconds+1L)*1000L;m.rest();
            check(m.bossStatus==BossStatus.RESPAWN,"Mabu eventually respawns: "+seconds);
        }
        for(int current:new int[]{-1,0,1}){
            var d=BossRespawnAuditTest.fixture(models.boss.boss_list.MajinBuu12H.Drabura2.class,data(300));
            d.currentLevel=current;d.bossAppearTogether=new Boss[1][];d.lastTimeRest=0;d.rest();
            check(d.bossStatus==BossStatus.RESPAWN,"Drabura missing/expired companion slot cannot break rest");
        }
        var d=BossRespawnAuditTest.fixture(models.boss.boss_list.MajinBuu12H.Drabura2.class,data(300));
        Boss child=BossRespawnAuditTest.fixture(BossRespawnAuditTest.Fixture.class,data(300));
        d.currentLevel=0;d.bossAppearTogether=new Boss[][]{{null,child}};d.lastTimeRest=System.currentTimeMillis()-6000;d.rest();
        check(child.bossStatus==BossStatus.RESPAWN&&d.bossStatus==BossStatus.REST,"Drabura still calls valid children after five seconds");
        child.bossStatus=BossStatus.REST;d.rest();check(child.bossStatus==BossStatus.REST,"Companions called once per cycle");
    }
    static void nullAttackers()throws Exception{
        for(String name:new String[]{"BuiBui","BuiBui2","Cadic","Drabura","Drabura2","Goku","Mabu","Yacon"}){
            var type=Class.forName("models.boss.boss_list.MajinBuu12H."+name).asSubclass(Boss.class);
            Boss boss=BossRespawnAuditTest.fixture(type,data(300));boss.currentLevel=0;boss.nPoint.hp=boss.nPoint.hpMax=1000;
            for(int i=0;i<10;i++)boss.injured(null,1,true,true);
            check(boss.nPoint.hp>=990&&boss.nPoint.hp<=1000,"Damage without a player attacker must not throw: "+name);
        }
    }
    static ToriNguSacShopTest.TestPlayer player(){return ToriNguSacShopTest.player(0);}
    static Shop shop(String tag,long cost,int typeSell){Shop s=ToriNguSacShopTest.shop();s.id=99;s.typeShop=0;s.tagName=tag;var offer=s.tabShops.get(0).itemShops.get(0);offer.cost=cost;offer.typeSell=(byte)typeSell;return s;}
    static void shops(){
        var p=player();ShopService.gI().buyItem(p,1449);check(ToriNguSacShopTest.count(p,1449)==0,"No open shop does not throw/grant");
        for(int type:new int[]{0,1,3,4})for(long cost:new long[]{-1,2147483648L,Long.MAX_VALUE}){
            p=player();Shop s=shop("FIXTURE",cost,type);p.idMark.setShopOpen(s);ShopService.gI().buyItem(p,1449);
            check(p.inventory.gold==1000000&&p.inventory.gem==500&&p.inventory.ruby==600&&p.inventory.coupon==700
                    &&ToriNguSacShopTest.count(p,1449)==0,"Invalid legacy shop cost cannot mint money or grant an item");
        }
        p=player();p.idMark.setShopOpen(shop("FIXTURE",100,2));ShopService.gI().buyItem(p,1449);
        check(ToriNguSacShopTest.count(p,1449)==0&&p.inventory.gold==1000000,"Unsupported normal-shop currency cannot buy free items");
        p=player();Shop unsupported=shop("FIXTURE",100,0);unsupported.typeShop=2;p.idMark.setShopOpen(unsupported);ShopService.gI().buyItem(p,1449);
        check(ToriNguSacShopTest.count(p,1449)==0&&p.inventory.gold==1000000,"Unsupported shop type cannot grant unpaid items");
        p=player();p.idMark.setShopOpen(shop("SANTA_PGG",100,0));ShopService.gI().buyItem(p,1449);
        check(p.inventory.gold==1000000&&ToriNguSacShopTest.count(p,1449)==0,"Missing Santa voucher blocks before any payment or grant");
        p=player();p.inventory.itemsBag.set(1,ToriNguSacShopTest.item(459,2));p.idMark.setShopOpen(shop("SANTA_PGG",100,0));ShopService.gI().buyItem(p,1449);
        check(p.inventory.gold==999900&&ToriNguSacShopTest.count(p,459)==1&&ToriNguSacShopTest.count(p,1449)==1,"Valid Santa purchase charges one voucher and normal price");
        p=player();p.inventory.gold=0;p.inventory.itemsBag.set(1,ToriNguSacShopTest.item(459,2));p.idMark.setShopOpen(shop("SANTA_PGG",100,0));ShopService.gI().buyItem(p,1449);
        check(ToriNguSacShopTest.count(p,459)==2&&ToriNguSacShopTest.count(p,1449)==0,"Insufficient funds preserves voucher");
        p=player();Shop skill=shop("FIXTURE",-1,0);skill.typeShop=1;p.idMark.setShopOpen(skill);ShopService.gI().buyItem(p,1449);
        check(p.hocKyNang.PotentialLearn==0,"Negative skill price is rejected before teaching/book parsing");
        p=player();Shop special=shop("FIXTURE",-1,4);special.typeShop=3;p.idMark.setShopOpen(special);ShopService.gI().buyItem(p,1449);
        check(ToriNguSacShopTest.count(p,1449)==0&&p.inventory.gold==1000000,"Negative special-item price rejected");
        for(int id:new int[]{517,518}){
            p=player();Shop expansion=shop("FIXTURE",100,0);expansion.tabShops.get(0).itemShops.get(0).temp=Manager.ITEM_TEMPLATES.get(id);
            var slots=id==517?p.inventory.itemsBag:p.inventory.itemsBox;
            while(slots.size()<100)slots.add(new Item());
            p.idMark.setShopOpen(expansion);ShopService.gI().buyItem(p,id);
            check(p.inventory.gold==1000000&&slots.size()==100,"Maxed expansion cannot consume price without adding a slot");
        }
    }
    static void gold(){
        for(boolean chibi:new boolean[]{false,true})for(int space:new int[]{0,10,20}){
            var p=player();p.effectSkill.isChibi=chibi;p.typeChibi=0;p.inventory.gold=Inventory.LIMIT_GOLD-space;
            Item item=ToriNguSacShopTest.item(190,10);item.template.type=9;long before=p.inventory.gold;long gain=chibi?20:10;
            boolean result=InventoryService.gI().addItemBag(p,item);
            check(result==(space>=gain)&&p.inventory.gold==before+(space>=gain?gain:0),"Gold limit checks full Chibi bonus before granting");
        }
        for(int quantity:new int[]{0,-1,Integer.MIN_VALUE}){
            var p=player();p.inventory.gold=1000;Item item=ToriNguSacShopTest.item(190,quantity);item.template.type=9;
            check(!InventoryService.gI().addItemBag(p,item)&&p.inventory.gold==1000,"Invalid gold stack cannot subtract money");
        }
        var p=player();p.inventory.gold=Long.MAX_VALUE;Item item=ToriNguSacShopTest.item(190,1);item.template.type=9;
        check(!InventoryService.gI().addItemBag(p,item)&&p.inventory.gold==Long.MAX_VALUE,"Gold addition cannot overflow long");
    }
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
}
