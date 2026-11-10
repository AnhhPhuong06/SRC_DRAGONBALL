package server.web.panel;

import com.google.gson.JsonObject;
import database.AlyraManager;
import database.daos.NDVSqlFetcher;
import java.sql.*;
import java.util.*;
import models.player.Player;
import network.session.MySession;
import server.Client;
import static server.web.panel.PanelSupport.*;

/** Prepared statements and the same login/disconnect locks used by the game. */
final class AccountPlayerStore {
    static final int LIMIT = 500;
    static final long JSON_INTEGER_MAX = 9_007_199_254_740_991L;
    private AccountPlayerStore() {}

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static List<Map<String, Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            try (ResultSet r = s.executeQuery()) {
                List<Map<String, Object>> result = new ArrayList<>();
                ResultSetMetaData md = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) {
                        Object value = r.getObject(i);
                        if (value instanceof java.util.Date) value = value.toString();
                        // MySQL giữ kiểu chữ tên cột từ schema đã import (ID, Username...).
                        // Các handler dùng khóa chữ thường; thiếu "id" từng bị đọc thành 0.
                        row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT), value);
                    }
                    result.add(row);
                }
                return result;
            }
        }
    }

    static Map<String, Object> one(Connection c, String sql, Object... args) throws SQLException {
        List<Map<String, Object>> list = rows(c, sql, args);
        if (list.isEmpty()) throw error(404, "Không tìm thấy bản ghi");
        return list.get(0);
    }

    static long scalar(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) {
            bind(s, args);
            try (ResultSet r = s.executeQuery()) { return r.next() ? r.getLong(1) : 0; }
        }
    }

    static int execute(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement s = c.prepareStatement(sql)) { bind(s, args); return s.executeUpdate(); }
    }

    private static void bind(PreparedStatement s, Object... args) throws SQLException {
        for (int i = 0; i < args.length; i++) s.setObject(i + 1, args[i]);
    }

    static long value(Map<String, Object> row, String key) {
        Object v = row.get(key);
        if (v instanceof Boolean b) return b ? 1 : 0;
        if (v instanceof Number n) return n.longValue();
        if (v == null) return 0;
        String text = v.toString().trim();
        if ("true".equalsIgnoreCase(text)) return 1;
        if ("false".equalsIgnoreCase(text)) return 0;
        try { return Long.parseLong(text); }
        catch (NumberFormatException e) { throw error(500, "Dữ liệu số không hợp lệ trong trường " + key); }
    }

    static int id(JsonObject b) { return (int) bound(b, "id", 1, Integer.MAX_VALUE); }
    static long bound(JsonObject b, String k, long min, long max) {
        if (!b.has(k) || b.get(k).isJsonNull()) throw error(400, "Thiếu trường: " + k);
        long v = longValue(b, k, 0);
        if (v < min || v > max) throw error(400, k + " phải trong khoảng " + min + ".." + max);
        return v;
    }

    static void keys(JsonObject b, String... allowed) {
        Set<String> set = new HashSet<>(Arrays.asList(allowed));
        set.add("id");
        for (String k : b.keySet()) if (!set.contains(k)) throw error(400, "Trường không được hỗ trợ: " + k);
    }

    static String limited(String v, String key, int min, int max) {
        if (v.length() < min || v.length() > max || v.chars().anyMatch(ch -> Character.isISOControl(ch))) {
            throw error(400, "Độ dài/nội dung không hợp lệ: " + key);
        }
        return v;
    }

    static void update(Connection c, String table, int id, Map<String, Object> fields) throws SQLException {
        if (fields.isEmpty()) throw error(400, "Không có trường nào cần cập nhật");
        // table/column names originate only in the handlers' hard-coded whitelists.
        StringJoiner assignments = new StringJoiner(",");
        for (String column : fields.keySet()) assignments.add("`" + column + "`=?");
        List<Object> values = new ArrayList<>(fields.values());
        values.add(id);
        execute(c, "UPDATE `" + table + "` SET " + assignments + " WHERE id=?", values.toArray());
    }

    @FunctionalInterface interface Edit { Object run(Connection c, Map<String, Object> account) throws Exception; }

    static Object edit(int accountId, Edit action) throws Exception {
        String username;
        try (Connection c = AlyraManager.getConnection()) {
            username = (String) one(c, "SELECT username FROM account WHERE id=?", accountId).get("username");
        }
        // MySession holds this lock through load AND Client.put, not just the SQL login call.
        synchronized (MySession.accountLock(username)) {
            synchronized (NDVSqlFetcher.class) {
                try (Connection c = AlyraManager.getConnection()) {
                    Map<String, Object> check = one(c, "SELECT username FROM account WHERE id=?", accountId);
                    if (!username.equals(check.get("username"))) throw error(409, "Tài khoản vừa đổi tên; tải lại rồi thử lại");
                    Player online = Client.gI().getPlayerByUser(accountId);
                    if (online != null) {
                        synchronized (online) {
                            Client.gI().kickSession(online.getSession());
                            if (!online.disposed || Client.gI().getPlayerByUser(accountId) != null) {
                                throw error(409, "Chưa hoàn tất lưu/ngắt phiên; chưa ghi thay đổi");
                            }
                        }
                    }
                    c.setAutoCommit(false);
                    try {
                        Map<String, Object> account = one(c,
                                "SELECT id,username,ban,cash,danap,sotien,coin,thoi_vang FROM account WHERE id=? FOR UPDATE", accountId);
                        Object result = action.run(c, account);
                        c.commit();
                        return result;
                    } catch (Exception e) { c.rollback(); throw e; }
                }
            }
        }
    }

    static int accountForPlayer(int playerId) throws SQLException {
        try (Connection c = AlyraManager.getConnection()) {
            return (int) value(one(c, "SELECT account_id FROM player WHERE id=?", playerId), "account_id");
        }
    }

    static boolean tableExists(Connection c, String table) throws SQLException {
        return scalar(c, "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema=DATABASE() AND table_name=?", table) > 0;
    }

    static Map<String, Object> kick(int accountId) throws Exception {
        return (Map<String, Object>) edit(accountId, (c, a) -> ok("Đã ngắt phiên và lưu hoàn tất"));
    }
}
