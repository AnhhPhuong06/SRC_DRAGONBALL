package services;

 
import managers.GiftCodeManager;
import models.GiftCode;
import models.item.Item;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import models.player.Player;
import services.player.InventoryService;
import services.map.NpcService;


public class GiftCodeService {

    private static GiftCodeService instance;

    public static GiftCodeService gI() {
        if (instance == null) {
            instance = new GiftCodeService();
        }
        return instance;
    }

    public void giftCode(Player player, String code) {
        GiftCode giftcode = GiftCodeManager.gI().checkUseGiftCode(player, code);
        // Hạn dùng đã được kiểm tra trong checkUseGiftCode (trước khi trừ lượt).
        if (giftcode != null) {
            Set<Integer> keySet = giftcode.detail.keySet();
            String textGift = "|0|Bạn vừa nhận được:\b";
            for (Integer key : keySet) {
                int idItem = key;
                int quantity = giftcode.detail.get(key);

                switch (idItem) {
                    case -1 -> {
                        player.inventory.gold = Math.min(player.inventory.gold + (long) quantity, 2000000000L);
                        textGift += "|2|" + quantity + " vàng\b";
                    }
                    case -2 -> {
                        player.inventory.gem = (int) Math.min((long) player.inventory.gem + quantity, 200000000L);
                        textGift += "|3|" + quantity + " ngọc\b";
                    }
                    case -3 -> {
                        player.inventory.ruby = (int) Math.min((long) player.inventory.ruby + quantity, 200000000L);
                        textGift += "|4|" + quantity + " ngọc khóa\b";
                    }
                    default -> {
                        Item itemGift = ItemService.gI().createNewItem((short) idItem);
                        // createNewItem/new Item luôn trả về object khác null kể cả khi id sai,
                        // template mới là thứ cần kiểm tra để không NPE.
                        if (itemGift.template != null) {
                            if (!giftcode.allGender && itemGift.template.gender <= 2 && player.gender != itemGift.template.gender) {
                                textGift += "|1|(bỏ qua " + itemGift.template.name + " - khác hành tinh)\b";
                                continue;
                            }
                            List<Item.ItemOption> customOpts = giftcode.option.get(key);
                            if (customOpts != null && !customOpts.isEmpty()) {
                                itemGift.itemOptions = cloneOptions(customOpts);
                            } else if (idItem == 457) {
                                itemGift.itemOptions.add(new Item.ItemOption(30, 0));
                            }
                            itemGift.quantity = quantity;
                            InventoryService.gI().addItemBag(player, itemGift);
                            textGift += "|1|x" + quantity + " " + itemGift.template.name + "\b";
                        }
                    }
                }
            }
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendMoney(player);
            NpcService.gI().createTutorial(player, 15272, textGift);
        }
    }

    private List<Item.ItemOption> cloneOptions(List<Item.ItemOption> src) {
        List<Item.ItemOption> copy = new ArrayList<>();
        if (src != null) {
            for (Item.ItemOption o : src) {
                if (o != null) {
                    copy.add(new Item.ItemOption(o));
                }
            }
        }
        return copy;
    }
}
