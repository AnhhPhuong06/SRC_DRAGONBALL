package server.ui;

import database.AlyraManager;
import models.Template.ItemTemplate;
import services.ItemService;
import services.VongQuayThoiVangConfig;
import services.func.MocVongQuayService;
import utils.Util;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static server.ui.ServerGuiUtils.*;

/**
 * Tiện ích tải và hiển thị Icon vật phẩm trực tiếp trên các Panel giao diện Admin & Server Manager.
 */
public class ItemIconHelper {

    private static final String ICON_FOLDER = "data/icon/";
    private static final Map<Integer, ImageIcon> iconCache = new ConcurrentHashMap<>();
    private static final Map<Integer, Integer> itemToIconIdMap = new ConcurrentHashMap<>();
    private static final Map<Integer, String> itemNameMap = new ConcurrentHashMap<>();

    static {
        // Khởi tạo trước một số vật phẩm thông dụng
        precache(457, 4028, "Thỏi vàng");
        precache(1618, 20661, "Hộp Hoàng Kim");
        precache(987, 9406, "Đá bảo vệ");
        precache(1277, 14249, "Mảnh Khỉ Oozaru");
        precache(1177, 11880, "Rương Mảnh Thiên Sứ");
        precache(1593, 17252, "Cải Trang Broly SSJ4");
        precache(1241, 14712, "Cải Trang Broly SSJ4 VIP");
        precache(16, 421, "Ngọc Rồng 3 sao");
        precache(17, 422, "Ngọc Rồng 4 sao");
        precache(18, 423, "Ngọc Rồng 5 sao");
        precache(19, 424, "Ngọc Rồng 6 sao");
        precache(20, 425, "Ngọc Rồng 7 sao");
        precache(674, 6467, "Đá ngũ sắc");
        precache(1357, 20657, "Bình x3 tiềm năng, sức mạnh");
        precache(1358, 20658, "Bình x5 tiềm năng, sức mạnh");
        precache(1359, 20659, "Bình x7 tiềm năng, sức mạnh");
        precache(1454, 15921, "Hộp Cải Trang Hit VIP");
        precache(1959, 20661, "Hộp Ngọc Rồng Băng");
        precache(1338, 14881, "Mảnh sắt vụn");
        precache(861, 7743, "Hồng ngọc");
    }

    private static void precache(int itemId, int iconId, String name) {
        itemToIconIdMap.put(itemId, iconId);
        itemNameMap.put(itemId, name);
    }

    /**
     * Lấy icon của vật phẩm theo Item ID và kích thước mong muốn.
     */
    public static ImageIcon getItemIcon(int itemId, int size) {
        int cacheKey = (itemId << 8) | (size & 0xFF);
        if (iconCache.containsKey(cacheKey)) {
            return iconCache.get(cacheKey);
        }

        int iconId = getIconId(itemId);
        ImageIcon icon = loadIconFromDisk(iconId, size);
        if (icon == null && iconId != itemId) {
            icon = loadIconFromDisk(itemId, size);
        }

        if (icon != null) {
            iconCache.put(cacheKey, icon);
            return icon;
        }

        // Tạo icon thay thế nếu không tìm thấy ảnh
        ImageIcon placeholder = createPlaceholderIcon(itemId, size);
        iconCache.put(cacheKey, placeholder);
        return placeholder;
    }

    /**
     * Lấy Icon ID từ Template hoặc Database.
     */
    public static int getIconId(int itemId) {
        if (itemToIconIdMap.containsKey(itemId)) {
            return itemToIconIdMap.get(itemId);
        }

        // 1. Thử lấy từ ItemTemplate trong bộ nhớ
        try {
            ItemTemplate temp = ItemService.gI().getTemplate(itemId);
            if (temp != null && temp.iconID > 0) {
                itemToIconIdMap.put(itemId, (int) temp.iconID);
                if (temp.name != null && !temp.name.trim().isEmpty()) {
                    itemNameMap.put(itemId, temp.name);
                }
                return temp.iconID;
            }
        } catch (Exception ignored) {
        }

        // 2. Thử truy vấn cơ sở dữ liệu nếu có kết nối
        try (Connection con = AlyraManager.getConnection_Data();
             PreparedStatement ps = con.prepareStatement("SELECT icon_id, name FROM item_template WHERE id = ?")) {
            ps.setInt(1, itemId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    int ic = rs.getInt("icon_id");
                    String nm = rs.getString("name");
                    itemToIconIdMap.put(itemId, ic);
                    if (nm != null && !nm.trim().isEmpty()) {
                        itemNameMap.put(itemId, nm);
                    }
                    return ic;
                }
            }
        } catch (Exception ignored) {
        }

        itemToIconIdMap.put(itemId, itemId);
        return itemId;
    }

    /**
     * Lấy tên vật phẩm.
     */
    public static String getItemName(int itemId) {
        if (itemNameMap.containsKey(itemId)) {
            return itemNameMap.get(itemId);
        }
        String name = VongQuayThoiVangConfig.getItemName(itemId);
        if (name != null) {
            itemNameMap.put(itemId, name);
            return name;
        }
        return "Vật phẩm [" + itemId + "]";
    }

    /**
     * Tải file icon từ thư mục data/icon (ưu tiên x4, rồi x3, x2, x1).
     */
    public static ImageIcon loadIconFromDisk(int iconFileId, int size) {
        if (iconFileId <= 0) return null;
        String[] zooms = {"x4", "x3", "x2", "x1"};
        for (String z : zooms) {
            File f = new File(ICON_FOLDER + z + "/" + iconFileId + ".png");
            if (f.exists()) {
                try {
                    BufferedImage img = ImageIO.read(f);
                    if (img != null) {
                        Image scaled = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                        return new ImageIcon(scaled);
                    }
                } catch (Exception ignored) {
                }
            }
        }
        return null;
    }

    /**
     * Tạo icon thay thế thanh lịch khi file png không tồn tại.
     */
    private static ImageIcon createPlaceholderIcon(int itemId, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(PRIMARY_SOFT);
        g2.fillRoundRect(0, 0, size, size, 6, 6);

        g2.setColor(PRIMARY_GRAD_A);
        g2.drawRoundRect(0, 0, size - 1, size - 1, 6, 6);

        g2.setColor(PRIMARY);
        g2.setFont(new Font("Segoe UI", Font.BOLD, Math.max(9, size / 2)));
        FontMetrics fm = g2.getFontMetrics();
        String txt = String.valueOf(itemId % 100);
        int tw = fm.stringWidth(txt);
        int th = fm.getAscent();
        g2.drawString(txt, (size - tw) / 2, (size + th) / 2 - 2);

        g2.dispose();
        return new ImageIcon(img);
    }

    /**
     * Tạo một Card Chip hiển thị trực quan vật phẩm: Icon (26x26) + Số lượng + Tên + Option pills.
     */
    public static JPanel createItemChip(MocVongQuayService.RewardItem item) {
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        chip.setOpaque(true);
        chip.setBackground(SURFACE);
        chip.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER_STRONG, 1, true),
                new EmptyBorder(3, 5, 3, 8)
        ));

        // 1. Icon ảnh của vật phẩm
        ImageIcon icon = getItemIcon(item.tempId, 24);
        JLabel lblIcon = new JLabel(icon);
        chip.add(lblIcon);

        // 2. Tên & Số lượng
        String name = getItemName(item.tempId);
        JLabel lblText = new JLabel("x" + Util.numberToMoney(item.quantity) + " " + name);
        lblText.setFont(FONT_BODY_B);
        lblText.setForeground(TEXT);
        chip.add(lblText);

        // 3. Option Tags nếu có
        if (item.options != null && item.options.length > 0) {
            for (int[] opt : item.options) {
                JLabel optTag = createOptionTag(opt[0], opt[1]);
                chip.add(optTag);
            }
        }

        // Tooltip hiển thị đầy đủ
        chip.setToolTipText("ID Vật phẩm: " + item.tempId + " | Icon ID: " + getIconId(item.tempId) + " | Số lượng: " + item.quantity);
        return chip;
    }

    /**
     * Tạo nhãn tag nhỏ cho option (ví dụ: [Khóa], [+30% SĐ]).
     */
    private static JLabel createOptionTag(int optId, int param) {
        String optText = VongQuayThoiVangConfig.getOptionShortName(optId, param);
        JLabel tag = new JLabel(optText);
        tag.setFont(new Font("Segoe UI", Font.BOLD, 10));
        tag.setOpaque(true);
        if (optId == 30) {
            tag.setBackground(DANGER_SOFT);
            tag.setForeground(DANGER);
            tag.setBorder(new EmptyBorder(1, 4, 1, 4));
        } else {
            tag.setBackground(VIOLET_SOFT);
            tag.setForeground(VIOLET);
            tag.setBorder(new EmptyBorder(1, 4, 1, 4));
        }
        return tag;
    }

    /**
     * Tạo Chip nhỏ hiển thị [Icon 20x20] {ID}: {Tên} cho danh sách nhóm vật phẩm.
     */
    public static JPanel createMiniBadge(int itemId) {
        JPanel badge = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        badge.setOpaque(true);
        badge.setBackground(SURFACE);
        badge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(2, 4, 2, 6)
        ));

        ImageIcon icon = getItemIcon(itemId, 18);
        JLabel lblIcon = new JLabel(icon);
        badge.add(lblIcon);

        String name = getItemName(itemId);
        JLabel lblName = new JLabel(itemId + ": " + name);
        lblName.setFont(FONT_SMALL_B);
        lblName.setForeground(TEXT_SECONDARY);
        badge.add(lblName);

        badge.setToolTipText("ID: " + itemId + " - " + name);
        return badge;
    }

    /**
     * Tạo bảng danh sách các Chip vật phẩm từ chuỗi cấu hình mốc (Dùng WrapLayout tự xuống dòng).
     */
    public static JPanel renderRewardsPanel(String rewardsStr) {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_XS, SP_XS));
        panel.setOpaque(false);

        List<MocVongQuayService.RewardItem> list = VongQuayThoiVangConfig.parseRewards(rewardsStr);
        if (list.isEmpty()) {
            JLabel lblEmpty = new JLabel("Chưa cấu hình vật phẩm nào cho mốc này.");
            lblEmpty.setFont(FONT_SMALL);
            lblEmpty.setForeground(TEXT_MUTED);
            panel.add(lblEmpty);
            return panel;
        }

        for (MocVongQuayService.RewardItem item : list) {
            panel.add(createItemChip(item));
        }
        return panel;
    }

    /**
     * Tạo dòng hiển thị các icon cho một mảng ID vật phẩm (Ví dụ cho Card 2).
     */
    public static JPanel renderItemGroupIcons(int[] itemIds) {
        JPanel panel = new JPanel(new WrapLayout(FlowLayout.LEFT, 4, 4));
        panel.setOpaque(false);
        if (itemIds == null || itemIds.length == 0) {
            return panel;
        }
        for (int id : itemIds) {
            panel.add(createMiniBadge(id));
        }
        return panel;
    }
}
