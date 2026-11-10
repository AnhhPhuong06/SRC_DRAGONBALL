package services;

 
import consts.ConstAchievement;
import consts.ConstHocKyNang;
import consts.ConstNpc;
import java.text.MessageFormat;
import models.item.Item;
import models.player.Inventory;
import models.player.Player;
import models.shop.ItemShop;
import models.shop.Shop;
import models.shop.TabShop;
import network.io.Message;
import models.item.Item.ItemOption;
import java.util.ArrayList;
import server.Manager;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

import java.util.List;
import java.util.stream.IntStream;

import services.func.Input;
import services.func.BuyBackService;
import services.map.NpcService;
import utils.TimeUtil;

public class ShopService {

    private static final byte COST_GOLD = 0;
    private static final byte COST_GEM = 1;
    private static final byte COST_RUBY = 3;
    private static final byte COST_COUPON = 4;

    private static final byte NORMAL_SHOP = 0;
    private static final byte LEARN_SKILL_SHOP = 1;
    private static final byte SPEC_SHOP = 3;

    private static ShopService I;

    public static ShopService gI() {
        if (ShopService.I == null) {
            ShopService.I = new ShopService();
        }
        return ShopService.I;
    }

    public void opendShop(Player player, String tagName, boolean allGender) {
        switch (tagName) {
            case "ITEMS_LUCKY_ROUND":
                openShopType4(player, tagName, player.inventory.itemsBoxCrackBall);
                return;
            case "Gift_Box":
                player.idMark.setTagNameShop(tagName);
                openMailBox(player);
                return;
            case "ITEMS_DABAN":
                openShopType8(player, tagName, player.inventory.itemsDaBan);
                return;
        }
        try {
            Shop shop = this.getShop(tagName);
            shop = this.resolveShop(player, shop, allGender);
            ToriBotItemShop.prepare(shop);
            switch (shop.typeShop) {
                case LEARN_SKILL_SHOP:
                    openShopType1(player, shop);
                    break;
                case NORMAL_SHOP:
                    openShopType0(player, shop);
                    break;
                case SPEC_SHOP:
                    openShopType3(player, shop);
                    break;
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            Service.gI().sendThongBao(player, ex.getMessage());
        }
    }

    private Shop getShop(String tagName) throws Exception {
        for (Shop s : Manager.SHOPS) {
            if (s.tagName != null && s.tagName.equals(tagName)) {
                return s;
            }
        }
        throw new Exception("Shop " + tagName + " không tồn tại!");
    }

    private Shop resolveShop(Player player, Shop shop, boolean allGender) {
        if (shop.tagName != null && (shop.tagName.equals("BUA_1H")
                || shop.tagName.equals("BUA_8H") || shop.tagName.equals("BUA_1M"))) {
            return this.resolveShopBua(player, new Shop(shop));
        }
        return allGender ? new Shop(shop) : new Shop(shop, player);
    }

    private Shop resolveShopBua(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            for (ItemShop item : tabShop.itemShops) {
                long min = 0;
                switch (item.temp.id) {
                    case 213:
                        long timeTriTue = player.charms.tdTriTue;
                        long current = System.currentTimeMillis();
                        min = (timeTriTue - current) / 60000;

                        break;
                    case 214:
                        min = (player.charms.tdManhMe - System.currentTimeMillis()) / 60000;
                        break;
                    case 215:
                        min = (player.charms.tdDaTrau - System.currentTimeMillis()) / 60000;
                        break;
                    case 216:
                        min = (player.charms.tdOaiHung - System.currentTimeMillis()) / 60000;
                        break;
                    case 217:
                        min = (player.charms.tdBatTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 218:
                        min = (player.charms.tdDeoDai - System.currentTimeMillis()) / 60000;
                        break;
                    case 219:
                        min = (player.charms.tdThuHut - System.currentTimeMillis()) / 60000;
                        break;
                    case 522:
                        min = (player.charms.tdDeTu - System.currentTimeMillis()) / 60000;
                        break;
                    case 671:
                        min = (player.charms.tdTriTue3 - System.currentTimeMillis()) / 60000;
                        item.typeSell = COST_RUBY;
                        break;
                    case 672:
                        min = (player.charms.tdTriTue4 - System.currentTimeMillis()) / 60000;
                        item.typeSell = COST_RUBY;
                        break;
                }
                item.iconSpec = (item.typeSell == COST_RUBY) ? 7743 : (item.typeSell == COST_GOLD) ? 927 : 932;
                if (min > 0) {
                    item.options.clear();
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

    private void _________________Gửi_cửa_hàng_Danh_Hiệu________________() {
        //**********************************************************************
    }

    private Shop DanhHieu(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            if (tabShop.id != 28) {
                continue;
            }
            for (ItemShop item : tabShop.itemShops) {
                item.options.removeIf(option -> option.optionTemplate.id == 228);
            }
        }
        return s;
    }

    private Shop SoHuu(Player player, Shop s) {
        for (TabShop tabShop : s.tabShops) {
            if (tabShop.id != 29) {
                continue;
            }
            for (ItemShop item : tabShop.itemShops) {
                long min = 3 * 24 * 60;
                if (!CheckDanhHieu(player, item)) {
                    continue;
                }
                boolean hasDayOption = false;
                boolean hasHourOption = false;
                boolean hasMinuteOption = false;
                for (Item.ItemOption option : item.options) {
                    if (option.optionTemplate.id == 63) {
                        hasDayOption = true;
                    }
                    if (option.optionTemplate.id == 64) {
                        hasHourOption = true;
                    }
                    if (option.optionTemplate.id == 65) {
                        hasMinuteOption = true;
                    }
                }
                if (min > 0 && !hasDayOption) {
                    if (min >= 1440) {
                        item.options.add(new Item.ItemOption(63, (int) min / 1440));
                    } else if (min >= 60) {
                        item.options.add(new Item.ItemOption(64, (int) min / 60));
                    } else {
                        item.options.add(new Item.ItemOption(65, (int) min));
                    }
                }
            }
        }
        return s;
    }

    private int ItemDanhHieu(Player player, TabShop tab) {
        int count = 0;
        for (ItemShop itemShop : tab.itemShops) {
            if (tab.id == 29 && CheckDanhHieu(player, itemShop)) {
                count++;
            }
        }
        return count;
    }

    private boolean CheckDanhHieu(Player player, ItemShop itemShop) {
        if (itemShop.temp.id == 1286) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1286);
            return napItem != null;
        }
        if (itemShop.temp.id == 1287) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1287);
            return napItem != null;
        }
        if (itemShop.temp.id == 1288) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1288);
            return napItem != null;
        }
        if (itemShop.temp.id == 1289) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1289);
            return napItem != null;
        }
        if (itemShop.temp.id == 1290) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1290);
            return napItem != null;
        }
        if (itemShop.temp.id == 1291) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1291);
            return napItem != null;
        }
        if (itemShop.temp.id == 1292) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1292);
            return napItem != null;
        }
        if (itemShop.temp.id == 1293) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1293);
            return napItem != null;
        }
        if (itemShop.temp.id == 1294) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1294);
            return napItem != null;
        }
        if (itemShop.temp.id == 1295) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1295);
            return napItem != null;
        }
        if (itemShop.temp.id == 1296) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1296);
            return napItem != null;
        }
        if (itemShop.temp.id == 1297) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1297);
            return napItem != null;
        }
        if (itemShop.temp.id == 1298) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1298);
            return napItem != null;
        }
        if (itemShop.temp.id == 1299) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1299);
            return napItem != null;
        }
        if (itemShop.temp.id == 1300) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1300);
            return napItem != null;
        }
        if (itemShop.temp.id == 1175) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1175);
            return napItem != null;
        }
        if (itemShop.temp.id == 1176) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1176);
            return napItem != null;
        }
        if (itemShop.temp.id == 1177) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1177);
            return napItem != null;
        }
        if (itemShop.temp.id == 1178) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1178);
            return napItem != null;
        }
        if (itemShop.temp.id == 1179) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1179);
            return napItem != null;
        }
        if (itemShop.temp.id == 1470) {
            Item napItem = InventoryService.gI().findItemInAllInventories(player, 1470);
            return napItem != null;
        }
        return true;
    }

    public void openShopType0(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(NORMAL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        if (itemShop.typeSell == COST_GOLD) {
                            msg.writer().writeInt((int) itemShop.cost);
                            msg.writer().writeInt(0);
                        } else if (itemShop.typeSell == COST_GEM) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt((int) itemShop.cost);
                        } else if (itemShop.typeSell == COST_RUBY) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt((int) itemShop.cost);
                        } else if (itemShop.typeSell == COST_COUPON) {
                            msg.writer().writeInt(0);
                            msg.writer().writeInt((int) itemShop.cost);
                        }
                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                        if (itemShop.temp.type == 5) {
                            msg.writer().writeByte(1);
                            msg.writer().writeShort(itemShop.temp.head);
                            msg.writer().writeShort(itemShop.temp.body);
                            msg.writer().writeShort(itemShop.temp.leg);
                            msg.writer().writeShort(-1);
                        } else {
                            msg.writer().writeByte(0);
                        }
                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType1(Player player, Shop shop) {
        if (shop != null) {
            player.idMark.setShopOpen(shop);
            player.idMark.setTagNameShop(shop.tagName);
            Message msg = null;
            try {
                msg = new Message(-44);
                msg.writer().writeByte(LEARN_SKILL_SHOP);
                msg.writer().writeByte(shop.tabShops.size());
                for (TabShop tab : shop.tabShops) {
                    msg.writer().writeUTF(tab.name);
                    msg.writer().writeByte(tab.itemShops.size());
                    for (ItemShop itemShop : tab.itemShops) {
                        msg.writer().writeShort(itemShop.temp.id);
                        msg.writer().writeLong(itemShop.cost);

                        msg.writer().writeByte(itemShop.options.size());
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                        msg.writer().writeByte(itemShop.isNew ? 1 : 0);

                        msg.writer().writeByte(0);

                    }
                }
                player.sendMessage(msg);
            } catch (Exception e) {
                Logger.logException(ShopService.class, e);
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    private void openShopType3(Player player, Shop shop) {
        player.idMark.setShopOpen(shop);
        player.idMark.setTagNameShop(shop.tagName);
        if (shop == null) {
            return;
        }
        try {
            Message msg = new Message(-44);
            msg.writer().writeByte(SPEC_SHOP);
            msg.writer().writeByte(shop.tabShops.size());
            for (TabShop tab : shop.tabShops) {
                if (tab.id == 29) {
                    tab.name = tab.name + "\n" + "[" + ItemDanhHieu(player, tab) + "]";
                }
                msg.writer().writeUTF(tab.name);

                List<ItemShop> validItems = new ArrayList<>();
                for (ItemShop itemShop : tab.itemShops) {
                    if (tab.id == 29 && !CheckDanhHieu(player, itemShop)) {
                        continue;
                    }
                    validItems.add(itemShop);
                }
                msg.writer().writeByte(validItems.size());

                boolean thisTab = true && (shop.id == 43 || shop.id == 44);
                for (ItemShop itemShop : validItems) {
                    msg.writer().writeShort(itemShop.temp.id);
                    shop = DanhHieu(player, shop);
                    shop = SoHuu(player, shop);
                    msg.writer().writeShort(itemShop.iconSpec);
                    msg.writer().writeInt((int) itemShop.cost);
                    int optCount = thisTab ? 0 : itemShop.options.size();
                    msg.writer().writeByte(optCount);
                    if (!thisTab) {
                        for (Item.ItemOption option : itemShop.options) {
                            msg.writer().writeByte(option.optionTemplate.id);
                            msg.writer().writeShort(option.param);
                        }
                    }

                    msg.writer().writeByte(itemShop.isNew ? 1 : 0);
                    if (itemShop.temp.type == 5) {
                        msg.writer().writeByte(1);
                        msg.writer().writeShort(itemShop.temp.head);
                        msg.writer().writeShort(itemShop.temp.body);
                        msg.writer().writeShort(itemShop.temp.leg);
                        msg.writer().writeShort(-1);
                    } else {
                        msg.writer().writeByte(0);
                    }
                }
            }
            player.sendMessage(msg);
            msg.cleanup();
        } catch (Exception e) {
            Logger.logException(ShopService.class, e);
        }
    }

    private void openShopType4(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(4);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Rương phụ\n(" + items.size() + "/" + Inventory.MAX_ITEMS_BOX_LUCKY_ROUND + ")");
            int soMon = Math.min(items.size(), 100);
            msg.writer().writeByte(soMon);
            for (int i = 0; i < soMon; i++) {
                Item item = items.get(i);
                msg.writer().writeShort(item.template.id);
                msg.writer().writeUTF(item.template.id == 189
                        ? "Vàng: " + Util.numberToMoney(item.quantity)
                        : "Ngọc Rồng Online");
                msg.writer().writeByte(item.itemOptions.size() + 1);
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                //số lượng
                msg.writer().writeByte(31);
                msg.writer().writeShort(item.quantity);
                //
                msg.writer().writeByte(1);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void openMailBox(Player player) {
        Message msg = null;
        try {
            if (player.inventory.itemsBoxCrackBall != null) {
                msg = new Message(-44);
                msg.writer().writeByte(4);
                msg.writer().writeByte(1);
                msg.writer().writeUTF(" Hòm\n Thư");
                // Số món khai báo và số món gửi kèm phải bằng nhau: trước đây khai báo tối
                // đa 200 nhưng vòng lặp vẫn gửi hết danh sách nên quá 200 là client đọc lệch.
                int soMon = Math.min(player.inventory.itemsBoxCrackBall.size(), 100);
                msg.writer().writeByte(soMon);
                for (int i = 0; i < soMon; i++) {
                    Item item = player.inventory.itemsBoxCrackBall.get(i);
                    msg.writer().writeShort(item.template.id);
                    msg.writer().writeUTF("Vật phẩm: " + item.template.name + (item.quantity == 0 ? "" : "\n|7|Số lượng: " + Util.format(item.quantity)) + (item.message != null ? "\n" + item.message : ""));
                    msg.writer().writeByte(item.itemOptions.size() + 1);
                    msg.writer().writeByte(31);
                    msg.writer().writeShort(item.quantity);
                    for (ItemOption io : item.itemOptions) {
                        msg.writer().writeByte(io.optionTemplate.id);
                        msg.writer().writeShort(io.param);
                    }
                    msg.writer().writeByte(item.message != null ? 1 : 0);
                    if (item.template.type == 5) {
                        msg.writer().writeByte(1);
                        msg.writer().writeShort(item.template.head);
                        msg.writer().writeShort(item.template.body);
                        msg.writer().writeShort(item.template.leg);
                        msg.writer().writeShort(-1);
                    } else {
                        msg.writer().writeByte(0);
                    }
                }
                player.sendMessage(msg);
            }
        } catch (Exception e) {
            Logger.logException(ShopService.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private void openShopType8(Player player, String tagName, List<Item> items) {
        if (items == null) {
            return;
        }
        player.idMark.setTagNameShop(tagName);
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(8);
            msg.writer().writeByte(1);
            msg.writer().writeUTF("Mua lại");
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                int giamualaingoc = item.template.gem / 2;
                int giamualaivang = giamualaingoc == 0 ? (int) item.template.gold / 2 > 0 ? (int) item.template.gold / 2 : item.quantity * 100 : 0;
                msg.writer().writeShort(item.template.id);
                msg.writer().writeInt(giamualaivang);
                msg.writer().writeInt(giamualaingoc);
                msg.writer().writeInt(item.quantity);
                msg.writer().writeByte(item.itemOptions.size());
                for (Item.ItemOption io : item.itemOptions) {
                    msg.writer().writeByte(io.optionTemplate.id);
                    msg.writer().writeShort(io.param);
                }
                msg.writer().writeByte(0);
                if (item.template.type == 5) {
                    msg.writer().writeByte(1);
                    msg.writer().writeShort(item.template.head);
                    msg.writer().writeShort(item.template.body);
                    msg.writer().writeShort(item.template.leg);
                    msg.writer().writeShort(-1);
                } else {
                    msg.writer().writeByte(0);
                }
            }
            player.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void takeItem(Player player, byte type, int tempId) {
        String tagName = player.idMark.getTagNameShop();
        if (tagName == null || tagName.length() <= 0) {
            return;
        }

        // Xử lý khi người chơi mua items từ các tag khác nhau
        if (tagName.equals("ITEMS_LUCKY_ROUND") || tagName.equals("Gift_Box")) {
            getItemSideBoxLuckyRound(player, player.inventory.itemsBoxCrackBall, type, tempId);
            return;
        } else if (tagName.equals("ITEMS_REWARD")) {
            return;

        } else if (tagName.equals("ITEMS_DABAN")) {
            buyItemDaBan(player, player.inventory.itemsDaBan, type, tempId);
            return;
        } else if (tagName.equals("BILL")) {
            if (isEligibleForBill(player, tempId)) {
                buyItemHD(player, tempId); // Tiến hành mua item nếu thỏa mãn điều kiện
            } else {
                Service.gI().sendThongBao(player, "Không đủ điều kiện để mua item này.");
            }
            return;
        }

        // Kiểm tra nếu không có shop mở
        if (player.idMark.getShopOpen() == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }

        // Các loại tag khác
        if (tagName.equals("BUA_1H") || tagName.equals("BUA_8H") || tagName.equals("BUA_1M")) {
            buyItemBua(player, tempId);
        } else if (tagName.equals("SANTA_HEAD")) {
            Item itS = ItemService.gI().createNewItem((short) tempId);
            player.head = (short) itS.template.head;
        } else {
            buyItem(player, tempId);
        }

        // Cập nhật lại số tiền của người chơi sau giao dịch
        Service.gI().sendMoney(player);
    }

    private boolean isEligibleForBill(Player player, int tempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop itemShop = shop.getItemShop(tempId);
        if (itemShop == null) {
            return false; // Nếu không có item trong shop
        }

        boolean hasSetThan = player.inventory.itemsBody.stream()
                .anyMatch(it -> it != null && it.template != null && it.template.level == 13);

        if (!hasSetThan) {
            Service.gI().sendThongBao(player, "Không có đủ set thần.");
            return false; // Nếu không có đủ set thần
        }
        Item item = ItemService.gI().createItemFromItemShop(itemShop);

        if (item.template.level == 14) {
            boolean hasEnoughFood = player.inventory.itemsBag.stream()
                    .filter(it -> it != null && it.template != null
                    && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667))
                    .anyMatch(it -> it.quantity >= 99);

            if (!hasEnoughFood) {
                Service.gI().sendThongBao(player, "Không có đủ thức ăn.");
                return false; // Nếu không có đủ thức ăn
            }
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "Hành trang đầy, không thể chứa thêm.");
            return false; // Nếu không đủ không gian hành trang
        }

        return true; // Nếu tất cả các điều kiện đã thỏa mãn
    }

    private int couponBalanceOf(Player player) {
        models.shop.Shop shop = player.idMark.getShopOpen();
        if (shop != null) {
            if (shop.id == 47) return player.inventory.diemDT;
            if (shop.id == 48) return player.inventory.diemXen;
        }
        return player.inventory.coupon;
    }

    private void subCouponBalance(Player player, int amount) {
        models.shop.Shop shop = player.idMark.getShopOpen();
        if (shop != null) {
            if (shop.id == 47) { player.inventory.diemDT = Math.max(0, player.inventory.diemDT - amount); return; }
            if (shop.id == 48) { player.inventory.diemXen = Math.max(0, player.inventory.diemXen - amount); return; }
        }
        player.inventory.coupon -= amount;
    }

    private boolean subMoneyByItemShop(Player player, ItemShop is) {
        if (!validPayment(player, is)) return false;
        long gold = 0;
        long gem = 0;
        long ruby = 0;
        long coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;

        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng");
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc");
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBao(player, "Bạn không có đủ hồng ngọc");
            return false;
        } else if (couponBalanceOf(player) < coupon) {
            Service.gI().sendThongBao(player, "Bạn không có đủ điểm");
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        subCouponBalance(player, (int) coupon);
        return true;
    }

    private boolean subMoneyByItemShopV2(Player player, ItemShop is) {
        if (!validPayment(player, is)) return false;
        long gold = 0;
        long gem = 0;
        long ruby = 0;
        long coupon = 0;
        switch (is.typeSell) {
            case COST_GOLD ->
                gold = is.cost;
            case COST_GEM ->
                gem = is.cost;
            case COST_RUBY ->
                ruby = is.cost;
            case COST_COUPON ->
                coupon = is.cost;

        }
        if (player.inventory.gold < gold) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ vàng, còn thiếu " + Util.numberToMoney(player.inventory.gold - gold));
            return false;
        } else if (player.inventory.gem < gem) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ ngọc, còn thiếu " + Util.numberToMoney(player.inventory.gem - gem));
            return false;
        } else if (player.inventory.ruby < ruby) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ hồng ngọc, còn thiếu " + Util.numberToMoney(player.inventory.ruby - ruby));
            return false;
        } else if (couponBalanceOf(player) < coupon) {
            Service.gI().sendThongBaoOK(player, "Bạn không đủ điểm, còn thiếu " + Util.numberToMoney(couponBalanceOf(player) - coupon));
            return false;
        }
        player.inventory.gold -= gold;
        player.inventory.gem -= gem;
        player.inventory.ruby -= ruby;
        subCouponBalance(player, (int) coupon);
        Service.gI().sendMoney(player);
        return true;
    }

    private boolean validPayment(Player player, ItemShop offer) {
        if (offer == null || offer.cost < 0 || offer.cost > Integer.MAX_VALUE
                || (offer.typeSell != COST_GOLD && offer.typeSell != COST_GEM
                && offer.typeSell != COST_RUBY && offer.typeSell != COST_COUPON)) {
            Service.gI().sendThongBao(player, "Giá bán hoặc loại tiền không hợp lệ, chưa trừ chi phí.");
            return false;
        }
        return true;
    }

    /**
     * Mua bùa
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    private void buyItemBua(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (!subMoneyByItemShop(player, is)) {
            return;
        }
        InventoryService.gI().addItemBag(player, ItemService.gI().createItemFromItemShop(is));
        InventoryService.gI().sendItemBags(player);
        opendShop(player, shop.tagName, true);
    }

    /**
     * Mua vật phẩm trong cửa hàng
     *
     * @param player người chơi
     * @param itemTempId id template vật phẩm
     */
    public void learnKyNang(Player player, ItemShop is) {
        if (is == null || is.temp == null || is.cost < 0) {
            Service.gI().sendThongBao(player, "Giá học kỹ năng không hợp lệ.");
            return;
        }
        if (player.nPoint.tiemNang < is.cost) {
            // ko du tiem nang
            return;
        }
        if (player.nPoint.power < is.temp.strRequire) {
            // ko dat yeu cau suc manh
            return;
        }
        String[] subName = is.temp.name.split("");
        byte levelBook = Byte.parseByte(subName[subName.length - 1]);

        var timeStudy = "";
        var timeLong = ConstHocKyNang.TimeUseSkill[levelBook - 1];
        switch (levelBook) {
            default:
                timeStudy = TimeUtil.convertTime((int) (timeLong / 1000));
                break;

        }
        player.hocKyNang.ItemTemplateSkillId = is.temp.id;
        player.hocKyNang.Time = timeLong;
        player.hocKyNang.PotentialLearn = is.cost;
        player.hocKyNang.Level = levelBook;

        var textMenu = MessageFormat.format(ConstHocKyNang.DO_YOU_ADD_SKILL, levelBook, Util.numberToMoney(is.cost), timeStudy);
        NpcService.gI().createMenuConMeo(player, ConstNpc.HOC_KY_NANG, NpcService.gI().getAvatar(13 + player.gender), textMenu, "Đồng ý", "Từ chối");

    }

    public void buyItem(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        if (shop == null) {
            Service.gI().sendThongBao(player, "Chưa mở cửa hàng.");
            return;
        }
        if (ToriBotItemShop.matches(shop)) {
            ToriBotItemShop.buy(player, itemTempId);
            return;
        }
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        if (InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }
        if (itemTempId == 711 && !InventoryService.gI().findItemSkinQuyLaoKame(player)) {
            Service.gI().sendThongBao(player, "Bạn phải có cải trang thành Quy Lão Kame mới có thể đổi.");
            return;
        }
        if (shop.id == 24) {
            if (!player.playerTask.taskdh.CheckItem(player, is, itemTempId)) {
                return;
            }
        }
        if (shop.typeShop == ShopService.LEARN_SKILL_SHOP) {
            learnKyNang(player, is);
            return;
        }
        if ((itemTempId == 517 && player.inventory.itemsBag.size() >= Inventory.MAX_ITEMS_BAG)
                || (itemTempId == 518 && player.inventory.itemsBox.size() >= Inventory.MAX_ITEMS_BOX)) {
            Service.gI().sendThongBao(player, "Hành trang hoặc rương đã đạt tối đa, chưa trừ chi phí.");
            return;
        }
        Item voucher = null;
        if ("SANTA_PGG".equals(shop.tagName)) {
            voucher = InventoryService.gI().findItem(player.inventory.itemsBag, 459);
            if (voucher == null || !voucher.isNotNullItem() || voucher.quantity < 1) {
                Service.gI().sendThongBao(player, "Bạn không có phiếu giảm giá!");
                return;
            }
        }
        if (shop.typeShop == ShopService.NORMAL_SHOP || shop.id == 30 || shop.id == 47 || shop.id == 48) {
            // shop 30 (Quy Lão), 47 (Đường Tăng), 48 (Xên): SPEC_SHOP để hiện icon tùy chỉnh nhưng trả bằng điểm (coupon)
            if (!subMoneyByItemShop(player, is)) {
                return;
            }
        } else if (shop.typeShop == ShopService.SPEC_SHOP) {
            if (!this.subIemByItemShop(player, is)) {
                return;
            }
        } else {
            Service.gI().sendThongBao(player, "Loại cửa hàng không được hỗ trợ.");
            return;
        }
        if (voucher != null) {
            InventoryService.gI().subQuantityItemsBag(player, voucher, 1);
        }
        Item quaBong = ItemService.gI().createItemFromItemShop(is);
        if (quaBong.template.id == 1532) {
            quaBong = ItemService.gI().createNewItem((short) 1532);
            for (Item.ItemOption io : is.options) {
                if (io != null && io.optionTemplate != null) {
                    quaBong.itemOptions.add(new Item.ItemOption(io.optionTemplate.id, io.param));
                }
            }
            player.point_pokemon++;
        }
        Item item = ItemService.gI().createItemFromItemShop(is);
        item.itemOptions.removeIf(option -> option.optionTemplate.id == 228);
        if (!InventoryService.gI().addItemBag(player, item)) {
            // Hoàn lại chi phí nếu không thêm được đồ (đầy túi / vượt trần vàng)
            if (is.typeSell == COST_GOLD) player.inventory.gold += is.cost;
            else if (is.typeSell == COST_GEM) player.inventory.gem += (int) is.cost;
            else if (is.typeSell == COST_RUBY) player.inventory.ruby += (int) is.cost;
            else if (is.typeSell == COST_COUPON) subCouponBalance(player, (int) is.cost);
            if (voucher != null) voucher.quantity += 1;
            Service.gI().sendMoney(player);
            InventoryService.gI().sendItemBags(player);
            Service.gI().sendThongBao(player, "Không thể nhận vật phẩm, đã hoàn lại chi phí.");
            return;
        }
        Service.gI().sendThongBao(player, "Mua thành công " + is.temp.name);
        InventoryService.gI().sendItemBags(player);
    }

    private boolean subIemByItemShop(Player pl, ItemShop itemShop) {
        if (itemShop.cost < 0 || itemShop.cost > Integer.MAX_VALUE) {
            Service.gI().sendThongBao(pl, "Giá bán không hợp lệ, chưa trừ chi phí.");
            return false;
        }
        boolean isBuy = false;
        if (itemShop.iconSpec == 14116) {//Coin

        }
        short itSpec = ItemService.gI().getItemIdByIcon((short) itemShop.iconSpec);
        long buySpec = itemShop.cost;
        Item itS = ItemService.gI().createNewItem(itSpec);
        switch (itS.template.id) {
            case 76:
            case 188:
            case 189:
            case 190:
                if (pl.inventory.gold >= buySpec) {
                    pl.inventory.gold -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Vàng Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            case 861:
                if (pl.inventory.ruby >= buySpec) {
                    pl.inventory.ruby -= buySpec;
                    isBuy = true;
                } else {
                    Service.gI().sendThongBao(pl, "Bạn Không Đủ Hồng Ngọc Để Mua Vật Phẩm");
                    isBuy = false;
                }
                break;
            default:
                if (InventoryService.gI().findItemBag(pl, itSpec) == null || !InventoryService.gI().findItemBag(pl, itSpec).isNotNullItem()) {
                    Service.gI().sendThongBao(pl, "Không tìm thấy " + itS.template.name);
                    isBuy = false;
                } else if (InventoryService.gI().findItemBag(pl, itSpec).quantity < buySpec) {
                    Service.gI().sendThongBao(pl, "Bạn không có đủ " + buySpec + " " + itS.template.name);
                    isBuy = false;
                } else {
                    InventoryService.gI().subQuantityItemsBag(pl, InventoryService.gI().findItemBag(pl, itSpec), (int) buySpec);
                    isBuy = true;
                }
                break;
        }
        return isBuy;
    }

    public void showConfirmSellItem(Player pl, int where, int index) {
        if (pl.isTrade) {
            return;
        }
        Item item = null;
        if (where == 0) {
            if (index < 0) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            item = pl.inventory.itemsBody.get(index);
        } else {
            if (pl.getSession().version < 220) {
                index -= (pl.inventory.itemsBody.size() - 7);
            }
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 1471) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này , ấn sử dụng để bú vàng");
                return;
            }
            int quantity = item.quantity;
            int cost = item.template.gold;
            if (item.template.id == 457) {
                if (quantity > 1) {
                    Service.gI().sendThongBao(pl, "Ấn sử dụng để dùng được nhiều Thỏi Vàng");
                    return;
                }
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= quantity;

            String text = "Bạn có muốn bán\nx" + quantity
                    + " " + item.template.name + "\nvới giá là " + Util.numberToMoney(cost) + " vàng?";
            Message msg = null;
            try {
                msg = new Message(7);
                msg.writer().writeByte(where);
                msg.writer().writeShort(index);
                msg.writer().writeUTF(text);
                pl.sendMessage(msg);
            } catch (Exception e) {
            } finally {
                if (msg != null) {
                    msg.cleanup();
                }
            }
        }
    }

    public void sellItem(Player pl, int where, int index) {
        if (pl.idMark.getShopOpen() == null || pl.idMark.getTagNameShop() == null) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        if (index < 0 || (where == 0 ? index >= pl.inventory.itemsBody.size() : index >= pl.inventory.itemsBag.size())) {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
            return;
        }
        Item item = null;
        if (where == 0) {
            item = pl.inventory.itemsBody.get(index);
        } else {
            item = pl.inventory.itemsBag.get(index);
        }
        if (item != null && item.isNotNullItem()) {
            if (item.template.id == 570) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm này");
                return;
            }
            if (InventoryService.gI().getParam(pl, 93, item.template.id) > 0) {
                Service.gI().sendThongBao(pl, "Bạn không thể bán vật phẩm có hạn sử dụng");
                return;
            }
            int quantity = item.quantity;
            long cost = item.template.gold;
            if (item.template.id == 457) {
                quantity = 1;
            } else {
                cost /= 4;
            }
            if (cost == 0) {
                cost = 1;
            }
            cost *= (long) quantity;

            if (pl.inventory.gold + cost > Inventory.LIMIT_GOLD) {
                Service.gI().sendThongBao(pl, "Vàng sau khi bán vượt quá giới hạn");
                return;
            }
            pl.inventory.gold += cost;
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "Đã bán " + item.template.name
                    + " thu được " + Util.numberToMoney(cost) + " vàng");

            //Add vật phẩm đã bán
            if (item.template.id != 457) {
                BuyBackService.gI().addItem(pl, item);
            }
            if (where == 0) {
                InventoryService.gI().subQuantityItemsBody(pl, item, quantity);
                InventoryService.gI().sendItemBody(pl);
                Service.gI().Send_Caitrang(pl);
            } else {
                InventoryService.gI().subQuantityItemsBag(pl, item, quantity);
                InventoryService.gI().sendItemBags(pl);
            }
            if ("BUNMA".equals(pl.idMark.getTagNameShop())
                    || "DENDE".equals(pl.idMark.getTagNameShop())
                    || "APPULE".equals(pl.idMark.getTagNameShop())) {
                AchievementService.gI().checkDoneTask(pl, ConstAchievement.TRUM_NHAT_VE_CHAI);
            }
        } else {
            Service.gI().sendThongBao(pl, "Không thể thực hiện");
        }
    }

    private void getItemSideBoxLuckyRound(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        switch (type) {
            case 0: //nhận
                if (!item.isNotNullItem()) {
                    Service.gI().sendThongBao(player, "Không thể thực hiện");
                    break;
                }
                boolean vaoVi = item.template.type == 9 || item.template.type == 10
                        || item.template.type == 34;
                if (!vaoVi && InventoryService.gI().getCountEmptyBag(player) == 0) {
                    Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    break;
                }

                if (InventoryService.gI().addItemBag(player, item)) {
                    Service.gI().sendThongBao(player,
                            "Bạn nhận được " + (item.template.id == 189
                                    ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                    InventoryService.gI().sendItemBags(player);
                    items.remove(index);
                } else {
                    InventoryService.gI().sendItemBags(player);
                    if (!vaoVi) {
                        Service.gI().sendThongBao(player, "Hành trang đã đầy");
                    }
                }
                break;
            case 1: //xóa
                items.remove(index);
                Service.gI().sendThongBao(player, "Xóa vật phẩm thành công");
                break;
            case 2: //nhận hết
                for (int i = items.size() - 1; i >= 0; i--) {
                    item = items.get(i);
                    if (!item.isNotNullItem()) {
                        continue; //ô rỗng thì không có template để đọc
                    }
                    if (InventoryService.gI().addItemBag(player, item)) {
                        Service.gI().sendThongBao(player,
                                "Bạn nhận được " + (item.template.id == 189
                                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
                        items.remove(i);
                    }
                }
                InventoryService.gI().sendItemBags(player);
                break;
        }

        if ("Gift_Box".equals(player.idMark.getTagNameShop())) {
            openMailBox(player);
        } else {
            openShopType4(player, player.idMark.getTagNameShop(), items);
        }
    }

    private void buyItemDaBan(Player player, List<Item> items, byte type, int index) {
        if (items == null) {
            return;
        }
        if (index < 0 || index >= items.size()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        Item item = items.get(index);
        if (!item.isNotNullItem()) {
            Service.gI().sendThongBao(player, "Không thể thực hiện");
            return;
        }
        // Giá mua lại tính bằng long: quantity*100 với pile lớn tràn int thành số âm
        // -> gold -= (âm) = CỘNG vàng. Chặn tại đây.
        long giamualaingoc = (long) item.template.gem / 2;
        long giamualaivang = giamualaingoc == 0
                ? ((long) item.template.gold / 2 > 0 ? (long) item.template.gold / 2 : (long) item.quantity * 100)
                : 0;
        if (giamualaivang < 0 || giamualaingoc < 0) {
            return;
        }
        // Kiểm tra chỗ chứa TRƯỚC khi trừ tiền (trước đây trừ xong mới phát hiện túi đầy -> mất tiền)
        boolean vaoVi = item.template.type == 9 || item.template.type == 10 || item.template.type == 34;
        if (!vaoVi && InventoryService.gI().getCountEmptyBag(player) == 0) {
            Service.gI().sendThongBao(player, "Hành trang đã đầy");
            return;
        }
        if (giamualaivang > 0 && player.inventory.gold < giamualaivang) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vàng!");
            return;
        }
        if (giamualaingoc > 0 && player.inventory.gem < giamualaingoc) {
            Service.gI().sendThongBao(player, "Bạn không có đủ ngọc xanh!");
            return;
        }
        player.inventory.gem -= giamualaingoc;
        player.inventory.gold -= giamualaivang;
        Service.gI().sendMoney(player);
        InventoryService.gI().addItemBag(player, item);
        Service.gI().sendThongBao(player,
                "Bạn nhận được " + (item.template.id == 189
                        ? Util.numberToMoney(item.quantity) + " vàng" : item.template.name));
        InventoryService.gI().sendItemBags(player);
        items.remove(index);
        openShopType8(player, player.idMark.getTagNameShop(), items);
    }


    private void buyItemHD(Player player, int itemTempId) {
        Shop shop = player.idMark.getShopOpen();
        ItemShop is = shop.getItemShop(itemTempId);
        if (is == null) {
            Service.gI().sendThongBao(player, "[ Hệ Thống ] Không thể thực hiện");
            return;
        }
        boolean checkfullset = IntStream.range(0, 5)
                .allMatch(i -> player.inventory.itemsBody.get(i) != null
                && player.inventory.itemsBody.get(i).template != null
                && player.inventory.itemsBody.get(i).template.level == 13);

        if (!checkfullset) {
            Service.gI().sendThongBao(player, "[ Hệ Thống ] Không đủ sét thần (5 món), không thể mua tiếp");
            return;
        }

        Item item = ItemService.gI().createItemFromItemShop(is);
        if (shop.typeShop == ShopService.SPEC_SHOP && !this.subIemByItemShop(player, is)) {
            return;
        }

        if (InventoryService.gI().getCountEmptyBag(player) < 1) {
            Service.gI().sendThongBao(player, "[ Hệ Thống ] Hành trang đã đầy, không thể chứa thêm.");
            return;
        }

        if (!subMoneyByItemShopV2(player, is)) {
            return;
        }
        if (item.template.level == 14) {
            Item doAn = player.inventory.itemsBag.stream().filter(it -> it != null && it.template != null && (it.template.id == 663 || it.template.id == 664 || it.template.id == 665 || it.template.id == 666 || it.template.id == 667) && it.quantity >= 99).findFirst().orElse(null);
            if (doAn != null) {
                InventoryService.gI().subQuantityItemsBag(player, doAn, 99);
            } else {
                Service.gI().sendThongBao(player, "[ Hệ Thống ] Không có đủ thức ăn");
                return;
            }
        }
        int checkbodyplayer = switch (item.template.type) {
            case 0 ->
                0;
            case 1 ->
                1;
            case 2 ->
                2;
            case 3 ->
                3;
            case 4 ->
                4;
            default ->
                -1;
        };

        if (checkbodyplayer != -1 && player.inventory.itemsBody.get(checkbodyplayer) != null) {
            Item removedItem = player.inventory.itemsBody.get(checkbodyplayer);
            player.inventory.itemsBody.set(checkbodyplayer, new Item());
            player.nPoint.calPoint();
            Service.gI().sendThongBao(player, "[ Hệ Thống ] Bạn đã mất " + removedItem.template.name);
        }

        int param = 0;
        if (item.template.level == 14) {
            param = Util.isTrue(25, 100) ? Util.nextInt(11, 15)
                    : Util.isTrue(25, 75) ? Util.nextInt(5, 10)
                    : Util.nextInt(0, 4);
        }

        List<ItemOption> itemOptions = new ArrayList<>();
        if (!item.itemOptions.isEmpty()) {
            for (ItemOption ios : item.itemOptions) {
                if (item.template.level == 14 && InventoryService.gI().optionCanUpgrade(ios.optionTemplate.id) && param > 0) {
                    int id = ios.optionTemplate.id;
                    int param1 = ios.param + (ios.param * param) / 100;
                    itemOptions.add(new ItemOption(id, param1));
                } else if (ios.optionTemplate.id != 164) {
                    itemOptions.add(new ItemOption(ios.optionTemplate.id, ios.param));
                }
            }
        } else {
            itemOptions.add(new ItemOption(73, (short) 0));
        }

        item.itemOptions.clear();
        item.itemOptions.addAll(itemOptions);

        InventoryService.gI().addItemBag(player, item);
        InventoryService.gI().sendItemBags(player);
        InventoryService.gI().sendItemBody(player);
        Service.gI().point(player);
        Service.gI().sendEffPlayer(player);
        Service.gI().sendThongBao(player, "[ Hệ Thống ] Mua thành công " + is.temp.name);
    }
}
