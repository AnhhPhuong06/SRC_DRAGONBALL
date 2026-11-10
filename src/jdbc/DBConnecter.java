package jdbc;

import database.AlyraManager;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Cầu nối JDBC cho bộ panel quản trị (server/ui).
 * Trả về Connection từ pool HikariCP dùng chung của server qua AlyraManager,
 * nên các panel không cần tự mở/đóng nguồn kết nối riêng.
 */
public final class DBConnecter {

    private DBConnecter() {
    }

    /** Lấy 1 Connection tới DB tài khoản (account: account, giftcode, player...). */
    public static Connection getConnectionServer() throws SQLException {
        return AlyraManager.getConnection();
    }

    /** Lấy 1 Connection tới DB dữ liệu game (item_template, tab_shop, part, shop...). */
    public static Connection getConnectionData() throws SQLException {
        return AlyraManager.getConnection_Data();
    }
}
