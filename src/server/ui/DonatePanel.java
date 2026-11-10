package server.ui;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.regex.Pattern;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

/**
 * Tab "Gói Donate" — quản lý gói quà bán tại NPC Chú Bé Đần (tempId 103, Đảo Kamê).
 * Dữ liệu: nro_data.donate_package (+ donate_package_buy ghi ai đã mua).
 * Server đọc DB TRỰC TIẾP mỗi lần người chơi mở NPC → thêm/sửa/xoá ở đây là
 * áp dụng NGAY, không cần restart. Mỗi gói mỗi nhân vật chỉ mua được 1 lần.
 *
 * Cột items lưu dạng text, mỗi dòng 1 item: "tempId sốLượng optId:param optId:param..."
 */
public class DonatePanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/";

    private JTable table;
    private DefaultTableModel model;

    // Cache template item/option để hiện tên + icon
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, String> optionTemplateMap = new HashMap<>();
    private final List<ItemData> listAllItems = new ArrayList<>();
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();

    private static class ItemData {
        int id;
        String name;
        ItemData(int id, String name) { this.id = id; this.name = name; }
    }

    public DonatePanel() {
        setLayout(new BorderLayout(0, SP_XS));
        setBackground(BG);
        setBorder(pad(SP_MD, SP_LG, SP_LG, SP_LG));

        try { database.daos.DonatePackageDAO.ensureTables(); } catch (Throwable ignored) {}

        initUI();
        // nạp cache tên item XONG rồi mới đổ bảng — đổ song song thì lần đầu
        // cột "Vật phẩm" hiện "Item #457" thay vì tên thật
        new Thread(() -> {
            loadCacheData();
            loadDataFromDB();
        }).start();
    }

    // ------------------------------------------------------------------ utils
    /** Nền zebra / hover / chọn giống styleTable — dùng cho renderer tùy biến. */
    private static void paintRowBackground(JLabel lbl, JTable t, boolean isSelected, int row) {
        Object hoverObj = t.getClientProperty("ui.hoverRow");
        int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
        if (isSelected) lbl.setBackground(PRIMARY_SOFT);
        else if (row == hover) lbl.setBackground(HOVER_ROW);
        else lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
    }

    /** Renderer chữ có font/màu riêng nhưng vẫn giữ zebra + padding chuẩn. */
    private static DefaultTableCellRenderer cellRenderer(Font font, Color fg) {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                lbl.setFont(font);
                lbl.setForeground(fg);
                paintRowBackground(lbl, t, s, r);
                return lbl;
            }
        };
    }

    /** Renderer cột icon (ImageIcon) giữ zebra + căn giữa. */
    private static DefaultTableCellRenderer iconCellRenderer() {
        return new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, null, s, f, r, c);
                lbl.setText("");
                lbl.setIcon(v instanceof Icon ? (Icon) v : null);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(new EmptyBorder(0, SP_XS, 0, SP_XS));
                paintRowBackground(lbl, t, s, r);
                return lbl;
            }
        };
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionData(); // donate_package nằm ở nro_data
    }

    private void loadCacheData() {
        try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement()) {
            try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id FROM item_template")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    itemTemplateMap.put(id, name);
                    itemIconMap.put(id, rs.getInt("icon_id"));
                    listAllItems.add(new ItemData(id, name));
                }
            }
            try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM item_option_template")) {
                while (rs.next()) optionTemplateMap.put(rs.getInt("id"), services.ItemService.resolveOptionName(rs.getInt("id"), rs.getString("name")));
            } catch (Exception ignored) {}
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getItemName(int id) { return itemTemplateMap.getOrDefault(id, "Item #" + id); }
    private String getOptionName(int id) { return optionTemplateMap.getOrDefault(id, "Option " + id); }

    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId)) return iconCache.get(itemId);
        if (noIconCache.containsKey(itemId)) return null;
        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId != -1) {
                String[] zoomLevels = {"x4", "x3", "x2", "x1"};
                File f = null;
                for (String zoom : zoomLevels) {
                    f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                    if (f.exists()) break;
                }
                if (f != null && f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    ImageIcon icon = new ImageIcon(img.getScaledInstance(22, 22, Image.SCALE_SMOOTH));
                    iconCache.put(itemId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        noIconCache.put(itemId, true);
        return null;
    }

    /** Diễn giải cột items (text) thành chữ dễ đọc cho bảng chính. */
    private String describeItems(String raw) {
        if (raw == null) return "";
        StringBuilder sb = new StringBuilder();
        for (String line : raw.split("[\r\n;]+")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] tk = line.split("[\\s,]+");
            try {
                int id = Integer.parseInt(tk[0]);
                int qty = tk.length > 1 && !tk[1].contains(":") ? Integer.parseInt(tk[1]) : 1;
                if (sb.length() > 0) sb.append(", ");
                sb.append(getItemName(id));
                if (qty > 1) sb.append(" x").append(qty);
            } catch (Exception ignored) {}
        }
        return sb.toString();
    }

    // ------------------------------------------------------------------ UI chính
    private void initUI() {
        // --- card tiêu đề + toolbar ---
        JPanel top = card(new BorderLayout());
        ((ShadowCard) top).setContentPadding(SP_LG, SP_LG, SP_MD, SP_LG);

        JButton btnAdd = button("Thêm Gói", ButtonVariant.PRIMARY, "diamond");
        JButton btnEdit = button("Sửa Gói", ButtonVariant.SECONDARY, "edit");
        JButton btnToggle = button("Bật / Tắt", ButtonVariant.SECONDARY, "power");
        JButton btnDelete = button("Xóa Gói", ButtonVariant.DANGER, "broom");
        JButton btnReload = button("Tải lại", ButtonVariant.GHOST, "refresh");

        btnAdd.addActionListener(e -> openPackEditor(null));
        btnEdit.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row == -1) { JOptionPane.showMessageDialog(this, "Chọn gói cần sửa!"); return; }
            openPackEditor(table.convertRowIndexToModel(row));
        });
        btnToggle.addActionListener(e -> toggleSelectedPack());
        btnDelete.addActionListener(e -> deleteSelectedPack());
        btnReload.addActionListener(e -> loadDataFromDB());

        JPanel toolbar = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_XS));
        toolbar.setOpaque(false);
        toolbar.add(btnAdd); toolbar.add(btnEdit); toolbar.add(btnToggle); toolbar.add(btnDelete); toolbar.add(btnReload);

        top.add(sectionHeader("Gói quà Donate — NPC Chú Bé Đần (Đảo Kamê)",
                "Giá tính bằng VNĐ, trừ vào SỐ DƯ NẠP (cột account.sotien). Mỗi gói mỗi nhân vật chỉ mua được 1 LẦN. Lưu xong mở lại NPC là thấy — KHÔNG cần restart.",
                null), BorderLayout.NORTH);
        top.add(toolbar, BorderLayout.CENTER);

        add(top, BorderLayout.NORTH);

        // --- card danh sách gói ---
        String[] columns = {"ID", "Tên gói", "Giá (VNĐ)", "Vật phẩm", "Đã bán", "Thứ tự", "Trạng thái"};
        model = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
        };
        table = new JTable(model);
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JScrollPane tableScroll = styleTable(table);
        table.setRowHeight(36);

        table.getColumnModel().getColumn(0).setPreferredWidth(40);
        table.getColumnModel().getColumn(1).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(360);

        // Tên gói: đậm
        table.getColumnModel().getColumn(1).setCellRenderer(cellRenderer(FONT_BODY_B, TEXT));
        // Giá: FONT_H2 + glyph tiền
        final Icon moneyGlyph = vectorIcon("money", 14, SUCCESS);
        table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                lbl.setFont(FONT_H2);
                lbl.setForeground(SUCCESS);
                lbl.setIcon(moneyGlyph);
                lbl.setIconTextGap(SP_XS + 2);
                paintRowBackground(lbl, t, s, r);
                return lbl;
            }
        });
        // Trạng thái: màu theo trạng thái bán
        table.getColumnModel().getColumn(6).setCellRenderer(new DefaultTableCellRenderer() {
            @Override public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                boolean on = "Đang bán".equals(String.valueOf(v));
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                lbl.setFont(FONT_SMALL_B);
                lbl.setForeground(on ? SUCCESS : TEXT_MUTED);
                paintRowBackground(lbl, t, s, r);
                return lbl;
            }
        });

        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row != -1) openPackEditor(table.convertRowIndexToModel(row));
                }
            }
        });

        JPanel tableCard = card(new BorderLayout());
        tableCard.add(sectionHeader("Danh sách gói"), BorderLayout.NORTH);
        tableCard.add(tableScroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    private void loadDataFromDB() {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(
                         "SELECT p.*, (SELECT COUNT(*) FROM donate_package_buy b WHERE b.package_id = p.id) AS sold "
                         + "FROM donate_package p ORDER BY p.sort_order, p.id")) {
                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getString("name"));
                    row.add(String.format("%,d", rs.getLong("cost_vnd")));
                    row.add(describeItems(rs.getString("items")));
                    row.add(rs.getInt("sold"));
                    row.add(rs.getInt("sort_order"));
                    row.add(rs.getInt("active") == 1 ? "Đang bán" : "Đang ẩn");
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void toggleSelectedPack() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Chọn gói cần bật/tắt!"); return; }
        int id = (int) model.getValueAt(table.convertRowIndexToModel(row), 0);
        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("UPDATE donate_package SET active = 1 - active WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                SwingUtilities.invokeLater(this::loadDataFromDB);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void deleteSelectedPack() {
        int row = table.getSelectedRow();
        if (row == -1) { JOptionPane.showMessageDialog(this, "Chọn gói cần xóa!"); return; }
        int modelRow = table.convertRowIndexToModel(row);
        int id = (int) model.getValueAt(modelRow, 0);
        String name = String.valueOf(model.getValueAt(modelRow, 1));
        if (JOptionPane.showConfirmDialog(this,
                "Xóa gói \"" + name + "\"?\nNgười đã mua vẫn giữ đồ, lịch sử mua được giữ lại.",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
            return;
        }
        new Thread(() -> {
            try (Connection conn = getConnection();
                 PreparedStatement ps = conn.prepareStatement("DELETE FROM donate_package WHERE id=?")) {
                ps.setInt(1, id);
                ps.executeUpdate();
                SwingUtilities.invokeLater(this::loadDataFromDB);
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    // ------------------------------------------------------------------ editor gói
    private void openPackEditor(Integer rowIndex) {
        Integer dbId = rowIndex != null ? (Integer) model.getValueAt(rowIndex, 0) : null;

        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                dbId == null ? "Thêm Gói Donate" : "Sửa Gói Donate #" + dbId, true);
        dialog.setSize(1000, 700);
        dialog.setLayout(new BorderLayout(SP_XS, SP_XS));
        dialog.setLocationRelativeTo(this);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(BG);
        content.setBorder(pad(SP_SM, SP_MD, SP_MD, SP_MD));

        // --- thông tin gói ---
        JPanel infoPanel = card(new BorderLayout());
        JPanel infoForm = new JPanel(new GridBagLayout());
        infoForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.WEST;

        JTextField txtName = new JTextField(20);
        JTextField txtCost = new JTextField("0", 12);
        JTextField txtSort = new JTextField("0", 6);
        JTextField txtDetail = new JTextField(40);
        JCheckBox chkActive = new JCheckBox("Đang bán (bỏ tick = ẩn khỏi NPC, không xóa dữ liệu)", true);
        chkActive.setOpaque(false);

        gbc.gridx = 0; gbc.gridy = 0; infoForm.add(label("Tên gói:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1; infoForm.add(txtName, gbc);
        gbc.gridx = 2; infoForm.add(label("Giá (VNĐ, trừ vào sotien):", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 3; infoForm.add(txtCost, gbc);
        gbc.gridx = 4; infoForm.add(label("Thứ tự:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 5; infoForm.add(txtSort, gbc);

        gbc.gridx = 0; gbc.gridy = 1; infoForm.add(label("Mô tả thêm:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1; gbc.gridwidth = 5; infoForm.add(txtDetail, gbc); gbc.gridwidth = 1;

        gbc.gridx = 1; gbc.gridy = 2; gbc.gridwidth = 5; infoForm.add(chkActive, gbc); gbc.gridwidth = 1;

        infoPanel.add(sectionHeader("Thông tin gói"), BorderLayout.NORTH);
        infoPanel.add(infoForm, BorderLayout.CENTER);

        // --- bảng item trong gói ---
        JPanel itemPanel = card(new BorderLayout());

        String[] iCols = {"ID", "Icon", "Tên Item", "Số lượng", "Options (id:param, cách nhau khoảng trắng)"};
        DefaultTableModel iModel = new DefaultTableModel(iCols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };
        JTable iTable = new JTable(iModel);
        JScrollPane iScroll = styleTable(iTable);
        iTable.setRowHeight(36);
        iTable.getColumnModel().getColumn(0).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(1).setPreferredWidth(50);
        iTable.getColumnModel().getColumn(2).setPreferredWidth(220);
        iTable.getColumnModel().getColumn(4).setPreferredWidth(280);
        iTable.getColumnModel().getColumn(1).setCellRenderer(iconCellRenderer());
        iTable.getColumnModel().getColumn(2).setCellRenderer(cellRenderer(FONT_BODY_B, TEXT));

        JPanel iTool = new JPanel(new WrapLayout(FlowLayout.RIGHT, SP_SM, 0));
        iTool.setOpaque(false);
        JButton btnAddI = button("Thêm Item", ButtonVariant.PRIMARY, "plus");
        JButton btnEditI = button("Sửa", ButtonVariant.SECONDARY, "edit");
        JButton btnDelI = button("Xóa", ButtonVariant.DANGER, "broom");
        iTool.add(btnAddI); iTool.add(btnEditI); iTool.add(btnDelI);

        btnAddI.addActionListener(e -> openItemBuilder(iModel, -1));
        btnEditI.addActionListener(e -> {
            if (iTable.getSelectedRow() != -1) openItemBuilder(iModel, iTable.getSelectedRow());
        });
        btnDelI.addActionListener(e -> {
            if (iTable.getSelectedRow() != -1) iModel.removeRow(iTable.getSelectedRow());
        });
        iTable.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && iTable.getSelectedRow() != -1) openItemBuilder(iModel, iTable.getSelectedRow());
            }
        });

        itemPanel.add(sectionHeader("Vật phẩm trong gói", null, iTool), BorderLayout.NORTH);
        itemPanel.add(iScroll, BorderLayout.CENTER);

        // --- nạp dữ liệu cũ khi sửa ---
        if (dbId != null) {
            new Thread(() -> {
                try (Connection conn = getConnection();
                     PreparedStatement ps = conn.prepareStatement("SELECT * FROM donate_package WHERE id=?")) {
                    ps.setInt(1, dbId);
                    ResultSet rs = ps.executeQuery();
                    if (rs.next()) {
                        String name = rs.getString("name");
                        long cost = rs.getLong("cost_vnd");
                        String detail = rs.getString("detail");
                        int sort = rs.getInt("sort_order");
                        boolean active = rs.getInt("active") == 1;
                        String items = rs.getString("items");
                        SwingUtilities.invokeLater(() -> {
                            txtName.setText(name);
                            txtCost.setText(String.valueOf(cost));
                            txtDetail.setText(detail);
                            txtSort.setText(String.valueOf(sort));
                            chkActive.setSelected(active);
                            loadItemsToTable(items, iModel);
                        });
                    }
                } catch (Exception e) { e.printStackTrace(); }
            }).start();
        }

        // --- lưu ---
        JButton btnSave = button("LƯU GÓI", ButtonVariant.SUCCESS, "save");
        btnSave.setPreferredSize(new Dimension(200, 45));
        btnSave.setFont(FONT_H2);
        btnSave.addActionListener(e -> {
            String name = txtName.getText().trim();
            if (name.isEmpty()) { JOptionPane.showMessageDialog(dialog, "Nhập tên gói!"); return; }
            long cost;
            int sort;
            try {
                cost = Long.parseLong(txtCost.getText().trim());
                if (cost < 0) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "Giá VNĐ phải là số >= 0!"); return;
            }
            if (cost > 2_000_000_000L) {
                // quá int max sẽ tràn số khi trừ sotien trong game
                JOptionPane.showMessageDialog(dialog, "Giá tối đa 2 tỷ VNĐ!"); return;
            }
            try { sort = Integer.parseInt(txtSort.getText().trim()); } catch (NumberFormatException ex) { sort = 0; }
            if (iModel.getRowCount() == 0) { JOptionPane.showMessageDialog(dialog, "Gói phải có ít nhất 1 item!"); return; }

            StringBuilder items = new StringBuilder();
            for (int i = 0; i < iModel.getRowCount(); i++) {
                if (i > 0) items.append("\n");
                items.append(iModel.getValueAt(i, 0)).append(" ").append(iModel.getValueAt(i, 3));
                String opts = String.valueOf(iModel.getValueAt(i, 4)).trim();
                if (!opts.isEmpty()) items.append(" ").append(opts);
            }
            savePackToDB(dbId, name, cost, txtDetail.getText().trim(), items.toString(),
                    chkActive.isSelected() ? 1 : 0, sort, dialog);
        });

        JPanel bot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, SP_XS));
        bot.setOpaque(false);
        bot.add(btnSave);

        dialog.add(infoPanel, BorderLayout.NORTH);
        dialog.add(itemPanel, BorderLayout.CENTER);
        dialog.add(bot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    private void savePackToDB(Integer dbId, String name, long cost, String detail,
            String items, int active, int sort, JDialog dialog) {
        new Thread(() -> {
            try (Connection conn = getConnection()) {
                if (dbId == null) {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "INSERT INTO donate_package (name, detail, cost_vnd, items, active, sort_order) VALUES (?,?,?,?,?,?)")) {
                        ps.setString(1, name);
                        ps.setString(2, detail);
                        ps.setLong(3, cost);
                        ps.setString(4, items);
                        ps.setInt(5, active);
                        ps.setInt(6, sort);
                        ps.executeUpdate();
                    }
                } else {
                    try (PreparedStatement ps = conn.prepareStatement(
                            "UPDATE donate_package SET name=?, detail=?, cost_vnd=?, items=?, active=?, sort_order=? WHERE id=?")) {
                        ps.setString(1, name);
                        ps.setString(2, detail);
                        ps.setLong(3, cost);
                        ps.setString(4, items);
                        ps.setInt(5, active);
                        ps.setInt(6, sort);
                        ps.setInt(7, dbId);
                        ps.executeUpdate();
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(dialog,
                            "Lưu thành công! Người chơi mở lại NPC là thấy thay đổi (không cần restart).");
                    dialog.dispose();
                    loadDataFromDB();
                });
            } catch (Exception e) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(dialog, "Lỗi SQL: " + e.getMessage()));
            }
        }).start();
    }

    /** Nạp cột items (text) của gói vào bảng item trong editor. */
    private void loadItemsToTable(String raw, DefaultTableModel iModel) {
        if (raw == null) return;
        for (String line : raw.split("[\r\n;]+")) {
            line = line.trim();
            if (line.isEmpty()) continue;
            String[] tk = line.split("[\\s,]+");
            try {
                int id = Integer.parseInt(tk[0]);
                int qty = 1;
                StringBuilder opts = new StringBuilder();
                for (int i = 1; i < tk.length; i++) {
                    if (tk[i].contains(":")) {
                        if (opts.length() > 0) opts.append(" ");
                        opts.append(tk[i]);
                    } else {
                        qty = Integer.parseInt(tk[i]);
                    }
                }
                iModel.addRow(new Object[]{id, getItemIcon(id), getItemName(id), qty, opts.toString()});
            } catch (Exception ignored) {}
        }
    }

    // ------------------------------------------------------------------ item builder
    private void openItemBuilder(DefaultTableModel parentModel, int editRow) {
        JDialog dialog = new JDialog((Frame) null, "Cấu hình Vật Phẩm", true);
        dialog.setSize(750, 650);
        dialog.setLayout(new BorderLayout(SP_XS, SP_XS));
        dialog.setLocationRelativeTo(null);
        JPanel content = (JPanel) dialog.getContentPane();
        content.setBackground(BG);
        content.setBorder(pad(SP_SM, SP_MD, SP_MD, SP_MD));

        JPanel pTop = card(new BorderLayout());
        JPanel pTopForm = new JPanel(new GridBagLayout());
        pTopForm.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(SP_XS, SP_XS, SP_XS, SP_XS);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtId = new JTextField();
        JTextField txtQty = new JTextField("1");
        JLabel lblName = label("Chưa chọn item", FONT_BODY_B, PRIMARY);
        JLabel lblIcon = new JLabel();
        lblIcon.setPreferredSize(new Dimension(30, 30));
        JButton btnSearchItem = button("Tìm nhanh", ButtonVariant.SECONDARY, "search");

        String oldOpts = "";
        if (editRow != -1) {
            txtId.setText(parentModel.getValueAt(editRow, 0).toString());
            txtQty.setText(parentModel.getValueAt(editRow, 3).toString());
            oldOpts = String.valueOf(parentModel.getValueAt(editRow, 4));
        }

        txtId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { u(); }
            public void removeUpdate(DocumentEvent e) { u(); }
            public void changedUpdate(DocumentEvent e) { u(); }
            void u() {
                try {
                    int id = Integer.parseInt(txtId.getText());
                    lblName.setText(getItemName(id));
                    lblIcon.setIcon(getItemIcon(id));
                } catch (Exception ex) {
                    lblName.setText("...");
                    lblIcon.setIcon(null);
                }
            }
        });
        if (!txtId.getText().isEmpty()) {
            try {
                int id = Integer.parseInt(txtId.getText());
                lblName.setText(getItemName(id));
                lblIcon.setIcon(getItemIcon(id));
            } catch (Exception ignored) {}
        }

        btnSearchItem.addActionListener(e -> showItemSearchDialog(dialog, (id, name) -> {
            txtId.setText(String.valueOf(id));
            lblName.setText(name);
        }));

        g.gridx = 0; g.gridy = 0; pTopForm.add(label("ID Item:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; pTopForm.add(txtId, g);
        g.gridx = 2; pTopForm.add(btnSearchItem, g);

        g.gridx = 0; g.gridy = 1; pTopForm.add(label("Thông tin:", FONT_SMALL_B, TEXT_SECONDARY), g);
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pInfo.setOpaque(false);
        pInfo.add(lblIcon);
        pInfo.add(lblName);
        g.gridx = 1; g.gridwidth = 2; pTopForm.add(pInfo, g); g.gridwidth = 1;

        g.gridx = 0; g.gridy = 2; pTopForm.add(label("Số lượng:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; pTopForm.add(txtQty, g);

        pTop.add(sectionHeader("Chọn Item"), BorderLayout.NORTH);
        pTop.add(pTopForm, BorderLayout.CENTER);

        // --- bảng option ---
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
                        oModel.setValueAt(getOptionName(id).replace("#", String.valueOf(param)), row, 2);
                    } catch (Exception ignored) {}
                });
            }
        });

        // nạp option cũ "50:20 77:20"
        for (String tk : oldOpts.trim().split("\\s+")) {
            if (tk.contains(":")) {
                try {
                    String[] p = tk.split(":");
                    int oid = Integer.parseInt(p[0]);
                    int par = Integer.parseInt(p[1]);
                    oModel.addRow(new Object[]{oid, par, getOptionName(oid).replace("#", String.valueOf(par))});
                } catch (Exception ignored) {}
            }
        }

        JPanel pOpt = card(new BorderLayout());
        JPanel pOptTool = new JPanel(new WrapLayout(FlowLayout.RIGHT, SP_SM, 0));
        pOptTool.setOpaque(false);
        JButton btnAddO = button("Thêm dòng", ButtonVariant.SECONDARY, "plus");
        JButton btnFindO = button("Tra cứu Option", ButtonVariant.SECONDARY, "search");
        JButton btnDelO = button("Xóa dòng", ButtonVariant.DANGER, "broom");

        btnAddO.addActionListener(e -> oModel.addRow(new Object[]{"0", "0", getOptionName(0).replace("#", "0")}));
        btnFindO.addActionListener(e -> showOptionSearchDialog(dialog, (id, name) ->
                oModel.addRow(new Object[]{id, "0", name.replace("#", "0")})));
        btnDelO.addActionListener(e -> {
            if (oTable.getSelectedRow() != -1) oModel.removeRow(oTable.getSelectedRow());
        });

        pOptTool.add(btnAddO); pOptTool.add(btnFindO); pOptTool.add(btnDelO);
        pOpt.add(sectionHeader("Danh sách Chỉ Số (Option)", null, pOptTool), BorderLayout.NORTH);
        pOpt.add(oScroll, BorderLayout.CENTER);

        JButton btnOk = button("XÁC NHẬN & LƯU ITEM", ButtonVariant.SUCCESS, "check");
        btnOk.addActionListener(e -> {
            int id;
            int qty;
            try {
                id = Integer.parseInt(txtId.getText().trim());
                qty = Integer.parseInt(txtQty.getText().trim());
                if (qty < 1) throw new NumberFormatException();
            } catch (NumberFormatException ex) {
                JOptionPane.showMessageDialog(dialog, "ID item và số lượng phải là số hợp lệ!");
                return;
            }
            if (oTable.isEditing()) oTable.getCellEditor().stopCellEditing();
            StringBuilder opts = new StringBuilder();
            for (int i = 0; i < oModel.getRowCount(); i++) {
                try {
                    int oid = Integer.parseInt(oModel.getValueAt(i, 0).toString().trim());
                    int par = Integer.parseInt(oModel.getValueAt(i, 1).toString().trim());
                    if (opts.length() > 0) opts.append(" ");
                    opts.append(oid).append(":").append(par);
                } catch (Exception ignored) {}
            }
            Object[] row = {id, getItemIcon(id), getItemName(id), qty, opts.toString()};
            if (editRow == -1) parentModel.addRow(row);
            else for (int i = 0; i < 5; i++) parentModel.setValueAt(row[i], editRow, i);
            dialog.dispose();
        });

        JPanel pBot = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, SP_XS));
        pBot.setOpaque(false);
        pBot.add(btnOk);

        dialog.add(pTop, BorderLayout.NORTH);
        dialog.add(pOpt, BorderLayout.CENTER);
        dialog.add(pBot, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ------------------------------------------------------------------ dialog tìm kiếm
    private interface ItemCallback { void onSelect(int id, String name); }

    private void showItemSearchDialog(JDialog parent, ItemCallback callback) {
        JDialog d = new JDialog(parent, "Tìm kiếm Vật Phẩm", true);
        d.setSize(850, 600);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);
        JPanel content = (JPanel) d.getContentPane();
        content.setBackground(BG);
        content.setBorder(pad(SP_MD, SP_MD, SP_MD, SP_MD));

        JTextField txtSearch = searchField("Nhập Tên hoặc ID item...");

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
        };
        for (ItemData item : listAllItems) {
            m.addRow(new Object[]{item.id, getItemIcon(item.id), item.name});
        }

        JTable t = new JTable(m);
        JScrollPane tScroll = styleTable(t);
        t.setRowHeight(36);
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(50);
        t.getColumnModel().getColumn(2).setPreferredWidth(500);
        t.getColumnModel().getColumn(1).setCellRenderer(iconCellRenderer());

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }
            void f() {
                String text = txtSearch.getText().trim();
                if (text.isEmpty()) { sorter.setRowFilter(null); return; }
                try {
                    int idVal = Integer.parseInt(text);
                    List<RowFilter<Object, Object>> or = new ArrayList<>();
                    or.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, idVal, 0));
                    or.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                    sorter.setRowFilter(RowFilter.orFilter(or));
                } catch (NumberFormatException e) {
                    sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                }
            }
        });

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        callback.onSelect((int) m.getValueAt(modelRow, 0), (String) m.getValueAt(modelRow, 2));
                        d.dispose();
                    }
                }
            }
        });

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.setBorder(pad(0, 0, SP_MD, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);

        d.add(searchWrap, BorderLayout.NORTH);
        d.add(tScroll, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void showOptionSearchDialog(JDialog parent, ItemCallback callback) {
        JDialog d = new JDialog(parent, "Tìm kiếm Option", true);
        d.setSize(600, 500);
        d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);
        JPanel content = (JPanel) d.getContentPane();
        content.setBackground(BG);
        content.setBorder(pad(SP_MD, SP_MD, SP_MD, SP_MD));

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
                if (text.trim().isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + text));
            }
        });

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        callback.onSelect((int) t.getValueAt(r, 0), (String) t.getValueAt(r, 1));
                        d.dispose();
                    }
                }
            }
        });

        JPanel searchWrap = new JPanel(new BorderLayout());
        searchWrap.setOpaque(false);
        searchWrap.setBorder(pad(0, 0, SP_MD, 0));
        searchWrap.add(txtSearch, BorderLayout.CENTER);

        d.add(searchWrap, BorderLayout.NORTH);
        d.add(tScroll, BorderLayout.CENTER);
        d.setVisible(true);
    }
}
