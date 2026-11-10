package database.daos;


import database.AlyraManager;
import models.item.Item;
import models.player.Player;
import utils.TimeUtil;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class HistoryTransactionDAO {

    public static void insert(Player pl1, Player pl2,
            //            int goldP1, int goldP2, List<Item> itemP1, List<Item> itemP2,
            //            List<Item> bag1Before, List<Item> bag2Before,
            //            List<Item> bag1After,
            //            List<Item> bag2After,
            //            long gold1Before, long gold2Before, long gold1After, long gold2After) {
            //        
            //        gold1Before = pl1.inventory.gold + goldP1 - goldP2;
            //        gold2Before = pl2.inventory.gold - goldP1 + goldP2;
            //
            //        gold1After = pl1.inventory.gold = goldP1 + goldP2;
            //        gold2After = pl2.inventory.gold - goldP2 + goldP1;
            //        
            //        
            //        String detailGold1 = "Trước khi giao dịch: " + gold1Before + " | Sau khi giao dịch: " + gold1After;
            //                String detailGold2 = "Trước khi giao dịch: " + gold2Before + " | Sau khi giao dịch: " + gold2After;
            //
            //        String player1 = pl1.name + " (" + pl1.id + ")";
            //        String player2 = pl2.name + " (" + pl2.id + ")";
            //        String itemPlayer1 = "Gold: " + goldP1 + ", ";
            //        String itemPlayer2 = "Gold: " + goldP2 + ", ";
            //        List<Item> doGD1 = new ArrayList<>();
            //        List<Item> doGD2 = new ArrayList<>();
            int goldP1, int goldP2, List<Item> itemP1, List<Item> itemP2,
            List<Item> bag1Before, List<Item> bag2Before,
            List<Item> bag1After,
            List<Item> bag2After,
            long gold1Before, long gold2Before, long gold1After, long gold2After) {

        try {
            if (pl1 == null || pl2 == null) {
                return;
            }
            String player1 = pl1.name + " (" + pl1.id + ")";
            String player2 = pl2.name + " (" + pl2.id + ")";
            String itemPlayer1 = "Gold: " + goldP1 + ", ";
            String itemPlayer2 = "Gold: " + goldP2 + ", ";
            List<Item> doGD1 = new ArrayList<>();
            List<Item> doGD2 = new ArrayList<>();
            if (itemP1 != null) {
                for (Item item : itemP1) {
                    if (item != null && item.isNotNullItem()) {
                        if (doGD1.stream().noneMatch(item1 -> item1 != null && item1.isNotNullItem() && item1.template.id == item.template.id)) {
                            doGD1.add(item);
                        } else {
                            doGD1.stream().filter(item1 -> item1 != null && item1.isNotNullItem() && item1.template.id == item.template.id)
                                    .findFirst().ifPresent(it -> it.quantityGD += item.quantityGD);
                        }
                    }
                }
            }
            if (itemP2 != null) {
                for (Item item : itemP2) {
                    if (item != null && item.isNotNullItem()) {
                        if (doGD2.stream().noneMatch(item1 -> item1 != null && item1.isNotNullItem() && item1.template.id == item.template.id)) {
                            doGD2.add(item);
                        } else {
                            doGD2.stream().filter(item1 -> item1 != null && item1.isNotNullItem() && item1.template.id == item.template.id)
                                    .findFirst().ifPresent(it -> it.quantityGD += item.quantityGD);
                        }
                    }
                }
            }

            for (Item item : doGD1) {
                if (item != null && item.isNotNullItem()) {
                    itemPlayer1 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantityGD + "),";
                }
            }
            for (Item item : doGD2) {
                if (item != null && item.isNotNullItem()) {
                    itemPlayer2 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantityGD + "),";
                }
            }
            String beforeTran1 = "";
            String beforeTran2 = "";
            if (bag1Before != null) {
                for (Item item : bag1Before) {
                    if (item != null && item.isNotNullItem()) {
                        beforeTran1 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantity + "),";
                    }
                }
            }
            if (bag2Before != null) {
                for (Item item : bag2Before) {
                    if (item != null && item.isNotNullItem()) {
                        beforeTran2 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantity + "),";
                    }
                }
            }
            String afterTran1 = "";
            String afterTran2 = "";
            if (bag1After != null) {
                for (Item item : bag1After) {
                    if (item != null && item.isNotNullItem()) {
                        afterTran1 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantity + "),";
                    }
                }
            }
            if (bag2After != null) {
                for (Item item : bag2After) {
                    if (item != null && item.isNotNullItem()) {
                        afterTran2 += (item.template.name != null ? item.template.name : "Item " + item.template.id) + " (x" + item.quantity + "),";
                    }
                }
            }
            // Đẩy ghi file + insert DB sang thread nền (fire-and-forget) để luồng giao dịch không phải chờ I/O.
            // Chuỗi text đã dựng đồng bộ ở trên (đọc đúng túi lúc đó); timestamp chốt ngay tại thời điểm giao dịch.
            final long fId1 = pl1.id, fId2 = pl2.id;
            final String fName1 = pl1.name, fName2 = pl2.name;
            final String fPlayer1 = player1, fPlayer2 = player2;
            final String fItem1 = itemPlayer1, fItem2 = itemPlayer2;
            final String fBefore1 = beforeTran1, fBefore2 = beforeTran2;
            final String fAfter1 = afterTran1, fAfter2 = afterTran2;
            final Timestamp fTime = new Timestamp(System.currentTimeMillis());
            new Thread(() -> {
                // Ghi file lịch sử trong thư mục lichsu/
                try {
                    lichsu.LichSuGiaoDich.luu(fId1, fId2, fName1, fName2,
                            fPlayer1, fPlayer2, fItem1, fItem2,
                            fBefore1, fBefore2, fAfter1, fAfter2);
                } catch (Exception ignored) {
                }
                // Ghi DB history_transaction
                try {
                    AlyraManager.executeUpdate("INSERT INTO history_transaction (player_1, player_2, item_player_1, item_player_2, bag_1_before_tran, bag_2_before_tran, bag_1_after_tran, bag_2_after_tran, time_tran) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)", fPlayer1, fPlayer2, fItem1, fItem2, fBefore1, fBefore2, fAfter1, fAfter2, fTime);
                } catch (Exception ex) {
                }
            }, "LuuLichSuGiaoDich").start();
        } catch (Exception e) {
            utils.Logger.logException(HistoryTransactionDAO.class, e);
        }
    }

    public static void deleteHistory() {
        PreparedStatement ps = null;
        try (Connection con = AlyraManager.getConnection();) {
            ps = con.prepareStatement("delete from history_transaction where time_tran < '"
                    + TimeUtil.getTimeBeforeCurrent(3 * 24 * 60 * 60 * 1000, "yyyy-MM-dd") + "'");
            ps.executeUpdate();
            ps.close();
        } catch (Exception e) {
        } finally {
            try {
                ps.close();
            } catch (SQLException ex) {
            }
        }
    }

}
