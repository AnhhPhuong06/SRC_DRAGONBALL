package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import database.daos.ShopDAO;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.shop.ItemShop;
import models.shop.Shop;
import models.shop.TabShop;
import server.Manager;
import services.ToriBotItemShop;
import static server.web.panel.CatalogShopApi.*;
import static server.web.panel.PanelSupport.*;

/** Shops and tabs are rows; shop items/options are embedded JSON in tab_shop.items. */
final class ShopApi {
    private static final Object LOCK = new Object();
    private static final Set<String> ACTIONS = Set.of("shop-create", "shop-update", "shop-delete", "tab-create", "tab-update",
            "tab-delete", "item-create", "item-update", "item-delete", "sync");
    private static final Set<Integer> SHOP_TYPES = Set.of(0, 1, 3);
    private static final Set<Integer> SELL_TYPES = Set.of(0, 1, 3, 4);
    private static final String[] ITEM_FIELDS = {"id", "tabId", "itemsVersion", "tempId", "cost", "costGold", "typeSell",
            "iconSpec", "isNew", "isSell", "options"};
    private record ShopRow(int id, int npc, String tag, int type) {}
    private record TabRow(int id, int shop, String name, int index, String items) {}
    private record Snapshot(Map<Integer, ShopRow> shops, Map<Integer, TabRow> tabs, Map<Integer, String> npcs) {}

    private ShopApi() {}

    static Object handle(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET", "POST");
        Map<String, String> query = query(exchange);
        if (exchange.getRequestMethod().equals("GET")) {
            fields(query.keySet(), "shop");
            try (Connection con = AlyraManager.getConnection_Data()) {
                con.setAutoCommit(false);
                try {
                    Snapshot snapshot = snapshot(con);
                    Object result = query.containsKey("shop")
                            ? map("shop", detail(snapshot, queryId(query, "shop", true), CatalogData.load(con))) : index(snapshot);
                    con.commit();
                    return result;
                } catch (Exception e) { rollback(con, e); throw e; }
            }
        }
        fields(query.keySet(), "action");
        String action = query.getOrDefault("action", "");
        if (!ACTIONS.contains(action)) throw error(400, "Thao tác shop không được hỗ trợ");
        JsonObject body = body(exchange);
        rejectNull(body);
        switch (action) {
            case "shop-create" -> fields(body, "npcId", "tagName", "typeShop");
            case "shop-update" -> fields(body, "id", "npcId", "tagName", "typeShop");
            case "shop-delete", "tab-delete" -> fields(body, "id");
            case "tab-create" -> fields(body, "shopId", "name", "index");
            case "tab-update" -> fields(body, "id", "name", "index");
            case "item-create", "item-update" -> fields(body, ITEM_FIELDS);
            case "item-delete" -> fields(body, "id", "tabId", "itemsVersion");
            case "sync" -> fields(body);
        }
        synchronized (LOCK) {
            try (Connection con = AlyraManager.getConnection_Data()) {
                con.setAutoCommit(false);
                try {
                    CatalogData catalog = CatalogData.load(con);
                    int id = mutate(con, action, body, catalog);
                    Snapshot snapshot = snapshot(con);
                    // Both DAO and Manager.reloadShops swallow failures. Validate the whole staged graph,
                    // then verify the DAO result before committing/publishing (also permits an empty shop list).
                    List<Shop> loaded = stage(con, snapshot, catalog);
                    con.commit();
                    Manager.SHOPS = loaded;
                    Map<String, Object> result = ok(action.equals("sync") ? "Đã đồng bộ shop vào máy chủ" : "Đã lưu và đồng bộ shop");
                    result.put("id", id);
                    result.put("loaded", loaded.size());
                    result.put("synced", true);
                    return result;
                } catch (Exception e) { rollback(con, e); throw e; }
            }
        }
    }

    private static int mutate(Connection con, String action, JsonObject body, CatalogData catalog) throws Exception {
        if (action.equals("sync")) return 0;
        if (action.startsWith("shop-")) {
            boolean create = action.equals("shop-create");
            int id = create ? 0 : id(body, "id", true);
            ShopRow old = create ? null : shop(con, id, true);
            if (action.equals("shop-delete")) {
                execute(con, "DELETE FROM tab_shop WHERE shop_id=?", id);
                if (execute(con, "DELETE FROM shop WHERE id=?", id) != 1) throw error(409, "Shop đã thay đổi");
                return id;
            }
            if (!create && body.size() == 1) throw error(400, "Không có trường để cập nhật");
            int npc = body.has("npcId") ? id(body, "npcId", false) : create ? -1 : old.npc;
            if (npc < 0 || npc > Byte.MAX_VALUE) throw error(400, "NPC ID phải trong khoảng 0..127");
            try (PreparedStatement ps = statement(con, "SELECT id FROM npc_template WHERE id=?", npc);
                    ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw error(422, "NPC không tồn tại");
            }
            String tag = string(body, "tagName", create ? null : old.tag, 50, false);
            if (tag == null) throw error(400, "Thiếu tagName");
            int type = integer(body, "typeShop", create ? 0 : old.type);
            if (ToriBotItemShop.matches(npc, tag)) type = 3;
            if (!SHOP_TYPES.contains(type)) throw error(422, "Máy chủ chỉ hỗ trợ typeShop 0, 1, 3");
            try (PreparedStatement ps = statement(con, "SELECT id FROM shop WHERE LOWER(tag_name)=LOWER(?) AND id<>? FOR UPDATE", tag, id);
                    ResultSet rs = ps.executeQuery()) {
                if (rs.next()) throw error(409, "Tag shop đã tồn tại");
            }
            if (create) return insert(con, "INSERT INTO shop(npc_id,tag_name,type_shop) VALUES(?,?,?)", npc, tag, type);
            execute(con, "UPDATE shop SET npc_id=?,tag_name=?,type_shop=? WHERE id=?", npc, tag, type, id);
            return id;
        }
        if (action.startsWith("tab-")) {
            boolean create = action.equals("tab-create");
            int id = create ? 0 : id(body, "id", true);
            TabRow old = create ? null : tab(con, id, true);
            int shopId = create ? id(body, "shopId", true) : old.shop;
            shop(con, shopId, true);
            if (action.equals("tab-delete")) {
                if (execute(con, "DELETE FROM tab_shop WHERE id=?", id) != 1) throw error(409, "Tab đã thay đổi");
                return id;
            }
            if (!create && body.size() == 1) throw error(400, "Không có trường để cập nhật");
            String name = string(body, "name", create ? null : old.name.replace("<>", "\n"), 50, false);
            if (name == null) throw error(400, "Thiếu tên tab");
            name = name.replace("\r\n", "\n").replace('\r', '\n').replace("\n", "<>");
            if (name.length() > 50) throw error(400, "Tên tab tối đa 50 ký tự sau khi mã hóa xuống dòng");
            int next = 0;
            if (create) {
                try (PreparedStatement ps = statement(con, "SELECT COALESCE(MAX(tab_index),-1)+1,COUNT(*) FROM tab_shop WHERE shop_id=?", shopId);
                        ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    next = rs.getInt(1);
                    if (rs.getInt(2) >= 255) throw error(422, "Tối đa 255 tab trong một shop");
                }
            }
            int index = integer(body, "index", create ? next : old.index);
            if (index < 0 || index > Short.MAX_VALUE) throw error(400, "Chỉ số tab không hợp lệ");
            if (create) return insert(con, "INSERT INTO tab_shop(shop_id,tab_name,tab_index,items) VALUES(?,?,?,?)", shopId, name, index, "[]");
            execute(con, "UPDATE tab_shop SET tab_name=?,tab_index=? WHERE id=?", name, index, id);
            return id;
        }
        int tabId = id(body, "tabId", true);
        TabRow tab = tab(con, tabId, true);
        boolean create = action.equals("item-create");
        if (create && body.has("id")) throw error(400, "ID vật phẩm do vị trí trong tab quyết định");
        if (!create && !body.has("itemsVersion")) throw error(400, "Thiếu itemsVersion; hãy tải lại tab trước khi sửa/xóa vật phẩm");
        if (body.has("itemsVersion")) {
            String expected = string(body, "itemsVersion", "", 64, false);
            if (!expected.equals(version(tab.items))) throw error(409, "Danh sách vật phẩm đã thay đổi; hãy tải lại shop");
        }
        JsonArray items = storedArray(tab.items, "tab #" + tabId);
        int itemId = create ? items.size() + 1 : id(body, "id", true);
        if (!create && itemId > items.size()) throw error(404, "Vật phẩm không tồn tại trong tab");
        if (!create && !items.get(itemId - 1).isJsonObject()) throw error(404, "Vật phẩm không tồn tại trong tab");
        if (action.equals("item-delete")) items.remove(itemId - 1);
        else {
            if (!create && body.keySet().stream().allMatch(k -> Set.of("id", "tabId", "itemsVersion").contains(k))) {
                throw error(400, "Không có trường để cập nhật");
            }
            JsonObject old = create ? null : object(items.get(itemId - 1));
            ShopRow parent = shop(con, tab.shop, true);
            JsonObject item = editItem(old, body, catalog, parent.type);
            if (ToriBotItemShop.matches(parent.npc, parent.tag)) {
                item.addProperty("type_sell", 4);
                item.addProperty("item_spec", ((Number) catalog.item(ToriBotItemShop.CURRENCY_ID, false).get("icon")).intValue());
            }
            if (create) {
                if (items.size() >= 10000) throw error(422, "Tab quá nhiều bản ghi vật phẩm");
                items.add(item);
            } else items.set(itemId - 1, item);
        }
        execute(con, "UPDATE tab_shop SET items=? WHERE id=?", items.toString(), tabId);
        return itemId;
    }

    private static JsonObject editItem(JsonObject old, JsonObject body, CatalogData catalog, int shopType) {
        if (body.has("costGold") && longValue(body, "costGold", 0) != 0) throw error(422, "Máy chủ không hỗ trợ giá phụ costGold");
        JsonObject item = old == null ? new JsonObject() : new JsonParser().parse(old.toString()).getAsJsonObject();
        int template = body.has("tempId") ? id(body, "tempId", false) : old == null ? -1 : integer(old, "temp_id", -1);
        catalog.item(template, false);
        long cost = longValue(body, "cost", old == null ? 0 : longValue(old, "cost", 0));
        if (cost < 0 || cost > maxCost(shopType)) throw error(400, "Giá phải từ 0 đến " + maxCost(shopType) + " cho loại shop này");
        int sell = integer(body, "typeSell", old == null ? 0 : integer(old, "type_sell", 0));
        if (!SELL_TYPES.contains(sell)) throw error(422, "typeSell chỉ hỗ trợ 0, 1, 3, 4");
        int icon = integer(body, "iconSpec", old == null ? 0 : integer(old, "item_spec", 0));
        if (icon < 0 || icon > Short.MAX_VALUE) throw error(400, "Icon phải từ 0 đến 32767");
        item.addProperty("temp_id", template);
        item.addProperty("cost", cost);
        item.addProperty("type_sell", sell);
        item.addProperty("item_spec", icon);
        item.addProperty("is_new", bool(body, "isNew", old != null && bool(old, "is_new", false)));
        item.addProperty("is_sell", bool(body, "isSell", old == null || bool(old, "is_sell", true)));
        JsonArray options = body.has("options") ? catalog.validateOptions(array(body, "options", 255))
                : old != null && old.has("options") ? catalog.validateOptions(array(old, "options", 255)) : new JsonArray();
        item.add("options", options);
        validateItem(item, catalog, false, shopType);
        return item;
    }

    private static long maxCost(int shopType) {
        // Skill shops send a long cost; other shops send an int. Keep browser integers exact.
        return shopType == 1 ? 9_007_199_254_740_991L : Integer.MAX_VALUE;
    }

    private static void validateItem(JsonObject item, CatalogData catalog, boolean runtime, int shopType) {
        for (String key : List.of("temp_id", "cost", "type_sell", "item_spec", "is_new", "is_sell")) {
            if (!item.has(key) || item.get(key).isJsonNull()) throw error(422, "Vật phẩm shop thiếu trường " + key);
        }
        int template = integer(item, "temp_id", -1);
        catalog.item(template, false);
        if (runtime) catalog.runtimeItem(template);
        long cost = longValue(item, "cost", -1);
        if (cost < 0 || cost > maxCost(shopType)) throw error(422, "Giá shop vượt giới hạn cho loại shop này");
        if (!SELL_TYPES.contains(integer(item, "type_sell", -1))) throw error(422, "Loại tiền trong shop không được hỗ trợ");
        int icon = integer(item, "item_spec", -1);
        if (icon < 0 || icon > Short.MAX_VALUE) throw error(422, "Icon shop không hợp lệ");
        for (String key : List.of("is_new", "is_sell")) {
            String value = text(item, key, "");
            if (!value.equalsIgnoreCase("true") && !value.equalsIgnoreCase("false")) throw error(422, "Cờ shop không phải true/false: " + key);
        }
        JsonArray options = item.has("options") && !item.get("options").isJsonNull()
                ? catalog.validateOptions(array(item, "options", 255)) : new JsonArray();
        for (JsonElement element : options) {
            JsonObject option = element.getAsJsonObject();
            int optionId = integer(option, "id", -1);
            int param = integer(option, "param", 0);
            if (optionId > 255 || param < Short.MIN_VALUE || param > Short.MAX_VALUE) {
                throw error(422, "Option shop vượt giới hạn giao thức (ID 0..255, param -32768..32767)");
            }
        }
        if (runtime) catalog.runtimeOptions(options);
    }

    private static Snapshot snapshot(Connection con) throws SQLException {
        Map<Integer, String> npcs = new LinkedHashMap<>();
        Map<Integer, ShopRow> shops = new LinkedHashMap<>();
        Map<Integer, TabRow> tabs = new LinkedHashMap<>();
        try (PreparedStatement ps = con.prepareStatement("SELECT id,name FROM npc_template ORDER BY id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) npcs.put(rs.getInt(1), rs.getString(2));
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT id,npc_id,tag_name,type_shop FROM shop ORDER BY npc_id,id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                ShopRow row = new ShopRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4));
                shops.put(row.id, row);
            }
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT id,shop_id,tab_name,tab_index,items FROM tab_shop ORDER BY shop_id,tab_index,id"); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                TabRow row = tabRow(rs);
                tabs.put(row.id, row);
            }
        }
        return new Snapshot(shops, tabs, npcs);
    }

    private static ShopRow shop(Connection con, int id, boolean lock) throws SQLException {
        try (PreparedStatement ps = statement(con, "SELECT id,npc_id,tag_name,type_shop FROM shop WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw error(404, "Shop không tồn tại");
            return new ShopRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4));
        }
    }

    private static TabRow tab(Connection con, int id, boolean lock) throws SQLException {
        try (PreparedStatement ps = statement(con, "SELECT id,shop_id,tab_name,tab_index,items FROM tab_shop WHERE id=?" + (lock ? " FOR UPDATE" : ""), id);
                ResultSet rs = ps.executeQuery()) {
            if (!rs.next()) throw error(404, "Tab không tồn tại");
            return tabRow(rs);
        }
    }

    private static TabRow tabRow(ResultSet rs) throws SQLException {
        return new TabRow(rs.getInt(1), rs.getInt(2), rs.getString(3), rs.getInt(4), rs.getString(5));
    }

    private static Object index(Snapshot snapshot) {
        List<Map<String, Object>> shops = new ArrayList<>();
        for (ShopRow shop : snapshot.shops.values()) {
            int tabs = 0, items = 0;
            for (TabRow tab : snapshot.tabs.values()) if (tab.shop == shop.id) {
                tabs++;
                for (JsonElement item : storedArray(tab.items, "tab #" + tab.id)) if (item.isJsonObject()) items++;
            }
            Map<String, Object> dto = shopDto(shop, snapshot);
            dto.put("tabCount", tabs);
            dto.put("itemCount", items);
            shops.add(dto);
        }
        List<Map<String, Object>> npcs = new ArrayList<>();
        snapshot.npcs.forEach((id, name) -> { if (id >= 0 && id <= Byte.MAX_VALUE) npcs.add(map("id", id, "name", name)); });
        return map("shops", shops, "npcs", npcs,
                "typeShops", List.of(map("id", 0, "name", "Shop thường"), map("id", 1, "name", "Học kỹ năng"), map("id", 3, "name", "Shop đặc biệt")),
                "typeSells", List.of(map("id", 0, "name", "Vàng"), map("id", 1, "name", "Ngọc xanh"), map("id", 3, "name", "Ngọc khóa"), map("id", 4, "name", "Điểm / phiếu")),
                "capabilities", map("costGold", false, "itemIdentity", "tab-position", "itemVersionRequired", true));
    }

    private static Map<String, Object> shopDto(ShopRow shop, Snapshot snapshot) {
        return map("id", shop.id, "npcId", shop.npc, "npcName", snapshot.npcs.getOrDefault(shop.npc, "NPC #" + shop.npc),
                "tagName", shop.tag, "typeShop", ToriBotItemShop.matches(shop.npc, shop.tag) ? 3 : shop.type);
    }

    private static Object detail(Snapshot snapshot, int id, CatalogData catalog) throws Exception {
        ShopRow shop = snapshot.shops.get(id);
        if (shop == null) throw error(404, "Shop không tồn tại");
        Map<String, Object> result = shopDto(shop, snapshot);
        if (ToriBotItemShop.matches(shop.npc, shop.tag)) {
            Map<String, Object> currency = catalog.item(ToriBotItemShop.CURRENCY_ID, false);
            result.put("fixedCurrency", map("itemId", ToriBotItemShop.CURRENCY_ID, "name", currency.get("name"), "icon", currency.get("icon")));
        }
        List<Map<String, Object>> tabs = new ArrayList<>();
        for (TabRow tab : snapshot.tabs.values()) if (tab.shop == id) {
            List<Map<String, Object>> items = new ArrayList<>();
            JsonArray raw = storedArray(tab.items, "tab #" + tab.id);
            for (int i = 0; i < raw.size(); i++) {
                // ShopDAO ignores non-object legacy slots. Keep physical array positions as panel IDs.
                if (raw.get(i).isJsonObject()) items.add(itemDto(raw.get(i).getAsJsonObject(), i + 1, tab.id, shop, catalog));
            }
            tabs.add(map("id", tab.id, "shopId", tab.shop, "name", tab.name.replace("<>", "\n"), "index", tab.index,
                    "itemCount", items.size(), "itemsVersion", version(tab.items), "items", items));
        }
        result.put("tabs", tabs);
        return result;
    }

    private static Map<String, Object> itemDto(JsonObject raw, int id, int tabId, ShopRow shop, CatalogData catalog) {
        validateItem(raw, catalog, false, shop.type);
        Map<String, Object> template = catalog.item(integer(raw, "temp_id", -1), false);
        int sell = integer(raw, "type_sell", 0);
        int iconSpec = integer(raw, "item_spec", 0);
        boolean toriCurrency = ToriBotItemShop.matches(shop.npc, shop.tag);
        if (toriCurrency) {
            sell = 4;
            iconSpec = ((Number) catalog.item(ToriBotItemShop.CURRENCY_ID, false).get("icon")).intValue();
        }
        String unit = switch (sell) { case 0 -> "Vàng"; case 1 -> "Ngọc xanh"; case 3 -> "Ngọc khóa"; default -> "Điểm / phiếu"; };
        if (shop.id == 47 && sell == 4) unit = "Điểm Đường Tăng";
        if (shop.id == 48 && sell == 4) unit = "Điểm Xên";
        String spec = "";
        if (iconSpec > 0) for (Map<String, Object> item : catalog.items.values()) if (((Number) item.get("icon")).intValue() == iconSpec) {
            spec = String.valueOf(item.get("name"));
            break;
        }
        if (shop.type == 3 && shop.id != 30 && shop.id != 47 && shop.id != 48) unit = spec.isEmpty() ? "Vật phẩm đặc biệt" : spec;
        if (toriCurrency) unit = "Đá ngũ sắc";
        JsonArray options = raw.has("options") && !raw.get("options").isJsonNull() ? raw.getAsJsonArray("options") : new JsonArray();
        return map("id", id, "tabId", tabId, "tempId", template.get("id"), "name", template.get("name"), "icon", template.get("icon"),
                "cost", longValue(raw, "cost", 0), "costGold", null, "typeSell", sell, "typeSellName", toriCurrency ? "Đá ngũ sắc" : switch (sell) {
                    case 0 -> "Vàng"; case 1 -> "Ngọc xanh"; case 3 -> "Ngọc khóa"; default -> "Điểm / phiếu";
                }, "unitName", unit, "iconSpec", iconSpec, "specName", spec, "isNew", bool(raw, "is_new", false),
                "isSell", bool(raw, "is_sell", false), "options", catalog.optionDtos(options));
    }

    static String version(String raw) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8)));
    }

    private static List<Shop> stage(Connection con, Snapshot snapshot, CatalogData catalog) throws SQLException {
        Set<String> tags = new HashSet<>();
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (ShopRow shop : snapshot.shops.values()) {
            if (!snapshot.npcs.containsKey(shop.npc) || shop.npc < 0 || shop.npc > Byte.MAX_VALUE || !SHOP_TYPES.contains(shop.type)
                    || shop.tag == null || shop.tag.isBlank() || !tags.add(shop.tag.toLowerCase(java.util.Locale.ROOT))) {
                throw error(422, "Cấu hình shop không hợp lệ hoặc trùng tag: #" + shop.id);
            }
        }
        for (TabRow tab : snapshot.tabs.values()) {
            if (!snapshot.shops.containsKey(tab.shop) || tab.name == null || tab.index < 0) throw error(422, "Tab shop không hợp lệ: #" + tab.id);
            if (counts.merge(tab.shop, 1, Integer::sum) > 255) throw error(422, "Shop vượt 255 tab");
            int visible = 0;
            for (JsonElement element : storedArray(tab.items, "tab #" + tab.id)) {
                if (!element.isJsonObject()) continue;
                JsonObject item = object(element);
                validateItem(item, catalog, true, snapshot.shops.get(tab.shop).type);
                if (bool(item, "is_sell", false)) visible++;
            }
            if (visible > 255) throw error(422, "Tab vượt 255 vật phẩm đang bán: #" + tab.id);
        }
        List<Shop> loaded = ShopDAO.getShops(con);
        if (loaded == null || loaded.size() != snapshot.shops.size()) throw error(500, "ShopDAO không nạp đầy đủ shop; đã hủy giao dịch");
        Set<Integer> seenTabs = new HashSet<>();
        for (Shop shop : loaded) {
            ShopRow expected = snapshot.shops.get(shop.id);
            if (expected == null || shop.npcId != expected.npc || shop.typeShop != expected.type || !java.util.Objects.equals(shop.tagName, expected.tag)
                    || shop.tabShops.size() != counts.getOrDefault(shop.id, 0)) throw error(500, "ShopDAO nạp sai shop");
            for (TabShop tab : shop.tabShops) {
                TabRow expectedTab = snapshot.tabs.get(tab.id);
                if (expectedTab == null || expectedTab.shop != shop.id || !seenTabs.add(tab.id) || tab.index != expectedTab.index
                        || !tab.name.equals(expectedTab.name.replace("<>", "\n"))) throw error(500, "ShopDAO nạp sai tab");
                List<JsonObject> visible = new ArrayList<>();
                for (JsonElement item : storedArray(expectedTab.items, "tab #" + tab.id)) {
                    if (!item.isJsonObject()) continue;
                    JsonObject raw = object(item);
                    if (bool(raw, "is_sell", false)) visible.add(raw);
                }
                if (tab.itemShops.size() != visible.size()) throw error(500, "ShopDAO bỏ sót vật phẩm; đã hủy giao dịch");
                for (int i = 0; i < visible.size(); i++) {
                    JsonObject raw = visible.get(i);
                    ItemShop item = tab.itemShops.get(i);
                    JsonArray options = raw.has("options") && !raw.get("options").isJsonNull() ? raw.getAsJsonArray("options") : new JsonArray();
                    if (item.temp == null || item.temp.id != integer(raw, "temp_id", -1) || item.cost != longValue(raw, "cost", 0)
                            || item.typeSell != integer(raw, "type_sell", -1) || item.iconSpec != integer(raw, "item_spec", -1)
                            || item.isNew != bool(raw, "is_new", false) || item.options.size() != options.size()) throw error(500, "ShopDAO nạp sai vật phẩm");
                    for (int j = 0; j < options.size(); j++) {
                        JsonObject option = options.get(j).getAsJsonObject();
                        if (item.options.get(j).optionTemplate == null || item.options.get(j).optionTemplate.id != integer(option, "id", -1)
                                || item.options.get(j).param != integer(option, "param", 0)) throw error(500, "ShopDAO nạp sai option");
                    }
                }
            }
        }
        if (seenTabs.size() != snapshot.tabs.size()) throw error(500, "ShopDAO bỏ sót tab");
        return loaded;
    }
}
