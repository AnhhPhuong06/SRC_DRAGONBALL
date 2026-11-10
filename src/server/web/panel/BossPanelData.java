package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpExchange;
import database.AlyraManager;
import java.io.IOException;
import java.sql.SQLException;
import java.util.*;
import consts.BossStatus;
import models.boss.Boss;
import server.Manager;
import services.BossSettings;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

final class BossPanelData {
    @FunctionalInterface interface MapsLoader { List<Map<String, Object>> load() throws Exception; }
    private BossPanelData() {}
    static Object handle(HttpExchange exchange) throws Exception { return handle(exchange, BossSettings.get(), BossPanelData::maps); }
    static Object handle(HttpExchange exchange, BossSettings store, MapsLoader loader) throws Exception {
        requireMethod(exchange, "GET", "POST");
        try {
            if ("GET".equals(exchange.getRequestMethod())) {
                var state = store.state();
                List<Map<String, Object>> maps;
                String warning = "";
                try { maps = loader.load(); }
                catch (SQLException offline) { maps = List.of(); warning = "Chưa đọc được danh sách map. Vẫn có thể sửa các chỉ số; đổi map cần kết nối dữ liệu."; }
                List<Map<String, Object>> templates = new ArrayList<>();
                for (var entry : state.templates()) templates.add(dto("key", entry.key(), "config", entry.config(),
                        "customized", entry.customized(), "appearType", entry.appearType()));
                List<Map<String, Object>> runtime = new ArrayList<>();
                for (Boss boss : ServerRuntimeApi.allBosses()) {
                    var row = ServerRuntimeApi.playerView(boss);
                    row.putAll(dto("status", boss.bossStatus == null ? "UNKNOWN" : boss.bossStatus.name(),
                            "hp", boss.nPoint == null ? 0 : boss.nPoint.hp, "hpMax", boss.nPoint == null ? 0 : boss.nPoint.hpMax,
                            "dame", boss.nPoint == null ? 0 : boss.nPoint.dame, "head", ServerRuntimeApi.bossHead(boss),
                            "templateKeys", store.keysFor(boss.data),
                            "canSummon", boss.bossStatus == BossStatus.REST && ServerRuntimeApi.summonable(boss)));
                    runtime.add(row);
                }
                return dto("templates", templates, "runtime", runtime, "revision", state.revision(), "loadError", state.loadError(),
                        "maps", maps, "mapWarning", warning, "maxStat", BossSettings.MAX_STAT,
                        "notes", "Mỗi mẫu là một dạng/giai đoạn boss. Lưu giữ cấu hình qua khởi động lại; HP và sát thương áp dụng từ lần xuất hiện tiếp theo. "
                                + "Boss phụ/phó bản vẫn theo chuỗi gọi và map của loại boss đó. Boss có code riêng có thể tự tính lại chỉ số.");
            }
            JsonObject body = body(exchange);
            String action = query(exchange).getOrDefault("action", text(body, "action", ""));
            if ("summon".equals(action)) return ServerRuntimeApi.summon(body);
            if (!Set.of("save", "reset").contains(action)) throw error(400, "Tác vụ boss không hợp lệ");
            if (!body.has("revision")) throw error(400, "Thiếu phiên bản cấu hình boss");
            long revision = longValue(body, "revision", -1);
            String key = required(body, "key");
            if ("reset".equals(action)) {
                store.reset(key, revision);
                return changed("Đã khôi phục mẫu boss về cấu hình gốc khi server khởi động.", false);
            }
            var previous = store.config(key);
            var config = new BossSettings.Config(name(body, "name", null, 100), field(body, "gender", -1, 0, 3),
                    longValue(body, "dame", -1), longArray(body, "hp", 64), intArray(body, "mapJoin", 256),
                    intArray(body, "outfit", 6), field(body, "secondsRest", -1, 0, 31_536_000));
            BossSettings.validate(config);
            if (!config.mapJoin().equals(previous.mapJoin())) {
                var valid = loader.load();
                for (int map : config.mapJoin()) {
                    if (map == -1 && previous.mapJoin().contains(-1)) continue;
                    // Some shipped definitions already include Kame alongside normal maps;
                    // Boss.getMapJoin ignores it. Preserve that entry when adding a normal map.
                    if (map == 5 && previous.mapJoin().contains(5)
                            && config.mapJoin().stream().anyMatch(id -> id >= 0 && id != 5)) continue;
                    if (map == 5) throw error(422, "Đảo Kame (map 5) không cho boss xuất hiện.");
                    if (valid.stream().noneMatch(row -> n(row, "id") == map)) throw error(422, "Map không tồn tại: " + map);
                }
            }
            store.save(key, config, revision);
            return changed("Đã lưu cấu hình boss. HP và sát thương áp dụng từ lần boss xuất hiện tiếp theo.", false);
        } catch (ConcurrentModificationException conflict) { throw error(409, conflict.getMessage()); }
        catch (NoSuchElementException missing) { throw error(404, missing.getMessage()); }
        catch (IllegalArgumentException invalid) { throw error(400, invalid.getMessage()); }
        catch (IOException failure) { throw error(503, "Không lưu được boss_settings.json. Cấu hình đang áp dụng được giữ nguyên."); }
    }
    private static List<Long> longArray(JsonObject body, String key, int max) {
        if (!body.has(key) || !body.get(key).isJsonArray() || body.getAsJsonArray(key).size() > max)
            throw error(400, "Danh sách không hợp lệ: " + key);
        List<Long> values = new ArrayList<>();
        for (JsonElement element : body.getAsJsonArray(key)) {
            try { values.add(element.getAsBigDecimal().longValueExact()); }
            catch (RuntimeException invalid) { throw error(400, key + " phải gồm các số nguyên."); }
        }
        return values;
    }
    private static List<Integer> intArray(JsonObject body, String key, int max) {
        try { return longArray(body, key, max).stream().map(Math::toIntExact).toList(); }
        catch (ArithmeticException invalid) { throw error(400, "ID ngoài giới hạn: " + key); }
    }
    private static List<Map<String, Object>> maps() throws SQLException {
        if (Manager.MAP_TEMPLATES != null && Manager.MAP_TEMPLATES.length > 0) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (var map : Manager.MAP_TEMPLATES) if (map != null) result.add(dto("id", map.id, "name", map.name));
            return result;
        }
        try (var connection = AlyraManager.getConnection_Data()) { return rows(connection, "SELECT id,name FROM map_template ORDER BY id"); }
    }
}
