package services.top;

import database.AlyraManager;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ObjIntConsumer;
import models.item.Item;
import models.player.Player;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import services.ItemService;
import utils.Logger;

/**
 * Nạp danh sách top từ bảng player. Các bảng top chỉ khác nhau ở biểu thức lấy điểm và
 * chỗ gán điểm vào Player; phần còn lại - tên, đầu, giới tính, sức mạnh, đồ đang mặc -
 * đều giống hệt nhau nên gom về đây thay vì mỗi bảng top chép một bản.
 */
public final class TopPlayerLoader {

    private TopPlayerLoader() {
    }

    /**
     * @param bieuThucDiem biểu thức SQL lấy điểm, phải là hằng trong code chứ không phải
     * dữ liệu người chơi vì được nối thẳng vào câu truy vấn
     * @param ganDiem gán điểm đọc được vào đúng trường của Player
     */
    public static List<Player> load(String bieuThucDiem, int gioiHan, ObjIntConsumer<Player> ganDiem) {
        List<Player> list = new ArrayList<>();
        String sql = "SELECT *, " + bieuThucDiem + " AS diemTop FROM player ORDER BY diemTop DESC LIMIT " + gioiHan;
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                Player player = new Player();
                player.id = rs.getInt("id");
                player.name = rs.getString("name");
                player.head = rs.getShort("head");
                player.gender = rs.getByte("gender");
                ganDiem.accept(player, rs.getInt("diemTop"));
                docSucManh(rs.getString("data_point"), player);
                docDoDangMac(rs.getString("items_body"), player);
                list.add(player);
            }
        } catch (Exception e) {
            Logger.logException(TopPlayerLoader.class, e);
        }
        return list;
    }

    private static void docSucManh(String dataPoint, Player player) {
        JSONArray dataArray = (JSONArray) JSONValue.parse(dataPoint);
        player.nPoint.power = Long.parseLong(dataArray.get(11).toString());
        dataArray.clear();
    }

    /** getHead/getBody/getLeg đọc từ đồ đang mặc nên phải dựng lại danh sách này. */
    private static void docDoDangMac(String itemsBody, Player player) {
        JSONArray dataArray = (JSONArray) JSONValue.parse(itemsBody);
        for (Object itemDataObject : dataArray) {
            player.inventory.itemsBody.add(docMotMon(itemDataObject.toString()));
        }
        dataArray.clear();
    }

    private static Item docMotMon(String itemData) {
        JSONArray dataObject = (JSONArray) JSONValue.parse(itemData);
        short tempId = Short.parseShort(String.valueOf(dataObject.get(0)));
        if (tempId == -1) {
            return ItemService.gI().createItemNull();
        }
        Item item = ItemService.gI().createNewItem(tempId, Integer.parseInt(String.valueOf(dataObject.get(1))));
        JSONArray options = (JSONArray) JSONValue.parse(String.valueOf(dataObject.get(2)).replaceAll("\"", ""));
        for (Object option : options) {
            JSONArray opt = (JSONArray) JSONValue.parse(String.valueOf(option));
            item.itemOptions.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get(0))),
                    Integer.parseInt(String.valueOf(opt.get(1)))));
        }
        long cTime = 0;
        if (dataObject.size() > 3 && dataObject.get(3) != null) {
            try {
                cTime = Long.parseLong(String.valueOf(dataObject.get(3)));
            } catch (Exception ignored) {}
        }
        item.createTime = cTime;
        return ItemService.gI().isOutOfDateTime(item) ? ItemService.gI().createItemNull() : item;
    }
}
