package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static server.web.panel.PanelSupport.*;

/** Authenticated panel resources. Authentication and response serialization belong to the router. */
public final class CatalogShopApi {
    private CatalogShopApi() {}

    public static Object handle(String resource, HttpExchange exchange) throws Exception {
        try {
            return switch (resource) {
                case "item", "option" -> catalog(resource, exchange);
                case "giftcode", "giftcode/sync", "giftcode/batch" -> GiftcodeApi.handle(resource, exchange);
                case "shop" -> ShopApi.handle(exchange);
                default -> throw error(404, "Không tìm thấy tài nguyên");
            };
        } catch (SQLException e) {
            String state = e.getSQLState();
            if (state != null && state.startsWith("23")) throw error(409, "Dữ liệu trùng hoặc đang được tham chiếu");
            if (state != null && (state.startsWith("08") || state.startsWith("40"))) {
                throw error(503, "Cơ sở dữ liệu chưa sẵn sàng hoặc giao dịch xung đột; vui lòng thử lại");
            }
            throw error(500, "Không thực hiện được giao dịch cơ sở dữ liệu");
        }
    }

    private static Object catalog(String resource, HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET");
        Map<String, String> query = query(exchange);
        fields(query.keySet(), "id", "q");
        if (query.containsKey("id") && query.containsKey("q")) throw error(400, "Chỉ chọn id hoặc q");
        try (Connection con = AlyraManager.getConnection_Data()) {
            CatalogData data = CatalogData.load(con);
            Map<Integer, Map<String, Object>> rows = resource.equals("item") ? data.items : data.options;
            if (query.containsKey("id")) {
                int id = queryId(query, "id", false);
                Map<String, Object> row = rows.get(id);
                if (row == null) throw error(404, "Không tìm thấy " + resource + " #" + id);
                return row;
            }
            String q = query.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
            if (q.length() > 255) throw error(400, "Từ khóa quá dài");
            List<Map<String, Object>> result = new ArrayList<>();
            for (Map<String, Object> row : rows.values()) {
                if (q.isEmpty() || String.valueOf(row.get("id")).contains(q)
                        || String.valueOf(row.get("name")).toLowerCase(Locale.ROOT).contains(q)) result.add(row);
            }
            return result;
        }
    }

    static void rejectNull(JsonObject body, String... nullable) {
        Set<String> allowed = Set.copyOf(Arrays.asList(nullable));
        for (String key : body.keySet()) {
            if (body.get(key).isJsonNull() && !allowed.contains(key)) throw error(400, "Trường không được NULL: " + key);
        }
    }

    static void fields(JsonObject object, String... allowed) { fields(object.keySet(), allowed); }

    static void fields(Set<String> keys, String... allowed) {
        Set<String> whitelist = Set.copyOf(Arrays.asList(allowed));
        for (String key : keys) if (!whitelist.contains(key)) throw error(400, "Trường không được hỗ trợ: " + key);
    }

    static int queryId(Map<String, String> query, String key, boolean positive) {
        String raw = query.get(key);
        if (raw == null || raw.isBlank()) throw error(400, "Thiếu tham số: " + key);
        int id = number(raw, -1);
        if (id < (positive ? 1 : 0)) throw error(400, "ID không hợp lệ: " + key);
        return id;
    }

    static int id(JsonObject body, String key, boolean positive) {
        if (!body.has(key) || body.get(key).isJsonNull()) throw error(400, "Thiếu trường: " + key);
        int id = integer(body, key, -1);
        if (id < (positive ? 1 : 0)) throw error(400, "ID không hợp lệ: " + key);
        return id;
    }

    static String string(JsonObject body, String key, String fallback, int max, boolean empty) {
        if (!body.has(key)) return fallback;
        JsonElement value = body.get(key);
        if (value.isJsonNull() || !value.isJsonPrimitive() || !value.getAsJsonPrimitive().isString()) {
            throw error(400, "Chuỗi không hợp lệ: " + key);
        }
        String result = value.getAsString().trim();
        if ((!empty && result.isEmpty()) || result.length() > max || result.indexOf('\0') >= 0) {
            throw error(400, "Độ dài không hợp lệ: " + key);
        }
        return result;
    }

    static JsonArray array(JsonObject object, String key, int max) {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonArray()) throw error(400, "Mảng không hợp lệ: " + key);
        JsonArray result = element.getAsJsonArray();
        if (result.size() > max) throw error(400, "Quá nhiều phần tử: " + key);
        return result;
    }

    static JsonObject object(JsonElement element) {
        if (element == null || !element.isJsonObject()) throw error(400, "Phần tử JSON phải là object");
        return element.getAsJsonObject();
    }

    static JsonArray storedArray(String raw, String location) {
        try {
            JsonElement parsed = new JsonParser().parse(raw == null ? "null" : raw);
            if (!parsed.isJsonArray()) throw new IllegalArgumentException();
            return parsed.getAsJsonArray();
        } catch (RuntimeException e) {
            throw error(422, "JSON trong cơ sở dữ liệu không hợp lệ: " + location);
        }
    }

    static JsonArray copy(JsonArray value) {
        return new JsonParser().parse(value.toString()).getAsJsonArray();
    }

    static Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) result.put((String) pairs[i], pairs[i + 1]);
        return result;
    }

    static PreparedStatement statement(Connection con, String sql, Object... args) throws SQLException {
        PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
        try {
            for (int i = 0; i < args.length; i++) ps.setObject(i + 1, args[i]);
            return ps;
        } catch (SQLException e) { ps.close(); throw e; }
    }

    static int execute(Connection con, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = statement(con, sql, args)) { return ps.executeUpdate(); }
    }

    static int insert(Connection con, String sql, Object... args) throws SQLException {
        try (PreparedStatement ps = statement(con, sql, args)) {
            if (ps.executeUpdate() != 1) throw error(500, "Không tạo được bản ghi");
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (!rs.next()) throw error(500, "Không lấy được ID bản ghi mới");
                return rs.getInt(1);
            }
        }
    }

    static void rollback(Connection con, Exception original) {
        try { con.rollback(); } catch (SQLException e) { original.addSuppressed(e); }
    }
}
