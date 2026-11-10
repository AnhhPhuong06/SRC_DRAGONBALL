package server;


import database.AlyraManager;
import managers.GiftCodeManager;
import models.GiftCode;
import models.item.Item.ItemOption;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public final class GiftCodeAdmin {

    /** Một dòng vật phẩm trong giftcode. options: danh sách cặp {optionId, param}. */
    public static final class DetailRow {
        public int itemId;
        public int quantity;
        public final List<int[]> options = new ArrayList<>();

        public DetailRow() {
        }

        public DetailRow(int itemId, int quantity) {
            this.itemId = itemId;
            this.quantity = quantity;
        }
    }

    private GiftCodeAdmin() {
    }
    private static volatile boolean schemaChecked = false;

    private static final int PLAYER_NAME_MIN_LEN = 65535;

    private static void ensureSchema() {
        if (schemaChecked) {
            return;
        }
        schemaChecked = true;
        try (Connection con = AlyraManager.getConnection();
             Statement st = con.createStatement()) {
            Integer currentLen = null;
            try (ResultSet rs = st.executeQuery(
                    "SELECT CHARACTER_MAXIMUM_LENGTH FROM INFORMATION_SCHEMA.COLUMNS "
                            + "WHERE TABLE_SCHEMA = DATABASE() "
                            + "  AND TABLE_NAME = 'giftcode' "
                            + "  AND COLUMN_NAME = 'player_name'")) {
                if (rs.next()) {
                    currentLen = rs.getInt(1);
                    if (rs.wasNull()) {
                        currentLen = null;
                    }
                }
            }
            if (currentLen == null) {
                st.executeUpdate("ALTER TABLE giftcode ADD COLUMN player_name TEXT NULL DEFAULT NULL");
            } else if (currentLen < PLAYER_NAME_MIN_LEN) {
                st.executeUpdate("ALTER TABLE giftcode MODIFY COLUMN player_name TEXT NULL DEFAULT NULL");
            }
        } catch (Exception ex) {
            utils.Logger.error("[PANEL] Lỗi kiểm tra/migrate cột player_name: " + ex.getMessage() + "\n");
        }
    }

    private static String safeGetPlayerName(ResultSet rs) {
        try {
            return rs.getString("player_name");
        } catch (Exception e) {
            return null;
        }
    }

    public static void reload() throws Exception {
        ensureSchema();
        try {
            doReload();
        } catch (Exception first) {
            try {
                Thread.sleep(150); // chờ Hikari evict broken connection
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
            }
            try {
                doReload();
            } catch (Exception second) {
                throw first;
            }
        }
    }

    private static void doReload() throws Exception {
        List<GiftCode> fresh = new ArrayList<>();
        try (Connection con = AlyraManager.getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery("SELECT * FROM giftcode")) {
            while (rs.next()) {
                GiftCode gc = new GiftCode();
                gc.code = rs.getString("code");
                gc.id = rs.getInt("id");
                gc.countLeft = rs.getInt("count_left");
                String ag = rs.getString("allGender");
                gc.allGender = "all".equalsIgnoreCase(ag) || "1".equals(ag) || "true".equalsIgnoreCase(ag);
                if (gc.countLeft == -1) {
                    gc.countLeft = 999999999;
                }
                gc.datecreate = rs.getTimestamp("datecreate");
                gc.dateexpired = rs.getTimestamp("expired");
                gc.playerName = safeGetPlayerName(rs);
                for (DetailRow r : parseDetailJson(rs.getString("detail"))) {
                    ArrayList<ItemOption> optionList = new ArrayList<>();
                    for (int[] op : r.options) {
                        optionList.add(new ItemOption(op[0], op[1]));
                    }
                    gc.option.put(r.itemId, optionList);
                    gc.detail.put(r.itemId, r.quantity);
                }
                fresh.add(gc);
            }
        }
        List<GiftCode> list = GiftCodeManager.gI().listGiftCode;
        synchronized (list) {
            list.clear();
            list.addAll(fresh);
        }
    }

    /** Lấy chuỗi JSON detail thô của 1 giftcode (để chỉnh sửa chính xác). */
    public static String fetchDetailJson(int id) throws Exception {
        try (Connection con = AlyraManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT detail FROM giftcode WHERE id = ?")) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("detail");
                }
            }
        }
        return "[]";
    }

    public static void insert(String code, int countLeft, boolean allGender,
                              Timestamp datecreate, Timestamp expired, String detailJson,
                              String playerName) throws Exception {
        ensureSchema();
        int newId = nextId();
        String pn = (playerName == null || playerName.trim().isEmpty()) ? null : playerName.trim();
        AlyraManager.executeUpdate(
                "INSERT INTO giftcode (id, code, count_left, detail, allGender, datecreate, expired, player_name) VALUES (?,?,?,?,?,?,?,?)",
                newId, code, countLeft, detailJson, allGender ? "all" : "0", datecreate, expired, pn);
    }

    public static void insertBatchShare(List<String> codes, int countLeft, boolean allGender,
                                        Timestamp datecreate, Timestamp expired, String detailJson,
                                        String playerName) throws Exception {
        ensureSchema();
        if (codes == null || codes.isEmpty()) {
            return;
        }
        String pn = (playerName == null || playerName.trim().isEmpty()) ? null : playerName.trim();
        String ag = allGender ? "all" : "0";
        try (Connection con = AlyraManager.getConnection()) {
            con.setAutoCommit(false);
            try (PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO giftcode (code, count_left, detail, allGender, datecreate, expired, player_name) VALUES (?,?,?,?,?,?,?)")) {
                for (String code : codes) {
                    ps.setString(1, code);
                    ps.setInt(2, countLeft);
                    ps.setString(3, detailJson);
                    ps.setString(4, ag);
                    ps.setTimestamp(5, datecreate);
                    ps.setTimestamp(6, expired);
                    ps.setString(7, pn);
                    ps.addBatch();
                }
                ps.executeBatch();
                con.commit();
            } catch (Exception ex) {
                con.rollback();
                throw ex;
            } finally {
                con.setAutoCommit(true);
            }
        }
        reload();
    }

    public static void update(int id, String code, int countLeft, boolean allGender,
                              Timestamp datecreate, Timestamp expired, String detailJson,
                              String playerName) throws Exception {
        ensureSchema();
        String pn = (playerName == null || playerName.trim().isEmpty()) ? null : playerName.trim();
        AlyraManager.executeUpdate(
                "UPDATE giftcode SET code=?, count_left=?, detail=?, allGender=?, datecreate=?, expired=?, player_name=? WHERE id=?",
                code, countLeft, detailJson, allGender ? "all" : "0", datecreate, expired, pn, id);
    }

    public static void delete(int id) throws Exception {
        AlyraManager.executeUpdate("DELETE FROM giftcode WHERE id = ?", id);
    }

    private static int nextId() throws Exception {
        try (Connection con = AlyraManager.getConnection();
             PreparedStatement ps = con.prepareStatement("SELECT COALESCE(MAX(id),0)+1 AS nid FROM giftcode");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt("nid");
            }
        }
        return 1;
    }

    /* ===================== JSON ===================== */

    /** Dựng chuỗi detail JSON: [{"id":..,"quantity":..,"options":[{"id":..,"param":..}]}] */
    @SuppressWarnings("unchecked")
    public static String buildDetailJson(List<DetailRow> rows) {
        JSONArray arr = new JSONArray();
        for (DetailRow r : rows) {
            JSONObject o = new JSONObject();
            o.put("id", r.itemId);
            o.put("quantity", r.quantity);
            JSONArray opts = new JSONArray();
            for (int[] op : r.options) {
                JSONObject oo = new JSONObject();
                oo.put("id", op[0]);
                oo.put("param", op[1]);
                opts.add(oo);
            }
            o.put("options", opts);
            arr.add(o);
        }
        return arr.toJSONString();
    }

    /** Phân tích chuỗi detail JSON thành danh sách dòng vật phẩm. */
    public static List<DetailRow> parseDetailJson(String json) {
        List<DetailRow> rows = new ArrayList<>();
        if (json == null || json.isEmpty()) {
            return rows;
        }
        Object parsed = JSONValue.parse(json);
        if (!(parsed instanceof JSONArray jar)) {
            return rows;
        }
        for (Object o : jar) {
            if (!(o instanceof JSONObject jo)) {
                continue;
            }
            DetailRow r = new DetailRow();
            r.itemId = toInt(jo.get("id"));
            r.quantity = toInt(jo.get("quantity"));
            Object opts = jo.get("options");
            if (opts instanceof JSONArray oarr) {
                for (Object oo : oarr) {
                    if (oo instanceof JSONObject jj) {
                        r.options.add(new int[]{toInt(jj.get("id")), toInt(jj.get("param"))});
                    }
                }
            }
            rows.add(r);
        }
        return rows;
    }

    private static int toInt(Object o) {
        if (o == null) {
            return 0;
        }
        try {
            return Integer.parseInt(o.toString().trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
