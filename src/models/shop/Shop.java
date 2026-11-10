package models.shop;

 


import java.util.ArrayList;
import java.util.List;
import models.player.Player;

public class Shop {

    public int id;

    public byte npcId;

    public List<TabShop> tabShops;

    public String tagName;

    public byte typeShop;

    public Shop() {
        this.tabShops = new ArrayList<>();
    }

    public Shop(Shop shop, int gender) {
        this.tabShops = new ArrayList<>();
        this.id = shop.id;
        this.npcId = shop.npcId;
        this.tagName = shop.tagName;
        this.typeShop = shop.typeShop;
        for (TabShop tabShop : shop.tabShops) {
            this.tabShops.add(new TabShop(tabShop, gender));
        }
    }
public Shop(Shop shop, Player player) {
        this.tabShops = new ArrayList<>();
        this.id = shop.id;
        this.npcId = shop.npcId;
        this.tagName = shop.tagName;
        this.typeShop = shop.typeShop;
        for (TabShop tabShop : shop.tabShops) {
//            if (shop.typeShop == 1){
//                            this.tabShops.add(new TabShopHocKyNang(tabShop, player));
//
//            }else{
            this.tabShops.add(new TabShop(tabShop, player));
           // }
        }
    }
    public Shop(Shop shop) {
        this.tabShops = new ArrayList<>();
        this.id = shop.id;
        this.npcId = shop.npcId;
        this.tagName = shop.tagName;
        this.typeShop = shop.typeShop;
        for (TabShop tabShop : shop.tabShops) {
            this.tabShops.add(new TabShop(tabShop));
        }
    }

    public ItemShop getItemShop(int temp) {
        for (TabShop tab : this.tabShops) {
            for (ItemShop is : tab.itemShops) {
                if (is.temp.id == temp) {
                    return is;
                }
            }
        }
        return null;
    }

    public void dispose() {
        if (this.tabShops != null) {
            for (TabShop ts : this.tabShops) {
                ts.dispose();
            }
            this.tabShops.clear();
        }
        this.tabShops = null;
    }

}
