package models.shop;

 


import java.util.ArrayList;
import java.util.List;
import models.player.Player;

public class TabShopHocKyNang extends TabShop {

    public Shop shop;

    public int id;

    public String name;

    public List<ItemShop> itemShops;
    public int index;

    public TabShopHocKyNang() {
        this.itemShops = new ArrayList<>();
    }

    public TabShopHocKyNang(TabShop tabShop, Player player) {
        this.itemShops = new ArrayList<>();
        this.shop = tabShop.shop;
        this.id = tabShop.id;
        this.name = tabShop.name;

        for (ItemShop itemShop : tabShop.itemShops) {
            if ((itemShop.temp.gender == player.gender || itemShop.temp.gender > 2) && !player.BoughtSkills.contains(itemShop.temp.id)) {
                this.itemShops.add(new ItemShop(itemShop));
            }
        }
    }

    

    public void dispose() {
        this.shop = null;
        this.name = null;
        if (this.itemShops != null) {
            for (ItemShop is : this.itemShops) {
                is.dispose();
            }
            this.itemShops.clear();
        }
        this.itemShops = null;
    }

}
