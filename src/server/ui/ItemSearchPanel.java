package server.ui;

import jdbc.DBConnecter;
import services.ItemService;
import models.Template;
import org.json.simple.JSONArray;
import org.json.simple.JSONValue;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static server.ui.ServerGuiUtils.*;

public class ItemSearchPanel extends JPanel {

    private DefaultTableModel searchTableModel;
    private JTable searchTable;
    private TableRowSorter<DefaultTableModel> searchSorter;
    private JTextField searchField;
    private JComboBox<String> searchTypeCombo;
    private JLabel searchStatusLbl;
    private final List<Object[]> allItemData = new ArrayList<>();
    private final List<Object[]> filteredItemData = new ArrayList<>();
    /** key = playerName + "_" + templateId  →  chi tiết từng cây đồ (vị trí, SL, option đã giải mã). */
    private final Map<String, List<String>> itemDetailMap = new HashMap<>();
    /** key = playerName + "_" + templateId  →  text option đã chuẩn hoá (lowercase, bỏ %) để tìm theo chỉ số. */
    private final Map<String, String> itemOptionsText = new HashMap<>();
    /** id option → tên (nạp thẳng từ DB item_option_template, key theo id thật; không phụ thuộc engine đã nạp xong). */
    private final Map<Integer, String> optionNames = new HashMap<>();
    /** id đồ (template id) → tên, nạp thẳng từ DB item_template (key theo id thật). */
    private final Map<Integer, String> itemNames = new HashMap<>();

    // ----- Tabs 2-4: Top by item id / set -----
    private TopItemView topThoiVang;  // id 457
    private TopItemView topBaSao;     // id 16
    private TopThanLinhView topThanLinh; // set Thần Linh (id 555-567)

    /** Khoảng template id của đồ "Thần Linh" (god set) theo định nghĩa game (godClothes/thanLinh): 555–567. */
    private static final int THAN_LINH_MIN = 555;
    private static final int THAN_LINH_MAX = 567;

    public ItemSearchPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));
        loadOptionNames();
        loadItemNames();
        initUI();

        // Auto-load on first show
        refreshAll();
    }

    // ----------------------------------------------------------------------
    //  Layout — tabbed
    // ----------------------------------------------------------------------
    private void initUI() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        column.add(sectionHeader("Vật Phẩm",
                "Tìm kiếm vật phẩm trong inventory player & xem bảng xếp hạng", null));
        column.add(Box.createVerticalStrut(SP_LG));

        topThoiVang = new TopItemView(457, "Thỏi Vàng", SUCCESS);
        topBaSao    = new TopItemView(16,  "3 Sao",    WARNING);
        topThanLinh = new TopThanLinhView();

        JTabbedPane tabs = new JTabbedPane();
        tabs.setFont(FONT_BODY_B);
        tabs.setBackground(SURFACE);
        tabs.addTab("Tìm kiếm",          vectorIcon("search", 14, TEXT_SECONDARY), buildSearchTab());
        tabs.addTab("Top Thỏi Vàng",     vectorIcon("coin", 14, SUCCESS), topThoiVang.getRoot());
        tabs.addTab("Top 3 Sao",         vectorIcon("star", 14, WARNING), topBaSao.getRoot());
        tabs.addTab("Top Đồ Thần Linh",  vectorIcon("diamond", 14, PRIMARY), topThanLinh.getRoot());
        column.add(tabs);

        add(column, BorderLayout.CENTER);
    }

    // ======================================================================
    //  TAB 1 — Search
    // ======================================================================
    private JComponent buildSearchTab() {
        JPanel wrap = new JPanel();
        wrap.setBackground(BG);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(SP_LG, 0, 0, 0));

        wrap.add(buildSearchToolbar());
        wrap.add(Box.createVerticalStrut(SP_LG));
        wrap.add(buildSearchTableCard());
        return wrap;
    }

    private JPanel buildSearchToolbar() {
        JPanel card = card(new BorderLayout(SP_LG, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 88));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        left.setOpaque(false);
        left.add(label("Tìm trong:", FONT_SMALL_B, TEXT_SECONDARY));
        searchTypeCombo = new JComboBox<>(new String[]{
                "ID", "Tên vật phẩm", "Tên nhân vật", "Option (chỉ số)", "Đang mặc", "Hành trang", "Rương đồ"
        });
        searchTypeCombo.setPreferredSize(new Dimension(140, 36));
        searchTypeCombo.addActionListener(e -> performSearch());
        left.add(searchTypeCombo);

        searchField = ServerGuiUtils.searchField(
                "Nhập ID, tên item, tên player, hoặc option (vd: 127% chí mạng)...");
        searchField.setPreferredSize(new Dimension(340, 36));
        left.add(searchField);

        JButton btnSearch = button("Tìm kiếm", ButtonVariant.PRIMARY, "search");
        btnSearch.addActionListener(e -> performSearch());
        left.add(btnSearch);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        right.setOpaque(false);
        searchStatusLbl = label("Đang tải...", FONT_SMALL, TEXT_MUTED);
        right.add(searchStatusLbl);
        JButton btnRefresh = button("Làm mới", ButtonVariant.SECONDARY, "refresh");
        btnRefresh.setToolTipText("Quét lại vật phẩm từ tất cả player");
        btnRefresh.addActionListener(e -> refreshSearchData());
        right.add(btnRefresh);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);

        // Live search
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { performSearch(); }
            @Override public void removeUpdate(DocumentEvent e) { performSearch(); }
            @Override public void changedUpdate(DocumentEvent e) { performSearch(); }
        });
        return card;
    }

    private JPanel buildSearchTableCard() {
        JPanel c = card(new BorderLayout(0, SP_MD));
        c.add(sectionHeader("Danh sách vật phẩm",
                "Nhấp đúp vào một dòng (hoặc chọn rồi nhấn Enter) để xem chi tiết chỉ số / option của vật phẩm",
                null), BorderLayout.NORTH);

        String[] columnNames = {"Tên Player", "Tên Vật Phẩm", "ID", "Số Lượng", "Vị Trí"};
        searchTableModel = new DefaultTableModel(columnNames, 0) {
            @Override public boolean isCellEditable(int row, int column) { return false; }
            @Override public Class<?> getColumnClass(int column) {
                return column == 2 || column == 3 ? Number.class : String.class;
            }
        };
        searchTable = new JTable(searchTableModel);
        searchSorter = new TableRowSorter<>(searchTableModel);
        searchTable.setRowSorter(searchSorter);

        JScrollPane sp = styleTable(searchTable);
        sp.setPreferredSize(new Dimension(0, 460));

        searchTable.getColumnModel().getColumn(0).setPreferredWidth(150);
        searchTable.getColumnModel().getColumn(1).setPreferredWidth(220);
        searchTable.getColumnModel().getColumn(2).setPreferredWidth(70);
        searchTable.getColumnModel().getColumn(3).setPreferredWidth(110);
        searchTable.getColumnModel().getColumn(4).setPreferredWidth(180);

        // Right-aligned quantity renderer
        searchTable.getColumnModel().getColumn(3).setCellRenderer(new NumberRenderer());

        // Nhấp đúp (hoặc Enter) vào 1 dòng để xem chi tiết option của vật phẩm
        searchTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) {
                    showItemDetail();
                }
            }
        });
        searchTable.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "viewItemDetail");
        searchTable.getActionMap().put("viewItemDetail", new AbstractAction() {
            @Override public void actionPerformed(java.awt.event.ActionEvent e) { showItemDetail(); }
        });

        c.add(sp, BorderLayout.CENTER);
        return c;
    }

    private void performSearch() {
        String text = searchField.getText().trim();
        String searchType = (String) searchTypeCombo.getSelectedItem();
        filteredItemData.clear();

        if ("Đang mặc".equals(searchType) || "Hành trang".equals(searchType) || "Rương đồ".equals(searchType)) {
            for (Object[] itemData : allItemData) {
                String locations = (String) itemData[4];
                if (locations != null && locations.contains(searchType)) {
                    if (text.isEmpty()) {
                        filteredItemData.add(itemData);
                    } else {
                        String itemName = (String) itemData[1];
                        String playerName = (String) itemData[0];
                        if ((itemName != null && itemName.toLowerCase().contains(text.toLowerCase()))
                                || (playerName != null && playerName.toLowerCase().contains(text.toLowerCase()))) {
                            filteredItemData.add(itemData);
                        }
                    }
                }
            }
        } else if ("Option (chỉ số)".equals(searchType)) {
            if (text.isEmpty()) {
                filteredItemData.addAll(allItemData);
            } else {
                // Khớp AND theo từng từ, bỏ dấu "%" → không phụ thuộc thứ tự chữ / cách gõ %
                String[] tokens = text.toLowerCase().replace("%", "").trim().split("\\s+");
                for (Object[] itemData : allItemData) {
                    String optText = itemOptionsText.get(itemData[0] + "_" + itemData[2]);
                    if (optText == null || optText.isEmpty()) continue;
                    boolean all = true;
                    for (String tk : tokens) {
                        if (!tk.isEmpty() && !optText.contains(tk)) { all = false; break; }
                    }
                    if (all) filteredItemData.add(itemData);
                }
            }
        } else if (text.isEmpty()) {
            filteredItemData.addAll(allItemData);
        } else {
            for (Object[] itemData : allItemData) {
                boolean match = false;
                if ("ID".equals(searchType)) {
                    try {
                        int searchItemId = Integer.parseInt(text);
                        Object itemIdObj = itemData[2];
                        int itemId = itemIdObj instanceof Integer ? (Integer) itemIdObj : Integer.parseInt(String.valueOf(itemIdObj));
                        match = itemId == searchItemId;
                    } catch (NumberFormatException ignored) {}
                } else if ("Tên vật phẩm".equals(searchType)) {
                    String itemName = (String) itemData[1];
                    match = itemName != null && itemName.toLowerCase().contains(text.toLowerCase());
                } else if ("Tên nhân vật".equals(searchType)) {
                    String playerName = (String) itemData[0];
                    match = playerName != null && playerName.toLowerCase().contains(text.toLowerCase());
                }
                if (match) filteredItemData.add(itemData);
            }
        }

        filteredItemData.sort((a, b) -> {
            try {
                long qtyA = a[3] instanceof Number ? ((Number) a[3]).longValue() : Long.parseLong(String.valueOf(a[3]));
                long qtyB = b[3] instanceof Number ? ((Number) b[3]).longValue() : Long.parseLong(String.valueOf(b[3]));
                return Long.compare(qtyB, qtyA);
            } catch (Exception e) { return 0; }
        });

        SwingUtilities.invokeLater(() -> {
            searchTableModel.setRowCount(0);
            List<Object[]> data = filteredItemData.isEmpty() && text.isEmpty() ? allItemData : filteredItemData;
            for (Object[] row : data) searchTableModel.addRow(row);
            searchStatusLbl.setText(data.size() + " kết quả / " + allItemData.size() + " tổng");
        });
    }

    /** Reloads the data and repopulates ALL tabs. */
    private void refreshAll() {
        refreshSearchData();
        topThoiVang.refresh();
        topBaSao.refresh();
        topThanLinh.refresh();
    }

    private void refreshSearchData() {
        SwingUtilities.invokeLater(() -> {
            searchTableModel.setRowCount(0);
            allItemData.clear();
            filteredItemData.clear();
            searchStatusLbl.setText("Đang quét DB...");
        });

        new Thread(() -> {
            try (Connection con = DBConnecter.getConnectionServer()) {
                if (con == null) {
                    SwingUtilities.invokeLater(() -> searchStatusLbl.setText("Lỗi kết nối DB"));
                    return;
                }
                Map<String, Object[]> itemMap = new HashMap<>();
                Map<String, List<String>> detailMap = new HashMap<>();
                Map<String, String> optTextMap = new HashMap<>();
                try (PreparedStatement ps = con.prepareStatement(
                        "SELECT name, items_bag, items_body, items_box FROM player ORDER BY name");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        String playerName = rs.getString("name");
                        if (playerName == null || playerName.trim().isEmpty()) continue;
                        parseItems(rs.getString("items_bag"),  playerName, itemMap, detailMap, optTextMap, "Hành trang");
                        parseItems(rs.getString("items_body"), playerName, itemMap, detailMap, optTextMap, "Đang mặc");
                        parseItems(rs.getString("items_box"),  playerName, itemMap, detailMap, optTextMap, "Rương đồ");
                    }
                }
                final List<Object[]> rows = new ArrayList<>(itemMap.values());
                SwingUtilities.invokeLater(() -> {
                    allItemData.clear();
                    allItemData.addAll(rows);
                    itemDetailMap.clear();
                    itemDetailMap.putAll(detailMap);
                    itemOptionsText.clear();
                    itemOptionsText.putAll(optTextMap);
                    performSearch();
                });
            } catch (Exception e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> searchStatusLbl.setText("Lỗi: " + e.getMessage()));
            }
        }, "ItemSearch-Refresh").start();
    }

    private void parseItems(String json, String playerName, Map<String, Object[]> itemMap,
                            Map<String, List<String>> detailMap, Map<String, String> optionTextMap, String location) {
        if (json == null || json.trim().isEmpty()) return;
        try {
            Object parsed = JSONValue.parse(json);
            JSONArray dataArray = null;
            if (parsed instanceof JSONArray) {
                dataArray = (JSONArray) parsed;
            } else if (parsed instanceof String) {
                Object parsed2 = JSONValue.parse((String) parsed);
                if (parsed2 instanceof JSONArray) dataArray = (JSONArray) parsed2;
            }
            if (dataArray == null) return;
            for (int i = 0; i < dataArray.size(); i++) {
                try {
                    Object itemObj = dataArray.get(i);
                    JSONArray dataItem = itemObj instanceof JSONArray
                            ? (JSONArray) itemObj
                            : (JSONArray) JSONValue.parse(itemObj.toString());
                    if (dataItem == null || dataItem.size() < 2) continue;
                    short tempId = Short.parseShort(String.valueOf(dataItem.get(0)));
                    if (tempId == -1) continue;
                    long quantity = Long.parseLong(String.valueOf(dataItem.get(1)));
                    String nm = resolveItemName(tempId);
                    if (nm == null || nm.isEmpty()) continue;
                    String key = playerName + "_" + tempId;

                    // Ghi chi tiết TỪNG cây đồ (không gộp option) để xem khi nhấp đúp
                    String opts = decodeOptions(dataItem);
                    String detailLine = "[" + location + "] SL " + quantity
                            + (opts.isEmpty() ? "  —  (không có chỉ số phụ)" : "  —  " + opts);
                    detailMap.computeIfAbsent(key, k -> new ArrayList<>()).add(detailLine);
                        if (!opts.isEmpty()) {
                        String norm = opts.toLowerCase().replace("%", "");
                        optionTextMap.merge(key, norm, (oldV, newV) -> oldV + " | " + newV);
                    }
                    Object[] existing = itemMap.get(key);
                    if (existing != null) {
                        long existingQty = existing[3] instanceof Number
                                ? ((Number) existing[3]).longValue()
                                : Long.parseLong(String.valueOf(existing[3]));
                        long totalQty = existingQty + quantity;
                        String locations = (String) existing[4];
                        if (!locations.contains(location)) {
                            locations = locations.isEmpty() ? location : locations + ", " + location;
                        }
                        itemMap.put(key, new Object[]{
                                existing[0], existing[1], existing[2], totalQty, locations
                        });
                    } else {
                        itemMap.put(key, new Object[]{
                                playerName, nm, (int) tempId, quantity, location
                        });
                    }
                } catch (Exception ignored) {}
            }
        } catch (Exception ignored) {}
    }

    // ----------------------------------------------------------------------
    //  Giải mã option của 1 cây đồ → chuỗi dễ đọc
    // ----------------------------------------------------------------------
    /** Đọc toàn bộ option (từ phần tử thứ 3 trở đi) của 1 item → "HP+10%, Giáp+12%". */
    private String decodeOptions(JSONArray dataItem) {
        if (dataItem == null || dataItem.size() <= 2) return "";
        StringBuilder sb = new StringBuilder();
        for (int k = 2; k < dataItem.size(); k++) {
            appendOption(sb, dataItem.get(k));
        }
        return sb.toString();
    }

    /** Bóc tách đệ quy: phần tử có thể là 1 cặp [oid,param], 1 mảng các cặp, hoặc chuỗi JSON. */
    private void appendOption(StringBuilder sb, Object node) {
        if (node == null) return;
        if (node instanceof String) {
            String s = ((String) node).trim();
            if (s.isEmpty() || s.equals("[]")) return;
            try {
                Object p = JSONValue.parse(s);
                if (p instanceof JSONArray) appendOption(sb, p);
            } catch (Exception ignored) {}
            return;
        }
        if (!(node instanceof JSONArray)) return;
        JSONArray arr = (JSONArray) node;
        if (arr.isEmpty()) return;
        Object first = arr.get(0);
        boolean firstIsList = (first instanceof JSONArray)
                || (first instanceof String && ((String) first).trim().startsWith("["));
        if (!firstIsList && arr.size() >= 2 && isNumber(arr.get(0)) && isNumber(arr.get(1))) {
            int oid = (int) toLong(arr.get(0));
            int param = (int) toLong(arr.get(1));
            if (sb.length() > 0) sb.append(", ");
            sb.append(optionName(oid, param));
        } else {
            for (int i = 0; i < arr.size(); i++) appendOption(sb, arr.get(i));
        }
    }

    private static boolean isNumber(Object o) {
        if (o instanceof Number) return true;
        if (o == null) return false;
        try { Long.parseLong(String.valueOf(o).trim()); return true; }
        catch (Exception e) { return false; }
    }

    private static long toLong(Object o) {
        if (o instanceof Number) return ((Number) o).longValue();
        return Long.parseLong(String.valueOf(o).trim());
    }

    private static long toLongSafe(Object o) {
        try { return toLong(o); } catch (Exception e) { return 0; }
    }

    /** Tra tên option: ưu tiên map nạp từ DB (key theo id thật), dự phòng engine RAM, cuối cùng mới hiện số. */
    private String optionName(int oid, int param) {
        String name = optionNames.get(oid);
        if (name == null || name.isEmpty()) {
            try {
                Template.ItemOptionTemplate t = ItemService.gI().getItemOptionTemplate(oid);
                if (t != null && t.name != null) name = t.name;
            } catch (Exception ignored) {}
        }
        if (name == null || name.isEmpty()) return "Opt " + oid + ": " + param;
        return name.contains("#") ? name.replace("#", String.valueOf(param)) : name + " " + param;
    }

    /** Nạp tên option thẳng từ DB (item_option_template ở nro_data), key theo id thật — tránh lệ thuộc engine. */
    private void loadOptionNames() {
        try (Connection conn = DBConnecter.getConnectionData();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM item_option_template");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                optionNames.put(rs.getInt("id"), ItemService.resolveOptionName(rs.getInt("id"), rs.getString("name")));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Nạp tên đồ thẳng từ DB (item_template ở nro_data), key theo id thật — tránh lệ thuộc engine. */
    private void loadItemNames() {
        try (Connection conn = DBConnecter.getConnectionData();
             PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM item_template");
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                itemNames.put(rs.getInt("id"), rs.getString("name"));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /** Tra tên đồ: ưu tiên map DB (key id thật), dự phòng engine RAM. Trả null nếu không rõ. */
    private String resolveItemName(int tempId) {
        String name = itemNames.get(tempId);
        if (name == null || name.isEmpty()) {
            try {
                Template.ItemTemplate t = ItemService.gI().getTemplate((short) tempId);
                if (t != null && t.name != null) name = t.name;
            } catch (Exception ignored) {}
        }
        return name;
    }

    // ----------------------------------------------------------------------
    //  Dialog chi tiết vật phẩm (nhấp đúp 1 dòng ở bảng tìm kiếm)
    // ----------------------------------------------------------------------
    private void showItemDetail() {
        int viewRow = searchTable.getSelectedRow();
        if (viewRow < 0) return;
        int modelRow = searchTable.convertRowIndexToModel(viewRow);
        if (modelRow < 0 || modelRow >= searchTableModel.getRowCount()) return;

        String player   = String.valueOf(searchTableModel.getValueAt(modelRow, 0));
        String itemName = String.valueOf(searchTableModel.getValueAt(modelRow, 1));
        Object idObj    = searchTableModel.getValueAt(modelRow, 2);
        Object qtyObj   = searchTableModel.getValueAt(modelRow, 3);
        String loc      = String.valueOf(searchTableModel.getValueAt(modelRow, 4));

        List<String> details = itemDetailMap.get(player + "_" + idObj);

        Window owner = SwingUtilities.getWindowAncestor(this);
        JDialog dlg = new JDialog(owner, "Chi tiết vật phẩm", Dialog.ModalityType.APPLICATION_MODAL);
        JPanel root = new JPanel(new BorderLayout(SP_LG, SP_LG));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));

        JPanel head = card(new GridLayout(0, 1, 0, SP_XS));
        JLabel titleLbl = label(itemName + "   (ID " + idObj + ")", FONT_H1, TEXT);
        titleLbl.setIcon(vectorIcon("box", 18, PRIMARY));
        titleLbl.setIconTextGap(SP_SM);
        head.add(titleLbl);
        head.add(label("Player: " + player, FONT_BODY, TEXT_SECONDARY));
        head.add(label("Tổng số lượng: " + formatNumber(toLongSafe(qtyObj))
                + "      •      Vị trí: " + loc, FONT_BODY, TEXT_SECONDARY));
        int instanceCount = details == null ? 0 : details.size();
        head.add(label("Số cây đồ riêng lẻ: " + instanceCount
                + "  —  mỗi dòng bên dưới là 1 cây đồ với option riêng", FONT_SMALL, TEXT_MUTED));
        root.add(head, BorderLayout.NORTH);

        StringBuilder body = new StringBuilder();
        if (details == null || details.isEmpty()) {
            body.append("Không có dữ liệu chi tiết (vật phẩm không có option, hoặc hãy bấm \"Làm mới\").");
        } else {
            int n = 1;
            for (String line : details) body.append(n++).append(". ").append(line).append("\n");
        }
        JTextArea area = new JTextArea(body.toString());
        area.setEditable(false);
        area.setFont(FONT_MONO);
        area.setForeground(TEXT);
        area.setBackground(SURFACE);
        area.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setCaretPosition(0);
        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(null);
        sp.setOpaque(false);
        sp.getViewport().setBackground(SURFACE);
        sp.setPreferredSize(new Dimension(680, 340));
        sp.getVerticalScrollBar().setUnitIncrement(16);
        JPanel bodyCard = card(new BorderLayout());
        ((ShadowCard) bodyCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
        bodyCard.add(sp, BorderLayout.CENTER);
        root.add(bodyCard, BorderLayout.CENTER);

        JButton close = button("Đóng", ButtonVariant.PRIMARY, "x");
        close.addActionListener(e -> dlg.dispose());
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        south.setOpaque(false);
        south.add(close);
        root.add(south, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.pack();
        dlg.setLocationRelativeTo(owner);
        dlg.setVisible(true);
    }

    // ======================================================================
    //  TopItemView — tái sử dụng cho mỗi tab xếp hạng theo item id
    // ======================================================================
    private class TopItemView {
        private final int templateId;
        private final String displayName;
        private final Color accent;

        private final List<TopEntry> data = new ArrayList<>();
        private DefaultTableModel tableModel;
        private JTable table;
        private TableRowSorter<DefaultTableModel> sorter;
        private JTextField filterField;
        private JLabel statusLbl;
        private StatTile tilePlayers, tileTotal, tileTopPlayer;

        private JPanel root;

        TopItemView(int templateId, String displayName, Color accent) {
            this.templateId = templateId;
            this.displayName = displayName;
            this.accent = accent;
            build();
        }

        JPanel getRoot() { return root; }

        private void build() {
            root = new JPanel();
            root.setBackground(BG);
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(SP_LG, 0, 0, 0));

            // Summary tiles
            JPanel meta = new JPanel(new GridLayout(1, 3, SP_LG, 0));
            meta.setOpaque(false);
            tilePlayers   = statTile("Số player có item",                "—", PRIMARY, "users");
            tileTotal     = statTile("Tổng " + displayName + " (id " + templateId + ")", "—", accent,
                    templateId == 457 ? "coin" : "box");
            tileTopPlayer = statTile("Top 1",                            "—", WARNING, "star");
            meta.add(tilePlayers);
            meta.add(tileTotal);
            meta.add(tileTopPlayer);
            root.add(meta);
            root.add(Box.createVerticalStrut(SP_LG));

            root.add(buildToolbar());
            root.add(Box.createVerticalStrut(SP_LG));
            root.add(buildTableCard());
        }

        private JPanel buildToolbar() {
            JPanel c = card(new BorderLayout(SP_LG, 0));
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
            left.setOpaque(false);
            left.add(label("Lọc theo tên player:", FONT_SMALL_B, TEXT_SECONDARY));
            filterField = ServerGuiUtils.searchField("Gõ tên để lọc...");
            filterField.setPreferredSize(new Dimension(260, 36));
            left.add(filterField);

            filterField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
            });

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
            right.setOpaque(false);
            statusLbl = label("Đang tải...", FONT_SMALL, TEXT_MUTED);
            right.add(statusLbl);
            JButton btnRefresh = button("Làm mới", ButtonVariant.SECONDARY, "refresh");
            btnRefresh.addActionListener(e -> refresh());
            right.add(btnRefresh);

            c.add(left, BorderLayout.WEST);
            c.add(right, BorderLayout.EAST);
            return c;
        }

        private JPanel buildTableCard() {
            JPanel c = card(new BorderLayout(0, SP_MD));
            c.add(sectionHeader("Top " + displayName,
                    "Xếp hạng theo tổng số " + displayName + " (item id " + templateId + ") của mỗi player",
                    null), BorderLayout.NORTH);

            String[] columns = {"#", "Tên Player", "Hành trang", "Đang mặc", "Rương đồ", "TỔNG"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int r, int col) { return false; }
                @Override public Class<?> getColumnClass(int col) {
                    return col == 0 || col == 2 || col == 3 || col == 4 || col == 5
                            ? Number.class : String.class;
                }
            };
            table = new JTable(tableModel);
            sorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(sorter);

            JScrollPane sp = styleTable(table);
            sp.setPreferredSize(new Dimension(0, 420));

            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(220);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(120);
            table.getColumnModel().getColumn(5).setPreferredWidth(160);

            // Rank cell — bold + medal colors for top 3
            table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected,
                                                               boolean hasFocus, int row, int col) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                    lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setFont(FONT_BODY_B);
                    int rank = -1;
                    try { rank = Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
                    if (!isSelected) {
                        lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                        if (rank == 1)      lbl.setForeground(darken(WARNING, 0.15f));
                        else if (rank == 2) lbl.setForeground(TEXT_MUTED);
                        else if (rank == 3) lbl.setForeground(darken(WARNING, 0.35f));
                        else                lbl.setForeground(TEXT_SECONDARY);
                    }
                    return lbl;
                }
            });
            for (int i = 2; i <= 5; i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(new NumberRenderer());
            }
            // TỔNG column — bold + primary color
            table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected,
                                                               boolean hasFocus, int row, int col) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                    lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    lbl.setFont(FONT_BODY_B);
                    if (v instanceof Number n) lbl.setText(formatNumber(n.longValue()));
                    if (!isSelected) {
                        lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                        lbl.setForeground(PRIMARY);
                    }
                    return lbl;
                }
            });

            c.add(sp, BorderLayout.CENTER);
            return c;
        }

        private void applyFilter() {
            String text = filterField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1));
            }
        }

        void refresh() {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                data.clear();
                statusLbl.setText("Đang quét DB...");
                tilePlayers.setValue("—");
                tileTotal.setValue("—");
                tileTopPlayer.setValue("—");
            });

            new Thread(() -> {
                try (Connection con = DBConnecter.getConnectionServer()) {
                    if (con == null) {
                        SwingUtilities.invokeLater(() -> statusLbl.setText("Lỗi kết nối DB"));
                        return;
                    }
                    List<TopEntry> entries = new ArrayList<>();
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT name, items_bag, items_body, items_box FROM player");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String playerName = rs.getString("name");
                            if (playerName == null || playerName.trim().isEmpty()) continue;
                            long bag  = sumItemQty(rs.getString("items_bag"),  templateId);
                            long body = sumItemQty(rs.getString("items_body"), templateId);
                            long box  = sumItemQty(rs.getString("items_box"),  templateId);
                            long total = bag + body + box;
                            if (total == 0) continue;
                            TopEntry e = new TopEntry();
                            e.playerName = playerName;
                            e.inBag  = bag;
                            e.inBody = body;
                            e.inBox  = box;
                            e.total  = total;
                            entries.add(e);
                        }
                    }
                    entries.sort((a, b) -> Long.compare(b.total, a.total));
                    data.addAll(entries);

                    long grandTotal = entries.stream().mapToLong(e -> e.total).sum();
                    String topName = entries.isEmpty()
                            ? "—"
                            : entries.get(0).playerName + " (" + formatNumber(entries.get(0).total) + ")";

                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        int rank = 1;
                        for (TopEntry e : entries) {
                            tableModel.addRow(new Object[]{
                                    rank++, e.playerName, e.inBag, e.inBody, e.inBox, e.total
                            });
                        }
                        tilePlayers.setValue(String.valueOf(entries.size()));
                        tileTotal.setValue(formatNumber(grandTotal));
                        tileTopPlayer.setValue(topName);
                        statusLbl.setText(entries.size() + " player có " + displayName);
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    final String msg = e.getMessage();
                    SwingUtilities.invokeLater(() -> statusLbl.setText("Lỗi: " + msg));
                }
            }, "Top-" + displayName.replace(' ', '_') + "-Refresh").start();
        }
    }

    /** Một dòng trong bảng top — dùng chung cho mọi TopItemView. */
    private static class TopEntry {
        String playerName;
        long inBag;
        long inBox;
        long inBody;
        long total;
    }

    // ======================================================================
    //  TopThanLinhView — bảng xếp hạng đồ "Thần Linh" (god set, id 555-567)
    //  Đếm số món Thần Linh mỗi player sở hữu (Đang mặc/Hành trang/Rương đồ);
    //  nhấp đúp 1 dòng để xem chi tiết từng món (vị trí + option đã giải mã).
    // ======================================================================
    private class TopThanLinhView {
        private DefaultTableModel tableModel;
        private JTable table;
        private TableRowSorter<DefaultTableModel> sorter;
        private JTextField filterField;
        private JLabel statusLbl;
        private StatTile tilePlayers, tileTotal, tileTopPlayer;
        private JPanel root;
        /** key = tên player → danh sách dòng chi tiết từng món Thần Linh. */
        private final Map<String, List<String>> detailByPlayer = new HashMap<>();

        TopThanLinhView() { build(); }

        JPanel getRoot() { return root; }

        private void build() {
            root = new JPanel();
            root.setBackground(BG);
            root.setLayout(new BoxLayout(root, BoxLayout.Y_AXIS));
            root.setBorder(new EmptyBorder(SP_LG, 0, 0, 0));

            JPanel meta = new JPanel(new GridLayout(1, 3, SP_LG, 0));
            meta.setOpaque(false);
            tilePlayers   = statTile("Số player có đồ Thần Linh",      "—", PRIMARY, "users");
            tileTotal     = statTile("Tổng món Thần Linh (id 555-567)", "—", SUCCESS, "diamond");
            tileTopPlayer = statTile("Top 1",                           "—", WARNING, "star");
            meta.add(tilePlayers);
            meta.add(tileTotal);
            meta.add(tileTopPlayer);
            root.add(meta);
            root.add(Box.createVerticalStrut(SP_LG));

            root.add(buildToolbar());
            root.add(Box.createVerticalStrut(SP_LG));
            root.add(buildTableCard());
        }

        private JPanel buildToolbar() {
            JPanel c = card(new BorderLayout(SP_LG, 0));
            c.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

            JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
            left.setOpaque(false);
            left.add(label("Lọc theo tên player:", FONT_SMALL_B, TEXT_SECONDARY));
            filterField = ServerGuiUtils.searchField("Gõ tên để lọc...");
            filterField.setPreferredSize(new Dimension(260, 36));
            left.add(filterField);
            filterField.getDocument().addDocumentListener(new DocumentListener() {
                @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
                @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
            });

            JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
            right.setOpaque(false);
            statusLbl = label("Đang tải...", FONT_SMALL, TEXT_MUTED);
            right.add(statusLbl);
            JButton btnRefresh = button("Làm mới", ButtonVariant.SECONDARY, "refresh");
            btnRefresh.addActionListener(e -> refresh());
            right.add(btnRefresh);

            c.add(left, BorderLayout.WEST);
            c.add(right, BorderLayout.EAST);
            return c;
        }

        private JPanel buildTableCard() {
            JPanel c = card(new BorderLayout(0, SP_MD));
            c.add(sectionHeader("Top Đồ Thần Linh",
                    "Xếp hạng theo tổng số món Thần Linh (id 555-567). Nhấp đúp 1 dòng để xem chi tiết từng món.",
                    null), BorderLayout.NORTH);

            String[] columns = {"#", "Tên Player", "Đang mặc", "Hành trang", "Rương đồ", "TỔNG"};
            tableModel = new DefaultTableModel(columns, 0) {
                @Override public boolean isCellEditable(int r, int col) { return false; }
                @Override public Class<?> getColumnClass(int col) {
                    return col == 1 ? String.class : Number.class;
                }
            };
            table = new JTable(tableModel);
            sorter = new TableRowSorter<>(tableModel);
            table.setRowSorter(sorter);

            JScrollPane sp = styleTable(table);
            sp.setPreferredSize(new Dimension(0, 420));

            table.getColumnModel().getColumn(0).setPreferredWidth(50);
            table.getColumnModel().getColumn(1).setPreferredWidth(220);
            table.getColumnModel().getColumn(2).setPreferredWidth(120);
            table.getColumnModel().getColumn(3).setPreferredWidth(120);
            table.getColumnModel().getColumn(4).setPreferredWidth(120);
            table.getColumnModel().getColumn(5).setPreferredWidth(120);

            // Rank cell — bold + medal colors for top 3
            table.getColumnModel().getColumn(0).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected,
                                                               boolean hasFocus, int row, int col) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                    lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    lbl.setFont(FONT_BODY_B);
                    int rank = -1;
                    try { rank = Integer.parseInt(String.valueOf(v)); } catch (Exception ignored) {}
                    if (!isSelected) {
                        lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                        if (rank == 1)      lbl.setForeground(darken(WARNING, 0.15f));
                        else if (rank == 2) lbl.setForeground(TEXT_MUTED);
                        else if (rank == 3) lbl.setForeground(darken(WARNING, 0.35f));
                        else                lbl.setForeground(TEXT_SECONDARY);
                    }
                    return lbl;
                }
            });
            for (int i = 2; i <= 4; i++) {
                table.getColumnModel().getColumn(i).setCellRenderer(new NumberRenderer());
            }
            // TỔNG column — bold + primary color
            table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
                @Override
                public Component getTableCellRendererComponent(JTable t, Object v, boolean isSelected,
                                                               boolean hasFocus, int row, int col) {
                    JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, isSelected, hasFocus, row, col);
                    lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                    lbl.setFont(FONT_BODY_B);
                    if (v instanceof Number n) lbl.setText(formatNumber(n.longValue()));
                    if (!isSelected) {
                        lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                        lbl.setForeground(PRIMARY);
                    }
                    return lbl;
                }
            });

            // Nhấp đúp / Enter để xem chi tiết các món Thần Linh của player
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    if (e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton(e)) showDetail();
                }
            });
            table.getInputMap(JComponent.WHEN_FOCUSED)
                    .put(KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ENTER, 0), "viewThanLinhDetail");
            table.getActionMap().put("viewThanLinhDetail", new AbstractAction() {
                @Override public void actionPerformed(java.awt.event.ActionEvent e) { showDetail(); }
            });

            c.add(sp, BorderLayout.CENTER);
            return c;
        }

        private void applyFilter() {
            String text = filterField.getText().trim();
            if (text.isEmpty()) {
                sorter.setRowFilter(null);
            } else {
                sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + java.util.regex.Pattern.quote(text), 1));
            }
        }

        void refresh() {
            SwingUtilities.invokeLater(() -> {
                tableModel.setRowCount(0);
                detailByPlayer.clear();
                statusLbl.setText("Đang quét DB...");
                tilePlayers.setValue("—");
                tileTotal.setValue("—");
                tileTopPlayer.setValue("—");
            });

            new Thread(() -> {
                try (Connection con = DBConnecter.getConnectionServer()) {
                    if (con == null) {
                        SwingUtilities.invokeLater(() -> statusLbl.setText("Lỗi kết nối DB"));
                        return;
                    }
                    List<Object[]> rows = new ArrayList<>();
                    Map<String, List<String>> details = new HashMap<>();
                    try (PreparedStatement ps = con.prepareStatement(
                            "SELECT name, items_bag, items_body, items_box FROM player");
                         ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String playerName = rs.getString("name");
                            if (playerName == null || playerName.trim().isEmpty()) continue;
                            List<String> dets = new ArrayList<>();
                            long body = collect(rs.getString("items_body"), "Đang mặc",   dets);
                            long bag  = collect(rs.getString("items_bag"),  "Hành trang",  dets);
                            long box  = collect(rs.getString("items_box"),  "Rương đồ",    dets);
                            long total = body + bag + box;
                            if (total == 0) continue;
                            rows.add(new Object[]{playerName, body, bag, box, total});
                            details.put(playerName, dets);
                        }
                    }
                    rows.sort((a, b) -> Long.compare(
                            ((Number) b[4]).longValue(), ((Number) a[4]).longValue()));

                    long grandTotal = rows.stream().mapToLong(r -> ((Number) r[4]).longValue()).sum();
                    String topName = rows.isEmpty()
                            ? "—"
                            : rows.get(0)[0] + " (" + formatNumber(((Number) rows.get(0)[4]).longValue()) + ")";

                    SwingUtilities.invokeLater(() -> {
                        tableModel.setRowCount(0);
                        detailByPlayer.clear();
                        detailByPlayer.putAll(details);
                        int rank = 1;
                        for (Object[] r : rows) {
                            tableModel.addRow(new Object[]{rank++, r[0], r[1], r[2], r[3], r[4]});
                        }
                        tilePlayers.setValue(String.valueOf(rows.size()));
                        tileTotal.setValue(formatNumber(grandTotal));
                        tileTopPlayer.setValue(topName);
                        statusLbl.setText(rows.size() + " player có đồ Thần Linh");
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    final String msg = e.getMessage();
                    SwingUtilities.invokeLater(() -> statusLbl.setText("Lỗi: " + msg));
                }
            }, "Top-ThanLinh-Refresh").start();
        }

        /** Đếm số món Thần Linh (id 555-567) trong 1 chuỗi JSON inventory + ghi chi tiết từng món. */
        private long collect(String json, String location, List<String> detailOut) {
            if (json == null || json.trim().isEmpty()) return 0;
            long count = 0;
            try {
                Object parsed = JSONValue.parse(json);
                JSONArray dataArray = null;
                if (parsed instanceof JSONArray) {
                    dataArray = (JSONArray) parsed;
                } else if (parsed instanceof String) {
                    Object p2 = JSONValue.parse((String) parsed);
                    if (p2 instanceof JSONArray) dataArray = (JSONArray) p2;
                }
                if (dataArray == null) return 0;
                for (int i = 0; i < dataArray.size(); i++) {
                    try {
                        Object itemObj = dataArray.get(i);
                        JSONArray dataItem = itemObj instanceof JSONArray
                                ? (JSONArray) itemObj
                                : (JSONArray) JSONValue.parse(itemObj.toString());
                        if (dataItem == null || dataItem.size() < 2) continue;
                        int tempId = Integer.parseInt(String.valueOf(dataItem.get(0)));
                        if (tempId < THAN_LINH_MIN || tempId > THAN_LINH_MAX) continue;
                        count++;
                        String nm = resolveItemName(tempId);
                        String itemName = (nm != null && !nm.isEmpty()) ? (nm + " (ID " + tempId + ")") : ("ID " + tempId);
                        String opts = decodeOptions(dataItem);
                        detailOut.add("[" + location + "] " + itemName
                                + (opts.isEmpty() ? "  —  (không có chỉ số phụ)" : "  —  " + opts));
                    } catch (Exception ignored) {}
                }
            } catch (Exception ignored) {}
            return count;
        }

        /** Dialog chi tiết: liệt kê từng món Thần Linh của player (nhấp đúp 1 dòng). */
        private void showDetail() {
            int viewRow = table.getSelectedRow();
            if (viewRow < 0) return;
            int modelRow = table.convertRowIndexToModel(viewRow);
            if (modelRow < 0 || modelRow >= tableModel.getRowCount()) return;
            String player = String.valueOf(tableModel.getValueAt(modelRow, 1));
            Object total  = tableModel.getValueAt(modelRow, 5);
            List<String> details = detailByPlayer.get(player);

            Window owner = SwingUtilities.getWindowAncestor(ItemSearchPanel.this);
            JDialog dlg = new JDialog(owner, "Chi tiết đồ Thần Linh", Dialog.ModalityType.APPLICATION_MODAL);
            JPanel rootP = new JPanel(new BorderLayout(SP_LG, SP_LG));
            rootP.setBackground(BG);
            rootP.setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));

            JPanel head = card(new GridLayout(0, 1, 0, SP_XS));
            JLabel titleLbl = label("Đồ Thần Linh của " + player, FONT_H1, TEXT);
            titleLbl.setIcon(vectorIcon("diamond", 18, PRIMARY));
            titleLbl.setIconTextGap(SP_SM);
            head.add(titleLbl);
            head.add(label("Tổng số món: " + total
                    + "  —  mỗi dòng bên dưới là 1 món với option riêng", FONT_BODY, TEXT_SECONDARY));
            rootP.add(head, BorderLayout.NORTH);

            StringBuilder body = new StringBuilder();
            if (details == null || details.isEmpty()) {
                body.append("Không có dữ liệu chi tiết (hãy bấm \"Làm mới\").");
            } else {
                int n = 1;
                for (String line : details) body.append(n++).append(". ").append(line).append("\n");
            }
            JTextArea area = new JTextArea(body.toString());
            area.setEditable(false);
            area.setFont(FONT_MONO);
            area.setForeground(TEXT);
            area.setBackground(SURFACE);
            area.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));
            area.setLineWrap(true);
            area.setWrapStyleWord(true);
            area.setCaretPosition(0);
            JScrollPane sp = new JScrollPane(area);
            sp.setBorder(null);
            sp.setOpaque(false);
            sp.getViewport().setBackground(SURFACE);
            sp.setPreferredSize(new Dimension(680, 340));
            sp.getVerticalScrollBar().setUnitIncrement(16);
            JPanel bodyCard = card(new BorderLayout());
            ((ShadowCard) bodyCard).setContentPadding(SP_SM, SP_SM, SP_SM, SP_SM);
            bodyCard.add(sp, BorderLayout.CENTER);
            rootP.add(bodyCard, BorderLayout.CENTER);

            JButton close = button("Đóng", ButtonVariant.PRIMARY, "x");
            close.addActionListener(e -> dlg.dispose());
            JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            south.setOpaque(false);
            south.add(close);
            rootP.add(south, BorderLayout.SOUTH);

            dlg.setContentPane(rootP);
            dlg.pack();
            dlg.setLocationRelativeTo(owner);
            dlg.setVisible(true);
        }
    }

    /** Sum quantity of all items with the given template id inside a JSON inventory string. */
    private static long sumItemQty(String json, int targetTempId) {
        if (json == null || json.trim().isEmpty()) return 0;
        try {
            Object parsed = JSONValue.parse(json);
            JSONArray dataArray = null;
            if (parsed instanceof JSONArray) {
                dataArray = (JSONArray) parsed;
            } else if (parsed instanceof String) {
                Object parsed2 = JSONValue.parse((String) parsed);
                if (parsed2 instanceof JSONArray) dataArray = (JSONArray) parsed2;
            }
            if (dataArray == null) return 0;
            long sum = 0;
            for (int i = 0; i < dataArray.size(); i++) {
                try {
                    Object itemObj = dataArray.get(i);
                    JSONArray dataItem = itemObj instanceof JSONArray
                            ? (JSONArray) itemObj
                            : (JSONArray) JSONValue.parse(itemObj.toString());
                    if (dataItem == null || dataItem.size() < 2) continue;
                    int tempId = Integer.parseInt(String.valueOf(dataItem.get(0)));
                    if (tempId != targetTempId) continue;
                    long qty = Long.parseLong(String.valueOf(dataItem.get(1)));
                    if (qty > 0) sum += qty;
                } catch (Exception ignored) {}
            }
            return sum;
        } catch (Exception ignored) {
            return 0;
        }
    }

    // ----------------------------------------------------------------------
    //  Helpers
    // ----------------------------------------------------------------------
    private static String formatNumber(long num) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(num);
    }

    /** Right-aligned number renderer with thousands separator. */
    private static class NumberRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                                                       boolean hasFocus, int row, int column) {
            JLabel lbl = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
            lbl.setHorizontalAlignment(SwingConstants.RIGHT);
            if (!isSelected) {
                lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                lbl.setForeground(TEXT);
            }
            if (value instanceof Number n) {
                lbl.setText(formatNumber(n.longValue()));
            } else if (value != null) {
                try { lbl.setText(formatNumber(Long.parseLong(String.valueOf(value)))); }
                catch (Exception e) { lbl.setText(String.valueOf(value)); }
            }
            return lbl;
        }
    }
}
