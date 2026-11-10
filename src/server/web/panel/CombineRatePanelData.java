package server.web.panel;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import services.CombineRateConfig;
import java.util.*;
import static server.web.panel.PanelSupport.*;
import static server.web.panel.WorldData.*;

/** Quản lý tỉ lệ đập đồ (CombineRateConfig) — GET xem, POST lưu; áp dụng ngay không cần restart. */
final class CombineRatePanelData {

    private CombineRatePanelData() {
    }

    static Object handle(HttpExchange exchange) throws Exception {
        requireMethod(exchange, "GET", "POST");
        if ("GET".equals(exchange.getRequestMethod())) {
            List<Map<String, Object>> rates = new ArrayList<>();
            for (int i = 0; i < CombineRateConfig.rateNangCapDo.length; i++) {
                rates.add(dto("key", "nangCapDo" + i, "label", "Nâng cấp đồ +" + (i + 1), "group", "Nâng cấp đồ",
                        "value", CombineRateConfig.rateNangCapDo[i], "min", 0d, "max", 100d, "integer", false));
            }
            for (int i = 0; i < CombineRateConfig.ratePhaLeHoa.length; i++) {
                rates.add(dto("key", "phaLeHoa" + i, "label", "Pha lê hóa " + i + "★ → " + (i + 1) + "★", "group", "Pha lê hóa",
                        "value", (double) CombineRateConfig.ratePhaLeHoa[i], "min", 0d, "max", 100d, "integer", false));
            }
            rates.add(dto("key", "skhVip1", "label", "Nhận SKH VIP 1", "group", "Set kích hoạt",
                    "value", (double) CombineRateConfig.skhVip1, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "skhVip2", "label", "Nhận SKH VIP 2", "group", "Set kích hoạt",
                    "value", (double) CombineRateConfig.skhVip2, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "skhThuong", "label", "Nhận SKH thường", "group", "Set kích hoạt",
                    "value", (double) CombineRateConfig.skhThuong, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "bongTai2", "label", "Bông tai Porata → Porata 2", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateBongTai2, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "bongTai3", "label", "Bông tai Porata 2 → Porata 3", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateBongTai3, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "trungDeTu1", "label", "Trứng Mabư → Ubu", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateTrungDeTu1, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "trungDeTu2", "label", "Trứng Ubu → Black", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateTrungDeTu2, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "trungDeTu3", "label", "Trứng Black → Beerus", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateTrungDeTu3, "min", 0d, "max", 100d, "integer", true));
            rates.add(dto("key", "zeno", "label", "Nâng cấp Zeno", "group", "Bông tai & Trứng đệ tử",
                    "value", (double) CombineRateConfig.rateZeno, "min", 0d, "max", 100d, "integer", true));
            return dto("rates", rates, "notes", "Tỉ lệ tính bằng % (0-100). Lưu là áp dụng ngay cho các lần đập đồ tiếp theo, không cần restart. "
                    + "Nâng cấp đồ: mảng theo cấp +1 → +8. Pha lê hóa: mảng theo số sao hiện tại của trang bị.");
        }
        JsonObject body = body(exchange);
        if (!body.has("values") || !body.get("values").isJsonObject()) throw error(400, "Thiếu danh sách tỉ lệ");
        JsonObject input = body.getAsJsonObject("values");
        int applied = 0;
        for (var entry : input.entrySet()) {
            String key = entry.getKey();
            if (!entry.getValue().isJsonPrimitive() || !entry.getValue().getAsJsonPrimitive().isNumber()) {
                throw error(400, "Tỉ lệ phải là số: " + key);
            }
            double v = entry.getValue().getAsDouble();
            if (Double.isNaN(v) || Double.isInfinite(v) || v < 0 || v > 100) {
                throw error(400, "Tỉ lệ ngoài khoảng 0-100: " + key);
            }
            apply(key, v);
            applied++;
        }
        CombineRateConfig.save();
        return changed("Đã lưu " + applied + " tỉ lệ. Áp dụng cho các lần đập đồ tiếp theo.", false);
    }

    private static void apply(String key, double v) {
        if (key.startsWith("nangCapDo")) {
            int i = Integer.parseInt(key.substring("nangCapDo".length()));
            if (i < 0 || i >= CombineRateConfig.rateNangCapDo.length) throw error(400, "Chỉ số nâng cấp không hợp lệ: " + key);
            CombineRateConfig.rateNangCapDo[i] = v;
            return;
        }
        if (key.startsWith("phaLeHoa")) {
            int i = Integer.parseInt(key.substring("phaLeHoa".length()));
            if (i < 0 || i >= CombineRateConfig.ratePhaLeHoa.length) throw error(400, "Chỉ số pha lê không hợp lệ: " + key);
            CombineRateConfig.ratePhaLeHoa[i] = (float) v;
            return;
        }
        int iv = (int) v;
        switch (key) {
            case "skhVip1" -> CombineRateConfig.skhVip1 = iv;
            case "skhVip2" -> CombineRateConfig.skhVip2 = iv;
            case "skhThuong" -> CombineRateConfig.skhThuong = iv;
            case "bongTai2" -> CombineRateConfig.rateBongTai2 = iv;
            case "bongTai3" -> CombineRateConfig.rateBongTai3 = iv;
            case "trungDeTu1" -> CombineRateConfig.rateTrungDeTu1 = iv;
            case "trungDeTu2" -> CombineRateConfig.rateTrungDeTu2 = iv;
            case "trungDeTu3" -> CombineRateConfig.rateTrungDeTu3 = iv;
            case "zeno" -> CombineRateConfig.rateZeno = iv;
            default -> throw error(400, "Tỉ lệ không tồn tại: " + key);
        }
    }
}
