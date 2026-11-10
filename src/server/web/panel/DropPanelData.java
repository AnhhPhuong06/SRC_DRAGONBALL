package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.io.IOException;
import java.sql.Connection;
import java.util.*;
import services.ItemService;
import services.MobDropRules;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

final class DropPanelData {
    record Catalog(List<Map<String, Object>> maps, List<Map<String, Object>> mobs,
                   List<Map<String, Object>> items, List<Map<String, Object>> options) {}
    @FunctionalInterface interface CatalogLoader { Catalog load() throws Exception; }
    private DropPanelData() {}

    static Object handle(HttpExchange exchange) throws Exception {
        return handle(exchange, MobDropRules.get(), DropPanelData::catalog);
    }
    static Object handle(HttpExchange exchange, MobDropRules store, CatalogLoader loader) throws Exception {
        requireMethod(exchange, "GET", "POST");
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                Catalog catalog = loader.load();
                MobDropRules.State state = store.state();
                List<Map<String, Object>> rules = new ArrayList<>();
                for (var rule : state.rules()) rules.add(ruleView(rule, catalog));
                return dto("rules", rules, "revision", state.revision(), "maxRules", MobDropRules.MAX_RULES,
                        "loadError", store.loadError(), "catalog", dto("maps", catalog.maps(), "mobs", catalog.mobs(),
                                "items", catalog.items(), "options", catalog.options()),
                        "notes", "Drop tùy chỉnh được cộng thêm vào drop hiện có. Mỗi quy tắc được xét riêng cho từng lần diệt quái thường. "
                                + "Quái mộc nhân và boss không dùng bộ drop này.");
            }
            JsonObject body = body(exchange);
            String action = query(exchange).getOrDefault("action", text(body, "action", ""));
            if (!body.has("revision")) throw error(400, "Thiếu phiên bản danh sách drop");
            long revision = longValue(body, "revision", -1);
            if ("rule-delete".equals(action)) {
                store.delete(field(body, "id", -1, 1, Integer.MAX_VALUE), revision);
                return changed("Đã xóa quy tắc drop.", false);
            }
            if (!"rule-save".equals(action)) throw error(400, "Tác vụ drop không hợp lệ");
            int id = field(body, "id", 0, 0, Integer.MAX_VALUE);
            int map = field(body, "mapId", -1, -1, 32767);
            int mob = field(body, "mobId", -1, -1, 254);
            int item = field(body, "itemId", -1, 0, 32767);
            if (!body.has("chance") || !body.get("chance").isJsonPrimitive()
                    || !body.getAsJsonPrimitive("chance").isNumber()) throw error(400, "Tỷ lệ phải là số từ 0 đến 100%");
            double chance = body.get("chance").getAsDouble();
            Catalog catalog = loader.load();
            if (map != -1) requireId(catalog.maps(), map, "Map");
            if (mob != -1) requireId(catalog.mobs(), mob, "Quái");
            requireId(catalog.items(), item, "Vật phẩm");
            List<MobDropRules.Option> options = new ArrayList<>();
            if (body.has("options")) {
                for (JsonElement element : array(body, "options", 32)) {
                    JsonObject option = element.getAsJsonObject();
                    int optionId = field(option, "id", -1, 0, 255);
                    requireId(catalog.options(), optionId, "Option");
                    options.add(new MobDropRules.Option(optionId, field(option, "param", -1, 0, 65535)));
                }
            }
            var rule = new MobDropRules.Rule(id, name(body, "name", null, 100), bool(body, "enabled", true),
                    map, mob, item, chance, field(body, "minQuantity", 1, 1, 99999),
                    field(body, "maxQuantity", 1, 1, 99999), options);
            MobDropRules.Rule saved = store.save(rule, revision);
            return changed("Đã lưu quy tắc drop. Áp dụng cho những lần diệt quái tiếp theo.", false, "id", saved.id());
        } catch (ConcurrentModificationException error) { throw error(409, error.getMessage()); }
        catch (NoSuchElementException error) { throw error(404, error.getMessage()); }
        catch (IllegalArgumentException error) { throw error(400, error.getMessage()); }
        catch (IOException error) { throw error(503, "Không lưu/đọc được file cấu hình drop; dữ liệu đang áp dụng được giữ nguyên."); }
    }
    private static void requireId(List<Map<String, Object>> rows, int id, String label) {
        if (rows.stream().noneMatch(row -> n(row, "id") == id)) throw error(422, label + " không tồn tại: " + id);
    }
    private static String label(List<Map<String, Object>> rows, int id, String all) {
        if (id == -1) return all;
        return rows.stream().filter(row -> n(row, "id") == id).findFirst().map(row -> s(row, "name")).orElse("Không tìm thấy #" + id);
    }
    private static Map<String, Object> ruleView(MobDropRules.Rule rule, Catalog catalog) {
        return dto("id", rule.id(), "name", rule.name(), "enabled", rule.enabled(), "mapId", rule.mapId(),
                "mapName", label(catalog.maps(), rule.mapId(), "Tất cả map"), "mobId", rule.mobId(),
                "mobName", label(catalog.mobs(), rule.mobId(), "Tất cả quái thường"), "itemId", rule.itemId(),
                "itemName", label(catalog.items(), rule.itemId(), ""), "chance", rule.chance(),
                "minQuantity", rule.minQuantity(), "maxQuantity", rule.maxQuantity(), "options", rule.options());
    }
    private static volatile Catalog cachedCatalog;
    private static volatile long catalogLoadedAt;
    private static final long CATALOG_TTL_MS = 300_000;

    private static Catalog catalog() throws Exception {
        Catalog cached = cachedCatalog;
        if (cached != null && System.currentTimeMillis() - catalogLoadedAt < CATALOG_TTL_MS) return cached;
        synchronized (DropPanelData.class) {
            cached = cachedCatalog;
            if (cached != null && System.currentTimeMillis() - catalogLoadedAt < CATALOG_TTL_MS) return cached;
            cached = loadCatalogFromDb();
            cachedCatalog = cached;
            catalogLoadedAt = System.currentTimeMillis();
            return cached;
        }
    }

    private static Catalog loadCatalogFromDb() throws Exception {
        try (Connection connection = AlyraManager.getConnection_Data()) {
            var maps = rows(connection, "SELECT id,name FROM map_template ORDER BY id");
            var mobs = rows(connection, "SELECT id,name FROM mob_template WHERE id>0 AND id<=254 ORDER BY id");
            var items = rows(connection, "SELECT id,name FROM item_template WHERE id>=0 AND id<=32767 ORDER BY id");
            var options = rows(connection, "SELECT id,name FROM item_option_template WHERE id>=0 AND id<=255 ORDER BY id");
            for (var option : options) option.put("name", ItemService.resolveOptionName(n(option, "id"), s(option, "name")));
            return new Catalog(maps, mobs, items, options);
        }
    }
}
