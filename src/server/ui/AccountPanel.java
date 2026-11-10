package server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import models.player.Player;
import server.Client;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import javax.imageio.ImageIO;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

public class AccountPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";

    // Font số (mono, đậm) dẫn xuất từ token FONT_MONO
    private static final Font FONT_NUM = FONT_MONO.deriveFont(Font.BOLD, 14f);

    // Format Date
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy");

    // Data Cache
    private final Map<Integer, Integer> partHeadIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> headCache = new HashMap<>();

    // Components
    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;

    public AccountPanel() {
        setLayout(new BorderLayout(0, SP_SM));
        setBackground(BG);
        setBorder(pad(SP_LG, SP_XL, SP_XL, SP_XL));

        initUI();
        loadHeadPartCache(); // Cache icons first
    }

    private void initUI() {
        // --- Header card ---
        txtSearch = searchField("Tìm ID, Username, Tên nhân vật...");
        txtSearch.setPreferredSize(new Dimension(300, 38));

        JButton btnSearch = button("Tìm kiếm", ButtonVariant.PRIMARY, "search");
        JButton btnReload = button("Làm mới", ButtonVariant.SECONDARY, "refresh");
        JButton btnDelete = button("Xóa tài khoản", ButtonVariant.DANGER, "trash");

        btnSearch.addActionListener(e -> searchData(txtSearch.getText()));
        btnReload.addActionListener(e -> { txtSearch.setText(""); loadData(); });
        btnDelete.addActionListener(e -> deleteSelectedAccount());

        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        searchPanel.setOpaque(false);
        searchPanel.add(txtSearch);
        searchPanel.add(btnSearch);
        searchPanel.add(btnReload);
        searchPanel.add(btnDelete);

        JPanel topCard = card(new BorderLayout());
        ((ShadowCard) topCard).setContentPadding(SP_LG, SP_LG, SP_MD, SP_LG);
        topCard.add(sectionHeader("QUẢN LÝ TÀI KHOẢN",
                "Nhấn đúp vào một dòng để chỉnh sửa tài khoản", null), BorderLayout.NORTH);
        topCard.add(searchPanel, BorderLayout.CENTER);
        add(topCard, BorderLayout.NORTH);

        // --- Table ---
        String[] columns = {"Head", "ID", "Tài khoản", "Tên NV", "Mật khẩu", "Trạng thái", "VIP", "sotien", "Đã Nạp", "Ngày tạo"};

        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int col) { return false; }
            @Override public Class<?> getColumnClass(int col) { return col == 0 ? ImageIcon.class : Object.class; }
        };

        table = new JTable(model);
        JScrollPane spTable = styleTable(table);
        spTable.setBorder(BorderFactory.createEmptyBorder());
        setupTableStyle();

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && table.getSelectedRow() != -1) {
                    int id = Integer.parseInt(table.getValueAt(table.getSelectedRow(), 1).toString());
                    openEditDialog(id);
                }
            }
            // Chuột phải: chọn dòng + hiện menu Xóa
            @Override public void mousePressed(MouseEvent e)  { showPopup(e); }
            @Override public void mouseReleased(MouseEvent e) { showPopup(e); }
            private void showPopup(MouseEvent e) {
                if (!e.isPopupTrigger()) return;
                int row = table.rowAtPoint(e.getPoint());
                if (row < 0) return;
                // Giữ nguyên vùng đang chọn nếu click vào dòng đã chọn (để xóa nhiều)
                if (!table.isRowSelected(row)) table.setRowSelectionInterval(row, row);
                buildRowPopup().show(table, e.getX(), e.getY());
            }
        });

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(spTable, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    private void setupTableStyle() {
        table.setRowHeight(48);
        table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                lbl.setIcon(null);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);

                // Zebra + hover + selection (đồng bộ với styleTable)
                Object hoverObj = t.getClientProperty("ui.hoverRow");
                int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
                if (isSelected)          lbl.setBackground(PRIMARY_SOFT);
                else if (row == hover)   lbl.setBackground(HOVER_ROW);
                else                     lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                lbl.setForeground(TEXT);
                lbl.setFont(FONT_BODY);

                if (column == 0 && value instanceof Icon) {
                    lbl.setText("");
                    lbl.setIcon((Icon) value);
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (column == 3) {
                    lbl.setFont(FONT_BODY_B);
                    lbl.setForeground(PRIMARY);
                } else if (column == 5) {
                    lbl.setFont(FONT_SMALL_B);
                    String s = String.valueOf(value);
                    if (s.contains("BAN"))          lbl.setForeground(DANGER);
                    else if (s.contains("Active"))  lbl.setForeground(SUCCESS);
                    else                            lbl.setForeground(TEXT_MUTED);
                } else if (column == 7 || column == 8) {
                    lbl.setFont(FONT_NUM);
                    lbl.setForeground(VIOLET);
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                }
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        // Widths
        TableColumnModel cm = table.getColumnModel();
        cm.getColumn(0).setPreferredWidth(60);
        cm.getColumn(1).setPreferredWidth(50);
        cm.getColumn(2).setPreferredWidth(120);
        cm.getColumn(3).setPreferredWidth(120);
        cm.getColumn(4).setPreferredWidth(80);
    }

    private void openEditDialog(int accountId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh sửa ID: " + accountId, true);
        d.setSize(980, 700);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout());
        d.getContentPane().setBackground(BG);

        // --- PREPARE FIELDS ---
        JTextField txtUser = createField(false);
        JTextField txtPass = createField(true);
        JTextField txtEmail = createField(true);

        JCheckBox chkActive = new JCheckBox("Kích hoạt");
        JCheckBox chkBan = new JCheckBox("Khóa (Ban)"); chkBan.setForeground(DANGER);
        JCheckBox chkAdmin = new JCheckBox("Admin");

        JTextField txtRole = createField(true);
        JTextField txtVip = createField(true);
        JTextField txtServer = createField(true);

        JTextField txtVnd = createField(true); txtVnd.setFont(FONT_NUM); txtVnd.setForeground(DANGER);
        JTextField txtDanap = createField(true); txtDanap.setFont(FONT_NUM); txtDanap.setForeground(INFO);
        JTextField txtGold = createField(true); txtGold.setFont(FONT_NUM);
        JTextField txtPoint = createField(true); txtPoint.setFont(FONT_NUM);
        JTextField txtCoupon = createField(true); txtCoupon.setFont(FONT_NUM); txtCoupon.setForeground(SUCCESS);

        Map<String, JTextField> eventMap = new HashMap<>();
        // Chỉ bao gồm các cột thực sự tồn tại trong bảng account (theo file SQL)
        String[] eventCols = {
            "DiemDanh", "bong_master", "hopquathang9", "hopquathang9vip",
            "luotquay", "event_point", "hopqua"
        };
        for (String col : eventCols) eventMap.put(col, createField(true));

        final int[] headInfo = {-1}; // wrapper for head id

        // --- LOAD DATA ---
        loadAccountData(accountId, txtUser, txtPass, txtEmail, chkActive, chkBan, chkAdmin,
                        txtRole, txtVip, txtServer, txtVnd, txtDanap, txtGold, txtPoint, txtCoupon, eventMap, headInfo);

        // --- BUILD UI ---
        JPanel pMain = new JPanel(new GridBagLayout());
        pMain.setBackground(BG);
        pMain.setBorder(pad(SP_MD, SP_LG, SP_MD, SP_LG));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, SP_SM, 0); // Spacing bottom
        gbc.gridx = 0; gbc.weightx = 1.0;

        // 1. PROFILE SECTION
        JPanel pProfile = createSectionPanel("THÔNG TIN TÀI KHOẢN");
        JPanel pProfileContent = new JPanel(new GridBagLayout());
        pProfileContent.setOpaque(false);

        GridBagConstraints gp = new GridBagConstraints();
        gp.insets = new Insets(SP_XS, SP_XS, SP_XS, SP_SM);
        gp.fill = GridBagConstraints.HORIZONTAL;

        // Avatar (Left)
        JLabel lblAvt = new JLabel(getAvatar(headInfo[0], txtUser.getText(), 80));
        gp.gridx = 0; gp.gridy = 0; gp.gridheight = 3; gp.weightx = 0;
        pProfileContent.add(lblAvt, gp);

        // Reset gridheight
        gp.gridheight = 1; gp.weightx = 1.0;

        // Row 1: User, Pass, Email
        JPanel pRow1 = new JPanel(new GridLayout(1, 3, SP_LG, 0));
        pRow1.setOpaque(false);
        pRow1.add(createInputGroup("Tài khoản:", txtUser));
        pRow1.add(createInputGroup("Mật khẩu:", txtPass));
        pRow1.add(createInputGroup("Email:", txtEmail));

        gp.gridx = 1; gp.gridy = 0;
        pProfileContent.add(pRow1, gp);

        // Row 2: Role, VIP, Server (Fixed width for these small fields)
        JPanel pRow2 = new JPanel(new GridLayout(1, 3, SP_LG, 0));
        pRow2.setOpaque(false);
        pRow2.add(createInputGroup("Role (-1):", txtRole));
        pRow2.add(createInputGroup("VIP:", txtVip));
        pRow2.add(createInputGroup("Server Login:", txtServer));

        gp.gridx = 1; gp.gridy = 1;
        pProfileContent.add(pRow2, gp);

        // Row 3: Checkboxes
        JPanel pRow3 = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pRow3.setOpaque(false);
        styleCheck(chkActive); styleCheck(chkBan); styleCheck(chkAdmin);
        pRow3.add(chkActive); pRow3.add(chkBan); pRow3.add(chkAdmin);

        gp.gridx = 1; gp.gridy = 2;
        pProfileContent.add(pRow3, gp);

        pProfile.add(pProfileContent, BorderLayout.CENTER);

        gbc.gridy = 0;
        pMain.add(pProfile, gbc);

        // 2. ASSETS SECTION
        JPanel pAsset = new JPanel(new GridLayout(1, 5, SP_MD, 0)); // 5 columns equal width
        pAsset.setOpaque(false);
        pAsset.add(createBorderedGroup("sotien", txtVnd));
        pAsset.add(createBorderedGroup("Đã Nạp", txtDanap));
        pAsset.add(createBorderedGroup("Vàng (Gold)", txtGold));
        pAsset.add(createBorderedGroup("Đã Nạp Gốc", txtPoint));
        pAsset.add(createBorderedGroup("Điểm Sự Kiện", txtCoupon));

        gbc.gridy = 1;
        pMain.add(pAsset, gbc);

        // 3. EVENTS SECTION
        JPanel pEventContainer = createSectionPanel("VẬT PHẨM & SỰ KIỆN");

        JPanel pEventGrid = new JPanel(new GridLayout(0, 4, SP_SM, SP_SM)); // 4 columns
        pEventGrid.setBackground(SURFACE);
        pEventGrid.setBorder(pad(SP_SM));
        for(String key : eventCols) {
            pEventGrid.add(createCompactInput(key, eventMap.get(key)));
        }

        // Wrap events in scroll pane in case of many items
        JScrollPane scrollEvents = new JScrollPane(pEventGrid);
        scrollEvents.setBorder(null);
        scrollEvents.getViewport().setBackground(SURFACE);
        scrollEvents.getVerticalScrollBar().setUnitIncrement(16);
        scrollEvents.setPreferredSize(new Dimension(800, 200)); // Set preferred height

        pEventContainer.add(scrollEvents, BorderLayout.CENTER);

        gbc.gridy = 2; gbc.weighty = 1.0; gbc.fill = GridBagConstraints.BOTH;
        pMain.add(pEventContainer, gbc);

        // --- BUTTONS ---
        JPanel pBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, SP_MD));
        pBtn.setBackground(SURFACE);
        pBtn.setBorder(new CompoundBorder(
                new MatteBorder(1, 0, 0, 0, BORDER), padH(SP_LG)));

        JButton btnSave = button("LƯU THAY ĐỔI", ButtonVariant.PRIMARY, "save");

        JButton btnCancel = button("Đóng", ButtonVariant.GHOST);
        btnCancel.addActionListener(e->d.dispose());

        btnSave.addActionListener(e -> {
            saveAccount(d, accountId, txtUser, txtPass, txtEmail, chkActive, chkBan, chkAdmin,
                        txtRole, txtVip, txtServer, txtVnd, txtDanap, txtGold, txtPoint, txtCoupon, eventMap);
        });

        pBtn.add(btnCancel);
        pBtn.add(btnSave);

        d.add(pMain, BorderLayout.CENTER);
        d.add(pBtn, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // ========================================================================
    // 4. DATA LOGIC (LOAD & SAVE)
    // ========================================================================
    private void loadAccountData(int id, JTextField user, JTextField pass, JTextField email,
                                 JCheckBox act, JCheckBox ban, JCheckBox adm,
                                 JTextField role, JTextField vip, JTextField server,
                                 JTextField vnd, JTextField danap, JTextField gold, JTextField point,
                                 JTextField coupon, Map<String, JTextField> events, int[] headRef) {
        try (Connection conn = DBConnecter.getConnectionServer();
             PreparedStatement ps = conn.prepareStatement(
                 "SELECT a.*, (SELECT head FROM player WHERE account_id = a.id LIMIT 1) as p_head, "
               + "(SELECT data_inventory FROM player WHERE account_id = a.id LIMIT 1) as p_inv FROM account a WHERE a.id = ?")) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                headRef[0] = rs.getInt("p_head");
                user.setText(rs.getString("username"));
                pass.setText(rs.getString("password"));
                email.setText(rs.getString("email"));
                act.setSelected(rs.getInt("active") == 1);
                ban.setSelected(rs.getInt("ban") == 1);
                adm.setSelected(rs.getInt("is_admin") == 1);
                // account trong schema này không có cột 'role' — đọc an toàn, mặc định -1
                try { role.setText(rs.getString("role")); } catch (SQLException exRole) { role.setText("-1"); }
                vip.setText(rs.getString("vip"));
                server.setText(rs.getString("server_login"));

                // Mapping: Cash = cash (cột SQL), Đã Nạp = danap, Đã Nạp Gốc = danapgoc
                vnd.setText(String.valueOf(rs.getInt("sotien")));
                danap.setText(String.valueOf(rs.getInt("danap")));
                gold.setText(String.valueOf(rs.getLong("vang")));
                // Đã Nạp Gốc từ cột danapgoc
                try {
                    point.setText(String.valueOf(rs.getInt("danapgoc")));
                } catch (SQLException e) {
                    point.setText("0");
                }

                // Điểm Sự Kiện (coupon) nằm trong player.data_inventory (JSON, index 3)
                coupon.setText(String.valueOf(getInventoryCoupon(rs.getString("p_inv"))));

                for (String key : events.keySet()) {
                    try { events.get(key).setText(String.valueOf(rs.getInt(key))); } catch (Exception ex) {}
                }
            }
        } catch (Exception e) { e.printStackTrace(); }
    }

    private void saveAccount(JDialog d, int id, JTextField user, JTextField pass, JTextField email,
                             JCheckBox act, JCheckBox ban, JCheckBox adm,
                             JTextField role, JTextField vip, JTextField server,
                             JTextField vnd, JTextField danap, JTextField gold, JTextField point,
                             JTextField coupon, Map<String, JTextField> events) {
        if (JOptionPane.showConfirmDialog(d, "Lưu dữ liệu?", "Xác nhận", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer()) {
                // cash=vnd, danap=tongnap, danapgoc=Đã Nạp Gốc
                StringBuilder sql = new StringBuilder("UPDATE account SET username=?, password=?, email=?, active=?, ban=?, is_admin=?, vip=?, server_login=?, ");
                sql.append("sotien=?, danap=?, vang=?, ");

                // Check if danapgoc column exists
                boolean hasDanapgoc = checkColumnExists(conn, "account", "danapgoc");
                if (hasDanapgoc) {
                    sql.append("danapgoc=?, ");
                }

                // Only include event columns that exist in the database
                java.util.List<String> validEventCols = new java.util.ArrayList<>();
                for (String k : events.keySet()) {
                    if (checkColumnExists(conn, "account", k)) {
                        sql.append(k).append("=?, ");
                        validEventCols.add(k);
                    }
                }

                sql.append("update_time=NOW() WHERE id=?");

                try (PreparedStatement ps = conn.prepareStatement(sql.toString())) {
                    int i = 1;
                    ps.setString(i++, user.getText());
                    ps.setString(i++, pass.getText());
                    ps.setString(i++, email.getText());
                    ps.setInt(i++, act.isSelected() ? 1 : 0);
                    ps.setInt(i++, ban.isSelected() ? 1 : 0);
                    ps.setInt(i++, adm.isSelected() ? 1 : 0);
                    ps.setInt(i++, safeInt(vip));
                    ps.setInt(i++, safeInt(server));

                    ps.setInt(i++, safeInt(vnd)); // cash
                    ps.setInt(i++, safeInt(danap)); // danap
                    ps.setLong(i++, safeLong(gold));

                    if (hasDanapgoc) {
                        ps.setInt(i++, safeInt(point)); // danapgoc (Đã Nạp Gốc)
                    }

                    // Only set parameters for columns that exist
                    for (String k : validEventCols) {
                        ps.setInt(i++, safeInt(events.get(k)));
                    }

                    ps.setInt(i++, id);

                    ps.executeUpdate();
                }

                // Điểm Sự Kiện (coupon) — lưu trong bảng player (cột data_inventory, JSON index 3)
                updatePlayerCoupon(conn, id, safeInt(coupon));

                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(d, "Lưu thành công!");
                    d.dispose();
                    loadData();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi: " + e.getMessage()));
            }
        }).start();
    }

    // ========================================================================
    // 4b. ĐIỂM SỰ KIỆN (COUPON) — lưu trong player.data_inventory (JSON index 3)
    //     Thứ tự mảng: [gold, gem, ruby, coupon, event, diemDT]
    // ========================================================================
    private static final int COUPON_INDEX = 3;

    /** Đọc coupon (điểm sự kiện) từ chuỗi JSON data_inventory. Trả 0 nếu không có. */
    private int getInventoryCoupon(String dataInventory) {
        try {
            JsonElement el = new JsonParser().parse(dataInventory);
            if (el != null && el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                if (arr.size() > COUPON_INDEX) {
                    return arr.get(COUPON_INDEX).getAsInt();
                }
            }
        } catch (Exception ignored) {}
        return 0;
    }

    /**
     * Trả về chuỗi JSON data_inventory mới với coupon (index 3) đã được thay bằng giá trị mới,
     * giữ nguyên gold/gem/ruby/event/diemDT. Hỗ trợ cả dữ liệu cũ dạng object {gold,gem,ruby}.
     */
    private String withCoupon(String dataInventory, int coupon) {
        long gold = 0; int gem = 0, ruby = 0, event = 0, diemDT = 0;
        JsonArray src = null;
        try {
            JsonElement el = new JsonParser().parse(dataInventory);
            if (el != null && el.isJsonArray()) {
                src = el.getAsJsonArray();
            } else if (el != null && el.isJsonObject()) {
                JsonObject ob = el.getAsJsonObject();
                if (ob.has("gold")) gold = ob.get("gold").getAsLong();
                if (ob.has("gem"))  gem  = ob.get("gem").getAsInt();
                if (ob.has("ruby")) ruby = ob.get("ruby").getAsInt();
            }
        } catch (Exception ignored) {}

        JsonArray out = new JsonArray();
        if (src != null) {
            int size = Math.max(src.size(), 6); // đảm bảo đủ 6 phần tử
            for (int idx = 0; idx < size; idx++) {
                if (idx == COUPON_INDEX)      out.add(coupon);
                else if (idx < src.size())    out.add(src.get(idx));
                else                          out.add(0);
            }
        } else {
            out.add(gold); out.add(gem); out.add(ruby);
            out.add(coupon); out.add(event); out.add(diemDT);
        }
        return out.toString();
    }

    private void updatePlayerCoupon(Connection conn, int accountId, int coupon) throws SQLException {
        try {
            Player online = Client.gI().getPlayerByUser(accountId);
            if (online != null && online.inventory != null) {
                online.inventory.coupon = coupon;
                return;
            }
        } catch (Exception ignored) {}

        long playerId = -1;
        String dataInventory = null;
        try (PreparedStatement ps = conn.prepareStatement(
                "SELECT id, data_inventory FROM player WHERE account_id = ? LIMIT 1")) {
            ps.setInt(1, accountId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    playerId = rs.getLong("id");
                    dataInventory = rs.getString("data_inventory");
                }
            }
        }
        if (playerId == -1) return; // account chưa tạo nhân vật

        try (PreparedStatement ps = conn.prepareStatement(
                "UPDATE player SET data_inventory = ? WHERE id = ?")) {
            ps.setString(1, withCoupon(dataInventory, coupon));
            ps.setLong(2, playerId);
            ps.executeUpdate();
        }
    }

    /** Card trắng bo góc + tiêu đề section nhỏ (thay cho TitledBorder cũ). */
    private JPanel createSectionPanel(String title) {
        JPanel p = card(new BorderLayout(0, SP_MD));
        ((ShadowCard) p).setContentPadding(SP_MD, SP_LG, SP_MD, SP_LG);
        JLabel lbl = label(title, FONT_H3, TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH);
        return p;
    }

    private JPanel createInputGroup(String label, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(FONT_SMALL_B);
        lbl.setForeground(TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JPanel createBorderedGroup(String title, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout(0, SP_XS));
        p.setBackground(SURFACE);
        p.setBorder(new CompoundBorder(softBorder(), pad(SP_SM, SP_MD, SP_SM, SP_MD)));

        JLabel lbl = label(title, FONT_TINY_B, TEXT_MUTED);

        txt.setBorder(null);
        txt.setOpaque(false);
        txt.setBackground(SURFACE);

        p.add(lbl, BorderLayout.NORTH);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JPanel createCompactInput(String title, JTextField txt) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(SURFACE_ALT);
        p.setBorder(new CompoundBorder(softBorder(), pad(SP_XS, SP_SM, SP_XS, SP_SM)));

        JLabel lbl = label(title, FONT_TINY, TEXT_MUTED);
        lbl.setPreferredSize(new Dimension(80, 20));

        txt.setBorder(null);
        txt.setOpaque(false);
        txt.setBackground(SURFACE_ALT);
        txt.setFont(FONT_MONO.deriveFont(13f));
        txt.setHorizontalAlignment(JTextField.RIGHT);

        p.add(lbl, BorderLayout.WEST);
        p.add(txt, BorderLayout.CENTER);
        return p;
    }

    private JTextField createField(boolean edit) {
        JTextField t = new JTextField();
        t.setEditable(edit);
        t.setFont(FONT_BODY);
        if(!edit) {
            t.setBorder(null); t.setOpaque(false);
            t.setFont(FONT_H2);
            t.setForeground(PRIMARY);
        }
        return t;
    }

    private void styleCheck(JCheckBox c) {
        c.setFont(FONT_BODY_B);
        c.setOpaque(false);
        c.setFocusPainted(false);
    }

    private int safeInt(JTextField t) { try { return Integer.parseInt(t.getText().trim()); } catch(Exception e) { return 0; } }
    private long safeLong(JTextField t) { try { return Long.parseLong(t.getText().trim()); } catch(Exception e) { return 0; } }

    private String formatNum(long num) { return java.text.NumberFormat.getInstance().format(num); }

    private void loadHeadPartCache() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0) partHeadIconMap.put(rs.getInt("id"), arr.get(0).getAsJsonArray().get(0).getAsInt());
                    } catch (Exception ignored) {}
                }
            } catch (Exception e) {}
            SwingUtilities.invokeLater(this::loadData);
        }).start();
    }

    private ImageIcon getAvatar(int headId, String text, int size) {
        if (headId > 0) {
            if (headCache.containsKey(headId) && size == 28) return headCache.get(headId);
            Integer iconId = partHeadIconMap.get(headId);
            if (iconId != null) {
                try {
                    String[] zooms = {"x4", "x3", "x2", "x1"};
                    for (String z : zooms) {
                        File f = new File(ICON_FOLDER + z + "/" + iconId + ".png");
                        if (f.exists()) {
                            Image img = ImageIO.read(f).getScaledInstance(size, size, Image.SCALE_SMOOTH);
                            ImageIcon icon = new ImageIcon(img);
                            if(size == 28) headCache.put(headId, icon);
                            return icon;
                        }
                    }
                } catch (Exception e) {}
            }
        }
        return AvatarGenerator.generate(text, size);
    }

    private void loadData() {
        // Cập nhật lấy cash, danap (Mapping: cash=vnd, danap=tongnap)
        updateTable("SELECT a.id, a.username, a.password, a.active, a.ban, a.vip, a.sotien, a.danap, a.create_time, " +
                    "(SELECT head FROM player WHERE account_id = a.id LIMIT 1) AS head, " +
                    "(SELECT name FROM player WHERE account_id = a.id LIMIT 1) AS p_name FROM account a ORDER BY a.id ASC");
    }

    private void searchData(String txt) {
        if(txt.isEmpty()) { loadData(); return; }
        updateTable("SELECT a.id, a.username, a.password, a.active, a.ban, a.vip, a.sotien, a.danap, a.create_time, " +
                    "(SELECT head FROM player WHERE account_id = a.id LIMIT 1) AS head, " +
                    "(SELECT name FROM player WHERE account_id = a.id LIMIT 1) AS p_name FROM account a " +
                    "WHERE a.username LIKE '%"+txt+"%' OR a.id='"+txt+"' OR (SELECT name FROM player WHERE account_id=a.id LIMIT 1) LIKE '%"+txt+"%'");
    }

    private void updateTable(String sql) {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionServer(); Statement s = conn.createStatement(); ResultSet rs = s.executeQuery(sql)) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    String u = rs.getString("username");
                    row.add(getAvatar(rs.getInt("head"), u, 28));
                    row.add(rs.getInt("id"));
                    row.add(u);
                    String pn = rs.getString("p_name");
                    row.add(pn == null ? "-(Chưa tạo)-" : pn);
                    row.add("******");

                    int active = rs.getInt("active");
                    int ban = rs.getInt("ban");
                    row.add(ban == 1 ? "ĐÃ BAN" : (active == 1 ? "Active" : "Chưa KH"));

                    row.add(rs.getInt("vip"));
                    row.add(formatNum(rs.getInt("sotien"))); // Cash
                    row.add(formatNum(rs.getInt("danap"))); // Đã nạp
                    Timestamp ts = rs.getTimestamp("create_time");
                    row.add(ts != null ? DATE_FMT.format(ts) : "N/A");

                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ========================================================================
    // 6b. XÓA TÀI KHOẢN
    // ========================================================================
    private JPopupMenu buildRowPopup() {
        int selCount = table.getSelectedRowCount();
        JPopupMenu menu = new JPopupMenu();

        JMenuItem itEdit = new JMenuItem("Chỉnh sửa");
        itEdit.setIcon(vectorIcon("edit", 14, TEXT_SECONDARY));
        itEdit.setEnabled(selCount == 1); // Chỉ sửa được khi chọn 1 dòng
        itEdit.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1) openEditDialog(Integer.parseInt(table.getValueAt(r, 1).toString()));
        });

        JMenuItem itDelete = new JMenuItem(selCount > 1
                ? "Xóa " + selCount + " tài khoản đã chọn" : "Xóa tài khoản");
        itDelete.setIcon(vectorIcon("trash", 14, DANGER));
        itDelete.setForeground(DANGER);
        itDelete.addActionListener(e -> deleteSelectedAccount());

        menu.add(itEdit);
        menu.addSeparator();
        menu.add(itDelete);
        return menu;
    }

    private void deleteSelectedAccount() {
        int[] rows = table.getSelectedRows();
        if (rows.length == 0) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất một tài khoản để xóa.",
                    "Chưa chọn", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Thu thập id + username các dòng đang chọn
        final java.util.List<Integer> ids = new java.util.ArrayList<>();
        final java.util.List<String> names = new java.util.ArrayList<>();
        for (int row : rows) {
            ids.add(Integer.parseInt(table.getValueAt(row, 1).toString()));
            names.add(String.valueOf(table.getValueAt(row, 2)));
        }

        // Nội dung xác nhận
        String detail;
        if (ids.size() == 1) {
            detail = "ID: <b>" + ids.get(0) + "</b><br>Tài khoản: <b>" + names.get(0) + "</b>";
        } else {
            StringBuilder sb = new StringBuilder("Số lượng: <b>" + ids.size() + " tài khoản</b><br>");
            int show = Math.min(ids.size(), 10);
            for (int k = 0; k < show; k++) {
                sb.append("&bull; ").append(names.get(k)).append(" (ID ").append(ids.get(k)).append(")<br>");
            }
            if (ids.size() > show) sb.append("&bull; ... và ").append(ids.size() - show).append(" tài khoản khác<br>");
            detail = sb.toString();
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "<html>Bạn có chắc muốn <b>XÓA VĨNH VIỄN</b> các tài khoản sau?<br><br>"
                + detail + "<br>"
                + "<font color='red'>Toàn bộ nhân vật và dữ liệu của những tài khoản này sẽ bị xóa<br>"
                + "và không thể khôi phục!</font></html>",
                "Xác nhận xóa tài khoản", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            int deleted = 0;
            try (Connection conn = DBConnecter.getConnectionServer()) {
                conn.setAutoCommit(false);
                try (PreparedStatement psPlayer = conn.prepareStatement("DELETE FROM player WHERE account_id = ?");
                     PreparedStatement psAcc = conn.prepareStatement("DELETE FROM account WHERE id = ?")) {
                    for (int id : ids) {
                        // Xóa nhân vật trước (FK player.account_id -> account.id)
                        psPlayer.setInt(1, id);
                        psPlayer.executeUpdate();
                        psAcc.setInt(1, id);
                        deleted += psAcc.executeUpdate();
                    }
                    conn.commit();
                } catch (Exception ex) {
                    conn.rollback();
                    throw ex;
                }
                final int total = deleted;
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(this, "Đã xóa " + total + " tài khoản.");
                    loadData();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Lỗi khi xóa: " + e.getMessage() + "\n(Không có tài khoản nào bị xóa)",
                        "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private boolean checkColumnExists(Connection conn, String tableName, String columnName) {
        try {
            DatabaseMetaData meta = conn.getMetaData();
            ResultSet rs = meta.getColumns(null, null, tableName, columnName);
            boolean exists = rs.next();
            rs.close();
            return exists;
        } catch (Exception e) {
            return false;
        }
    }

    static class AvatarGenerator {
        private static final Color[] COLORS = { SUCCESS, INFO, PRIMARY, VIOLET, WARNING, DANGER };
        public static ImageIcon generate(String text, int size) {
            if (text == null || text.isEmpty()) text = "?";
            String l = text.substring(0, 1).toUpperCase();
            Color bg = COLORS[Math.abs(text.hashCode()) % COLORS.length];
            BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = img.createGraphics();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg); g2.fill(new Ellipse2D.Float(0, 0, size, size));
            g2.setColor(Color.WHITE); g2.setFont(new Font(FONT_FAMILY, Font.BOLD, size / 2));
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(l, (size - fm.stringWidth(l)) / 2, (size - fm.getHeight()) / 2 + fm.getAscent());
            g2.dispose();
            return new ImageIcon(img);
        }
    }
}
