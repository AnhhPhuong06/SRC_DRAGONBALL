package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import static server.web.panel.CatalogShopApi.*;
import static server.web.panel.PanelSupport.*;

/** Edits the same TEXT reward format consumed by DonatePackageDAO and the game NPC. */
final class DonatePackageApi {
    @FunctionalInterface interface Connections { Connection open() throws SQLException; }
    @FunctionalInterface interface Catalog { CatalogData load(Connection con) throws SQLException; }
    private record Row(int id, String name, String detail, long cost, String items, boolean active, int sort) {}

    private DonatePackageApi() {}

    static Object handle(HttpExchange exchange) throws Exception {
        return handle(exchange, AlyraManager::getConnection_Data, CatalogData::load);
    }

    static Object handle(HttpExchange exchange, Connections connections, Catalog loader) throws Exception {
        requireMethod(exchange, "GET", "POST");
        var query = query(exchange);
        boolean read = exchange.getRequestMethod().equals("GET");
        fields(query.keySet(), read ? new String[]{} : new String[]{"action"});
        String action = query.getOrDefault("action", "");
        if (!read && !List.of("create", "update", "delete", "toggle").contains(action)) {
            throw error(400, "Thao tác gói donate không được hỗ trợ");
        }
        JsonObject request = read ? null : body(exchange);
        if (!read) {
            rejectNull(request);
            if (action.equals("delete")) fields(request, "id", "revision");
            else if (action.equals("toggle")) fields(request, "id", "revision", "active");
            else fields(request, action.equals("create")
                    ? new String[]{"name", "detail", "costVnd", "active", "sortOrder", "items"}
                    : new String[]{"id", "revision", "name", "detail", "costVnd", "active", "sortOrder", "items"});
        }
        try (Connection con = connections.open()) {
            con.setAutoCommit(false);
            try {
                Object response;
                if (read) response = snapshot(con, loader.load(con));
                else {
                    int id = mutate(con, action, request, loader);
                    var result = ok(action.equals("delete") ? "Đã xóa gói donate" : "Đã lưu gói donate. Mở lại NPC Chú Bé Đần để xem.");
                    result.put("id", id);
                    response = result;
                }
                con.commit();
                return response;
            } catch (Exception e) { rollback(con, e); throw e; }
        } catch (SQLException e) {
            String state = e.getSQLState();
            if (state != null && state.startsWith("23")) throw error(409, "Gói donate trùng hoặc đang được sử dụng");
            if (state != null && (state.equals("42S02") || state.equals("42S22"))) {
                throw error(503, "Database chưa có cấu trúc donate_package / donate_package_buy phù hợp với server");
            }
            throw error(503, "Không đọc/lưu được gói donate; kiểm tra kết nối database và thử lại");
        }
    }

    private static int mutate(Connection con, String action, JsonObject body, Catalog loader) throws Exception {
        boolean create = action.equals("create");
        int id = create ? 0 : id(body, "id", true);
        Row old = null;
        if (!create) {
            try (var ps = statement(con, "SELECT id,name,detail,cost_vnd,items,active,sort_order FROM donate_package WHERE id=? FOR UPDATE", id);
                    var rs = ps.executeQuery()) {
                if (!rs.next()) throw error(404, "Gói donate không còn tồn tại; hãy tải lại");
                old = row(rs);
            }
            if (!revision(old).equals(string(body, "revision", "", 64, false))) {
                throw error(409, "Gói donate đã thay đổi; tải lại trước khi sửa");
            }
        }
        if (action.equals("delete")) {
            // Lock the purchase range too: never discard the once-per-account purchase history.
            try (var ps = statement(con, "SELECT account_id FROM donate_package_buy WHERE package_id=? FOR UPDATE", id);
                    var rs = ps.executeQuery()) {
                if (rs.next()) throw error(409, "Gói đã có người mua; hãy tắt gói để giữ lịch sử mua");
            }
            if (execute(con, "DELETE FROM donate_package WHERE id=?", id) != 1) throw error(409, "Gói đã thay đổi");
            return id;
        }
        if (action.equals("toggle")) {
            if (!body.has("active")) throw error(400, "Thiếu trạng thái active");
            boolean active = bool(body, "active", false);
            if (active) validateStored(old, loader.load(con));
            execute(con, "UPDATE donate_package SET active=? WHERE id=?", active ? 1 : 0, id);
            return id;
        }
        String name = string(body, "name", null, 100, false);
        if (name == null) throw error(400, "Thiếu tên gói donate");
        String detail = string(body, "detail", "", 500, true);
        long cost = longValue(body, "costVnd", 0);
        if (cost < 1 || cost > Integer.MAX_VALUE) throw error(400, "Giá gói từ 1 đến 2.147.483.647 VNĐ");
        int sort = integer(body, "sortOrder", 0);
        boolean active = bool(body, "active", false);
        String items = encode(array(body, "items", 100), loader.load(con));
        if (create) return insert(con, "INSERT INTO donate_package(name,detail,cost_vnd,items,active,sort_order) VALUES(?,?,?,?,?,?)",
                name, detail, cost, items, active ? 1 : 0, sort);
        execute(con, "UPDATE donate_package SET name=?,detail=?,cost_vnd=?,items=?,active=?,sort_order=? WHERE id=?",
                name, detail, cost, items, active ? 1 : 0, sort, id);
        return id;
    }

    private static void validateStored(Row row, CatalogData catalog) {
        if (row.name == null || row.name.isBlank() || row.cost < 1 || row.cost > Integer.MAX_VALUE) {
            throw error(422, "Sửa tên/giá gói trước khi bật bán");
        }
        encode(decode(row.items), catalog);
    }

    static String encode(JsonArray items, CatalogData catalog) {
        if (items.size() < 1 || items.size() > 100) throw error(400, "Gói cần từ 1 đến 100 vật phẩm");
        StringBuilder result = new StringBuilder();
        for (var element : items) {
            var item = object(element);
            fields(item, "itemId", "quantity", "options");
            int id = id(item, "itemId", false);
            catalog.item(id, false);
            int quantity = integer(item, "quantity", 0);
            if (quantity < 1) throw error(400, "Số lượng vật phẩm từ 1 đến 2.147.483.647");
            JsonArray options = catalog.validateOptions(array(item, "options", 255));
            if (!result.isEmpty()) result.append('\n');
            result.append(id).append(' ').append(quantity);
            for (var option : options) {
                var value = option.getAsJsonObject();
                int param = integer(value, "param", -1);
                int optionId = integer(value, "id", -1);
                if (optionId > 255 || param < 0 || param > 65535) throw error(400, "Option ID 0..255, giá trị 0..65535");
                result.append(' ').append(optionId).append(':').append(param);
            }
        }
        if (result.toString().getBytes(StandardCharsets.UTF_8).length > 65535) throw error(400, "Danh sách quà vượt giới hạn lưu trữ của gói");
        return result.toString();
    }

    /** Strict read for the editor: malformed legacy lines must be visible, never silently lost. */
    static JsonArray decode(String raw) {
        JsonArray items = new JsonArray();
        if (raw == null) return items;
        for (String line : raw.split("[\r\n;]+")) {
            if (line.isBlank()) continue;
            try {
                String[] tokens = line.trim().split("[\\s,]+");
                JsonObject item = new JsonObject();
                item.addProperty("itemId", Integer.parseInt(tokens[0]));
                int start = tokens.length > 1 && !tokens[1].contains(":") ? 2 : 1;
                item.addProperty("quantity", start == 2 ? Integer.parseInt(tokens[1]) : 1);
                JsonArray options = new JsonArray();
                for (int i = start; i < tokens.length; i++) {
                    String[] pair = tokens[i].split(":", -1);
                    if (pair.length != 2) throw new IllegalArgumentException();
                    JsonObject option = new JsonObject();
                    option.addProperty("id", Integer.parseInt(pair[0]));
                    option.addProperty("param", Integer.parseInt(pair[1]));
                    options.add(option);
                }
                item.add("options", options);
                items.add(item);
            } catch (RuntimeException e) { throw error(422, "Có dòng quà cũ sai định dạng; sửa lại vật phẩm trước khi lưu/bật gói"); }
        }
        return items;
    }

    private static Row row(ResultSet rs) throws SQLException {
        return new Row(rs.getInt("id"), rs.getString("name"), rs.getString("detail"), rs.getLong("cost_vnd"),
                rs.getString("items"), rs.getInt("active") == 1, rs.getInt("sort_order"));
    }

    private static String revision(Row row) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(GSON.toJson(row).getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.NoSuchAlgorithmException e) { throw new IllegalStateException(e); }
    }

    private static Object snapshot(Connection con, CatalogData catalog) throws SQLException {
        List<Map<String, Object>> packages = new ArrayList<>();
        try (var ps = statement(con, "SELECT p.id,p.name,p.detail,p.cost_vnd,p.items,p.active,p.sort_order,"
                + "(SELECT COUNT(*) FROM donate_package_buy b WHERE b.package_id=p.id) AS sold FROM donate_package p ORDER BY p.sort_order,p.id");
                var rs = ps.executeQuery()) {
            while (rs.next()) {
                Row row = row(rs);
                JsonArray items = new JsonArray();
                String warning = "";
                try { items = decode(row.items); validateStored(row, catalog); }
                catch (PanelException e) { warning = e.getMessage(); }
                packages.add(map("id", row.id, "name", row.name, "detail", row.detail, "costVnd", row.cost,
                        "active", row.active, "sortOrder", row.sort, "items", items, "sold", rs.getLong("sold"),
                        "revision", revision(row), "warning", warning, "rawItems", warning.isEmpty() ? "" : row.items));
            }
        }
        return map("packages", packages, "catalog", map("items", catalog.items.values(), "options", catalog.options.values()),
                "notes", "NPC Chú Bé Đần ở đảo Kame. Giá trừ số dư VNĐ (sotien). Mỗi tài khoản mua mỗi gói 1 lần. Lưu xong mở lại NPC để áp dụng.");
    }
}
