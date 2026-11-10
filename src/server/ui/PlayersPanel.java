package server.ui;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import jdbc.DBConnecter;
import utils.Logger;

import static server.ui.ServerGuiUtils.*;


public class PlayersPanel extends JPanel {

    private static final String ICON_FOLDER = "data/icon/"; 
    private static final String SERVER_IP = "127.0.0.1"; 
    private static final int SERVER_PORT = 14445; 

    private JTable table;
    private DefaultTableModel model;
    private JTextField txtSearch;
    private TableRowSorter<DefaultTableModel> sorter;
    private JButton btnQuickSave;
    
    private final Map<Integer, String> itemTemplateMap = new HashMap<>();
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();
    private final Map<Integer, String> clanNameMap = new HashMap<>();
    private final Map<Integer, String> optionTemplateMap = new HashMap<>();
    
    private final Map<Integer, BadgeTemplate> badgeTemplateMap = new HashMap<>();

    private final Map<Integer, String> taskMainTemplateMap = new HashMap<>();
    private final Map<Integer, String> taskMainDetailMap = new HashMap<>();
    private final Map<Integer, List<SubTaskTemplate>> taskSubTemplateMap = new HashMap<>();
    private final Map<Integer, String> sideTaskTemplateMap = new HashMap<>();
    private final Map<Integer, String> clanTaskTemplateMap = new HashMap<>();
    private final Map<Integer, String> kolTaskTemplateMap = new HashMap<>();
    
    private final Map<Integer, String> inventoryCache = new HashMap<>();
    private final Set<Integer> modifiedPlayerIds = new HashSet<>();
    
    private final List<ItemData> listAllItems = new ArrayList<>();

    private static class ItemData {
        int id; String name; int type; int gender;
        public ItemData(int id, String name, int type, int gender) {
            this.id = id; this.name = name; this.type = type; this.gender = gender;
        }
    }
    
    private static class BadgeTemplate {
        int id;
        String name;
        String optionsJson;
        int iconId;
        
        public BadgeTemplate(int id, String name, String optionsJson, int iconId) {
            this.id = id;
            this.name = name;
            this.optionsJson = optionsJson;
            this.iconId = iconId;
        }
    }
    
    private static class SubTaskTemplate {
        String name;
        short maxCount;
        String notify;
        byte npcId;
        short mapId;
    }
    
    private final Map<Integer, Integer> partHeadIconMap = new HashMap<>(); 
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> headCache = new HashMap<>();
    private final Map<Integer, ImageIcon> rawIconCache = new HashMap<>();

    public PlayersPanel() {
        setLayout(new BorderLayout(0, SP_SM));
        setBackground(BG);
        setBorder(new EmptyBorder(SP_MD, SP_LG, SP_LG, SP_LG));

        initStaticData();
        loadCacheData();
        loadPartsHead("");

        initTopControls();
        initTable();
        setupGlobalShortcuts();
    }

    private void setupGlobalShortcuts() {
        InputMap im = this.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = this.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "quickSave");
        am.put("quickSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (btnQuickSave.isEnabled()) {
                    saveModifiedRows();
                }
            }
        });
    }

    private void addUndoRedo(JTextComponent textComponent) {
        UndoManager undoManager = new UndoManager();
        textComponent.getDocument().addUndoableEditListener(e -> undoManager.addEdit(e.getEdit()));

        InputMap im = textComponent.getInputMap(JComponent.WHEN_FOCUSED);
        ActionMap am = textComponent.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Z, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Undo");
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_Y, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "Redo");

        am.put("Undo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canUndo()) undoManager.undo();
            }
        });
        am.put("Redo", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (undoManager.canRedo()) undoManager.redo();
            }
        });
    }

    private Connection getConnection() throws SQLException {
        return DBConnecter.getConnectionServer();
    }

    private void sendCommandToServer(String command) {
        new Thread(() -> {
            try (DatagramSocket socket = new DatagramSocket()) {
                InetAddress address = InetAddress.getByName(SERVER_IP);
                byte[] buffer = command.getBytes("UTF-8");
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, SERVER_PORT);
                socket.send(packet);
                System.out.println("Đã gửi lệnh đến server: " + command);
            } catch (Exception e) {
                System.err.println("Lỗi gửi lệnh đến server: " + e.getMessage());
            }
        }).start();
    }

    private void loadCacheData() {
        new Thread(() -> {
            listAllItems.clear();
            try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement()) {
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, type, gender FROM item_template")) {
                    while (rs.next()) {
                        listAllItems.add(new ItemData(rs.getInt("id"), rs.getString("name"), rs.getInt("type"), rs.getInt("gender")));
                        itemTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                        itemIconMap.put(rs.getInt("id"), rs.getInt("icon_id"));
                    }
                }
                
                // clan nằm ở DB tài khoản (nro_acc), không phải nro_data
                try (Connection accCon = DBConnecter.getConnectionServer();
                     Statement accStmt = accCon.createStatement();
                     ResultSet rs = accStmt.executeQuery("SELECT id, name FROM clan")) {
                    while (rs.next()) clanNameMap.put(rs.getInt("id"), rs.getString("name"));
                } catch (Exception ex) {
                }
                
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM item_option_template")) {
                    while (rs.next()) optionTemplateMap.put(rs.getInt("id"), services.ItemService.resolveOptionName(rs.getInt("id"), rs.getString("name")));
                }
                
                // data_badges không có trong schema này — bỏ qua để không làm hỏng các query sau
                try (ResultSet rs = stmt.executeQuery("SELECT b.id, b.idEffect, b.name, b.Options, i.icon_id FROM data_badges b LEFT JOIN item_template i ON b.iditem = i.id")) {
                    while (rs.next()) {
                        int idKey = rs.getInt("idEffect");

                        String name = rs.getString("name");
                        String options = rs.getString("Options");
                        int iconId = rs.getInt("icon_id");

                        badgeTemplateMap.put(idKey, new BadgeTemplate(idKey, name, options, iconId));
                    }
                } catch (Exception exBadge) {
                }
                
                try (ResultSet rs = stmt.executeQuery("SELECT id, name, detail FROM task_main_template")) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        taskMainTemplateMap.put(id, rs.getString("name"));
                        taskMainDetailMap.put(id, rs.getString("detail"));
                    }
                }
                
                try (ResultSet rs = stmt.executeQuery("SELECT task_main_id, name, max_count, notify, npc_id, map FROM task_sub_template ORDER BY task_main_id")) {
                    while (rs.next()) {
                        int taskMainId = rs.getInt("task_main_id");
                        SubTaskTemplate subTask = new SubTaskTemplate();
                        subTask.name = rs.getString("name");
                        subTask.maxCount = rs.getShort("max_count");
                        subTask.notify = rs.getString("notify");
                        subTask.npcId = rs.getByte("npc_id");
                        subTask.mapId = rs.getShort("map");
                        
                        taskSubTemplateMap.computeIfAbsent(taskMainId, k -> new ArrayList<>()).add(subTask);
                    }
                }
                
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM side_task_template")) {
                    while (rs.next()) {
                        sideTaskTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                    }
                }
                
                try (ResultSet rs = stmt.executeQuery("SELECT id, name FROM clan_task_template")) {
                    while (rs.next()) {
                        clanTaskTemplateMap.put(rs.getInt("id"), rs.getString("name"));
                    }
                }
                
                // task_kol_template không có trong schema này — bỏ qua
                try (ResultSet rs = stmt.executeQuery("SELECT id, info FROM task_kol_template")) {
                    while (rs.next()) {
                        kolTaskTemplateMap.put(rs.getInt("id"), rs.getString("info"));
                    }
                } catch (Exception exKol) {
                }
                
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
    
    private void loadPartsHead(String keyword) {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
                while (rs.next()) {
                    try {
                        JsonArray arr = new JsonParser().parse(rs.getString("data")).getAsJsonArray();
                        if (arr.size() > 0) partHeadIconMap.put(rs.getInt("id"), arr.get(0).getAsJsonArray().get(0).getAsInt());
                    } catch (Exception ignored) {}
                }
            } catch (SQLException ignored) {}
            SwingUtilities.invokeLater(() -> loadPlayersFromDB(keyword));
        }).start();
    }
    
    private ImageIcon drawHeadIcon(int headPartId) {
        if (headPartId <= 0) return null;
        if (headCache.containsKey(headPartId)) return headCache.get(headPartId);
        Integer iconId = partHeadIconMap.get(headPartId);
        if (iconId != null) {
            try {
                String[] zoomLevels = {"x4", "x3", "x2", "x1"};
                for (String zoom : zoomLevels) {
                    File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                    if (f.exists()) {
                        Image dimg = ImageIO.read(f).getScaledInstance(28, 28, Image.SCALE_SMOOTH);
                        ImageIcon icon = new ImageIcon(dimg);
                        headCache.put(headPartId, icon);
                        return icon;
                    }
                }
            } catch (Exception e) {}
        }
        return null;
    }

    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId)) return iconCache.get(itemId);
        if (noIconCache.containsKey(itemId)) return null;
        try {
            int iconId = itemIconMap.getOrDefault(itemId, -1);
            if (iconId == -1) { noIconCache.put(itemId, true); return null; }
            return loadIconRaw(iconId);
        } catch (Exception e) { }
        noIconCache.put(itemId, true);
        return null;
    }
    
    private ImageIcon loadIconRaw(int iconId) {
        if (iconId <= 0) return null;
        if (rawIconCache.containsKey(iconId)) return rawIconCache.get(iconId);
        try {
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            File f = null;
            for (String zoom : zoomLevels) {
                f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) break;
            }
            if (f != null && f.exists()) {
                Image dimg = ImageIO.read(f).getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                ImageIcon icon = new ImageIcon(dimg);
                rawIconCache.put(iconId, icon);
                return icon;
            }
        } catch (Exception e) { }
        return null;
    }

    private void initStaticData() {
        String raw = "0,Tấn công +#;50,Sức đánh +#%;77,HP +#%;103,KI +#%;14,Chí mạng +#%;30,Khóa giao dịch;93,Hạn sử dụng # ngày;73,Không thể bán;9,Hiệu lực # phút";
        for (String s : raw.split(";")) {
            String[] p = s.split(",");
            if(p.length==2) optionTemplateMap.put(Integer.parseInt(p[0]), p[1]);
        }
    }

    private String getItemName(int id) { return itemTemplateMap.getOrDefault(id, "Unknown [" + id + "]"); }
    private String getClanName(int id) { return id == -1 ? "Không có" : clanNameMap.getOrDefault(id, "Clan [" + id + "]"); }
    private String getOptionName(int id) { return optionTemplateMap.getOrDefault(id, "Option " + id); }
    private String formatOption(int id, int param) { return getOptionName(id).replace("#", String.valueOf(param)); }

    private void initTopControls() {
        txtSearch = searchField("Nhập tên nhân vật để tìm...");
        txtSearch.setPreferredSize(new Dimension(260, 34));
        txtSearch.addKeyListener(new KeyAdapter() {
            public void keyReleased(KeyEvent e) { if (e.getKeyCode() == KeyEvent.VK_ENTER) loadPlayersFromDB(txtSearch.getText().trim()); }
        });
        addUndoRedo(txtSearch);

        JButton btnSearch = button("Tìm kiếm", ButtonVariant.PRIMARY, "search");
        btnSearch.addActionListener(e -> loadPlayersFromDB(txtSearch.getText().trim()));

        btnQuickSave = button("Lưu thay đổi (Ctrl+S)", ButtonVariant.SUCCESS, "save");
        btnQuickSave.setEnabled(false);
        btnQuickSave.setBackground(TEXT_MUTED);
        btnQuickSave.addActionListener(e -> saveModifiedRows());

        JButton btnReload = button("Tải lại DB", ButtonVariant.SECONDARY, "refresh");
        btnReload.addActionListener(e -> { loadPartsHead(txtSearch.getText().trim()); });

        JButton btnGuide = button("Hướng dẫn", ButtonVariant.GHOST, "info");
        btnGuide.addActionListener(e -> showGuide());

        JPanel controls = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_SM));
        controls.setOpaque(false);
        controls.add(txtSearch); controls.add(btnSearch); controls.add(btnQuickSave); controls.add(btnReload); controls.add(btnGuide);

        JPanel topCard = card(new BorderLayout());
        ((ShadowCard) topCard).setContentPadding(SP_LG, SP_LG, SP_MD, SP_LG);
        topCard.add(sectionHeader("Quản lý Người chơi", "Tìm kiếm, sửa nhanh Vàng/Ngọc trên bảng và click đúp để mở hồ sơ chi tiết", null), BorderLayout.NORTH);
        topCard.add(controls, BorderLayout.CENTER);
        add(topCard, BorderLayout.NORTH);
    }

    private void showGuide() {
        String html = "<html><body style='width: 300px'>"
                + "<h3>Hướng dẫn quản lý Người chơi</h3>"
                + "<ul>"
                + "<li><b>Sửa nhanh:</b> Click đúp vào các cột <font color='blue'>Vàng, Ngọc</font> để sửa trực tiếp trên bảng.</li>"
                + "<li><b>Chi tiết:</b> Click đúp vào các cột còn lại (Tên, ID...) để mở cửa sổ chỉnh sửa đầy đủ (Item, Đệ tử...).</li>"
                + "<li><b>Lưu:</b> Sau khi sửa nhanh trên bảng, nhấn nút <b>'Lưu thay đổi'</b> hoặc phím tắt <b>Ctrl + S</b>.</li>"
                + "<li><b>Tác động trực tiếp:</b> Thay đổi sẽ ảnh hưởng ngay lập tức đến server, kể cả khi người chơi đang online.</li>"
                + "<li><b>Tiện ích:</b>"
                + "<ul>"
                + "<li><b>Ctrl + Z:</b> Hoàn tác (Undo) khi nhập liệu text.</li>"
                + "<li><b>Ctrl + Y:</b> Làm lại (Redo) khi nhập liệu text.</li>"
                + "<li><b>Chuột phải:</b> Mở menu chức năng phụ (Buff item...).</li>"
                + "</ul></li>"
                + "</ul>"
                + "<p><i>Lưu ý: Cột 'Tình trạng' màu đỏ nghĩa là tài khoản đang bị khóa (Ban).</i></p>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), "Hướng dẫn sử dụng", JOptionPane.INFORMATION_MESSAGE);
    }

    private void initTable() {
        String[] cols = { "ID", "Head", "Tên nhân vật", "Sức Mạnh", "Clan", "Vàng", "Ngọc", "Xu", "Trạng thái", "Tình trạng" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 5 || column == 6; // Chỉ có thể sửa Vàng và Ngọc
            }
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return ImageIcon.class; 
                if (columnIndex == 0 || columnIndex == 7) return Long.class;
                return super.getColumnClass(columnIndex);
            }
        };
        
        model.addTableModelListener(e -> {
            if (e.getType() == TableModelEvent.UPDATE) {
                int row = e.getFirstRow();
                int col = e.getColumn();
                if (row >= 0 && col >= 0) {
                    try {
                        int playerId = Integer.parseInt(model.getValueAt(row, 0).toString());
                        modifiedPlayerIds.add(playerId);
                        btnQuickSave.setEnabled(true);
                        btnQuickSave.setBackground(WARNING);
                        btnQuickSave.setText("Lưu thay đổi (" + modifiedPlayerIds.size() + ")");
                    } catch (Exception ex) {}
                }
            }
        });

        table = new JTable(model);

        sorter = new TableRowSorter<>(model);
        table.setRowSorter(sorter);

        JScrollPane scroll = styleTable(table);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        table.setRowHeight(40);

        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(50);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);

        PlayersCellRenderer cellRenderer = new PlayersCellRenderer();
        table.setDefaultRenderer(Object.class, cellRenderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(cellRenderer);
        }

        createContextMenu();
        
        table.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int viewRow = table.getSelectedRow();
                    int viewCol = table.getSelectedColumn();
                    if (viewRow != -1) {
                        int modelCol = table.convertColumnIndexToModel(viewCol);
                        if (modelCol == 5 || modelCol == 6) {
                            return; 
                        }
                        int modelRow = table.convertRowIndexToModel(viewRow);
                        openPlayerEditorDB(Integer.parseInt(model.getValueAt(modelRow, 0).toString()));
                    }
                }
            }
        });

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(scroll, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);
    }

    /** Renderer chính cho bảng người chơi: zebra + hover đồng bộ styleTable, cột trạng thái vẽ dạng pill badge. */
    private class PlayersCellRenderer extends DefaultTableCellRenderer {
        private Color pill;

        @Override
        public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
            pill = null;
            setIcon(null);
            setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
            setHorizontalAlignment(SwingConstants.LEFT);
            setFont(FONT_BODY);
            applyRowBackground(this, t, row, isSelected);
            setForeground(TEXT);

            if (value instanceof Icon) {
                setIcon((Icon) value);
                setText(null);
                setHorizontalAlignment(SwingConstants.CENTER);
                return this;
            }

            int modelCol = t.convertColumnIndexToModel(column);
            if (modelCol == 5 || modelCol == 6) {
                setFont(FONT_BODY_B);
                setForeground(PRIMARY);
            } else if (modelCol == 8) {
                setFont(FONT_TINY_B);
                pill = "Đã kích hoạt".equals(String.valueOf(value)) ? SUCCESS : WARNING;
                setForeground(darken(pill, 0.25f));
            } else if (modelCol == 9) {
                setFont(FONT_TINY_B);
                pill = "Bị chặn (Block)".equals(String.valueOf(value)) ? DANGER : SUCCESS;
                setForeground(darken(pill, 0.25f));
            }
            return this;
        }

        @Override
        protected void paintComponent(Graphics g) {
            if (pill == null) {
                super.paintComponent(g);
                return;
            }
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(getBackground());
            g2.fillRect(0, 0, getWidth(), getHeight());
            String txt = getText() == null ? "" : getText();
            FontMetrics fm = g2.getFontMetrics(getFont());
            int pw = fm.stringWidth(txt) + 16;
            int ph = fm.getHeight() + 6;
            int py = (getHeight() - ph) / 2;
            g2.setColor(soften(pill));
            g2.fill(new RoundRectangle2D.Float(SP_MD - 8, py, pw, ph, ph, ph));
            g2.dispose();
            setOpaque(false);
            super.paintComponent(g);
            setOpaque(true);
        }
    }

    /** Nền zebra/hover/chọn giống hệt styleTable để renderer tùy biến không lệch tông. */
    private static void applyRowBackground(JComponent c, JTable t, int row, boolean isSelected) {
        Object hoverObj = t.getClientProperty("ui.hoverRow");
        int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
        if (isSelected) c.setBackground(PRIMARY_SOFT);
        else if (row == hover) c.setBackground(HOVER_ROW);
        else c.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
    }

    /** Khôi phục hiển thị icon cho 1 cột sau khi styleTable gán renderer text mặc định. */
    private static void styleIconColumn(JTable t, int columnIndex) {
        t.getColumnModel().getColumn(columnIndex).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tb, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tb, null, isSelected, hasFocus, row, column);
                lbl.setIcon(value instanceof Icon ? (Icon) value : null);
                lbl.setText(null);
                lbl.setHorizontalAlignment(SwingConstants.CENTER);
                lbl.setBorder(new EmptyBorder(0, SP_XS, 0, SP_XS));
                applyRowBackground(lbl, tb, row, isSelected);
                return lbl;
            }
        });
    }

    /** Khối form trong dialog: thẻ bo góc + tiêu đề nhỏ theo design system (thay cho TitledBorder cũ). */
    private static JPanel sectionCard(String title, JComponent content) {
        JPanel p = card(new BorderLayout(0, SP_SM));
        ((ShadowCard) p).setContentPadding(SP_MD, SP_LG, SP_LG, SP_LG);
        JLabel lbl = ServerGuiUtils.label(title, FONT_H3, TEXT);
        lbl.setBorder(new EmptyBorder(0, 0, SP_XS, 0));
        content.setOpaque(false);
        p.add(lbl, BorderLayout.NORTH);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    /** Scroll pane phẳng trên nền BG cho nội dung tab/dialog. */
    private static JScrollPane plainScroll(JComponent content) {
        JScrollPane sp = new JScrollPane(content);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    private void createContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem mBuffItem = new JMenuItem("Buff Item (Thêm vào hành trang)");
        mBuffItem.setFont(FONT_SMALL_B);
        mBuffItem.setIcon(vectorIcon("gift", 14, TEXT_SECONDARY));
        mBuffItem.addActionListener(e -> {
            int r = table.getSelectedRow();
            if (r != -1) openBuffItemDialog(Integer.parseInt(model.getValueAt(table.convertRowIndexToModel(r), 0).toString()), model.getValueAt(table.convertRowIndexToModel(r), 2).toString());
        });
        menu.add(mBuffItem);
        table.addMouseListener(new MouseAdapter() {
            public void mouseReleased(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            public void mousePressed(MouseEvent e) { if (e.isPopupTrigger()) showMenu(e); }
            private void showMenu(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0 && r < table.getRowCount()) {
                    table.setRowSelectionInterval(r, r);
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });
    }

    private void openBuffItemDialog(int playerId, String playerName) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Buff Item: " + playerName, true);
        d.setSize(400, 300); d.setLocationRelativeTo(null); d.setLayout(new GridLayout(5, 2, SP_MD, SP_MD));
        d.getContentPane().setBackground(BG);
        ((JPanel) d.getContentPane()).setBorder(pad(SP_LG));
        JTextField txtId = new JTextField(); JTextField txtQty = new JTextField("1"); JTextField txtOpt = new JTextField("[]");
        txtId.setFont(FONT_BODY); txtQty.setFont(FONT_BODY); txtOpt.setFont(FONT_BODY);
        addUndoRedo(txtId); addUndoRedo(txtQty); addUndoRedo(txtOpt);
        d.add(ServerGuiUtils.label("ID Item:", FONT_SMALL_B, TEXT_SECONDARY)); d.add(txtId);
        d.add(ServerGuiUtils.label("Số lượng:", FONT_SMALL_B, TEXT_SECONDARY)); d.add(txtQty);
        d.add(ServerGuiUtils.label("Option:", FONT_SMALL_B, TEXT_SECONDARY)); d.add(txtOpt);
        JButton btn = button("OK", ButtonVariant.PRIMARY, "send");
        btn.addActionListener(e -> { 
            try {
                String rawId = txtId.getText().trim();
                String rawQty = txtQty.getText().trim();
                if (rawId.isEmpty()) {
                    JOptionPane.showMessageDialog(d, "Vui lòng nhập ID Item!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }
                int tempId = Integer.parseInt(rawId);
                int qty = rawQty.isEmpty() ? 1 : Integer.parseInt(rawQty);
                if (qty <= 0) qty = 1;

                models.item.Item item = services.ItemService.gI().createNewItem((short) tempId, qty);
                if (item == null || item.template == null) {
                    JOptionPane.showMessageDialog(d, "Vật phẩm có ID " + tempId + " không tồn tại!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                java.util.List<int[]> optionPairs = new java.util.ArrayList<>();
                String rawOpt = txtOpt.getText().trim();
                if (!rawOpt.isEmpty() && !rawOpt.equals("[]")) {
                    try {
                        JsonElement parsed = new JsonParser().parse(rawOpt);
                        if (parsed.isJsonArray()) {
                            for (JsonElement elem : parsed.getAsJsonArray()) {
                                if (elem.isJsonObject()) {
                                    JsonObject o = elem.getAsJsonObject();
                                    int optId = o.get("id").getAsInt();
                                    int param = o.get("param").getAsInt();
                                    item.itemOptions.add(new models.item.Item.ItemOption(optId, param));
                                    optionPairs.add(new int[]{optId, param});
                                } else if (elem.isJsonArray()) {
                                    JsonArray arr = elem.getAsJsonArray();
                                    int optId = arr.get(0).getAsInt();
                                    int param = arr.get(1).getAsInt();
                                    item.itemOptions.add(new models.item.Item.ItemOption(optId, param));
                                    optionPairs.add(new int[]{optId, param});
                                }
                            }
                        }
                    } catch (Exception exOpt) {
                        JOptionPane.showMessageDialog(d, "Định dạng Option JSON không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                        return;
                    }
                }

                models.player.Player pl = server.Client.gI().getPlayer(playerId);
                if (pl != null) {
                    boolean added = services.player.InventoryService.gI().addItemBag(pl, item);
                    if (added) {
                        services.player.InventoryService.gI().sendItemBags(pl);
                        services.Service.gI().sendThongBao(pl, "Admin vừa buff cho bạn " + qty + "x " + item.template.name);
                        JOptionPane.showMessageDialog(this, "Đã trao " + qty + "x " + item.template.name + " vào hành trang " + pl.name + " thành công!");
                    } else {
                        database.daos.GiftDataDAO.Reward rew = new database.daos.GiftDataDAO.Reward(tempId, qty);
                        rew.options = optionPairs;
                        java.util.List<database.daos.GiftDataDAO.Reward> rewards = new java.util.ArrayList<>();
                        rewards.add(rew);
                        services.func.GiftMailService.gI().deliver(playerId, "Hệ Thống", "Vật Phẩm Từ Admin", "Vật phẩm gửi vào hòm thư do hành trang đầy", rewards, 30);
                        JOptionPane.showMessageDialog(this, "Hành trang đầy! Đã chuyển " + qty + "x " + item.template.name + " vào Hòm Thư thành công!");
                    }
                } else {
                    database.daos.GiftDataDAO.Reward rew = new database.daos.GiftDataDAO.Reward(tempId, qty);
                    rew.options = optionPairs;
                    java.util.List<database.daos.GiftDataDAO.Reward> rewards = new java.util.ArrayList<>();
                    rewards.add(rew);
                    int mailId = services.func.GiftMailService.gI().deliver(playerId, "Hệ Thống", "Vật Phẩm Từ Admin", "Ban Quản Trị đã gửi cho bạn " + qty + "x " + item.template.name, rewards, 30);
                    JOptionPane.showMessageDialog(this, "Người chơi đang offline. Đã gửi " + qty + "x " + item.template.name + " vào Hòm Thư (Mail ID: " + mailId + ") thành công!");
                }

                Logger.fileLog("buff_item_admin", "BUFF ITEM OK | player=" + playerName + " (id=" + playerId + ")"
                        + " | itemId=" + tempId + " | qty=" + qty
                        + " | operator=" + System.getProperty("user.name"));
                d.dispose();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });
        d.add(btn); d.setVisible(true);
    }

    private long countItemTotal(String... jsonLists) {
        long total = 0;
        for (String json : jsonLists) {
            try {
                if (json == null || json.isEmpty()) continue;
                JsonElement parsed = new JsonParser().parse(json);
                if (!parsed.isJsonArray()) continue;
                JsonArray arr = parsed.getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonArray item;
                    if (e.isJsonPrimitive()) item = new JsonParser().parse(e.getAsString()).getAsJsonArray();
                    else item = e.getAsJsonArray();
                    if (item.size() >= 2 && item.get(0).getAsInt() == 1705) total += item.get(1).getAsLong(); 
                }
            } catch (Exception e) {}
        }
        return total;
    }

    private void loadPlayersFromDB(String keyword) {
        new Thread(() -> {
            SwingUtilities.invokeLater(() -> {
                model.setRowCount(0);
                inventoryCache.clear();
                modifiedPlayerIds.clear();
                btnQuickSave.setEnabled(false);
                btnQuickSave.setText("Lưu thay đổi (Ctrl+S)");
                btnQuickSave.setBackground(TEXT_MUTED);
            });
            
            // player không có cột 'power' (sức mạnh nằm trong data_point JSON) — bỏ khỏi truy vấn
            String sql = "SELECT p.id, p.head, p.name, p.clan_id, p.data_inventory, p.items_bag, p.items_box, a.active, a.ban FROM player p LEFT JOIN account a ON p.account_id = a.id ";
            if (!keyword.isEmpty()) sql += "WHERE p.name LIKE '%" + keyword + "%' ";
            sql += "ORDER BY p.id ASC LIMIT 50";
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    int pid = rs.getInt("id");
                    Vector<Object> row = new Vector<>();
                    row.add((long)pid);
                    row.add(drawHeadIcon(rs.getInt("head"))); 
                    row.add(rs.getString("name"));
                    row.add("—"); // power không có cột riêng trong schema này (nằm trong data_point)
                    row.add(getClanName(rs.getInt("clan_id")));
                    
                    String rawInv = rs.getString("data_inventory");
                    inventoryCache.put(pid, rawInv);
                    
                    try {
                        JsonArray inv = new JsonParser().parse(rawInv).getAsJsonArray();
                        row.add(inv.get(0).getAsLong()); 
                        row.add(inv.get(1).getAsLong()); 
                    } catch (Exception e) { row.add(0L); row.add(0L); }
                    
                    row.add(countItemTotal(rs.getString("items_bag"), rs.getString("items_box"))); 
                    row.add(rs.getInt("active") == 1 ? "Đã kích hoạt" : "Chưa kích hoạt");
                    row.add(rs.getInt("ban") == 1 ? "Bị chặn (Block)" : "Bình thường");
                    
                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }

    private void saveModifiedRows() {
        if (modifiedPlayerIds.isEmpty()) return;
        int confirm = JOptionPane.showConfirmDialog(this, "Bạn có chắc muốn lưu " + modifiedPlayerIds.size() + " tài khoản đã sửa? Thay đổi sẽ ảnh hưởng ngay đến server!", "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        new Thread(() -> {
            try (Connection conn = getConnection()) {
                conn.setAutoCommit(false); 
                String updatePlayerSql = "UPDATE player SET data_inventory = ? WHERE id = ?";
                
                try (PreparedStatement psPlayer = conn.prepareStatement(updatePlayerSql)) {
                    
                    for (int i = 0; i < model.getRowCount(); i++) {
                        int pid = Integer.parseInt(model.getValueAt(i, 0).toString());
                        if (modifiedPlayerIds.contains(pid)) {
                            long newGold = Long.parseLong(model.getValueAt(i, 5).toString().replace(",", "").replace(".", ""));
                            long newGem = Long.parseLong(model.getValueAt(i, 6).toString().replace(",", "").replace(".", ""));
                            
                            String rawInv = inventoryCache.get(pid);
                            JsonArray invArr;
                            try { invArr = new JsonParser().parse(rawInv).getAsJsonArray(); } catch (Exception ex) { invArr = new JsonArray(); invArr.add(0); invArr.add(0); invArr.add(0); }
                            while (invArr.size() < 3) invArr.add(0);
                            
                            invArr.set(0, new JsonPrimitive(newGold));
                            invArr.set(1, new JsonPrimitive(newGem));
                            
                            psPlayer.setString(1, invArr.toString());
                            psPlayer.setInt(2, pid);
                            psPlayer.addBatch();
                            
                            models.player.Player onlinePl = server.Client.gI().getPlayer(pid);
                            if (onlinePl != null) {
                                onlinePl.inventory.gold = Math.min(models.player.Inventory.LIMIT_GOLD, newGold);
                                onlinePl.inventory.gem = (int) Math.min(Integer.MAX_VALUE, newGem);
                                services.Service.gI().sendMoney(onlinePl);
                            }
                        }
                    }
                    psPlayer.executeBatch();
                    conn.commit();
                    
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(this, "Đã lưu thành công và gửi lệnh cập nhật đến server!");
                        modifiedPlayerIds.clear();
                        btnQuickSave.setEnabled(false);
                        btnQuickSave.setBackground(TEXT_MUTED);
                        btnQuickSave.setText("Lưu thay đổi (Ctrl+S)");
                        loadPlayersFromDB(txtSearch.getText().trim());
                    });
                } catch (Exception ex) {
                    conn.rollback();
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi khi lưu: " + ex.getMessage()));
                }
            } catch (SQLException e) { e.printStackTrace(); }
        }).start();
    }

    private void openPlayerEditorDB(int playerId) {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chỉnh sửa Chi Tiết - ID: " + playerId, true);
        d.setSize(1200, 800);
        d.setLocationRelativeTo(null);
        d.setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_BODY_B);
        d.getContentPane().setBackground(BG);
        
        Map<String, Component> inputs = new HashMap<>();
        Map<String, String> originalData = new HashMap<>();
        
        JRootPane rootPane = d.getRootPane();
        InputMap im = rootPane.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = rootPane.getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "saveDetail");
        
        new Thread(() -> {
            String query = "SELECT p.*, a.active, a.ban FROM player p LEFT JOIN account a ON p.account_id = a.id WHERE p.id = " + playerId;
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next()) {
                    int accountId = rs.getInt("account_id");
                    originalData.put("data_inventory", rs.getString("data_inventory"));
                    originalData.put("data_point", rs.getString("data_point"));
                    originalData.put("items_body", rs.getString("items_body"));
                    originalData.put("items_bag", rs.getString("items_bag"));
                    originalData.put("items_box", rs.getString("items_box"));
                    originalData.put("pet", rs.getString("pet"));
                    
                    originalData.put("data_task", rs.getString("data_task"));
                    originalData.put("data_side_task", rs.getString("data_side_task"));
                    originalData.put("data_clan_task", rs.getString("data_clan_task"));
                    originalData.put("data_kol_task", rs.getString("data_KOL")); // cột thật là data_KOL

                    // Ở schema này data_danh_hieu là bộ đếm nhiệm vụ danh hiệu hằng ngày (mảng số),
                    // KHÔNG phải danh sách badge → để trống, tránh crash và tránh ghi đè khi lưu.
                    originalData.put("dataBadges", "[]");
                    
                    JPanel pMainInfo = new JPanel(new GridBagLayout());
                    pMainInfo.setBackground(BG);
                    pMainInfo.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));
                    GridBagConstraints g = new GridBagConstraints();
                    g.fill = GridBagConstraints.HORIZONTAL; g.insets = new Insets(SP_XS, SP_XS, SP_XS, SP_XS);

                    // Đã bỏ phần thông tin tài khoản

                    JPanel pCharBody = new JPanel();
                    pCharBody.setLayout(new BoxLayout(pCharBody, BoxLayout.Y_AXIS));
                    addLabelInput(pCharBody, "Tên:", rs.getString("name"), "name", inputs);
                    // power không có cột riêng — lấy từ data_point[1]
                    addLabelInput(pCharBody, "Sức mạnh:", getJsonVal(new JsonParser().parse(originalData.get("data_point")).getAsJsonArray(), 1), "power", inputs);
                    addLabelInput(pCharBody, "Head Part ID:", String.valueOf(rs.getInt("head")), "head", inputs);
                    JPanel pChar = sectionCard("Thông tin Nhân vật", pCharBody);

                    JPanel pPointBody = new JPanel(new GridLayout(0, 2, SP_SM, SP_SM));
                    JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                    addLabelInputGrid(pPointBody, "Tiềm năng:", getJsonVal(point, 2), "tiemnang", inputs);
                    addLabelInputGrid(pPointBody, "HP Gốc:", getJsonVal(point, 5), "hpg", inputs);
                    addLabelInputGrid(pPointBody, "KI Gốc:", getJsonVal(point, 6), "mpg", inputs);
                    addLabelInputGrid(pPointBody, "Sức đánh:", getJsonVal(point, 7), "dameg", inputs);
                    addLabelInputGrid(pPointBody, "Giáp:", getJsonVal(point, 8), "defg", inputs);
                    addLabelInputGrid(pPointBody, "Chí mạng:", getJsonVal(point, 9), "critg", inputs);
                    JPanel pPoint = sectionCard("Chỉ số & Tiềm năng", pPointBody);

                    JPanel pAssetBody = new JPanel();
                    pAssetBody.setLayout(new BoxLayout(pAssetBody, BoxLayout.Y_AXIS));
                    JsonArray inv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
                    addLabelInput(pAssetBody, "Vàng:", inv.get(0).getAsString(), "gold", inputs);
                    addLabelInput(pAssetBody, "Ngọc xanh:", inv.get(1).getAsString(), "gem", inputs);
                    addLabelInput(pAssetBody, "Hồng ngọc:", inv.size()>2?inv.get(2).getAsString():"0", "ruby", inputs);
                    JPanel pAsset = sectionCard("Tài sản", pAssetBody);

                    g.gridx=0; g.gridy=0; g.weightx=1.0; pMainInfo.add(pChar, g);
                    g.gridy=1; pMainInfo.add(pPoint, g);
                    g.gridy=2; g.weighty=1.0; g.anchor=GridBagConstraints.NORTH; pMainInfo.add(pAsset, g);

                    JTabbedPane tabItems = new JTabbedPane();
                    DefaultTableModel mBody = createItemModel();
                    DefaultTableModel mBag = createItemModel();
                    DefaultTableModel mBox = createItemModel();
                    
                    loadItemsToModel(originalData.get("items_body"), mBody);
                    loadItemsToModel(originalData.get("items_bag"), mBag);
                    loadItemsToModel(originalData.get("items_box"), mBox);
                    
                    tabItems.addTab("Đồ đang mặc", createItemPanel(mBody, d));
                    tabItems.addTab("Hành trang", createItemPanel(mBag, d));
                    tabItems.addTab("Rương đồ", createItemPanel(mBox, d));

                    JPanel pPet = new JPanel(new BorderLayout());
                    pPet.setBackground(BG);
                    JPanel pPetContent = new JPanel(new GridBagLayout());
                    pPetContent.setBackground(BG);
                    pPetContent.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));
                    JScrollPane petScroll = plainScroll(pPetContent);
                    pPet.add(petScroll, BorderLayout.CENTER);
                    
                    String petStr = rs.getString("pet");
                    if(petStr != null && !petStr.equals("[]") && !petStr.isEmpty()) {
                        try {
                            JsonArray petArr = new JsonParser().parse(petStr).getAsJsonArray();
                            if(petArr.size() > 1) {
                                String infoStr = petArr.get(0).getAsString(); 
                                JsonArray infoArr = new JsonParser().parse(infoStr).getAsJsonArray();
                                String pointStr = petArr.get(1).getAsString();
                                JsonArray pointArr = new JsonParser().parse(pointStr).getAsJsonArray();

                                GridBagConstraints gp = new GridBagConstraints();
                                gp.fill = GridBagConstraints.HORIZONTAL; gp.weightx=1.0; gp.insets = new Insets(SP_XS,SP_XS,SP_XS,SP_XS); gp.gridx=0; gp.gridy=0;

                                JPanel pPetInfoBody = new JPanel();
                                pPetInfoBody.setLayout(new BoxLayout(pPetInfoBody, BoxLayout.Y_AXIS));
                                JComboBox<String> cbPetType = new JComboBox<>(new String[]{"0 - Mabu", "1 - Fide", "2 - Cadic", "3 - Pic", "4 - Quy lão"});
                                cbPetType.setEditable(true); cbPetType.setSelectedItem(infoArr.get(0).getAsString());
                                inputs.put("pet_type", cbPetType);
                                JPanel pT = new JPanel(new BorderLayout(SP_SM, 0)); pT.setOpaque(false); pT.setBorder(new EmptyBorder(SP_XS, 0, SP_XS, 0));
                                JLabel lblPetType = ServerGuiUtils.label("Loại Đệ:", FONT_SMALL_B, TEXT_SECONDARY);
                                lblPetType.setPreferredSize(new Dimension(100, 25));
                                pT.add(lblPetType, BorderLayout.WEST); pT.add(cbPetType); pPetInfoBody.add(pT);

                                JComboBox<String> cbPetGender = new JComboBox<>(new String[]{"0 - Trái đất", "1 - Namếc", "2 - Xayda"});
                                cbPetGender.setSelectedIndex(infoArr.get(1).getAsInt());
                                inputs.put("pet_gender", cbPetGender);
                                JPanel pG = new JPanel(new BorderLayout(SP_SM, 0)); pG.setOpaque(false); pG.setBorder(new EmptyBorder(SP_XS, 0, SP_XS, 0));
                                JLabel lblPetGender = ServerGuiUtils.label("Hệ:", FONT_SMALL_B, TEXT_SECONDARY);
                                lblPetGender.setPreferredSize(new Dimension(100, 25));
                                pG.add(lblPetGender, BorderLayout.WEST); pG.add(cbPetGender); pPetInfoBody.add(pG);

                                addLabelInput(pPetInfoBody, "Tên Đệ tử:", infoArr.get(2).getAsString(), "pet_name", inputs);

                                JComboBox<String> cbPetStatus = new JComboBox<>(new String[]{"0 - Đi theo", "1 - Bảo vệ", "2 - Tấn công", "3 - Về nhà", "4 - Hợp thể"});
                                try { cbPetStatus.setSelectedIndex(infoArr.get(5).getAsInt()); } catch(Exception ex) {}
                                inputs.put("pet_status", cbPetStatus);
                                JPanel pS = new JPanel(new BorderLayout(SP_SM, 0)); pS.setOpaque(false); pS.setBorder(new EmptyBorder(SP_XS, 0, SP_XS, 0));
                                JLabel lblPetStatus = ServerGuiUtils.label("Trạng thái:", FONT_SMALL_B, TEXT_SECONDARY);
                                lblPetStatus.setPreferredSize(new Dimension(100, 25));
                                pS.add(lblPetStatus, BorderLayout.WEST); pS.add(cbPetStatus); pPetInfoBody.add(pS);
                                JPanel pPetInfo = sectionCard("Thông tin cơ bản", pPetInfoBody);
                                pPetContent.add(pPetInfo, gp);

                                gp.gridy=1;
                                JPanel pPetStatsBody = new JPanel(new GridLayout(0, 2, SP_MD, SP_MD));
                                addLabelInputGrid(pPetStatsBody, "Sức mạnh:", getJsonVal(pointArr, 1), "pet_power", inputs);
                                addLabelInputGrid(pPetStatsBody, "Tiềm năng:", getJsonVal(pointArr, 2), "pet_tiemnang", inputs);
                                addLabelInputGrid(pPetStatsBody, "HP Gốc:", getJsonVal(pointArr, 5), "pet_hpg", inputs);
                                addLabelInputGrid(pPetStatsBody, "KI Gốc:", getJsonVal(pointArr, 6), "pet_mpg", inputs);
                                addLabelInputGrid(pPetStatsBody, "Sức đánh:", getJsonVal(pointArr, 7), "pet_dameg", inputs);
                                addLabelInputGrid(pPetStatsBody, "Giáp:", getJsonVal(pointArr, 8), "pet_defg", inputs);
                                addLabelInputGrid(pPetStatsBody, "Chí mạng:", getJsonVal(pointArr, 9), "pet_critg", inputs);
                                JPanel pPetStats = sectionCard("Chỉ số Sức Mạnh (Point)", pPetStatsBody);
                                pPetContent.add(pPetStats, gp);
                            }
                        } catch(Exception ex) {
                            JLabel lblPetErr = new JLabel("Lỗi đọc đệ tử: " + ex.getMessage());
                            lblPetErr.setFont(FONT_BODY);
                            lblPetErr.setForeground(DANGER);
                            pPet.add(lblPetErr);
                        }
                    } else {
                        JLabel lblNoPet = new JLabel("Không có đệ tử.", SwingConstants.CENTER);
                        lblNoPet.setFont(FONT_BODY);
                        lblNoPet.setForeground(TEXT_MUTED);
                        pPet.add(lblNoPet);
                    }

                    JPanel pTasks = createTaskPanel(originalData, inputs);
                    
                    DefaultTableModel mBadges = new DefaultTableModel(new String[]{"ID Badges", "Icon", "Tên Danh Hiệu", "Chỉ số (Options)", "Thời gian hết hạn (Long)", "Ngày còn lại", "Đang dùng"}, 0) {
                         @Override public boolean isCellEditable(int row, int column) { return column == 0 || column == 4 || column == 6; }
                         @Override public Class<?> getColumnClass(int columnIndex) {
                             if(columnIndex == 1) return ImageIcon.class;
                             if(columnIndex == 6) return Boolean.class;
                             return Object.class;
                         }
                    };
                    JPanel pBadges = createBadgesPanel(originalData.get("dataBadges"), mBadges, d);

                    tabs.addTab("Thông tin chung", plainScroll(pMainInfo));
                    tabs.addTab("Vật phẩm", tabItems);
                    tabs.addTab("Đệ tử", pPet);
                    tabs.addTab("Nhiệm vụ", plainScroll(pTasks));
                    tabs.addTab("Danh hiệu (Badges)", pBadges);

                    d.add(tabs, BorderLayout.CENTER);

                    JPanel pBtn = new JPanel();
                    pBtn.setOpaque(false);
                    pBtn.setBorder(new EmptyBorder(SP_MD, 0, SP_MD, 0));
                    JButton btnSave = button("LƯU DỮ LIỆU & CẬP NHẬT SERVER", ButtonVariant.PRIMARY, "save");
                    btnSave.addActionListener(ev -> savePlayerDB(playerId, accountId, inputs, mBody, mBag, mBox, mBadges, originalData, d));
                    pBtn.add(btnSave); d.add(pBtn, BorderLayout.SOUTH);
                    
                    am.put("saveDetail", new AbstractAction() {
                        @Override
                        public void actionPerformed(ActionEvent e) {
                            btnSave.doClick();
                        }
                    });
                    
                    SwingUtilities.invokeLater(() -> d.setVisible(true));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();
    }
    
    private JPanel createBadgesPanel(String jsonBadges, DefaultTableModel model, JDialog parent) {
        JPanel p = new JPanel(new BorderLayout(SP_SM, SP_SM));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        model.setRowCount(0);

        if (jsonBadges != null && !jsonBadges.isEmpty() && !jsonBadges.equals("[]")) {
            try {
                JsonArray arr = new JsonParser().parse(jsonBadges).getAsJsonArray();
                for (JsonElement e : arr) {
                    JsonObject obj = e.getAsJsonObject();
                    
                    int id = obj.get("idBadGes").getAsInt();
                    long time = obj.get("timeofUseBadges").getAsLong();
                    boolean isUse = obj.get("isUse").getAsBoolean();
                    
                    long timeLeft = time - System.currentTimeMillis();
                    long daysLeft = timeLeft / (24 * 60 * 60 * 1000L);
                    
                    BadgeTemplate temp = badgeTemplateMap.get(id);
                    
                    String name;
                    String optionsReadable;
                    ImageIcon icon;

                    if (temp != null) {
                        name = temp.name;
                        optionsReadable = parseBadgeOptions(temp.optionsJson);
                        icon = loadIconRaw(temp.iconId); 
                    } else {
                        name = "Unknown [" + id + "]";
                        optionsReadable = "";
                        icon = null;
                    }
                    
                    model.addRow(new Object[]{id, icon, name, optionsReadable, time, (daysLeft > 0 ? daysLeft + " ngày" : "Hết hạn"), isUse});
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        JTable t = new JTable(model);
        JScrollPane spBadges = styleTable(t);
        styleIconColumn(t, 1);
        t.getColumnModel().getColumn(6).setCellRenderer(null); // giữ checkbox mặc định cho cột "Đang dùng"
        t.getColumnModel().getColumn(0).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        t.getColumnModel().getColumn(3).setPreferredWidth(250);

        JPanel pTool = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pTool.setOpaque(false);
        JButton btnAdd = button("Thêm Badge", ButtonVariant.SECONDARY, "plus");
        JButton btnDel = button("Xóa Badge", ButtonVariant.DANGER, "broom");

        btnAdd.addActionListener(e -> openBadgeAddDialog(model, parent));
        btnDel.addActionListener(e -> {
            int r = t.getSelectedRow();
            if(r != -1) model.removeRow(r);
        });

        pTool.add(btnAdd); pTool.add(btnDel);
        p.add(pTool, BorderLayout.NORTH);
        p.add(spBadges, BorderLayout.CENTER);
        return p;
    }
    
    private void openBadgeAddDialog(DefaultTableModel model, JDialog parent) {
        JDialog d = new JDialog(parent, "Thêm Danh Hiệu", true);
        d.setSize(600, 500); d.setLayout(new BorderLayout()); d.setLocationRelativeTo(parent);
        
        d.getContentPane().setBackground(BG);
        JPanel pTop = new JPanel(new BorderLayout(SP_SM, SP_SM));
        pTop.setOpaque(false);
        pTop.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_SM, SP_MD));
        JTextField txtSearch = searchField("Nhập tên hoặc ID danh hiệu...");
        pTop.add(ServerGuiUtils.label("Tìm kiếm: ", FONT_SMALL_B, TEXT_SECONDARY), BorderLayout.WEST);
        pTop.add(txtSearch, BorderLayout.CENTER);
        
        DefaultTableModel searchModel = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Danh Hiệu", "Options"}, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int columnIndex) {
                if(columnIndex == 1) return ImageIcon.class;
                return Object.class;
            }
        };
        
        for(BadgeTemplate b : badgeTemplateMap.values()) {
            searchModel.addRow(new Object[]{b.id, loadIconRaw(b.iconId), b.name, parseBadgeOptions(b.optionsJson)});
        }
        
        JTable t = new JTable(searchModel);
        JScrollPane spSearch = styleTable(t);
        styleIconColumn(t, 1);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(searchModel);
        t.setRowSorter(sorter);
        
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filter(); }
            public void removeUpdate(DocumentEvent e) { filter(); }
            public void changedUpdate(DocumentEvent e) { filter(); }
            void filter() {
                String text = txtSearch.getText().trim();
                if(text.isEmpty()) sorter.setRowFilter(null);
                else sorter.setRowFilter(RowFilter.regexFilter("(?i)" + Pattern.quote(text)));
            }
        });
        
        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if(r != -1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        int id = (int) searchModel.getValueAt(modelRow, 0);
                        ImageIcon icon = (ImageIcon) searchModel.getValueAt(modelRow, 1);
                        String name = (String) searchModel.getValueAt(modelRow, 2);
                        String opts = (String) searchModel.getValueAt(modelRow, 3);
                        
                        String dayStr = JOptionPane.showInputDialog(d, "Nhập số ngày sử dụng:", "30");
                        if(dayStr != null) {
                            try {
                                int days = Integer.parseInt(dayStr);
                                long time = System.currentTimeMillis() + (long)days * 24 * 60 * 60 * 1000L;
                                model.addRow(new Object[]{id, icon, name, opts, time, days + " ngày", false});
                                d.dispose();
                            } catch(Exception ex) { JOptionPane.showMessageDialog(d, "Lỗi nhập ngày!"); }
                        }
                    }
                }
            }
        });
        
        d.add(pTop, BorderLayout.NORTH);
        d.add(spSearch, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private String parseBadgeOptions(String jsonOpt) {
        if(jsonOpt == null || jsonOpt.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        try {
            JsonArray arr = new JsonParser().parse(jsonOpt).getAsJsonArray();
            for(JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                int id = obj.get("id").getAsInt();
                int param = obj.get("param").getAsInt();
                sb.append(formatOption(id, param)).append("; ");
            }
        } catch(Exception e) { return jsonOpt; }
        return sb.toString();
    }

    private JPanel createTaskPanel(Map<String, String> originalData, Map<String, Component> inputs) {
        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBackground(BG);
        panel.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        JPanel mainTaskBody = new JPanel(new GridLayout(0, 2, SP_SM, SP_SM));
        
        JsonArray dataTask = new JsonArray();
        try {
            String taskStr = originalData.get("data_task");
            if (taskStr != null && !taskStr.isEmpty()) {
                dataTask = new JsonParser().parse(taskStr).getAsJsonArray();
            }
        } catch (Exception e) {
            dataTask = new JsonArray();
        }
        while (dataTask.size() < 4) dataTask.add(0);
        
        JLabel lblMainTaskId = ServerGuiUtils.label("ID Nhiệm vụ:", FONT_SMALL_B, TEXT_SECONDARY);
        JComboBox<String> cbMainTaskId = new JComboBox<>();
        cbMainTaskId.addItem("-1 - Không có");
        for (Map.Entry<Integer, String> entry : taskMainTemplateMap.entrySet()) {
            cbMainTaskId.addItem(entry.getKey() + " - " + entry.getValue());
        }
        int currentMainTaskId = dataTask.get(0).getAsInt();
        for (int i = 0; i < cbMainTaskId.getItemCount(); i++) {
            String item = cbMainTaskId.getItemAt(i);
            if (item.startsWith(currentMainTaskId + " - ")) {
                cbMainTaskId.setSelectedIndex(i);
                break;
            }
        }
        inputs.put("mainTaskId", cbMainTaskId);
        
        JLabel lblMainTaskIndex = ServerGuiUtils.label("Index:", FONT_SMALL_B, TEXT_SECONDARY);
        JTextField txtMainTaskIndex = new JTextField(dataTask.get(1).getAsString());
        txtMainTaskIndex.setFont(FONT_BODY);
        addUndoRedo(txtMainTaskIndex);
        inputs.put("mainTaskIndex", txtMainTaskIndex);

        JLabel lblMainTaskCount = ServerGuiUtils.label("Count:", FONT_SMALL_B, TEXT_SECONDARY);
        JTextField txtMainTaskCount = new JTextField(dataTask.get(2).getAsString());
        txtMainTaskCount.setFont(FONT_BODY);
        addUndoRedo(txtMainTaskCount);
        inputs.put("mainTaskCount", txtMainTaskCount);

        JLabel lblMainTaskLastTime = ServerGuiUtils.label("Last Time:", FONT_SMALL_B, TEXT_SECONDARY);
        JTextField txtMainTaskLastTime = new JTextField(dataTask.get(3).getAsString());
        txtMainTaskLastTime.setFont(FONT_BODY);
        addUndoRedo(txtMainTaskLastTime);
        inputs.put("mainTaskLastTime", txtMainTaskLastTime);

        mainTaskBody.add(lblMainTaskId); mainTaskBody.add(cbMainTaskId);
        mainTaskBody.add(lblMainTaskIndex); mainTaskBody.add(txtMainTaskIndex);
        mainTaskBody.add(lblMainTaskCount); mainTaskBody.add(txtMainTaskCount);
        mainTaskBody.add(lblMainTaskLastTime); mainTaskBody.add(txtMainTaskLastTime);
        JPanel mainTaskPanel = sectionCard("Nhiệm vụ chính tuyến", mainTaskBody);

        JPanel sideTaskBody = new JPanel(new GridLayout(0, 4, SP_SM, SP_SM));
        
        JsonArray sideTask = new JsonArray();
        try {
            String sideStr = originalData.get("data_side_task");
            if (sideStr != null && !sideStr.isEmpty()) {
                sideTask = new JsonParser().parse(sideStr).getAsJsonArray();
            }
        } catch (Exception e) {
            sideTask = new JsonArray();
        }
        while (sideTask.size() < 6) sideTask.add(0);
        
        for (int i = 0; i < 3; i++) {
            int idx = i * 2;
            JLabel lblSideId = ServerGuiUtils.label("Nhiệm vụ " + (i + 1) + " ID:", FONT_SMALL_B, TEXT_SECONDARY);
            JComboBox<String> cbSideId = new JComboBox<>();
            cbSideId.addItem("-1 - Không có");
            for (Map.Entry<Integer, String> entry : sideTaskTemplateMap.entrySet()) {
                cbSideId.addItem(entry.getKey() + " - " + entry.getValue());
            }
            int currentSideId = sideTask.get(idx).getAsInt();
            for (int j = 0; j < cbSideId.getItemCount(); j++) {
                String item = cbSideId.getItemAt(j);
                if (item.startsWith(currentSideId + " - ")) {
                    cbSideId.setSelectedIndex(j);
                    break;
                }
            }
            inputs.put("sideTaskId_" + i, cbSideId);

            JLabel lblSideCount = ServerGuiUtils.label("Count:", FONT_SMALL_B, TEXT_SECONDARY);
            JTextField txtSideCount = new JTextField(sideTask.get(idx + 1).getAsString());
            txtSideCount.setFont(FONT_BODY);
            addUndoRedo(txtSideCount);
            inputs.put("sideTaskCount_" + i, txtSideCount);

            sideTaskBody.add(lblSideId); sideTaskBody.add(cbSideId);
            sideTaskBody.add(lblSideCount); sideTaskBody.add(txtSideCount);
        }
        JPanel sideTaskPanel = sectionCard("Nhiệm vụ hàng ngày", sideTaskBody);

        JPanel clanTaskBody = new JPanel(new GridLayout(0, 4, SP_SM, SP_SM));
        
        JsonArray clanTask = new JsonArray();
        try {
            String clanStr = originalData.get("data_clan_task");
            if (clanStr != null && !clanStr.isEmpty()) {
                clanTask = new JsonParser().parse(clanStr).getAsJsonArray();
            }
        } catch (Exception e) {
            clanTask = new JsonArray();
        }
        while (clanTask.size() < 6) clanTask.add(0);
        
        for (int i = 0; i < 3; i++) {
            int idx = i * 2;
            JLabel lblClanId = ServerGuiUtils.label("Nhiệm vụ " + (i + 1) + " ID:", FONT_SMALL_B, TEXT_SECONDARY);
            JComboBox<String> cbClanId = new JComboBox<>();
            cbClanId.addItem("-1 - Không có");
            for (Map.Entry<Integer, String> entry : clanTaskTemplateMap.entrySet()) {
                cbClanId.addItem(entry.getKey() + " - " + entry.getValue());
            }
            int currentClanId = clanTask.get(idx).getAsInt();
            for (int j = 0; j < cbClanId.getItemCount(); j++) {
                String item = cbClanId.getItemAt(j);
                if (item.startsWith(currentClanId + " - ")) {
                    cbClanId.setSelectedIndex(j);
                    break;
                }
            }
            inputs.put("clanTaskId_" + i, cbClanId);

            JLabel lblClanCount = ServerGuiUtils.label("Count:", FONT_SMALL_B, TEXT_SECONDARY);
            JTextField txtClanCount = new JTextField(clanTask.get(idx + 1).getAsString());
            txtClanCount.setFont(FONT_BODY);
            addUndoRedo(txtClanCount);
            inputs.put("clanTaskCount_" + i, txtClanCount);

            clanTaskBody.add(lblClanId); clanTaskBody.add(cbClanId);
            clanTaskBody.add(lblClanCount); clanTaskBody.add(txtClanCount);
        }
        JPanel clanTaskPanel = sectionCard("Nhiệm vụ clan", clanTaskBody);

        JPanel kolTaskBody = new JPanel(new GridLayout(0, 2, SP_SM, SP_SM));
        
        JsonArray kolTask = new JsonArray();
        try {
            String kolStr = originalData.get("data_kol_task");
            if (kolStr != null && !kolStr.isEmpty()) {
                kolTask = new JsonParser().parse(kolStr).getAsJsonArray();
            }
        } catch (Exception e) {
            kolTask = new JsonArray();
        }
        while (kolTask.size() < 2) kolTask.add(0);
        
        JLabel lblKolTaskId = ServerGuiUtils.label("ID Nhiệm vụ KOL:", FONT_SMALL_B, TEXT_SECONDARY);
        JComboBox<String> cbKolTaskId = new JComboBox<>();
        cbKolTaskId.addItem("-1 - Không có");
        for (Map.Entry<Integer, String> entry : kolTaskTemplateMap.entrySet()) {
            cbKolTaskId.addItem(entry.getKey() + " - " + entry.getValue());
        }
        int currentKolId = kolTask.get(0).getAsInt();
        for (int i = 0; i < cbKolTaskId.getItemCount(); i++) {
            String item = cbKolTaskId.getItemAt(i);
            if (item.startsWith(currentKolId + " - ")) {
                cbKolTaskId.setSelectedIndex(i);
                break;
            }
        }
        inputs.put("kolTaskId", cbKolTaskId);
        
        JLabel lblKolTaskCount = ServerGuiUtils.label("Count:", FONT_SMALL_B, TEXT_SECONDARY);
        JTextField txtKolTaskCount = new JTextField(kolTask.get(1).getAsString());
        txtKolTaskCount.setFont(FONT_BODY);
        addUndoRedo(txtKolTaskCount);
        inputs.put("kolTaskCount", txtKolTaskCount);

        kolTaskBody.add(lblKolTaskId); kolTaskBody.add(cbKolTaskId);
        kolTaskBody.add(lblKolTaskCount); kolTaskBody.add(txtKolTaskCount);
        JPanel kolTaskPanel = sectionCard("Nhiệm vụ KOL", kolTaskBody);

        panel.add(mainTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, SP_SM)));
        panel.add(sideTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, SP_SM)));
        panel.add(clanTaskPanel);
        panel.add(Box.createRigidArea(new Dimension(0, SP_SM)));
        panel.add(kolTaskPanel);

        return panel;
    }

    private String getJsonVal(JsonArray arr, int index) {
        if (index < arr.size()) return arr.get(index).getAsString();
        return "0";
    }

    private void addLabelInput(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(SP_MD, SP_XS));
        row.setOpaque(false);
        row.setBorder(new EmptyBorder(SP_XS, 0, SP_XS, 0));
        JLabel lbl = ServerGuiUtils.label(label, FONT_SMALL_B, TEXT_SECONDARY);
        lbl.setPreferredSize(new Dimension(100, 25));
        JTextField txt = new JTextField(value);
        txt.setFont(FONT_BODY);
        addUndoRedo(txt);
        row.add(lbl, BorderLayout.WEST);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }

    private void addLabelInputGrid(JPanel p, String label, String value, String key, Map<String, Component> map) {
        JPanel row = new JPanel(new BorderLayout(SP_XS, 2));
        row.setOpaque(false);
        JLabel lbl = ServerGuiUtils.label(label, FONT_SMALL_B, TEXT_SECONDARY);
        JTextField txt = new JTextField(value);
        txt.setFont(FONT_BODY);
        addUndoRedo(txt);
        row.add(lbl, BorderLayout.NORTH);
        row.add(txt, BorderLayout.CENTER);
        p.add(row);
        map.put(key, txt);
    }

    private JPanel createItemPanel(DefaultTableModel model, JDialog parent) {
        JPanel p = new JPanel(new BorderLayout(0, SP_SM));
        p.setBackground(BG);
        p.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));
        JTable t = new JTable(model);
        JScrollPane spItems = styleTable(t);
        styleIconColumn(t, 1);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(150);
        t.getColumnModel().getColumn(4).setPreferredWidth(300);
        // Ẩn 2 cột kỹ thuật cuối (createTime, slot) khỏi bảng hiển thị nhưng vẫn giữ trong model,
        // để khi lưu vẫn bảo toàn thời gian tạo và đúng vị trí ô của từng item.
        while (t.getColumnModel().getColumnCount() > 6) {
            t.removeColumn(t.getColumnModel().getColumn(t.getColumnModel().getColumnCount() - 1));
        }

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if(e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if(r != -1) openItemDetailEditor(model, r, parent);
                }
            }
        });

        JPanel tool = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        tool.setOpaque(false);
        JButton btnAdd = button("Thêm Item", ButtonVariant.PRIMARY, "plus");
        JButton btnDel = button("Xóa Item", ButtonVariant.DANGER, "broom");
        btnAdd.addActionListener(e -> openItemAddDialog(model, parent));
        btnDel.addActionListener(e -> { if(t.getSelectedRow()!=-1) model.removeRow(t.getSelectedRow()); });
        tool.add(btnAdd); tool.add(btnDel);
        p.add(tool, BorderLayout.NORTH);
        p.add(spItems, BorderLayout.CENTER);
        return p;
    }

    private void openItemDetailEditor(DefaultTableModel model, int row, JDialog parent) {
        JDialog d = new JDialog(parent, "Chỉnh sửa Vật phẩm", true);
        d.setSize(600, 500); d.setLocationRelativeTo(parent); d.setLayout(new BorderLayout());
        d.getContentPane().setBackground(BG);

        JPanel pTop = new JPanel(new GridBagLayout());
        pTop.setOpaque(false);
        pTop.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_XS, SP_MD));
        GridBagConstraints g = new GridBagConstraints(); g.insets = new Insets(SP_XS,SP_XS,SP_XS,SP_XS); g.fill = GridBagConstraints.HORIZONTAL;

        int currId = Integer.parseInt(model.getValueAt(row, 0).toString());
        String currName = model.getValueAt(row, 2).toString();
        int currQty = Integer.parseInt(model.getValueAt(row, 3).toString());
        String currOpt = model.getValueAt(row, 5).toString();

        JLabel lblIcon = new JLabel(getItemIcon(currId));
        JTextField txtId = new JTextField(String.valueOf(currId), 10); txtId.setFont(FONT_BODY); addUndoRedo(txtId);
        JLabel lblName = ServerGuiUtils.label(currName, FONT_BODY_B, PRIMARY);
        JTextField txtQty = new JTextField(String.valueOf(currQty), 10); txtQty.setFont(FONT_BODY); addUndoRedo(txtQty);
        JButton btnFind = button("", ButtonVariant.SECONDARY, "search");

        g.gridx=0; g.gridy=0; pTop.add(ServerGuiUtils.label("ID Item:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx=1; pTop.add(txtId, g);
        g.gridx=2; pTop.add(btnFind, g);

        g.gridx=0; g.gridy=1; pTop.add(ServerGuiUtils.label("Info:", FONT_SMALL_B, TEXT_SECONDARY), g);
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT)); pInfo.setOpaque(false); pInfo.add(lblIcon); pInfo.add(lblName);
        g.gridx=1; g.gridwidth=2; pTop.add(pInfo, g); g.gridwidth=1;

        g.gridx=0; g.gridy=2; pTop.add(ServerGuiUtils.label("Số lượng:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx=1; pTop.add(txtQty, g);

        txtId.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { u(); }
            public void removeUpdate(DocumentEvent e) { u(); }
            public void changedUpdate(DocumentEvent e) { u(); }
            void u() { try { 
                int id = Integer.parseInt(txtId.getText()); 
                lblName.setText(getItemName(id)); 
                lblIcon.setIcon(getItemIcon(id)); 
            } catch(Exception ex) {} }
        });

        btnFind.addActionListener(e -> {
             JDialog sd = new JDialog(d, "Tìm Item", true); sd.setSize(900, 600);
             sd.setLocationRelativeTo(d);
             sd.setLayout(new BorderLayout());
             sd.getContentPane().setBackground(BG);
             JPanel pFilter = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_SM));
             pFilter.setOpaque(false);
             pFilter.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_XS, SP_MD));
             JTextField st = searchField("Tìm theo tên hoặc ID...");
             st.setColumns(15);
             JComboBox<String> cbType = new JComboBox<>(new String[]{"- Tất cả -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada", "5 - Cải trang", "12 - Ngọc rồng", "27 - Vật phẩm"});
             JComboBox<String> cbGender = new JComboBox<>(new String[]{"- Tất cả -", "0 - Trái đất", "1 - Namếc", "2 - Xayda"});
             pFilter.add(ServerGuiUtils.label("Tên/ID:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(st);
             pFilter.add(ServerGuiUtils.label("Loại:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(cbType);
             pFilter.add(ServerGuiUtils.label("Hệ:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(cbGender);

             DefaultTableModel sm = new DefaultTableModel(new String[]{"ID", "Icon", "Name", "Type", "Gender"},0) {
                 public Class<?> getColumnClass(int c){return c==1?ImageIcon.class:Object.class;}
                 @Override public boolean isCellEditable(int r, int c) { return false; }
             };
             for(ItemData i : listAllItems) sm.addRow(new Object[]{i.id, getItemIcon(i.id), i.name, i.type, i.gender});
             JTable stab = new JTable(sm);
             JScrollPane spStab = styleTable(stab);
             styleIconColumn(stab, 1);
             stab.getColumnModel().getColumn(0).setPreferredWidth(50); stab.getColumnModel().getColumn(1).setPreferredWidth(40);
             stab.getColumnModel().getColumn(3).setMinWidth(0); stab.getColumnModel().getColumn(3).setMaxWidth(0);
             stab.getColumnModel().getColumn(4).setMinWidth(0); stab.getColumnModel().getColumn(4).setMaxWidth(0);
             
             TableRowSorter<DefaultTableModel> ss = new TableRowSorter<>(sm); stab.setRowSorter(ss);
             
             Runnable doFilter = () -> {
                String text = st.getText().trim();
                List<RowFilter<Object, Object>> filters = new ArrayList<>();
                if (!text.isEmpty()) {
                    try {
                        int id = Integer.parseInt(text);
                        List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                        orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, id, 0));
                        orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                        filters.add(RowFilter.orFilter(orFilters));
                    } catch (NumberFormatException ex) {
                        filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                    }
                }
                if (cbType.getSelectedIndex() > 0) try { filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]), 3)); } catch(Exception ex){}
                if (cbGender.getSelectedIndex() > 0) try { filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]), 4)); } catch(Exception ex){}
                if (filters.isEmpty()) ss.setRowFilter(null); else ss.setRowFilter(RowFilter.andFilter(filters));
             };
             st.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) {doFilter.run();} public void removeUpdate(DocumentEvent e) {doFilter.run();} public void changedUpdate(DocumentEvent e) {doFilter.run();} });
             cbType.addActionListener(e1->doFilter.run()); cbGender.addActionListener(e1->doFilter.run());

             stab.addMouseListener(new MouseAdapter(){
                 public void mouseClicked(MouseEvent e){
                     if(e.getClickCount()==2){
                         int viewRow = stab.getSelectedRow();
                         if(viewRow != -1) {
                             int modelRow = stab.convertRowIndexToModel(viewRow);
                             int mid = (int) sm.getValueAt(modelRow, 0);
                             txtId.setText(String.valueOf(mid)); 
                             sd.dispose(); 
                         }
                     }
                 }
             });
             sd.add(pFilter, BorderLayout.NORTH); sd.add(spStab, BorderLayout.CENTER); sd.setVisible(true);
        });

        String[] optCols = {"ID Option", "Param", "Mô tả"};
        DefaultTableModel optModel = new DefaultTableModel(optCols, 0);
        // currOpt có thể ở 1 trong 2 format:
        //   - Array of arrays: [[97,9999],[107,7]]                  (mới, từ btnSave bên dưới)
        //   - Array of strings: ["[97,9999]","[107,7]"]             (từ loadItemsToModel, format gốc của game)
        // Trước đây chỉ handle format 1 -> el.getAsJsonArray() throw -> bảng options trống ->
        // sau khi save bị xoá hết options. Giờ handle cả 2.
        try {
            JsonArray arr = new JsonParser().parse(currOpt).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonArray o;
                try {
                    if (el.isJsonArray()) {
                        // Format 1: [[oid, param], ...]
                        o = el.getAsJsonArray();
                    } else if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                        // Format 2: ["[oid,param]", ...] — parse the inner string as a JSON array
                        o = new JsonParser().parse(el.getAsString()).getAsJsonArray();
                    } else {
                        continue;
                    }
                    if (o.size() < 2) continue;
                    int oid = o.get(0).getAsInt();
                    int op  = o.get(1).getAsInt();
                    optModel.addRow(new Object[]{oid, op, formatOption(oid, op)});
                } catch (Exception perRowEx) {
                    // Bỏ qua row bị lỗi nhưng không bỏ cả options còn lại
                    System.err.println("openItemDetailEditor: bỏ qua option lỗi: " + el + " — " + perRowEx.getMessage());
                }
            }
        } catch(Exception ex) {
            System.err.println("openItemDetailEditor: không parse được currOpt = " + currOpt + " — " + ex.getMessage());
        }
        
        JTable optTable = new JTable(optModel);
        JScrollPane spOpts = styleTable(optTable);
        optModel.addTableModelListener(e -> {
            int r = e.getFirstRow(); int c = e.getColumn();
            if(r>=0 && r<optModel.getRowCount() && (c==0 || c==1)) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        int oid = Integer.parseInt(optModel.getValueAt(r, 0).toString());
                        int op = Integer.parseInt(optModel.getValueAt(r, 1).toString());
                        optModel.setValueAt(formatOption(oid, op), r, 2);
                    } catch(Exception ex){}
                });
            }
        });
        
        JPanel pOptTool = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pOptTool.setOpaque(false);
        JButton btnAddOpt = button("Thêm Opt", ButtonVariant.SECONDARY, "plus");
        JButton btnFindOpt = button("Tìm Opt", ButtonVariant.GHOST, "search");
        JButton btnDelOpt = button("Xóa Opt", ButtonVariant.DANGER, "broom");
        
        btnAddOpt.addActionListener(e -> optModel.addRow(new Object[]{0, 0, getOptionName(0)}));
        
        btnFindOpt.addActionListener(e -> {
             JDialog fod = new JDialog(d, "Tìm Option", true); fod.setSize(400, 500);
             fod.setLocationRelativeTo(d);
             fod.setLayout(new BorderLayout());
             fod.getContentPane().setBackground(BG);
             JTextField tf = searchField("Tìm option...");
             JPanel pTf = new JPanel(new BorderLayout());
             pTf.setOpaque(false);
             pTf.setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));
             pTf.add(tf, BorderLayout.CENTER);

             DefaultTableModel om = new DefaultTableModel(new String[]{"ID", "Name"},0) {
                 @Override public boolean isCellEditable(int r, int c) { return false; }
             };
             optionTemplateMap.forEach((k,v)->om.addRow(new Object[]{k,v}));
             JTable ot = new JTable(om);
             JScrollPane spOt = styleTable(ot);
             TableRowSorter<DefaultTableModel> os = new TableRowSorter<>(om); ot.setRowSorter(os);
             tf.getDocument().addDocumentListener(new DocumentListener() {
                 public void insertUpdate(DocumentEvent e) {f();} public void removeUpdate(DocumentEvent e) {f();} public void changedUpdate(DocumentEvent e) {f();}
                 void f() { String tx=tf.getText(); if(tx.isEmpty()) os.setRowFilter(null); else os.setRowFilter(RowFilter.regexFilter("(?i)"+Pattern.quote(tx))); }
             });
             ot.addMouseListener(new MouseAdapter(){
                 public void mouseClicked(MouseEvent e){
                     if(e.getClickCount()==2){
                         int viewRow = ot.getSelectedRow();
                         if(viewRow != -1) {
                             int modelRow = ot.convertRowIndexToModel(viewRow);
                             int oid = (int) om.getValueAt(modelRow, 0);
                             optModel.addRow(new Object[]{oid, 0, getOptionName(oid).replace("#", "0")});
                             fod.dispose(); 
                         }
                     }
                 }
             });
             fod.add(pTf, BorderLayout.NORTH); fod.add(spOt, BorderLayout.CENTER); fod.setVisible(true);
        });
        
        btnDelOpt.addActionListener(e -> { if(optTable.getSelectedRow()!=-1) optModel.removeRow(optTable.getSelectedRow()); });
        pOptTool.add(btnAddOpt); pOptTool.add(btnFindOpt); pOptTool.add(btnDelOpt);

        JPanel pCenter = new JPanel(new BorderLayout(0, SP_XS));
        pCenter.setOpaque(false);
        pCenter.setBorder(new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD));
        JPanel pOptNorth = new JPanel(new BorderLayout(0, SP_XS));
        pOptNorth.setOpaque(false);
        JLabel lblOptTitle = ServerGuiUtils.label("Options", FONT_H3, TEXT);
        lblOptTitle.setBorder(new EmptyBorder(0, 0, SP_XS, 0));
        pOptNorth.add(lblOptTitle, BorderLayout.NORTH);
        pOptNorth.add(pOptTool, BorderLayout.CENTER);
        pCenter.add(pOptNorth, BorderLayout.NORTH);
        pCenter.add(spOpts, BorderLayout.CENTER);

        JButton btnSave = button("Lưu thay đổi", ButtonVariant.SUCCESS, "save");
        btnSave.addActionListener(e -> {
            JsonArray newArr = new JsonArray();
            for(int i=0; i<optModel.getRowCount(); i++){
                JsonArray o = new JsonArray();
                o.add(Integer.parseInt(optModel.getValueAt(i,0).toString()));
                o.add(Integer.parseInt(optModel.getValueAt(i,1).toString()));
                newArr.add(o);
            }
            int newId = Integer.parseInt(txtId.getText());
            String rawOpt = newArr.toString(); // JSON array string like [[30,0]]
            model.setValueAt(newId, row, 0);
            model.setValueAt(getItemIcon(newId), row, 1);
            model.setValueAt(lblName.getText(), row, 2);
            model.setValueAt(Integer.parseInt(txtQty.getText()), row, 3);
            model.setValueAt(parseOptionReadable(rawOpt), row, 4);
            model.setValueAt(rawOpt, row, 5);
            // Đổi sang item khác -> coi như item mới, reset thời gian tạo để tránh dùng hạn cũ.
            // Chỉ sửa số lượng/option của cùng 1 item thì giữ nguyên thời gian tạo gốc.
            if (newId != currId) {
                model.setValueAt(System.currentTimeMillis(), row, 6);
            }
            d.dispose();
        });

        JPanel pSave = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_MD, SP_SM));
        pSave.setOpaque(false);
        pSave.add(btnSave);

        d.add(pTop, BorderLayout.NORTH);
        d.add(pCenter, BorderLayout.CENTER);
        d.add(pSave, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private DefaultTableModel createItemModel() {
        // 2 cột cuối ("createTime", "slot") là cột kỹ thuật ẩn:
        //  - createTime: thời gian tạo gốc của item (giữ nguyên để không reset hạn dùng)
        //  - slot: vị trí ô gốc trong DB (giữ nguyên để không xáo trộn ô đồ, nhất là đồ đang mặc)
        return new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "SL", "Options (Readable)", "Raw Options", "createTime", "slot"}, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; } 
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 1) return ImageIcon.class; 
                return Object.class;
            }
        };
    }

    private void loadItemsToModel(String jsonArrayStr, DefaultTableModel model) {
        if (jsonArrayStr == null || jsonArrayStr.isEmpty()) return;
        
        try {
            JsonArray arr = new JsonParser().parse(jsonArrayStr).getAsJsonArray();
            for (int slot = 0; slot < arr.size(); slot++) {
                JsonElement e = arr.get(slot);
                try {
                    String innerStr = e.getAsString();
                    JsonArray itemData = new JsonParser().parse(innerStr).getAsJsonArray();
                    int id = itemData.get(0).getAsInt();
                    if (id == -1) continue;
                    int qty = itemData.get(1).getAsInt();
                    String rawOpt = (itemData.size() > 2) ? itemData.get(2).getAsString() : "[]";
                    // Giữ nguyên thời gian tạo gốc (trường thứ 4) để không reset hạn dùng của item
                    long createTime;
                    try {
                        createTime = (itemData.size() > 3) ? itemData.get(3).getAsLong() : System.currentTimeMillis();
                    } catch (Exception ctEx) {
                        createTime = System.currentTimeMillis();
                    }

                    // Fix: Handle options stored as JSON string with quotes
                    // The options in DB might be like "\"[[30,0]]\"" which needs to be unwrapped
                    try {
                        JsonElement optParsed = new JsonParser().parse(rawOpt);
                        if (optParsed.isJsonPrimitive() && optParsed.getAsJsonPrimitive().isString()) {
                            // It's a string (quoted JSON), try to parse the inner string
                            String innerOpt = optParsed.getAsString();
                            try {
                                JsonElement innerParsed = new JsonParser().parse(innerOpt);
                                if (innerParsed.isJsonArray()) {
                                    rawOpt = innerParsed.getAsJsonArray().toString();
                                } else {
                                    rawOpt = "[]";
                                }
                            } catch (Exception ee) {
                                // If inner parsing fails, use as-is
                                rawOpt = "[]";
                            }
                        } else if (optParsed.isJsonArray()) {
                            rawOpt = optParsed.getAsJsonArray().toString();
                        }
                    } catch (Exception ex) {
                        // If parsing fails, use empty options
                        rawOpt = "[]";
                    }
                    
                    model.addRow(new Object[]{id, getItemIcon(id), getItemName(id), qty, parseOptionReadable(rawOpt), rawOpt, createTime, slot});
                } catch (Exception itemEx) {
                    System.err.println("DEBUG loadItemsToModel: Error loading item: " + itemEx.getMessage());
                }
            }
        } catch (Exception e) {
            System.err.println("DEBUG loadItemsToModel: Error parsing JSON: " + e.getMessage());
        }
    }
    
    private String parseOptionReadable(String jsonOpt) {
        // Handle cả 2 format: array of arrays [[oid,p]...] và array of strings ["[oid,p]"...]
        try {
            StringBuilder sb = new StringBuilder();
            JsonArray arr = new JsonParser().parse(jsonOpt).getAsJsonArray();
            for (JsonElement e : arr) {
                try {
                    JsonArray opt;
                    if (e.isJsonArray()) {
                        opt = e.getAsJsonArray();
                    } else if (e.isJsonPrimitive() && e.getAsJsonPrimitive().isString()) {
                        opt = new JsonParser().parse(e.getAsString()).getAsJsonArray();
                    } else {
                        continue;
                    }
                    if (opt.size() < 2) continue;
                    int id    = opt.get(0).getAsInt();
                    int param = opt.get(1).getAsInt();
                    sb.append(formatOption(id, param)).append(", ");
                } catch (Exception perItem) {
                    // skip bad row
                }
            }
            if (sb.length() > 2) return sb.substring(0, sb.length() - 2);
        } catch (Exception e) { return jsonOpt; }
        return "";
    }

    private void openItemAddDialog(DefaultTableModel model, JDialog parent) {
        JDialog d = new JDialog(parent, "Thêm Vật Phẩm", true);
        d.setSize(900, 600); d.setLayout(new BorderLayout());
        d.setLocationRelativeTo(parent);
        d.getContentPane().setBackground(BG);

        JPanel pFilter = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_SM));
        pFilter.setOpaque(false);
        pFilter.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_XS, SP_MD));
        JTextField txtSearch = searchField("Tìm theo tên hoặc ID...");
        txtSearch.setColumns(15);
        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada", "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);
        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả"};
        JComboBox<String> cbGender = new JComboBox<>(genders);
        JLabel lblFilterTitle = ServerGuiUtils.label("Bộ Lọc", FONT_H3, TEXT);
        pFilter.add(lblFilterTitle);
        pFilter.add(ServerGuiUtils.label("Tên/ID:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(txtSearch);
        pFilter.add(ServerGuiUtils.label(" | Loại:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(cbType);
        pFilter.add(ServerGuiUtils.label(" | Hệ:", FONT_SMALL_B, TEXT_SECONDARY)); pFilter.add(cbGender);

        DefaultTableModel searchModel = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
             @Override public Class<?> getColumnClass(int c) { return c==1 ? ImageIcon.class : Object.class; }
             @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        for (ItemData item : listAllItems) searchModel.addRow(new Object[]{item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        
        JTable t = new JTable(searchModel);
        JScrollPane spItems = styleTable(t);
        styleIconColumn(t, 1);
        t.getColumnModel().getColumn(0).setPreferredWidth(50); t.getColumnModel().getColumn(1).setPreferredWidth(40); t.getColumnModel().getColumn(2).setPreferredWidth(350);
        t.getColumnModel().getColumn(3).setMinWidth(0); t.getColumnModel().getColumn(3).setMaxWidth(0);
        t.getColumnModel().getColumn(4).setMinWidth(0); t.getColumnModel().getColumn(4).setMaxWidth(0);
        
        TableRowSorter<DefaultTableModel> s = new TableRowSorter<>(searchModel); t.setRowSorter(s);

        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();
            
            if (!text.isEmpty()) {
                try {
                    int id = Integer.parseInt(text);
                    List<RowFilter<Object, Object>> orFilters = new ArrayList<>();
                    orFilters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, id, 0));
                    orFilters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                    filters.add(RowFilter.orFilter(orFilters));
                } catch (NumberFormatException e) {
                    filters.add(RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2));
                }
            }
            
            if (typeIdx > 0) { try { int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]); filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3)); } catch (Exception e) {} }
            if (genderIdx > 0) { try { int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]); filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4)); } catch (Exception e) {} }
            if (filters.isEmpty()) s.setRowFilter(null); else s.setRowFilter(RowFilter.andFilter(filters));
        };

        txtSearch.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { doFilter.run(); } public void removeUpdate(DocumentEvent e) { doFilter.run(); } public void changedUpdate(DocumentEvent e) { doFilter.run(); } });
        cbType.addActionListener(e -> doFilter.run()); cbGender.addActionListener(e -> doFilter.run());

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if(r!=-1) {
                         int modelRow = t.convertRowIndexToModel(r);
                         int id = (int) searchModel.getValueAt(modelRow, 0);
                         ImageIcon icon = (ImageIcon) searchModel.getValueAt(modelRow, 1);
                         String name = (String) searchModel.getValueAt(modelRow, 2);
                         model.addRow(new Object[]{id, icon, name, 1, "", "[]", System.currentTimeMillis(), -1});
                         d.dispose();
                    }
                }
            }
        });

        d.add(pFilter, BorderLayout.NORTH); d.add(spItems, BorderLayout.CENTER); d.setVisible(true);
    }

    private void savePlayerDB(int pid, int accountId, Map<String, Component> inputs, 
                              DefaultTableModel mBody, DefaultTableModel mBag, DefaultTableModel mBox, 
                              DefaultTableModel mBadges,
                              Map<String, String> originalData, JDialog d) {
        new Thread(() -> {
            try {
                JsonArray inv = new JsonArray();
                inv.add(getLongVal(inputs, "gold"));
                inv.add(getLongVal(inputs, "gem"));
                inv.add(getLongVal(inputs, "ruby"));
                JsonArray oldInv = new JsonParser().parse(originalData.get("data_inventory")).getAsJsonArray();
                for(int i = 3; i < oldInv.size(); i++) {
                    inv.add(oldInv.get(i));
                }

                JsonArray point = new JsonParser().parse(originalData.get("data_point")).getAsJsonArray();
                setVal(point, 1, getText(inputs, "power"));
                setVal(point, 2, getText(inputs, "tiemnang"));
                setVal(point, 5, getText(inputs, "hpg"));
                setVal(point, 6, getText(inputs, "mpg"));
                setVal(point, 7, getText(inputs, "dameg"));
                setVal(point, 8, getText(inputs, "defg"));
                String critRaw = getText(inputs, "critg");
                try {
                    int c = Integer.parseInt(critRaw.trim());
                    if (c > 127) c = 127;
                    else if (c < -128) c = -128;
                    setVal(point, 9, String.valueOf(c));
                } catch (NumberFormatException nfe) {
                    setVal(point, 9, "0");
                }

                String jsonBody, jsonBag, jsonBox;
                try {
                    String itemsBodyStr = originalData.get("items_body");
                    String itemsBagStr = originalData.get("items_bag");
                    String itemsBoxStr = originalData.get("items_box");

                    jsonBody = mergeItemToDB(mBody, itemsBodyStr, "Body/Đồ mặc");
                    jsonBag = mergeItemToDB(mBag, itemsBagStr, "Hành trang");
                    jsonBox = mergeItemToDB(mBox, itemsBoxStr, "Rương đồ");
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, ex.getMessage(), "Lỗi đây rồi", JOptionPane.ERROR_MESSAGE));
                    return; 
                }

                String petJson = originalData.get("pet");
                if (petJson != null && !petJson.equals("[]") && inputs.containsKey("pet_name")) {
                    JsonArray petArr = new JsonParser().parse(petJson).getAsJsonArray();
                    if(petArr.size() > 1) {
                        JsonArray infoArr = new JsonParser().parse(petArr.get(0).getAsString()).getAsJsonArray();
                        String typeStr = ((JComboBox)inputs.get("pet_type")).getSelectedItem().toString();
                        if(typeStr.contains(" - ")) typeStr = typeStr.split(" - ")[0];
                        setVal(infoArr, 0, typeStr);
                        setVal(infoArr, 1, String.valueOf(((JComboBox)inputs.get("pet_gender")).getSelectedIndex()));
                        setVal(infoArr, 2, getText(inputs, "pet_name"));
                        setVal(infoArr, 5, String.valueOf(((JComboBox)inputs.get("pet_status")).getSelectedIndex()));
                        petArr.set(0, new JsonPrimitive(infoArr.toString()));

                        JsonArray pointArr = new JsonParser().parse(petArr.get(1).getAsString()).getAsJsonArray();
                        setVal(pointArr, 1, getText(inputs, "pet_power"));
                        setVal(pointArr, 2, getText(inputs, "pet_tiemnang"));
                        setVal(pointArr, 5, getText(inputs, "pet_hpg"));
                        setVal(pointArr, 6, getText(inputs, "pet_mpg"));
                        setVal(pointArr, 7, getText(inputs, "pet_dameg"));
                        setVal(pointArr, 8, getText(inputs, "pet_defg"));
                        setVal(pointArr, 9, getText(inputs, "pet_critg"));
                        petArr.set(1, new JsonPrimitive(pointArr.toString()));
                        petJson = petArr.toString();
                    }
                }

                String dataTaskJson = createTaskJson(inputs, "mainTaskId", "mainTaskIndex", "mainTaskCount", "mainTaskLastTime", 4);
                String sideTaskJson = createMultiTaskJson(inputs, "sideTaskId_", "sideTaskCount_", 3);
                String clanTaskJson = createMultiTaskJson(inputs, "clanTaskId_", "clanTaskCount_", 3);
                String kolTaskJson = createTaskJson(inputs, "kolTaskId", null, "kolTaskCount", null, 2);
                
                JsonArray badgesArr = new JsonArray();
                for(int i = 0; i < mBadges.getRowCount(); i++) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("idBadGes", Integer.parseInt(mBadges.getValueAt(i, 0).toString()));
                    obj.addProperty("timeofUseBadges", Long.parseLong(mBadges.getValueAt(i, 4).toString()));
                    obj.addProperty("isUse", Boolean.parseBoolean(mBadges.getValueAt(i, 6).toString()));
                    badgesArr.add(obj);
                }
                String dataBadgesJson = badgesArr.toString();

                // Schema này: không có cột 'power' (power nằm trong data_point[1], đã set ở trên);
                // 'data_kol_task'→'data_KOL', 'dataBadges'→'data_danh_hieu'
                // KHÔNG ghi data_danh_hieu (schema này là bộ đếm nhiệm vụ hằng ngày, không phải badge) — giữ nguyên
                String sqlPlayer = "UPDATE player SET name=?, head=?, data_inventory=?, data_point=?, items_body=?, items_bag=?, items_box=?, pet=?, data_task=?, data_side_task=?, data_clan_task=?, data_KOL=? WHERE id=?";
                try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sqlPlayer)) {
                    ps.setString(1, getText(inputs, "name"));
                    ps.setInt(2, Integer.parseInt(getText(inputs, "head")));
                    ps.setString(3, inv.toString());
                    ps.setString(4, point.toString());
                    ps.setString(5, jsonBody);
                    ps.setString(6, jsonBag);
                    ps.setString(7, jsonBox);
                    ps.setString(8, petJson);
                    ps.setString(9, dataTaskJson);
                    ps.setString(10, sideTaskJson);
                    ps.setString(11, clanTaskJson);
                    ps.setString(12, kolTaskJson);
                    ps.setInt(13, pid);
                    ps.executeUpdate();
                }

                models.player.Player onlinePl = server.Client.gI().getPlayer(pid);
                if (onlinePl != null) {
                    try {
                        onlinePl.inventory.gold = Math.min(models.player.Inventory.LIMIT_GOLD, inv.get(0).getAsLong());
                        onlinePl.inventory.gem = (int) Math.min(Integer.MAX_VALUE, inv.get(1).getAsLong());
                        onlinePl.inventory.ruby = (int) Math.min(Integer.MAX_VALUE, inv.get(2).getAsLong());
                        services.Service.gI().sendMoney(onlinePl);
                        services.Service.gI().sendThongBao(onlinePl, "Dữ liệu của bạn vừa được cập nhật bởi Ban Quản Trị!");
                    } catch (Exception exx) {}
                }

                SwingUtilities.invokeLater(() -> { 
                    JOptionPane.showMessageDialog(d, "Đã lưu thành công và gửi lệnh cập nhật đầy đủ đến server!"); 
                    d.dispose(); 
                    loadPlayersFromDB(""); 
                });
            } catch (Exception e) { 
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(d, "Lỗi lưu: " + e.getMessage())); 
            }
        }).start();
    }

    private String createTaskJson(Map<String, Component> inputs, String idKey, String indexKey, String countKey, String timeKey, int size) {
        JsonArray arr = new JsonArray();
        
        int id = -1;
        Component idComp = inputs.get(idKey);
        if (idComp instanceof JComboBox) {
            String selected = ((JComboBox)idComp).getSelectedItem().toString();
            if (selected.contains(" - ")) {
                try {
                    id = Integer.parseInt(selected.split(" - ")[0]);
                } catch (NumberFormatException e) {
                    id = -1;
                }
            }
        }
        arr.add(id);
        
        if (indexKey != null && inputs.containsKey(indexKey)) {
            try {
                int index = Integer.parseInt(((JTextField)inputs.get(indexKey)).getText());
                arr.add(index);
            } catch (Exception e) {
                arr.add(0);
            }
        } else if (size > 1) {
            arr.add(0);
        }
        
        if (countKey != null && inputs.containsKey(countKey)) {
            try {
                int count = Integer.parseInt(((JTextField)inputs.get(countKey)).getText());
                arr.add(count);
            } catch (Exception e) {
                arr.add(0);
            }
        } else if (size > 2) {
            arr.add(0);
        }
        
        if (timeKey != null && inputs.containsKey(timeKey)) {
            try {
                long lastTime = Long.parseLong(((JTextField)inputs.get(timeKey)).getText());
                arr.add(lastTime);
            } catch (Exception e) {
                arr.add(System.currentTimeMillis());
            }
        } else if (size > 3) {
            arr.add(System.currentTimeMillis());
        }
        
        while (arr.size() < size) {
            arr.add(0);
        }
        
        return arr.toString();
    }

    private String createMultiTaskJson(Map<String, Component> inputs, String idPrefix, String countPrefix, int count) {
        JsonArray arr = new JsonArray();
        
        for (int i = 0; i < count; i++) {
            String idKey = idPrefix + i;
            String countKey = countPrefix + i;
            
            int id = -1;
            Component idComp = inputs.get(idKey);
            if (idComp instanceof JComboBox) {
                String selected = ((JComboBox)idComp).getSelectedItem().toString();
                if (selected.contains(" - ")) {
                    try {
                        id = Integer.parseInt(selected.split(" - ")[0]);
                    } catch (NumberFormatException e) {
                        id = -1;
                    }
                }
            }
            arr.add(id);
            
            int taskCount = 0;
            Component countComp = inputs.get(countKey);
            if (countComp instanceof JTextField) {
                try {
                    taskCount = Integer.parseInt(((JTextField)countComp).getText());
                } catch (Exception e) {
                    taskCount = 0;
                }
            }
            arr.add(taskCount);
        }
        
        return arr.toString();
    }

    private String mergeItemToDB(DefaultTableModel model, String originalJson, String typeName) throws Exception {
        JsonArray dbArr;
        try {
            if (originalJson == null || originalJson.isEmpty() || originalJson.equals("[]")) {
                // Default inventory sizes: body=16, bag=128, box=64
                int defaultSize = typeName.contains("Body") ? 16 : (typeName.contains("Hành") ? 128 : 64);
                dbArr = new JsonArray();
                for (int i = 0; i < defaultSize; i++) {
                    dbArr.add(new JsonPrimitive(createEmptyItem()));
                }
            } else {
                dbArr = new JsonParser().parse(originalJson).getAsJsonArray();
            }
        } catch (Exception e) {
            e.printStackTrace();
            // Default sizes on error
            int defaultSize = typeName.contains("Body") ? 16 : (typeName.contains("Hành") ? 128 : 64);
            dbArr = new JsonArray();
            for (int i = 0; i < defaultSize; i++) {
                dbArr.add(new JsonPrimitive(createEmptyItem()));
            }
        }

        int maxSlots = dbArr.size();
        int itemsInTable = model.getRowCount();

        if (itemsInTable > maxSlots) {
            throw new Exception("Lỗi: " + typeName + " đã bị ĐẦY!\nSố lượng hiện tại: " + itemsInTable + "\nSức chứa tối đa: " + maxSlots + "\nVui lòng xóa bớt item trước khi lưu.");
        }

        // Bảo toàn ĐÚNG vị trí ô gốc của từng item: khởi tạo toàn ô rỗng rồi đặt item vào.
        //  - Lượt 1: item đọc từ DB (có slot gốc hợp lệ & còn trống) -> giữ nguyên ô.
        //  - Lượt 2: item mới thêm (slot = -1) hoặc bị tranh chấp -> dồn vào ô trống đầu tiên.
        // Nhờ vậy đồ đang mặc (ô = loại trang bị) không bị xáo trộn và thời gian tạo được giữ nguyên.
        String[] slots = new String[maxSlots];
        java.util.List<Integer> pending = new java.util.ArrayList<>();
        for (int row = 0; row < itemsInTable; row++) {
            int slotIdx = -1;
            try {
                Object colSlot = model.getValueAt(row, 7);
                if (colSlot != null) {
                    slotIdx = Integer.parseInt(colSlot.toString());
                }
            } catch (Exception ignore) {
                slotIdx = -1;
            }
            if (slotIdx >= 0 && slotIdx < maxSlots && slots[slotIdx] == null) {
                slots[slotIdx] = buildItemNode(model, row);
            } else {
                pending.add(row);
            }
        }
        int scan = 0;
        for (int row : pending) {
            while (scan < maxSlots && slots[scan] != null) {
                scan++;
            }
            if (scan >= maxSlots) {
                throw new Exception("Lỗi: " + typeName + " đã bị ĐẦY!\nSố lượng hiện tại: " + itemsInTable + "\nSức chứa tối đa: " + maxSlots + "\nVui lòng xóa bớt item trước khi lưu.");
            }
            slots[scan] = buildItemNode(model, row);
        }

        JsonArray result = new JsonArray();
        for (int i = 0; i < maxSlots; i++) {
            result.add(new JsonPrimitive(slots[i] != null ? slots[i] : createEmptyItem()));
        }
        return result.toString();
    }

    /**
     * Dựng 1 ô item dạng JSON [id, qty, options, createTime] từ 1 dòng của bảng.
     * Giữ nguyên createTime gốc (cột 6); chỉ dùng now() khi thiếu (item mới thêm).
     */
    private String buildItemNode(DefaultTableModel model, int row) {
        try {
            Object col0 = model.getValueAt(row, 0);
            Object col3 = model.getValueAt(row, 3);
            Object col5 = model.getValueAt(row, 5);
            if (col0 == null || col3 == null || col5 == null) {
                System.err.println("DEBUG: Null value at row " + row + ", col0=" + col0 + ", col3=" + col3 + ", col5=" + col5);
                return createEmptyItem();
            }
            int id = Integer.parseInt(col0.toString());
            int qty = Integer.parseInt(col3.toString());
            String rawOpt = col5.toString();
            try {
                JsonElement parsed = new JsonParser().parse(rawOpt);
                if (parsed.isJsonArray()) {
                    rawOpt = parsed.getAsJsonArray().toString();
                } else if (parsed.isJsonPrimitive() && parsed.getAsJsonPrimitive().isString()) {
                    String inner = parsed.getAsString();
                    try {
                        JsonElement innerParsed = new JsonParser().parse(inner);
                        rawOpt = innerParsed.isJsonArray() ? innerParsed.getAsJsonArray().toString() : "[]";
                    } catch (Exception ee) {
                        rawOpt = "[]";
                    }
                } else {
                    rawOpt = "[]";
                }
            } catch (Exception e) {
                rawOpt = "[]";
            }

            long createTime;
            try {
                Object col6 = model.getValueAt(row, 6);
                createTime = (col6 != null) ? Long.parseLong(col6.toString()) : System.currentTimeMillis();
            } catch (Exception e) {
                createTime = System.currentTimeMillis();
            }

            JsonArray itemNode = new JsonArray();
            itemNode.add(id);
            itemNode.add(qty);
            itemNode.add(rawOpt);
            itemNode.add(createTime);
            return itemNode.toString();
        } catch (Exception e) {
            e.printStackTrace();
            return createEmptyItem();
        }
    }

    private String createEmptyItem() {
        JsonArray emptyNode = new JsonArray();
        emptyNode.add(-1);
        emptyNode.add(0);
        emptyNode.add("[]");
        // Ô rỗng: createTime không có ý nghĩa (login bỏ qua khi id == -1) nên để 0 cho ổn định, tránh churn.
        emptyNode.add(0);
        return emptyNode.toString();
    }

    private String getText(Map<String, Component> inputs, String key) {
        Component c = inputs.get(key);
        if (c instanceof JTextField) return ((JTextField)c).getText();
        return "0";
    }
    
    private long getLongVal(Map<String, Component> inputs, String key) {
        try {
            String txt = ((JTextField)inputs.get(key)).getText();
            return Long.parseLong(txt.replaceAll("[^0-9-]", ""));
        } catch(Exception e) { return 0; }
    }

    private void setVal(JsonArray arr, int index, String val) {
        while (arr.size() <= index) {
            arr.add(new JsonPrimitive(0));
        }
        try {
            String cleanVal = val.replaceAll("[^0-9-]", "");
            long v = Long.parseLong(cleanVal);
            arr.set(index, new JsonPrimitive(v));
        } catch (Exception e) {
            arr.set(index, new JsonPrimitive(val));
        }
    }
    
}
