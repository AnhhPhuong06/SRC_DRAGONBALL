package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import java.io.IOException;
import java.net.IDN;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import static server.web.panel.PanelSupport.*;

/** Auth/CSRF are enforced by the owning router before invoking this module. */
public final class SettingsCloudflareApi {
    private static final Object CONFIG_LOCK = new Object();
    private static final Object CLOUDFLARE_LOCK = new Object();
    private static final List<String> TYPES = List.of("A", "AAAA", "CNAME", "TXT", "MX", "NS");
    private static final Set<String> PROXYABLE = Set.of("A", "AAAA", "CNAME");
    private static volatile CloudflareClient cloudflare = new CloudflareClient();
    private SettingsCloudflareApi() {}

    public static Object handle(String resource, HttpExchange exchange) throws Exception {
        switch (resource) {
            case "settings":
                synchronized (CONFIG_LOCK) { return settings(exchange); }
            case "website":
                synchronized (CONFIG_LOCK) { return website(exchange); }
            case "logo":
                requireMethod(exchange, "POST");
                synchronized (CONFIG_LOCK) {
                    SettingsLogo.upload(exchange);
                    Map<String, Object> result = ok("Đã tải logo; bấm Lưu cấu hình để áp dụng");
                    result.put("url", SettingsConfig.LOGO_URL);
                    return result;
                }
            case "cloudflare":
                synchronized (CLOUDFLARE_LOCK) { return cloudflare(exchange); }
            default: throw error(404, "Không có tài nguyên cấu hình này");
        }
    }

    /** GET/HEAD /api/panel/logo: router must call directly, not serialize a JSON response. */
    public static void serveLogo(HttpExchange exchange) throws IOException { SettingsLogo.serve(exchange); }

    /** Tests replace the transport, not the API base URL. Close restores the previous client. */
    public static AutoCloseable useCloudflareTransportForTests(CloudflareClient.Transport transport) {
        synchronized (CLOUDFLARE_LOCK) {
            CloudflareClient previous = cloudflare;
            CloudflareClient replacement = new CloudflareClient(transport);
            cloudflare = replacement;
            return () -> {
                synchronized (CLOUDFLARE_LOCK) {
                    if (cloudflare == replacement) cloudflare = previous;
                }
            };
        }
    }

    private static Object settings(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET", "POST");
        JsonObject config = PanelFiles.readJson("settings.json");
        if ("POST".equals(exchange.getRequestMethod())) {
            config = SettingsConfig.merge(config, body(exchange), false);
            PanelFiles.writeJson("settings.json", config);
            Map<String, Object> result = ok("Đã lưu cấu hình Java panel; thông tin SMTP chỉ được lưu, chưa kết nối dịch vụ");
            result.put("settings", settingsDto(config));
            return result;
        }
        return settingsDto(config);
    }

    private static JsonObject settingsDto(JsonObject config) {
        JsonObject result = SettingsConfig.masked(config, false);
        JsonObject meta = new JsonObject();
        meta.addProperty("storage", "settings.json");
        meta.addProperty("secretMask", SettingsConfig.MASK);
        meta.add("secretSet", GSON.toJsonTree(SettingsConfig.secretsSet(config, false)));
        meta.addProperty("message", SettingsConfig.STORED_ONLY);
        meta.addProperty("paymentRuntimeConnected", false);
        result.add("_meta", meta);
        return result;
    }

    private static Object website(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET", "POST");
        String action = query(exchange).getOrDefault("action", "");
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!action.isEmpty()) throw error(400, "GET website không nhận action");
            return websiteDto("Cấu hình chỉ lưu trong Java panel");
        }
        JsonObject request = body(exchange);
        JsonObject next;
        String message;
        switch (action) {
            case "save":
                if (!request.has("values") || !request.get("values").isJsonObject()) throw error(400, "Thiếu đối tượng values");
                next = SettingsConfig.merge(PanelFiles.readJson("website.json"), request.getAsJsonObject("values"), true);
                message = "Đã lưu cấu hình Java panel; chưa kết nối website PHP hoặc nhà cung cấp thanh toán";
                break;
            case "restore":
                next = SettingsConfig.validateRestore(PanelFiles.restoreWebsite(text(request, "backup", "").trim()));
                message = "Đã phục hồi cấu hình Java panel; không thay đổi website PHP hoặc thanh toán đang chạy";
                break;
            default: throw error(400, "Thao tác website không được hỗ trợ");
        }
        PanelFiles.backupWebsite();
        PanelFiles.writeJson("website.json", next);
        return websiteDto(message);
    }

    private static Map<String, Object> websiteDto(String message) throws IOException {
        JsonObject config = PanelFiles.readJson("website.json");
        Map<String, Object> result = ok(message);
        result.put("config", SettingsConfig.masked(config, true));
        result.put("secretSet", SettingsConfig.secretsSet(config, true));
        result.put("secretMask", SettingsConfig.MASK);
        result.put("fields", SettingsConfig.fields());
        result.put("groups", SettingsConfig.groups());
        result.put("storedOnly", true);
        result.put("paymentRuntimeConnected", false);
        Map<String, Object> storage = new LinkedHashMap<>();
        storage.put("mode", "java-panel-json");
        storage.put("path", "website.json");
        storage.put("database", "Java panel");
        storage.put("table", "website.json");
        storage.put("rows", config.size());
        storage.put("updated", PanelFiles.exists("website.json")
                ? java.nio.file.Files.getLastModifiedTime(PanelFiles.root().resolve("website.json")).toInstant().toString() : "");
        storage.put("paymentRuntimeConnected", false);
        result.put("storage", storage);
        result.put("backups", PanelFiles.backups());
        return result;
    }

    private static Object cloudflare(HttpExchange exchange) throws IOException {
        requireMethod(exchange, "GET", "POST");
        String action = query(exchange).getOrDefault("action", "");
        JsonObject config = PanelFiles.readJson("cloudflare.json");
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!action.isEmpty()) throw error(400, "GET Cloudflare không nhận action");
            return cloudflareDto(config, "");
        }
        JsonObject request = body(exchange);
        String message;
        switch (action) {
            case "token": {
                String token = required(request, "token");
                JsonObject verified = cloudflare.object("GET", "/user/tokens/verify", token, null);
                if (!"active".equals(text(verified, "status", ""))) throw error(400, "API Token chưa active");
                JsonArray zones = cloudflare.list("/zones", token);
                config = new JsonObject();
                config.addProperty("token", token);
                PanelFiles.writeJson("cloudflare.json", config);
                Map<String, Object> result = cloudflareBase(config, "Đã kiểm tra và lưu token; hãy chọn zone");
                result.put("zones", zonesDto(zones));
                return result;
            }
            case "forget":
                // Tokens are deliberately excluded from config backups so forget leaves no saved copy.
                config = new JsonObject();
                PanelFiles.writeJson("cloudflare.json", config);
                return cloudflareBase(config, "Đã xóa token Cloudflare khỏi Java panel");
            case "zone": {
                String zoneId = identifier(required(request, "zoneId"));
                String token = token(config);
                JsonObject zone = cloudflare.object("GET", "/zones/" + zoneId, token, null);
                String zoneName = host(required(zone, "name"), false);
                if (!zoneId.equals(text(zone, "id", ""))) throw error(502, "Zone Cloudflare không khớp");
                config.addProperty("zoneId", zoneId);
                config.addProperty("zoneName", zoneName);
                PanelFiles.writeJson("cloudflare.json", config);
                message = "Đã chọn zone " + zoneName;
                break;
            }
            case "record-create":
            case "record-update": {
                String token = token(config), zoneId = selectedZone(config);
                JsonObject record = record(request, required(config, "zoneName"));
                String path = "/zones/" + zoneId + "/dns_records";
                boolean update = "record-update".equals(action);
                if (update) path += "/" + identifier(required(request, "id"));
                cloudflare.object(update ? "PUT" : "POST", path, token, record);
                message = update ? "Đã sửa bản ghi DNS" : "Đã tạo bản ghi DNS";
                break;
            }
            case "record-delete":
                cloudflare.object("DELETE", "/zones/" + selectedZone(config) + "/dns_records/"
                        + identifier(required(request, "id")), token(config), null);
                message = "Đã xóa bản ghi DNS";
                break;
            case "purge": {
                JsonObject purge = new JsonObject();
                purge.addProperty("purge_everything", true);
                cloudflare.object("POST", "/zones/" + selectedZone(config) + "/purge_cache", token(config), purge);
                message = "Đã gửi yêu cầu xóa toàn bộ cache của zone";
                break;
            }
            default: throw error(400, "Thao tác Cloudflare không được hỗ trợ");
        }
        return cloudflareDto(config, message);
    }

    private static String token(JsonObject config) {
        String token = text(config, "token", "");
        if (token.isEmpty()) throw error(409, "Chưa lưu API Token Cloudflare");
        return token;
    }

    private static String selectedZone(JsonObject config) {
        String zone = text(config, "zoneId", "");
        if (zone.isEmpty()) throw error(409, "Chưa chọn zone Cloudflare");
        return identifier(zone);
    }

    private static String identifier(String value) {
        if (!value.matches("[a-fA-F0-9]{32}")) throw error(400, "Cloudflare ID không hợp lệ");
        return value.toLowerCase(Locale.ROOT);
    }

    private static Map<String, Object> cloudflareBase(JsonObject config, String message) {
        Map<String, Object> result = ok(message);
        boolean tokenSet = !text(config, "token", "").isEmpty();
        result.put("configured", tokenSet);
        result.put("tokenSet", tokenSet);
        result.put("zoneId", text(config, "zoneId", ""));
        result.put("zoneName", text(config, "zoneName", ""));
        result.put("storage", "Java panel · cloudflare.json (riêng tư)");
        result.put("types", TYPES);
        result.put("proxyable", List.of("A", "AAAA", "CNAME"));
        List<Map<String, Object>> ttl = new ArrayList<>();
        for (int value : new int[] {1, 60, 120, 300, 600, 1800, 3600, 7200, 86400}) {
            ttl.add(Map.of("value", value, "label", value == 1 ? "Tự động" : value + " giây"));
        }
        result.put("ttlOptions", ttl);
        result.put("zones", List.of());
        result.put("records", List.of());
        return result;
    }

    private static Map<String, Object> cloudflareDto(JsonObject config, String message) {
        Map<String, Object> result = cloudflareBase(config, message);
        if (!(Boolean) result.get("tokenSet")) return result;
        try {
            String token = token(config);
            result.put("zones", zonesDto(cloudflare.list("/zones", token)));
            if (!text(config, "zoneId", "").isEmpty()) {
                JsonArray records = cloudflare.list("/zones/" + selectedZone(config) + "/dns_records", token);
                List<Map<String, Object>> rows = new ArrayList<>();
                for (JsonElement value : records) {
                    JsonObject record = value.getAsJsonObject();
                    Map<String, Object> row = new LinkedHashMap<>();
                    for (String key : List.of("id", "type", "name", "content")) row.put(key, text(record, key, ""));
                    row.put("ttl", integer(record, "ttl", 1));
                    row.put("proxied", bool(record, "proxied", false));
                    row.put("priority", integer(record, "priority", 0));
                    rows.add(row);
                }
                result.put("records", rows);
            }
        } catch (PanelException e) {
            result.put("error", e.getMessage());
            if (message.isEmpty()) result.put("message", "Không đọc được trạng thái Cloudflare");
        }
        return result;
    }

    private static List<Map<String, String>> zonesDto(JsonArray zones) {
        List<Map<String, String>> result = new ArrayList<>();
        for (JsonElement value : zones) {
            JsonObject zone = value.getAsJsonObject();
            result.add(Map.of("id", text(zone, "id", ""), "name", text(zone, "name", ""), "status", text(zone, "status", "")));
        }
        return result;
    }

    private static JsonObject record(JsonObject request, String zoneName) {
        String type = required(request, "type").toUpperCase(Locale.ROOT);
        if (!TYPES.contains(type)) throw error(400, "Loại bản ghi không được hỗ trợ");
        String name = required(request, "name");
        if ("@".equals(name)) name = zoneName;
        else {
            name = host(name, true);
            if (!name.contains(".")) name += "." + zoneName;
        }
        if (!(name.equals(zoneName) || name.endsWith("." + zoneName))) throw error(400, "Tên bản ghi phải nằm trong zone đã chọn");
        String content = required(request, "content");
        if (content.length() > 4096 || content.indexOf('\0') >= 0) throw error(400, "Nội dung DNS quá dài hoặc không hợp lệ");
        switch (type) {
            case "A":
                if (!content.matches("(?:[0-9]{1,3}\\.){3}[0-9]{1,3}")) throw error(400, "Địa chỉ IPv4 không hợp lệ");
                for (String part : content.split("\\.")) if (Integer.parseInt(part) > 255) throw error(400, "Địa chỉ IPv4 không hợp lệ");
                break;
            case "AAAA":
                try {
                    if (!content.matches("[0-9a-fA-F:]+") || !content.contains(":")
                            || !(InetAddress.getByName(content) instanceof Inet6Address)) throw new IllegalArgumentException();
                } catch (Exception e) { throw error(400, "Địa chỉ IPv6 không hợp lệ"); }
                break;
            case "CNAME": case "MX": case "NS": content = host(content, false); break;
            default: break;
        }
        int ttl = integer(request, "ttl", 1);
        if (ttl != 1 && (ttl < 60 || ttl > 86400)) throw error(400, "TTL phải là 1 hoặc từ 60 đến 86400 giây");
        boolean proxied = bool(request, "proxied", false);
        if (proxied && !PROXYABLE.contains(type)) throw error(400, "Loại bản ghi này không thể bật proxy");
        JsonObject record = new JsonObject();
        record.addProperty("type", type);
        record.addProperty("name", name);
        record.addProperty("content", content);
        record.addProperty("ttl", proxied ? 1 : ttl);
        record.addProperty("proxied", proxied);
        if ("MX".equals(type)) {
            int priority = integer(request, "priority", 0);
            if (priority < 0 || priority > 65535) throw error(400, "Priority phải từ 0 đến 65535");
            record.addProperty("priority", priority);
        }
        return record;
    }

    /** Pure syntax checks; no DNS lookup of user-supplied hosts or URLs. */
    private static String host(String input, boolean recordName) {
        String value = input.endsWith(".") ? input.substring(0, input.length() - 1) : input;
        try {
            StringBuilder ascii = new StringBuilder();
            String[] labels = value.split("\\.", -1);
            for (int i = 0; i < labels.length; i++) {
                String label = labels[i];
                if (recordName && i == 0 && "*".equals(label)) { /* wildcard record */ }
                else {
                    label = IDN.toASCII(label).toLowerCase(Locale.ROOT);
                    if (!label.matches(recordName ? "[a-z0-9_](?:[a-z0-9_-]{0,61}[a-z0-9_])?"
                            : "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?")) throw new IllegalArgumentException();
                }
                if (i > 0) ascii.append('.');
                ascii.append(label);
            }
            if (ascii.length() == 0 || ascii.length() > 253) throw new IllegalArgumentException();
            return ascii.toString();
        } catch (IllegalArgumentException e) { throw error(400, "Tên DNS không hợp lệ"); }
    }
}
