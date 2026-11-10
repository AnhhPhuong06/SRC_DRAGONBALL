import models.ConsignItem;
import models.item.Item;
import models.Template;
import models.player.Inventory;
import models.player.Player;
import managers.ConsignShopManager;
import server.Manager;
import services.ConsignShopService;
import java.util.*;

/** Real listing/claim methods, fixture players and no network refresh or DB. */
public final class AuditConsignGameplayTest {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static final class Fixture extends ConsignShopService {
        @Override public void openShopKyGui(Player player) {}
    }
    static ConsignItem listing(int id,long seller,int gold,int ruby,boolean sold){
        return new ConsignItem(id,(short)533,(int)seller,(byte)3,gold,ruby,1,0,new ArrayList<>(),sold);
    }
    public static void main(String[] args){
        for(int id=0;id<1450;id++){var t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.gender=3;t.name="fixture";Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"fixture",0));
        var service=new Fixture();var list=ConsignShopManager.gI().listItem;
        var seller=ToriNguSacShopTest.player(0);seller.id=7001;seller.nPoint.power=20_000_000_000L;
        var sold=listing(1,seller.id,2_000_000_000,-1,true);list.add(sold);
        check(service.getItemBuy(seller,1)==sold&&service.getItemBuy(seller,sold)==sold,"Sold listing remains visible and claimable by owner");
        long before=seller.inventory.gold;service.claimOrDel(seller,(byte)2,1);
        check(seller.inventory.gold==before+1_900_000_000L,"Exact 5 percent fee uses wide arithmetic");
        check(!list.contains(sold),"Successful claim removes listing");
        service.claimOrDel(seller,(byte)2,1);
        check(seller.inventory.gold==before+1_900_000_000L,"Claim replay has no second payout");
        seller.inventory.gold=Inventory.LIMIT_GOLD;sold=listing(2,seller.id,1000,-1,true);list.add(sold);
        service.claimOrDel(seller,(byte)2,2);
        check(seller.inventory.gold==Inventory.LIMIT_GOLD&&list.contains(sold),"Balance cap preserves unclaimed listing");list.clear();
        seller.inventory.ruby=Integer.MAX_VALUE-10;sold=listing(3,seller.id,-1,Integer.MAX_VALUE,true);list.add(sold);
        service.claimOrDel(seller,(byte)2,3);
        check(seller.inventory.ruby==Integer.MAX_VALUE-10&&list.contains(sold),"Ruby payout never overflows");list.clear();
        seller.inventory.ruby=100;seller.inventory.itemsBag.set(0,ToriNguSacShopTest.item(533,10));
        for(int index:new int[]{-1,seller.inventory.itemsBag.size(),Integer.MAX_VALUE})service.KiGui(seller,index,100,(byte)0,1);
        for(int quantity:new int[]{-1,0,100,Integer.MAX_VALUE})service.KiGui(seller,0,100,(byte)0,quantity);
        service.KiGui(seller,0,100,(byte)2,1);
        check(list.isEmpty()&&seller.inventory.ruby==100&&ToriNguSacShopTest.count(seller,533)==10,"Invalid index, quantity and currency never charge listing fee");
        service.KiGui(seller,0,100,(byte)0,2);
        check(list.size()==1&&seller.inventory.ruby==95&&ToriNguSacShopTest.count(seller,533)==8,"Valid listing charges exactly one fee and offered quantity");
        list.clear();System.out.println("PASS: "+checks+" consign ownership, claim replay, tax/caps and listing validation regressions");
    }
}
