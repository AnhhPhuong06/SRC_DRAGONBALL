package database.daos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import database.AlyraManager;
import models.player.Player;
import utils.Logger;


public class DonatePackageDAO {

    static {
        ensureTables(); 
    }

    public static class PackItem {

        public int tempId;
        public int quantity;
        public List<int[]> options = new ArrayList<>(); 
    }

    public static class Pack {

        public int id;
        public String name;
        public String detail;
        public long costVnd;
        public List<PackItem> items = new ArrayList<>();

        /** A purchase must still match the price and rewards reviewed in the NPC menu. */
        public boolean sameContents(Pack other) {
            if (other == null || id != other.id || costVnd != other.costVnd
                    || !java.util.Objects.equals(name, other.name) || !java.util.Objects.equals(detail, other.detail)
                    || items.size() != other.items.size()) return false;
            for (int i = 0; i < items.size(); i++) {
                PackItem a = items.get(i), b = other.items.get(i);
                if (a.tempId != b.tempId || a.quantity != b.quantity || a.options.size() != b.options.size()) return false;
                for (int j = 0; j < a.options.size(); j++) {
                    if (!java.util.Arrays.equals(a.options.get(j), b.options.get(j))) return false;
                }
            }
            return true;
        }
    }

    public static void ensureTables() {
        try (Connection con = AlyraManager.getConnection_Data()) {
            con.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS donate_package ("
                    + "id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,"
                    + "name VARCHAR(100) NOT NULL,"
                    + "detail VARCHAR(500) NOT NULL DEFAULT '',"
                    + "cost_vnd BIGINT NOT NULL DEFAULT 0,"
                    + "items TEXT NOT NULL,"
                    + "active TINYINT NOT NULL DEFAULT 1,"
                    + "sort_order INT NOT NULL DEFAULT 0"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            // vá bảng tạo từ bản cũ (cột từng tên cost_ruby / từng có cost_type) — lỗi thì bỏ qua
            try {
                con.createStatement().executeUpdate(
                        "ALTER TABLE donate_package CHANGE cost_ruby cost_vnd BIGINT NOT NULL DEFAULT 0");
            } catch (Exception ignored) {
            }
            try {
                con.createStatement().executeUpdate("ALTER TABLE donate_package DROP COLUMN cost_type");
            } catch (Exception ignored) {
            }
            con.createStatement().executeUpdate(
                    "CREATE TABLE IF NOT EXISTS donate_package_buy ("
                    + "package_id INT NOT NULL,"
                    + "account_id BIGINT NOT NULL,"
                    + "buy_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,"
                    + "PRIMARY KEY (package_id, account_id)"
                    + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            try {
                con.createStatement().executeUpdate(
                        "ALTER TABLE donate_package_buy CHANGE player_id account_id BIGINT NOT NULL");
            } catch (Exception ignored) {
            }
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi tạo bảng donate_package");
        }
    }

    public static List<Pack> getActivePackages() {
        List<Pack> packs = new ArrayList<>();
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT id, name, detail, cost_vnd, items FROM donate_package "
                        + "WHERE active = 1 ORDER BY sort_order, id")) {
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Pack p = new Pack();
                p.id = rs.getInt("id");
                p.name = rs.getString("name");
                p.detail = rs.getString("detail");
                p.costVnd = rs.getLong("cost_vnd");
                p.items = parseItems(rs.getString("items"));
                packs.add(p);
            }
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi load donate_package");
        }
        return packs;
    }

    public static List<PackItem> parseItems(String raw) {
        List<PackItem> items = new ArrayList<>();
        if (raw == null) {
            return items;
        }
        for (String line : raw.split("[\r\n;]+")) {
            line = line.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                String[] tokens = line.split("[\\s,]+");
                PackItem it = new PackItem();
                it.tempId = Integer.parseInt(tokens[0]);
                it.quantity = tokens.length > 1 && !tokens[1].contains(":")
                        ? Integer.parseInt(tokens[1]) : 1;
                for (int i = 1; i < tokens.length; i++) {
                    if (tokens[i].contains(":")) {
                        String[] op = tokens[i].split(":");
                        it.options.add(new int[]{Integer.parseInt(op[0]), Integer.parseInt(op[1])});
                    }
                }
                if (it.quantity > 0) {
                    items.add(it);
                }
            } catch (Exception ignore) {
                // dòng sai định dạng thì bỏ qua, không làm hỏng cả gói
            }
        }
        return items;
    }

    public static Set<Integer> getBoughtPackageIds(long accountId) {
        Set<Integer> ids = new HashSet<>();
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT package_id FROM donate_package_buy WHERE account_id = ?")) {
            ps.setLong(1, accountId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                ids.add(rs.getInt("package_id"));
            }
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi load donate_package_buy");
        }
        return ids;
    }

    public static boolean hasBought(int packageId, long accountId) {
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "SELECT 1 FROM donate_package_buy WHERE package_id = ? AND account_id = ?")) {
            ps.setInt(1, packageId);
            ps.setLong(2, accountId);
            ResultSet rs = ps.executeQuery();
            return rs.next();
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi check donate_package_buy");
            return true; // lỗi DB thì coi như đã mua — thà chặn nhầm còn hơn phát trùng
        }
    }

    public static void unmarkBought(int packageId, long accountId) {
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "DELETE FROM donate_package_buy WHERE package_id = ? AND account_id = ?")) {
            ps.setInt(1, packageId);
            ps.setLong(2, accountId);
            ps.executeUpdate();
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi gỡ donate_package_buy");
        }
    }

    public static boolean markBought(int packageId, long accountId) {
        try (Connection con = AlyraManager.getConnection_Data();
                PreparedStatement ps = con.prepareStatement(
                        "INSERT IGNORE INTO donate_package_buy (package_id, account_id) VALUES (?, ?)")) {
            ps.setInt(1, packageId);
            ps.setLong(2, accountId);
            return ps.executeUpdate() == 1;
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi ghi donate_package_buy");
            return false;
        }
    }

    public static boolean chargeVnd(Player player, long amount) {
        try (Connection con = AlyraManager.getConnection();
                PreparedStatement ps = con.prepareStatement(
                        "UPDATE account SET sotien = sotien - ? WHERE id = ? AND sotien >= ?")) {
            ps.setLong(1, amount);
            ps.setInt(2, player.getSession().userId);
            ps.setLong(3, amount);
            if (ps.executeUpdate() == 1) {
                player.getSession().sotien -= (int) amount;
                return true;
            }
            return false; // số dư thật trong DB không đủ
        } catch (Exception e) {
            Logger.logException(DonatePackageDAO.class, e, "Lỗi trừ sotien mua gói donate");
            return false;
        }
    }
}
