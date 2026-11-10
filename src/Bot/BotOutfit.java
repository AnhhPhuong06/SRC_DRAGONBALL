package Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import models.Template.ItemTemplate;
import models.item.Item;
import models.player.Player;
import server.Manager;
import services.ItemService;

public final class BotOutfit {

    private static final int SO_O_BODY = 13;
    private static final int O_CAI_TRANG = 5;
    private static final int O_BONG_TAI = 6;
    private static final int O_VAN_BAY = 9;
    private static final int O_DEO_LUNG = 11;
    private static final int OPT_CAP_DO = 72;

    private BotOutfit() {
    }

    public static void macDo(Player bot, int capDo) {
        macDo(bot, capDo, -1);
    }

    public static void macDo(Player bot, int capDo, int idCaiTrang) {
        if (bot == null || bot.inventory == null) {
            return;
        }
        Random rd = new Random();
        List<Item> body = bot.inventory.itemsBody;
        body.clear();
        for (int i = 0; i < SO_O_BODY; i++) {
            body.add(ItemService.gI().createItemNull());
        }

        for (int type = 0; type <= 4; type++) {
            ItemTemplate t = bocTemplate(type, bot.gender, rd);
            if (t == null) {
                continue;
            }
            Item it = taoMon(t, capDo);
            if (it != null) {
                body.set(type, it);
            }
        }

        ItemTemplate caiTrang = idCaiTrang >= 0
                ? ItemService.gI().getTemplate(idCaiTrang) : bocTemplate(5, bot.gender, rd);
        datPhuKien(body, O_CAI_TRANG, caiTrang);
        datPhuKien(body, O_BONG_TAI, bocTemplate(32, bot.gender, rd));
        // Ván bay có 2 loại (23/24), bốc ngẫu nhiên 1
        datPhuKien(body, O_VAN_BAY, bocTemplate(rd.nextBoolean() ? 23 : 24, bot.gender, rd));
        datPhuKien(body, O_DEO_LUNG, bocTemplate(11, bot.gender, rd));
    }

    /** Bảo đảm túi đồ mặc đủ ô, dùng cho bot tân thủ (ban đầu chưa có gì). */
    public static void moODo(Player bot) {
        if (bot == null || bot.inventory == null) {
            return;
        }
        while (bot.inventory.itemsBody.size() < SO_O_BODY) {
            bot.inventory.itemsBody.add(ItemService.gI().createItemNull());
        }
    }
    public static boolean muaMon(Player bot, int type, int capDo) {
        if (bot == null || bot.inventory == null) {
            return false;
        }
        moODo(bot);
        int o = (type == 32) ? O_BONG_TAI : type;
        if (o < 0 || o >= bot.inventory.itemsBody.size()) {
            return false;
        }
        ItemTemplate t = bocTemplate(type, bot.gender, new Random());
        if (t == null) {
            return false;
        }
        Item it = taoMon(t, capDo);
        if (it == null) {
            return false;
        }
        bot.inventory.itemsBody.set(o, it);
        return true;
    }

    private static void datPhuKien(List<Item> body, int o, ItemTemplate t) {
        if (t == null || o >= body.size()) {
            return;
        }
        Item it = taoMon(t, 0);
        if (it != null) {
            body.set(o, it);
        }
    }

    private static Item taoMon(ItemTemplate t, int capDo) {
        try {
            Item it = ItemService.gI().createNewItem(t.id);
            if (it == null || it.template == null) {
                return null;
            }
            for (Item.ItemOption io : ItemService.gI().getListOptionItemShop(t.id)) {
                if (io != null && io.optionTemplate != null) {
                    it.itemOptions.add(new Item.ItemOption(io.optionTemplate.id, io.param));
                }
            }
            if (capDo > 0 && t.type >= 0 && t.type <= 4) {
                it.itemOptions.add(new Item.ItemOption(OPT_CAP_DO, capDo));
            }
            return it;
        } catch (Exception e) {
            return null;
        }
    }

    private static ItemTemplate bocTemplate(int type, int gender, Random rd) {
        try {
            List<ItemTemplate> hop = new ArrayList<>();
            for (ItemTemplate t : Manager.ITEM_TEMPLATES) {
                if (t != null && t.type == type && (t.gender == gender || t.gender == 3)) {
                    hop.add(t);
                }
            }
            if (hop.isEmpty()) {
                return null;
            }
            return hop.get(rd.nextInt(hop.size()));
        } catch (Exception e) {
            return null;
        }
    }
}
