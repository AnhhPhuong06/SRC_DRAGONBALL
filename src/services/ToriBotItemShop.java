package services;

import consts.ConstNpc;
import models.item.Item;
import models.player.Player;
import models.shop.ItemShop;
import models.shop.Shop;
import services.player.InventoryService;

/** Fixed currency for the Kame Tori-bot item shop; independent of an item's display icon. */
public final class ToriBotItemShop {
    public static final String TAG = "TORI_BOT_ITEMS";
    public static final int CURRENCY_ID = 674;
    private ToriBotItemShop() {}
    public static boolean matches(int npc,String tag){return npc==ConstNpc.TORI_BOT&&TAG.equals(tag);}
    public static boolean matches(Shop shop){return shop!=null&&matches(shop.npcId,shop.tagName);}
    public static void prepare(Shop shop){
        if(!matches(shop))return;
        var currency=ItemService.gI().getTemplate(CURRENCY_ID);
        if(currency==null)throw new IllegalStateException("Chưa nạp dữ liệu Đá ngũ sắc (674).");
        shop.typeShop=3;
        for(var tab:shop.tabShops)for(ItemShop item:tab.itemShops){item.typeSell=4;item.iconSpec=Short.toUnsignedInt(currency.iconID);}
    }
    public static void buy(Player player,int templateId){
        synchronized(player.inventory.itemsBag){
            Shop shop=player.idMark.getShopOpen();
            ItemShop offer=matches(shop)?shop.getItemShop(templateId):null;
            if(offer==null||offer.temp==null||offer.cost<0||offer.cost>Integer.MAX_VALUE){notice(player,"Vật phẩm hoặc giá bán không hợp lệ.");return;}
            InventoryService inventory=InventoryService.gI();
            if(inventory.getCountEmptyBag(player)<1){notice(player,"Hành trang đã đầy.");return;}
            long total=0;
            for(Item item:player.inventory.itemsBag)if(item!=null&&item.isNotNullItem()&&item.template.id==CURRENCY_ID&&item.quantity>0)total+=item.quantity;
            if(total<offer.cost){notice(player,"Cần "+offer.cost+" Đá ngũ sắc để mua vật phẩm này.");return;}
            Item reward=ItemService.gI().createItemFromItemShop(offer);
            if(!inventory.addItemBag(player,reward)){notice(player,"Không thể thêm vật phẩm vào hành trang.");return;}
            // The bag stays locked across granting and deducting; all currency stacks are counted.
            if(offer.cost>0)inventory.subQuantityItemsBag(player,CURRENCY_ID,(int)offer.cost);
            inventory.sendItemBags(player);notice(player,"Mua thành công "+offer.temp.name+" bằng "+offer.cost+" Đá ngũ sắc.");
        }
    }
    private static void notice(Player player,String message){Service.gI().sendThongBao(player,message);}
}
