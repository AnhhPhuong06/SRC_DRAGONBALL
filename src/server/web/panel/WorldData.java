package server.web.panel;

import com.google.gson.*;
import database.AlyraManager;
import java.sql.*;
import java.util.*;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import static server.web.panel.PanelSupport.*;

/** Small JDBC/loader utilities private to the world endpoints. No runtime initializers. */
final class WorldData {
    static final Object EDIT_LOCK = new Object();
    private WorldData() {}

    static Map<String, Object> dto(Object... pairs) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) out.put((String) pairs[i], pairs[i + 1]);
        return out;
    }
    static List<Map<String, Object>> choices(String... names) {
        List<Map<String, Object>> out = new ArrayList<>();
        for (int i = 0; i < names.length; i++) out.add(dto("id", i, "name", names[i]));
        return out;
    }
    static final List<Map<String, Object>> PLANETS = choices("Trái Đất", "Namec", "Xayda", "Khác");
    static final List<Map<String, Object>> GENDERS = choices("Trái Đất", "Namec", "Xayda", "Cả ba hệ");
    static String gender(int id) { return id >= 0 && id <= 2 ? (String) GENDERS.get(id).get("name") : "Cả ba hệ"; }
    static int range(int value, int min, int max, String field) {
        if (value < min || value > max) throw error(400, field + " phải trong khoảng " + min + " - " + max);
        return value;
    }
    static int field(JsonObject b, String key, int fallback, int min, int max) {
        return range(integer(b, key, fallback), min, max, key);
    }
    static String name(JsonObject b, String key, String fallback, int max) {
        String value = text(b, key, fallback);
        if (value == null || value.trim().isEmpty() || value.length() > max || value.indexOf('\0') >= 0)
            throw error(400, key + " không được rỗng, tối đa " + max + " ký tự");
        return value;
    }
    static int id(JsonObject b) {
        if (!b.has("id")) throw error(400, "Thiếu id");
        return field(b, "id", -1, 0, 32767);
    }
    static boolean matches(Map<String, String> q, int id, String name) {
        String needle = q.getOrDefault("q", "").trim().toLowerCase(Locale.ROOT);
        return needle.isEmpty() || Integer.toString(id).contains(needle) || name.toLowerCase(Locale.ROOT).contains(needle);
    }
    static boolean filter(Map<String, String> q, String key, int value) {
        String raw = q.getOrDefault(key, "");
        return raw.isEmpty() || "all".equals(raw) || number(raw, -1) == value;
    }
    static int limit(Map<String, String> q) { return range(number(q.get("limit"), 500), 1, 1000, "limit"); }

    static List<Map<String, Object>> rows(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String, Object>> out = new ArrayList<>();
                ResultSetMetaData md = r.getMetaData();
                while (r.next()) {
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (int i = 1; i <= md.getColumnCount(); i++) row.put(md.getColumnLabel(i).toLowerCase(Locale.ROOT), r.getObject(i));
                    out.add(row);
                }
                return out;
            }
        }
    }
    static Map<String, Object> one(Connection c, String sql, Object... args) throws SQLException {
        List<Map<String, Object>> list = rows(c, sql, args);
        if (list.isEmpty()) throw error(404, "Không tìm thấy bản ghi");
        return list.getFirst();
    }
    static int update(Connection c, String sql, Object... args) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(sql)) {
            for (int i = 0; i < args.length; i++) p.setObject(i + 1, args[i]);
            return p.executeUpdate();
        }
    }
    static int n(Map<String, Object> r, String key) {
        Object value = r.get(key);
        if (value instanceof Boolean b) return b ? 1 : 0;
        if (value instanceof Number number) return number.intValue();
        try { return Integer.parseInt(String.valueOf(value)); }
        catch (RuntimeException e) { throw error(500, "Dữ liệu số không hợp lệ trong trường " + key); }
    }
    static long l(Map<String, Object> r, String key) {
        Object value = r.get(key);
        if (value instanceof Boolean b) return b ? 1L : 0L;
        if (value instanceof Number number) return number.longValue();
        try { return Long.parseLong(String.valueOf(value)); }
        catch (RuntimeException e) { throw error(500, "Dữ liệu số không hợp lệ trong trường " + key); }
    }
    static String s(Map<String, Object> r, String key) { return Objects.toString(r.get(key), ""); }

    @FunctionalInterface interface Work<T> { T run(Connection c) throws Exception; }
    static <T> T transaction(boolean data, Work<T> work) throws Exception {
        try (Connection c = data ? AlyraManager.getConnection_Data() : AlyraManager.getConnection()) {
            c.setAutoCommit(false);
            try {
                T value = work.run(c);
                c.commit();
                return value;
            } catch (Exception e) {
                try { c.rollback(); } catch (SQLException rollback) { e.addSuppressed(rollback); }
                if (e instanceof SQLException sql && "23000".equals(sql.getSQLState()))
                    throw error(409, "ID đã tồn tại hoặc bản ghi còn được tham chiếu");
                throw e;
            }
        }
    }
    /** Decode both current JSON arrays and the legacy stringified arrays used by Manager.loadDatabase. */
    static JsonArray storedArray(String raw, boolean waypoints) {
        if (raw == null || raw.isBlank()) throw error(409, "JSON danh sách trong DB đang rỗng/không hợp lệ");
        String cleaned = waypoints
                ? raw.replace("[\"[", "[[").replace("]\"]", "]]" ).replace("\",\"", ",")
                : raw.replace("\"", "");
        JsonArray decoded = decodeStoredRows(JSONValue.parse(raw));
        if (decoded != null) return decoded;
        // Some legacy strings contain several tuples, not just one. Their outer array parses,
        // but the individual string is not a JSON array. Retry the whole loader-normalized value
        // rather than rejecting the first tuple or dropping the remaining spawns.
        decoded = decodeStoredRows(JSONValue.parse(cleaned));
        if (decoded != null) return decoded;
        throw error(409, "JSON danh sách hoặc phần tử trong DB không đúng định dạng loader");
    }

    private static JsonArray decodeStoredRows(Object parsed) {
        if (!(parsed instanceof JSONArray)) return null;
        JsonArray out = new JsonArray();
        for (Object element : (JSONArray) parsed) {
            Object nested = element;
            if (element instanceof String text) nested = JSONValue.parse(text);
            if (!(nested instanceof JSONArray)) return null;
            out.add(toJson((JSONArray) nested));
        }
        return out;
    }

    private static JsonElement toJson(Object value) {
        if (value instanceof JSONArray array) {
            JsonArray result = new JsonArray();
            for (Object item : array) result.add(toJson(item));
            return result;
        }
        if (value instanceof org.json.simple.JSONObject object) {
            JsonObject result = new JsonObject();
            for (Object key : object.keySet()) result.add(String.valueOf(key), toJson(object.get(key)));
            return result;
        }
        if (value instanceof Number number) return new JsonPrimitive(number);
        if (value instanceof Boolean bool) return new JsonPrimitive(bool);
        return new JsonPrimitive(String.valueOf(value));
    }
    static JsonArray array(JsonObject b, String key, int max) {
        if (!b.has(key) || !b.get(key).isJsonArray()) throw error(400, key + " phải là mảng");
        JsonArray a = b.getAsJsonArray(key);
        if (a.size() > max) throw error(400, key + " vượt quá " + max + " dòng");
        for (JsonElement e : a) if (!e.isJsonObject()) throw error(400, key + " phải chứa các object");
        return a;
    }
    static JsonArray tuple(Object... values) {
        JsonArray a = new JsonArray();
        for (Object value : values) {
            if (value instanceof Number n) a.add(n);
            else if (value instanceof Boolean b) a.add(b);
            else a.add(String.valueOf(value));
        }
        return a;
    }
    static Map<String, Object> changed(String message, boolean restart, Object... extra) {
        Map<String, Object> out = ok(message);
        out.put("needsRestart", restart);
        out.putAll(dto(extra));
        return out;
    }
}
