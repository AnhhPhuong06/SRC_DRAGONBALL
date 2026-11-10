package managers;

 


import database.AlyraManager;
import java.sql.Connection;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import models.ConsignItem;
import org.json.simple.JSONValue;

public class ConsignShopManager {

    private static ConsignShopManager instance;

    public static ConsignShopManager gI() {
        if (instance == null) {
            instance = new ConsignShopManager();
        }
        return instance;
    }


    public String[] tabName = {"Trang bị", "Phụ kiện", "Hỗ trợ", "Linh tinh", ""};

    public List<ConsignItem> listItem = new ArrayList<>();

    public void save() {
        synchronized (listItem) {
        try (Connection con = AlyraManager.getConnection()) {
            database.ConsignStore.save(con, listItem);
            System.err.println("Save ký gửi thành công " + listItem.size());
        } catch (Exception e) {
            e.printStackTrace();
        }
        }
    }
}
