package services.func;

 
import database.daos.HistoryTransactionDAO;
import models.item.Item;
import models.player.Inventory;
import models.player.Player;
import network.io.Message;
import services.ItemService;
import services.player.PlayerService;
import services.Service;
import services.player.InventoryService;
import utils.Logger;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import services.TaskService;

public class Trade {

    public static final int TIME_TRADE = 180000;
    public static final int QUANLITY_MAX = 2_000_000_000;

    /** Trần vàng tươi mỗi LẦN giao dịch. Đổi ở đây nếu muốn nới/siết. */
    public static final int VANG_TOI_DA_MOI_LAN = 100_000_000;

    /** Bật/tắt log debug cho giao dịch (chỉ in ra console/cmd, không ghi file). Đặt false để tắt. */
    public static boolean DEBUG = true;

    private Player player1;
    private Player player2;

    private long gold1Before;
    private long gold2Before;
    private List<Item> bag1Before;
    private List<Item> bag2Before;

    private List<Item> itemsBag1;
    private List<Item> itemsBag2;

    private List<Item> itemsTrade1;
    private List<Item> itemsTrade2;
    private int goldTrade1;
    private int goldTrade2;

    public byte accept;
    private boolean acc1;
    private boolean acc2;
    private volatile boolean finished;

    private long lastTimeStart;
    private boolean start;

    public Trade(Player pl1, Player pl2) {
        this.player1 = pl1;
        this.player2 = pl2;
        this.gold1Before = pl1.inventory.gold;
        this.gold2Before = pl2.inventory.gold;
        this.bag1Before = InventoryService.gI().copyItemsBag(player1);
        this.bag2Before = InventoryService.gI().copyItemsBag(player2);
        this.itemsBag1 = InventoryService.gI().copyItemsBag(player1);
        this.itemsBag2 = InventoryService.gI().copyItemsBag(player2);
        this.itemsTrade1 = new ArrayList<>();
        this.itemsTrade2 = new ArrayList<>();
        TransactionService.PLAYER_TRADE.put(pl1, this);
        TransactionService.PLAYER_TRADE.put(pl2, this);
        debug("TAO phien | vang truoc: P1=" + gold1Before + ", P2=" + gold2Before);
    }

    public void openTabTrade() {
        player1.idMark.setAcpTrade(true);
        player2.idMark.setAcpTrade(true);
        this.lastTimeStart = System.currentTimeMillis();
        this.start = true;
        debug("MO tab giao dich");
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(1);
            msg.writer().writeInt((int) player1.id);
            player2.sendMessage(msg);
            msg.cleanup();
            msg = new Message(-86);
            msg.writer().writeByte(1);
            msg.writer().writeInt((int) player2.id);
            player1.sendMessage(msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void addItemBot(Item it) {
        it.quantityGD = it.quantity; 
        itemsTrade2.add(it);
    }

    public synchronized void addItemTrade(Player pl, byte index, int quantity) {
        if (finished) {
            return;
        }
        if ((pl.equals(this.player1) && khoa1) || (pl.equals(this.player2) && khoa2)) {
            Service.gI().sendThongBao(pl, "Bạn đã khoá giao dịch, không thể thay đổi");
            return;
        }
        this.acc1 = false;
        this.acc2 = false;
        this.accept = 0;
//        if (!this.player1.getSession().actived || !this.player2.getSession().actived) {
//            Service.gI().sendThongBaoFromAdmin(pl, "|7|Bạn chưa mở thành viên hoặc người kia chưa mở");
//            removeItemTrade(pl, index);
//            return;
//        }
        if (index == -1) {
            if (quantity > VANG_TOI_DA_MOI_LAN) {
                Service.gI().sendThongBao(pl, "Mỗi lần giao dịch chỉ được tối đa "
                        + Util.numberToMoney(VANG_TOI_DA_MOI_LAN) + " vàng tươi");
                return;
            }
            if (quantity > pl.inventory.gold || quantity < 0) {
                Service.gI().sendThongBao(pl, "Bạn không có đủ vàng để giao dịch!");
                return;
            }
            if (pl.equals(this.player1)) {
                goldTrade1 = quantity;
            } else {
                goldTrade2 = quantity;
            }
            debug("DAT vang: " + pl.name + " = " + quantity);
            return;
        }

        Item item;
        if (pl.equals(this.player1)) {
            item = itemsBag1.get(index);
        } else {
            item = itemsBag2.get(index);
        }

        if (item == null || !item.isNotNullItem()) {
            Service.gI().sendThongBaoFromAdmin(pl, "|7|Số lượng hoặc vật phẩm không hợp lệ!");
            return;
        }

        if (isItemCannotTran(item)) {
            debug("TU CHOI do (khong the giao dich): " + (item.template != null ? item.template.name : "?"));
            if (item.isManhVoBongTai() && !item.hasOption(31)) {
                Service.gI().sendThongBaoFromAdmin(pl, "|7|Mảnh vỡ bông tai loại vật phẩm không thể giao dịch (chỉ cho phép loại có option 31)!");
            } else {
                Service.gI().sendThongBaoFromAdmin(pl, "|7|Vật phẩm này không thể giao dịch!");
            }
            removeItemTrade(pl, index);
            return;
        }

        if (item.isManhVoBongTai() && item.hasOption(31)) {
            item.syncManhVoBongTai();
            int optQuantity = item.getOptionParam(31);
            if (quantity > optQuantity || quantity <= 0) {
                Service.gI().sendThongBaoFromAdmin(pl, "|7|Số lượng hoặc vật phẩm không hợp lệ!");
                removeItemTradeOnly(pl, index);
                return;
            }

            debug("THEM do (ManhVoBongTai Opt31): " + pl.name + " -> " + (item.template != null ? item.template.name : "?") + " x" + quantity);

            Item itemTrade = ItemService.gI().copyItem(item);
            itemTrade.setOptionParam(31, quantity);
            itemTrade.quantity = 1;
            itemTrade.quantityGD = quantity;

            List<Item> bagCopy = pl.equals(this.player1) ? itemsBag1 : itemsBag2;
            List<Item> tradeList = pl.equals(this.player1) ? itemsTrade1 : itemsTrade2;

            int remain = optQuantity - quantity;
            if (remain <= 0) {
                bagCopy.set(index, ItemService.gI().createItemNull());
            } else {
                item.setOptionParam(31, remain);
                item.quantity = 1;
            }
            tradeList.add(itemTrade);
            return;
        }

        if (quantity > item.quantity || quantity <= 0) {
            Service.gI().sendThongBaoFromAdmin(pl, "|7|Số lượng hoặc vật phẩm không hợp lệ!");
            return;
        }

        debug("THEM do: " + pl.name + " -> " + (item.template != null ? item.template.name : "?") + " x" + quantity);

        if (quantity > 99999) {
            int n = quantity / 99999;
            int left = quantity % 99999;

            for (int i = 0; i < n; i++) {
                Item itemTrade = ItemService.gI().copyItem(item);
                itemTrade.quantity = 99999;
                itemTrade.quantityGD = 99999; // giữ SL trade cho log (vì .quantity sẽ bị addItemList zero hóa)
                if (pl.equals(this.player1)) {
                    InventoryService.gI().subQuantityItem(itemsBag1, item, itemTrade.quantity);
                    itemsTrade1.add(itemTrade);
                } else {
                    InventoryService.gI().subQuantityItem(itemsBag2, item, itemTrade.quantity);
                    itemsTrade2.add(itemTrade);
                }
            }

            if (left > 0) {
                Item itemTrade = ItemService.gI().copyItem(item);
                itemTrade.quantity = left;
                itemTrade.quantityGD = left; // giữ SL trade cho log (vì .quantity sẽ bị addItemList zero hóa)
                if (pl.equals(this.player1)) {
                    InventoryService.gI().subQuantityItem(itemsBag1, item, itemTrade.quantity);
                    itemsTrade1.add(itemTrade);
                } else {
                    InventoryService.gI().subQuantityItem(itemsBag2, item, itemTrade.quantity);
                    itemsTrade2.add(itemTrade);
                }
            }
        } else {
            Item itemTrade = ItemService.gI().copyItem(item);
            itemTrade.quantity = quantity;
            itemTrade.quantityGD = quantity; // giữ SL trade cho log (vì .quantity sẽ bị addItemList zero hóa)
            if (pl.equals(this.player1)) {
                InventoryService.gI().subQuantityItem(itemsBag1, item, itemTrade.quantity);
                itemsTrade1.add(itemTrade);
            } else {
                InventoryService.gI().subQuantityItem(itemsBag2, item, itemTrade.quantity);
                itemsTrade2.add(itemTrade);
            }
        }
    }

    public void removeItemTrade(Player pl, byte index) {
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(2);
            msg.writer().writeByte(index);
            pl.sendMessage(msg);
            Service.gI().sendThongBao(pl, "Không thể giao dịch vật phẩm này");
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void removeItemTradeOnly(Player pl, byte index) {
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(2);
            msg.writer().writeByte(index);
            pl.sendMessage(msg);
        } catch (Exception ignored) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public Item getItemBag(Player pl, byte index) {
        List<Item> bag = pl.equals(this.player1) ? itemsBag1 : itemsBag2;
        if (bag != null && index >= 0 && index < bag.size()) {
            return bag.get(index);
        }
        return null;
    }

    private void removeItemTrade2(Player pl, byte index) {
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(2);
            msg.writer().write(index);
            pl.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    private boolean isItemCannotTran(Item item) {
        if (item == null || item.template == null) {
            return true;
        }
        for (Item.ItemOption io : item.itemOptions) {
            if (io.optionTemplate.id == 30) {
                return true;
            }
        }
        // Mảnh vỡ bông tai: chỉ cho phép giao dịch loại có Option 31 (loại vật phẩm thường không có option 31 không cho giao dịch)
        if (item.isManhVoBongTai() && !item.hasOption(31)) {
            return true;
        }
        switch (item.template.id) {
            case 454:
            case 579:
            case 921:
            case 1492:
                return true;
        }
        switch (item.template.type) {
            case 27: //
                return item.template.id == 590;
            case 8: //vật phẩm nhiệm vụ
                return true;
            default:
                return false;
        }
    }

    public synchronized void cancelTrade() {
        if (finished) {
            return;
        }
        finished = true;
        debug("HUY giao dich");
        String notifiText = "Giao dịch bị hủy bỏ";
        Service.gI().sendThongBao(player1, notifiText);
        Service.gI().sendThongBao(player2, notifiText);
        closeTab();
        dispose();
    }

    private void closeTab() {
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(7);
            player1.sendMessage(msg);
            player2.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public synchronized void dispose() {
        if (player1 == null || player2 == null) {
            return;
        }
        player1.idMark.setPlayerTradeId(-1);
        player2.idMark.setPlayerTradeId(-1);
        player1.idMark.setIndexItemTrade(-1);
        player2.idMark.setIndexItemTrade(-1);
        TransactionService.PLAYER_TRADE.remove(player1);
        TransactionService.PLAYER_TRADE.remove(player2);
        this.player1 = null;
        this.player2 = null;
        this.itemsBag1 = null;
        this.itemsBag2 = null;
        this.itemsTrade1 = null;
        this.itemsTrade2 = null;
    }

    /** Mỗi bên đã khoá bàn chưa. Chưa khoá đủ hai bên thì không được đồng ý. */
    private boolean khoa1;
    private boolean khoa2;

    public synchronized void lockTran(Player pl) {
        if (finished) {
            return;
        }
        if (pl.equals(player1)) {
            khoa1 = true;
        } else if (pl.equals(player2)) {
            khoa2 = true;
        }
        debug("KHOA (lock) boi " + pl.name + " | vang dat=" + (pl.equals(player1) ? goldTrade1 : goldTrade2)
                + " | so mon=" + (pl.equals(player1) ? itemsTrade1.size() : itemsTrade2.size()));
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(6);
            if (pl.equals(player1)) {
                msg.writer().writeInt(goldTrade1);
                msg.writer().writeByte(itemsTrade1.size());
                for (Item item : itemsTrade1) {
                    msg.writer().writeShort(item.template.id);
                    int q = item.quantity;
                    if (item.isManhVoBongTai() && item.hasOption(31)) {
                        q = item.getOptionParam(31);
                    }
                    if (player2.getSession() != null && player2.getSession().version < 222) {
                        msg.writer().writeByte(q > Byte.MAX_VALUE ? Byte.MAX_VALUE : q);
                    } else {
                        msg.writer().writeInt(q);
                    }
                    msg.writer().writeByte(item.itemOptions.size());
                    for (Item.ItemOption io : item.itemOptions) {
                        msg.writer().writeByte(io.optionTemplate.id);
                        msg.writer().writeShort(io.param);
                    }
                }
                player2.sendMessage(msg);
            } else {
                msg.writer().writeInt(goldTrade2);
                msg.writer().writeByte(itemsTrade2.size());
                for (Item item : itemsTrade2) {
                    msg.writer().writeShort(item.template.id);
                    int q = item.quantity;
                    if (item.isManhVoBongTai() && item.hasOption(31)) {
                        q = item.getOptionParam(31);
                    }
                    if (player1.getSession() != null && player1.getSession().version < 222) {
                        msg.writer().writeByte(q > Byte.MAX_VALUE ? Byte.MAX_VALUE : q);
                    } else {
                        msg.writer().writeInt(q);
                    }
                    msg.writer().writeByte(item.itemOptions.size());
                    for (Item.ItemOption io : item.itemOptions) {
                        msg.writer().writeByte(io.optionTemplate.id);
                        msg.writer().writeShort(io.param);
                    }
                }
                player1.sendMessage(msg);
            }
        } catch (Exception e) {
            Logger.logException(Trade.class, e);
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public synchronized void acceptTrade(Player pl) {
        if (finished) {
            return;
        }
        // Phải khoá đủ HAI bên mới được đồng ý. Không có chốt này thì một bên có thể
        // đổi số vàng/đồ sau khi bên kia đã nhìn thấy bảng khoá rồi bấm đồng ý —
        // màn hình đối phương vẫn hiện số cũ, nhận về số khác hẳn.
        if (!khoa1 || !khoa2) {
            Service.gI().sendThongBao(pl, "Cả hai bên phải khoá giao dịch trước khi đồng ý");
            return;
        }
        // Đếm đồng ý theo TỪNG người: 1 nick spam ACCEPT không thể tự đẩy đủ 2/2.
        if (pl.equals(player1)) {
            this.acc1 = true;
        } else if (pl.equals(player2)) {
            this.acc2 = true;
        }
        this.accept = (byte) ((acc1 ? 1 : 0) + (acc2 ? 1 : 0));
        debug("DONG Y (" + accept + "/2)");
        if (this.acc1 && this.acc2) {
            this.startTrade();
        }
    }

    private void startTrade() {
        // Chốt cửa: chỉ thực thi đúng 1 lần, chặn race execute kép giữa 2 luồng.
        if (finished) {
            return;
        }
        finished = true;
        // Kiểm tra lại tại thời điểm thực thi: mỗi bên còn đủ vàng đã đặt.
        if (player1.inventory.gold < goldTrade1 || player2.inventory.gold < goldTrade2) {
            debug("THAT BAI: vang khong hop le luc thuc thi");
            Service.gI().sendThongBao(player1, "Giao dịch thất bại: số vàng không hợp lệ");
            Service.gI().sendThongBao(player2, "Giao dịch thất bại: số vàng không hợp lệ");
            closeTab();
            dispose();
            return;
        }
        byte tradeStatus = SUCCESS;
        debug("BAT DAU thuc thi | P1 dat: vang=" + goldTrade1 + ", do=[" + itemsToStr(itemsTrade1) + "]"
                + " | P2 dat: vang=" + goldTrade2 + ", do=[" + itemsToStr(itemsTrade2) + "]");
        if (player1.inventory.gold - goldTrade1 + goldTrade2 > Inventory.LIMIT_GOLD) {
            tradeStatus = FAIL_MAX_GOLD_PLAYER1;
        } else if (player2.inventory.gold - goldTrade2 + goldTrade1 > Inventory.LIMIT_GOLD) {
            tradeStatus = FAIL_MAX_GOLD_PLAYER2;
        }
        if (tradeStatus != SUCCESS) {
            sendNotifyTrade(tradeStatus);
            closeTab();
            dispose();
            return;
        }

        List<Item> tuiMoi1 = InventoryService.gI().copyItemsBag(player1);
        List<Item> tuiMoi2 = InventoryService.gI().copyItemsBag(player2);
        if (!truDoDaDat(tuiMoi1, itemsTrade1) || !truDoDaDat(tuiMoi2, itemsTrade2)) {
            debug("THAT BAI: do dat len khong con trong tui luc thuc thi");
            Service.gI().sendThongBao(player1, "Giao dịch thất bại: vật phẩm không còn hợp lệ");
            Service.gI().sendThongBao(player2, "Giao dịch thất bại: vật phẩm không còn hợp lệ");
            sendNotifyTrade(FAIL_NOT_ENOUGH_BAG_P2);
            closeTab();
            dispose();
            return;
        }
        for (Item item : itemsTrade1) {
            if (!InventoryService.gI().addItemList(tuiMoi2, item)) {
                tradeStatus = FAIL_NOT_ENOUGH_BAG_P2;
                break;
            }
        }
        if (tradeStatus != SUCCESS) {
            sendNotifyTrade(tradeStatus);
            closeTab();
            dispose();
            return;
        }
        for (Item item : itemsTrade2) {
            if (!InventoryService.gI().addItemList(tuiMoi1, item)) {
                tradeStatus = FAIL_NOT_ENOUGH_BAG_P1;
                break;
            }
        }
        if (tradeStatus != SUCCESS) {
            sendNotifyTrade(tradeStatus);
            closeTab();
            dispose();
            return;
        }

        player1.inventory.gold += goldTrade2;
        player2.inventory.gold += goldTrade1;
        player1.inventory.gold -= goldTrade1;
        player2.inventory.gold -= goldTrade2;
        player1.inventory.itemsBag = tuiMoi1;
        player2.inventory.itemsBag = tuiMoi2;

        InventoryService.gI().sendItemBags(player1);
        InventoryService.gI().sendItemBags(player2);
        PlayerService.gI().sendInfoHpMpMoney(player1);
        PlayerService.gI().sendInfoHpMpMoney(player2);

        try {
            HistoryTransactionDAO.insert(player1, player2, goldTrade1, goldTrade2, itemsTrade1, itemsTrade2,
                    bag1Before, bag2Before, this.player1.inventory.itemsBag, this.player2.inventory.itemsBag,
                    gold1Before, gold2Before, this.player1.inventory.gold, this.player2.inventory.gold);
        } catch (Exception e) {
            Logger.logException(Trade.class, e);
        }
        debug("THANH CONG | vang P1: " + gold1Before + "->" + player1.inventory.gold
                + " | vang P2: " + gold2Before + "->" + player2.inventory.gold
                + " | P1 nhan [" + itemsToStr(itemsTrade2) + "], P2 nhan [" + itemsToStr(itemsTrade1) + "]");

        sendNotifyTrade(tradeStatus);
        closeTab();
        dispose();
    }

    /**
     * Trừ đúng những món đã đặt lên bàn ra khỏi túi hiện tại.
     *
     * <p>Tìm theo mẫu vật phẩm + bộ chỉ số nên không phụ thuộc vị trí ô (người chơi có
     * thể đã sắp xếp lại túi). Thiếu dù chỉ một món là trả false để huỷ cả giao dịch —
     * thà không giao dịch còn hơn cho ra đồ từ hư không.
     */
    private boolean truDoDaDat(List<Item> tui, List<Item> daDat) {
        for (Item dat : daDat) {
            if (dat == null || dat.template == null) {
                continue;
            }
            if (dat.isManhVoBongTai() && dat.hasOption(31)) {
                int can = dat.getOptionParam(31) > 0 ? dat.getOptionParam(31) : (dat.quantityGD > 0 ? dat.quantityGD : dat.quantity);
                for (int i = 0; i < tui.size() && can > 0; i++) {
                    Item it = tui.get(i);
                    if (it == null || !it.isNotNullItem() || it.template.id != dat.template.id || !it.hasOption(31)) {
                        continue;
                    }
                    if (!InventoryService.checkOptionsEqualManhBongTai(it.itemOptions, dat.itemOptions)) {
                        continue;
                    }
                    it.syncManhVoBongTai();
                    int itOpt = it.getOptionParam(31);
                    int tru = Math.min(can, itOpt);
                    int remain = itOpt - tru;
                    if (remain <= 0) {
                        tui.set(i, ItemService.gI().createItemNull());
                    } else {
                        it.setOptionParam(31, remain);
                        it.quantity = 1;
                    }
                    can -= tru;
                }
                if (can > 0) {
                    return false; // đã dùng/bán/vứt mất trong lúc giao dịch
                }
            } else {
                int can = dat.quantityGD > 0 ? dat.quantityGD : dat.quantity;
                for (int i = 0; i < tui.size() && can > 0; i++) {
                    Item it = tui.get(i);
                    if (it == null || !it.isNotNullItem() || it.template.id != dat.template.id) {
                        continue;
                    }
                    if (it.isManhBongTai()) {
                        if (!InventoryService.checkOptionsEqualManhBongTai(it.itemOptions, dat.itemOptions)) {
                            continue;
                        }
                    } else if (!InventoryService.checkListsEqual(it.itemOptions, dat.itemOptions)) {
                        continue;
                    }
                    int tru = Math.min(can, it.quantity);
                    InventoryService.gI().subQuantityItem(tui, it, tru);
                    can -= tru;
                }
                if (can > 0) {
                    return false; // đã dùng/bán/vứt mất trong lúc giao dịch
                }
            }
        }
        return true;
    }

    private static final byte SUCCESS = 0;
    private static final byte FAIL_MAX_GOLD_PLAYER1 = 1;
    private static final byte FAIL_MAX_GOLD_PLAYER2 = 2;
    private static final byte FAIL_NOT_ENOUGH_BAG_P1 = 3;
    private static final byte FAIL_NOT_ENOUGH_BAG_P2 = 4;

    private void sendNotifyTrade(byte status) {
        if (status != SUCCESS) debug("THAT BAI: " + failName(status));
        player1.idMark.setLastTimeTrade(System.currentTimeMillis());
        player2.idMark.setLastTimeTrade(System.currentTimeMillis());
        switch (status) {
            case SUCCESS:
                Service.gI().sendThongBao(player1, "Giao dịch thành công");
                Service.gI().sendThongBao(player2, "Giao dịch thành công");
                break;
            case FAIL_MAX_GOLD_PLAYER1:
                Service.gI().sendThongBao(player1, "Giao dịch thất bại do số lượng vàng sau giao dịch vượt tối đa");
                Service.gI().sendThongBao(player2, "Giao dịch thất bại do số lượng vàng " + player1.name + " sau giao dịch vượt tối đa");
                break;
            case FAIL_MAX_GOLD_PLAYER2:
                Service.gI().sendThongBao(player2, "Giao dịch thất bại do số lượng vàng sau giao dịch vượt tối đa");
                Service.gI().sendThongBao(player1, "Giao dịch thất bại do số lượng vàng " + player2.name + " sau giao dịch vượt tối đa");
                break;
            case FAIL_NOT_ENOUGH_BAG_P1:
                Service.gI().sendThongBao(player1, "Giao dịch thất bại vì " + player1.name + " không đủ chỗ chứa");
                Service.gI().sendThongBao(player2, "Giao dịch thất bại vì " + player1.name + " không đủ chỗ chứa");
                break;
            case FAIL_NOT_ENOUGH_BAG_P2:
                Service.gI().sendThongBao(player1, "Giao dịch thất bại vì " + player2.name + " không đủ chỗ chứa");
                Service.gI().sendThongBao(player2, "Giao dịch thất bại vì " + player2.name + " không đủ chỗ chứa");
                break;
        }
    }

    public void update() {
        if (this.start && Util.canDoWithTime(lastTimeStart, TIME_TRADE)) {
            debug("HET GIO -> tu dong huy");
            this.cancelTrade();
        }
    }

    // ------------------------------------------------------------------
    //  DEBUG giao dịch — bật/tắt bằng Trade.DEBUG
    // ------------------------------------------------------------------
    private String tradeTag() {
        String n1 = player1 != null ? player1.name + "(" + player1.id + ")" : "?";
        String n2 = player2 != null ? player2.name + "(" + player2.id + ")" : "?";
        return n1 + " <-> " + n2;
    }

    private void debug(String msg) {
        if (!DEBUG) return;
        Logger.primaryln("[GIAODICH] " + tradeTag() + " | " + msg);
    }

    private static String itemsToStr(List<Item> items) {
        if (items == null || items.isEmpty()) return "khong co";
        StringBuilder sb = new StringBuilder();
        for (Item it : items) {
            if (it == null) continue;
            if (sb.length() > 0) sb.append(", ");
            String nm = (it.template != null && it.template.name != null) ? it.template.name : "?";
            // Dùng quantityGD: addItemList zero hoá .quantity sau khi chuyển xong, in
            // .quantity ra chỉ toàn "x0" nên log không dùng được để đối chiếu.
            int sl = it.quantityGD > 0 ? it.quantityGD : it.quantity;
            sb.append(nm).append(" x").append(sl);
        }
        return sb.length() == 0 ? "khong co" : sb.toString();
    }

    private static String failName(byte s) {
        switch (s) {
            case FAIL_MAX_GOLD_PLAYER1: return "vang P1 vuot toi da";
            case FAIL_MAX_GOLD_PLAYER2: return "vang P2 vuot toi da";
            case FAIL_NOT_ENOUGH_BAG_P1: return "P1 khong du cho chua";
            case FAIL_NOT_ENOUGH_BAG_P2: return "P2 khong du cho chua";
            default: return "khac (" + s + ")";
        }
    }
}
