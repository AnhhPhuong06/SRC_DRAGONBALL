package server.web.panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.AccountPlayerStore.*;

final class AccountDataApi {
    private AccountDataApi() {}

    static Object handle(HttpExchange exchange) throws Exception {
        Map<String, String> q = query(exchange);
        String action = q.getOrDefault("action", "");
        if (!"GET".equals(exchange.getRequestMethod())) requireMethod(exchange, "POST");
        if ("GET".equals(exchange.getRequestMethod())) return !q.containsKey("id") ? list(q) : detail(number(q.get("id"), 0));
        JsonObject body = body(exchange);
        return switch (action) {
            case "account-create" -> create(body);
            case "account-update" -> update(body);
            case "account-delete" -> delete(body);
            case "password" -> password(body);
            case "money" -> money(body);
            case "kick" -> AccountPlayerStore.kick(AccountPlayerStore.id(body));
            default -> throw error(400, "Tác vụ tài khoản không được hỗ trợ: " + action);
        };
    }

    private static Object list(Map<String, String> q) throws Exception {
        String search = q.getOrDefault("q", "").trim();
        if (search.length() > 64) throw error(400, "Từ khóa quá dài");
        String filter = q.getOrDefault("filter", "");
        if (!Set.of("", "online", "offline", "banned", "active", "admin").contains(filter)) {
            throw error(400, "Bộ lọc tài khoản không hợp lệ");
        }
        String where = " WHERE 1=1";
        List<Object> args = new ArrayList<>();
        if (!search.isEmpty()) {
            where += " AND (a.username LIKE ? OR CAST(a.id AS CHAR)=? OR EXISTS (SELECT 1 FROM player px WHERE px.account_id=a.id AND px.name LIKE ?))";
            String like = "%" + search + "%";
            args.add(like); args.add(search); args.add(like);
        }
        switch (filter) {
            case "online" -> where += " AND EXISTS (SELECT 1 FROM player po WHERE po.account_id=a.id AND po.id IN (SELECT id FROM player))"; // refined below
            case "banned" -> where += " AND a.ban=1";
            case "active" -> where += " AND a.active=1";
            case "admin" -> where += " AND a.is_admin=1";
            default -> { }
        }
        // Online is a runtime property and cannot be represented by SQL alone.
        if ("online".equals(filter) || "offline".equals(filter)) {
            // Load the bounded candidate set, then apply the runtime filter below.
            where = where.replace(" AND EXISTS (SELECT 1 FROM player po WHERE po.account_id=a.id AND po.id IN (SELECT id FROM player))", "");
        }
        try (Connection c = AlyraManager.getConnection()) {
            long total = AccountPlayerStore.scalar(c, "SELECT COUNT(*) FROM account a" + where, args.toArray());
            String sql = "SELECT a.id,a.username,a.create_time,a.ban,a.is_admin,a.active,a.vip,a.ip_address,a.danap,a.cash,a.sotien,a.thoi_vang,"
                    + "(SELECT COUNT(*) FROM player p WHERE p.account_id=a.id) char_count FROM account a" + where + " ORDER BY a.id DESC LIMIT " + AccountPlayerStore.LIMIT;
            List<Map<String, Object>> source = AccountPlayerStore.rows(c, sql, args.toArray());
            List<Map<String, Object>> accounts = new ArrayList<>();
            int onlineCount = 0;
            for (Map<String, Object> r : source) {
                int id = (int) AccountPlayerStore.value(r, "id");
                boolean online = server.Client.gI().getPlayerByUser(id) != null;
                if (online) onlineCount++;
                if (("online".equals(filter) && !online) || ("offline".equals(filter) && online)) continue;
                accounts.add(row(r, online));
            }
            return AccountPlayerStore.map("accounts", accounts, "total", total, "online", onlineCount,
                    "limit", AccountPlayerStore.LIMIT, "filters", filters());
        }
    }

    private static List<Map<String, Object>> filters() {
        return List.of(AccountPlayerStore.map("id", "", "name", "Tất cả"),
                AccountPlayerStore.map("id", "online", "name", "Đang online"),
                AccountPlayerStore.map("id", "offline", "name", "Đang offline"),
                AccountPlayerStore.map("id", "banned", "name", "Đang khóa"),
                AccountPlayerStore.map("id", "active", "name", "Thành viên"),
                AccountPlayerStore.map("id", "admin", "name", "Quản trị"));
    }

    private static Map<String, Object> row(Map<String, Object> r, boolean online) {
        int id = (int) AccountPlayerStore.value(r, "id");
        return AccountPlayerStore.map("id", id, "username", r.get("username"), "created", r.get("create_time"),
                "ban", AccountPlayerStore.value(r, "ban") != 0, "isAdmin", AccountPlayerStore.value(r, "is_admin") != 0,
                "active", AccountPlayerStore.value(r, "active") != 0, "vip", AccountPlayerStore.value(r, "vip"),
                "online", online, "charCount", AccountPlayerStore.value(r, "char_count"),
                "vnd", AccountPlayerStore.value(r, "cash"), "tongnap", AccountPlayerStore.value(r, "danap"),
                "cash", AccountPlayerStore.value(r, "cash"), "danap", AccountPlayerStore.value(r, "danap"),
                "sotien", AccountPlayerStore.value(r, "sotien"), "thoiVang", AccountPlayerStore.value(r, "thoi_vang"),
                "ipAddress", r.get("ip_address"));
    }

    private static Object detail(int id) throws Exception {
        if (id <= 0) throw error(400, "ID tài khoản không hợp lệ");
        try (Connection c = AlyraManager.getConnection()) {
            Map<String, Object> a = AccountPlayerStore.one(c, "SELECT id,username,email,create_time,update_time,ban,is_admin,last_time_login,ip_address,active,bd_player,server_login,mkc2,cash,danap,sotien,coin,thoi_vang,vip FROM account WHERE id=?", id);
            boolean online = server.Client.gI().getPlayerByUser(id) != null;
            Map<String, Object> account = AccountPlayerStore.map("id", id, "username", a.get("username"), "email", a.get("email"),
                    "created", a.get("create_time"), "lastLogin", a.get("last_time_login"), "updated", a.get("update_time"),
                    "ban", AccountPlayerStore.value(a, "ban") != 0, "isAdmin", AccountPlayerStore.value(a, "is_admin") != 0,
                    "active", AccountPlayerStore.value(a, "active") != 0, "online", online,
                    "bdPlayer", a.get("bd_player"), "serverLogin", a.get("server_login"), "mkc2", a.get("mkc2"),
                    "cash", AccountPlayerStore.value(a, "cash"), "danap", AccountPlayerStore.value(a, "danap"),
                    "sotien", AccountPlayerStore.value(a, "sotien"), "coin", AccountPlayerStore.value(a, "coin"),
                    "thoiVang", AccountPlayerStore.value(a, "thoi_vang"), "vnd", AccountPlayerStore.value(a, "cash"),
                    "tongnap", AccountPlayerStore.value(a, "danap"), "accountAgeDays", ageDays(a.get("create_time")));
            List<Map<String, Object>> characters = new ArrayList<>();
            for (Map<String, Object> p : AccountPlayerStore.rows(c, "SELECT id,name,gender,data_point,data_inventory,create_time,head,rank FROM player WHERE account_id=? ORDER BY id", id)) {
                List<?> point = array(p.get("data_point")); List<?> inv = array(p.get("data_inventory"));
                boolean po = server.Client.gI().getPlayer(AccountPlayerStore.value(p, "id")) != null;
                characters.add(AccountPlayerStore.map("id", AccountPlayerStore.value(p, "id"), "name", p.get("name"),
                        "gender", AccountPlayerStore.value(p, "gender"), "genderName", gender(AccountPlayerStore.value(p, "gender")),
                        "power", at(point, 1), "gold", at(inv, 0), "gem", at(inv, 1), "ruby", at(inv, 2), "online", po));
            }
            return AccountPlayerStore.map("account", account, "characters", characters, "catalog", AccountPlayerStore.map(
                    "moneyFields", List.of(AccountPlayerStore.map("id", "cash", "name", "Cash"), AccountPlayerStore.map("id", "danap", "name", "Tổng nạp"), AccountPlayerStore.map("id", "sotien", "name", "Số tiền"), AccountPlayerStore.map("id", "coin", "name", "Coin"), AccountPlayerStore.map("id", "thoi_vang", "name", "Thỏi vàng"))));
        }
    }

    private static Object create(JsonObject b) throws Exception {
        keys(b, "username", "password", "email", "ipAddress");
        String u = limited(required(b, "username"), "username", 3, 20);
        String pw = limited(required(b, "password"), "password", 3, 100);
        String email = text(b, "email", "");
        String ip = text(b, "ipAddress", "");
        try (Connection c = AlyraManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                if (AccountPlayerStore.scalar(c, "SELECT COUNT(*) FROM account WHERE username=?", u) != 0) throw error(409, "Username đã tồn tại");
                AccountPlayerStore.execute(c, "INSERT INTO account(username,password,email,ip_address,create_time) VALUES(?,?,?,?,NOW())", u, pw, email, ip.isEmpty() ? null : ip);
                c.commit(); return ok("Đã tạo tài khoản");
            } catch (Exception e) { c.rollback(); throw e; }
        }
    }

    private static Object update(JsonObject b) throws Exception {
        keys(b, "username", "email", "ipAddress", "active", "ban", "isAdmin", "vip", "bdPlayer", "serverLogin", "mkc2");
        int id = AccountPlayerStore.id(b);
        return AccountPlayerStore.edit(id, (c, old) -> {
            Map<String, Object> fields = new LinkedHashMap<>();
            if (b.has("username")) fields.put("username", limited(required(b, "username"), "username", 3, 20));
            if (b.has("email")) fields.put("email", limited(text(b, "email", ""), "email", 0, 255));
            if (b.has("ipAddress")) fields.put("ip_address", limited(text(b, "ipAddress", ""), "ipAddress", 0, 50));
            if (b.has("active")) fields.put("active", bool(b, "active", false) ? 1 : 0);
            if (b.has("ban")) fields.put("ban", bool(b, "ban", false) ? 1 : 0);
            if (b.has("isAdmin")) fields.put("is_admin", bool(b, "isAdmin", false) ? 1 : 0);
            if (b.has("vip")) fields.put("vip", AccountPlayerStore.bound(b, "vip", 0, Integer.MAX_VALUE));
            if (b.has("bdPlayer")) { double v = Double.parseDouble(text(b, "bdPlayer", "")); if (!Double.isFinite(v) || v < 0 || v > 100) throw error(400, "bdPlayer ngoài giới hạn"); fields.put("bd_player", v); }
            if (b.has("serverLogin")) fields.put("server_login", AccountPlayerStore.bound(b, "serverLogin", -1, Integer.MAX_VALUE));
            if (b.has("mkc2")) fields.put("mkc2", AccountPlayerStore.bound(b, "mkc2", 0, Integer.MAX_VALUE));
            if (b.has("username") && AccountPlayerStore.scalar(c, "SELECT COUNT(*) FROM account WHERE username=? AND id<>?", fields.get("username"), id) != 0) throw error(409, "Username đã tồn tại");
            AccountPlayerStore.update(c, "account", id, fields); return ok("Đã lưu thông tin tài khoản");
        });
    }

    private static Object password(JsonObject b) throws Exception {
        keys(b, "password");
        int id = AccountPlayerStore.id(b); String pw = limited(required(b, "password"), "password", 3, 100);
        return AccountPlayerStore.edit(id, (c, old) -> { AccountPlayerStore.execute(c, "UPDATE account SET password=? WHERE id=?", pw, id); return ok("Đã đổi mật khẩu"); });
    }

    private static Object money(JsonObject b) throws Exception {
        keys(b, "field", "amount", "add");
        int id = AccountPlayerStore.id(b); String field = required(b, "field");
        if (!Set.of("cash", "danap", "sotien", "coin", "thoi_vang").contains(field)) throw error(400, "Ví không được hỗ trợ: " + field);
        long amount = AccountPlayerStore.bound(b, "amount", 1, Integer.MAX_VALUE); boolean add = bool(b, "add", false);
        return AccountPlayerStore.edit(id, (c, old) -> {
            long current = AccountPlayerStore.value(old, field);
            if (!add && amount > current) throw error(409, "Số dư không đủ");
            long next = add ? current + amount : current - amount;
            if (next > Integer.MAX_VALUE || next < 0) throw error(400, "Giá trị ví vượt giới hạn cột");
            String op = add ? "+" : "-";
            int changed = AccountPlayerStore.execute(c, "UPDATE account SET `" + field + "`=`" + field + "`" + op + "? WHERE id=?", amount, id);
            if (changed != 1) throw error(409, "Tài khoản đã thay đổi; hãy tải lại");
            return ok("Đã cập nhật ví " + field);
        });
    }

    private static Object delete(JsonObject b) throws Exception {
        keys(b); int id = AccountPlayerStore.id(b);
        return AccountPlayerStore.edit(id, (c, old) -> {
            AccountPlayerStore.execute(c, "DELETE FROM player_gift_data WHERE player_id IN (SELECT id FROM player WHERE account_id=?)", id);
            AccountPlayerStore.execute(c, "DELETE FROM player WHERE account_id=?", id);
            AccountPlayerStore.execute(c, "DELETE FROM account WHERE id=?", id);
            return ok("Đã xóa tài khoản và nhân vật");
        });
    }

    static List<?> array(Object value) {
        if (value == null) return List.of();
        com.google.gson.JsonElement parsed = new JsonParser().parse(value.toString());
        return parsed.isJsonArray() ? PanelSupport.GSON.fromJson(parsed, List.class) : List.of();
    }
    static Object at(List<?> list, int i) { return i < list.size() ? list.get(i) : 0; }
    static String gender(long gender) { return gender == 0 ? "Trái Đất" : gender == 1 ? "Namếc" : gender == 2 ? "Xayda" : "Hệ " + gender; }
    static int ageDays(Object timestamp) { if (!(timestamp instanceof Timestamp t)) return 0; return (int) Math.max(0, (System.currentTimeMillis() - t.getTime()) / 86_400_000L); }
}
