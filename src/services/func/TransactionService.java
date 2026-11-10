package services.func;


import database.AlyraManager;
import database.daos.PlayerDAO;
import models.item.Item;
import models.player.Player;
import network.io.Message;
import server.Client;
import server.Maintenance;
import services.Service;
import utils.Logger;
import utils.TimeUtil;
import utils.Util;

import java.sql.Connection;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class TransactionService implements Runnable {

    private static final int TIME_DELAY_TRADE = 30000;
    private static final int TIME_INVITE_TRADE = 30000;
    static final Map<Player, Trade> PLAYER_TRADE = new ConcurrentHashMap<>();

    private static final byte SEND_INVITE_TRADE = 0;
    private static final byte ACCEPT_TRADE = 1;
    private static final byte ADD_ITEM_TRADE = 2;
    private static final byte CANCEL_TRADE = 3;
    private static final byte LOCK_TRADE = 5;
    private static final byte ACCEPT = 7;

    /** Khoá chung cho bước kiểm-tra-rồi-tạo phiên giao dịch. */
    private static final Object KHOA_TAO_PHIEN = new Object();

    private static TransactionService i;

    private TransactionService() {
    }

    public static synchronized TransactionService gI() {
        if (i == null) {
            i = new TransactionService();
            new Thread(i).start();
        }
        return i;
    }

    public void controller(Player pl, Message msg) {
        try {
            byte action = msg.reader().readByte();
            int playerId = -1;
            Player plMap = null;
            Trade trade = PLAYER_TRADE.get(pl);
            // Cho phép huỷ kể cả khi quyền thành viên/bảo vệ vừa thay đổi.
            if (action == CANCEL_TRADE) {
                cancelTrade(pl);
                return;
            }
            if (!canTrade(pl)) {
                if (trade != null) trade.cancelTrade();
                Service.gI().sendThongBao(pl, "Bạn cần mở thành viên và đăng nhập hợp lệ để giao dịch");
                return;
            }
            if (pl.baovetaikhoan) {
                if (trade != null) trade.cancelTrade();
                Service.gI().sendThongBao(pl, "Chức năng bảo vệ đã được bật. Bạn vui lòng kiểm tra lại");
                return;
            }
//            if (pl.nPoint.power < 1_500_000_000) {
//                Service.gI().sendThongBao(pl, "Yêu cầu sức mạnh trên 1Tỉ5");
//                return;
//            }
            if (action == SEND_INVITE_TRADE) {
                pl.idMark.setTransactionWP(false);
                pl.idMark.setTransactionWVP(false);
            }
            switch (action) {
                case SEND_INVITE_TRADE:
               case ACCEPT_TRADE:
                    playerId = msg.reader().readInt();
                    if (pl.zone == null) return;
                    plMap = pl.zone.getPlayerInMap(playerId);
                    if (plMap != null && plMap.isPl()) {
                        if (plMap == pl || plMap.id == pl.id || !canTrade(plMap)
                                || plMap.getSession().userId == pl.getSession().userId
                                || plMap.baovetaikhoan || Maintenance.isRunning) {
                            Service.gI().sendThongBao(pl, "Đối phương chưa mở thành viên hoặc không thể giao dịch");
                            return;
                        }
                        if (plMap.tradeWVP) {
                            return;
                        }
                        trade = PLAYER_TRADE.get(pl);
                        if (trade == null) {
                            trade = PLAYER_TRADE.get(plMap);
                        }
                        if (trade == null) {
                            if (action == SEND_INVITE_TRADE) {
                                if (Util.canDoWithTime(pl.idMark.getLastTimeTrade(), TIME_DELAY_TRADE)
                                        && Util.canDoWithTime(plMap.idMark.getLastTimeTrade(), TIME_DELAY_TRADE)) {
                                    boolean checkLogout1 = false;
                                    boolean checkLogout2 = false;
                                    try (Connection con = AlyraManager.getConnection()) {
                                        checkLogout1 = PlayerDAO.checkLogoutForTrade(con, pl);
                                        checkLogout2 = PlayerDAO.checkLogoutForTrade(con, plMap);
                                    } catch (Exception e) {
                                        Logger.logException(TransactionService.class, e);
                                        Service.gI().sendThongBao(pl, "Không thể kiểm tra tài khoản, vui lòng thử lại sau");
                                        return;
                                    }
                                    if (checkLogout1) {
                                        Client.gI().kickSession(pl.getSession());
                                        break;
                                    }
                                    if (checkLogout2) {
                                        Client.gI().kickSession(plMap.getSession());
                                        break;
                                    }
                                    synchronized (KHOA_TAO_PHIEN) {
                                        if (PLAYER_TRADE.containsKey(pl) || PLAYER_TRADE.containsKey(plMap)
                                                || !canTrade(pl) || !canTrade(plMap)
                                                || !Util.canDoWithTime(pl.idMark.getLastTimeTrade(), TIME_DELAY_TRADE)
                                                || !Util.canDoWithTime(plMap.idMark.getLastTimeTrade(), TIME_DELAY_TRADE)) return;
                                        pl.idMark.setLastTimeTrade(System.currentTimeMillis());
                                        pl.idMark.setPlayerTradeId((int) plMap.id);
                                        sendInviteTrade(pl, plMap);
                                    }
                                } else {
                                    Service.gI().sendThongBao(pl, "Thử lại sau "
                                            + TimeUtil.getTimeLeft(Math.max(pl.idMark.getLastTimeTrade(), plMap.idMark.getLastTimeTrade()), TIME_DELAY_TRADE / 1000));
                                }
                            } else {
                                if (plMap.idMark.getPlayerTradeId() == pl.id
                                        && !Util.canDoWithTime(plMap.idMark.getLastTimeTrade(), TIME_INVITE_TRADE)) {
                                    // Khoá chung khi tạo phiên: hai người mời chéo rồi
                                    // cùng bấm nhận sẽ tạo HAI phiên, map trỏ chéo nhau
                                    // (A->T2, B->T1) nên không phiên nào đủ 2/2 -> cả hai
                                    // kẹt bàn tới khi hết giờ 3 phút.
                                    synchronized (KHOA_TAO_PHIEN) {
                                        if (PLAYER_TRADE.get(pl) == null && PLAYER_TRADE.get(plMap) == null
                                                && canTrade(pl) && canTrade(plMap)
                                                && pl.zone == plMap.zone
                                                && plMap.idMark.getPlayerTradeId() == pl.id
                                                && !Util.canDoWithTime(plMap.idMark.getLastTimeTrade(), TIME_INVITE_TRADE)) {
                                            trade = new Trade(pl, plMap);
                                            pl.idMark.setPlayerTradeId(-1);
                                            plMap.idMark.setPlayerTradeId(-1);
                                            trade.openTabTrade();
                                        }
                                    }
                                }
                            }

                        } else {
                            Service.gI().sendThongBao(pl, "Không thể thực hiện");
                        }
                    }
                    break;
                case ADD_ITEM_TRADE:
                    if (trade != null) {
                        byte index = msg.reader().readByte();
                        int quantity = msg.reader().readInt();
                        if (quantity < 0) {
                            Service.gI().sendThongBao(pl, "Không thể thực hiện");
                            trade.cancelTrade();
                            break;
                        }
                        // Chỉ ép về 1 cho ĐỒ. Nhánh vàng (index == -1) phải cho đặt 0
                        // để người chơi rút lại số vàng đã chào.
                        if (index != -1 && quantity == 0) {
                            quantity = 1;
                        }
                        if (index != -1 && quantity > Trade.QUANLITY_MAX) {
                            Service.gI().sendThongBao(pl, "Đã quá giới hạn giao dịch...");
                            trade.cancelTrade();
                            break;
                        }
                        // Chặn biên ô túi: client sửa đổi gửi index rác (-2, 99...) sẽ
                        // ném IndexOutOfBounds mỗi gói, vừa ngập log vừa âm thầm xoá
                        // trạng thái "đã đồng ý" của đối phương.
                        if (index != -1 && (index < 0 || index >= pl.inventory.itemsBag.size())) {
                            Service.gI().sendThongBao(pl, "Không thể thực hiện");
                            trade.cancelTrade();
                            break;
                        }
                        trade.addItemTrade(pl, index, quantity);
                    }
                    break;
                case CANCEL_TRADE:
                    if (trade != null) {
                        trade.cancelTrade();
                    }
                    break;
                case LOCK_TRADE:
                    // Kiểm tra null TRƯỚC: bản cũ gọi trade.cancelTrade() ngay khi bảo
                    // trì, người chưa vào giao dịch nào mà gửi gói này là NPE.
                    if (trade != null) {
                        if (Maintenance.isRunning) {
                            trade.cancelTrade();
                            break;
                        }
                        trade.lockTran(pl);
                    }
                    break;
                case ACCEPT:
                    if (trade != null && Maintenance.isRunning) {
                        trade.cancelTrade();
                        break;
                    }
                    if (trade != null) {
                        trade.acceptTrade(pl);
                        if (trade.accept == 1) {
                            Service.gI().sendThongBao(pl, "Xin chờ đối phương đồng ý");
                        }
                    }
                    break;
            }
        } catch (Exception e) {
            Logger.logException(this.getClass(), e);
            cancelTrade(pl);
        }
    }

    /** Chỉ phiên online chính thức, đã mở thành viên mới được giao dịch. */
    static boolean canTrade(Player player) {
        return player != null && player.isPl() && !player.isBot
                && !player.disposed && !player.beforeDispose
                && player.inventory != null && player.idMark != null
                && player.getSession() != null && player.getSession().actived
                && player.getSession().isConnected() && player.getSession().player == player
                && Client.gI().getPlayer(player.id) == player
                && Client.gI().getPlayerByUser(player.getSession().userId) == player;
    }

    static boolean accountsReady(Player first, Player second) {
        if (!canTrade(first) || !canTrade(second)) return false;
        try (Connection con = AlyraManager.getConnection()) {
            return !PlayerDAO.checkLogoutForTrade(con, first)
                    && !PlayerDAO.checkLogoutForTrade(con, second);
        } catch (Exception e) {
            Logger.logException(TransactionService.class, e);
            return false;
        }
    }

    /**
     * Mời giao dịch
     */
    private void sendInviteTrade(Player plInvite, Player plReceive) {
        Message msg = null;
        try {
            msg = new Message(-86);
            msg.writer().writeByte(0);
            msg.writer().writeInt((int) plInvite.id);
            plReceive.sendMessage(msg);
        } catch (Exception e) {
        } finally {
            if (msg != null) {
                msg.cleanup();
                msg = null;
            }
        }
    }

    /**
     * Hủy giao dịch
     *
     * @param player
     */
    public void cancelTrade(Player player) {
        Trade trade = PLAYER_TRADE.get(player);
        if (trade != null) {
            trade.cancelTrade();
        }
    }

    public boolean check(Player player) {
        return PLAYER_TRADE.get(player) != null;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long st = System.currentTimeMillis();
                Set<Map.Entry<Player, Trade>> entrySet = PLAYER_TRADE.entrySet();
                for (Map.Entry entry : entrySet) {
                    ((Trade) entry.getValue()).update();
                }
                Thread.sleep(Math.max(10, 300 - (System.currentTimeMillis() - st)));
            } catch (Exception e) {
            }
        }
    }
}
