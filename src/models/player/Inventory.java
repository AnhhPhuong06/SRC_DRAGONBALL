package models.player;

 
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.item.Item.ItemOption;

public class Inventory {

    public static final long LIMIT_GOLD = 2_000_000_000_000L;
    public static final int MAX_ITEMS_BAG = 100;
    public static final int MAX_ITEMS_BOX = 100;
    /** Rương phụ vòng quay may mắn: tối đa 1.000 ô. */
    public static final int MAX_ITEMS_BOX_LUCKY_ROUND = 1000;
    public Item trainArmor;
    public List<String> giftCode;
    public List<Item> itemsBody;
    public List<Item> itemsBag;
    public List<Item> itemsBox;

    public List<Item> itemsBoxCrackBall;
    public List<Item> itemsDaBan;

    private Player player;

    public long gold, goldLimit;
    public int gem;
    public int ruby;
    public int coupon;
    public int event;
    /** Điểm sự kiện Đường Tăng — persist qua PlayerDAO (index 5 trong data_inventory JSON). */
    public int diemDT;
    /** Điểm sự kiện Xên Bọ Hùng — persist qua PlayerDAO (index 6). Đổi quà tại NPC đảo Kame (map 5, tempId 77). */
    public int diemXen;

    public Inventory() {
        itemsBody = new ArrayList<>();
        itemsBag = new ArrayList<>();
        itemsBox = new ArrayList<>();
        itemsBoxCrackBall = new ArrayList<>();
        itemsDaBan = new ArrayList<>();
        giftCode = new ArrayList<>();
    }

    public int getGemAndRuby() {
        return (int)Math.min(Integer.MAX_VALUE, (long)this.gem + this.ruby);
    }
    public int getGem() {
        return this.gem;
    }

    public int getParam(Item it, int id) {
        for (ItemOption op : it.itemOptions) {
            if (op != null && op.optionTemplate.id == id) {
                return op.param;
            }
        }
        return 0;
    }

    public long getGoldDisplay() {
        long amount = gold;
        if (player != null && amount > Integer.MAX_VALUE && !player.isVersionAbove(214)) {
            return Integer.MAX_VALUE;
        }
        return amount;
    }

    public void subRuby(int num) {
        this.ruby -= num;
    }

    public void subGem(int num) {
        this.gem -= num;
    }

    public void subGold(int num) {
        this.gold -= num;
    }

    public boolean haveOption(List<Item> l, int index, int id) {
        Item it = l.get(index);
        if (it != null && it.isNotNullItem()) {
            return it.itemOptions.stream().anyMatch(op -> op != null && op.optionTemplate.id == id);
        }
        return false;
    }

    public void subGemAndRuby(int num) {
        this.ruby -= num;
        if (this.ruby < 0) {
            this.gem += this.ruby;
            this.ruby = 0;
        }
    }

    public long getGoldLimit() {
        return goldLimit + LIMIT_GOLD;
    }

    public void addGold(int gold) {
        this.gold += gold;
        long limitGold = getGoldLimit();
        if (this.gold > limitGold) {
            this.gold = limitGold;
        }
    }

    public void dispose() {
        this.trainArmor = null;
        if (this.itemsBody != null) {
            for (Item it : this.itemsBody) {
                it.dispose();
            }
            this.itemsBody.clear();
        }
        if (this.itemsBag != null) {
            for (Item it : this.itemsBag) {
                it.dispose();
            }
            this.itemsBag.clear();
        }
        if (this.itemsBox != null) {
            for (Item it : this.itemsBox) {
                it.dispose();
            }
            this.itemsBox.clear();
        }
        if (this.itemsBoxCrackBall != null) {
            for (Item it : this.itemsBoxCrackBall) {
                it.dispose();
            }
            this.itemsBoxCrackBall.clear();
        }
        if (this.itemsDaBan != null) {
            for (Item it : this.itemsDaBan) {
                it.dispose();
            }
            this.itemsDaBan.clear();
        }
        this.itemsBody = null;
        this.itemsBag = null;
        this.itemsBox = null;
        this.itemsBoxCrackBall = null;
        this.itemsDaBan = null;
    }

}
