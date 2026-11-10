package database.daos;




import models.item.Item;
import models.shop.ItemShop;
import models.shop.Shop;
import models.shop.TabShop;
import services.ItemService;
import utils.Logger;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class ShopDAO {

    public static List<Shop> getShops(Connection con) {
        List<Shop> list = new ArrayList<>();
        try {
            PreparedStatement ps = con.prepareStatement("select * from shop order by npc_id asc");
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Shop shop = new Shop();
                shop.id = rs.getInt("id");
                shop.npcId = rs.getByte("npc_id");
                shop.tagName = rs.getString("tag_name");
                shop.typeShop = rs.getByte("type_shop");
                loadShopTab(con, shop);
                list.add(shop);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
            return null;
        }
        return list;
    }

    private static void loadShopTab(Connection con, Shop shop) {
        try {
            PreparedStatement ps = con
                    .prepareStatement("select * from tab_shop where shop_id = ? order by tab_index asc");
            ps.setInt(1, shop.id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                TabShop tab = new TabShop();
                tab.shop = shop;
                tab.id = rs.getInt("id");
                tab.name = rs.getString("tab_name").replaceAll("<>", "\n");
                tab.index = rs.getInt("tab_index");
                // Dùng luôn cột items của chính dòng này thay vì mở query lồng nhau:
                // bản cũ query lại tab_shop theo (tab_index, shop_id) — không phải khoá chính —
                // nên hai tab trùng tab_index sẽ nuốt item của nhau.
                loadItemShop(tab, rs.getString("items"));
                shop.tabShops.add(tab);
            }
            try {
                if (rs != null) {
                    rs.close();
                }
                if (ps != null) {
                    ps.close();
                }
            } catch (SQLException ex) {
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }

    private static void loadItemShop(TabShop tabShop, String itemsJson) {
        if (itemsJson == null || itemsJson.trim().isEmpty()) {
            return;
        }
        try {
            Object parsed = JSONValue.parse(itemsJson);
            if (!(parsed instanceof JSONArray dataArray)) {
                Logger.error("ShopDAO: tab_shop id=" + tabShop.id + " có cột items không phải JSON array\n");
                return;
            }
            for (Object o : dataArray) {
                if (!(o instanceof JSONObject dataObject)) {
                    continue;
                }
                ItemShop itemShop = new ItemShop();
                itemShop.tabShop = tabShop;
                itemShop.id = tabShop.itemShops.size() + 1;
                itemShop.temp = ItemService.gI()
                        .getTemplate(Short.parseShort(String.valueOf(dataObject.get("temp_id"))));
                itemShop.isNew = Boolean.parseBoolean(String.valueOf(dataObject.get("is_new")));
                itemShop.cost = Long.parseLong(String.valueOf(dataObject.get("cost")));
                itemShop.iconSpec = Integer.parseInt(String.valueOf(dataObject.get("item_spec")));
                itemShop.typeSell = Byte.parseByte(String.valueOf(dataObject.get("type_sell")));
                JSONArray options = (JSONArray) dataObject.get("options");
                if (options != null) {
                    for (int j = 0; j < options.size(); j++) {
                        JSONObject opt = (JSONObject) options.get(j);
                        itemShop.options.add(new Item.ItemOption(Integer.parseInt(String.valueOf(opt.get("id"))),
                                Integer.parseInt(String.valueOf(opt.get("param")))));
                    }
                }
                boolean isSell = Boolean.parseBoolean(String.valueOf(dataObject.get("is_sell")));
                if (isSell) {
                    tabShop.itemShops.add(itemShop);
                }
            }
        } catch (Exception e) {
            Logger.logException(ShopDAO.class, e);
        }
    }
}
