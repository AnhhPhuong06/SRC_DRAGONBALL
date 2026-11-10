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
import javax.swing.table.TableRowSorter;
import javax.swing.text.JTextComponent;
import javax.swing.undo.UndoManager;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

public class ShopEditorPanel extends JPanel {

    // --- Cấu hình Icon ---
    private static final String ICON_FOLDER = "data/icon/"; // Đường dẫn tới thư mục icon

    // Tự động dò tên cột
    private String colId = "id";
    private String colShopId = "shop_id";
    private String colName = "";
    private String colType = "";
    private String colItems = "items";

    private JTable tableShop;
    private DefaultTableModel modelShop;

    // Cache dữ liệu (để tìm kiếm nhanh)
    private final Map<Integer, String> itemTemplateMap = new HashMap<>(); // ID Item -> Tên Item
    private final Map<Integer, Integer> itemIconMap = new HashMap<>();    // ID Item -> Icon ID
    private final Map<Integer, String> shopOwnerMap = new HashMap<>();    // Shop ID -> Tên NPC
    private final Map<Integer, Integer> npcHeadPartIconMap = new HashMap<>(); // Part ID -> Icon ID (Type 0)
    private static final Map<Integer, String> OPTION_TEMPLATE = new HashMap<>(); // ID Option -> Tên Option
    private final java.util.List<ItemData> listAllItems = new java.util.ArrayList<>();
    private final AtomicBoolean loading = new AtomicBoolean(false);

    // Cache Icon (RAM)
    private final Map<Integer, ImageIcon> iconCache = new HashMap<>();
    private final Map<Integer, Boolean> noIconCache = new HashMap<>();
    private final Map<Integer, ImageIcon> npcHeadCache = new HashMap<>();

    public ShopEditorPanel() {
        setLayout(new BorderLayout(SP_SM, SP_SM));
        setBackground(BG);
        setBorder(pad(SP_SM));
        initUI();
        startLoadingData();
    }

    private void setupGlobalShortcuts(JComponent component, Runnable saveAction) {
        InputMap im = component.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = component.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "quickSave");
        am.put("quickSave", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (saveAction != null) {
                    saveAction.run();
                }
            }
        });
    }

    // --- [NEW] Tiện ích Undo/Redo cho Text Field ---
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

    // [FIX] Hàm chạy luồng tải dữ liệu tuần tự
    private void startLoadingData() {
        if (!loading.compareAndSet(false, true)) {
            return;
        }
        new Thread(() -> {
            try {
                loadPartsHead();
                initOptionTemplateData(); // [FIXED] Load option từ DB
                loadItemTemplates();
                loadShopOwners();
                loadDataFromDB();
            } finally {
                loading.set(false);
            }
        }).start();
    }

    private static class ItemData {

        int id;
        String name;
        int type;
        int gender;

        public ItemData(int id, String name, int type, int gender) {
            this.id = id;
            this.name = name;
            this.type = type;
            this.gender = gender;
        }
    }

    // Class hỗ trợ Undo (Lưu trạng thái dòng bị xóa)
    private static class RestoreData {
        int index;
        Vector<Object> data;

        public RestoreData(int index, Vector<Object> data) {
            this.index = index;
            this.data = data;
        }
    }

    // --- Renderer icon đồng bộ với styleTable (zebra + hover + padding) ---
    private static void applyIconRenderer(JTable table, int column) {
        table.getColumnModel().getColumn(column).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected, boolean hasFocus, int row, int col) {
                super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, col);
                if (value instanceof ImageIcon) {
                    setIcon((ImageIcon) value);
                    setText("");
                } else {
                    setIcon(null);
                }
                setHorizontalAlignment(JLabel.CENTER);
                setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                Object hoverObj = t.getClientProperty("ui.hoverRow");
                int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
                if (isSelected) {
                    setBackground(PRIMARY_SOFT);
                    setForeground(TEXT);
                } else if (row == hover) {
                    setBackground(HOVER_ROW);
                    setForeground(TEXT);
                } else {
                    setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                    setForeground(TEXT);
                }
                return this;
            }
        });
    }

    // --- Renderer checkbox đồng bộ với styleTable (zebra + hover) ---
    private static void applyBooleanRenderer(JTable table, int column) {
        JCheckBox check = new JCheckBox();
        check.setHorizontalAlignment(SwingConstants.CENTER);
        check.setOpaque(true);
        check.setBorderPainted(false);
        table.getColumnModel().getColumn(column).setCellRenderer((t, value, isSelected, hasFocus, row, col) -> {
            check.setSelected(Boolean.TRUE.equals(value));
            Object hoverObj = t.getClientProperty("ui.hoverRow");
            int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
            if (isSelected) {
                check.setBackground(PRIMARY_SOFT);
            } else if (row == hover) {
                check.setBackground(HOVER_ROW);
            } else {
                check.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
            }
            return check;
        });
    }

    private Connection getConnection() throws SQLException {
        // Shop, tab_shop, item_template, part đều nằm ở DB dữ liệu game (nro_data)
        return DBConnecter.getConnectionData();
    }

    // --- LOAD DỮ LIỆU TỪ BẢNG PART (CHỈ LẤY TYPE 0 - PHẦN ĐẦU) ---
    private void loadPartsHead() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, data FROM part WHERE type = 0")) {
            while (rs.next()) {
                try {
                    String dataJson = rs.getString("data");
                    JsonArray arr = new JsonParser().parse(dataJson).getAsJsonArray();
                    if (arr.size() > 0) {
                        JsonArray firstElement = arr.get(0).getAsJsonArray();
                        int iconId = firstElement.get(0).getAsInt();
                        npcHeadPartIconMap.put(rs.getInt("id"), iconId);
                    }
                } catch (Exception ignored) {
                }
            }
        } catch (SQLException ignored) {
        }
    }

    // --- VẼ ICON ĐẦU NPC ---
    private ImageIcon drawNpcHead(int headPartId) {
        if (headPartId <= 0) {
            return null;
        }
        if (npcHeadCache.containsKey(headPartId)) {
            return npcHeadCache.get(headPartId);
        }

        Integer iconId = npcHeadPartIconMap.get(headPartId);
        if (iconId != null) {
            ImageIcon icon = getIconByFileId(iconId, 32);
            if (icon != null) {
                npcHeadCache.put(headPartId, icon);
                return icon;
            }
        }
        return null;
    }

    private ImageIcon getIconByFileId(int fileId, int size) {
        try {
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + fileId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return new ImageIcon(dimg);
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // --- [ĐÃ SỬA] XỬ LÝ LOAD OPTION TỪ DATABASE ---
    private void initOptionTemplateData() {
        OPTION_TEMPLATE.clear();
        // Lấy dữ liệu thực từ database thay vì chuỗi hardcode
        String query = "SELECT id, name FROM item_option_template";
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {

            while (rs.next()) {
                int id = rs.getInt("id");
                String name = services.ItemService.resolveOptionName(id, rs.getString("name"));
                OPTION_TEMPLATE.put(id, name);
            }
            // System.out.println("Loaded " + OPTION_TEMPLATE.size() + " options.");

        } catch (Exception e) {
            e.printStackTrace();
            // Fallback nếu lỗi DB (giữ lại 1 ít mẫu để không crash)
            OPTION_TEMPLATE.put(50, "Sức đánh +#%");
            OPTION_TEMPLATE.put(77, "HP +#%");
            OPTION_TEMPLATE.put(103, "KI +#%");
        }
    }

    private String getOptionDescription(int id, int param) {
        String template = OPTION_TEMPLATE.getOrDefault(id, "Unknown Option (" + id + ")");
        return template.replace("#", String.valueOf(param));
    }

    private String getOptionName(int id) {
        return OPTION_TEMPLATE.getOrDefault(id, "Option " + id);
    }

    // --- Tự động dò tên cột ---
    private void detectColumns(ResultSetMetaData meta) throws SQLException {
        int count = meta.getColumnCount();
        for (int i = 1; i <= count; i++) {
            String cName = meta.getColumnName(i).toLowerCase();
            if (cName.equals("items") || cName.equals("item_json")) {
                colItems = cName;
            } else if (cName.equals("id")) {
                colId = cName;
            } else if (cName.equals("shop_id")) {
                colShopId = cName;
            } else if (colName.isEmpty() && (cName.contains("name") || cName.contains("title"))) {
                colName = cName;
            } else if (colType.isEmpty() && (cName.contains("type") || cName.equals("kieu") || cName.contains("index"))) {
                colType = cName;
            }
        }
    }

    private void loadItemTemplates() {
        listAllItems.clear();
        itemTemplateMap.clear();

        String query = "SELECT id, name, icon_id, type, gender FROM item_template";

        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int id = rs.getInt("id");
                String name = rs.getString("name");
                int type = rs.getInt("type");
                int gender = rs.getInt("gender");
                int iconId = rs.getInt("icon_id");

                itemTemplateMap.put(id, name);
                itemIconMap.put(id, iconId);
                listAllItems.add(new ItemData(id, name, type, gender));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    private String getItemName(int tempId) {
        return itemTemplateMap.getOrDefault(tempId, "Unknown Item (" + tempId + ")");
    }

    private ImageIcon getItemIcon(int itemId) {
        if (iconCache.containsKey(itemId)) {
            return iconCache.get(itemId);
        }
        if (noIconCache.containsKey(itemId)) {
            return null;
        }

        int iconId = itemIconMap.getOrDefault(itemId, -1);
        ImageIcon icon = getIconByFileId(iconId, 20);
        if (icon != null) {
            iconCache.put(itemId, icon);
            return icon;
        }

        noIconCache.put(itemId, true);
        return null;
    }

    private void loadShopOwners() {
        shopOwnerMap.clear();
        String query = "SELECT s.id, n.name FROM shop s LEFT JOIN npc_template n ON s.npc_id = n.id";
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int shopId = rs.getInt("id");
                String npcName = rs.getString("name");
                if (npcName == null) {
                    npcName = "Unknown";
                }
                shopOwnerMap.put(shopId, npcName);
            }
        } catch (SQLException e) {
        }
    }

    private String getShopOwnerName(int shopId) {
        return shopOwnerMap.getOrDefault(shopId, "---");
    }

    // --- GIAO DIỆN CHÍNH ---
    private void initUI() {
        JButton btnReload = button("Tải lại", ButtonVariant.SECONDARY, "refresh");
        btnReload.addActionListener(e -> {
            modelShop.setRowCount(0);
            startLoadingData();
        });

        JButton btnSave = button("Lưu xuống DB (Ctrl+S)", ButtonVariant.PRIMARY, "save");
        btnSave.addActionListener(e -> saveSelectedRowToDB());

        // [NEW] Nút Hướng Dẫn
        JButton btnHelp = button("Hướng dẫn", ButtonVariant.GHOST, "info");
        btnHelp.addActionListener(e -> showHelp());

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        actions.setOpaque(false);
        actions.add(btnHelp);
        actions.add(btnReload);
        actions.add(btnSave);

        String[] columns = {"ID", "Avatar", "Shop ID", "Chủ Shop (NPC)", "Tên Tab", "Loại/Index", "Danh sách Vật Phẩm (Click đúp)"};
        modelShop = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 4 || column == 5;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                return (c == 1) ? ImageIcon.class : Object.class;
            }
        };

        tableShop = new JTable(modelShop);

        tableShop.getColumnModel().getColumn(0).setPreferredWidth(40);
        tableShop.getColumnModel().getColumn(1).setPreferredWidth(45);
        tableShop.getColumnModel().getColumn(2).setPreferredWidth(50);
        tableShop.getColumnModel().getColumn(3).setPreferredWidth(120);
        tableShop.getColumnModel().getColumn(4).setPreferredWidth(120);
        tableShop.getColumnModel().getColumn(5).setPreferredWidth(50);
        tableShop.getColumnModel().getColumn(6).setPreferredWidth(450);

        tableShop.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableShop.getSelectedColumn() == 6) {
                    openItemEditorDialog(tableShop.getSelectedRow());
                }
            }
        });

        JScrollPane tableScroll = styleTable(tableShop);
        tableShop.setRowHeight(40);
        applyIconRenderer(tableShop, 1);

        // Thiết lập phím tắt Ctrl+S cho Panel chính
        setupGlobalShortcuts(this, this::saveSelectedRowToDB);

        JPanel content = card(new BorderLayout());
        content.add(sectionHeader("Quản lý Shop (Table: tab_shop)", "*Click đúp 'Vật Phẩm' để sửa.", actions), BorderLayout.NORTH);
        content.add(tableScroll, BorderLayout.CENTER);
        add(content, BorderLayout.CENTER);
    }

    // [NEW] Hàm hiển thị hướng dẫn
    private void showHelp() {
        String html = "<html><body style='width: 300px'>"
                + "<h3>Hướng dẫn sử dụng Shop Editor</h3>"
                + "<ul>"
                + "<li><b>Sửa Tên/Loại Tab:</b> Click đúp trực tiếp vào ô trên bảng chính để sửa.</li>"
                + "<li><b>Sửa Vật Phẩm:</b> Click đúp vào cột <i>'Danh sách Vật Phẩm'</i> để mở cửa sổ chi tiết.</li>"
                + "<li><b>Trong cửa sổ Vật Phẩm:</b>"
                + "<ul>"
                + "<li><b>Thêm Mới:</b> Thêm từng item.</li>"
                + "<li><b>Thêm (Nhiều):</b> Mở danh sách chọn nhiều item cùng lúc.</li>"
                + "<li><b>Xóa:</b> Chọn 1 hoặc nhiều dòng rồi nhấn Xóa.</li>"
                + "<li><b>Hoàn tác (Undo):</b> Khôi phục lại các dòng vừa xóa nhầm.</li>"
                + "<li><b>Di chuyển:</b> Dùng nút Lên/Xuống để sắp xếp vị trí item.</li>"
                + "</ul></li>"
                + "<li><b>Phím tắt tiện ích:</b>"
                + "<ul>"
                + "<li><b>Ctrl + S:</b> Lưu dữ liệu (Hoạt động cả ở bảng chính và bảng item).</li>"
                + "<li><b>Ctrl + Z:</b> Hoàn tác (Undo) khi nhập liệu text.</li>"
                + "<li><b>Ctrl + Y:</b> Làm lại (Redo) khi nhập liệu text.</li>"
                + "</ul></li>"
                + "</ul>"
                + "</body></html>";
        JOptionPane.showMessageDialog(this, new JLabel(html), "Hướng dẫn", JOptionPane.INFORMATION_MESSAGE);
    }

    private void loadDataFromDB() {
        SwingUtilities.invokeLater(() -> modelShop.setRowCount(0));

        String query = """
                SELECT ts.*, n.head
                FROM tab_shop ts
                LEFT JOIN shop s ON ts.shop_id = s.id
                LEFT JOIN npc_template n ON s.npc_id = n.id
                ORDER BY ts.shop_id ASC, ts.id ASC
                """;
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {

            detectColumns(rs.getMetaData());

            while (rs.next()) {
                Vector<Object> row = new Vector<>();
                int sId = rs.getInt(colShopId);
                int headPartId = rs.getInt("head");

                row.add(rs.getInt(colId));
                row.add(drawNpcHead(headPartId));
                row.add(sId);
                row.add(getShopOwnerName(sId));

                try {
                    row.add(rs.getString(colName));
                } catch (Exception e) {
                    row.add("NULL");
                }
                try {
                    row.add(rs.getInt(colType));
                } catch (Exception e) {
                    row.add(0);
                }

                String itemsJson = rs.getString(colItems);
                row.add(itemsJson != null ? itemsJson : "[]");

                SwingUtilities.invokeLater(() -> modelShop.addRow(row));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveSelectedRowToDB() {
        int row = tableShop.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Chưa chọn dòng!");
            return;
        }

        try {
            int id = Integer.parseInt(modelShop.getValueAt(row, 0).toString());
            int shopId = Integer.parseInt(modelShop.getValueAt(row, 2).toString());
            String name = modelShop.getValueAt(row, 4).toString();
            int type = Integer.parseInt(modelShop.getValueAt(row, 5).toString());
            String items = modelShop.getValueAt(row, 6).toString();

            new Thread(() -> {
                String query = "UPDATE tab_shop SET " + colShopId + "=?, " + colName + "=?, " + colType + "=?, " + colItems + "=? WHERE " + colId + "=?";
                try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(query)) {
                    pstmt.setInt(1, shopId);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, type);
                    pstmt.setString(4, items);
                    pstmt.setInt(5, id);

                    int affected = pstmt.executeUpdate();
                    // Nạp lại shop vào RAM để cập nhật ngay (không cần restart)
                    if (affected > 0) {
                        try (Connection c2 = getConnection()) {
                            server.Manager.SHOPS = database.daos.ShopDAO.getShops(c2);
                        } catch (Exception ignoredReload) {
                        }
                    }
                    SwingUtilities.invokeLater(() -> {
                        if (affected > 0) {
                            JOptionPane.showMessageDialog(this, "Đã lưu!");
                        } else {
                            JOptionPane.showMessageDialog(this, "Lỗi lưu!");
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Dữ liệu lỗi!");
        }
    }

    // ========================================================================
    // 1. ITEM EDITOR DIALOG (LIST OF ITEMS)
    // ========================================================================
    private void openItemEditorDialog(int mainRow) {
        String jsonString = (String) modelShop.getValueAt(mainRow, 6);
        JDialog dialog = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Danh sách vật phẩm", true);
        dialog.setSize(1100, 600);
        dialog.setLayout(new BorderLayout(SP_SM, SP_SM));
        dialog.getContentPane().setBackground(BG);
        ((JComponent) dialog.getContentPane()).setBorder(pad(SP_SM));
        dialog.setLocationRelativeTo(this);

        String[] itemCols = {"Temp ID", "Icon", "Tên Item", "Giá", "Loại Bán", "Spec", "Mới?", "Bán?", "Options (JSON)"};
        DefaultTableModel itemModel = new DefaultTableModel(itemCols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return column != 1 && column != 2 && column != 8;
            }

            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 1) {
                    return ImageIcon.class;
                }
                return (c == 6 || c == 7) ? Boolean.class : String.class;
            }
        };

        JTable itemTable = new JTable(itemModel);
        itemTable.getColumnModel().getColumn(0).setPreferredWidth(60);
        itemTable.getColumnModel().getColumn(1).setPreferredWidth(40);
        itemTable.getColumnModel().getColumn(2).setPreferredWidth(150);
        itemTable.getColumnModel().getColumn(8).setPreferredWidth(300);

        itemModel.addTableModelListener(e -> {
            if (e.getColumn() == 0) {
                int r = e.getFirstRow();
                try {
                    int newId = Integer.parseInt(itemModel.getValueAt(r, 0).toString());
                    itemModel.setValueAt(getItemName(newId), r, 2);
                    itemModel.setValueAt(getItemIcon(newId), r, 1);
                } catch (Exception ignored) {
                }
            }
        });

        try {
            JsonArray arr = new JsonParser().parse(jsonString).getAsJsonArray();
            for (JsonElement e : arr) {
                JsonObject obj = e.getAsJsonObject();
                Vector<Object> v = new Vector<>();
                int tempId = Integer.parseInt(getData(obj, "temp_id"));
                v.add(tempId);
                v.add(getItemIcon(tempId));
                v.add(getItemName(tempId));
                v.add(getData(obj, "cost"));
                v.add(getData(obj, "type_sell"));
                v.add(getData(obj, "item_spec"));
                v.add(obj.has("is_new") && obj.get("is_new").getAsBoolean());
                v.add(obj.has("is_sell") && obj.get("is_sell").getAsBoolean());
                v.add(obj.has("options") ? obj.get("options").toString() : "[]");
                itemModel.addRow(v);
            }
        } catch (Exception ignored) {
        }

        JScrollPane itemScroll = styleTable(itemTable);
        applyIconRenderer(itemTable, 1);
        applyBooleanRenderer(itemTable, 6);
        applyBooleanRenderer(itemTable, 7);

        JPanel btnPanel = new JPanel(new WrapLayout(FlowLayout.RIGHT, SP_SM, SP_SM));
        btnPanel.setOpaque(false);
        JButton btnAdd = button("Thêm Mới", ButtonVariant.SECONDARY, "plus");
        JButton btnAddBulk = button("Thêm (Nhiều)", ButtonVariant.SECONDARY, "box");
        JButton btnUp = button("⬆ Lên", ButtonVariant.GHOST);
        JButton btnDown = button("⬇ Xuống", ButtonVariant.GHOST);
        JButton btnEdit = button("Sửa Item", ButtonVariant.SECONDARY, "edit");
        JButton btnDel = button("Xóa (Chọn nhiều)", ButtonVariant.DANGER, "broom");

        JButton btnUndo = button("↩ Hoàn tác", ButtonVariant.GHOST);
        btnUndo.setEnabled(false); // Mặc định ẩn

        JButton btnOk = button("LƯU JSON & ĐÓNG (Ctrl+S)", ButtonVariant.PRIMARY, "save");

        // --- Stack quản lý lịch sử xóa ---
        Stack<List<RestoreData>> undoStack = new Stack<>();

        // Logic nút Thêm Mới (1 item)
        btnAdd.addActionListener(e -> openItemBuilderDialog(itemModel, -1));

        // Logic nút Thêm Nhiều (Bulk Add)
        btnAddBulk.addActionListener(e -> showBulkSearchDialog(dialog, (id, name) -> {
            Vector<Object> row = new Vector<>();
            row.add(id);
            row.add(getItemIcon(id));
            row.add(name);
            row.add("0"); // Cost
            row.add("0"); // Type Sell
            row.add("0"); // Spec
            row.add(false); // New
            row.add(true);  // Sell
            row.add("[]");  // Options
            itemModel.addRow(row);
        }));

        // Logic nút Lên
        btnUp.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row > 0) {
                itemModel.moveRow(row, row, row - 1);
                itemTable.setRowSelectionInterval(row - 1, row - 1);
            }
        });

        // Logic nút Xuống
        btnDown.addActionListener(e -> {
            int row = itemTable.getSelectedRow();
            if (row != -1 && row < itemModel.getRowCount() - 1) {
                itemModel.moveRow(row, row, row + 1);
                itemTable.setRowSelectionInterval(row + 1, row + 1);
            }
        });

        // Logic nút Sửa
        btnEdit.addActionListener(e -> {
            if (itemTable.getSelectedRow() != -1) {
                openItemBuilderDialog(itemModel, itemTable.getSelectedRow());
            }
        });

        // Logic nút Xóa Hàng Loạt (Có lưu lịch sử để Undo)
        btnDel.addActionListener(e -> {
            int[] selectedRows = itemTable.getSelectedRows();
            if (selectedRows.length > 0) {
                if (JOptionPane.showConfirmDialog(dialog, "Xóa " + selectedRows.length + " item đã chọn?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {

                    List<RestoreData> batch = new ArrayList<>();
                    for (int i = selectedRows.length - 1; i >= 0; i--) {
                        int rowIdx = selectedRows[i];
                        Vector data = (Vector) ((Vector) itemModel.getDataVector()).get(rowIdx);
                        batch.add(new RestoreData(rowIdx, new Vector<>(data)));
                        itemModel.removeRow(rowIdx);
                    }

                    undoStack.push(batch);
                    btnUndo.setEnabled(true);
                }
            }
        });

        // Logic nút Undo (Hoàn tác)
        btnUndo.addActionListener(e -> {
            if (!undoStack.isEmpty()) {
                List<RestoreData> batch = undoStack.pop();
                for (RestoreData item : batch) {
                    itemModel.insertRow(item.index, item.data);
                }
                if (undoStack.isEmpty()) {
                    btnUndo.setEnabled(false);
                }
            }
        });

        // Logic nút Lưu (Format JSON đẹp)
        Runnable saveAction = () -> {
            try {
                JsonArray newArr = new JsonArray();
                for (int i = 0; i < itemModel.getRowCount(); i++) {
                    JsonObject obj = new JsonObject();
                    obj.addProperty("cost", Integer.parseInt(itemModel.getValueAt(i, 3).toString()));
                    obj.addProperty("type_sell", Integer.parseInt(itemModel.getValueAt(i, 4).toString()));
                    obj.addProperty("is_new", (Boolean) itemModel.getValueAt(i, 6));
                    obj.addProperty("temp_id", Integer.parseInt(itemModel.getValueAt(i, 0).toString()));
                    obj.addProperty("item_spec", Integer.parseInt(itemModel.getValueAt(i, 5).toString()));

                    try {
                        obj.add("options", new JsonParser().parse(itemModel.getValueAt(i, 8).toString()));
                    } catch (Exception ex) {
                        obj.add("options", new JsonArray());
                    }

                    obj.addProperty("is_sell", (Boolean) itemModel.getValueAt(i, 7));
                    newArr.add(obj);
                }

                StringBuilder sb = new StringBuilder("[\n");
                for (int j = 0; j < newArr.size(); j++) {
                    sb.append(newArr.get(j).toString());
                    if (j < newArr.size() - 1) {
                        sb.append(",\n");
                    }
                }
                sb.append("\n]");

                modelShop.setValueAt(sb.toString(), mainRow, 6);
                dialog.dispose();
                if (JOptionPane.showConfirmDialog(this, "Lưu xuống Database?", "Lưu?", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                    saveSelectedRowToDB();
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dialog, "Lỗi: " + ex.getMessage());
            }
        };
        btnOk.addActionListener(e -> saveAction.run());

        itemTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    openItemBuilderDialog(itemModel, itemTable.getSelectedRow());
                }
            }
        });

        // Thiết lập phím tắt cho Dialog
        setupGlobalShortcuts(dialog.getRootPane(), saveAction);

        btnPanel.add(btnAdd);
        btnPanel.add(btnAddBulk);
        btnPanel.add(btnUp);
        btnPanel.add(btnDown);
        btnPanel.add(btnEdit);
        btnPanel.add(btnDel);
        btnPanel.add(btnUndo); // Thêm vào giao diện
        btnPanel.add(btnOk);

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(itemScroll, BorderLayout.CENTER);

        dialog.add(tableCard, BorderLayout.CENTER);
        dialog.add(btnPanel, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // ========================================================================
    // 2. ITEM BUILDER DIALOG (ADD/EDIT ITEM DETAIL)
    // ========================================================================
    private void openItemBuilderDialog(DefaultTableModel parentModel, int editRow) {
        JDialog dialog = new JDialog((Frame) null, "Cấu hình Vật Phẩm Chi Tiết", true);
        dialog.setSize(750, 700);
        dialog.setLayout(new BorderLayout(SP_SM, SP_SM));
        dialog.getContentPane().setBackground(BG);
        ((JComponent) dialog.getContentPane()).setBorder(pad(SP_SM));
        dialog.setLocationRelativeTo(null);

        JPanel pTop = card(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtId = new JTextField(); addUndoRedo(txtId);
        JLabel lblName = label("...", FONT_BODY_B, PRIMARY);
        JLabel lblIcon = new JLabel();
        lblIcon.setPreferredSize(new Dimension(24, 24));

        JButton btnSearchItem = button("Tìm", ButtonVariant.SECONDARY, "search");
        JTextField txtCost = new JTextField("0"); addUndoRedo(txtCost);
        JComboBox<String> cbTypeSell = new JComboBox<>(new String[]{"0 - Vàng", "1 - Ngọc Xanh", "2 - Hồng Ngọc"});
        JTextField txtSpec = new JTextField("0"); addUndoRedo(txtSpec);
        JLabel lblSpecName = label("None", FONT_BODY_B, SUCCESS);
        JButton btnSearchSpec = button("Tìm Spec", ButtonVariant.SECONDARY, "search");

        JCheckBox chkNew = new JCheckBox("Mới (New)");
        JCheckBox chkSell = new JCheckBox("Bán (Sell)");
        chkNew.setOpaque(false);
        chkNew.setFont(FONT_BODY);
        chkSell.setOpaque(false);
        chkSell.setFont(FONT_BODY);
        chkSell.setSelected(true);

        String oldJsonOpt = "[]";
        if (editRow != -1) {
            txtId.setText(parentModel.getValueAt(editRow, 0).toString());
            lblName.setText(parentModel.getValueAt(editRow, 2).toString());
            txtCost.setText(parentModel.getValueAt(editRow, 3).toString());
            try {
                cbTypeSell.setSelectedIndex(Integer.parseInt(parentModel.getValueAt(editRow, 4).toString()));
            } catch (Exception e) {
            }
            txtSpec.setText(parentModel.getValueAt(editRow, 5).toString());
            chkNew.setSelected((Boolean) parentModel.getValueAt(editRow, 6));
            chkSell.setSelected((Boolean) parentModel.getValueAt(editRow, 7));
            oldJsonOpt = parentModel.getValueAt(editRow, 8).toString();
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

        txtSpec.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { u(); }
            public void removeUpdate(DocumentEvent e) { u(); }
            public void changedUpdate(DocumentEvent e) { u(); }
            void u() {
                try {
                    lblSpecName.setText(getItemName(Integer.parseInt(txtSpec.getText())));
                } catch (Exception ex) {
                    lblSpecName.setText("None");
                }
            }
        });

        try {
            int id = Integer.parseInt(txtId.getText());
            lblName.setText(getItemName(id));
            lblIcon.setIcon(getItemIcon(id));
        } catch (Exception ignored) {
        }

        btnSearchItem.addActionListener(e -> showItemSearchDialog(dialog, (id, name) -> {
            txtId.setText(String.valueOf(id));
            lblName.setText(name);
        }));

        btnSearchSpec.addActionListener(e -> showItemSearchDialog(dialog, (id, name) -> {
            txtSpec.setText(String.valueOf(id));
            lblSpecName.setText(name);
        }));

        g.gridx = 0; g.gridy = 0; pTop.add(label("ID Item:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; pTop.add(txtId, g);
        g.gridx = 2; pTop.add(btnSearchItem, g);
        g.gridx = 0; g.gridy = 1; pTop.add(label("Info:", FONT_SMALL_B, TEXT_SECONDARY), g);
        JPanel pInfo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pInfo.setOpaque(false);
        pInfo.add(lblIcon); pInfo.add(lblName);
        g.gridx = 1; g.gridwidth = 2; pTop.add(pInfo, g);
        g.gridwidth = 1; g.gridx = 0; g.gridy = 2; pTop.add(label("Giá bán:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; pTop.add(txtCost, g);
        g.gridx = 2; pTop.add(cbTypeSell, g);
        g.gridx = 0; g.gridy = 3; pTop.add(label("Spec ID:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; pTop.add(txtSpec, g);
        g.gridx = 2; pTop.add(btnSearchSpec, g);
        g.gridx = 0; g.gridy = 4; pTop.add(label("Tên Spec:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; g.gridwidth = 2; pTop.add(lblSpecName, g);
        g.gridwidth = 1; g.gridx = 0; g.gridy = 5; pTop.add(label("Tùy chọn:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; g.gridwidth = 2;
        JPanel pChk = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pChk.setOpaque(false);
        pChk.add(chkNew); pChk.add(chkSell);
        pTop.add(pChk, g); g.gridwidth = 1;

        String[] oCols = {"ID Option", "Param", "Mô tả (Tự động)"};
        DefaultTableModel oModel = new DefaultTableModel(oCols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c != 2;
            }
        };
        JTable oTable = new JTable(oModel);

        oModel.addTableModelListener(e -> {
            int r = e.getFirstRow();
            if (r >= 0 && r < oModel.getRowCount() && e.getColumn() != 2) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        int id = Integer.parseInt(oModel.getValueAt(r, 0).toString());
                        int param = Integer.parseInt(oModel.getValueAt(r, 1).toString());
                        oModel.setValueAt(getOptionDescription(id, param), r, 2);
                    } catch (Exception ex) {
                    }
                });
            }
        });

        try {
            JsonArray arr = new JsonParser().parse(oldJsonOpt).getAsJsonArray();
            for (JsonElement el : arr) {
                JsonObject obj = el.getAsJsonObject();
                int id = Integer.parseInt(getData(obj, "id"));
                int param = Integer.parseInt(getData(obj, "param"));
                oModel.addRow(new Object[]{id, param, getOptionDescription(id, param)});
            }
        } catch (Exception e) {
        }

        JPanel pOpt = card(new BorderLayout(0, SP_SM));
        JPanel pOptHead = new JPanel(new BorderLayout(SP_SM, 0));
        pOptHead.setOpaque(false);
        JLabel lblOptTitle = label("Options", FONT_H3, TEXT);
        lblOptTitle.setIcon(vectorIcon("settings", 14, TEXT_SECONDARY));
        pOptHead.add(lblOptTitle, BorderLayout.WEST);
        JPanel pOptTool = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pOptTool.setOpaque(false);
        JButton btnAddO = button("Thêm", ButtonVariant.SECONDARY, "plus");
        JButton btnFindO = button("Tìm Option", ButtonVariant.SECONDARY, "search");
        JButton btnDelO = button("Xóa", ButtonVariant.DANGER, "broom");
        btnAddO.addActionListener(e -> oModel.addRow(new Object[]{"0", "0", getOptionDescription(0, 0)}));

        btnFindO.addActionListener(e -> showSearchDialog(dialog, "Tìm Option", OPTION_TEMPLATE, (id, name) -> {
            oModel.addRow(new Object[]{id, "0", name.replace("#", "0")});
            int newRow = oModel.getRowCount() - 1;
            oTable.setRowSelectionInterval(newRow, newRow);
            oTable.editCellAt(newRow, 1);
            oTable.getEditorComponent().requestFocus();
        }));

        btnDelO.addActionListener(e -> {
            if (oTable.getSelectedRow() != -1) {
                oModel.removeRow(oTable.getSelectedRow());
            }
        });
        pOptTool.add(btnAddO);
        pOptTool.add(btnFindO);
        pOptTool.add(btnDelO);
        pOptHead.add(pOptTool, BorderLayout.EAST);
        pOpt.add(pOptHead, BorderLayout.NORTH);
        pOpt.add(styleTable(oTable), BorderLayout.CENTER);

        JButton btnOk = button("XONG - LƯU ITEM", ButtonVariant.PRIMARY, "check");
        Runnable saveItemAction = () -> {
            JsonArray arr = new JsonArray();
            for (int i = 0; i < oModel.getRowCount(); i++) {
                try {
                    JsonObject o = new JsonObject();
                    o.addProperty("id", Integer.parseInt(oModel.getValueAt(i, 0).toString()));
                    o.addProperty("param", Integer.parseInt(oModel.getValueAt(i, 1).toString()));
                    arr.add(o);
                } catch (Exception ex) {
                }
            }
            Object[] row = {txtId.getText(), getItemIcon(Integer.parseInt(txtId.getText())), lblName.getText(), txtCost.getText(), String.valueOf(cbTypeSell.getSelectedIndex()), txtSpec.getText(), chkNew.isSelected(), chkSell.isSelected(), arr.toString()};
            if (editRow == -1) {
                parentModel.addRow(row);
            } else {
                for (int i = 0; i < 9; i++) {
                    parentModel.setValueAt(row[i], editRow, i);
                }
            }
            dialog.dispose();
        };
        btnOk.addActionListener(e -> saveItemAction.run());

        // Phím tắt Ctrl+S cho dialog
        setupGlobalShortcuts(dialog.getRootPane(), saveItemAction);

        JPanel pSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pSouth.setOpaque(false);
        pSouth.add(btnOk);

        dialog.add(pTop, BorderLayout.NORTH);
        dialog.add(pOpt, BorderLayout.CENTER);
        dialog.add(pSouth, BorderLayout.SOUTH);
        dialog.setVisible(true);
    }

    // --- [MỚI] DIALOG TÌM KIẾM ITEM ĐƠN LẺ (CÓ LỌC TYPE & GENDER) ---
    private void showItemSearchDialog(JDialog parent, SearchCallback callback) {
        JDialog d = new JDialog(parent, "Tìm kiếm Vật Phẩm (Tìm theo ID hoặc Tên)", true);
        d.setSize(900, 600);
        d.setLayout(new BorderLayout(SP_SM, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JComponent) d.getContentPane()).setBorder(pad(SP_SM));
        d.setLocationRelativeTo(parent);

        JPanel pFilterRow = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_XS));
        pFilterRow.setOpaque(false);

        JTextField txtSearch = searchField("Tìm theo ID hoặc Tên...");
        txtSearch.setColumns(20);
        addUndoRedo(txtSearch);
        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
            "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);

        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả"};
        JComboBox<String> cbGender = new JComboBox<>(genders);

        pFilterRow.add(label("Tên/ID:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(txtSearch);
        pFilterRow.add(label("Loại:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(cbType);
        pFilterRow.add(label("Hệ:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(cbGender);

        JPanel pFilter = card(new BorderLayout(0, SP_SM));
        ((ShadowCard) pFilter).setContentPadding(SP_MD, SP_MD, SP_MD, SP_MD);
        pFilter.add(label("Bộ Lọc", FONT_H3, TEXT), BorderLayout.NORTH);
        pFilter.add(pFilterRow, BorderLayout.CENTER);

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? ImageIcon.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };

        for (ItemData item : listAllItems) {
            m.addRow(new Object[]{item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        }

        JTable t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(350);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        JScrollPane tScroll = styleTable(t);
        applyIconRenderer(t, 1);

        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();

            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();

            if (!text.isEmpty()) {
                var idFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0);
                var nameFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2);
                filters.add(RowFilter.orFilter(Arrays.asList(idFilter, nameFilter)));
            }

            if (typeIdx > 0) {
                try {
                    int val = Integer.parseInt(cbType.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 3));
                } catch (Exception e) {
                }
            }

            if (genderIdx > 0) {
                try {
                    int val = Integer.parseInt(cbGender.getSelectedItem().toString().split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) {
                }
            }

            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doFilter.run(); }
            public void removeUpdate(DocumentEvent e) { doFilter.run(); }
            public void changedUpdate(DocumentEvent e) { doFilter.run(); }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        Runnable doSelect = () -> {
            int r = t.getSelectedRow();
            if (r != -1) {
                int modelRow = t.convertRowIndexToModel(r);
                int id = (int) m.getValueAt(modelRow, 0);
                String name = (String) m.getValueAt(modelRow, 2);
                callback.onSelect(id, name);
                d.dispose();
            }
        };

        t.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doSelect.run();
                }
            }
        });

        JButton btnOk = button("CHỌN ITEM NÀY", ButtonVariant.PRIMARY, "check");
        btnOk.addActionListener(e -> doSelect.run());

        JPanel pSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pSouth.setOpaque(false);
        pSouth.add(btnOk);

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(tScroll, BorderLayout.CENTER);

        d.add(pFilter, BorderLayout.NORTH);
        d.add(tableCard, BorderLayout.CENTER);
        d.add(pSouth, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    interface SearchCallback {
        void onSelect(int id, String name);
    }

    private void showBulkSearchDialog(JDialog parent, SearchCallback callback) {
        JDialog d = new JDialog(parent, "Thêm Nhiều Vật Phẩm (Tích chọn để thêm)", true);
        d.setSize(950, 600);
        d.setLayout(new BorderLayout(SP_SM, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JComponent) d.getContentPane()).setBorder(pad(SP_SM));
        d.setLocationRelativeTo(parent);

        JPanel pFilterRow = new JPanel(new WrapLayout(FlowLayout.LEFT, SP_SM, SP_XS));
        pFilterRow.setOpaque(false);

        JTextField txtSearch = searchField("Tìm theo ID hoặc Tên...");
        txtSearch.setColumns(20);
        addUndoRedo(txtSearch);
        String[] types = {"- Tất cả Loại -", "0 - Áo", "1 - Quần", "2 - Găng", "3 - Giày", "4 - Rada",
            "5 - Cải trang/Tóc", "6 - Đậu thần", "12 - Ngọc rồng", "27 - Vật phẩm", "29 - Capsule/Bánh", "32 - Giáp tập"};
        JComboBox<String> cbType = new JComboBox<>(types);

        String[] genders = {"- Tất cả Hệ -", "0 - Trái Đất", "1 - Namếc", "2 - Xayda", "3 - Chung/Tất cả"};
        JComboBox<String> cbGender = new JComboBox<>(genders);

        pFilterRow.add(label("Tên/ID:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(txtSearch);
        pFilterRow.add(label("Loại:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(cbType);
        pFilterRow.add(label("Hệ:", FONT_SMALL_B, TEXT_SECONDARY));
        pFilterRow.add(cbGender);

        JPanel pFilter = card(new BorderLayout(0, SP_SM));
        ((ShadowCard) pFilter).setContentPadding(SP_MD, SP_MD, SP_MD, SP_MD);
        pFilter.add(label("Bộ Lọc Tìm Kiếm", FONT_H3, TEXT), BorderLayout.NORTH);
        pFilter.add(pFilterRow, BorderLayout.CENTER);

        DefaultTableModel m = new DefaultTableModel(new String[]{"Chọn", "ID", "Icon", "Tên Item", "Type", "Gender"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                if (c == 0) return Boolean.class;
                return c == 2 ? ImageIcon.class : Object.class;
            }

            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 0;
            }
        };

        for (ItemData item : listAllItems) {
            m.addRow(new Object[]{false, item.id, getItemIcon(item.id), item.name, item.type, item.gender});
        }

        JTable t = new JTable(m);
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(50);
        t.getColumnModel().getColumn(2).setPreferredWidth(40);
        t.getColumnModel().getColumn(3).setPreferredWidth(350);
        t.getColumnModel().getColumn(4).setPreferredWidth(50);
        t.getColumnModel().getColumn(5).setPreferredWidth(50);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        JScrollPane tScroll = styleTable(t);
        applyBooleanRenderer(t, 0);
        applyIconRenderer(t, 2);

        Runnable doFilter = () -> {
            String text = txtSearch.getText().trim();
            int typeIdx = cbType.getSelectedIndex();
            int genderIdx = cbGender.getSelectedIndex();

            java.util.List<RowFilter<Object, Object>> filters = new java.util.ArrayList<>();

            if (!text.isEmpty()) {
                var idFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1);
                var nameFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 3);
                filters.add(RowFilter.orFilter(Arrays.asList(idFilter, nameFilter)));
            }

            if (typeIdx > 0) {
                String str = cbType.getSelectedItem().toString();
                try {
                    int val = Integer.parseInt(str.split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 4));
                } catch (Exception e) {
                }
            }

            if (genderIdx > 0) {
                String str = cbGender.getSelectedItem().toString();
                try {
                    int val = Integer.parseInt(str.split(" - ")[0]);
                    filters.add(RowFilter.numberFilter(RowFilter.ComparisonType.EQUAL, val, 5));
                } catch (Exception e) {
                }
            }

            if (filters.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { doFilter.run(); }
            public void removeUpdate(DocumentEvent e) { doFilter.run(); }
            public void changedUpdate(DocumentEvent e) { doFilter.run(); }
        });
        cbType.addActionListener(e -> doFilter.run());
        cbGender.addActionListener(e -> doFilter.run());

        JButton btnSelect = button("Thêm Các Mục Đã Tích (✔)", ButtonVariant.PRIMARY, "check");
        btnSelect.addActionListener(e -> {
            int count = 0;
            for (int i = 0; i < m.getRowCount(); i++) {
                Boolean isChecked = (Boolean) m.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    int id = (int) m.getValueAt(i, 1);
                    String name = (String) m.getValueAt(i, 3);
                    callback.onSelect(id, name);
                    count++;
                }
            }

            if (count == 0) {
                JOptionPane.showMessageDialog(d, "Chưa tích chọn item nào!");
                return;
            }
            d.dispose();
        });

        JPanel pSouth = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        pSouth.setOpaque(false);
        pSouth.add(btnSelect);

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(tScroll, BorderLayout.CENTER);

        d.add(pFilter, BorderLayout.NORTH);
        d.add(tableCard, BorderLayout.CENTER);
        d.add(pSouth, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private void showSearchDialog(JDialog parent, String title, Map<Integer, String> dataMap, SearchCallback callback) {
        JDialog d = new JDialog(parent, title, true);
        d.setSize(600, 500);
        d.setLayout(new BorderLayout(SP_SM, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JComponent) d.getContentPane()).setBorder(pad(SP_SM));
        d.setLocationRelativeTo(parent);
        JTextField txtSearch = searchField("Nhập ID hoặc Tên để lọc...");
        addUndoRedo(txtSearch); // Undo/Redo cho ô tìm kiếm

        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên / Mô tả"}, 0) {
            @Override
            public Class<?> getColumnClass(int c) {
                return c == 1 ? ImageIcon.class : Object.class;
            }
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        for (Map.Entry<Integer, String> entry : dataMap.entrySet()) {
            ImageIcon icon = (dataMap == itemTemplateMap) ? getItemIcon(entry.getKey()) : null;
            m.addRow(new Object[]{entry.getKey(), icon, entry.getValue()});
        }
        JTable t = new JTable(m);
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        JScrollPane tScroll = styleTable(t);
        applyIconRenderer(t, 1);

        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }

            void f() {
                String text = txtSearch.getText();
                if (text.trim().length() == 0) {
                    sorter.setRowFilter(null);
                } else {
                    var idFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0);
                    var nameFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2);
                    sorter.setRowFilter(RowFilter.orFilter(Arrays.asList(idFilter, nameFilter)));
                }
            }
        });
        t.addMouseListener(new MouseAdapter() {
            @Override
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

        JPanel pSearch = new JPanel(new BorderLayout());
        pSearch.setOpaque(false);
        pSearch.setBorder(pad(SP_XS, SP_XS, 0, SP_XS));
        pSearch.add(txtSearch, BorderLayout.CENTER);

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        tableCard.add(tScroll, BorderLayout.CENTER);

        d.add(pSearch, BorderLayout.NORTH);
        d.add(tableCard, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private String getData(JsonObject obj, String key) {
        return obj.has(key) ? obj.get(key).getAsString() : "0";
    }
}
