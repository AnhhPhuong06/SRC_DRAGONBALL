package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import models.Template;
import services.ItemService;
import static server.web.panel.CatalogShopApi.*;
import static server.web.panel.PanelSupport.*;

/** One request's database catalog snapshot; no truncation and no game/database startup. */
final class CatalogData {
    final Map<Integer, Map<String, Object>> items = new LinkedHashMap<>();
    final Map<Integer, Map<String, Object>> options = new LinkedHashMap<>();

    static CatalogData load(Connection con) throws SQLException {
        CatalogData data = new CatalogData();
        try (PreparedStatement ps = con.prepareStatement("SELECT id,name,type,gender,description,level,icon_id,part,"
                + "is_up_to_up,power_require,gold,gem,head,body,leg FROM item_template ORDER BY id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) data.items.put(rs.getInt("id"), map("id", rs.getInt("id"), "name", rs.getString("name"),
                    "type", rs.getInt("type"), "gender", rs.getInt("gender"), "description", rs.getString("description"),
                    "level", rs.getInt("level"), "icon", rs.getInt("icon_id"), "part", rs.getInt("part"),
                    "isUpToUp", rs.getBoolean("is_up_to_up"), "powerRequire", rs.getLong("power_require"),
                    "gold", rs.getInt("gold"), "gem", rs.getInt("gem"), "head", rs.getInt("head"),
                    "body", rs.getInt("body"), "leg", rs.getInt("leg")));
        }
        try (PreparedStatement ps = con.prepareStatement("SELECT id,name FROM item_option_template ORDER BY id");
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) data.options.put(rs.getInt("id"), map("id", rs.getInt("id"), "name", rs.getString("name")));
        }
        return data;
    }

    Map<String, Object> item(int id, boolean currency) {
        if (currency && id >= -3 && id <= -1) {
            return map("id", id, "name", switch (id) { case -1 -> "Vàng"; case -2 -> "Ngọc xanh"; default -> "Ngọc khóa"; },
                    "icon", 0);
        }
        if (id < 0 || id > Short.MAX_VALUE || !items.containsKey(id)) throw error(422, "Template vật phẩm không tồn tại: " + id);
        return items.get(id);
    }

    void runtimeItem(int id) {
        item(id, false);
        Template.ItemTemplate template = ItemService.gI().getTemplate(id);
        if (template == null || template.id != id) throw error(409, "Template chưa được nạp vào máy chủ: " + id);
    }

    void runtimeOptions(JsonArray values) {
        for (JsonElement element : values) {
            int id = integer(object(element), "id", -1);
            Template.ItemOptionTemplate template = ItemService.gI().getItemOptionTemplate(id);
            if (template == null || template.id != id) throw error(409, "Option chưa được nạp vào máy chủ: " + id);
        }
    }

    JsonArray validateOptions(JsonArray values) {
        if (values.size() > 255) throw error(400, "Tối đa 255 option");
        JsonArray result = new JsonArray();
        Set<Integer> seen = new HashSet<>();
        for (JsonElement element : values) {
            JsonObject option = object(element);
            fields(option, "id", "param");
            int id = id(option, "id", false);
            if (!options.containsKey(id)) throw error(422, "Option không tồn tại: " + id);
            if (!seen.add(id)) throw error(400, "Option bị trùng: " + id);
            if (!option.has("param") || option.get("param").isJsonNull()) throw error(400, "Thiếu param của option");
            int param = integer(option, "param", 0);
            JsonObject saved = new JsonObject();
            saved.addProperty("id", id);
            saved.addProperty("param", param);
            result.add(saved);
        }
        return result;
    }

    List<Map<String, Object>> optionDtos(JsonArray values) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (JsonElement element : values) {
            JsonObject option = object(element);
            int id = integer(option, "id", -1);
            int param = integer(option, "param", 0);
            Map<String, Object> template = options.get(id);
            if (template == null) throw error(422, "Option trong dữ liệu không tồn tại: " + id);
            String name = String.valueOf(template.get("name"));
            result.add(map("id", id, "param", param, "name", name, "text", name.replace("#", String.valueOf(param))));
        }
        return result;
    }
}
