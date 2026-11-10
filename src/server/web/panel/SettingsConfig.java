package server.web.panel;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.net.URI;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import static server.web.panel.PanelSupport.*;

/** DTO metadata and validation. Payment/email values are storage only: no provider consumer exists. */
final class SettingsConfig {
    static final String MASK = "********";
    static final String STORED_ONLY = "Chỉ lưu trong Java panel; không kết nối website PHP, SMTP hay nhà cung cấp thanh toán.";
    static final String LOGO_URL = "/api/panel/logo";
    private static final Map<String, Field> SETTINGS = new LinkedHashMap<>();
    private static final Map<String, Field> WEBSITE = new LinkedHashMap<>();

    private static final class Field {
        final String name, label, group, kind;
        final boolean secret;
        Field(String name, String label, String group, String kind, boolean secret) {
            this.name = name; this.label = label; this.group = group; this.kind = kind; this.secret = secret;
        }
    }

    static {
        add(SETTINGS, "appName", "Tên ứng dụng", "site", "text", false);
        add(SETTINGS, "fname", "Tên hiển thị", "site", "text", false);
        add(SETTINGS, "ftitle", "Tiêu đề", "site", "text", false);
        add(SETTINGS, "domain", "URL website", "site", "url", false);
        add(SETTINGS, "supportEmail", "Email hỗ trợ", "site", "text", false);
        add(SETTINGS, "fgmail", "Tài khoản Gmail CSKH (chỉ lưu)", "site", "text", false);
        add(SETTINGS, "fpgmail", "Mật khẩu ứng dụng Gmail (chỉ lưu)", "site", "text", true);
        add(SETTINGS, "zalo", "URL Zalo hỗ trợ", "site", "url", false);
        add(SETTINGS, "zalo2", "URL cộng đồng Zalo", "site", "url", false);
        add(SETTINGS, "logo", "Logo", "site", "logo", false);
        add(WEBSITE, "site_name", "Tên website", "site", "text", false);
        add(WEBSITE, "site_title", "Tiêu đề website", "site", "text", false);
        add(WEBSITE, "site_url", "URL website", "site", "url", false);
        add(WEBSITE, "support_email", "Email hỗ trợ", "site", "text", false);
        add(WEBSITE, "zalo_url", "URL Zalo hỗ trợ", "site", "url", false);
        add(WEBSITE, "download_android", "Link tải Android", "download", "url", false);
        add(WEBSITE, "download_ios", "Link tải iOS", "download", "url", false);
        add(WEBSITE, "download_pc", "Link tải PC", "download", "url", false);
        add(WEBSITE, "download_java", "Link tải Java", "download", "url", false);
        add(WEBSITE, "card_partner_id", "Mã đối tác thẻ cào", "card", "text", false);
        add(WEBSITE, "card_partner_key", "Khóa đối tác thẻ cào", "card", "text", true);
        add(WEBSITE, "card_rate", "Tỷ lệ quy đổi thẻ cào", "card", "int", false);
        add(WEBSITE, "payos_client_id", "PayOS client ID", "payos", "text", false);
        add(WEBSITE, "payos_api_key", "PayOS API key", "payos", "text", true);
        add(WEBSITE, "payos_checksum_key", "PayOS checksum key", "payos", "text", true);
        add(WEBSITE, "bank_name", "Tên ngân hàng", "bank", "text", false);
        add(WEBSITE, "bank_account", "Số tài khoản", "bank", "text", false);
        add(WEBSITE, "bank_owner", "Chủ tài khoản", "bank", "text", false);
        add(WEBSITE, "bank_transfer_prefix", "Tiền tố nội dung chuyển khoản", "bank", "text", false);
        add(WEBSITE, "bank_signature", "Chữ ký webhook ngân hàng (chỉ lưu)", "bank", "text", true);
        add(WEBSITE, "bank_rate", "Tỷ lệ quy đổi ngân hàng", "bank", "int", false);
    }

    private SettingsConfig() {}
    private static void add(Map<String, Field> fields, String name, String label, String group, String kind, boolean secret) {
        fields.put(name, new Field(name, label, group, kind, secret));
    }

    static JsonObject merge(JsonObject current, JsonObject changes, boolean website) {
        Map<String, Field> fields = website ? WEBSITE : SETTINGS;
        JsonObject result = current.deepCopy();
        for (Map.Entry<String, JsonElement> entry : changes.entrySet()) {
            Field field = fields.get(entry.getKey());
            if (field == null) throw error(400, "Trường cấu hình không được hỗ trợ: " + entry.getKey());
            JsonElement raw = entry.getValue();
            if (!raw.isJsonNull() && (!raw.isJsonPrimitive() || !raw.getAsJsonPrimitive().isString()
                    && !raw.getAsJsonPrimitive().isNumber())) throw error(400, "Giá trị không hợp lệ: " + field.name);
            String value = raw.isJsonNull() ? "" : raw.getAsString();
            if (field.secret && !raw.isJsonNull() && (value.isEmpty() || MASK.equals(value))) continue;
            if (!field.secret) value = value.trim();
            if (value.length() > (field.secret ? 4096 : 2048) || value.indexOf('\0') >= 0) {
                throw error(400, "Giá trị quá dài hoặc không hợp lệ: " + field.name);
            }
            if ("url".equals(field.kind) && !value.isEmpty()) validateUrl(value, field.name);
            if ("logo".equals(field.kind) && !value.isEmpty() && !LOGO_URL.equals(value)) {
                throw error(400, "Logo phải được tải lên API của panel");
            }
            if ("int".equals(field.kind) && !value.isEmpty()) {
                long number = longValue(changes, field.name, 0);
                if (number < 0 || number > 1000000000L) throw error(400, "Số ngoài giới hạn: " + field.name);
                value = Long.toString(number);
            }
            result.addProperty(field.name, value);
        }
        return result;
    }

    static JsonObject validateRestore(JsonObject object) {
        // A private backup must still contain only this module's defined scalar values.
        JsonObject checked = merge(new JsonObject(), object, true);
        for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
            Field field = WEBSITE.get(entry.getKey());
            if (field != null && field.secret && entry.getValue().isJsonPrimitive()
                    && !MASK.equals(entry.getValue().getAsString())) checked.add(entry.getKey(), entry.getValue());
        }
        return checked;
    }

    static JsonObject masked(JsonObject object, boolean website) {
        JsonObject result = new JsonObject();
        for (Field field : (website ? WEBSITE : SETTINGS).values()) {
            String value = text(object, field.name, "");
            result.addProperty(field.name, field.secret && !value.isEmpty() ? MASK : value);
        }
        return result;
    }

    static Map<String, Boolean> secretsSet(JsonObject object, boolean website) {
        Map<String, Boolean> result = new LinkedHashMap<>();
        for (Field field : (website ? WEBSITE : SETTINGS).values()) {
            if (field.secret) result.put(field.name, !text(object, field.name, "").isEmpty());
        }
        return result;
    }

    static List<Map<String, Object>> fields() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Field field : WEBSITE.values()) {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("name", field.name); row.put("label", field.label);
            row.put("group", field.group); row.put("kind", field.kind);
            row.put("secret", field.secret); row.put("storedOnly", true);
            row.put("hint", STORED_ONLY + (field.secret
                    ? " Giữ nguyên " + MASK + " hoặc để trống để không đổi; null để xóa khóa." : ""));
            result.add(row);
        }
        return result;
    }

    static List<Map<String, String>> groups() {
        return List.of(Map.of("id", "site", "label", "Website (chỉ lưu)"),
                Map.of("id", "download", "label", "Link tải (chỉ lưu)"),
                Map.of("id", "card", "label", "Thẻ cào (chưa kết nối)"),
                Map.of("id", "payos", "label", "PayOS (chưa kết nối)"),
                Map.of("id", "bank", "label", "Ngân hàng (chưa kết nối)"));
    }

    private static void validateUrl(String value, String name) {
        try {
            URI uri = URI.create(value);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null || uri.getRawUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException e) { throw error(400, "URL http(s) không hợp lệ: " + name); }
    }
}
