package server.web.panel;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.AccountPlayerStore.*;

/** Revenue and account-state actions backed by this project's actual ledger. */
final class RevenueDataApi {
    private RevenueDataApi() {}

    static Object handle(HttpExchange exchange) throws Exception {
        Map<String, String> q = query(exchange);
        if ("GET".equals(exchange.getRequestMethod())) {
            requireMethod(exchange, "GET");
            return info();
        }
        requireMethod(exchange, "POST");
        JsonObject body = body(exchange);
        String action = q.getOrDefault("action", "");
        return switch (action) {
            case "money-add", "money-sub" -> money(body, "money-add".equals(action));
            case "account-mtv-on", "account-mtv-off" -> accountFlag(body, "active", action.endsWith("-on"), "thành viên");
            case "account-lock", "account-unlock" -> accountFlag(body, "ban", action.endsWith("-lock"), "khóa");
            case "account-admin-on", "account-admin-off" -> accountFlag(body, "is_admin", action.endsWith("-on"), "admin game");
            case "ip-ban", "ip-unban" -> ipFlag(body, action.endsWith("-ban"));
            case "clone-ban" -> cloneBan(body);
            default -> throw error(400, "Tác vụ doanh thu không được hỗ trợ: " + action);
        };
    }

    private static Object info() throws Exception {
        try (Connection c = AlyraManager.getConnection()) {
            long total = scalar(c, "SELECT COUNT(*) FROM account");
            long today = scalar(c, "SELECT COUNT(*) FROM account WHERE create_time >= CURRENT_DATE()");
            long week = scalar(c, "SELECT COUNT(*) FROM account WHERE create_time >= DATE_SUB(CURRENT_DATE(), INTERVAL 6 DAY)");
            long active = scalar(c, "SELECT COALESCE(SUM(active),0) FROM account");
            long locked = scalar(c, "SELECT COALESCE(SUM(CASE WHEN ban<>0 THEN 1 ELSE 0 END),0) FROM account");
            long admins = scalar(c, "SELECT COALESCE(SUM(CASE WHEN is_admin<>0 THEN 1 ELSE 0 END),0) FROM account");
            long topupAccounts = scalar(c, "SELECT COUNT(*) FROM account WHERE danap > 0");
            long accumulated = scalar(c, "SELECT COALESCE(SUM(danap),0) FROM account");

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("accounts", map("total", total, "today", today, "week", week, "mtv", active,
                    "locked", locked, "adminTotal", admins, "topupAccounts", topupAccounts,
                    "totalTopup", accumulated));
            result.put("revenue", map("total", accumulated, "today", null, "week", null, "month", null,
                    "weekLabel", "Chưa có nguồn ledger doanh thu được xác minh"));
            result.put("chart", chart(c));
            result.put("topupToday", topups(c));
            result.put("recharge", map("ready", false, "reason",
                    "Source này chỉ có bảng napthe nhưng chưa có producer xác định trạng thái thành công; không tự tính doanh thu theo ngày."));
            result.put("cloneLimit", 5);
            result.put("clones", List.of());
            return result;
        }
    }

    private static List<Map<String, Object>> chart(Connection c) throws SQLException {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            long accounts = scalar(c, "SELECT COUNT(*) FROM account WHERE create_time >= ? AND create_time < ?",
                    Timestamp.valueOf(day.atStartOfDay()), Timestamp.valueOf(day.plusDays(1).atStartOfDay()));
            result.add(map("date", day.toString(), "day", day.getDayOfWeek().toString(), "accounts", accounts, "revenue", null));
        }
        return result;
    }

    private static Map<String, Object> topups(Connection c) {
        try {
            if (!tableExists(c, "napthe")) {
                return map("available", false, "count", 0, "rows", List.of(), "reason", "Không có bảng napthe");
            }
            List<Map<String, Object>> rows = rows(c,
                    "SELECT user_nap,amount,telco,created_at,status FROM napthe WHERE created_at >= CURRENT_DATE() ORDER BY created_at DESC LIMIT 100");
            List<Map<String, Object>> mapped = new ArrayList<>();
            for (Map<String, Object> row : rows) {
                mapped.add(map("username", row.get("user_nap"), "amount", value(row, "amount"),
                        "source", row.get("telco"), "status", row.get("status"), "time", row.get("created_at")));
            }
            return map("available", true, "count", mapped.size(), "rows", mapped);
        } catch (Exception e) {
            return map("available", false, "count", 0, "rows", List.of(), "reason", "Không đọc được lịch sử napthe");
        }
    }

    private static Object money(JsonObject body, boolean add) throws Exception {
        keys(body, "username", "amount");
        String username = limited(required(body, "username"), "username", 3, 20);
        long amount = bound(body, "amount", 1, Integer.MAX_VALUE);
        int id;
        try (Connection c = AlyraManager.getConnection()) {
            id = moneyAccountId(c, username);
        }
        return edit(id, (c, account) -> adjustMoney(c, account, amount, add));
    }

    static int moneyAccountId(Connection c, String username) throws SQLException {
        // Read the typed ID directly: an imported column named ID must not become ID 0.
        try (PreparedStatement statement = c.prepareStatement("SELECT id FROM account WHERE username=? LIMIT 2")) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) throw error(404,
                        "Không tìm thấy tên đăng nhập \"" + username + "\" trong database tài khoản của server đang chạy. "
                                + "Hãy kiểm tra trong Quản Lý Tài Khoản.");
                long id = result.getLong(1);
                if (result.wasNull() || id < 0 || id > Integer.MAX_VALUE) {
                    throw error(500, "ID tài khoản không hợp lệ; chưa thay đổi số dư");
                }
                if (result.next()) throw error(409,
                        "Tên đăng nhập bị trùng trong database; chưa thay đổi số dư");
                return (int) id;
            }
        }
    }

    // edit() has saved/disconnected the online session and locked this account row.
    static Object adjustMoney(Connection c, Map<String, Object> account, long amount, boolean add) throws SQLException {
        long current = value(account, "sotien");
        long next = add ? current + amount : current - amount;
        if (next < 0 || next > Integer.MAX_VALUE) throw error(409, "Số dư trong game vượt giới hạn hoặc không đủ");
        int changed;
        if (add) {
            long danap = value(account, "danap");
            long nextDanap = danap + amount;
            if (danap < 0 || nextDanap > Integer.MAX_VALUE) throw error(409, "Tổng nạp vượt giới hạn cột");
            changed = execute(c, "UPDATE account SET sotien=?,danap=? WHERE id=?",
                    next, nextDanap, value(account, "id"));
        } else {
            changed = execute(c, "UPDATE account SET sotien=? WHERE id=?", next, value(account, "id"));
        }
        if (changed != 1) throw error(409, "Tài khoản đã thay đổi; hãy tải lại");
        return ok(add ? "Đã cộng tiền vào số dư và tổng nạp" : "Đã trừ tiền từ số dư trong game");
    }

    private static Object accountFlag(JsonObject body, String column, boolean enabled, String label) throws Exception {
        keys(body, "username");
        String username = limited(required(body, "username"), "username", 3, 20);
        try (Connection c = AlyraManager.getConnection()) {
            int id = (int) value(one(c, "SELECT id FROM account WHERE username=?", username), "id");
            if ("ban".equals(column)) {
                long admin = scalar(c, "SELECT is_admin FROM account WHERE id=?", id);
                if (enabled && admin != 0) throw error(403, "Không khóa được tài khoản game admin");
            }
            return edit(id, (locked, account) -> {
                int changed = execute(locked, "UPDATE account SET `" + column + "`=? WHERE id=?", enabled ? 1 : 0, id);
                if (changed != 1) throw error(409, "Tài khoản đã thay đổi; hãy tải lại");
                return ok((enabled ? "Đã bật " : "Đã tắt ") + label);
            });
        }
    }

    private static Object ipFlag(JsonObject body, boolean enabled) throws Exception {
        keys(body, "ip");
        String ip = limited(required(body, "ip"), "ip", 1, 50);
        try (Connection c = AlyraManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed = execute(c, "UPDATE account SET ban=? WHERE ip_address=? AND is_admin=0", enabled ? 1 : 0, ip);
                c.commit();
                return ok((enabled ? "Đã khóa " : "Đã mở khóa ") + changed + " tài khoản cùng IP");
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    private static Object cloneBan(JsonObject body) throws Exception {
        keys(body, "limit");
        long limit = bound(body, "limit", 2, 1000);
        try (Connection c = AlyraManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                int changed = execute(c, "UPDATE account SET ban=1 WHERE is_admin=0 AND ip_address IS NOT NULL AND ip_address<>'' "
                        + "AND ip_address IN (SELECT ip_address FROM (SELECT ip_address FROM account WHERE ip_address IS NOT NULL "
                        + "AND ip_address<>'' GROUP BY ip_address HAVING COUNT(*)>?) x)", limit);
                c.commit();
                return ok("Đã khóa " + changed + " tài khoản clone");
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }
}
