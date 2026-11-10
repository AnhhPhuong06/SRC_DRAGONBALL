package services.func;

import consts.ConstItem;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.player.Inventory;
import models.player.Player;
import network.io.Message;
import services.ItemService;
import services.RewardService;
import services.Service;
import services.VongQuayThoiVangConfig;
import services.player.InventoryService;

public class LuckyRound {

    /** Rương phụ đúng 100 ô: mỗi lượt quay ra 1 món nên quay xong không bao giờ vượt số này. */
    private static final int MAX_ITEM_IN_BOX = Inventory.MAX_ITEMS_BOX_LUCKY_ROUND;

    /** Kiểu client hiểu để vẽ giá theo vật phẩm. Cũng là kiểu của vòng quay đặc biệt. */
    public static final byte USING_TICKET = 1;
    /** Chỉ dùng phía server để phân luồng, không gửi ra client. */
    public static final byte USING_THOI_VANG = 3;

    private static final int TICKET = 821;
    private static final int PRICE_TICKET = 1;

    /** Số lượt của nút quay nhanh. Không vượt quá MAX_ITEM_IN_BOX vì mỗi lượt ra 1 món. */
    public static final byte SO_LAN_QUAY_NHANH = 100;
    private static final int[] ICON_VONG_QUAY_VIP = {10197, 8935, 7711, 14831, 425, 1416, 929};
    private static final int[] ICON_VONG_QUAY_THUONG = {419, 420, 421, 422, 423, 424, 425};

    private static LuckyRound instance;

    public static LuckyRound gI() {
        if (instance == null) {
            instance = new LuckyRound();
        }
        return instance;
    }

    public void openCrackBallVipUI(Player pl) {
        if (pl.inventory.itemsBoxCrackBall.size() >= MAX_ITEM_IN_BOX) {
            Service.gI().sendThongBao(pl, "Rương phụ đã đầy (" + MAX_ITEM_IN_BOX + " món), hãy nhận bớt hoặc xóa vật phẩm trước khi quay!");
            return;
        }
        openBallUI(pl, USING_TICKET, ICON_VONG_QUAY_VIP, PRICE_TICKET, TICKET);
    }

    public void openCrackBallThoiVangUI(Player pl) {
        int[] spinPrice = VongQuayThoiVangConfig.spinPrice();
        int itemId = spinPrice[0];
        int price = spinPrice[1];
        if (demTrongTui(pl, itemId) < price) {
            Service.gI().sendThongBao(pl, "Bạn không có đủ vé để sử dụng!");
            return;
        }
        if (pl.inventory.itemsBoxCrackBall.size() >= MAX_ITEM_IN_BOX) {
            Service.gI().sendThongBao(pl, "Rương phụ đã đầy (" + MAX_ITEM_IN_BOX + " món), hãy nhận bớt hoặc xóa vật phẩm trước khi quay!");
            return;
        }
        openBallUI(pl, USING_THOI_VANG, ICON_VONG_QUAY_THUONG, price, itemId);
    }

    /**
     * Quay nhanh bằng vé quay (ID 821) ngay từ menu NPC. Không mở giao diện vòng quay nên bỏ
     * hiệu ứng quay, chỉ trừ nguyên liệu rồi dồn thưởng vào rương phụ và báo bằng chữ.
     */
    public void quayNhanhThoiVang(Player player) {
        synchronized (VongQuayThoiVangConfig.class) {
            quayNhanhThoiVangWithConfig(player);
        }
    }

    private void quayNhanhThoiVangWithConfig(Player player) {
        int[] spinPrice = VongQuayThoiVangConfig.spinPrice();
        int itemId = spinPrice[0];
        int price = spinPrice[1];
        if (demTrongTui(player, itemId) < price) {
            Service.gI().sendThongBao(player, "Bạn không có đủ vé để sử dụng!");
            return;
        }
        if (player.inventory.itemsBoxCrackBall.size() >= MAX_ITEM_IN_BOX) {
            Service.gI().sendThongBao(player, "Rương phụ đã đầy (" + MAX_ITEM_IN_BOX + " món), hãy nhận bớt hoặc xóa vật phẩm trước khi quay tiếp!");
            return;
        }
        int oTrong = Math.max(0, MAX_ITEM_IN_BOX - player.inventory.itemsBoxCrackBall.size());
        if (SO_LAN_QUAY_NHANH > oTrong) {
            Service.gI().sendThongBao(player, "Rương phụ chỉ còn " + oTrong + " ô trống, không đủ để quay nhanh " + SO_LAN_QUAY_NHANH + " lượt!");
            return;
        }
        openBallByItem(player, SO_LAN_QUAY_NHANH, itemId, price, false, false);
    }

    /**
     * Client chỉ hiểu 3 kiểu giá 0/1/2 nên luôn báo nó USING_TICKET để vẽ giá theo vật
     * phẩm; server tự nhớ kiểu riêng trong idMark, và readOpenBall vốn bỏ qua byte type
     * do client gửi lên nên hai bên không lẫn nhau.
     */
    private void openBallUI(Player pl, byte kieuServer, int[] iconVongQuay, int price, int itemId) {
        pl.idMark.setTypeLuckyRound(kieuServer);
        Message msg = null;
        try {
            msg = new Message(-127);
            msg.writer().writeByte(0);
            msg.writer().writeByte(iconVongQuay.length);
            for (int icon : iconVongQuay) {
                msg.writer().writeShort(icon);
            }
            msg.writer().writeByte(USING_TICKET); //kiểu hiển thị: trả bằng vật phẩm
            msg.writer().writeInt(price);
            msg.writer().writeShort(itemId);
            pl.sendMessage(msg);
        } catch (IOException e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }

    public void readOpenBall(Player player, Message msg) {
        try {
            msg.reader().readByte(); //type, không tin client - dùng kiểu lưu trong idMark
            byte count = msg.reader().readByte();
            // count là byte có dấu: số âm sẽ biến phép trừ vật phẩm thành phép cộng
            if (count <= 0) {
                return;
            }
            switch (player.idMark.getTypeLuckyRound()) {
                case USING_TICKET:
                    openBallByItem(player, count, TICKET, PRICE_TICKET, true, true);
                    break;
                case USING_THOI_VANG:
                    synchronized (VongQuayThoiVangConfig.class) {
                        int[] spinPrice = VongQuayThoiVangConfig.spinPrice();
                        openBallByItem(player, count, spinPrice[0], spinPrice[1], false, true);
                    }
                    break;
            }
        } catch (Exception e) {
            switch (player.idMark.getTypeLuckyRound()) {
                case USING_TICKET:
                    openCrackBallVipUI(player);
                    break;
                case USING_THOI_VANG:
                    openCrackBallThoiVangUI(player);
                    break;
                default:
                    break; //chưa mở vòng quay nào, không có giao diện để mở lại
            }
        }
    }

    /**
     * Quay bằng vật phẩm trong hành trang. Các chế độ chỉ khác nhau ở vật phẩm trả giá
     * và bảng thưởng nên dùng chung một hàm.
     *
     * @param quaGiaoDien true khi quay từ giao diện vòng quay - phải gửi gói phần thưởng
     * kể cả khi hỏng, không gửi thì vòng quay bên client quay mãi. false khi quay nhanh
     * từ menu NPC, lúc đó không có giao diện nào đang mở nên báo bằng chữ.
     */
    private void openBallByItem(Player player, byte count, int itemId, int price,
            boolean bangDacBiet, boolean quaGiaoDien) {
        // Client có thể gửi tới 127 lượt trong 1 gói, mà mỗi lượt chiếm 1 ô rương phụ nên
        // chặn ngay ở đây: không lượt quay nào được vượt sức chứa của rương.
        if (count > MAX_ITEM_IN_BOX) {
            Service.gI().sendThongBao(player, "Mỗi lần chỉ được quay tối đa "
                    + MAX_ITEM_IN_BOX + " lượt");
            if (quaGiaoDien) {
                sendReward(player, new ArrayList<>());
            }
            return;
        }
        int canDung = count * price;
        int co = demTrongTui(player, itemId);
        if (co < canDung) {
            if (co < price) {
                Service.gI().sendThongBao(player, "Bạn không có đủ vé để sử dụng!");
            } else {
                models.Template.ItemTemplate temp = ItemService.gI().getTemplate(itemId);
                String itemName = (temp != null && temp.name != null) ? temp.name : "Vé quay (ID " + itemId + ")";
                Service.gI().sendThongBao(player, "Bạn không đủ "
                        + itemName + " để quay " + count + " lượt (hiện có " + co + " vé)");
            }
            if (quaGiaoDien) {
                sendReward(player, new ArrayList<>());
            }
            return;
        }
        if (player.inventory.itemsBoxCrackBall.size() >= MAX_ITEM_IN_BOX) {
            Service.gI().sendThongBao(player, "Rương phụ đã đầy (" + MAX_ITEM_IN_BOX + " món), không thể quay tiếp!");
            if (quaGiaoDien) {
                sendReward(player, new ArrayList<>());
            }
            return;
        }
        int oTrong = Math.max(0, MAX_ITEM_IN_BOX - player.inventory.itemsBoxCrackBall.size());
        if (count > oTrong) {
            Service.gI().sendThongBao(player, "Rương phụ chỉ còn " + oTrong + " ô trống (tối đa " + MAX_ITEM_IN_BOX
                    + " món), không đủ cho " + count + " lượt quay!");
            if (quaGiaoDien) {
                sendReward(player, new ArrayList<>());
            }
            return;
        }

        truVatPham(player, itemId, canDung);
        InventoryService.gI().sendItemBags(player);

        player.lixi += count;
        List<Item> phanThuong = bangDacBiet
                ? RewardService.gI().getListItemLuckyRound(count)
                : RewardService.gI().getListItemVongQuayThoiVang(player, count);
        for (Item item : phanThuong) {
            if (item != null && item.isNotNullItem() && !item.isManhTS() && !item.isHaveOption(30)) {
                item.itemOptions.add(new Item.ItemOption(30, 0));
            }
        }
        player.inventory.itemsBoxCrackBall.addAll(phanThuong);
        if (quaGiaoDien) {
            sendReward(player, phanThuong);
        } else {
            Service.gI().sendThongBao(player, "Đã quay " + count + " lượt, "
                    + phanThuong.size() + " vật phẩm đã vào rương phụ");
        }
    }

    /**
     * Tổng của mọi stack cùng loại trong hành trang. findItemBag chỉ trả stack đầu tiên
     * nên không dùng được: người chơi có thể giữ nhiều stack thỏi vàng rời nhau, stack
     * đầu ít hơn giá quay là bị báo thiếu dù tổng thừa sức.
     */
    public int demTrongTui(Player player, int tempId) {
        int tong = 0;
        for (Item item : player.inventory.itemsBag) {
            if (item != null && item.isNotNullItem() && item.template.id == tempId) {
                tong += item.quantity;
            }
        }
        return tong;
    }

    /**
     * Trừ trải qua nhiều stack. Mỗi stack chỉ bị trừ tối đa đúng số nó đang có nhờ
     * Math.min nên không stack nào âm được; tổng đã kiểm đủ trước khi gọi nên trừ vừa
     * đúng số cần, không thừa không thiếu. Gọi sau khi đã chắc chắn demTrongTui >= can.
     */
    private void truVatPham(Player player, int tempId, int can) {
        // subQuantityItemsBag chỉ set ô thành item rỗng chứ không xoá phần tử nên
        // duyệt thẳng danh sách gốc vẫn an toàn.
        for (Item item : player.inventory.itemsBag) {
            if (can <= 0) {
                break;
            }
            if (item == null || !item.isNotNullItem() || item.template.id != tempId) {
                continue;
            }
            int tru = Math.min(can, item.quantity);
            if (tru <= 0) {
                continue;
            }
            InventoryService.gI().subQuantityItemsBag(player, item, tru);
            can -= tru;
        }
    }

    private void sendReward(Player player, List<Item> items) {
        Message msg = null;
        try {
            msg = new Message(-127);
            msg.writer().writeByte(1);
            msg.writer().writeByte(items.size());
            for (Item item : items) {
                msg.writer().writeShort(item.template.iconID);
            }
            player.sendMessage(msg);
        } catch (IOException e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
            }
        }
    }
}
