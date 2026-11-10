package services.player;

import database.AlyraManager;
import org.json.JSONArray;
import org.json.JSONObject;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.java.Log;
import models.item.Item;
import models.item.Item.ItemOption;
import models.player.Player;
import org.json.JSONException;
import services.ItemService;
import services.Service;
import services.map.NpcService;
import utils.Util;

public class GiftService {

    private static GiftService i;

    private GiftService() {

    }

    public static GiftService gI() {
        if (i == null) {
            i = new GiftService();
        }
        return i;
    }

    public void use(Player player, String code) {
        int lent = code.length();
        if (code.isEmpty() || lent < 5 || lent > 30) {
            Service.gI().sendThongBaoOK(player, "Mã quà tặng có chiều dài từ 5 đến 30 ký tự.");
            return;
        }
        Pattern p = Pattern.compile("^[a-zA-Z0-9]+$");
        Matcher m1 = p.matcher(code);
        if (!m1.find()) {
            Service.gI().sendThongBaoOK(player, "Mã quà tặng chỉ gồm chữ và số.");
            return;
        }
        code = code.toLowerCase();
        try (PreparedStatement stmt = AlyraManager.getConnection().prepareStatement("SELECT * FROM `gift_codes` WHERE `code` = ? AND (expires_at IS NULL OR expires_at > now()) LIMIT 1;",
                ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)) {
            stmt.setString(1, code);
            try (ResultSet res = stmt.executeQuery()) {
                if (!res.first()) {
                    Service.gI().sendThongBaoOK(player, "Mã quà tặng không tồn tại hoặc đã hết hạn.");
                    return;
                }

                int id = res.getInt("id");
                byte status = res.getByte("status");
                byte type = res.getByte("type");
                boolean active = res.getBoolean("active");

                if (status == 1) {
                    Service.gI().sendThongBaoOK(player, "Mã quà tặng đã được sử dụng");
                    return;
                } else if (type == 1 && isUsedGiftCode((int) player.id, id)) {
                    Service.gI().sendThongBaoOK(player, "Mỗi người chỉ được sử dụng 1 lần.");
                    return;
                } else if (active && !player.getSession().actived) {
                    Service.gI().sendThongBao(player, "Cần kích hoạt tài khoản để nhận mã quà tặng này");
                    return;
                }

                int gold = res.getInt("gold");
                int gem = res.getInt("gem");
                int ruby = res.getInt("ruby");

                JSONArray arrItem = new JSONArray(res.getString("items"));
                int size = arrItem.length();

                if (size > InventoryService.gI().getCountEmptyBag(player)) {
                    Service.gI().sendThongBaoOK(player, "Bạn không đủ chỗ trống trong hành trang.");
                    return;
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Chúc mừng, bạn đã được tặng").append("\b");
                for (int a = 0; a < size; a++) {
                    JSONObject itemObj = (JSONObject) arrItem.get(a);
                    int itemID = itemObj.getInt("id");
                    int quantity = itemObj.getInt("quantity");
                    JSONArray options = itemObj.getJSONArray("options");

                    Item item = ItemService.gI().createNewItem((short) itemID, quantity);
                    for (int j = 0; j < options.length(); j++) {
                        JSONObject obj = options.getJSONObject(j);
                        int optionID = obj.getInt("id");
                        int param = obj.getInt("param");
                        item.itemOptions.add(new ItemOption(optionID, param));
                    }
                    item.createTime = System.currentTimeMillis();
                    InventoryService.gI().addItemBag(player, item);
                    sb.append(String.format("- x%s %s", Util.numberToMoney(quantity), item.template.name)).append("\b");
                }

                if (gold > 0) {
                    player.inventory.addGold(gold);
                    sb.append(String.format("- %s vàng", Util.numberToMoney(gold))).append("\b");
                }

                if (gem > 0) {
                    player.inventory.gem += gem;
                    sb.append(String.format("- %s ngọc xanh", Util.numberToMoney(gem))).append("\b");
                }

                if (ruby > 0) {
                    player.inventory.ruby += ruby;
                    sb.append(String.format("- %s hồng ngọc", Util.numberToMoney(ruby))).append("\b");
                }
                Service.gI().sendMoney(player);
                InventoryService.gI().sendItemBags(player);
                String text = sb.toString();
                String[] arr = text.split("\b");
                StringBuilder sb2 = new StringBuilder();
                for (int b = 0; b < arr.length; b++) {
                    sb2.append(arr[b]);
                    if (b % 10 == 0 && b != 0 && b != arr.length - 1) {
                        sb2.append("\n");
                    } else {
                        sb2.append("\b");
                    }
                }
                NpcService.gI().createTutorial(player, -1, sb2.toString());
                addUsedGiftCode((int) player.id, id, code);
                if (type == 0) {
                    Timestamp timestamp = new Timestamp(System.currentTimeMillis());
                    res.updateByte("status", (byte) 1);
                    res.updateTimestamp("updated_at", timestamp);
                    res.updateRow();
                }
            }
        } catch (SQLException | JSONException e) {
            //  Log.error(GiftService.class, e);
        }
    }

    private boolean isUsedGiftCode(int playerID, int giftCodeId) {
        String query = "SELECT 1 FROM `gift_code_histories` WHERE `gift_code_id` = ? AND `player_id` = ? LIMIT 1;";

        try (PreparedStatement stmt = AlyraManager.getConnection().prepareStatement(query)) {
            stmt.setInt(1, giftCodeId);
            stmt.setInt(2, playerID);

            try (ResultSet res = stmt.executeQuery()) {
                if (res.next()) {
                    return true;
                }
            }
        } catch (SQLException e) {
            //  Log.error(GiftService.class, e);
        }

        return false;
    }

    private void addUsedGiftCode(int playerID, int giftCodeId, String code) {
        try {
            Timestamp timestamp = new Timestamp(System.currentTimeMillis());
            try (PreparedStatement stmt = AlyraManager.getConnection().prepareStatement("INSERT INTO `gift_code_histories`(`player_id`, `gift_code_id`, `code`, `created_at`) VALUES (?, ?, ?, ?)")) {
                stmt.setInt(1, playerID);
                stmt.setInt(2, giftCodeId);
                stmt.setString(3, code);
                stmt.setTimestamp(4, timestamp);
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
//            Logg.error(GiftService.class, e);
        }
    }
}
