package server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.io.File;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.security.SecureRandom;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

public class GiftcodePanel extends JPanel {

    // --- Cấu hình Icon ---
    private static final String ICON_FOLDER = "data/icon/";

    private JTable table;
    private DefaultTableModel model;

    // Font mono cho mã code
    private static final Font FONT_CODE = FONT_MONO.deriveFont(Font.BOLD, 13f);

    // Cache dữ liệu
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, String> optionTemplateMap = new HashMap<>();

    private final List<ItemData> listAllItems = new ArrayList<>();

    private static class ItemData {
        int id;
        String name;
        int type;
        int gender;
        public ItemData(int id, String name, int type, int gender) {
            this.id = id; this.name = name; this.type = type; this.gender = gender;
        }
    }

    // Cache Icon (RAM)
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();

    public GiftcodePanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));

        // 1. Nạp dữ liệu nền
        new Thread(this::loadCacheData).start();

        // 2. Giao diện chính
        initUI();

        // 3. Tải danh sách Giftcode
        loadDataFromDB();
    }

    // --- RENDERER HELPERS (đồng bộ với styleTable: zebra + hover + selection) ---
    private static Color zebraBackground(JTable t, int row, boolean isSelected) {
        if (isSelected) return PRIMARY_SOFT;
        Object hoverObj = t.getClientProperty("ui.hoverRow");
        int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
        if (row == hover) return HOVER_ROW;
        return row % 2 == 0 ? SURFACE : SURFACE_ALT;
    }

    private static class ZebraCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            lbl.setIcon(null);
            lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
            lbl.setBackground(zebraBackground(t, row, isSelected));
            lbl.setForeground(TEXT);
            lbl.setFont(FONT_BODY);
            lbl.setHorizontalAlignment(SwingConstants.LEFT);
            return lbl;
        }
    }

    /** Cột mã code: font mono + màu thương hiệu. */
    private static class CodeCellRenderer extends ZebraCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            lbl.setFont(FONT_CODE);
            lbl.setForeground(PRIMARY);
            return lbl;
        }
    }

    /** Cột icon vật phẩm: giữ zebra nhưng vẽ ImageIcon căn giữa. */
    private static class IconCellRenderer extends ZebraCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, "", isSelected, hasFocus, row, column);
            lbl.setIcon(value instanceof Icon ? (Icon) value : null);
            lbl.setHorizontalAlignment(SwingConstants.CENTER);
            return lbl;
        }
    }

    /** Cột ngày tạo: định dạng dd/MM/yyyy HH:mm, màu phụ. */
    private static class DateCellRenderer extends ZebraCellRenderer {
        private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private final SimpleDateFormat viewFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            try {
                lbl.setText(viewFmt.format(dbFmt.parse(String.valueOf(value))));
            } catch (Exception e) {
                lbl.setText(value != null ? value.toString() : "");
            }
            lbl.setForeground(TEXT_SECONDARY);
            return lbl;
        }
    }

    /** Cột hết hạn: ngày + badge trạng thái ("Đã hết" / "Còn hạn"). */
    private static class ExpiryCellRenderer extends JPanel implements TableCellRenderer {
        private final SimpleDateFormat dbFmt = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        private final SimpleDateFormat viewFmt = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        private final JLabel lblDate = new JLabel();
        private final JLabel badgeActive = badgeSuccess("Còn hạn");
        private final JLabel badgeExpired = badgeDanger("Đã hết");

        ExpiryCellRenderer() {
            super(new GridBagLayout());
            setOpaque(true);
            lblDate.setFont(FONT_BODY);
            GridBagConstraints gc = new GridBagConstraints();
            gc.gridy = 0; gc.anchor = GridBagConstraints.WEST;
            gc.gridx = 0; gc.insets = new Insets(0, SP_MD, 0, SP_SM);
            add(lblDate, gc);
            gc.gridx = 1; gc.insets = new Insets(0, 0, 0, 0);
            add(badgeActive, gc);
            gc.gridx = 2;
            add(badgeExpired, gc);
            JPanel filler = new JPanel();
            filler.setOpaque(false);
            gc.gridx = 3; gc.weightx = 1; gc.fill = GridBagConstraints.HORIZONTAL;
            add(filler, gc);
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            setBackground(zebraBackground(t, row, isSelected));
            String raw = value != null ? value.toString() : "";
            String txt = raw;
            Date d = null;
            try {
                d = dbFmt.parse(raw);
                txt = viewFmt.format(d);
            } catch (Exception ignored) { }
            lblDate.setText(txt);
            lblDate.setForeground(TEXT);
            boolean parsed = d != null;
            boolean expired = parsed && d.before(new Date());
            badgeExpired.setVisible(parsed && expired);
            badgeActive.setVisible(parsed && !expired);
            return this;
        }
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    // --- LOAD CACHE ---
    private void loadCacheData() {
        listAllItems.clear();
        try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, type, gender FROM item_template")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int iconId = rs.getInt("icon_id");
                    int type = rs.getInt("type");
                    int gender = rs.getInt("gender");

                    itemTemplateMap.put(id, name);
                    itemIconMap.put(id, iconId);
                    listAllItems.add(new ItemData(id, name, type, gender));
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM item_option_template")) {
                while (rs.next()) optionTemplateMap.put(rs.getInt("id"), services.ItemService.resolveOptionName(rs.getInt("id"), rs.getString("name")));
            } catch (Exception e) {
                initHardcodedOptionData();
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private void initHardcodedOptionData() {
        // Các option thường dùng trong game
        String rawData =
            "0\tTấn công+#\n" +
            "1\tNăng lượng+#%\n" +
            "2\tMay mắn+#%\n" +
            "3\tTuyệt kỹ+#%\n" +
            "4\tBùa lửa+#\n" +
            "5\tBùa nước+#\n" +
            "6\tBùa gió+#\n" +
            "7\tBùa đất+#\n" +
            "8\tKháng+#%\n" +
            "9\tNé tránh+#%\n" +
            "10\tHút máu+#%\n" +
            "11\tPhản đòn+#%\n" +
            "12\tGiáp+#\n" +
            "13\tSức đánh+#\n" +
            "14\tChí mạng+#%\n" +
            "15\tXuyên giáp+#%\n" +
            "16\tHồi HP+#%\n" +
            "17\tHồi KI+#%\n" +
            "18\tTăng sức mạnh+#\n" +
            "19\tTăng thể lực+#\n" +
            "20\tTăng nhanh nhẹn+#\n" +
            "21\tTăng may mắn+#\n" +
            "22\tKỹ năng+#\n" +
            "23\tTrứng+#\n" +
            "24\tNgọc rồng+#\n" +
            "25\tĐệ tử+#\n" +
            "26\tLinh thú+#\n" +
            "27\tVIP+#\n" +
            "28\tBảo vệ+#\n" +
            "29\tMở khóa+#\n" +
            "30\tKhóa giao dịch\n" +
            "31\tCường hóa+#\n" +
            "32\tGhép ngọc+#\n" +
            "33\tĐục lỗ+#\n" +
            "34\tPha lê+#\n" +
            "35\tNgọc xanh+#\n" +
            "36\tNgọc tím+#\n" +
            "37\tNgọc vàng+#\n" +
            "38\tHồng ngọc+#\n" +
            "39\tBạch ngọc+#\n" +
            "40\tEXP+#%\n" +
            "41\tTan gạch+#\n" +
            "42\tHóa giải+#\n" +
            "43\tMiễn thường\n" +
            "44\tKháng phép+#%\n" +
            "45\tKháng vật lý+#%\n" +
            "46\tTăng SM#+#\n" +
            "47\tTăng HP tối đa+#\n" +
            "48\tTăng KI tối đa+#\n" +
            "49\tTăng sức đánh+#%\n" +
            "50\tSức đánh+#%\n" +
            "51\tPhòng thủ+#%\n" +
            "52\tNăng lượng+#\n" +
            "53\tThể lực+#\n" +
            "54\tNhanh nhẹn+#\n" +
            "55\tMay mắn+#\n" +
            "56\tTuyệt kỹ+#\n" +
            "57\tKỹ năng+#\n" +
            "58\tTrí tuệ+#\n" +
            "59\tSức mạnh+#\n" +
            "60\tHỗ trợ+#\n" +
            "61\tTấn công+#\n" +
            "62\tPhòng thủ+#\n" +
            "63\tHồi phục+#\n" +
            "64\tTăng trưởng+#\n" +
            "65\tGiảm HP địch+#%\n" +
            "66\tTăngdamage+\n" +
            "67\tHPDMTang+\n" +
            "68\tKIDMTang+\n" +
            "69\tGDMTang+\n" +
            "70\tTien hoa+\n" +
            "71\tThoát\n" +
            "72\tNhanh\n" +
            "73\tChuuyen nhom\n" +
            "74\tNhom\n" +
            "75\tNhanh+#\n" +
            "76\tNhanh+\n" +
            "77\tHP+#%\n" +
            "78\tHP+#\n" +
            "79\tKI+#\n" +
            "80\tHP hồi phụ+#\n" +
            "81\tKI hồi phụ+#\n" +
            "82\tHP+#+%\n" +
            "83\tKI+#+%\n" +
            "84\tVàng+#\n" +
            "85\tNgọc+#\n" +
            "86\tSức mạnh+#+%\n" +
            "87\tThể lực+#+%\n" +
            "88\tNhanh nhẹn+#+%\n" +
            "89\tMay mắn+#+%\n" +
            "90\tTrí tuệ+#+%\n" +
            "91\tKinh nghiệm+#%\n" +
            "92\tNhiệt lạnh+#%\n" +
            "93\tĐiện từ+#%\n" +
            "94\tSiêu nhiên+#%\n" +
            "95\tBão tố+#%\n" +
            "96\tThời gian+#\n" +
            "97\tSố lượng+#\n" +
            "98\tBảo hiểm\n" +
            "99\tHỗ trợ\n" +
            "100\tTăng kinh nghiệm\n" +
            "101\tBoss\n" +
            "102\tThử thách\n" +
            "103\tKI +#%\n" +
            "104\tGiáp năng lượng\n" +
            "105\tHút năng lượng\n" +
            "106\tPhản năng lượng\n" +
            "107\tHút máu\n" +
            "108\tTrọng kích\n" +
            "109\tXuyên thấu\n" +
            "110\tTăng sức mạnh\n" +
            "111\tTăng nhanh\n" +
            "112\tTăng HP\n" +
            "113\tTăng KI\n" +
            "114\tTăng năng lượng\n" +
            "115\tTăng giáp\n" +
            "116\tKháng chiêu\n" +
            "117\tMiễn thương\n" +
            "118\tKháng độc\n" +
            "119\tKháng lửa\n" +
            "120\tKháng nước\n" +
            "121\tKháng gió\n" +
            "122\tKháng đất\n" +
            "123\tTăng lực\n" +
            "124\tTăng trí\n" +
            "125\tTăng nhanh\n" +
            "126\tTăng kinh\n" +
            "127\tHồi phục\n" +
            "128\tTăng sm\n" +
            "129\tMở khóa\n" +
            "130\tBỏ trứng\n" +
            "131\tĐổi ngọc\n" +
            "132\tBán\n" +
            "133\tNâng cấp\n" +
            "134\tGhép\n" +
            "135\tTìm kiếm\n" +
            "136\tPhân biệt\n" +
            "137\tẨn danh\n" +
            "138\tGhi nhớ\n" +
            "139\tHọc thuộc\n" +
            "140\tĐánh giá\n" +
            "141\tBình chọn\n" +
            "142\tQuyên góp\n" +
            "143\tTặng quà\n" +
            "144\tNhận quà\n" +
            "145\tMở rộng\n" +
            "146\tThu hồi\n" +
            "147\tNạp xu\n" +
            "148\tNạp ngọc\n" +
            "149\tRút xu\n" +
            "150\tRút ngọc";

        for (String line : rawData.split("\n")) {
            String[] p = line.split("\t");
            if (p.length >= 2) {
                try {
                    optionTemplateMap.put(Integer.parseInt(p[0]), services.ItemService.resolveOptionName(Integer.parseInt(p[0]), p[1]));
                } catch (NumberFormatException e) {
                    // Bỏ qua dòng không hợp lệ
                }
            }
        }
    }

    private String getItemName(int id) { return itemTemplateMap.getOrDefault(id, "Unknown Item (" + id + ")"); }
    private String getOptionName(int id) { return optionTemplateMap.getOrDefault(id, "Option " + id); }

    // --- LOAD ICON ---
    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId)) return iconCache.get(itemId);
        if (noIconCache.containsKey(itemId)) return null;

        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId == -1) {
                noIconCache.put(itemId, true);
                return null;
            }
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            File f = null;
            for (String zoom : zoomLevels) {
                f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) break;
            }
            if (f != null && f.exists()) {
                BufferedImage img = ImageIO.read(f);
                Image dimg = img.getScaledInstance(22, 22, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(dimg);
                iconCache.put(itemId, icon);
                return icon;
            }
        } catch (Exception e) { }
        noIconCache.put(itemId, true);
        return null;
    }

    // --- GIAO DIỆN CHÍNH ---
    private void initUI() {
        // Toolbar hành động (góc phải header)
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        actions.setOpaque(false);

        JButton btnReload = button("Tải lại", ButtonVariant.SECONDARY, "refresh");
        JButton btnDelete = button("Xóa Code", ButtonVariant.DANGER, "broom");
        JButton btnAddShare = button("Tạo Code Share (50 mã)", ButtonVariant.SUCCESS, "gift");
        JButton btnAdd = button("Tạo Code Mới", ButtonVariant.PRIMARY, "plus");

        btnAddShare.addActionListener(e -> openGiftcodeShareDialog());
        btnAdd.addActionListener(e -> openGiftcodeEditor(null));
        btnReload.addActionListener(e -> loadDataFromDB());
        btnDelete.addActionListener(e -> deleteSelectedGiftcode());

        actions.add(btnReload); actions.add(btnDelete); actions.add(btnAddShare); actions.add(btnAdd);

        add(sectionHeader("Quản Lý Giftcode",
                "Tạo, chỉnh sửa và phân phối code quà tặng", actions),
                BorderLayout.NORTH);

        // Table
        String[] columns = {"ID", "Mã Code", "Lượt còn", "Ngày tạo", "Hết hạn", "Loại", "Người chơi", "Chi tiết (JSON)"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = styleTable(table);

        // Format cột Code (mono + màu brand)
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(1).setCellRenderer(new CodeCellRenderer());

        // Format cột Ngày tháng (Cột 3 và 4)
        table.getColumnModel().getColumn(3).setCellRenderer(new DateCellRenderer());
        table.getColumnModel().getColumn(4).setCellRenderer(new ExpiryCellRenderer());
        table.getColumnModel().getColumn(4).setPreferredWidth(210);

        table.getColumnModel().getColumn(6).setPreferredWidth(150); // Người chơi
        table.getColumnModel().getColumn(7).setPreferredWidth(300);

        // Double click sửa
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) openGiftcodeEditor(row);
                }
            }
        });

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_MD, SP_MD, SP_MD, SP_MD);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    private void loadDataFromDB() {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 // SỬA: Đổi DESC thành ASC để sắp xếp ID từ thấp đến cao
                 ResultSet rs = stmt.executeQuery("SELECT * FROM giftcode ORDER BY id ASC")) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("code"));
                    row.add(rs.getInt("count_left"));
                    row.add(rs.getString("datecreate")); // Giữ nguyên chuỗi gốc, renderer sẽ xử lý
                    row.add(rs.getString("expired"));
                    row.add(rs.getString("allGender")); // schema này dùng allGender thay cho 'type'
                    row.add(rs.getString("player_name")); // cột thật là player_name
                    row.add(rs.getString("detail"));
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void deleteSelectedGiftcode() {
        int row = table.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn dòng cần xóa!");
            return;
        }
        int id = (int) model.getValueAt(row, 0);
        String code = (String) model.getValueAt(row, 1);

        if (JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn xóa Code: " + code + "?", "Xác nhận xóa", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            new Thread(() -> {
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement("DELETE FROM giftcode WHERE id=?")) {
                    ps.setInt(1, id);
                    ps.executeUpdate();
                    // Nạp lại giftcode vào RAM sau khi xoá
                    try { server.GiftCodeAdmin.reload(); } catch (Exception ignoredReload) {}
                    SwingUtilities.invokeLater(this::loadDataFromDB);
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }
    }

    // ========================================================================
    // 1. EDITOR GIFTCODE (MAIN DIALOG)
    // ========================================================================
    private void openGiftcodeEditor(Integer rowIndex) {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Giftcode Editor", true);
        dialog.setSize(1100, 750);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setLocationRelativeTo(this);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        // --- Card Thông tin chung ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM); gbc.fill = GridBagConstraints.HORIZONTAL; gbc.anchor = GridBagConstraints.WEST;

        // Code & Count
        JTextField txtCode = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 1).toString() : "GIFT" + System.currentTimeMillis()/1000, 15);
        txtCode.setFont(FONT_MONO.deriveFont(Font.BOLD, 14f));
        txtCode.setForeground(PRIMARY);
        JTextField txtCount = new JTextField(rowIndex != null ? model.getValueAt(rowIndex, 2).toString() : "100", 10);

        gbc.gridx=0; gbc.gridy=0; form.add(label("Mã Code:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx=1; form.add(txtCode, gbc);
        gbc.gridx=2; form.add(label("Số lượng nhập:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx=3; form.add(txtCount, gbc);

        // Expired Date logic
        String expStr = rowIndex != null ? model.getValueAt(rowIndex, 4).toString() : "2030-01-01 00:00:00";
        // Parse simple string
        String[] dateTime = expStr.split(" ");
        String[] dateParts = dateTime[0].split("-");
        String[] timeParts = dateTime.length > 1 ? dateTime[1].split(":") : new String[]{"00","00","00"};

        JComboBox<Integer> cbYear = new JComboBox<>(); for(int i=2023;i<=2035;i++) cbYear.addItem(i); cbYear.setSelectedItem(Integer.parseInt(dateParts[0]));
        JComboBox<Integer> cbMonth = new JComboBox<>(); for(int i=1;i<=12;i++) cbMonth.addItem(i); cbMonth.setSelectedItem(Integer.parseInt(dateParts[1]));
        JComboBox<Integer> cbDay = new JComboBox<>(); for(int i=1;i<=31;i++) cbDay.addItem(i); cbDay.setSelectedItem(Integer.parseInt(dateParts[2]));
        JComboBox<Integer> cbHour = new JComboBox<>(); for(int i=0;i<24;i++) cbHour.addItem(i); cbHour.setSelectedItem(Integer.parseInt(timeParts[0]));
        JComboBox<Integer> cbMin = new JComboBox<>(); for(int i=0;i<60;i++) cbMin.addItem(i); cbMin.setSelectedItem(Integer.parseInt(timeParts[1]));

        JPanel pDate = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        pDate.setOpaque(false);
        pDate.add(label("Ngày", FONT_SMALL, TEXT_SECONDARY)); pDate.add(cbDay);
        pDate.add(label("/", FONT_SMALL, TEXT_MUTED)); pDate.add(cbMonth);
        pDate.add(label("/", FONT_SMALL, TEXT_MUTED)); pDate.add(cbYear);
        pDate.add(label(" | Giờ:", FONT_SMALL, TEXT_SECONDARY)); pDate.add(cbHour);
        pDate.add(label(":", FONT_SMALL, TEXT_MUTED)); pDate.add(cbMin);

        gbc.gridx=0; gbc.gridy=1; form.add(label("Thời hạn:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx=1; gbc.gridwidth=3; form.add(pDate, gbc); gbc.gridwidth=1;

        // Type & Note
        String[] typeDescs = {"0 - Tất cả mọi người", "1 - Chỉ thành viên kích hoạt", "2 - Chỉ tài khoản mới tạo (<7 ngày)"};
        JComboBox<String> cbType = new JComboBox<>(typeDescs);
        if(rowIndex != null) {
            int t = 0;
            Object rawType = model.getValueAt(rowIndex, 5);
            String sType = rawType == null ? "" : rawType.toString().trim();
            // cột thật là allGender (chuỗi "all"/"true"/số); "all"/"true" coi như 0 (tất cả)
            if (!"all".equalsIgnoreCase(sType) && !"true".equalsIgnoreCase(sType)) {
                try { t = Integer.parseInt(sType); } catch (NumberFormatException ex) { t = 0; }
            }
            if(t >= 0 && t < 3) cbType.setSelectedIndex(t);
        }

        gbc.gridx=0; gbc.gridy=2; form.add(label("Đối tượng:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx=1; gbc.gridwidth=2; form.add(cbType, gbc); gbc.gridwidth=1;

        // Tên người chơi được phép nhập code (cách nhau bởi dấu phẩy, để trống = tất cả)
        String playerVal = "";
        if (rowIndex != null) {
            Object pv = model.getValueAt(rowIndex, 6);
            playerVal = pv != null ? pv.toString() : "";
        }
        JTextField txtPlayer = new JTextField(playerVal, 30);
        JLabel lblPlayerHint = label("Để trống = mọi người chơi. Nhiều tên cách nhau bởi dấu phẩy (,)",
                FONT_TINY.deriveFont(Font.ITALIC), TEXT_MUTED);

        gbc.gridx=0; gbc.gridy=3; form.add(label("Tên người chơi:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx=1; gbc.gridwidth=3; form.add(txtPlayer, gbc); gbc.gridwidth=1;
        gbc.gridx=1; gbc.gridy=4; gbc.gridwidth=3; form.add(lblPlayerHint, gbc); gbc.gridwidth=1;

        JPanel infoCard = card(new BorderLayout());
        infoCard.add(sectionHeader(rowIndex == null ? "Tạo Giftcode" : "Chỉnh Sửa Giftcode",
                "Thông tin cấu hình", null), BorderLayout.NORTH);
        infoCard.add(form, BorderLayout.CENTER);

        // --- Card Danh sách Item (CÓ ICON) ---
        String[] iCols = {"ID", "Icon", "Tên Item", "Số lượng", "Options (JSON)"};
        DefaultTableModel iModel = new DefaultTableModel(iCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };
        JTable iTable = new JTable(iModel);
        JScrollPane iScroll = styleTable(iTable);
        iTable.setRowHeight(35); // Icon lớn
        iTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
        iTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(300);

        if (rowIndex != null) {
            loadItemsToTable((String) model.getValueAt(rowIndex, 7), iModel);
        }

        JPanel iTool = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        iTool.setOpaque(false);
        JButton btnAddI = button("Thêm Item", ButtonVariant.SECONDARY, "plus");
        JButton btnEditI = button("Sửa", ButtonVariant.SECONDARY, "edit");
        JButton btnDelI = button("Xóa", ButtonVariant.DANGER, "broom");
        iTool.add(btnAddI); iTool.add(btnEditI); iTool.add(btnDelI);

        btnAddI.addActionListener(e -> openItemBuilder(iModel, -1));
        btnEditI.addActionListener(e -> {
            if(iTable.getSelectedRow() != -1) openItemBuilder(iModel, iTable.getSelectedRow());
        });
        iTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount()==2 && iTable.getSelectedRow()!=-1) openItemBuilder(iModel, iTable.getSelectedRow());
            }
        });
        btnDelI.addActionListener(e -> {
            if(iTable.getSelectedRow() != -1) iModel.removeRow(iTable.getSelectedRow());
        });

        JPanel itemCard = card(new BorderLayout());
        itemCard.add(sectionHeader("Danh sách Phần Thưởng",
                "Vật phẩm người chơi nhận được khi nhập code", iTool), BorderLayout.NORTH);
        itemCard.add(iScroll, BorderLayout.CENTER);

        // --- Nút Lưu ---
        JButton btnSave = button("LƯU DỮ LIỆU", ButtonVariant.PRIMARY, "save");
        btnSave.setPreferredSize(new Dimension(220, 44));
        btnSave.addActionListener(e -> {
            String expDate = String.format("%04d-%02d-%02d %02d:%02d:00",
                cbYear.getSelectedItem(), cbMonth.getSelectedItem(), cbDay.getSelectedItem(),
                cbHour.getSelectedItem(), cbMin.getSelectedItem());
            int type = cbType.getSelectedIndex();

            saveGiftcodeToDB(rowIndex, txtCode.getText(), txtCount.getText(), expDate, type, txtPlayer.getText(), iModel, dialog);
        });

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));
        bot.add(btnSave);

        dialog.add(infoCard, BorderLayout.NORTH);
        dialog.add(itemCard, BorderLayout.CENTER);
        dialog.add(bot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ========================================================================
    // 1.5. GIFTCODE SHARE GENERATOR (TẠO 50 MÃ CÙNG LÚC, 1 LƯỢT DÙNG/MÃ)
    // ========================================================================
    private void openGiftcodeShareDialog() {
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tạo Hàng Loạt Giftcode Share", true);
        dialog.setSize(1100, 750);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setLocationRelativeTo(this);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        // --- Card Thông tin chung ---
        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        // Tiền tố Prefix & Số lượng code
        JTextField txtPrefix = new JTextField("SHARE_", 12);
        txtPrefix.setFont(FONT_MONO.deriveFont(Font.BOLD, 14f));
        txtPrefix.setForeground(PRIMARY);

        JSpinner spnTotal = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        spnTotal.setFont(FONT_BODY_B);

        // Số lượt dùng: Cố định 1 lượt/mã
        JTextField txtUsage = new JTextField("1 lượt / mã (Share cộng đồng)", 15);
        txtUsage.setFont(FONT_BODY_B);
        txtUsage.setForeground(SUCCESS);
        txtUsage.setEditable(false);
        txtUsage.setFocusable(false);

        gbc.gridx = 0; gbc.gridy = 0; form.add(label("Tiền tố (Prefix):", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1; form.add(txtPrefix, gbc);
        gbc.gridx = 2; form.add(label("Số lượng code tạo:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 3; form.add(spnTotal, gbc);

        gbc.gridx = 0; gbc.gridy = 1; form.add(label("Lượt dùng mỗi mã:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1; form.add(txtUsage, gbc);

        // Expired Date logic (mặc định +30 ngày)
        long defaultExpMs = System.currentTimeMillis() + 30L * 24 * 3600 * 1000;
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(defaultExpMs);

        JComboBox<Integer> cbYear = new JComboBox<>(); for (int i = 2023; i <= 2035; i++) cbYear.addItem(i); cbYear.setSelectedItem(cal.get(Calendar.YEAR));
        JComboBox<Integer> cbMonth = new JComboBox<>(); for (int i = 1; i <= 12; i++) cbMonth.addItem(i); cbMonth.setSelectedItem(cal.get(Calendar.MONTH) + 1);
        JComboBox<Integer> cbDay = new JComboBox<>(); for (int i = 1; i <= 31; i++) cbDay.addItem(i); cbDay.setSelectedItem(cal.get(Calendar.DAY_OF_MONTH));
        JComboBox<Integer> cbHour = new JComboBox<>(); for (int i = 0; i < 24; i++) cbHour.addItem(i); cbHour.setSelectedItem(23);
        JComboBox<Integer> cbMin = new JComboBox<>(); for (int i = 0; i < 60; i++) cbMin.addItem(i); cbMin.setSelectedItem(59);

        JPanel pDate = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        pDate.setOpaque(false);
        pDate.add(label("Ngày", FONT_SMALL, TEXT_SECONDARY)); pDate.add(cbDay);
        pDate.add(label("/", FONT_SMALL, TEXT_MUTED)); pDate.add(cbMonth);
        pDate.add(label("/", FONT_SMALL, TEXT_MUTED)); pDate.add(cbYear);
        pDate.add(label(" | Giờ:", FONT_SMALL, TEXT_SECONDARY)); pDate.add(cbHour);
        pDate.add(label(":", FONT_SMALL, TEXT_MUTED)); pDate.add(cbMin);

        gbc.gridx = 2; gbc.gridy = 1; form.add(label("Thời hạn:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 3; form.add(pDate, gbc);

        // Type & Note
        String[] typeDescs = {"0 - Tất cả mọi người", "1 - Chỉ thành viên kích hoạt", "2 - Chỉ tài khoản mới tạo (<7 ngày)"};
        JComboBox<String> cbType = new JComboBox<>(typeDescs);

        gbc.gridx = 0; gbc.gridy = 2; form.add(label("Đối tượng:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1; form.add(cbType, gbc);

        // Player whitelist
        JTextField txtPlayer = new JTextField("", 30);
        JLabel lblPlayerHint = label("Để trống = mọi người chơi. Mã sinh ra dạng: PREFIX + 8 ký tự (VD: SHARE_9K2M7P4X).",
                FONT_TINY.deriveFont(Font.ITALIC), TEXT_MUTED);

        gbc.gridx = 2; gbc.gridy = 2; form.add(label("Tên người chơi:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 3; form.add(txtPlayer, gbc);

        gbc.gridx = 1; gbc.gridy = 3; gbc.gridwidth = 3; form.add(lblPlayerHint, gbc); gbc.gridwidth = 1;

        JPanel infoCard = card(new BorderLayout());
        infoCard.add(sectionHeader("Cấu Hình Đợt Giftcode Share",
                "Tạo 50 mã giftcode ngẫu nhiên cùng lúc (mỗi mã 1 lượt nhập duy nhất)", null), BorderLayout.NORTH);
        infoCard.add(form, BorderLayout.CENTER);

        // --- Card Danh sách Item (CÓ ICON) ---
        String[] iCols = {"ID", "Icon", "Tên Item", "Số lượng", "Options (JSON)"};
        DefaultTableModel iModel = new DefaultTableModel(iCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };
        JTable iTable = new JTable(iModel);
        JScrollPane iScroll = styleTable(iTable);
        iTable.setRowHeight(35);
        iTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
        iTable.getColumnModel().getColumn(2).setPreferredWidth(200);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(300);

        // Tự động nạp mẫu item từ dòng đang chọn ngoài bảng nếu có
        int selRow = table.getSelectedRow();
        if (selRow != -1) {
            try {
                Object detailObj = model.getValueAt(selRow, 7);
                if (detailObj != null && !detailObj.toString().trim().isEmpty()) {
                    loadItemsToTable(detailObj.toString(), iModel);
                }
            } catch (Exception ignored) {}
        }

        JPanel iTool = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        iTool.setOpaque(false);
        JButton btnCopyFromMain = button("Sao chép quà từ code đang chọn", ButtonVariant.SECONDARY, "refresh");
        btnCopyFromMain.setToolTipText("Lấy toàn bộ vật phẩm từ code đang chọn ngoài danh sách chính");
        btnCopyFromMain.addActionListener(e -> {
            int sel = table.getSelectedRow();
            if (sel == -1) {
                JOptionPane.showMessageDialog(dialog, "Vui lòng chọn một dòng code trong bảng danh sách bên ngoài trước!");
                return;
            }
            try {
                iModel.setRowCount(0);
                loadItemsToTable(model.getValueAt(sel, 7).toString(), iModel);
                JOptionPane.showMessageDialog(dialog, "Đã nạp thành công quà từ code: " + model.getValueAt(sel, 1));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi nạp quà: " + ex.getMessage());
            }
        });

        JButton btnAddI = button("Thêm Item", ButtonVariant.SECONDARY, "plus");
        JButton btnEditI = button("Sửa", ButtonVariant.SECONDARY, "edit");
        JButton btnDelI = button("Xóa", ButtonVariant.DANGER, "broom");
        iTool.add(btnCopyFromMain);
        iTool.add(btnAddI);
        iTool.add(btnEditI);
        iTool.add(btnDelI);

        btnAddI.addActionListener(e -> openItemBuilder(iModel, -1));
        btnEditI.addActionListener(e -> {
            if (iTable.getSelectedRow() != -1) openItemBuilder(iModel, iTable.getSelectedRow());
        });
        iTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && iTable.getSelectedRow() != -1) openItemBuilder(iModel, iTable.getSelectedRow());
            }
        });
        btnDelI.addActionListener(e -> {
            if (iTable.getSelectedRow() != -1) iModel.removeRow(iTable.getSelectedRow());
        });

        JPanel itemCard = card(new BorderLayout());
        itemCard.add(sectionHeader("Danh sách Phần Thưởng Cho Các Mã Share",
                "Tất cả các mã trong đợt này sẽ có chung danh sách quà tặng này", iTool), BorderLayout.NORTH);
        itemCard.add(iScroll, BorderLayout.CENTER);

        // --- Nút Tạo ---
        JButton btnSave = button("TẠO & LƯU 50 MÃ SHARE", ButtonVariant.SUCCESS, "gift");
        btnSave.setPreferredSize(new Dimension(280, 44));

        spnTotal.addChangeListener(e -> {
            int val = (Integer) spnTotal.getValue();
            btnSave.setText("TẠO & LƯU " + val + " MÃ SHARE");
        });

        btnSave.addActionListener(e -> {
            int total = (Integer) spnTotal.getValue();
            if (total <= 0 || total > 1000) {
                JOptionPane.showMessageDialog(dialog, "Số lượng mã cần tạo phải từ 1 đến 1000!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
                return;
            }
            String prefix = txtPrefix.getText().trim();
            if (prefix.isEmpty()) {
                prefix = "SHARE_";
            }
            if (!prefix.matches("^[a-zA-Z0-9_-]+$")) {
                JOptionPane.showMessageDialog(dialog, "Tiền tố (Prefix) chỉ được chứa chữ cái, số, gạch dưới (_) hoặc gạch ngang (-).", "Lỗi nhập liệu", JOptionPane.ERROR_MESSAGE);
                return;
            }
            if (iModel.getRowCount() == 0) {
                int cf = JOptionPane.showConfirmDialog(dialog,
                        "Danh sách quà tặng đang trống (người chơi sẽ không nhận được vật phẩm nào).\nBạn có chắc muốn tiếp tục tạo mã không?",
                        "Xác nhận phần thưởng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (cf != JOptionPane.YES_OPTION) return;
            }

            String expDate = String.format("%04d-%02d-%02d %02d:%02d:00",
                cbYear.getSelectedItem(), cbMonth.getSelectedItem(), cbDay.getSelectedItem(),
                cbHour.getSelectedItem(), cbMin.getSelectedItem());
            int type = cbType.getSelectedIndex();

            btnSave.setEnabled(false);
            btnSave.setText("Đang tạo " + total + " mã...");

            saveGiftcodeShareToDB(total, prefix, expDate, type, txtPlayer.getText(), iModel, dialog, btnSave);
        });

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));
        bot.add(btnSave);

        dialog.add(infoCard, BorderLayout.NORTH);
        dialog.add(itemCard, BorderLayout.CENTER);
        dialog.add(bot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void saveGiftcodeShareToDB(int totalCodes, String prefix, String expired, int type,
                                       String player, DefaultTableModel itemModel, JDialog dialog, JButton btnSave) {
        // Build JSON Detail
        JsonArray detailArr = new JsonArray();
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            try {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("id", Integer.parseInt(itemModel.getValueAt(i, 0).toString()));
                itemObj.addProperty("quantity", Integer.parseInt(itemModel.getValueAt(i, 3).toString()));
                String optStr = itemModel.getValueAt(i, 4).toString();
                itemObj.add("options", new JsonParser().parse(optStr));
                detailArr.add(itemObj);
            } catch (Exception ignored) {}
        }

        final String detailJson = detailArr.toString();
        final String playerVal = (player == null) ? "" : player.trim();
        final String cleanPrefix = prefix.toUpperCase();

        new Thread(() -> {
            try (Connection conn = getConnection()) {
                // 1. Sinh danh sách các mã duy nhất
                final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
                SecureRandom rng = new SecureRandom();
                Set<String> generatedCodes = new LinkedHashSet<>();

                // Tải trước một tập code hiện có từ DB để chống trùng
                Set<String> existingCodes = new HashSet<>();
                try (Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT code FROM giftcode WHERE code LIKE '" + cleanPrefix + "%'")) {
                    while (rs.next()) {
                        existingCodes.add(rs.getString("code").toUpperCase());
                    }
                } catch (Exception ignored) {}

                while (generatedCodes.size() < totalCodes) {
                    StringBuilder sb = new StringBuilder(cleanPrefix);
                    for (int i = 0; i < 8; i++) {
                        sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
                    }
                    String candidate = sb.toString();
                    if (!existingCodes.contains(candidate)) {
                        generatedCodes.add(candidate);
                        existingCodes.add(candidate);
                    }
                }

                // 2. Batch insert vào Database trong Transaction
                conn.setAutoCommit(false);
                String query = "INSERT INTO giftcode (code, count_left, detail, datecreate, expired, allGender, player_name) VALUES (?, 1, ?, NOW(), ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(query)) {
                    for (String code : generatedCodes) {
                        ps.setString(1, code);
                        ps.setString(2, detailJson);
                        ps.setString(3, expired);
                        ps.setString(4, type == 0 ? "all" : String.valueOf(type));
                        ps.setString(5, playerVal.isEmpty() ? null : playerVal);
                        ps.addBatch();
                    }
                    ps.executeBatch();
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                } finally {
                    conn.setAutoCommit(true);
                }

                // 3. Nạp lại RAM server để người chơi có thể nhập ngay
                try { server.GiftCodeAdmin.reload(); } catch (Exception ignoredReload) {}
                try { managers.GiftCodeManager.gI().init(); } catch (Exception ignoredReload) {}

                // 4. Cập nhật giao diện & hiển thị bảng kết quả
                List<String> codeList = new ArrayList<>(generatedCodes);
                SwingUtilities.invokeLater(() -> {
                    dialog.dispose();
                    loadDataFromDB();
                    showGeneratedShareCodesDialog(codeList);
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    btnSave.setEnabled(true);
                    btnSave.setText("TẠO & LƯU " + totalCodes + " MÃ SHARE");
                    JOptionPane.showMessageDialog(dialog, "Lỗi SQL khi lưu giftcode share: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }

    private void showGeneratedShareCodesDialog(List<String> codes) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Danh Sách " + codes.size() + " Giftcode Share", true);
        d.setSize(620, 560);
        d.setLayout(new BorderLayout(0, 0));
        d.setLocationRelativeTo(this);

        JPanel content = (JPanel) d.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        // Header
        d.add(sectionHeader("Đã Tạo Thành Công " + codes.size() + " Giftcode Share!",
                "Mỗi mã chỉ có 1 lượt nhập (count = 1). Hãy sao chép để chia sẻ cho cộng đồng.", null), BorderLayout.NORTH);

        // Center: JTextArea with codes
        String fullText = String.join("\n", codes);
        JTextArea txt = new JTextArea(fullText);
        txt.setFont(FONT_CODE);
        txt.setEditable(false);
        txt.setBackground(SURFACE_ALT);
        txt.setForeground(PRIMARY);
        txt.setCaretPosition(0);
        txt.setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));

        JScrollPane scroll = new JScrollPane(txt);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER));

        JPanel cardMiddle = card(new BorderLayout());
        cardMiddle.add(scroll, BorderLayout.CENTER);
        d.add(cardMiddle, BorderLayout.CENTER);

        // Bottom buttons
        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pBtns.setOpaque(false);
        pBtns.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));

        JButton btnCopy = button("📋 Sao chép tất cả (" + codes.size() + " mã)", ButtonVariant.PRIMARY, "check");
        btnCopy.addActionListener(e -> {
            try {
                StringSelection sel = new StringSelection(fullText);
                Clipboard clp = Toolkit.getDefaultToolkit().getSystemClipboard();
                clp.setContents(sel, null);
                JOptionPane.showMessageDialog(d, "Đã sao chép thành công " + codes.size() + " mã vào Clipboard!\nBạn có thể dán (Ctrl+V) vào Zalo / Discord / Facebook ngay bây giờ.", "Đã sao chép", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Lỗi sao chép: " + ex.getMessage());
            }
        });

        JButton btnExport = button("💾 Xuất file .txt", ButtonVariant.SECONDARY, "download");
        btnExport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("giftcode_share_" + codes.size() + ".txt"));
            if (fc.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    for (String c : codes) {
                        pw.println(c);
                    }
                    JOptionPane.showMessageDialog(d, "Đã xuất " + codes.size() + " mã ra file:\n" + f.getAbsolutePath(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(d, "Lỗi ghi file: " + ex.getMessage());
                }
            }
        });

        JButton btnClose = button("Đóng", ButtonVariant.GHOST, "x");
        btnClose.addActionListener(e -> d.dispose());

        pBtns.add(btnExport);
        pBtns.add(btnCopy);
        pBtns.add(btnClose);

        d.add(pBtns, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ========================================================================
    // 2. ITEM BUILDER (DIALOG CON)
    // ========================================================================
    private void openItemBuilder(DefaultTableModel parentModel, int editRow) {
        JDialog dialog = new JDialog((Frame)null, "Cấu hình Vật Phẩm", true);
        dialog.setSize(750, 650);
        dialog.setLayout(new BorderLayout(0, 0));
        dialog.setLocationRelativeTo(null);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        // --- Input ID/Qty ---
        JPanel formTop = new JPanel(new GridBagLayout());
        formTop.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(SP_XS, SP_XS, SP_XS, SP_XS); g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtId = new JTextField();
        txtId.setFont(FONT_MONO);
        JTextField txtQty = new JTextField("1");
        JLabel lblName = label("Chưa chọn item", FONT_BODY_B, PRIMARY);
        JLabel lblIcon = new JLabel(); lblIcon.setPreferredSize(new Dimension(30, 30));
        JButton btnSearchItem = button("Tìm nhanh", ButtonVariant.SECONDARY, "search");

        // Load data cũ
        String oldJsonOpt = "[]";
        if (editRow != -1) {
            txtId.setText(parentModel.getValueAt(editRow, 0).toString());
            lblName.setText(parentModel.getValueAt(editRow, 2).toString());
            txtQty.setText(parentModel.getValueAt(editRow, 3).toString());
            oldJsonOpt = parentModel.getValueAt(editRow, 4).toString();
        }

        // Auto update name & icon when typing ID
        txtId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { u(); }
            public void removeUpdate(DocumentEvent e) { u(); }
            public void changedUpdate(DocumentEvent e) { u(); }
            void u() {
                try {
                    int id = Integer.parseInt(txtId.getText());
                    lblName.setText(getItemName(id));
                    lblIcon.setIcon(getItemIcon(id));
                } catch(Exception ex){
                    lblName.setText("...");
                    lblIcon.setIcon(null);
                }
            }
        });

        // Trigger init icon
        if(!txtId.getText().isEmpty()) {
            try {
                int id = Integer.parseInt(txtId.getText());
                lblIcon.setIcon(getItemIcon(id));
            } catch(Exception ignored){}
        }

        btnSearchItem.addActionListener(e -> showItemSearchDialog(dialog, (id, name) -> {
            txtId.setText(String.valueOf(id));
            lblName.setText(name);
        }));

        g.gridx=0; g.gridy=0; formTop.add(label("ID Item:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx=1; formTop.add(txtId, g);
        g.gridx=2; formTop.add(btnSearchItem, g);

        g.gridx=0; g.gridy=1; formTop.add(label("Thông tin:", FONT_SMALL_B, TEXT_SECONDARY), g);
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        pInfo.setOpaque(false);
        pInfo.add(lblIcon); pInfo.add(lblName);
        g.gridx=1; g.gridwidth=2; formTop.add(pInfo, g); g.gridwidth=1;

        g.gridx=0; g.gridy=2; formTop.add(label("Số lượng:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx=1; formTop.add(txtQty, g);

        JPanel pTop = card(new BorderLayout());
        pTop.add(sectionHeader("Chọn Item", "Nhập ID hoặc tìm nhanh vật phẩm", null), BorderLayout.NORTH);
        pTop.add(formTop, BorderLayout.CENTER);

        // --- Option Table ---
        String[] oCols = {"Option ID", "Chỉ số (Param)", "Mô tả tự động"};
        DefaultTableModel oModel = new DefaultTableModel(oCols, 0) {
            @Override public boolean isCellEditable(int row, int column) { return column != 2; }
        };
        JTable oTable = new JTable(oModel);
        JScrollPane oScroll = styleTable(oTable);
        oTable.getColumnModel().getColumn(0).setPreferredWidth(80);
        oTable.getColumnModel().getColumn(1).setPreferredWidth(100);
        oTable.getColumnModel().getColumn(2).setPreferredWidth(300);

        oModel.addTableModelListener(e -> {
            int row = e.getFirstRow();
            int col = e.getColumn();
            if (row >= 0 && row < oModel.getRowCount() && (col == 0 || col == 1)) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        int id = Integer.parseInt(oModel.getValueAt(row, 0).toString());
                        int param = Integer.parseInt(oModel.getValueAt(row, 1).toString());
                        String template = getOptionName(id);
                        oModel.setValueAt(template.replace("#", String.valueOf(param)), row, 2);
                    } catch (Exception ex) {}
                });
            }
        });

        loadOptionsToTable(oldJsonOpt, oModel);

        JPanel pOptTool = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pOptTool.setOpaque(false);
        JButton btnAddO = button("Thêm dòng", ButtonVariant.SECONDARY, "plus");
        JButton btnFindO = button("Tra cứu Option", ButtonVariant.SECONDARY, "search");
        JButton btnDelO = button("Xóa dòng", ButtonVariant.DANGER, "broom");

        btnAddO.addActionListener(e -> oModel.addRow(new Object[]{"0", "0", getOptionName(0).replace("#", "0")}));

        btnFindO.addActionListener(e -> showOptionSearchDialog(dialog, (id, name) -> {
            oModel.addRow(new Object[]{id, "0", name.replace("#", "0")});
        }));

        btnDelO.addActionListener(e -> { if(oTable.getSelectedRow()!=-1) oModel.removeRow(oTable.getSelectedRow()); });

        pOptTool.add(btnAddO); pOptTool.add(btnFindO); pOptTool.add(btnDelO);

        JPanel pOpt = card(new BorderLayout());
        pOpt.add(sectionHeader("Danh sách Chỉ Số (Option)", null, pOptTool), BorderLayout.NORTH);
        pOpt.add(oScroll, BorderLayout.CENTER);

        // --- Finish ---
        JButton btnOk = button("XÁC NHẬN & LƯU ITEM", ButtonVariant.PRIMARY, "check");
        btnOk.setPreferredSize(new Dimension(240, 40));
        btnOk.addActionListener(e -> {
            JsonArray arr = new JsonArray();
            for(int i=0; i<oModel.getRowCount(); i++) {
                try {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", Integer.parseInt(oModel.getValueAt(i, 0).toString()));
                    o.addProperty("param", Integer.parseInt(oModel.getValueAt(i, 1).toString()));
                    arr.add(o);
                } catch(Exception ex){}
            }

            Object[] row = {
                txtId.getText(),
                getItemIcon(Integer.parseInt(txtId.getText())),
                lblName.getText(),
                txtQty.getText(),
                arr.toString()
            };
            if(editRow == -1) parentModel.addRow(row);
            else for(int i=0; i<5; i++) parentModel.setValueAt(row[i], editRow, i);

            dialog.dispose();
        });

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        bot.setOpaque(false);
        bot.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));
        bot.add(btnOk);

        dialog.add(pTop, BorderLayout.NORTH);
        dialog.add(pOpt, BorderLayout.CENTER);
        dialog.add(bot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ========================================================================
    // 3. DIALOG TÌM KIẾM CHUNG (ĐÃ SỬA: TÌM BẰNG ID HOẶC TÊN)
    // ========================================================================
    interface ItemCallback { void onSelect(int id, String name); }

    private void showItemSearchDialog(JDialog parent, ItemCallback callback) {
        JDialog d = new JDialog(parent, "Tìm kiếm Vật Phẩm", true);
        d.setSize(950, 600);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);
        JPanel content = (JPanel) d.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        // --- PANEL BỘ LỌC ---
        JPanel filterRow = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_XS));
        filterRow.setOpaque(false);

        JTextField txtSearch = searchField("Nhập Tên hoặc ID item...");
        txtSearch.setColumns(22);

        // ComboBox Type
        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
                          "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);

        // ComboBox Gender
        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả"};
        JComboBox<String> cbGender = new JComboBox<>(genders);

        filterRow.add(label("Tìm kiếm (Tên/ID):", FONT_SMALL_B, TEXT_SECONDARY)); filterRow.add(txtSearch);
        filterRow.add(label(" | Loại:", FONT_SMALL_B, TEXT_SECONDARY)); filterRow.add(cbType);
        filterRow.add(label(" | Hệ:", FONT_SMALL_B, TEXT_SECONDARY)); filterRow.add(cbGender);

        JPanel pFilter = card(new BorderLayout());
        ((ShadowCard) pFilter).setContentPadding(SP_MD, SP_MD, SP_MD, SP_MD);
        pFilter.add(filterRow, BorderLayout.CENTER);

        // --- BẢNG DỮ LIỆU ---
        // Cột 0: ID, 1: Icon, 2: Name, 3: Type, 4: Gender
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };

        for (ItemData item : listAllItems) {
            m.addRow(new Object[]{item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        }

        JTable t = new JTable(m);
        JScrollPane tScroll = styleTable(t);
        t.setRowHeight(35);
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setCellRenderer(new IconCellRenderer());
        t.getColumnModel().getColumn(2).setPreferredWidth(350);

        // Ẩn cột Type và Gender
        t.getColumnModel().getColumn(3).setMinWidth(0); t.getColumnModel().getColumn(3).setMaxWidth(0);
        t.getColumnModel().getColumn(4).setMinWidth(0); t.getColumnModel().getColumn(4).setMaxWidth(0);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        // --- LOGIC LỌC (QUAN TRỌNG) ---
        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();

            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            // Logic tìm Tên hoặc ID
            if (!text.isEmpty()) {
                try {
                    // Nếu là số: Tìm ID chính xác HOẶC Tên chứa số đó
                    int idVal = Integer.parseInt(text);
                    List<RowFilter<Object,Object>> orFilters = new ArrayList<>();
                    orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, idVal, 0)); // ID column
                    orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2)); // Name column
                    filters.add(RowFilter.orFilter(orFilters));
                } catch (NumberFormatException e) {
                    // Nếu là chữ: Chỉ tìm theo tên
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                }
            }

            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception e) {}
            }

            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) {}
            }

            if (filters.isEmpty()) sorter.setRowFilter(null);
            else sorter.setRowFilter(RowFilter.andFilter(filters));
        };

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doFilter.run(); }
            public void removeUpdate(DocumentEvent e) { doFilter.run(); }
            public void changedUpdate(DocumentEvent e) { doFilter.run(); }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        // Select logic
        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    int r = t.getSelectedRow();
                    if(r!=-1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        callback.onSelect((int) m.getValueAt(modelRow, 0), (String) m.getValueAt(modelRow, 2));
                        d.dispose();
                    }
                }
            }
        });

        d.add(pFilter, BorderLayout.NORTH);
        d.add(tScroll, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void showOptionSearchDialog(JDialog parent, ItemCallback callback) {
        JDialog d = new JDialog(parent, "Tìm kiếm Option", true);
        d.setSize(600, 500);
        d.setLayout(new BorderLayout(0, SP_SM));
        d.setLocationRelativeTo(parent);
        JPanel content = (JPanel) d.getContentPane();
        content.setBackground(BG);
        content.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        JTextField txtSearch = searchField("Nhập tên option...");

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Tên Option"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        for (Map.Entry<Integer, String> entry : optionTemplateMap.entrySet()) {
            m.addRow(new Object[]{entry.getKey(), entry.getValue()});
        }

        JTable t = new JTable(m);
        JScrollPane tScroll = styleTable(t);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }
            void f() {
                String text = txtSearch.getText();
                if(text.trim().length()==0) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount()==2) {
                    int r = t.getSelectedRow();
                    if(r!=-1) {
                        callback.onSelect((int)t.getValueAt(r, 0), (String)t.getValueAt(r, 1));
                        d.dispose();
                    }
                }
            }
        });

        d.add(txtSearch, BorderLayout.NORTH);
        d.add(tScroll, BorderLayout.CENTER);
        d.setVisible(true);
    }

    // ========================================================================
    // HELPERS & LOGIC LƯU
    // ========================================================================

    private void saveGiftcodeToDB(Integer id, String code, String count, String expired, int type, String player, DefaultTableModel itemModel, JDialog dialog) {
        // Build JSON Detail
        JsonArray detailArr = new JsonArray();
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            try {
                JsonObject itemObj = new JsonObject();
                itemObj.addProperty("id", Integer.parseInt(itemModel.getValueAt(i, 0).toString()));
                itemObj.addProperty("quantity", Integer.parseInt(itemModel.getValueAt(i, 3).toString()));
                String optStr = itemModel.getValueAt(i, 4).toString();
                itemObj.add("options", new JsonParser().parse(optStr));
                detailArr.add(itemObj);
            } catch (Exception e) {}
        }

        String playerVal = (player == null) ? "" : player.trim();
        new Thread(() -> {
            try (Connection conn = getConnection()) {
                String query;
                if (id == null) {
                    query = "INSERT INTO giftcode (code, count_left, detail, datecreate, expired, allGender, player_name) VALUES (?, ?, ?, NOW(), ?, ?, ?)";
                    try (PreparedStatement ps = conn.prepareStatement(query)) {
                        ps.setString(1, code);
                        ps.setInt(2, Integer.parseInt(count));
                        ps.setString(3, detailArr.toString());
                        ps.setString(4, expired);
                        ps.setString(5, type == 0 ? "all" : String.valueOf(type));
                        ps.setString(6, playerVal.isEmpty() ? null : playerVal);
                        ps.executeUpdate();
                    }
                } else {
                    int dbId = (int) model.getValueAt(id, 0);
                    query = "UPDATE giftcode SET code=?, count_left=?, detail=?, expired=?, allGender=?, player_name=? WHERE id=?";
                    try (PreparedStatement ps = conn.prepareStatement(query)) {
                        ps.setString(1, code);
                        ps.setInt(2, Integer.parseInt(count));
                        ps.setString(3, detailArr.toString());
                        ps.setString(4, expired);
                        ps.setString(5, type == 0 ? "all" : String.valueOf(type));
                        ps.setString(6, playerVal.isEmpty() ? null : playerVal);
                        ps.setInt(7, dbId);
                        ps.executeUpdate();
                    }
                }
                // Nạp lại giftcode vào RAM để redeem được ngay (không cần restart)
                try { server.GiftCodeAdmin.reload(); } catch (Exception ignoredReload) {}
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(dialog, "Lưu thành công!");
                    dialog.dispose();
                    loadDataFromDB();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialog, "Lỗi SQL: " + e.getMessage()));
            }
        }).start();
    }

    private void loadItemsToTable(String json, DefaultTableModel model) {
        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                // Định dạng chuẩn dùng key 'id'; fallback 'temp_id' cho code cũ
                int tempId = obj.has("id") ? obj.get("id").getAsInt()
                        : (obj.has("temp_id") ? obj.get("temp_id").getAsInt() : -1);
                int qty = obj.has("quantity") ? obj.get("quantity").getAsInt() : 1;
                String opts = obj.has("options") ? obj.get("options").toString() : "[]";
                // Load item kèm Icon
                model.addRow(new Object[]{tempId, getItemIcon(tempId), getItemName(tempId), qty, opts});
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi loadItemsToTable: " + e.getMessage() + ", JSON: " + json);
        }
    }

    private void loadOptionsToTable(String json, DefaultTableModel model) {
        try {
            JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                int param = obj.get("param").getAsInt();

                String template = getOptionName(id);
                String desc = template.replace("#", String.valueOf(param));

                model.addRow(new Object[]{id, param, desc});
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Lỗi loadOptionsToTable: " + e.getMessage() + ", JSON: " + json);
        }
    }
}
