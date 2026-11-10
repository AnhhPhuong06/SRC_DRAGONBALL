package server.web.panel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

/** Shared JSON, request parsing and validation for the embedded panel. */
public final class PanelSupport {
    public static final Gson GSON = new Gson();
    private static final int MAX_BODY = 1024 * 1024;

    private PanelSupport() {}

    public static final class PanelException extends RuntimeException {
        public final int status;
        public PanelException(int status, String message) {
            super(message);
            this.status = status;
        }
    }

    public static PanelException error(int status, String message) {
        return new PanelException(status, message);
    }

    public static Map<String, String> query(HttpExchange exchange) {
        Map<String, String> result = new LinkedHashMap<>();
        String raw = exchange.getRequestURI().getRawQuery();
        if (raw == null || raw.isEmpty()) return result;
        try {
            for (String part : raw.split("&")) {
                String[] pair = part.split("=", 2);
                result.put(URLDecoder.decode(pair[0], StandardCharsets.UTF_8),
                        pair.length == 2 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "");
            }
        } catch (IllegalArgumentException e) {
            throw error(400, "Tham số URL không hợp lệ");
        }
        return result;
    }

    public static byte[] readBytes(HttpExchange exchange, int limit) throws IOException {
        byte[] bytes = exchange.getRequestBody().readNBytes(limit + 1);
        if (bytes.length > limit) throw error(413, "Dữ liệu gửi lên quá lớn");
        return bytes;
    }

    public static JsonObject body(HttpExchange exchange) throws IOException {
        byte[] bytes = readBytes(exchange, MAX_BODY);
        if (bytes.length == 0) return new JsonObject();
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.toLowerCase(java.util.Locale.ROOT).startsWith("application/json")) {
            throw error(415, "Yêu cầu Content-Type: application/json");
        }
        String json = new String(bytes, StandardCharsets.UTF_8);
        if (utils.JsonInput.hasExcessiveDepth(json)) throw error(400, "JSON lồng nhau quá sâu");
        try {
            return new JsonParser().parse(json).getAsJsonObject();
        } catch (RuntimeException e) {
            throw error(400, "JSON không hợp lệ");
        }
    }

    public static String text(JsonObject body, String key, String fallback) {
        if (!body.has(key) || body.get(key).isJsonNull()) return fallback;
        try { return body.get(key).getAsString(); }
        catch (RuntimeException e) { throw error(400, "Trường không hợp lệ: " + key); }
    }

    public static long longValue(JsonObject body, String key, long fallback) {
        if (!body.has(key) || body.get(key).isJsonNull()) return fallback;
        try { return new java.math.BigDecimal(body.get(key).getAsString()).longValueExact(); }
        catch (RuntimeException e) { throw error(400, "Số nguyên không hợp lệ: " + key); }
    }

    public static int integer(JsonObject body, String key, int fallback) {
        long value = longValue(body, key, fallback);
        if (value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) throw error(400, "Số ngoài giới hạn: " + key);
        return (int) value;
    }

    public static boolean bool(JsonObject body, String key, boolean fallback) {
        if (!body.has(key) || body.get(key).isJsonNull()) return fallback;
        String value = text(body, key, "");
        if ("true".equalsIgnoreCase(value) || "1".equals(value)) return true;
        if ("false".equalsIgnoreCase(value) || "0".equals(value)) return false;
        throw error(400, "Giá trị đúng/sai không hợp lệ: " + key);
    }

    public static String required(JsonObject body, String key) {
        String value = text(body, key, "").trim();
        if (value.isEmpty()) throw error(400, "Thiếu trường: " + key);
        return value;
    }

    public static int number(String value, int fallback) {
        if (value == null || value.isEmpty()) return fallback;
        try { return Integer.parseInt(value); }
        catch (NumberFormatException e) { throw error(400, "Tham số số không hợp lệ"); }
    }

    public static void requireMethod(HttpExchange exchange, String... methods) {
        for (String method : methods) if (method.equals(exchange.getRequestMethod())) return;
        exchange.getResponseHeaders().set("Allow", String.join(", ", methods));
        throw error(405, "Phương thức không được hỗ trợ");
    }

    public static Map<String, Object> ok(String message) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("message", message);
        return result;
    }

    public static void sendJson(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = GSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var out = exchange.getResponseBody()) { out.write(bytes); }
    }
}
