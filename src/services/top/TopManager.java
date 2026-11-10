/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package services.top;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import utils.Logger;

/**
 *
 * @author Son
 */
public class TopManager {

    public static final ArrayList<TopTemplate> TOP_TEMPLATE = new ArrayList<>();
    public static final ArrayList<TOPAUTO> AUTO_GIFT_TOPS = new ArrayList<>();
    public static String[] SELECT = new String[0];

    public static class TopTemplate {

        public int id;
        public String name;
        public String query;
        public final ArrayList<TOP> tops = new ArrayList<>();

        public TopTemplate(int id, String name, String query) {
            this.id = id;
            this.name = name;
            this.query = query;
        }
    }

    public static void setSelectNpc() {
        SELECT = new String[TOP_TEMPLATE.size()];
        for (int i = 0; i < SELECT.length; i++) {
            SELECT[i] = "Top\n" + TOP_TEMPLATE.get(i).name + "\n[" + TOP_TEMPLATE.get(i).id + "]";
        }
    }

    public static synchronized void loadTop(Connection con) {
        try (PreparedStatement ps = con.prepareStatement("select * from top_template"); ResultSet rs = ps.executeQuery()) {
            TOP_TEMPLATE.clear();
            AUTO_GIFT_TOPS.clear();
            while (rs.next()) {
                TopTemplate temp = new TopTemplate(rs.getInt("id"), rs.getString("name"), rs.getString("query"));
                TOP_TEMPLATE.add(temp);
                try {
                    AUTO_GIFT_TOPS.add(new TOPAUTO(rs, temp));
                } catch (Exception eGift) {
                    Logger.logException(TopManager.class, eGift, "Lỗi tạo TOPAUTO cho " + temp.name);
                }
            }
            Logger.log("TOPS -> ");
            Logger.logln(Logger.RED, "(" + TOP_TEMPLATE.size() + ")");
            TopService.gI().loadListTop(con);
            Logger.log("AUTO_GIFT_TOPS -> ");
            Logger.logln(Logger.RED, "(" + AUTO_GIFT_TOPS.size() + ")");
            setSelectNpc();
        } catch (Exception e) {
            Logger.logException(TopManager.class, e);
        }
    }
}
