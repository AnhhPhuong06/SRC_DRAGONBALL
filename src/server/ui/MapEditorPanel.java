package server.ui;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jdbc.DBConnecter;
import models.Template.MapTemplate;
import models.map.EffectMap;
import models.map.WayPoint;
import models.map.Zone;
import server.Manager;
import services.Service;
import services.map.ChangeMapService;
import utils.Logger;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static server.ui.ServerGuiUtils.*;

/**
 * Bảng quản trị & Chỉnh sửa Bản Đồ (Map Editor).
 * 
 * Hỗ trợ:
 * - Tìm kiếm & lọc bản đồ theo Hành Tinh, Loại Map, ID, Tên.
 * - Sửa thông tin cấu hình Map: Tên, Số khu (Zones), Max player, Tile ID, Bg ID, Hành tinh...
 * - Quản lý Quái vật (Mobs) trên Map: Thêm / Sửa / Xóa / Nhân bản với tên gợi ý từ MobTemplate.
 * - Quản lý NPC trên Map: Thêm / Sửa / Xóa với tên gợi ý từ NpcTemplate.
 * - Quản lý Điểm dịch chuyển (WayPoints): Cổng vào, tọa độ, bản đồ đích đến.
 * - Giám sát Realtime: Xem các khu vực & số lượng người chơi đang online trên Map hiện tại.
 * - Hot Reload / Live Sync: Cập nhật trực tiếp vào bộ nhớ Server đang chạy mà không cần khởi động lại.
 */
public class MapEditorPanel extends JPanel {

    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private static final JsonParser JSON_PARSER = new JsonParser();

    // Cache template từ database để hiển thị tên thân thiện thay vì ID thô
    private final Map<Integer, String> npcNameMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> mobNameMap = new ConcurrentHashMap<>();
    private final Map<Integer, Long> mobDefaultHpMap = new ConcurrentHashMap<>();
    private final Map<Integer, String> mapNameLookup = new ConcurrentHashMap<>();

    // Danh sách toàn bộ map tải từ DB
    private final List<MapItem> allMaps = new ArrayList<>();
    private MapItem currentSelectedMap = null;
    private final AtomicBoolean isLoading = new AtomicBoolean(false);

    // Bảng danh sách Map (bên trái)
    private JTable mapTable;
    private DefaultTableModel mapTableModel;
    private TableRowSorter<DefaultTableModel> mapTableSorter;
    private JTextField txtSearch;
    private JComboBox<String> cboPlanetFilter;
    private JLabel lblTotalMaps;

    // Component chi tiết Map (bên phải)
    private JLabel lblCurrentMapTitle;
    private JLabel lblCurrentMapBadge;
    private JTextField txtMapId;
    private JTextField txtMapName;
    private JComboBox<PlanetOption> cboPlanet;
    private JComboBox<TypeOption> cboMapType;
    private JSpinner spZones;
    private JSpinner spMaxPlayer;
    private JSpinner spTileId;
    private JSpinner spBgId;
    private JSpinner spBgType;
    private JCheckBox chkMapDouble;

    // Sub-tables
    private DefaultTableModel mobTableModel;
    private JTable mobTable;
    private DefaultTableModel npcTableModel;
    private JTable npcTable;
    private DefaultTableModel wpTableModel;
    private JTable wpTable;
    private DefaultTableModel liveZoneTableModel;
    private JTable liveZoneTable;
    private JLabel lblLiveStatus;

    public MapEditorPanel() {
        setLayout(new BorderLayout(SP_SM, SP_SM));
        setBackground(BG);
        setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));

        initUI();
        setupShortcuts();
        loadDataAsync();
    }

    private void setupShortcuts() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();
        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_S, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx()), "saveMap");
        am.put("saveMap", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                saveCurrentMap();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "refreshData");
        am.put("refreshData", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadDataAsync();
            }
        });
    }

    private void initUI() {
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buildLeftPanel(), buildRightPanel());
        splitPane.setDividerLocation(460);
        splitPane.setResizeWeight(0.35);
        splitPane.setBorder(null);
        splitPane.setBackground(BG);
        splitPane.setDividerSize(6);

        add(splitPane, BorderLayout.CENTER);
    }

    // =========================================================================
    //  PANEL TRÁI: DANH SÁCH BẢN ĐỒ & TÌM KIẾM
    // =========================================================================
    private JPanel buildLeftPanel() {
        JPanel cardPanel = card();
        cardPanel.setLayout(new BorderLayout(0, SP_SM));

        // Header tìm kiếm & bộ lọc
        JPanel topFilter = new JPanel(new BorderLayout(0, SP_XS));
        topFilter.setOpaque(false);

        JPanel headerRow = new JPanel(new BorderLayout());
        headerRow.setOpaque(false);
        JLabel titleLbl = new JLabel("Danh Sách Bản Đồ", vectorIcon("map", 18, PRIMARY), SwingConstants.LEFT);
        titleLbl.setFont(FONT_H2);
        titleLbl.setForeground(TEXT);
        lblTotalMaps = new JLabel("0 map", SwingConstants.RIGHT);
        lblTotalMaps.setFont(FONT_TINY_B);
        lblTotalMaps.setForeground(TEXT_MUTED);
        headerRow.add(titleLbl, BorderLayout.WEST);
        headerRow.add(lblTotalMaps, BorderLayout.EAST);
        topFilter.add(headerRow, BorderLayout.NORTH);

        JPanel searchRow = new JPanel(new BorderLayout(SP_XS, 0));
        searchRow.setOpaque(false);
        txtSearch = searchField("Tìm ID, tên map...");
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });

        cboPlanetFilter = new JComboBox<>(new String[]{"Tất cả hành tinh", "Trái Đất", "Namếc", "Xayda", "Khác/Vũ trụ"});
        cboPlanetFilter.setFont(FONT_SMALL);
        cboPlanetFilter.setPreferredSize(new Dimension(130, 32));
        cboPlanetFilter.addActionListener(e -> applyFilter());

        searchRow.add(txtSearch, BorderLayout.CENTER);
        searchRow.add(cboPlanetFilter, BorderLayout.EAST);
        topFilter.add(searchRow, BorderLayout.SOUTH);

        cardPanel.add(topFilter, BorderLayout.NORTH);

        // Bảng danh sách Map
        String[] cols = {"ID", "Tên Bản Đồ", "Hành Tinh", "Khu", "Quái", "NPC", "Cổng"};
        mapTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
            @Override public Class<?> getColumnClass(int col) {
                if (col == 0 || col == 3 || col == 4 || col == 5 || col == 6) return Integer.class;
                return String.class;
            }
        };
        mapTable = new JTable(mapTableModel);
        mapTableSorter = new TableRowSorter<>(mapTableModel);
        mapTable.setRowSorter(mapTableSorter);

        mapTable.getColumnModel().getColumn(0).setPreferredWidth(45);
        mapTable.getColumnModel().getColumn(1).setPreferredWidth(140);
        mapTable.getColumnModel().getColumn(2).setPreferredWidth(80);
        mapTable.getColumnModel().getColumn(3).setPreferredWidth(45);
        mapTable.getColumnModel().getColumn(4).setPreferredWidth(45);
        mapTable.getColumnModel().getColumn(5).setPreferredWidth(45);
        mapTable.getColumnModel().getColumn(6).setPreferredWidth(45);

        // Renderer cho cột hành tinh
        mapTable.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object val, boolean isSel, boolean hasFocus, int row, int col) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, val, isSel, hasFocus, row, col);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                String s = val == null ? "" : val.toString();
                if (!isSel) {
                    if (s.contains("Trái Đất")) {
                        l.setForeground(new Color(0x0284C7));
                    } else if (s.contains("Namếc")) {
                        l.setForeground(new Color(0x059669));
                    } else if (s.contains("Xayda")) {
                        l.setForeground(new Color(0xD97706));
                    } else {
                        l.setForeground(new Color(0x7C3AED));
                    }
                }
                return l;
            }
        });

        mapTable.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                int row = mapTable.getSelectedRow();
                if (row != -1) {
                    int modelRow = mapTable.convertRowIndexToModel(row);
                    int mapId = (int) mapTableModel.getValueAt(modelRow, 0);
                    selectMapById(mapId);
                }
            }
        });

        JScrollPane scrollTable = styleTable(mapTable);
        cardPanel.add(scrollTable, BorderLayout.CENTER);

        // Thanh thao tác dưới danh sách
        JPanel bottomBar = new JPanel(new GridLayout(1, 3, SP_XS, 0));
        bottomBar.setOpaque(false);

        JButton btnReload = button("Làm Mới", ButtonVariant.SECONDARY, "refresh");
        btnReload.addActionListener(e -> loadDataAsync());

        JButton btnAdd = button("+ Thêm Map", ButtonVariant.SECONDARY, "plus");
        btnAdd.addActionListener(e -> showCreateMapDialog());

        JButton btnDelete = button("Xóa Map", ButtonVariant.DANGER, "trash");
        btnDelete.addActionListener(e -> deleteCurrentMap());

        bottomBar.add(btnReload);
        bottomBar.add(btnAdd);
        bottomBar.add(btnDelete);
        cardPanel.add(bottomBar, BorderLayout.SOUTH);

        return cardPanel;
    }

    // =========================================================================
    //  PANEL PHẢI: CHI TIẾT & CHỈNH SỬA MAP
    // =========================================================================
    private JPanel buildRightPanel() {
        JPanel cardPanel = card();
        cardPanel.setLayout(new BorderLayout(0, SP_SM));

        // Header Panel chi tiết
        JPanel headerPanel = new JPanel(new BorderLayout(SP_MD, 0));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(new EmptyBorder(0, 0, SP_XS, 0));

        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        titleBox.setOpaque(false);
        lblCurrentMapTitle = new JLabel("Chưa chọn bản đồ nào");
        lblCurrentMapTitle.setFont(FONT_H1);
        lblCurrentMapTitle.setForeground(TEXT);

        lblCurrentMapBadge = new JLabel("ID: --");
        lblCurrentMapBadge.setFont(FONT_TINY_B);
        lblCurrentMapBadge.setForeground(PRIMARY);
        lblCurrentMapBadge.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_SOFT, 1, true),
                new EmptyBorder(2, 6, 2, 6)
        ));

        titleBox.add(lblCurrentMapTitle);
        titleBox.add(lblCurrentMapBadge);
        headerPanel.add(titleBox, BorderLayout.WEST);

        // Các nút hành động chính
        JPanel actionBox = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_XS, 0));
        actionBox.setOpaque(false);

        JButton btnSyncServer = button("Đồng Bộ Live", ButtonVariant.SECONDARY, "server");
        btnSyncServer.setToolTipText("Cập nhật trực tiếp vào bộ nhớ Server đang chạy");
        btnSyncServer.addActionListener(e -> syncToLiveServer());

        JButton btnClone = button("Nhân Bản", ButtonVariant.SECONDARY, "box");
        btnClone.setToolTipText("Tạo bản sao của map này sang ID mới");
        btnClone.addActionListener(e -> cloneCurrentMap());

        JButton btnSave = button("Lưu Thay Đổi (Ctrl+S)", ButtonVariant.PRIMARY, "save");
        btnSave.addActionListener(e -> saveCurrentMap());

        actionBox.add(btnSyncServer);
        actionBox.add(btnClone);
        actionBox.add(btnSave);
        headerPanel.add(actionBox, BorderLayout.EAST);

        cardPanel.add(headerPanel, BorderLayout.NORTH);

        // Tabbed Pane cho các mục chi tiết
        JTabbedPane detailTabs = new JTabbedPane();
        detailTabs.setFont(FONT_BODY_B);

        detailTabs.addTab("Thông Tin Chung", vectorIcon("settings", 14, PRIMARY), buildGeneralTab());
        detailTabs.addTab("Quái Vật (Mobs)", vectorIcon("boss", 14, DANGER), buildMobTab());
        detailTabs.addTab("NPC", vectorIcon("user", 14, INFO), buildNpcTab());
        detailTabs.addTab("Điểm Dịch Chuyển (WayPoints)", vectorIcon("map", 14, SUCCESS), buildWaypointTab());
        detailTabs.addTab("Giám Sát Live Server", vectorIcon("chart", 14, VIOLET), buildLiveMonitorTab());

        cardPanel.add(detailTabs, BorderLayout.CENTER);

        return cardPanel;
    }

    // --- TAB 1: THÔNG TIN CHUNG ---
    private JPanel buildGeneralTab() {
        JPanel p = new JPanel(new BorderLayout(SP_MD, SP_MD));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(6, 6, 6, 6);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtMapId = textField("");
        txtMapId.setEditable(false);
        txtMapId.setBackground(SURFACE_ALT);

        txtMapName = textField("Tên bản đồ...");

        cboPlanet = new JComboBox<>(new PlanetOption[]{
                new PlanetOption(0, "0 - Trái Đất"),
                new PlanetOption(1, "1 - Namếc"),
                new PlanetOption(2, "2 - Xayda"),
                new PlanetOption(3, "3 - Không gian / Khác")
        });

        cboMapType = new JComboBox<>(new TypeOption[]{
                new TypeOption(0, "0 - Bản đồ bình thường"),
                new TypeOption(1, "1 - Bản đồ offline (nhà/vực)"),
                new TypeOption(2, "2 - Doanh Trại Độc Nhãn"),
                new TypeOption(3, "3 - Ngọc Rồng Sao Đen"),
                new TypeOption(4, "4 - Bản Đồ Kho Báu"),
                new TypeOption(5, "5 - Con Đường Rắn Độc"),
                new TypeOption(6, "6 - Khí Gas Hủy Diệt"),
                new TypeOption(7, "7 - Ma Bưu 14H / 2H")
        });

        spZones = new JSpinner(new SpinnerNumberModel(15, 1, 100, 1));
        spMaxPlayer = new JSpinner(new SpinnerNumberModel(12, 1, 100, 1));
        spTileId = new JSpinner(new SpinnerNumberModel(1, 1, 50, 1));
        spBgId = new JSpinner(new SpinnerNumberModel(0, 0, 100, 1));
        spBgType = new JSpinner(new SpinnerNumberModel(0, 0, 10, 1));
        chkMapDouble = new JCheckBox("Bản đồ kép (is_map_double)");
        chkMapDouble.setOpaque(false);
        chkMapDouble.setFont(FONT_BODY);

        // Dòng 1: ID & Tên
        addFormRow(form, g, 0, "Mã Bản Đồ (ID):", txtMapId, "Tên Bản Đồ:", txtMapName);
        // Dòng 2: Hành tinh & Loại Map
        addFormRow(form, g, 1, "Hành Tinh:", cboPlanet, "Loại Bản Đồ:", cboMapType);
        // Dòng 3: Số khu & Tối đa người
        addFormRow(form, g, 2, "Số Khu Vực (Zones):", spZones, "Người Tối Đa / Khu:", spMaxPlayer);
        // Dòng 4: Tile & Background
        addFormRow(form, g, 3, "Tile ID (Gạch nền):", spTileId, "Background ID:", spBgId);
        // Dòng 5: Bg Type & Double Map
        addFormRow(form, g, 4, "Background Type:", spBgType, "Tùy chọn khác:", chkMapDouble);

        p.add(form, BorderLayout.NORTH);

        // Hướng dẫn & lưu ý tối ưu hiệu năng
        JPanel tipCard = new JPanel(new BorderLayout(SP_SM, SP_SM));
        tipCard.setBackground(PRIMARY_SOFT);
        tipCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xC7D2FE), 1, true),
                new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM)
        ));

        JLabel tipIcon = new JLabel(vectorIcon("info", 20, PRIMARY));
        JLabel tipText = new JLabel("<html><b>Lưu ý hiệu năng server:</b><br/>"
                + "• Số người tối đa / khu nên để từ <b>12 đến 15 người</b> đối với map bình thường.<br/>"
                + "• Đối với 3 map nhà (21: Gôhan, 22: Moori, 23: Paragus), khuyến nghị giữ max_player = 15 để tránh lag packet.<br/>"
                + "• Sau khi chỉnh sửa, bạn có thể bấm <b>'Đồng Bộ Live'</b> để áp dụng trực tiếp cho người chơi trong game.</html>");
        tipText.setFont(FONT_SMALL);
        tipText.setForeground(TEXT);

        tipCard.add(tipIcon, BorderLayout.WEST);
        tipCard.add(tipText, BorderLayout.CENTER);
        p.add(tipCard, BorderLayout.CENTER);

        return p;
    }

    private void addFormRow(JPanel panel, GridBagConstraints g, int row, String lbl1, JComponent c1, String lbl2, JComponent c2) {
        g.gridy = row;

        g.gridx = 0; g.weightx = 0.12;
        JLabel l1 = new JLabel(lbl1);
        l1.setFont(FONT_BODY_B);
        l1.setForeground(TEXT_SECONDARY);
        panel.add(l1, g);

        g.gridx = 1; g.weightx = 0.38;
        panel.add(c1, g);

        g.gridx = 2; g.weightx = 0.12;
        JLabel l2 = new JLabel(lbl2);
        l2.setFont(FONT_BODY_B);
        l2.setForeground(TEXT_SECONDARY);
        panel.add(l2, g);

        g.gridx = 3; g.weightx = 0.38;
        panel.add(c2, g);
    }

    // --- TAB 2: QUÁI VẬT (MOBS) ---
    private JPanel buildMobTab() {
        JPanel p = new JPanel(new BorderLayout(0, SP_SM));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        bar.setOpaque(false);

        JButton btnAddMob = button("+ Thêm Quái", ButtonVariant.SECONDARY, "plus");
        btnAddMob.addActionListener(e -> showAddMobDialog());

        JButton btnEditMob = button("Sửa Quái", ButtonVariant.SECONDARY, "edit");
        btnEditMob.addActionListener(e -> editSelectedMob());

        JButton btnDupMob = button("Nhân Đôi", ButtonVariant.SECONDARY, "box");
        btnDupMob.addActionListener(e -> duplicateSelectedMob());

        JButton btnDelMob = button("Xóa Quái", ButtonVariant.DANGER, "trash");
        btnDelMob.addActionListener(e -> deleteSelectedMob());

        bar.add(btnAddMob);
        bar.add(btnEditMob);
        bar.add(btnDupMob);
        bar.add(btnDelMob);

        p.add(bar, BorderLayout.NORTH);

        String[] cols = {"STT", "Tên Quái", "Mob Temp ID", "Cấp Độ", "Máu (HP)", "Tọa Độ X", "Tọa Độ Y"};
        mobTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        mobTable = new JTable(mobTableModel);
        mobTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedMob();
            }
        });

        p.add(styleTable(mobTable), BorderLayout.CENTER);
        return p;
    }

    // --- TAB 3: NPC ---
    private JPanel buildNpcTab() {
        JPanel p = new JPanel(new BorderLayout(0, SP_SM));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        bar.setOpaque(false);

        JButton btnAddNpc = button("+ Thêm NPC", ButtonVariant.SECONDARY, "plus");
        btnAddNpc.addActionListener(e -> showAddNpcDialog());

        JButton btnEditNpc = button("Sửa NPC", ButtonVariant.SECONDARY, "edit");
        btnEditNpc.addActionListener(e -> editSelectedNpc());

        JButton btnDelNpc = button("Xóa NPC", ButtonVariant.DANGER, "trash");
        btnDelNpc.addActionListener(e -> deleteSelectedNpc());

        bar.add(btnAddNpc);
        bar.add(btnEditNpc);
        bar.add(btnDelNpc);

        p.add(bar, BorderLayout.NORTH);

        String[] cols = {"STT", "Tên NPC", "NPC ID", "Tọa Độ X", "Tọa Độ Y", "Avatar"};
        npcTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        npcTable = new JTable(npcTableModel);
        npcTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedNpc();
            }
        });

        p.add(styleTable(npcTable), BorderLayout.CENTER);
        return p;
    }

    // --- TAB 4: ĐIỂM DỊCH CHUYỂN (WAYPOINTS) ---
    private JPanel buildWaypointTab() {
        JPanel p = new JPanel(new BorderLayout(0, SP_SM));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(SP_SM, SP_SM, SP_SM, SP_SM));

        JPanel bar = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        bar.setOpaque(false);

        JButton btnAddWp = button("+ Thêm Cổng Dịch Chuyển", ButtonVariant.SECONDARY, "plus");
        btnAddWp.addActionListener(e -> showAddWaypointDialog());

        JButton btnEditWp = button("Sửa Cổng", ButtonVariant.SECONDARY, "edit");
        btnEditWp.addActionListener(e -> editSelectedWaypoint());

        JButton btnDelWp = button("Xóa Cổng", ButtonVariant.DANGER, "trash");
        btnDelWp.addActionListener(e -> deleteSelectedWaypoint());

        bar.add(btnAddWp);
        bar.add(btnEditWp);
        bar.add(btnDelWp);

        p.add(bar, BorderLayout.NORTH);

        String[] cols = {"Tên Cổng", "Phạm Vi Vào (X1..X2, Y1..Y2)", "Bấm Enter", "Offline", "Tới Bản Đồ", "Tới Tọa Độ X", "Tới Tọa Độ Y"};
        wpTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        wpTable = new JTable(wpTableModel);
        wpTable.addMouseListener(new MouseAdapter() {
            @Override public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelectedWaypoint();
            }
        });

        p.add(styleTable(wpTable), BorderLayout.CENTER);
        return p;
    }

    // --- TAB 5: GIÁM SÁT LIVE SERVER ---
    private JPanel buildLiveMonitorTab() {
        JPanel p = new JPanel(new BorderLayout(SP_SM, SP_SM));
        p.setBackground(SURFACE);
        p.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        JPanel topStatus = new JPanel(new BorderLayout());
        topStatus.setOpaque(false);

        lblLiveStatus = new JLabel("Trạng thái: Đang kiểm tra...", vectorIcon("server", 16, PRIMARY), SwingConstants.LEFT);
        lblLiveStatus.setFont(FONT_H2);
        topStatus.add(lblLiveStatus, BorderLayout.WEST);

        JPanel liveActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_XS, 0));
        liveActions.setOpaque(false);

        JButton btnRefreshLive = button("Cập Nhật Realtime", ButtonVariant.SECONDARY, "refresh");
        btnRefreshLive.addActionListener(e -> refreshLiveZoneStatus());

        JButton btnTeleHome = button("Đưa Người Chơi Về Nhà", ButtonVariant.DANGER, "broom");
        btnTeleHome.addActionListener(e -> teleportPlayersHome());

        liveActions.add(btnRefreshLive);
        liveActions.add(btnTeleHome);
        topStatus.add(liveActions, BorderLayout.EAST);

        p.add(topStatus, BorderLayout.NORTH);

        String[] cols = {"Khu Vực (Zone ID)", "Số Người Hiện Tại", "Tối Đa", "Số Quái", "Trạng Thái"};
        liveZoneTableModel = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        liveZoneTable = new JTable(liveZoneTableModel);

        p.add(styleTable(liveZoneTable), BorderLayout.CENTER);
        return p;
    }

    // =========================================================================
    //  DATABASE & ENGINE LOGIC
    // =========================================================================
    private void loadDataAsync() {
        if (isLoading.getAndSet(true)) return;

        txtSearch.setEnabled(false);
        allMaps.clear();
        mapTableModel.setRowCount(0);
        lblTotalMaps.setText("Đang tải dữ liệu...");

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData()) {
                // 1. Tải mob templates để tra cứu tên
                try (PreparedStatement ps = conn.prepareStatement("SELECT id, name, hp FROM mob_template");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        long hp = rs.getLong("hp");
                        mobNameMap.put(id, name);
                        mobDefaultHpMap.put(id, hp);
                    }
                } catch (Exception ex) {
                    Logger.error("[MapEditor] Lỗi load mob_template: " + ex.getMessage() + "\n");
                }

                // 2. Tải npc templates để tra cứu tên
                try (PreparedStatement ps = conn.prepareStatement("SELECT id, name FROM npc_template");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        int id = rs.getInt("id");
                        String name = rs.getString("name");
                        npcNameMap.put(id, name);
                    }
                } catch (Exception ex) {
                    Logger.error("[MapEditor] Lỗi load npc_template: " + ex.getMessage() + "\n");
                }

                // 3. Tải toàn bộ map_template
                try (PreparedStatement ps = conn.prepareStatement("SELECT * FROM map_template ORDER BY id ASC");
                     ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        MapItem item = parseMapItem(rs);
                        allMaps.add(item);
                        mapNameLookup.put(item.id, item.name);
                    }
                }

                SwingUtilities.invokeLater(() -> {
                    updateMapTableRows();
                    lblTotalMaps.setText(allMaps.size() + " bản đồ");
                    txtSearch.setEnabled(true);
                    if (!allMaps.isEmpty()) {
                        mapTable.setRowSelectionInterval(0, 0);
                    }
                });

            } catch (SQLException e) {
                Logger.logException(MapEditorPanel.class, e, "[MapEditor] Lỗi kết nối DB");
                SwingUtilities.invokeLater(() -> {
                    lblTotalMaps.setText("Lỗi kết nối CSDL!");
                    txtSearch.setEnabled(true);
                    JOptionPane.showMessageDialog(this, "Không thể tải danh sách map từ Database:\n" + e.getMessage(), "Lỗi Kết Nối", JOptionPane.ERROR_MESSAGE);
                });
            } finally {
                isLoading.set(false);
            }
        }, "MapEditor-Loader").start();
    }

    private MapItem parseMapItem(ResultSet rs) throws SQLException {
        MapItem item = new MapItem();
        item.id = rs.getInt("id");
        item.name = rs.getString("name");
        item.zones = rs.getInt("zones");
        item.maxPlayer = rs.getInt("max_player");
        item.type = rs.getInt("type");
        item.planetId = rs.getInt("planet_id");
        item.bgType = rs.getInt("bg_type");
        item.tileId = rs.getInt("tile_id");
        item.bgId = rs.getInt("bg_id");
        try {
            item.isMapDouble = rs.getInt("is_map_double");
        } catch (Exception ignored) {}

        // Parse Mobs
        String rawMobs = rs.getString("mobs");
        parseMobs(item, rawMobs);

        // Parse NPCs
        String rawNpcs = rs.getString("npcs");
        parseNpcs(item, rawNpcs);

        // Parse Waypoints
        String rawWp = rs.getString("waypoints");
        parseWaypoints(item, rawWp);

        // Parse Effects
        try {
            String rawEff = rs.getString("effect");
            parseEffects(item, rawEff);
        } catch (Exception ignored) {}

        return item;
    }

    private void parseMobs(MapItem item, String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return;
        try {
            // Chuẩn hóa format legacy kiểu '["[0,1,100,900,432]"]' hoặc '[[0,1,100,900,432]]'
            String json = raw.replace("\\\"", "").replace("\"", "");
            JsonElement el = JSON_PARSER.parse(json);
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement sub : arr) {
                    if (sub.isJsonArray()) {
                        JsonArray dtm = sub.getAsJsonArray();
                        if (dtm.size() >= 5) {
                            MobEntry m = new MobEntry();
                            m.tempId = dtm.get(0).getAsInt();
                            m.level = dtm.get(1).getAsInt();
                            m.hp = dtm.get(2).getAsLong();
                            m.x = dtm.get(3).getAsInt();
                            m.y = dtm.get(4).getAsInt();
                            item.mobs.add(m);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("[MapEditor] Lỗi parse mobs map " + item.id + ": " + e.getMessage() + "\n");
        }
    }

    private void parseNpcs(MapItem item, String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return;
        try {
            String json = raw.replace("\\\"", "").replace("\"", "");
            JsonElement el = JSON_PARSER.parse(json);
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement sub : arr) {
                    if (sub.isJsonArray()) {
                        JsonArray dtn = sub.getAsJsonArray();
                        if (dtn.size() >= 3) {
                            NpcEntry n = new NpcEntry();
                            n.tempId = dtn.get(0).getAsInt();
                            n.x = dtn.get(1).getAsInt();
                            n.y = dtn.get(2).getAsInt();
                            if (dtn.size() >= 4) {
                                n.avatar = dtn.get(3).getAsInt();
                            }
                            item.npcs.add(n);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("[MapEditor] Lỗi parse npcs map " + item.id + ": " + e.getMessage() + "\n");
        }
    }

    private void parseWaypoints(MapItem item, String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return;
        try {
            // Chuẩn hóa format: Manager.java dùng: .replaceAll("\\[\"\\[", "[[").replaceAll("\\]\"\\]", "]]").replaceAll("\",\"", ",")
            String json = raw.replaceAll("\\[\"\\[", "[[")
                    .replaceAll("\\]\"\\]", "]]")
                    .replaceAll("\",\"", ",")
                    .replaceAll("\\\\\"", "\"");
            JsonElement el = JSON_PARSER.parse(json);
            if (el.isJsonArray()) {
                JsonArray arr = el.getAsJsonArray();
                for (JsonElement sub : arr) {
                    JsonArray dtwp;
                    if (sub.isJsonArray()) {
                        dtwp = sub.getAsJsonArray();
                    } else {
                        dtwp = JSON_PARSER.parse(sub.getAsString()).getAsJsonArray();
                    }
                    if (dtwp.size() >= 10) {
                        WaypointEntry wp = new WaypointEntry();
                        wp.name = dtwp.get(0).getAsString();
                        wp.minX = dtwp.get(1).getAsInt();
                        wp.minY = dtwp.get(2).getAsInt();
                        wp.maxX = dtwp.get(3).getAsInt();
                        wp.maxY = dtwp.get(4).getAsInt();
                        wp.isEnter = dtwp.get(5).getAsInt() == 1;
                        wp.isOffline = dtwp.get(6).getAsInt() == 1;
                        wp.goMap = dtwp.get(7).getAsInt();
                        wp.goX = dtwp.get(8).getAsInt();
                        wp.goY = dtwp.get(9).getAsInt();
                        item.waypoints.add(wp);
                    }
                }
            }
        } catch (Exception e) {
            Logger.error("[MapEditor] Lỗi parse waypoints map " + item.id + ": " + e.getMessage() + "\n");
        }
    }

    private void parseEffects(MapItem item, String raw) {
        if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return;
        try {
            JsonElement el = JSON_PARSER.parse(raw);
            if (el.isJsonArray()) {
                for (JsonElement sub : el.getAsJsonArray()) {
                    if (sub.isJsonObject()) {
                        JsonObject obj = sub.getAsJsonObject();
                        EffectEntry eff = new EffectEntry();
                        if (obj.has("key")) eff.key = obj.get("key").getAsString();
                        if (obj.has("value")) eff.value = obj.get("value").getAsString();
                        item.effects.add(eff);
                    }
                }
            }
        } catch (Exception ignored) {}
    }

    private void updateMapTableRows() {
        mapTableModel.setRowCount(0);
        for (MapItem m : allMaps) {
            String pName = switch (m.planetId) {
                case 0 -> "Trái Đất";
                case 1 -> "Namếc";
                case 2 -> "Xayda";
                default -> "Khác";
            };
            mapTableModel.addRow(new Object[]{
                    m.id,
                    m.name,
                    pName,
                    m.zones,
                    m.mobs.size(),
                    m.npcs.size(),
                    m.waypoints.size()
            });
        }
    }

    private void applyFilter() {
        String keyword = txtSearch.getText().trim().toLowerCase();
        int planetIdx = cboPlanetFilter.getSelectedIndex();

        RowFilter<DefaultTableModel, Object> rf = new RowFilter<>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ?> entry) {
                int id = (int) entry.getValue(0);
                String name = entry.getValue(1).toString().toLowerCase();
                String planetStr = entry.getValue(2).toString();

                if (!keyword.isEmpty()) {
                    boolean match = String.valueOf(id).contains(keyword) || name.contains(keyword);
                    if (!match) return false;
                }

                return switch (planetIdx) {
                    case 1 -> planetStr.contains("Trái Đất");
                    case 2 -> planetStr.contains("Namếc");
                    case 3 -> planetStr.contains("Xayda");
                    case 4 -> !planetStr.contains("Trái Đất") && !planetStr.contains("Namếc") && !planetStr.contains("Xayda");
                    default -> true;
                };
            }
        };
        mapTableSorter.setRowFilter(rf);
    }

    private void selectMapById(int mapId) {
        for (MapItem m : allMaps) {
            if (m.id == mapId) {
                this.currentSelectedMap = m;
                populateMapDetails(m);
                break;
            }
        }
    }

    private void populateMapDetails(MapItem m) {
        lblCurrentMapTitle.setText(m.name);
        lblCurrentMapBadge.setText("ID: " + m.id);

        txtMapId.setText(String.valueOf(m.id));
        txtMapName.setText(m.name);

        // Planet combo
        for (int i = 0; i < cboPlanet.getItemCount(); i++) {
            if (cboPlanet.getItemAt(i).id == m.planetId) {
                cboPlanet.setSelectedIndex(i);
                break;
            }
        }

        // Type combo
        boolean matchedType = false;
        for (int i = 0; i < cboMapType.getItemCount(); i++) {
            if (cboMapType.getItemAt(i).id == m.type) {
                cboMapType.setSelectedIndex(i);
                matchedType = true;
                break;
            }
        }
        if (!matchedType && cboMapType.getItemCount() > 0) {
            cboMapType.setSelectedIndex(0);
        }

        spZones.setValue(m.zones);
        spMaxPlayer.setValue(m.maxPlayer);
        spTileId.setValue(m.tileId);
        spBgId.setValue(m.bgId);
        spBgType.setValue(m.bgType);
        chkMapDouble.setSelected(m.isMapDouble == 1);

        // Populate Sub-tables
        updateMobTable(m);
        updateNpcTable(m);
        updateWaypointTable(m);
        refreshLiveZoneStatus();
    }

    private void updateMobTable(MapItem m) {
        mobTableModel.setRowCount(0);
        int idx = 1;
        for (MobEntry mob : m.mobs) {
            String name = mobNameMap.getOrDefault(mob.tempId, "Quái #" + mob.tempId);
            mobTableModel.addRow(new Object[]{
                    idx++,
                    name,
                    mob.tempId,
                    mob.level,
                    mob.hp,
                    mob.x,
                    mob.y
            });
        }
    }

    private void updateNpcTable(MapItem m) {
        npcTableModel.setRowCount(0);
        int idx = 1;
        for (NpcEntry npc : m.npcs) {
            String name = npcNameMap.getOrDefault(npc.tempId, "NPC #" + npc.tempId);
            npcTableModel.addRow(new Object[]{
                    idx++,
                    name,
                    npc.tempId,
                    npc.x,
                    npc.y,
                    npc.avatar
            });
        }
    }

    private void updateWaypointTable(MapItem m) {
        wpTableModel.setRowCount(0);
        for (WaypointEntry wp : m.waypoints) {
            String targetMapName = mapNameLookup.getOrDefault(wp.goMap, "Map #" + wp.goMap);
            String range = "[" + wp.minX + ".." + wp.maxX + ", " + wp.minY + ".." + wp.maxY + "]";
            wpTableModel.addRow(new Object[]{
                    wp.name,
                    range,
                    wp.isEnter ? "Có" : "Không",
                    wp.isOffline ? "Có" : "Không",
                    "[" + wp.goMap + "] " + targetMapName,
                    wp.goX,
                    wp.goY
            });
        }
    }

    private void refreshLiveZoneStatus() {
        liveZoneTableModel.setRowCount(0);
        if (currentSelectedMap == null) {
            lblLiveStatus.setText("Trạng thái: Chưa chọn map");
            lblLiveStatus.setForeground(TEXT_MUTED);
            return;
        }

        models.map.Map liveMap = Manager.MAP_BY_ID.get(currentSelectedMap.id);
        if (liveMap == null) {
            lblLiveStatus.setText("Trạng thái: Chưa được nạp vào bộ nhớ Server");
            lblLiveStatus.setForeground(WARNING);
            return;
        }

        lblLiveStatus.setText("Trạng thái: Đang hoạt động (" + liveMap.zones.size() + " khu)");
        lblLiveStatus.setForeground(SUCCESS);

        for (Zone z : liveMap.zones) {
            int playerCount = z.getPlayers().size();
            int mobCount = z.mobs.size();
            String status = playerCount >= z.maxPlayer ? "ĐẦY" : (playerCount > 0 ? "Có người" : "Trống");
            liveZoneTableModel.addRow(new Object[]{
                    z.zoneId,
                    playerCount,
                    z.maxPlayer,
                    mobCount,
                    status
            });
        }
    }

    // =========================================================================
    //  ACTIONS: LƯU / ĐỒNG BỘ / TẠO / NHÂN BẢN / XÓA
    // =========================================================================
    private void saveCurrentMap() {
        if (currentSelectedMap == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một bản đồ để lưu!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        // Cập nhật giá trị từ Form vào model
        String newName = txtMapName.getText().trim();
        if (newName.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Tên bản đồ không được để trống!", "Lỗi Xác Thực", JOptionPane.ERROR_MESSAGE);
            return;
        }

        currentSelectedMap.name = newName;
        currentSelectedMap.planetId = ((PlanetOption) Objects.requireNonNull(cboPlanet.getSelectedItem())).id;
        currentSelectedMap.type = ((TypeOption) Objects.requireNonNull(cboMapType.getSelectedItem())).id;
        currentSelectedMap.zones = (int) spZones.getValue();
        currentSelectedMap.maxPlayer = (int) spMaxPlayer.getValue();
        currentSelectedMap.tileId = (int) spTileId.getValue();
        currentSelectedMap.bgId = (int) spBgId.getValue();
        currentSelectedMap.bgType = (int) spBgType.getValue();
        currentSelectedMap.isMapDouble = chkMapDouble.isSelected() ? 1 : 0;

        // Serialize Mobs, NPCs, Waypoints thành JSON
        String mobsJson = buildMobsJson(currentSelectedMap.mobs);
        String npcsJson = buildNpcsJson(currentSelectedMap.npcs);
        String wpJson = buildWaypointsJson(currentSelectedMap.waypoints);
        String effJson = buildEffectsJson(currentSelectedMap.effects);

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData()) {
                String sql = "UPDATE map_template SET name=?, zones=?, max_player=?, type=?, planet_id=?, bg_type=?, tile_id=?, bg_id=?, waypoints=?, mobs=?, npcs=?, is_map_double=?, effect=? WHERE id=?";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setString(1, currentSelectedMap.name);
                    ps.setInt(2, currentSelectedMap.zones);
                    ps.setInt(3, currentSelectedMap.maxPlayer);
                    ps.setInt(4, currentSelectedMap.type);
                    ps.setInt(5, currentSelectedMap.planetId);
                    ps.setInt(6, currentSelectedMap.bgType);
                    ps.setInt(7, currentSelectedMap.tileId);
                    ps.setInt(8, currentSelectedMap.bgId);
                    ps.setString(9, wpJson);
                    ps.setString(10, mobsJson);
                    ps.setString(11, npcsJson);
                    ps.setInt(12, currentSelectedMap.isMapDouble);
                    ps.setString(13, effJson);
                    ps.setInt(14, currentSelectedMap.id);

                    int affected = ps.executeUpdate();
                    if (affected > 0) {
                        mapNameLookup.put(currentSelectedMap.id, currentSelectedMap.name);

                        // Đồng bộ trực tiếp vào bộ nhớ Server
                        syncMapToMemory(currentSelectedMap);

                        SwingUtilities.invokeLater(() -> {
                            updateMapTableRows();
                            lblCurrentMapTitle.setText(currentSelectedMap.name);
                            JOptionPane.showMessageDialog(this, "Lưu cấu hình bản đồ [" + currentSelectedMap.id + "] thành công!\nĐã cập nhật đồng thời CSDL và bộ nhớ Server.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                        });
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Không tìm thấy map ID " + currentSelectedMap.id + " trong CSDL!", "Lỗi Lưu", JOptionPane.ERROR_MESSAGE));
                    }
                }
            } catch (SQLException ex) {
                Logger.logException(MapEditorPanel.class, ex, "[MapEditor] Lỗi lưu map " + currentSelectedMap.id);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi lưu CSDL:\n" + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE));
            }
        }, "MapEditor-Save").start();
    }

    private void syncToLiveServer() {
        if (currentSelectedMap == null) return;
        syncMapToMemory(currentSelectedMap);
        refreshLiveZoneStatus();
        JOptionPane.showMessageDialog(this, "Đã đồng bộ thông số bản đồ [" + currentSelectedMap.id + "] vào bộ nhớ máy chủ thành công!", "Đồng Bộ Live", JOptionPane.INFORMATION_MESSAGE);
    }

    private void syncMapToMemory(MapItem item) {
        try {
            // 1. Cập nhật MapTemplate trong Manager.MAP_TEMPLATES
            if (Manager.MAP_TEMPLATES != null) {
                for (MapTemplate temp : Manager.MAP_TEMPLATES) {
                    if (temp != null && temp.id == item.id) {
                        temp.name = item.name;
                        temp.planetId = (byte) item.planetId;
                        temp.type = (byte) item.type;
                        temp.zones = (byte) item.zones;
                        temp.maxPlayerPerZone = (byte) item.maxPlayer;
                        temp.tileId = (byte) item.tileId;
                        temp.bgId = (byte) item.bgId;
                        temp.bgType = (byte) item.bgType;

                        // Cập nhật waypoints
                        List<WayPoint> liveWps = new ArrayList<>();
                        for (WaypointEntry wpe : item.waypoints) {
                            WayPoint wp = new WayPoint();
                            wp.name = wpe.name;
                            wp.minX = (short) wpe.minX;
                            wp.minY = (short) wpe.minY;
                            wp.maxX = (short) wpe.maxX;
                            wp.maxY = (short) wpe.maxY;
                            wp.isEnter = wpe.isEnter;
                            wp.isOffline = wpe.isOffline;
                            wp.goMap = (short) wpe.goMap;
                            wp.goX = (short) wpe.goX;
                            wp.goY = (short) wpe.goY;
                            liveWps.add(wp);
                        }
                        temp.wayPoints = liveWps;
                        break;
                    }
                }
            }

            // 2. Cập nhật đối tượng models.map.Map đang chạy
            models.map.Map liveMap = Manager.MAP_BY_ID.get(item.id);
            if (liveMap != null) {
                liveMap.mapName = item.name;
                liveMap.planetId = (byte) item.planetId;
                liveMap.planetName = Service.gI().get_HanhTinh((byte) item.planetId);
                liveMap.type = (byte) item.type;
                liveMap.tileId = (byte) item.tileId;
                liveMap.bgId = (byte) item.bgId;
                liveMap.bgType = (byte) item.bgType;

                // Cập nhật maxPlayer cho các Zone đang chạy
                if (liveMap.zones != null) {
                    for (Zone z : liveMap.zones) {
                        z.maxPlayer = (byte) item.maxPlayer;
                    }
                }

                // Cập nhật waypoints cho map
                List<WayPoint> liveWps = new ArrayList<>();
                for (WaypointEntry wpe : item.waypoints) {
                    WayPoint wp = new WayPoint();
                    wp.name = wpe.name;
                    wp.minX = (short) wpe.minX;
                    wp.minY = (short) wpe.minY;
                    wp.maxX = (short) wpe.maxX;
                    wp.maxY = (short) wpe.maxY;
                    wp.isEnter = wpe.isEnter;
                    wp.isOffline = wpe.isOffline;
                    wp.goMap = (short) wpe.goMap;
                    wp.goX = (short) wpe.goX;
                    wp.goY = (short) wpe.goY;
                    liveWps.add(wp);
                }
                liveMap.wayPoints = liveWps;

                Logger.success("[MapEditor] Live hot-sync thành công map " + item.id + " (" + item.name + ")\n");
            }
        } catch (Exception e) {
            Logger.logException(MapEditorPanel.class, e, "[MapEditor] Lỗi hot-sync map vào memory");
        }
    }

    private void teleportPlayersHome() {
        if (currentSelectedMap == null) return;
        models.map.Map liveMap = Manager.MAP_BY_ID.get(currentSelectedMap.id);
        if (liveMap == null) {
            JOptionPane.showMessageDialog(this, "Map này hiện không nạp trong Server!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int count = 0;
        for (Zone z : liveMap.zones) {
            count += z.getPlayers().size();
        }

        if (count == 0) {
            JOptionPane.showMessageDialog(this, "Không có người chơi nào trên map này.", "Thông Báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Có " + count + " người chơi đang ở map [" + currentSelectedMap.name + "].\nBạn có chắc chắn muốn dịch chuyển tất cả về nhà?",
                "Xác Nhận Dịch Chuyển", JOptionPane.YES_NO_OPTION);
        if (confirm != JOptionPane.YES_OPTION) return;

        int finalCount = count;
        new Thread(() -> {
            try {
                for (Zone z : liveMap.zones) {
                    List<models.player.Player> players = new ArrayList<>(z.getPlayers());
                    for (models.player.Player pl : players) {
                        if (pl != null && pl.location != null) {
                            ChangeMapService.gI().changeMapBySpaceShip(pl, pl.gender + 21, 0, -1);
                        }
                    }
                }
                SwingUtilities.invokeLater(() -> {
                    refreshLiveZoneStatus();
                    JOptionPane.showMessageDialog(this, "Đã đưa " + finalCount + " người chơi về nhà thành công!", "Hoàn Tất", JOptionPane.INFORMATION_MESSAGE);
                });
            } catch (Exception ex) {
                Logger.logException(MapEditorPanel.class, ex, "Lỗi teleport players");
            }
        }).start();
    }

    private void showCreateMapDialog() {
        JTextField idField = textField("Nhập ID...");
        JTextField nameField = textField("Nhập tên bản đồ...");

        JPanel p = new JPanel(new GridLayout(2, 2, 6, 6));
        p.add(new JLabel("Mã Bản Đồ mới (ID):"));
        p.add(idField);
        p.add(new JLabel("Tên Bản Đồ mới:"));
        p.add(nameField);

        int res = JOptionPane.showConfirmDialog(this, p, "Thêm Bản Đồ Mới", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String idStr = idField.getText().trim();
            String name = nameField.getText().trim();
            if (idStr.isEmpty() || name.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập đầy đủ ID và Tên bản đồ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int newId;
            try {
                newId = Integer.parseInt(idStr);
                if (newId < 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "ID phải là số nguyên dương hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            for (MapItem m : allMaps) {
                if (m.id == newId) {
                    JOptionPane.showMessageDialog(this, "Bản đồ với ID " + newId + " đã tồn tại!", "Trùng Lặp ID", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            MapItem newItem = new MapItem();
            newItem.id = newId;
            newItem.name = name;
            newItem.zones = 15;
            newItem.maxPlayer = 12;
            newItem.type = 0;
            newItem.planetId = 0;
            newItem.tileId = 1;
            newItem.bgId = 0;
            newItem.bgType = 0;

            insertNewMapToDb(newItem);
        }
    }

    private void cloneCurrentMap() {
        if (currentSelectedMap == null) return;

        String input = JOptionPane.showInputDialog(this, "Nhập ID mới cho bản đồ sao chép từ [" + currentSelectedMap.name + "]:");
        if (input == null || input.trim().isEmpty()) return;

        try {
            int newId = Integer.parseInt(input.trim());
            if (newId < 0) throw new NumberFormatException();

            for (MapItem m : allMaps) {
                if (m.id == newId) {
                    JOptionPane.showMessageDialog(this, "ID " + newId + " đã tồn tại!", "Trùng ID", JOptionPane.ERROR_MESSAGE);
                    return;
                }
            }

            MapItem clone = new MapItem();
            clone.id = newId;
            clone.name = currentSelectedMap.name + " (Copy)";
            clone.zones = currentSelectedMap.zones;
            clone.maxPlayer = currentSelectedMap.maxPlayer;
            clone.type = currentSelectedMap.type;
            clone.planetId = currentSelectedMap.planetId;
            clone.tileId = currentSelectedMap.tileId;
            clone.bgId = currentSelectedMap.bgId;
            clone.bgType = currentSelectedMap.bgType;
            clone.isMapDouble = currentSelectedMap.isMapDouble;

            // Deep copy Mobs
            for (MobEntry m : currentSelectedMap.mobs) {
                MobEntry me = new MobEntry();
                me.tempId = m.tempId; me.level = m.level; me.hp = m.hp; me.x = m.x; me.y = m.y;
                clone.mobs.add(me);
            }
            // Deep copy NPCs
            for (NpcEntry n : currentSelectedMap.npcs) {
                NpcEntry ne = new NpcEntry();
                ne.tempId = n.tempId; ne.x = n.x; ne.y = n.y; ne.avatar = n.avatar;
                clone.npcs.add(ne);
            }
            // Deep copy Waypoints
            for (WaypointEntry w : currentSelectedMap.waypoints) {
                WaypointEntry we = new WaypointEntry();
                we.name = w.name; we.minX = w.minX; we.minY = w.minY; we.maxX = w.maxX; we.maxY = w.maxY;
                we.isEnter = w.isEnter; we.isOffline = w.isOffline; we.goMap = w.goMap; we.goX = w.goX; we.goY = w.goY;
                clone.waypoints.add(we);
            }

            insertNewMapToDb(clone);

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "ID phải là số nguyên dương hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void insertNewMapToDb(MapItem newItem) {
        String mobsJson = buildMobsJson(newItem.mobs);
        String npcsJson = buildNpcsJson(newItem.npcs);
        String wpJson = buildWaypointsJson(newItem.waypoints);
        String effJson = buildEffectsJson(newItem.effects);

        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData()) {
                String sql = "INSERT INTO map_template (id, name, zones, max_player, data, type, planet_id, bg_type, tile_id, bg_id, waypoints, mobs, npcs, is_map_double, effect) "
                        + "VALUES (?, ?, ?, ?, '[0,0,0,1,0]', ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setInt(1, newItem.id);
                    ps.setString(2, newItem.name);
                    ps.setInt(3, newItem.zones);
                    ps.setInt(4, newItem.maxPlayer);
                    ps.setInt(5, newItem.type);
                    ps.setInt(6, newItem.planetId);
                    ps.setInt(7, newItem.bgType);
                    ps.setInt(8, newItem.tileId);
                    ps.setInt(9, newItem.bgId);
                    ps.setString(10, wpJson);
                    ps.setString(11, mobsJson);
                    ps.setString(12, npcsJson);
                    ps.setInt(13, newItem.isMapDouble);
                    ps.setString(14, effJson);

                    ps.executeUpdate();
                }

                allMaps.add(newItem);
                allMaps.sort(Comparator.comparingInt(a -> a.id));
                mapNameLookup.put(newItem.id, newItem.name);

                SwingUtilities.invokeLater(() -> {
                    updateMapTableRows();
                    selectMapById(newItem.id);
                    JOptionPane.showMessageDialog(this, "Tạo thành công bản đồ [" + newItem.id + "] " + newItem.name, "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (SQLException ex) {
                Logger.logException(MapEditorPanel.class, ex, "[MapEditor] Lỗi tạo map " + newItem.id);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi thêm map vào CSDL:\n" + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE));
            }
        }, "MapEditor-Insert").start();
    }

    private void deleteCurrentMap() {
        if (currentSelectedMap == null) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn map cần xóa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "CẢNH BÁO NGUY HIỂM:\nBạn có thực sự muốn xóa bản đồ [" + currentSelectedMap.id + "] " + currentSelectedMap.name + "?\nHành động này không thể hoàn tác!",
                "Xác Nhận Xóa Bản Đồ", JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) return;

        int delId = currentSelectedMap.id;
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData()) {
                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM map_template WHERE id = ?")) {
                    ps.setInt(1, delId);
                    ps.executeUpdate();
                }

                allMaps.removeIf(m -> m.id == delId);
                mapNameLookup.remove(delId);
                Manager.MAP_BY_ID.remove(delId);

                SwingUtilities.invokeLater(() -> {
                    updateMapTableRows();
                    if (!allMaps.isEmpty()) {
                        selectMapById(allMaps.get(0).id);
                    } else {
                        currentSelectedMap = null;
                        lblCurrentMapTitle.setText("Chưa chọn bản đồ nào");
                    }
                    JOptionPane.showMessageDialog(this, "Đã xóa bản đồ ID " + delId + " khỏi CSDL.", "Thành Công", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (SQLException ex) {
                Logger.logException(MapEditorPanel.class, ex, "[MapEditor] Lỗi xóa map " + delId);
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi xóa map:\n" + ex.getMessage(), "Lỗi SQL", JOptionPane.ERROR_MESSAGE));
            }
        }, "MapEditor-Delete").start();
    }

    // =========================================================================
    //  SUB-DIALOGS: MOBS
    // =========================================================================
    private void showAddMobDialog() {
        if (currentSelectedMap == null) return;
        showMobDialog(null);
    }

    private void editSelectedMob() {
        if (currentSelectedMap == null) return;
        int row = mobTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn quái vật để sửa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MobEntry m = currentSelectedMap.mobs.get(row);
        showMobDialog(m);
    }

    private void duplicateSelectedMob() {
        if (currentSelectedMap == null) return;
        int row = mobTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn quái vật để nhân bản!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        MobEntry m = currentSelectedMap.mobs.get(row);
        MobEntry copy = new MobEntry();
        copy.tempId = m.tempId;
        copy.level = m.level;
        copy.hp = m.hp;
        copy.x = m.x + 30;
        copy.y = m.y;
        currentSelectedMap.mobs.add(copy);
        updateMobTable(currentSelectedMap);
    }

    private void deleteSelectedMob() {
        if (currentSelectedMap == null) return;
        int row = mobTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn quái vật cần xóa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentSelectedMap.mobs.remove(row);
        updateMobTable(currentSelectedMap);
    }

    private void showMobDialog(MobEntry target) {
        boolean isEdit = target != null;
        JComboBox<MobTemplateOption> cboMob = new JComboBox<>();
        List<MobTemplateOption> list = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : mobNameMap.entrySet()) {
            list.add(new MobTemplateOption(entry.getKey(), entry.getValue(), mobDefaultHpMap.getOrDefault(entry.getKey(), 100L)));
        }
        list.sort(Comparator.comparingInt(a -> a.id));
        for (MobTemplateOption opt : list) {
            cboMob.addItem(opt);
        }

        JSpinner spLevel = new JSpinner(new SpinnerNumberModel(isEdit ? target.level : 1, 1, 100, 1));
        JTextField txtHp = textField("Máu quái vật...");
        txtHp.setText(isEdit ? String.valueOf(target.hp) : "1000");

        JSpinner spX = new JSpinner(new SpinnerNumberModel(isEdit ? target.x : 500, 0, 5000, 10));
        JSpinner spY = new JSpinner(new SpinnerNumberModel(isEdit ? target.y : 400, 0, 5000, 10));

        cboMob.addActionListener(e -> {
            MobTemplateOption opt = (MobTemplateOption) cboMob.getSelectedItem();
            if (opt != null && !isEdit) {
                txtHp.setText(String.valueOf(opt.defaultHp));
            }
        });

        if (isEdit) {
            for (int i = 0; i < cboMob.getItemCount(); i++) {
                if (cboMob.getItemAt(i).id == target.tempId) {
                    cboMob.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel p = new JPanel(new GridLayout(5, 2, 6, 6));
        p.add(new JLabel("Mẫu Quái Vật (Template):")); p.add(cboMob);
        p.add(new JLabel("Cấp Độ (Level):"));           p.add(spLevel);
        p.add(new JLabel("Máu Tối Đa (HP):"));          p.add(txtHp);
        p.add(new JLabel("Tọa Độ X:"));                 p.add(spX);
        p.add(new JLabel("Tọa Độ Y:"));                 p.add(spY);

        int res = JOptionPane.showConfirmDialog(this, p, isEdit ? "Sửa Quái Vật" : "Thêm Quái Vật", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            MobTemplateOption opt = (MobTemplateOption) cboMob.getSelectedItem();
            if (opt == null) return;
            long hp;
            try {
                hp = Long.parseLong(txtHp.getText().trim());
                if (hp <= 0) throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "HP quái vật phải là số lớn hơn 0!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (isEdit) {
                target.tempId = opt.id;
                target.level = (int) spLevel.getValue();
                target.hp = hp;
                target.x = (int) spX.getValue();
                target.y = (int) spY.getValue();
            } else {
                MobEntry newMob = new MobEntry();
                newMob.tempId = opt.id;
                newMob.level = (int) spLevel.getValue();
                newMob.hp = hp;
                newMob.x = (int) spX.getValue();
                newMob.y = (int) spY.getValue();
                currentSelectedMap.mobs.add(newMob);
            }
            updateMobTable(currentSelectedMap);
        }
    }

    // =========================================================================
    //  SUB-DIALOGS: NPCS
    // =========================================================================
    private void showAddNpcDialog() {
        if (currentSelectedMap == null) return;
        showNpcDialog(null);
    }

    private void editSelectedNpc() {
        if (currentSelectedMap == null) return;
        int row = npcTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC để sửa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        NpcEntry n = currentSelectedMap.npcs.get(row);
        showNpcDialog(n);
    }

    private void deleteSelectedNpc() {
        if (currentSelectedMap == null) return;
        int row = npcTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn NPC cần xóa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentSelectedMap.npcs.remove(row);
        updateNpcTable(currentSelectedMap);
    }

    private void showNpcDialog(NpcEntry target) {
        boolean isEdit = target != null;
        JComboBox<NpcTemplateOption> cboNpc = new JComboBox<>();
        List<NpcTemplateOption> list = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : npcNameMap.entrySet()) {
            list.add(new NpcTemplateOption(entry.getKey(), entry.getValue()));
        }
        list.sort(Comparator.comparingInt(a -> a.id));
        for (NpcTemplateOption opt : list) {
            cboNpc.addItem(opt);
        }

        JSpinner spX = new JSpinner(new SpinnerNumberModel(isEdit ? target.x : 400, 0, 5000, 10));
        JSpinner spY = new JSpinner(new SpinnerNumberModel(isEdit ? target.y : 400, 0, 5000, 10));

        if (isEdit) {
            for (int i = 0; i < cboNpc.getItemCount(); i++) {
                if (cboNpc.getItemAt(i).id == target.tempId) {
                    cboNpc.setSelectedIndex(i);
                    break;
                }
            }
        }

        JPanel p = new JPanel(new GridLayout(3, 2, 6, 6));
        p.add(new JLabel("Mẫu NPC:"));    p.add(cboNpc);
        p.add(new JLabel("Tọa Độ X:")); p.add(spX);
        p.add(new JLabel("Tọa Độ Y:")); p.add(spY);

        int res = JOptionPane.showConfirmDialog(this, p, isEdit ? "Sửa NPC" : "Thêm NPC", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            NpcTemplateOption opt = (NpcTemplateOption) cboNpc.getSelectedItem();
            if (opt == null) return;

            if (isEdit) {
                target.tempId = opt.id;
                target.x = (int) spX.getValue();
                target.y = (int) spY.getValue();
            } else {
                NpcEntry newNpc = new NpcEntry();
                newNpc.tempId = opt.id;
                newNpc.x = (int) spX.getValue();
                newNpc.y = (int) spY.getValue();
                currentSelectedMap.npcs.add(newNpc);
            }
            updateNpcTable(currentSelectedMap);
        }
    }

    // =========================================================================
    //  SUB-DIALOGS: WAYPOINTS
    // =========================================================================
    private void showAddWaypointDialog() {
        if (currentSelectedMap == null) return;
        showWaypointDialog(null);
    }

    private void editSelectedWaypoint() {
        if (currentSelectedMap == null) return;
        int row = wpTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn cổng dịch chuyển để sửa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        WaypointEntry wp = currentSelectedMap.waypoints.get(row);
        showWaypointDialog(wp);
    }

    private void deleteSelectedWaypoint() {
        if (currentSelectedMap == null) return;
        int row = wpTable.getSelectedRow();
        if (row == -1) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn cổng dịch chuyển cần xóa!", "Thông Báo", JOptionPane.WARNING_MESSAGE);
            return;
        }
        currentSelectedMap.waypoints.remove(row);
        updateWaypointTable(currentSelectedMap);
    }

    private void showWaypointDialog(WaypointEntry target) {
        boolean isEdit = target != null;
        JTextField txtWpName = textField("Tên cổng dịch chuyển...");
        txtWpName.setText(isEdit ? target.name : "Cổng Dịch Chuyển");

        JSpinner spMinX = new JSpinner(new SpinnerNumberModel(isEdit ? target.minX : 0, 0, 5000, 10));
        JSpinner spMinY = new JSpinner(new SpinnerNumberModel(isEdit ? target.minY : 360, 0, 5000, 10));
        JSpinner spMaxX = new JSpinner(new SpinnerNumberModel(isEdit ? target.maxX : 40, 0, 5000, 10));
        JSpinner spMaxY = new JSpinner(new SpinnerNumberModel(isEdit ? target.maxY : 400, 0, 5000, 10));

        JCheckBox chkEnter = new JCheckBox("Yêu cầu nhấn Enter (isEnter)", isEdit && target.isEnter);
        JCheckBox chkOffline = new JCheckBox("Map Offline (isOffline)", isEdit && target.isOffline);

        JComboBox<MapOption> cboTargetMap = new JComboBox<>();
        for (MapItem item : allMaps) {
            cboTargetMap.addItem(new MapOption(item.id, item.name));
        }
        if (isEdit) {
            for (int i = 0; i < cboTargetMap.getItemCount(); i++) {
                if (cboTargetMap.getItemAt(i).id == target.goMap) {
                    cboTargetMap.setSelectedIndex(i);
                    break;
                }
            }
        }

        JSpinner spGoX = new JSpinner(new SpinnerNumberModel(isEdit ? target.goX : 100, 0, 5000, 10));
        JSpinner spGoY = new JSpinner(new SpinnerNumberModel(isEdit ? target.goY : 400, 0, 5000, 10));

        JPanel p = new JPanel(new GridLayout(8, 2, 6, 6));
        p.add(new JLabel("Tên Cổng / Mô Tả:"));  p.add(txtWpName);
        p.add(new JLabel("Vùng Vào Min X:"));       p.add(spMinX);
        p.add(new JLabel("Vùng Vào Min Y:"));       p.add(spMinY);
        p.add(new JLabel("Vùng Vào Max X:"));       p.add(spMaxX);
        p.add(new JLabel("Vùng Vào Max Y:"));       p.add(spMaxY);
        p.add(new JLabel("Bản Đồ Đích (goMap):"));  p.add(cboTargetMap);
        p.add(new JLabel("Tọa Độ Đích Đến X:"));    p.add(spGoX);
        p.add(new JLabel("Tọa Độ Đích Đến Y:"));    p.add(spGoY);

        JPanel wrapper = new JPanel(new BorderLayout(0, 8));
        wrapper.add(p, BorderLayout.CENTER);
        JPanel chkBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        chkBox.add(chkEnter);
        chkBox.add(chkOffline);
        wrapper.add(chkBox, BorderLayout.SOUTH);

        int res = JOptionPane.showConfirmDialog(this, wrapper, isEdit ? "Sửa Cổng Dịch Chuyển" : "Thêm Cổng Dịch Chuyển", JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String wpName = txtWpName.getText().trim();
            if (wpName.isEmpty()) wpName = "Cổng";

            MapOption opt = (MapOption) cboTargetMap.getSelectedItem();
            int goMapId = opt != null ? opt.id : 0;

            if (isEdit) {
                target.name = wpName;
                target.minX = (int) spMinX.getValue();
                target.minY = (int) spMinY.getValue();
                target.maxX = (int) spMaxX.getValue();
                target.maxY = (int) spMaxY.getValue();
                target.isEnter = chkEnter.isSelected();
                target.isOffline = chkOffline.isSelected();
                target.goMap = goMapId;
                target.goX = (int) spGoX.getValue();
                target.goY = (int) spGoY.getValue();
            } else {
                WaypointEntry we = new WaypointEntry();
                we.name = wpName;
                we.minX = (int) spMinX.getValue();
                we.minY = (int) spMinY.getValue();
                we.maxX = (int) spMaxX.getValue();
                we.maxY = (int) spMaxY.getValue();
                we.isEnter = chkEnter.isSelected();
                we.isOffline = chkOffline.isSelected();
                we.goMap = goMapId;
                we.goX = (int) spGoX.getValue();
                we.goY = (int) spGoY.getValue();
                currentSelectedMap.waypoints.add(we);
            }
            updateWaypointTable(currentSelectedMap);
        }
    }

    // =========================================================================
    //  JSON BUILDERS (Tương thích 100% với Manager.java & CSDL nro_data)
    // =========================================================================
    private String buildMobsJson(List<MobEntry> mobs) {
        JsonArray arr = new JsonArray();
        for (MobEntry m : mobs) {
            JsonArray sub = new JsonArray();
            sub.add(m.tempId);
            sub.add(m.level);
            sub.add(m.hp);
            sub.add(m.x);
            sub.add(m.y);
            arr.add(sub);
        }
        return GSON.toJson(arr);
    }

    private String buildNpcsJson(List<NpcEntry> npcs) {
        JsonArray arr = new JsonArray();
        for (NpcEntry n : npcs) {
            JsonArray sub = new JsonArray();
            sub.add(n.tempId);
            sub.add(n.x);
            sub.add(n.y);
            sub.add(n.avatar);
            arr.add(sub);
        }
        return GSON.toJson(arr);
    }

    private String buildWaypointsJson(List<WaypointEntry> waypoints) {
        JsonArray arr = new JsonArray();
        for (WaypointEntry w : waypoints) {
            JsonArray sub = new JsonArray();
            sub.add(w.name);
            sub.add(w.minX);
            sub.add(w.minY);
            sub.add(w.maxX);
            sub.add(w.maxY);
            sub.add(w.isEnter ? 1 : 0);
            sub.add(w.isOffline ? 1 : 0);
            sub.add(w.goMap);
            sub.add(w.goX);
            sub.add(w.goY);
            arr.add(sub);
        }
        return GSON.toJson(arr);
    }

    private String buildEffectsJson(List<EffectEntry> effects) {
        JsonArray arr = new JsonArray();
        for (EffectEntry eff : effects) {
            JsonObject obj = new JsonObject();
            obj.addProperty("key", eff.key);
            obj.addProperty("value", eff.value);
            arr.add(obj);
        }
        return GSON.toJson(arr);
    }

    // =========================================================================
    //  INTERNAL MODELS & DTOs
    // =========================================================================
    public static class MapItem {
        public int id;
        public String name = "";
        public int zones = 15;
        public int maxPlayer = 12;
        public int type = 0;
        public int planetId = 0;
        public int bgType = 0;
        public int tileId = 1;
        public int bgId = 0;
        public int isMapDouble = 0;

        public final List<MobEntry> mobs = new ArrayList<>();
        public final List<NpcEntry> npcs = new ArrayList<>();
        public final List<WaypointEntry> waypoints = new ArrayList<>();
        public final List<EffectEntry> effects = new ArrayList<>();
    }

    public static class MobEntry {
        public int tempId;
        public int level = 1;
        public long hp = 1000;
        public int x;
        public int y;
    }

    public static class NpcEntry {
        public int tempId;
        public int x;
        public int y;
        public int avatar = 0;
    }

    public static class WaypointEntry {
        public String name = "Cổng";
        public int minX, minY, maxX, maxY;
        public boolean isEnter = false;
        public boolean isOffline = false;
        public int goMap = 0;
        public int goX, goY;
    }

    public static class EffectEntry {
        public String key = "";
        public String value = "";
    }

    private record PlanetOption(int id, String name) {
        @Override public String toString() { return name; }
    }

    private record TypeOption(int id, String name) {
        @Override public String toString() { return name; }
    }

    private record MobTemplateOption(int id, String name, long defaultHp) {
        @Override public String toString() { return "[" + id + "] " + name + " (HP: " + defaultHp + ")"; }
    }

    private record NpcTemplateOption(int id, String name) {
        @Override public String toString() { return "[" + id + "] " + name; }
    }

    private record MapOption(int id, String name) {
        @Override public String toString() { return "[" + id + "] " + name; }
    }
}
