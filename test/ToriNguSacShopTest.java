import java.io.*;
import java.util.*;
import models.Template.*;
import models.item.Item;
import models.player.Player;
import models.shop.*;
import network.io.Message;
import server.Manager;
import services.*;

public class ToriNguSacShopTest {
    static int checks;
    public static void main(String[] args)throws Exception{
        for(int id=0;id<=1449;id++){ItemTemplate t=new ItemTemplate();t.id=(short)id;t.gender=3;t.type=(byte)(id==1449?21:27);t.iconID=(short)(id==674||id==10?6467:100);t.name="Item "+id;Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        Shop original=shop();Manager.SHOPS.add(original);
        TestPlayer p=player(0);ShopService.gI().opendShop(p,ToriBotItemShop.TAG,false);
        check(p.idMark.getShopOpen().typeShop==3,"Tori shop always opens as item-currency shop");
        check(original.typeShop==0&&original.tabShops.get(0).itemShops.get(0).typeSell==0,"Normalizing player view does not modify raw cached shop");
        try(DataInputStream r=new DataInputStream(new ByteArrayInputStream(p.packet))){check(r.readUnsignedByte()==3&&r.readUnsignedByte()==1,"Special shop wire type");r.readUTF();check(r.readUnsignedByte()==1&&r.readUnsignedShort()==1449,"Correct offer packet");check(r.readUnsignedShort()==6467&&r.readInt()==100,"Packet displays stone icon and configured stone count");}
        for(int funds:new int[]{0,99,100,101,250}){
            p=player(funds);ShopService.gI().opendShop(p,ToriBotItemShop.TAG,false);long gold=p.inventory.gold;int gems=p.inventory.gem,ruby=p.inventory.ruby,coupon=p.inventory.coupon;
            ShopService.gI().buyItem(p,1449);
            check(count(p,674)==funds-(funds>=100?100:0),"Exact stone debit for balance "+funds);
            check(count(p,1449)==(funds>=100?1:0),"No item granted for insufficient stones");
            check(p.inventory.gold==gold&&p.inventory.gem==gems&&p.inventory.ruby==ruby&&p.inventory.coupon==coupon,"No gold/gem/ruby/coupon debit");
            check(count(p,10)==999,"Duplicate display icon never makes unrelated item a payment currency");
        }
        p=player(0);p.inventory.itemsBag.set(0,item(674,40));p.inventory.itemsBag.set(1,item(674,60));
        ShopService.gI().opendShop(p,ToriBotItemShop.TAG,false);ShopService.gI().buyItem(p,1449);
        check(count(p,674)==0&&count(p,1449)==1,"Multiple stone stacks can jointly pay one offer");
        p=player(200);for(int i=0;i<p.inventory.itemsBag.size();i++)if(!p.inventory.itemsBag.get(i).isNotNullItem())p.inventory.itemsBag.set(i,item(1,1));
        ShopService.gI().opendShop(p,ToriBotItemShop.TAG,false);ShopService.gI().buyItem(p,1449);
        check(count(p,674)==200&&count(p,1449)==0,"Full bag consumes no stones");
        p=player(200);ShopService.gI().opendShop(p,ToriBotItemShop.TAG,false);ShopService.gI().buyItem(p,999);
        check(count(p,674)==200,"Invalid offer consumes nothing");
        p=player(200);p.idMark.setShopOpen(new Shop(original));
        ShopService.gI().buyItem(p,1449);check(count(p,674)==100&&count(p,1449)==1,"Stale normal-shop view still pays stones by shop identity");
        check(!ToriBotItemShop.matches(74,"SHOP_VND")&&!ToriBotItemShop.matches(21,ToriBotItemShop.TAG),"Other NPC shops and village Tori shops are outside policy");
        System.out.println("PASS: "+checks+" Tori stone pricing, real shop packet/purchase, multiple stacks, insufficient/full bag and alternate-currency guards");
    }
    static Shop shop(){Shop s=new Shop();s.id=49;s.npcId=74;s.typeShop=0;s.tagName=ToriBotItemShop.TAG;TabShop t=new TabShop();t.id=64;t.name="Vật phẩm";t.shop=s;s.tabShops.add(t);ItemShop offer=new ItemShop();offer.id=1;offer.tabShop=t;offer.temp=Manager.ITEM_TEMPLATES.get(1449);offer.cost=100;offer.typeSell=0;offer.iconSpec=100;offer.options.add(new Item.ItemOption(30,0));t.itemShops.add(offer);return s;}
    static TestPlayer player(int stones){TestPlayer p=new TestPlayer();p.inventory.gold=1000000;p.inventory.gem=500;p.inventory.ruby=600;p.inventory.coupon=700;p.isPet=true;for(int i=0;i<14;i++)p.inventory.itemsBody.add(new Item());for(int i=0;i<8;i++)p.inventory.itemsBag.add(new Item());if(stones>0)p.inventory.itemsBag.set(0,item(674,stones));p.inventory.itemsBag.set(2,item(10,999));return p;}
    static Item item(int id,int quantity){Item i=new Item();i.template=Manager.ITEM_TEMPLATES.get(id);i.quantity=quantity;return i;}
    static int count(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class TestPlayer extends Player{byte[] packet;@Override public void sendMessage(Message message){if(message.command==-44)packet=message.getData().clone();}}
}
