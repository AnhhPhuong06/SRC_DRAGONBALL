package database;

import java.sql.*;
import java.util.List;
import models.ConsignItem;
import org.json.simple.JSONValue;

/** Replace the consign snapshot without TRUNCATE's implicit transaction commit. */
public final class ConsignStore {
    private ConsignStore() {}
    public static void save(Connection connection, List<ConsignItem> items) throws SQLException {
        if (!connection.getAutoCommit()) throw new SQLException("Consign snapshot needs its own transaction");
        connection.setAutoCommit(false);
        boolean ended = false;
        try {
            try (PreparedStatement delete = connection.prepareStatement("DELETE FROM shop_ky_gui")) {
                delete.executeUpdate();
            }
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO shop_ky_gui (id,player_id,tab,item_id,gold,ruby,quantity,itemOption,lasttime,isBuy) VALUES (?,?,?,?,?,?,?,?,?,?)")) {
                for (ConsignItem item : items) {
                    if (item == null) continue;
                    insert.setInt(1, item.id); insert.setLong(2, item.player_sell);
                    insert.setInt(3, item.tab); insert.setInt(4, item.itemId);
                    insert.setInt(5, item.goldSell); insert.setInt(6, item.rubySell);
                    insert.setInt(7, item.quantity);
                    String options = JSONValue.toJSONString(item.options);
                    insert.setString(8, "null".equals(options) ? "[]" : options);
                    insert.setLong(9, item.lasttime); insert.setInt(10, item.isBuy ? 1 : 0);
                    insert.addBatch();
                }
                insert.executeBatch();
            }
            connection.commit();
            ended = true;
        } catch (SQLException | RuntimeException error) {
            try { connection.rollback(); ended = true; }
            catch (SQLException rollback) { error.addSuppressed(rollback); }
            throw error;
        } finally {
            // Never implicitly commit a partial snapshot if rollback itself failed.
            if (ended) connection.setAutoCommit(true);
        }
    }
}
