package managers;

 


import database.AlyraManager;
import models.GiftCode;
import models.player.Player;
import services.map.NpcService;
import services.Service;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import services.player.InventoryService;

public class GiftCodeManager {

    public String name;
    public final List<GiftCode> listGiftCode = new CopyOnWriteArrayList<>();

    private static GiftCodeManager instance;

    public static GiftCodeManager gI() {
        if (instance == null) {
            instance = new GiftCodeManager();
        }
        return instance;
    }

    public synchronized void init() {
        listGiftCode.clear();
        String sql = "SELECT * FROM giftcode WHERE count_left > 0 AND (expired IS NULL OR expired > NOW())";
        try (java.sql.Connection con = database.AlyraManager.getConnection();
             java.sql.PreparedStatement ps = con.prepareStatement(sql);
             java.sql.ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.id = rs.getInt("id");
                giftcode.code = rs.getString("code");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) {
                    giftcode.countLeft = 999999999;
                }
                try {
                    String ag = rs.getString("allGender");
                    giftcode.allGender = ag == null || "all".equalsIgnoreCase(ag) || "true".equalsIgnoreCase(ag) || "1".equals(ag);
                } catch (Exception ignored) {
                    giftcode.allGender = true;
                }
                try {
                    giftcode.playerName = rs.getString("player_name");
                } catch (Exception ignored) {
                    giftcode.playerName = null;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                String detailJson = rs.getString("detail");
                if (detailJson != null && !detailJson.isEmpty()) {
                    Object parsed = org.json.simple.JSONValue.parse(detailJson);
                    if (parsed instanceof org.json.simple.JSONArray jar) {
                        for (Object itemObj : jar) {
                            org.json.simple.JSONObject jsonObj = (org.json.simple.JSONObject) itemObj;
                            int itemId = jsonObj.containsKey("id") ? Integer.parseInt(jsonObj.get("id").toString()) : 0;
                            int quantity = jsonObj.containsKey("quantity") ? Integer.parseInt(jsonObj.get("quantity").toString()) : 1;
                            org.json.simple.JSONArray optionArray = (org.json.simple.JSONArray) jsonObj.get("options");
                            java.util.ArrayList<models.item.Item.ItemOption> optionList = new java.util.ArrayList<>();
                            if (optionArray != null) {
                                for (Object optionObj : optionArray) {
                                    org.json.simple.JSONObject optionJson = (org.json.simple.JSONObject) optionObj;
                                    int optId = Integer.parseInt(optionJson.get("id").toString());
                                    int param = Integer.parseInt(optionJson.get("param").toString());
                                    optionList.add(new models.item.Item.ItemOption(optId, param));
                                }
                            }
                            giftcode.option.put(itemId, optionList);
                            giftcode.detail.put(itemId, quantity);
                        }
                    }
                }
                listGiftCode.add(giftcode);
            }
        } catch (Exception e) {
            utils.Logger.error("GiftCodeManager init: " + e.getMessage() + "\n");
        }
    }

    public GiftCode checkUseGiftCode(Player player, String code) {
        if (code == null || code.trim().isEmpty()) {
            Service.gI().sendThongBaoOK(player, "Vui lòng nhập mã code");
            return null;
        }
        String trimmed = code.trim();
        return consumeGiftCode(player, trimmed);
    }

    private synchronized GiftCode consumeGiftCode(Player player, String trimmed) {
        for (GiftCode giftCode : listGiftCode) {
            if (giftCode.code != null && giftCode.code.equalsIgnoreCase(trimmed)) {
                if (giftCode.countLeft <= 0) {
                    Service.gI().sendThongBaoOK(player, "Giftcode đã hết lượt sử dụng");
                    return null;
                }
                if (giftCode.timeCode()) {
                    Service.gI().sendThongBaoOK(player, "Giftcode đã hết hạn");
                    return null;
                }
                for (var reward : giftCode.detail.entrySet()) {
                    Integer id = reward.getKey();
                    Integer quantity = reward.getValue();
                    if (id == null || id < -3 || id > Short.MAX_VALUE || quantity == null || quantity <= 0) {
                        Service.gI().sendThongBaoOK(player, "Giftcode có phần thưởng không hợp lệ. Vui lòng báo quản trị viên.");
                        return null;
                    }
                }
                if (!isNameAllowed(giftCode, player)) {
                    String curName = (player.name == null || player.name.isEmpty()) ? "?" : player.name;
                    Service.gI().sendThongBaoOK(player,
                            "Code này chỉ dành cho người chơi được chỉ định.\nNhân vật \"" + curName + "\" không nằm trong danh sách.");
                    return null;
                }
                // Defensive: player.giftCode có thể null với player cũ chưa migration đầy đủ.
                if (player.giftCode == null) {
                    player.giftCode = new models.player.GiftCode();
                }
                if (giftCode.isUsedGiftCode(player)) {
                    Service.gI().sendThongBaoOK(player, "Bạn đã nhập code này rồi");
                    return null;
                }
                // Chỉ đếm item cần chỗ trong bag — gold/gem/ruby (id âm -1/-2/-3) không cần ô hành trang.
                int slotsNeeded = 0;
                for (Integer itemId : giftCode.detail.keySet()) {
                    if (itemId != null && itemId >= 0) slotsNeeded++;
                }
                if (slotsNeeded > 0 && InventoryService.gI().getCountEmptyBag(player) < slotsNeeded) {
                    Service.gI().sendThongBaoOK(player, "Cần tối thiểu " + slotsNeeded + " ô hành trang trống");
                    return null;
                }
                try {
                    // Claim the database use before granting rewards, including across server processes.
                    if (!reserveGiftCodeUse(giftCode)) {
                        giftCode.countLeft = 0;
                        Service.gI().sendThongBaoOK(player, "Giftcode đã hết lượt sử dụng hoặc đã hết hạn");
                        return null;
                    }
                } catch (Exception e) {
                    utils.Logger.error("GiftCode claim failed: " + e.getClass().getSimpleName() + "\n");
                    Service.gI().sendThongBaoOK(player, "Chưa thể nhận giftcode, vui lòng thử lại sau");
                    return null;
                }
                giftCode.countLeft -= 1;
                player.giftCode.add(giftCode.code);
                return giftCode;
            }
        }

        Service.gI().sendThongBaoOK(player, "Mã code không tồn tại hoặc đã sai");
        return null;
    }

    protected boolean reserveGiftCodeUse(GiftCode giftCode) throws Exception {
        return AlyraManager.executeUpdate("UPDATE giftcode SET count_left=count_left-1,datecreate=datecreate WHERE id=? AND LOWER(code)=LOWER(?) "
                + "AND count_left>0 AND (expired IS NULL OR expired>NOW())", giftCode.id, giftCode.code) == 1;
    }

    private boolean isNameAllowed(GiftCode giftCode, Player player) {
        String whitelist = giftCode.playerName;
        if (whitelist == null || whitelist.trim().isEmpty()) {
            return true; // không giới hạn
        }
        String charName = player.name == null ? "" : player.name.trim();
        String accName = "";
        if (player.getSession() != null && player.getSession().uu != null) {
            accName = player.getSession().uu.trim();
        }
        for (String raw : whitelist.split("[,;，\\r\\n]")) {
            String n = raw.trim();
            if (n.isEmpty()) {
                continue;
            }
            if (n.equalsIgnoreCase(charName)
                    || (!accName.isEmpty() && n.equalsIgnoreCase(accName))) {
                return true;
            }
        }
        return false;
    }

    public void updateGiftCode(GiftCode giftcode) {
        try {
            AlyraManager.executeUpdate("update giftcode set count_left = ? where id = ?", giftcode.countLeft, giftcode.id);
        } catch (Exception e) {
        }
    }

    public void checkInfomationGiftCode(Player p) {
        StringBuilder sb = new StringBuilder();
        for (GiftCode giftCode : listGiftCode) {
            sb.append("Code: ").append(giftCode.code).append(", Số lượng còn lại: ").append(giftCode.countLeft).append("\b")
                    .append("Ngày tạo: ")
                    .append(giftCode.datecreate).append(", Ngày hết hạn: ").append(giftCode.dateexpired)
                    .append("\n");
        }
        if (sb.length() == 0) {
            NpcService.gI().createTutorial(p, 5073, "Hiện chưa có giftcode nào.");
            return;
        }
        sb.deleteCharAt(sb.length() - 1);
        NpcService.gI().createTutorial(p, 5073, sb.toString());
    }

}
