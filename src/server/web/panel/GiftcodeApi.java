package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.Locale;
import java.util.function.Supplier;
import managers.GiftCodeManager;
import models.GiftCode;
import models.item.Item;
import services.ItemService;
import static server.web.panel.CatalogShopApi.*;
import static server.web.panel.PanelSupport.*;

/** giftcode uses the account pool; its item/option definitions use the data pool. */
final class GiftcodeApi {
    static final int BATCH_SIZE = 50;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String CODE_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private GiftcodeApi() {}

    private record Row(int id, String code, int count, String detail, String allGender,
                       Timestamp created, Timestamp expired, String playerName) {}

    static Object handle(String resource, HttpExchange exchange) throws Exception {
        if (resource.equals("giftcode/batch")) {
            requireMethod(exchange, "POST");
            fields(query(exchange).keySet());
            JsonObject request = body(exchange);
            CatalogData catalog;
            try (Connection con = AlyraManager.getConnection_Data()) { catalog = CatalogData.load(con); }
            GiftCodeManager manager = GiftCodeManager.gI();
            synchronized (manager) {
                try (Connection con = AlyraManager.getConnection()) {
                    return createBatch(request, catalog, con, manager, GiftcodeApi::randomSuffix);
                }
            }
        }
        boolean sync = resource.equals("giftcode/sync");
        if (sync) requireMethod(exchange, "POST");
        else requireMethod(exchange, "GET", "POST", "PUT", "DELETE");
        Map<String, String> query = query(exchange);
        fields(query.keySet(), sync ? new String[0] : new String[]{"id"});
        String method = exchange.getRequestMethod();
        JsonObject body = !method.equals("GET") ? body(exchange) : new JsonObject();
        if (sync || method.equals("DELETE")) fields(body);
        else fields(body, "id", "code", "countLeft", "items", "options", "type", "expired", "datecreate", "allGender", "playerName");
        rejectNull(body, "type", "playerName");
        if (!sync && method.equals("POST") && (query.containsKey("id") || body.has("id"))) {
            throw error(400, "ID do cơ sở dữ liệu cấp khi tạo giftcode");
        }
        int targetId = !sync && (method.equals("PUT") || method.equals("DELETE") || query.containsKey("id"))
                ? queryId(query, "id", true) : 0;
        if (body.has("id") && id(body, "id", true) != targetId) throw error(400, "ID URL và nội dung không khớp");
        if (body.has("type") && !body.get("type").isJsonNull() && !text(body, "type", "").isBlank()) {
            throw error(422, "Giftcode của máy chủ không có phân loại type");
        }
        CatalogData catalog;
        try (Connection con = AlyraManager.getConnection_Data()) { catalog = CatalogData.load(con); }
        if (!sync && method.equals("GET")) {
            try (Connection con = AlyraManager.getConnection()) {
                if (targetId > 0) return dto(find(con, targetId, false), catalog);
                List<Map<String, Object>> result = new ArrayList<>();
                try (PreparedStatement ps = con.prepareStatement("SELECT * FROM giftcode ORDER BY id DESC");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) result.add(dto(row(rs), catalog));
                }
                return result;
            }
        }
        GiftCodeManager manager = GiftCodeManager.gI();
        synchronized (manager) {
            try (Connection con = AlyraManager.getConnection()) {
                con.setAutoCommit(false);
                try {
                    int resultId = targetId;
                    if (!sync && method.equals("DELETE")) {
                        find(con, targetId, true);
                        if (execute(con, "DELETE FROM giftcode WHERE id=?", targetId) != 1) throw error(409, "Giftcode đã thay đổi");
                    } else if (!sync) {
                        Row old = method.equals("PUT") ? find(con, targetId, true) : null;
                        if (old != null && body.keySet().stream().allMatch(k -> k.equals("id") || k.equals("type"))) {
                            throw error(400, "Không có trường để cập nhật");
                        }
                        String code = string(body, "code", old == null ? null : old.code, 128, false);
                        if (code == null) throw error(400, "Thiếu mã giftcode");
                        int count = integer(body, "countLeft", old == null ? 100 : old.count);
                        // The live manager excludes -1 in SQL; never advertise it as unlimited.
                        if (count < 0) throw error(422, "countLeft phải >= 0; máy chủ không hỗ trợ lượt vô hạn");
                        Timestamp created = date(body, "datecreate", old == null ? Timestamp.valueOf(LocalDateTime.now()) : old.created);
                        Timestamp expired = date(body, "expired", old == null ? Timestamp.valueOf(LocalDateTime.now().plusDays(30)) : old.expired);
                        if (expired == null) throw error(422, "Cột expired không cho phép NULL");
                        if (created != null && expired.before(created)) throw error(400, "Hạn dùng phải sau ngày tạo");
                        boolean gender = body.has("allGender") ? bool(body, "allGender", true) : old == null || allGender(old.allGender);
                        String player = old == null ? null : old.playerName;
                        if (body.has("playerName")) player = body.get("playerName").isJsonNull() ? null
                                : string(body, "playerName", null, 4096, true);
                        JsonArray detail = body.has("items") ? rewards(array(body, "items", 200), catalog)
                                : old == null ? new JsonArray() : storedRewards(old, catalog);
                        if (detail.size() == 0) throw error(400, "Giftcode phải có ít nhất một phần thưởng");
                        if (body.has("options")) {
                            JsonArray common = catalog.validateOptions(array(body, "options", 255));
                            if (common.size() > 0) {
                                for (JsonElement element : detail) {
                                    JsonObject reward = element.getAsJsonObject();
                                    if (reward.getAsJsonArray("options").size() == 0 && integer(reward, "id", 0) >= 0) {
                                        reward.add("options", copy(common));
                                    }
                                }
                            }
                        }
                        try (PreparedStatement ps = statement(con,
                                "SELECT id FROM giftcode WHERE LOWER(code)=LOWER(?) AND id<>? FOR UPDATE", code, targetId);
                                ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) throw error(409, "Mã giftcode đã tồn tại");
                        }
                        if (old == null) {
                            resultId = insert(con, "INSERT INTO giftcode(code,count_left,detail,allGender,datecreate,expired,player_name) "
                                    + "VALUES(?,?,?,?,?,?,?)", code, count, detail.toString(), gender ? "all" : "false", created, expired, player);
                        } else {
                            // Explicit datecreate prevents its ON UPDATE clause from resetting creation time.
                            execute(con, "UPDATE giftcode SET code=?,count_left=?,detail=?,allGender=?,datecreate=?,expired=?,player_name=? WHERE id=?",
                                    code, count, detail.toString(), gender ? "all" : "false", created, expired, player, targetId);
                        }
                    }
                    List<GiftCode> loaded = active(con, catalog);
                    con.commit();
                    // init() swallows SQL/JSON failures and clears live state first. Stage completely instead.
                    manager.listGiftCode.clear();
                    manager.listGiftCode.addAll(loaded);
                    Map<String, Object> result = ok(sync ? "Đã đồng bộ giftcode vào máy chủ" : "Đã lưu và đồng bộ giftcode");
                    result.put("id", resultId);
                    result.put("loaded", loaded.size());
                    result.put("synced", true);
                    return result;
                } catch (Exception e) { rollback(con, e); throw e; }
            }
        }
    }

    private static String randomSuffix() {
        StringBuilder code = new StringBuilder(16);
        for (int i = 0; i < 16; i++) code.append(CODE_ALPHABET.charAt(RANDOM.nextInt(CODE_ALPHABET.length())));
        return code.toString();
    }

    static Object createBatch(JsonObject body, CatalogData catalog, Connection con,
            GiftCodeManager manager, Supplier<String> suffix) throws Exception {
        fields(body, "prefix", "items", "expired");
        rejectNull(body);
        String prefix = string(body, "prefix", "", 32, true);
        if (!prefix.matches("[A-Za-z0-9_-]*")) throw error(400, "Tiền tố chỉ gồm chữ không dấu, số, dấu - hoặc _");
        JsonArray detail = rewards(array(body, "items", 200), catalog);
        if (detail.size() == 0) throw error(400, "Giftcode phải có ít nhất một phần thưởng");
        Timestamp created = Timestamp.valueOf(LocalDateTime.now());
        Timestamp expired = date(body, "expired", Timestamp.valueOf(LocalDateTime.now().plusDays(30)));
        if (!expired.after(created)) throw error(400, "Hạn sử dụng phải ở tương lai");
        synchronized (manager) {
            con.setAutoCommit(false);
            try {
                Set<String> existing = new HashSet<>();
                try (PreparedStatement ps = con.prepareStatement("SELECT code FROM giftcode FOR UPDATE"); ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String code = rs.getString("code");
                        if (code != null) existing.add(code.toLowerCase(Locale.ROOT));
                    }
                }
                List<String> codes = new ArrayList<>();
                for (int attempts = 0; codes.size() < BATCH_SIZE && attempts < 2000; attempts++) {
                    String code = (prefix.isEmpty() ? "" : prefix + "-") + suffix.get();
                    if (!existing.add(code.toLowerCase(Locale.ROOT))) continue;
                    insert(con, "INSERT INTO giftcode(code,count_left,detail,allGender,datecreate,expired,player_name) VALUES(?,?,?,?,?,?,?)",
                            code, 1, detail.toString(), "all", created, expired, null);
                    codes.add(code);
                }
                if (codes.size() != BATCH_SIZE) throw error(409, "Không tạo đủ mã khác nhau; hãy thử lại");
                List<GiftCode> loaded = active(con, catalog);
                con.commit();
                manager.listGiftCode.clear();
                manager.listGiftCode.addAll(loaded);
                Map<String, Object> result = ok("Đã tạo 50 giftcode, mỗi mã dùng 1 lần trên toàn máy chủ");
                result.put("codes", codes);
                result.put("count", BATCH_SIZE);
                result.put("countLeft", 1);
                result.put("synced", true);
                return result;
            } catch (Exception e) { rollback(con, e); throw e; }
        }
    }

    private static boolean allGender(String raw) {
        return raw == null || raw.equalsIgnoreCase("all") || raw.equalsIgnoreCase("true") || raw.equals("1");
    }

    private static Row row(ResultSet rs) throws SQLException {
        return new Row(rs.getInt("id"), rs.getString("code"), rs.getInt("count_left"), rs.getString("detail"),
                rs.getString("allGender"), rs.getTimestamp("datecreate"), rs.getTimestamp("expired"), rs.getString("player_name"));
    }

    private static Row find(Connection con, int id, boolean lock) throws SQLException {
        try (PreparedStatement ps = statement(con, "SELECT * FROM giftcode WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw error(404, "Giftcode không tồn tại");
            return row(rs);
        }
    }

    static Timestamp date(JsonObject body, String key, Timestamp fallback) {
        if (!body.has(key)) return fallback;
        if (body.get(key).isJsonNull()) throw error(422, "Ngày không được NULL: " + key);
        String raw = string(body, key, "", 64, false).replace(' ', 'T');
        try {
            Timestamp value;
            try { value = Timestamp.valueOf(LocalDateTime.parse(raw)); }
            catch (DateTimeParseException e) { value = Timestamp.from(OffsetDateTime.parse(raw).toInstant()); }
            // MariaDB/MySQL TIMESTAMP in the supplied schema is signed 32-bit epoch seconds.
            long seconds = Math.floorDiv(value.getTime(), 1000L);
            if (seconds < 1 || seconds > Integer.MAX_VALUE) throw error(400, "Ngày ngoài giới hạn TIMESTAMP: " + key);
            return value;
        } catch (DateTimeParseException e) { throw error(400, "Ngày không hợp lệ: " + key); }
    }

    private static JsonArray rewards(JsonArray input, CatalogData catalog) {
        return rewards(input, catalog, false);
    }

    private static JsonArray storedRewards(Row row, CatalogData catalog) {
        return rewards(storedArray(row.detail, "giftcode #" + row.id), catalog, true);
    }

    private static JsonArray rewards(JsonArray input, CatalogData catalog, boolean stored) {
        if (input.size() > 200) throw error(400, "Tối đa 200 phần thưởng");
        Map<Integer, JsonObject> byId = new LinkedHashMap<>();
        for (JsonElement element : input) {
            JsonObject reward = object(element);
            fields(reward, "id", "quantity", "option", "options");
            if (!reward.has("id") || reward.get("id").isJsonNull()) throw error(400, "Thiếu ID phần thưởng");
            int id = integer(reward, "id", Integer.MIN_VALUE);
            catalog.item(id, true);
            if (!stored && byId.containsKey(id)) throw error(400, "Mã phần thưởng bị trùng; máy chủ lưu theo ID: " + id);
            int quantity = integer(reward, "quantity", 1);
            if (quantity <= 0) throw error(400, "Số lượng quà phải > 0");
            if (reward.has("option") && reward.has("options")) throw error(400, "Chỉ dùng option hoặc options");
            JsonArray options = reward.has("option") ? catalog.validateOptions(array(reward, "option", 255))
                    : reward.has("options") ? catalog.validateOptions(array(reward, "options", 255)) : new JsonArray();
            if (id < 0 && options.size() > 0) throw error(422, "Tiền tệ không sử dụng option");
            JsonObject saved = new JsonObject();
            saved.addProperty("id", id);
            saved.addProperty("quantity", quantity);
            saved.add("options", options);
            // GiftCodeManager.init() puts rewards in maps keyed by item ID: the last row wins.
            // Preserve that legacy meaning on reads, while rejecting duplicates in new input.
            byId.put(id, saved);
        }
        JsonArray result = new JsonArray();
        for (JsonObject reward : byId.values()) result.add(reward);
        return result;
    }

    private static Map<String, Object> dto(Row row, CatalogData catalog) {
        List<Map<String, Object>> items = new ArrayList<>();
        for (JsonElement element : storedRewards(row, catalog)) {
            JsonObject reward = element.getAsJsonObject();
            int id = integer(reward, "id", 0);
            Map<String, Object> item = catalog.item(id, true);
            JsonArray options = reward.getAsJsonArray("options");
            JsonArray defaults = new JsonArray();
            if (id >= 0 && options.size() == 0 && ItemService.gI().getTemplate(id) != null) {
                Item runtimeItem = ItemService.gI().createNewItem((short) id);
                if (id == 457) runtimeItem.itemOptions.add(new Item.ItemOption(30, 0));
                for (Item.ItemOption option : runtimeItem.itemOptions) {
                    if (option.optionTemplate == null) continue;
                    JsonObject value = new JsonObject();
                    value.addProperty("id", option.optionTemplate.id);
                    value.addProperty("param", option.param);
                    defaults.add(value);
                }
            }
            items.add(map("id", id, "quantity", integer(reward, "quantity", 1), "name", item.get("name"), "icon", item.get("icon"),
                    "option", catalog.optionDtos(options), "inherited", catalog.optionDtos(defaults), "inheritedType", "default"));
        }
        return map("id", row.id, "code", row.code, "countLeft", row.count, "datecreate", format(row.created),
                "expired", format(row.expired), "allGender", allGender(row.allGender), "playerName", row.playerName,
                "items", items, "options", List.of(), "type", null, "used", null,
                "capabilities", map("type", false, "used", false, "globalOptions", false, "unlimited", false));
    }

    private static String format(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toLocalDateTime().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private static List<GiftCode> active(Connection con, CatalogData catalog) throws SQLException {
        List<GiftCode> result = new ArrayList<>();
        Set<String> codes = new HashSet<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT * FROM giftcode WHERE count_left>0 AND (expired IS NULL OR expired>NOW()) ORDER BY id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Row row = row(rs);
                if (row.code == null || row.code.isBlank() || !codes.add(row.code.toLowerCase(java.util.Locale.ROOT))) {
                    throw error(422, "Giftcode có mã rỗng hoặc trùng: #" + row.id);
                }
                GiftCode gift = new GiftCode();
                gift.id = row.id;
                gift.code = row.code;
                gift.countLeft = row.count;
                gift.allGender = allGender(row.allGender);
                gift.datecreate = row.created;
                gift.dateexpired = row.expired;
                gift.playerName = row.playerName;
                JsonArray rewards = storedRewards(row, catalog);
                if (rewards.size() == 0) throw error(422, "Giftcode không có phần thưởng: #" + row.id);
                for (JsonElement element : rewards) {
                    JsonObject reward = element.getAsJsonObject();
                    int id = integer(reward, "id", 0);
                    if (id >= 0) catalog.runtimeItem(id);
                    JsonArray options = reward.getAsJsonArray("options");
                    catalog.runtimeOptions(options);
                    ArrayList<Item.ItemOption> runtime = new ArrayList<>();
                    for (JsonElement option : options) runtime.add(new Item.ItemOption(integer(option.getAsJsonObject(), "id", 0),
                            integer(option.getAsJsonObject(), "param", 0)));
                    gift.detail.put(id, integer(reward, "quantity", 1));
                    gift.option.put(id, runtime);
                }
                result.add(gift);
            }
        }
        return result;
    }
}
