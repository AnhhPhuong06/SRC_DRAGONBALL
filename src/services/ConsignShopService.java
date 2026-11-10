package services;

 
import consts.ConstNpc;
import managers.ConsignShopManager;
import models.ConsignItem;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import network.io.Message;
import services.player.InventoryService;
import services.map.NpcService;

import java.util.*;

import database.daos.NDVSqlFetcher;
import utils.Logger;

public class ConsignShopService {

    private static ConsignShopService instance;

    public static ConsignShopService gI() {
        if (instance == null) {
            instance = new ConsignShopService();
        }
        return instance;
    }

    private boolean isKyGui(Item item) {
        // Kiểm tra theo item.template.type
        switch (item.template.type) {
            case 12:
            case 15:
            case 14:
            case 6:
                return true;
            case 27:
                // Kiểm tra các item cụ thể với template.id = 568, 921, 1155, 1156
                switch (item.template.id) {
                    case 533:
                    case 663:
                    case 664:
                    case 665:
                    case 666:
                    case 667:
                    case 380:
                    case 1099:
                    case 1100:
                    case 1101:
                    case 1102:
                    case 1103:
                        return true;
                    default:
                        return false;
                }
            default:
                // Kiểm tra các itemOptions để xem có optionTemplate.id == 86 hoặc 87 không
                for (int i = 0; i < item.itemOptions.size(); ++i) {
                    int optionId = ((Item.ItemOption) item.itemOptions.get(i)).optionTemplate.id;
                    if (optionId == 86 || optionId == 87) {
                        return true;  // Nếu tìm thấy option 86 hoặc 87, trả về true
                    }
                }
                return false;  // Nếu không tìm thấy, trả về false
        }
    }

    private List<ConsignItem> getItemKyGui(byte tab, int... max) {
        List<ConsignItem> listSort = new ArrayList<>();
        ConsignShopManager.gI().listItem.stream()
                .filter(it -> it != null && it.tab == tab && !it.isBuy) // Filter once
                .sorted(Comparator.comparing((ConsignItem i) -> i.lasttime, Comparator.reverseOrder())) // Sort once
                .forEachOrdered(listSort::add);

        List<ConsignItem> listSort2 = new ArrayList<>();
        if (max.length == 2) {
            int from = max[0];
            int to = Math.min(max[1], listSort.size());
            for (int i = from; i < to; i++) {
                if (listSort.get(i) != null) {
                    listSort2.add(listSort.get(i));
                }
            }
            return listSort2;
        }
        if (max.length == 1 && listSort.size() > max[0]) {
            for (int i = 0; i < max[0]; i++) {
                if (listSort.get(i) != null) {
                    listSort2.add(listSort.get(i));
                }
            }
            return listSort2;
        }
        return listSort;
    }

    private List<ConsignItem> getItemKyGui() {
        List<ConsignItem> its = new ArrayList<>();
        List<ConsignItem> listSort = new ArrayList<>();
        ConsignShopManager.gI().listItem.stream().filter((it) -> (it != null && !it.isBuy)).forEachOrdered(its::add);
        its.stream().filter(Objects::nonNull).sorted(Comparator.comparing(i -> i.lasttime, Comparator.reverseOrder())).forEach(i -> listSort.add(i));
        return listSort;
    }

    public void buyItem(Player pl, int id) {
        List<ConsignItem> list = ConsignShopManager.gI().listItem;
        synchronized (list) {
            ConsignItem it = getItemBuy(id);
            if (it == null || it.isBuy) {
                Service.gI().sendThongBaoOK(pl, "Vật phẩm không tồn tại hoặc đã được bán");
                return;
            }
            if (it.player_sell == pl.id) {
                Service.gI().sendThongBaoOK(pl, "Không thể mua vật phẩm bản thân đăng bán");
                return;
            }
            if (InventoryService.gI().getCountEmptyBag(pl) == 0) {
                Service.gI().sendThongBaoOK(pl, "Hành trang đã đầy!");
                return;
            }
            // Chiếm quyền (atomic claim) TRƯỚC khi đụng tiền - chặn race mua kép nhân bản item
            it.isBuy = true;
            if (it.goldSell > 0) {
                if (pl.inventory.gold < it.goldSell) {
                    it.isBuy = false;
                    Service.gI().sendThongBaoOK(pl, "Bạn không đủ vàng để mua vật phẩm này!");
                    return;
                }
                pl.inventory.subGold(it.goldSell);
            } else if (it.rubySell > 0) {
                if (pl.inventory.ruby < it.rubySell) {
                    it.isBuy = false;
                    Service.gI().sendThongBaoOK(pl, "Bạn không đủ hồng ngọc để mua vật phẩm này!");
                    return;
                }
                pl.inventory.subRuby(it.rubySell);
            }
            Service.gI().sendMoney(pl);
            Item item = ItemService.gI().createNewItem(it.itemId, it.quantity);
            item.itemOptions.addAll(it.options);
            if (!InventoryService.gI().addItemBag(pl, item)) {
                if (it.goldSell > 0) pl.inventory.gold += it.goldSell;
                if (it.rubySell > 0) pl.inventory.ruby += it.rubySell;
                it.isBuy = false;
                Service.gI().sendMoney(pl);
                Service.gI().sendThongBao(pl, "Hành trang đã đầy, giao dịch không thành công");
                openShopKyGui(pl);
                return;
            }
            InventoryService.gI().sendItemBags(pl);
            Service.gI().sendThongBao(pl, "Bạn đã nhận được " + item.template.name);
            openShopKyGui(pl);
        }
    }


    public ConsignItem getItemBuy(int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && !it.isBuy) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, int id) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.id == id && it.player_sell == pl.id) {
                return it;
            }
        }
        return null;
    }

    public ConsignItem getItemBuy(Player pl, ConsignItem itk) {
        for (ConsignItem it : ConsignShopManager.gI().listItem) {
            if (it != null && it.player_sell == pl.id && it == itk) {
                return it;
            }
        }
        return null;
    }

    public void openShopKyGui(Player pl, byte index, int page) {
        if (page > getItemKyGui(index).size()) {
            return;
        }
        Message msg = null;
        try {
            msg = new Message(-100);
            msg.writer().writeByte(index);
            List<ConsignItem> items = getItemKyGui(index);
            List<ConsignItem> itemsSend = getItemKyGui(index, (int) (page * 20), (int) (page * 20 + 20));
            int cTab = (int) Math.ceil((double) items.size() / 20);
            byte tab = (byte) (cTab > 0 ? cTab : 1);
            msg.writer().writeByte(tab); // max page
            msg.writer().writeByte(page);
            msg.writer().writeByte(itemsSend.size());
            for (int j = 0; j < itemsSend.size(); j++) {
                ConsignItem itk = itemsSend.get(j);
                Item it = ItemService.gI().createNewItem(itk.itemId);
                it.itemOptions.clear();
                if (itk.options.isEmpty()) {
                    it.itemOptions.add(new ItemOption(73, 0));
                } else {
                    it.itemOptions.addAll(itk.options);
                }
                msg.writer().writeShort(it.template.id);
                msg.writer().writeShort(itk.id);
                msg.writer().writeInt(itk.goldSell);
                msg.writer().writeInt(itk.rubySell);
                msg.writer().writeByte(0); // buy type
                if (pl.getSession().version >= 222) {
                    msg.writer().writeInt(itk.quantity);
                } else {
                    msg.writer().writeByte(itk.quantity);
                }
                msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe
                msg.writer().writeByte(it.itemOptions.size());
                for (int a = 0; a < it.itemOptions.size(); a++) {
                    msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                    msg.writer().writeShort(it.itemOptions.get(a).param);
                }
                msg.writer().writeByte(0);
                if (pl.getSession().version >= 237 && pl.getSession().version != 240) {
                    try {
                        Player plSell = NDVSqlFetcher.loadById(itk.player_sell);
                        if (plSell != null) {
                            msg.writer().writeUTF(plSell.name);
                        } else {
                            msg.writer().writeUTF("");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            pl.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void upItemToTop(Player pl, int id) {
        ConsignItem it = getItemBuy(id);
        if (it == null || it.isBuy) {
            Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
            return;
        }
        if (it.player_sell != pl.id) {
            Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
            openShopKyGui(pl);
            return;
        }
        pl.idMark.setIdItemUpTop(id);
        NpcService.gI().createMenuConMeo(pl, ConstNpc.UP_TOP_ITEM, -1, "Bạn có muốn đưa vật phẩm '" + ItemService.gI().createNewItem(it.itemId).template.name + "' của bản thân lên trang đầu?\nYêu cầu 5tr vàng.", "Đồng ý", "Từ Chối");
    }

    public void claimOrDel(Player pl, byte action, int id) {
        List<ConsignItem> list = ConsignShopManager.gI().listItem;
        synchronized (list) {
            ConsignItem it = getItemBuy(pl, id);
            switch (action) {
                case 1:
                    if (it == null || it.isBuy) {
                        Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc đã được bán");
                        return;
                    }
                    if (it.player_sell != pl.id) {
                        Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
                        openShopKyGui(pl);
                        return;
                    }
                    Item item = ItemService.gI().createNewItem(it.itemId);
                    item.quantity = it.quantity;
                    item.itemOptions.clear();
                    item.itemOptions.addAll(it.options);
                    if (InventoryService.gI().getCountEmptyBag(pl) == 0) {
                        Service.gI().sendThongBao(pl, "Hành trang đã đầy, không thể hủy bán");
                        openShopKyGui(pl);
                        return;
                    }
                    if (ConsignShopManager.gI().listItem.remove(it)) {
                        if (InventoryService.gI().addItemBag(pl, item)) {
                            InventoryService.gI().sendItemBags(pl);
                            Service.gI().sendMoney(pl);
                            Service.gI().sendThongBao(pl, "Hủy bán vật phẩm thành công");
                            openShopKyGui(pl);
                        } else {
                            ConsignShopManager.gI().listItem.add(it);
                            Service.gI().sendThongBao(pl, "Hành trang đã đầy, không thể hủy bán");
                            openShopKyGui(pl);
                        }
                    }
                    break;
                case 2:
                    if (it == null || !it.isBuy) {
                        Service.gI().sendThongBao(pl, "Vật phẩm không tồn tại hoặc chưa được bán");
                        return;
                    }
                    if (it.player_sell != pl.id) {
                        Service.gI().sendThongBao(pl, "Vật phẩm không thuộc quyền sở hữu");
                        openShopKyGui(pl);
                        return;
                    }
                    long price = it.goldSell > 0 ? it.goldSell : it.rubySell;
                    long payout = price - price * 5L / 100;
                    if (price <= 0 || (it.goldSell > 0
                            ? pl.inventory.gold > models.player.Inventory.LIMIT_GOLD - payout
                            : pl.inventory.ruby > Integer.MAX_VALUE - payout)) {
                        Service.gI().sendThongBao(pl, "Số dư sau khi nhận vượt giới hạn. Hãy tiêu bớt tiền trước khi nhận.");
                        return;
                    }
                    // Nhận tiền SAU khi gỡ listing (atomic claim) - chặn race nhận thưởng kép
                    if (ConsignShopManager.gI().listItem.remove(it)) {
                        if (it.goldSell > 0) {
                            pl.inventory.gold += payout;
                        } else if (it.rubySell > 0) {
                            pl.inventory.ruby += (int)payout;
                        }
                        Service.gI().sendMoney(pl);
                        Service.gI().sendThongBao(pl, "Bạn đã bán vật phẩm thành công");
                        openShopKyGui(pl);
                    }
                    break;
            }
        }
    }


    public List<ConsignItem> getItemCanKiGui(Player pl) {
        List<ConsignItem> its = new ArrayList<>();

        // Lọc các vật phẩm đã đăng ký bán từ danh sách vật phẩm ký gửi
        ConsignShopManager.gI().listItem.stream()
                .filter((it) -> (it != null && it.player_sell == pl.id))
                .forEachOrdered(its::add);

        // Lọc vật phẩm từ hành trang của người chơi có thể ký gửi (dựa trên điều kiện)
        pl.inventory.itemsBag.stream()
                .filter((it) -> (it != null && it.template != null && isKyGui(it))) // Kiểm tra nếu vật phẩm có thể ký gửi
                .forEachOrdered((it) -> {
                    its.add(new ConsignItem(InventoryService.gI().getIndexBag(pl, it), it.template.id, (int) pl.id, (byte) 4, -1, -1, it.quantity, (byte) -1, it.itemOptions, false));
                });

        return its;
    }

    public int getMaxId() {
        try {
            List<Integer> id = new ArrayList<>();
            ConsignShopManager.gI().listItem.stream().filter(Objects::nonNull).forEachOrdered((it) -> {
                id.add(it.id);
            });
            return Collections.max(id);
        } catch (Exception e) {
            return 0;
        }
    }

    public byte getTabKiGui(Item it) {
        if (it.template.type >= 0 && it.template.type <= 2) {
            return 0;
        } else if ((it.template.type >= 3 && it.template.type <= 4)) {
            return 1;
        } else if (it.template.type == 29) {
            return 2;
        } else {
            return 3;
        }
    }

    public void KiGui(Player pl, int id, int money, byte moneyType, int quantity) {
        if (pl == null || pl.inventory == null || pl.nPoint == null || id < 0
                || id >= pl.inventory.itemsBag.size() || quantity <= 0 || quantity > 99
                || money <= 0 || (moneyType != 0 && moneyType != 1)
                || (moneyType == 0 && money > models.player.Inventory.LIMIT_GOLD)) return;
        if (pl.nPoint.power < 1_500_000_000) {
            Service.gI().sendThongBao(pl, "Yêu cầu đạt 1 tỉ 5 sức mạnh mới có ký gửi");
            return;
        }
        synchronized (ConsignShopManager.gI().listItem) {
        try {
            // Check if item is eligible for consignment
            if (pl.inventory.ruby < 5) {
                Service.gI().sendThongBao(pl, "Bạn cần có ít nhất 5 hồng ngọc để làm phí đăng bán");
                return;
            }
            if (id < 0 || id >= pl.inventory.itemsBag.size()) {
                Service.gI().sendThongBao(pl, "Không thể thực hiện");
                return;
            }
            Item it = ItemService.gI().copyItem(pl.inventory.itemsBag.get(id));
            if (it == null || it.template == null) return;
            if (!isKyGui(it)) {  // Ensure the item is allowed for consignment
                Service.gI().sendThongBao(pl, "Vật phẩm này không thể ký gửi");
                return;
            }
            if (money <= 0 || quantity > it.quantity) {
                Service.gI().sendThongBao(pl, "Có lỗi xảy ra");
                openShopKyGui(pl);
                return;
            }
            if (quantity > 99) {
                Service.gI().sendThongBao(pl, "Ký gửi tối đa x99");
                openShopKyGui(pl);
                return;
            }
            pl.inventory.ruby -= 5; // Subtract fee
            switch (moneyType) {
                case 0 -> {
                    // Gold
                    InventoryService.gI().subQuantityItemsBag(pl, pl.inventory.itemsBag.get(id), quantity);
                    ConsignShopManager.gI().listItem.add(new ConsignItem(getMaxId() + 1, it.template.id, (int) pl.id, getTabKiGui(it), money, -1, quantity, System.currentTimeMillis(), it.itemOptions, false));
                }
                case 1 -> {
                    // Ruby
                    InventoryService.gI().subQuantityItemsBag(pl, pl.inventory.itemsBag.get(id), quantity);
                    ConsignShopManager.gI().listItem.add(new ConsignItem(getMaxId() + 1, it.template.id, (int) pl.id, getTabKiGui(it), -1, money, quantity, System.currentTimeMillis(), it.itemOptions, false));
                }
                default -> {
                    Service.gI().sendThongBao(pl, "Có lỗi xảy ra");
                    openShopKyGui(pl);
                    return;
                }
            }
            InventoryService.gI().sendItemBags(pl);
            openShopKyGui(pl);
            Service.gI().sendMoney(pl);
            Service.gI().sendThongBao(pl, "Đăng bán thành công");
        } catch (Exception e) {
            Logger.logException(ConsignShopService.class, e);
        }
        }
    }

    public void openShopKyGui(Player pl) {
        Message msg = null;
        try {
            msg = new Message(-44);
            msg.writer().writeByte(2);
            msg.writer().writeByte(5);
            for (byte i = 0; i < 5; i++) {
                if (i == 4) {
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    msg.writer().writeByte(0);
                    msg.writer().writeByte(getItemCanKiGui(pl).size());
                    for (int j = 0; j < getItemCanKiGui(pl).size(); j++) {
                        ConsignItem itk = getItemCanKiGui(pl).get(j);
                        if (itk == null) {
                            continue;
                        }
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            it.itemOptions.addAll(itk.options);
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.rubySell);
                        if (getItemBuy(pl, itk) == null) {
                            msg.writer().writeByte(0); // buy type
                        } else if (itk.isBuy) {
                            msg.writer().writeByte(2);
                        } else {
                            msg.writer().writeByte(1);
                        }
                        if (pl.getSession().version >= 222) {
                            msg.writer().writeInt(itk.quantity);
                        } else {
                            msg.writer().writeByte(itk.quantity);
                        }
                        msg.writer().writeByte(1); // isMe
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            msg.writer().writeShort(it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0);
                        int type = pl.getSession().version >= 237 && pl.getSession().version != 240 ? 2 : 0;
                        msg.writer().writeByte(type);
                        if (type == 2) {
                            try {
                                Player plSell = NDVSqlFetcher.loadById(itk.player_sell);
                                if (plSell != null) {
                                    msg.writer().writeUTF(plSell.name);
                                } else {
                                    msg.writer().writeUTF("");
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                } else {
                    List<ConsignItem> items = getItemKyGui(i);
                    List<ConsignItem> itemsSend = getItemKyGui(i, (byte) 20);
                    msg.writer().writeUTF(ConsignShopManager.gI().tabName[i]);
                    int cTab = (int) Math.ceil((double) items.size() / 20);
                    byte tab = (byte) (cTab > 0 ? cTab : 1);
                    msg.writer().writeByte(tab); // max page
                    msg.writer().writeByte(itemsSend.size());
                    for (int j = 0; j < itemsSend.size(); j++) {
                        ConsignItem itk = itemsSend.get(j);
                        Item it = ItemService.gI().createNewItem(itk.itemId);
                        it.itemOptions.clear();
                        if (itk.options.isEmpty()) {
                            it.itemOptions.add(new ItemOption(73, 0));
                        } else {
                            it.itemOptions.addAll(itk.options);
                        }
                        msg.writer().writeShort(it.template.id);
                        msg.writer().writeShort(itk.id);
                        msg.writer().writeInt(itk.goldSell);
                        msg.writer().writeInt(itk.rubySell);
                        msg.writer().writeByte(0); // buy type
                        if (pl.getSession().version >= 222) {
                            msg.writer().writeInt(itk.quantity);
                        } else {
                            msg.writer().writeByte(itk.quantity);
                        }
                        msg.writer().writeByte(itk.player_sell == pl.id ? 1 : 0); // isMe
                        msg.writer().writeByte(it.itemOptions.size());
                        for (int a = 0; a < it.itemOptions.size(); a++) {
                            msg.writer().writeByte(it.itemOptions.get(a).optionTemplate.id);
                            msg.writer().writeShort(it.itemOptions.get(a).param);
                        }
                        msg.writer().writeByte(0); // new item
                        int type = pl.getSession().version >= 237 && pl.getSession().version != 240 ? 2 : 0;
                        msg.writer().writeByte(type);
                        if (type == 2) {
                            try {
                                Player plSell = NDVSqlFetcher.loadById(itk.player_sell);
                                if (plSell != null) {
                                    msg.writer().writeUTF(plSell.name);
                                } else {
                                    msg.writer().writeUTF("");
                                }
                            } catch (Exception e) {
                            }
                        }
                    }
                }
            }
            pl.sendMessage(msg);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
}
