/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package server.ui;

/*
 * @Author: barcoll
 * @Refactored by Assistant
 * @Feature: Multi-tab Skill Selection & Full Set Support & Fix Icon/Double Click
 */
import models.boss.BossData;
import models.boss.BossesData;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.*;
import java.util.List;
import java.util.Vector;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import models.boss.Boss;
import managers.boss.*;
import services.Service;
import models.map.Zone;
import server.Manager;
import models.skill.Skill;
import services.JavaSourceSync;

public class BossEditorPanel extends JPanel {

    private static final String SOURCE_FILE_PATH = "src/models/boss/BossesData.java";
    private static final String ICON_FOLDER = "data/icon/";

    // --- ĐỊNH NGHĨA CÁC BỘ SKILL FULL (FULL SETS) ---
    private static final Map<String, int[][]> FULL_SETS = new LinkedHashMap<>();

    static {
        FULL_SETS.put("FULL_DRAGON", new int[][]{{Skill.DRAGON, 1}, {Skill.DRAGON, 2}, {Skill.DRAGON, 3}, {Skill.DRAGON, 4}, {Skill.DRAGON, 5}, {Skill.DRAGON, 6}, {Skill.DRAGON, 7}});
        FULL_SETS.put("FULL_DEMON", new int[][]{{Skill.DEMON, 1}, {Skill.DEMON, 2}, {Skill.DEMON, 3}, {Skill.DEMON, 4}, {Skill.DEMON, 5}, {Skill.DEMON, 6}, {Skill.DEMON, 7}});
        FULL_SETS.put("FULL_GALICK", new int[][]{{Skill.GALICK, 1}, {Skill.GALICK, 2}, {Skill.GALICK, 3}, {Skill.GALICK, 4}, {Skill.GALICK, 5}, {Skill.GALICK, 6}, {Skill.GALICK, 7}});
        FULL_SETS.put("FULL_KAMEJOKO", new int[][]{{Skill.KAMEJOKO, 1}, {Skill.KAMEJOKO, 2}, {Skill.KAMEJOKO, 3}, {Skill.KAMEJOKO, 4}, {Skill.KAMEJOKO, 5}, {Skill.KAMEJOKO, 6}, {Skill.KAMEJOKO, 7}});
        FULL_SETS.put("FULL_TAI_TAO_NANG_LUONG", new int[][]{{Skill.TAI_TAO_NANG_LUONG, 1}, {Skill.TAI_TAO_NANG_LUONG, 2}, {Skill.TAI_TAO_NANG_LUONG, 3}, {Skill.TAI_TAO_NANG_LUONG, 4}, {Skill.TAI_TAO_NANG_LUONG, 5}, {Skill.TAI_TAO_NANG_LUONG, 6}, {Skill.TAI_TAO_NANG_LUONG, 7}});
        FULL_SETS.put("FULL_MASENKO", new int[][]{{Skill.MASENKO, 1}, {Skill.MASENKO, 2}, {Skill.MASENKO, 3}, {Skill.MASENKO, 4}, {Skill.MASENKO, 5}, {Skill.MASENKO, 6}, {Skill.MASENKO, 7}});
        FULL_SETS.put("FULL_ANTOMIC", new int[][]{{Skill.ANTOMIC, 1}, {Skill.ANTOMIC, 2}, {Skill.ANTOMIC, 3}, {Skill.ANTOMIC, 4}, {Skill.ANTOMIC, 5}, {Skill.ANTOMIC, 6}, {Skill.ANTOMIC, 7}});
        FULL_SETS.put("FULL_LIENHOAN", new int[][]{{Skill.LIEN_HOAN, 1}, {Skill.LIEN_HOAN, 2}, {Skill.LIEN_HOAN, 3}, {Skill.LIEN_HOAN, 4}, {Skill.LIEN_HOAN, 5}, {Skill.LIEN_HOAN, 6}, {Skill.LIEN_HOAN, 7}});
        FULL_SETS.put("FULL_TDHS", new int[][]{{Skill.THAI_DUONG_HA_SAN, 1}, {Skill.THAI_DUONG_HA_SAN, 2}, {Skill.THAI_DUONG_HA_SAN, 3}, {Skill.THAI_DUONG_HA_SAN, 4}, {Skill.THAI_DUONG_HA_SAN, 5}, {Skill.THAI_DUONG_HA_SAN, 6}, {Skill.THAI_DUONG_HA_SAN, 7}});
    }

    // Components
    private JTextField txtBossName, txtBossHp, txtBossDame, txtBossMap;
    private JComboBox<String> cboGender;
    private JComboBox<RespawnOption> cboBossRespawn;
    
    private JTextField txtOutfitHead, txtOutfitBody, txtOutfitLeg, txtOutfitBag, txtOutfitAura, txtOutfitEff;
    
    private JTextArea txtBossChat;
    private JList<String> listBosses;
    private JTable tableSkills;
    private DefaultTableModel modelSkills;
    private JLabel lblStatus;

    private BossData currentSelectedBossData;
    private String currentBossKey;
    private String previousBossKey; 
    private Vector<String> originalBossKeys;
    private JTextField txtSearchBoss;

    private Map<String, String> pendingChanges = new HashMap<>();
    
    private Map<Integer, Integer> partIconMap = new HashMap<>();
    private Map<Integer, ImageIcon> iconImageCache = new HashMap<>();
    private List<SkillOption> cachedSkillOptions = null;

    private final Font FONT_BOLD = FONT_SMALL_B;
    private final Font FONT_PLAIN = FONT_BODY;

    private static class SkillOption {
        int id;
        String name;
        int iconId;
        ImageIcon icon;

        public SkillOption(int id, String name, int iconId, ImageIcon icon) {
            this.id = id;
            this.name = name;
            this.iconId = iconId;
            this.icon = icon;
        }

        @Override
        public String toString() {
            return id + ". " + name; 
        }
    }

    private static class RespawnOption {
        String label;
        String codeConstant;
        int value;

        public RespawnOption(String label, String codeConstant, int value) {
            this.label = label;
            this.codeConstant = codeConstant;
            this.value = value;
        }

        @Override
        public String toString() {
            return label; 
        }
    }
    
    private Vector<RespawnOption> respawnOptions;

    public BossEditorPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        initRespawnOptions(); 
        loadPartDataFromDB(); 
        add(createBossEditorPanel());
    }

    private void initRespawnOptions() {
        respawnOptions = new Vector<>();
        respawnOptions.add(new RespawnOption("1 Giây", "REST_1_S", 1));
        respawnOptions.add(new RespawnOption("2 Giây", "REST_2_S", 2));
        respawnOptions.add(new RespawnOption("5 Giây", "REST_5_S", 5));
        respawnOptions.add(new RespawnOption("10 Giây", "REST_10_S", 10));
        respawnOptions.add(new RespawnOption("20 Giây", "REST_20_S", 20));
        respawnOptions.add(new RespawnOption("30 Giây", "REST_30_S", 30));
        respawnOptions.add(new RespawnOption("1 Phút", "REST_1_M", 60));
        respawnOptions.add(new RespawnOption("2 Phút", "REST_2_M", 120));
        respawnOptions.add(new RespawnOption("5 Phút", "REST_5_M", 300));
        respawnOptions.add(new RespawnOption("10 Phút", "REST_10_M", 600));
        respawnOptions.add(new RespawnOption("15 Phút", "REST_15_M", 900));
        respawnOptions.add(new RespawnOption("30 Phút", "REST_30_M", 1800));
        respawnOptions.add(new RespawnOption("2 Giờ", "REST_2_H", 7200));
        respawnOptions.add(new RespawnOption("24 Giờ", "REST_24_H", 86400));
        respawnOptions.add(new RespawnOption("Khác (Custom)", "", 0));
    }

    private Connection getConnection() throws SQLException {
        // Boss/part/item_template đều nằm ở DB dữ liệu game (nro_data)
        return DBConnecter.getConnectionData();
    }

    private void loadPartDataFromDB() {
        new Thread(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, DATA FROM part WHERE TYPE = 0")) { 
                
                partIconMap.clear();
                while (rs.next()) {
                    int partId = rs.getInt("id");
                    String json = rs.getString("DATA");
                    try {
                        JsonArray arr = new JsonParser().parse(json).getAsJsonArray();
                        if (arr.size() > 0) {
                            JsonArray firstLayer = arr.get(0).getAsJsonArray();
                            int iconId = firstLayer.get(0).getAsInt();
                            partIconMap.put(partId, iconId);
                        }
                    } catch (Exception e) {}
                }
                if (listBosses != null) SwingUtilities.invokeLater(() -> listBosses.repaint());
            } catch (Exception e) {
                System.err.println("Lỗi load Part DB: " + e.getMessage());
            }
        }).start();
    }

    private ImageIcon getIconByIconId(int iconId, int size) {
        if (iconId <= -1) return null;
        if (iconImageCache.containsKey(iconId)) {
             Image img = iconImageCache.get(iconId).getImage();
             if (img.getWidth(null) != size) {
                 return new ImageIcon(img.getScaledInstance(size, size, Image.SCALE_SMOOTH));
             }
             return iconImageCache.get(iconId);
        }
        try {
            String[] zoomLevels = {"x4", "x3", "x2", "x1"};
            for (String zoom : zoomLevels) {
                File f = new File(ICON_FOLDER + zoom + "/" + iconId + ".png");
                if (f.exists()) {
                    BufferedImage img = ImageIO.read(f);
                    Image dimg = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    ImageIcon icon = new ImageIcon(dimg);
                    iconImageCache.put(iconId, new ImageIcon(img));
                    return icon;
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    public JPanel createBossEditorPanel() {
        JPanel p = new JPanel(new BorderLayout(SP_SM, 0));
        p.setBorder(pad(SP_MD));
        p.setBackground(BG);

        originalBossKeys = new Vector<>();
        // Load lần đầu
        reloadBossListFromClass();

        listBosses = new JList<>(originalBossKeys);
        listBosses.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        listBosses.setFont(FONT_BODY);
        listBosses.setFixedCellHeight(40);
        listBosses.setBackground(SURFACE);
        listBosses.setSelectionBackground(PRIMARY_SOFT);
        listBosses.setSelectionForeground(TEXT);
        
        listBosses.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String bossKey = (String) value;
                BossData data = null;
                
                try {
                    Field field = BossesData.class.getField(bossKey);
                    data = (BossData) field.get(null);
                    lbl.setForeground(TEXT);
                } catch (Exception e) {}

                if (data != null && data.getOutfit() != null && data.getOutfit().length > 0) {
                    int headPartId = data.getOutfit()[0];
                    int iconId = partIconMap.getOrDefault(headPartId, headPartId);
                    ImageIcon icon = getIconByIconId(iconId, 32); 
                    if (icon != null) {
                        lbl.setIcon(icon);
                    }
                }
                
                lbl.setBorder(new EmptyBorder(0, SP_SM, 0, 0));
                lbl.setIconTextGap(10);
                return lbl;
            }
        });

        listBosses.addListSelectionListener(this::onBossSelected);

        JScrollPane scrollList = new JScrollPane(listBosses);
        scrollList.setBorder(softBorder());
        scrollList.getViewport().setBackground(SURFACE);
        scrollList.getVerticalScrollBar().setUnitIncrement(16);

        // --- TOP LEFT PANEL: SEARCH & BUTTONS ---
        JPanel pTopLeft = new JPanel(new BorderLayout(SP_SM, 0));
        pTopLeft.setOpaque(false);

        txtSearchBoss = searchField("Tìm kiếm...");

        // Nút Reload
        JButton btnReloadDB = button("Tải Lại", ButtonVariant.SECONDARY, "refresh");
        btnReloadDB.setToolTipText("Tải lại Danh sách & Cache");
        btnReloadDB.addActionListener(e -> {
            iconImageCache.clear();
            loadPartDataFromDB();   
            cachedSkillOptions = null;
            reloadBossListFromClass(); 
            filterBossList(); // Re-apply filter
            JOptionPane.showMessageDialog(this, "Đã tải lại danh sách boss và dữ liệu!");
        });

        pTopLeft.add(txtSearchBoss, BorderLayout.CENTER);
        pTopLeft.add(btnReloadDB, BorderLayout.EAST);

        txtSearchBoss.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { filterBossList(); }
            public void removeUpdate(DocumentEvent e) { filterBossList(); }
            public void changedUpdate(DocumentEvent e) { filterBossList(); }
        });

        JPanel leftTop = new JPanel(new BorderLayout());
        leftTop.setOpaque(false);
        leftTop.add(sectionHeader("Danh Sách Boss", "Chọn boss để chỉnh sửa", null), BorderLayout.NORTH);
        leftTop.add(pTopLeft, BorderLayout.CENTER);

        JPanel leftPanel = card(new BorderLayout(0, SP_MD));
        leftPanel.add(leftTop, BorderLayout.NORTH);
        leftPanel.add(scrollList, BorderLayout.CENTER);
        leftPanel.setPreferredSize(new Dimension(320, 0));

        // --- FORM PANEL ---
        // Sử dụng BoxLayout trục Y để xếp các panel con
        JPanel formPanel = new JPanel();
        formPanel.setLayout(new BoxLayout(formPanel, BoxLayout.Y_AXIS));
        formPanel.setOpaque(false);
        formPanel.setBorder(new EmptyBorder(0, SP_XS, 0, 0));

        // 1. INFO PANEL
        JPanel infoGrid = new JPanel(new GridBagLayout());
        infoGrid.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(SP_XS, SP_XS, SP_XS, SP_XS); g.fill = GridBagConstraints.HORIZONTAL; g.anchor = GridBagConstraints.WEST; g.weightx = 1.0;

        txtBossName = createStyledTextField(TEXT);
        txtBossHp = createStyledTextField(DANGER); // Đỏ đậm
        txtBossHp.setFont(FONT_MONO.deriveFont(Font.BOLD, 14f));
        txtBossHp.setEditable(true);
        txtBossHp.setFocusable(true);
        txtBossHp.setToolTipText("Nhập lượng HP của boss (ví dụ: 100000000 hoặc nhiều mốc: 100000000, 120000000)");
        txtBossDame = createStyledTextField(INFO); // Xanh đậm
        txtBossDame.setFont(FONT_MONO.deriveFont(Font.BOLD, 14f));

        cboGender = new JComboBox<>(new String[]{"0 - Trái Đất", "1 - Namếc", "2 - Xayda"});
        cboGender.setFont(FONT_PLAIN);
        cboGender.setBackground(SURFACE);

        addFormRow(infoGrid, g, 0, "Tên Boss:", txtBossName);
        addFormRow(infoGrid, g, 1, "Hành Tinh:", cboGender);
        addFormRow(infoGrid, g, 2, "Máu (HP):", txtBossHp);
        addFormRow(infoGrid, g, 3, "Sức Đánh:", txtBossDame);

        JPanel infoPanel = card(new BorderLayout());
        infoPanel.add(sectionHeader("Thông Tin Cơ Bản", "Tên, máu và sức đánh của boss", null), BorderLayout.NORTH);
        infoPanel.add(infoGrid, BorderLayout.CENTER);
        infoPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, infoPanel.getPreferredSize().height));

        // 2. APPEARANCE PANEL
        JPanel pOutfitFields = new JPanel(new GridLayout(1, 6, SP_SM, 0));
        pOutfitFields.setOpaque(false);
        txtOutfitHead = createStyledOutfitField("Đầu");
        txtOutfitBody = createStyledOutfitField("Thân");
        txtOutfitLeg = createStyledOutfitField("Chân");
        txtOutfitBag = createStyledOutfitField("Cánh");
        txtOutfitAura = createStyledOutfitField("Aura");
        txtOutfitEff = createStyledOutfitField("Eff");

        pOutfitFields.add(outfitFieldBox(txtOutfitHead)); pOutfitFields.add(outfitFieldBox(txtOutfitBody)); pOutfitFields.add(outfitFieldBox(txtOutfitLeg));
        pOutfitFields.add(outfitFieldBox(txtOutfitBag)); pOutfitFields.add(outfitFieldBox(txtOutfitAura)); pOutfitFields.add(outfitFieldBox(txtOutfitEff));

        JButton btnSearchCaiTrang = button("Tìm Skin (Type 5)", ButtonVariant.SECONDARY, "search");
        btnSearchCaiTrang.addActionListener(e -> openCaiTrangSearchDialog());

        JPanel pAppearHeader = new JPanel(new GridBagLayout());
        pAppearHeader.setOpaque(false);
        pAppearHeader.add(btnSearchCaiTrang);

        JPanel appearPanel = card(new BorderLayout());
        appearPanel.add(sectionHeader("Ngoại Hình (Outfit)", "ID trang bị hiển thị của boss", pAppearHeader), BorderLayout.NORTH);
        appearPanel.add(pOutfitFields, BorderLayout.CENTER);
        appearPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, appearPanel.getPreferredSize().height));

        // 3. LOCATION & RESPAWN
        JPanel locGrid = new JPanel(new GridBagLayout());
        locGrid.setOpaque(false);

        txtBossMap = createStyledTextField(TEXT);
        JButton btnSearchMap = button("Tìm Map", ButtonVariant.SECONDARY, "search");
        btnSearchMap.addActionListener(e -> openMapSearchDialog());

        JPanel pMapCont = new JPanel(new BorderLayout(SP_SM, 0));
        pMapCont.setOpaque(false);
        pMapCont.add(txtBossMap, BorderLayout.CENTER);
        pMapCont.add(btnSearchMap, BorderLayout.EAST);

        cboBossRespawn = new JComboBox<>(respawnOptions);
        cboBossRespawn.setFont(FONT_PLAIN);
        cboBossRespawn.setBackground(SURFACE);

        addFormRow(locGrid, g, 0, "Map ID:", pMapCont);
        addFormRow(locGrid, g, 1, "T.Gian Hồi:", cboBossRespawn);

        JPanel locPanel = card(new BorderLayout());
        locPanel.add(sectionHeader("Vị Trí & Hồi Sinh", "Map xuất hiện và thời gian hồi sinh", null), BorderLayout.NORTH);
        locPanel.add(locGrid, BorderLayout.CENTER);
        locPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, locPanel.getPreferredSize().height));

        // 4. SKILLS & CHAT
        JPanel miscGrid = new JPanel(new GridBagLayout());
        miscGrid.setOpaque(false);

        // Skill Table
        String[] skillCols = {"Tên Chiêu / Set", "Level", "Delay (ms)"};
        modelSkills = new DefaultTableModel(skillCols, 0);
        tableSkills = new JTable(modelSkills);
        JScrollPane scrollTable = styleTable(tableSkills);
        scrollTable.setPreferredSize(new Dimension(0, 140));

        JPanel pSkillBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pSkillBtns.setOpaque(false);
        JButton btnAddSkill = button("Thêm Skill / Set", ButtonVariant.SUCCESS, "plus");
        JButton btnDelSkill = button("Xóa", ButtonVariant.DANGER, "trash");
        pSkillBtns.add(btnAddSkill); pSkillBtns.add(btnDelSkill);

        btnAddSkill.addActionListener(e -> openSkillSelectionDialog());
        Runnable delSkillAction = () -> {
            if (tableSkills.isEditing() && tableSkills.getCellEditor() != null) {
                tableSkills.getCellEditor().stopCellEditing();
            }
            int[] selectedRows = tableSkills.getSelectedRows();
            if (selectedRows != null && selectedRows.length > 0) {
                for (int i = selectedRows.length - 1; i >= 0; i--) {
                    if (selectedRows[i] >= 0 && selectedRows[i] < modelSkills.getRowCount()) {
                        modelSkills.removeRow(selectedRows[i]);
                    }
                }
            } else if (tableSkills.getSelectedRow() >= 0 && tableSkills.getSelectedRow() < modelSkills.getRowCount()) {
                modelSkills.removeRow(tableSkills.getSelectedRow());
            }
        };
        btnDelSkill.addActionListener(e -> delSkillAction.run());
        tableSkills.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_DELETE) {
                    delSkillAction.run();
                }
            }
        });

        // Chat Area - Mở rộng size
        txtBossChat = new JTextArea(8, 20); // Tăng lên 8 dòng
        txtBossChat.setFont(FONT_BODY);
        txtBossChat.setLineWrap(true);
        txtBossChat.setWrapStyleWord(true);
        JScrollPane scrollChat = new JScrollPane(txtBossChat);
        scrollChat.setBorder(softBorder());
        scrollChat.getViewport().setBackground(SURFACE);

        JPanel chatWrap = new JPanel(new BorderLayout(0, SP_XS));
        chatWrap.setOpaque(false);
        chatWrap.add(label("Nội Dung Hội Thoại (Xuống dòng để tách câu)", FONT_SMALL_B, TEXT_SECONDARY), BorderLayout.NORTH);
        chatWrap.add(scrollChat, BorderLayout.CENTER);

        // Layout Misc Panel
        g.gridx = 0; g.gridy = 0; g.weightx = 0;
        miscGrid.add(label("Skills:", FONT_SMALL_B, TEXT_SECONDARY), g);

        g.gridx = 1; g.weightx = 1;
        JPanel pSkillCont = new JPanel(new BorderLayout(0, SP_SM));
        pSkillCont.setOpaque(false);
        pSkillCont.add(scrollTable, BorderLayout.CENTER);
        pSkillCont.add(pSkillBtns, BorderLayout.SOUTH);
        miscGrid.add(pSkillCont, g);

        g.gridx = 0; g.gridy = 1; g.gridwidth = 2; g.weightx = 1; g.fill = GridBagConstraints.BOTH; g.weighty = 1.0;
        g.insets = new Insets(SP_MD, SP_XS, SP_XS, SP_XS);
        miscGrid.add(chatWrap, g);

        JPanel miscPanel = card(new BorderLayout());
        miscPanel.add(sectionHeader("Kỹ Năng & Hội Thoại", "Danh sách chiêu thức và câu thoại của boss", null), BorderLayout.NORTH);
        miscPanel.add(miscGrid, BorderLayout.CENTER);

        // Add all panels to form
        formPanel.add(infoPanel);
        formPanel.add(Box.createVerticalStrut(SP_SM));
        formPanel.add(appearPanel);
        formPanel.add(Box.createVerticalStrut(SP_SM));
        formPanel.add(locPanel);
        formPanel.add(Box.createVerticalStrut(SP_SM));
        formPanel.add(miscPanel);

        // --- BOTTOM BUTTONS ---
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        btnPanel.setOpaque(false);
        btnPanel.setBorder(padV(SP_SM));

        lblStatus = new JLabel("");
        lblStatus.setFont(FONT_BODY_B.deriveFont(Font.BOLD | Font.ITALIC));
        lblStatus.setForeground(SUCCESS);

        JButton btnSyncRealtime = button("ĐỒNG BỘ REALTIME", ButtonVariant.SECONDARY, "refresh");
        btnSyncRealtime.setToolTipText("Áp dụng ngay các thông số vào Boss đang chạy trong game (RAM)");
        btnSyncRealtime.setPreferredSize(new Dimension(180, 42));
        btnSyncRealtime.addActionListener(e -> syncCurrentBossRealtime());

        JButton btnSaveCurrent = button("LƯU BOSS NÀY", ButtonVariant.SUCCESS, "save");
        btnSaveCurrent.setToolTipText("Ghi vào file code BossesData.java & đồng bộ Realtime boss này");
        btnSaveCurrent.setPreferredSize(new Dimension(170, 42));
        btnSaveCurrent.addActionListener(e -> saveCurrentBossToFileAndRealtime());

        JButton btnSaveAll = button("LƯU TẤT CẢ", ButtonVariant.PRIMARY, "save");
        btnSaveAll.setToolTipText("Ghi tất cả Boss đã chỉnh sửa vào file code & đồng bộ Realtime toàn bộ");
        btnSaveAll.setPreferredSize(new Dimension(160, 42));
        btnSaveAll.addActionListener(e -> commitAllChangesToFile());

        btnPanel.add(lblStatus);
        btnPanel.add(Box.createHorizontalStrut(SP_MD));
        btnPanel.add(btnSyncRealtime);
        btnPanel.add(btnSaveCurrent);
        btnPanel.add(btnSaveAll);

        JPanel rightPanel = new JPanel(new BorderLayout());
        rightPanel.setOpaque(false);
        JScrollPane formScroll = new JScrollPane(formPanel);
        formScroll.setBorder(BorderFactory.createEmptyBorder());
        formScroll.getViewport().setBackground(BG);
        formScroll.getVerticalScrollBar().setUnitIncrement(16);
        rightPanel.add(formScroll, BorderLayout.CENTER);
        rightPanel.add(btnPanel, BorderLayout.SOUTH);

        p.add(leftPanel, BorderLayout.WEST);
        p.add(rightPanel, BorderLayout.CENTER);
        return p;
    }

    // --- LOGIC LOAD LIST TỪ CLASS (RELOAD) ---
    private void reloadBossListFromClass() {
        originalBossKeys.clear();
        try {
            Field[] fields = BossesData.class.getFields();
            for (Field field : fields) {
                if (field.getType() == BossData.class) {
                    originalBossKeys.add(field.getName());
                }
            }
        } catch (Exception e) {
            originalBossKeys.add("Error Loading");
        }
    }

    // --- DIALOG CHỌN KỸ NĂNG 2 TAB (LẺ & SET) ---
    private void openSkillSelectionDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Thêm Kỹ Năng", true);
        d.setSize(500, 600);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout());
        d.getContentPane().setBackground(BG);

        JTabbedPane tab = new JTabbedPane();
        
        // --- CHUẨN BỊ DỮ LIỆU SKILL OPTION (CACHE) ---
        if (cachedSkillOptions == null) {
            cachedSkillOptions = new ArrayList<>();
            Set<Integer> addedIds = new HashSet<>(); 
             try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id FROM skill_template ORDER BY id ASC")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    if (!addedIds.contains(id)) {
                        cachedSkillOptions.add(new SkillOption(id, rs.getString("name"), rs.getInt("icon_id"), getIconByIconId(rs.getInt("icon_id"), 25)));
                        addedIds.add(id);
                    }
                }
            } catch (Exception e) {}
        }

        // TAB 1: SKILLS LẺ (Cũ)
        JPanel pSingle = new JPanel(new BorderLayout(0, SP_SM));
        pSingle.setBackground(BG);
        pSingle.setBorder(pad(SP_MD));
        JList<SkillOption> listSkills = new JList<>(new Vector<>(cachedSkillOptions));
        listSkills.setFixedCellHeight(35);
        listSkills.setFont(FONT_BODY);
        listSkills.setBackground(SURFACE);
        listSkills.setSelectionBackground(PRIMARY_SOFT);
        listSkills.setSelectionForeground(TEXT);
        listSkills.setCellRenderer(new DefaultListCellRenderer() {
             public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                if (value instanceof SkillOption) {
                    SkillOption skill = (SkillOption) value;
                    lbl.setText(skill.toString());
                    if (skill.icon != null) lbl.setIcon(skill.icon);
                    lbl.setIconTextGap(10);
                }
                return lbl;
             }
        });

        // Search cho Tab Skill Lẻ
        JTextField txtSearchSkill = searchField("Tìm kiếm Kỹ Năng...");
        txtSearchSkill.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }
            void f() {
                String q = txtSearchSkill.getText().toLowerCase();
                Vector<SkillOption> filtered = new Vector<>();
                for (SkillOption s : cachedSkillOptions) {
                    if (s.name.toLowerCase().contains(q) || String.valueOf(s.id).contains(q)) filtered.add(s);
                }
                listSkills.setListData(filtered);
            }
        });

        Runnable addSingleAction = () -> {
            SkillOption sel = listSkills.getSelectedValue();
            if (sel != null) {
                modelSkills.addRow(new Object[]{sel.toString(), "7", "1000"});
                d.dispose();
            }
        };

        // Double click để chọn luôn
        listSkills.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSingleAction.run();
                }
            }
        });

        JButton btnSelectSingle = button("Thêm Skill Lẻ", ButtonVariant.SUCCESS, "plus");
        btnSelectSingle.setPreferredSize(new Dimension(0, 40));
        btnSelectSingle.addActionListener(e -> addSingleAction.run());

        JScrollPane spSkillList = new JScrollPane(listSkills);
        spSkillList.setBorder(softBorder());
        spSkillList.getViewport().setBackground(SURFACE);

        pSingle.add(txtSearchSkill, BorderLayout.NORTH);
        pSingle.add(spSkillList, BorderLayout.CENTER);
        pSingle.add(btnSelectSingle, BorderLayout.SOUTH);
        
        // TAB 2: FULL SETS (Mới & Đã Sửa Icon)
        JPanel pSets = new JPanel(new BorderLayout(0, SP_SM));
        pSets.setBackground(BG);
        pSets.setBorder(pad(SP_MD));
        Vector<String> setNames = new Vector<>(FULL_SETS.keySet());
        JList<String> listSets = new JList<>(setNames);
        listSets.setFixedCellHeight(40);
        listSets.setFont(FONT_BODY_B);
        listSets.setBackground(SURFACE);
        listSets.setSelectionBackground(PRIMARY_SOFT);
        listSets.setSelectionForeground(TEXT);
        listSets.setCellRenderer(new DefaultListCellRenderer() {
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel l = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                String setName = (String) value;
                
                // --- FIX ICON: Tìm icon của skill đầu tiên trong set ---
                ImageIcon icon = null;
                if (FULL_SETS.containsKey(setName)) {
                    int[][] skills = FULL_SETS.get(setName);
                    if (skills.length > 0) {
                        int firstSkillId = skills[0][0];
                        // Tìm trong cache để lấy icon
                        for (SkillOption opt : cachedSkillOptions) {
                            if (opt.id == firstSkillId) {
                                icon = opt.icon;
                                break;
                            }
                        }
                    }
                }
                
                if (icon != null) {
                    l.setIcon(icon);
                } else {
                    l.setIcon(getIconByIconId(1234, 25)); // Fallback
                }
                
                l.setBorder(new EmptyBorder(0, SP_MD, 0, 0));
                return l;
            }
        });

        Runnable addSetAction = () -> {
            String val = listSets.getSelectedValue();
            if (val != null) {
                // Thêm vào bảng dưới dạng đặc biệt "Set: NAME"
                modelSkills.addRow(new Object[]{"Set: " + val, "-", "-"});
                d.dispose();
            }
        };
        
        // Double click để chọn Set
        listSets.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    addSetAction.run();
                }
            }
        });
        
        JButton btnSelectSet = button("Thêm Bộ Skill (Full Set)", ButtonVariant.PRIMARY, "plus");
        btnSelectSet.setPreferredSize(new Dimension(0, 40));
        btnSelectSet.addActionListener(e -> addSetAction.run());

        JScrollPane spSetList = new JScrollPane(listSets);
        spSetList.setBorder(softBorder());
        spSetList.getViewport().setBackground(SURFACE);

        pSets.add(spSetList, BorderLayout.CENTER);
        pSets.add(btnSelectSet, BorderLayout.SOUTH);
        
        tab.addTab("Skill Đơn Lẻ", pSingle);
        tab.addTab("Bộ Skill (Full Set)", pSets);
        
        d.add(tab, BorderLayout.CENTER);
        d.setVisible(true);
    }

    // --- DIALOG MAP ---
    private void openMapSearchDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tìm Kiếm Map (map_template)", true);
        d.setSize(600, 650);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(0, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JComponent) d.getContentPane()).setBorder(pad(SP_MD));

        JTextField txtSearchMap = searchField("Nhập Tên Map hoặc ID...");
        
        DefaultTableModel m = new DefaultTableModel(new Object[]{"Chọn", "ID", "Tên Map"}, 0) {
            @Override public Class<?> getColumnClass(int columnIndex) { return columnIndex == 0 ? Boolean.class : Object.class; }
            @Override public boolean isCellEditable(int r, int c) { return c == 0; }
        };
        
        JTable t = new JTable(m);
        JScrollPane spMaps = styleTable(t);
        t.getColumnModel().getColumn(0).setCellRenderer(null); // giữ checkbox mặc định
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(0).setMaxWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(60);
        t.getColumnModel().getColumn(1).setMaxWidth(80);

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        // Logic đọc Map ID hiện tại để tích sẵn
        Set<String> existingIds = new HashSet<>();
        String currentText = txtBossMap.getText().trim();
        if (!currentText.isEmpty()) {
            String[] parts = currentText.split(",");
            for (String part : parts) existingIds.add(part.trim());
        }

        new Thread(() -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name FROM map_template")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    boolean isChecked = existingIds.contains(String.valueOf(id));
                    SwingUtilities.invokeLater(() -> m.addRow(new Object[]{isChecked, id, name}));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        txtSearchMap.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }
            void f() {
                String text = txtSearchMap.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else {
                    var idFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 1);
                    var nameFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2);
                    sorter.setRowFilter(RowFilter.orFilter(Arrays.asList(idFilter, nameFilter)));
                }
            }
        });

        Runnable addCheckedMaps = () -> {
            List<String> idsToAdd = new ArrayList<>();
            for (int i = 0; i < m.getRowCount(); i++) {
                Boolean isChecked = (Boolean) m.getValueAt(i, 0);
                if (isChecked != null && isChecked) {
                    idsToAdd.add(m.getValueAt(i, 1).toString());
                }
            }
            if (idsToAdd.isEmpty()) {
                JOptionPane.showMessageDialog(d, "Bạn chưa tích chọn map nào!");
                return;
            }
            String newText = String.join(",", idsToAdd);
            txtBossMap.setText(newText);
            d.dispose();
        };

        JButton btnAddSelected = button("Xác Nhận Map Đã Chọn", ButtonVariant.SUCCESS, "check");
        btnAddSelected.setPreferredSize(new Dimension(0, 48));
        btnAddSelected.addActionListener(e -> addCheckedMaps.run());

        d.add(txtSearchMap, BorderLayout.NORTH);
        d.add(spMaps, BorderLayout.CENTER);
        d.add(btnAddSelected, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    // --- DIALOG CAI TRANG ---
    private void openCaiTrangSearchDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Tìm Kiếm Cải Trang (Type 5)", true);
        d.setSize(650, 550);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(0, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JComponent) d.getContentPane()).setBorder(pad(SP_MD));

        JTextField txtSearchCT = searchField("Nhập Tên hoặc ID Cải Trang...");
        
        DefaultTableModel m = new DefaultTableModel(new String[]{"ID", "Icon", "Tên Item", "Head", "Body", "Leg"}, 0) {
            @Override public Class<?> getColumnClass(int c) { return c == 1 ? ImageIcon.class : Object.class; }
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        
        JTable t = new JTable(m);
        JScrollPane spItems = styleTable(t);
        t.setRowHeight(35);
        t.getColumnModel().getColumn(1).setCellRenderer(null); // giữ renderer icon mặc định
        t.getColumnModel().getColumn(0).setPreferredWidth(50);
        t.getColumnModel().getColumn(1).setPreferredWidth(40);
        t.getColumnModel().getColumn(2).setPreferredWidth(250);
        
        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(m);
        t.setRowSorter(sorter);

        new Thread(() -> {
            try (Connection conn = getConnection(); Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT id, name, icon_id, head, body, leg FROM item_template WHERE type = 5")) {
                while (rs.next()) {
                    int id = rs.getInt("id");
                    String name = rs.getString("name");
                    int iconId = rs.getInt("icon_id");
                    int head = rs.getInt("head");
                    int body = rs.getInt("body");
                    int leg = rs.getInt("leg");
                    ImageIcon icon = getIconByIconId(iconId, 28);
                    SwingUtilities.invokeLater(() -> m.addRow(new Object[]{id, icon, name, head, body, leg}));
                }
            } catch (Exception e) { e.printStackTrace(); }
        }).start();

        txtSearchCT.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { f(); }
            public void removeUpdate(DocumentEvent e) { f(); }
            public void changedUpdate(DocumentEvent e) { f(); }
            void f() {
                String text = txtSearchCT.getText().trim();
                if (text.isEmpty()) sorter.setRowFilter(null);
                else {
                    var idFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 0);
                    var nameFilter = RowFilter.regexFilter("(?i)" + Pattern.quote(text), 2);
                    sorter.setRowFilter(RowFilter.orFilter(Arrays.asList(idFilter, nameFilter)));
                }
            }
        });

        t.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int r = t.getSelectedRow();
                    if (r != -1) {
                        int modelRow = t.convertRowIndexToModel(r);
                        txtOutfitHead.setText(m.getValueAt(modelRow, 3).toString());
                        txtOutfitBody.setText(m.getValueAt(modelRow, 4).toString());
                        txtOutfitLeg.setText(m.getValueAt(modelRow, 5).toString());
                        d.dispose();
                    }
                }
            }
        });

        d.add(txtSearchCT, BorderLayout.NORTH);
        d.add(spItems, BorderLayout.CENTER);
        d.setVisible(true);
    }

    private void filterBossList() {
        String query = txtSearchBoss.getText().trim().toLowerCase();
        Vector<String> filtered = new Vector<>();
        if (originalBossKeys != null) {
            for (String key : originalBossKeys) {
                if (key.toLowerCase().contains(query)) filtered.add(key);
            }
        }
        listBosses.setListData(filtered);
    }

    private void onBossSelected(ListSelectionEvent e) {
        if (e.getValueIsAdjusting()) return;

        if (previousBossKey != null && !previousBossKey.isEmpty()) {
            saveCurrentConfigToMemory(previousBossKey);
        }

        String selectedKey = listBosses.getSelectedValue();
        if (selectedKey == null) return;
        
        currentBossKey = selectedKey;
        previousBossKey = currentBossKey; 

        try {
            Field field = BossesData.class.getField(currentBossKey);
            currentSelectedBossData = (BossData) field.get(null);
        } catch (Exception ex) { ex.printStackTrace(); }

        if (currentSelectedBossData != null) {
            txtBossName.setText(currentSelectedBossData.getName());
            if (cboGender != null) {
                cboGender.setSelectedIndex(Math.max(0, Math.min(2, currentSelectedBossData.getGender())));
            }

            StringBuilder sbHp = new StringBuilder();
            if (currentSelectedBossData.getHp() != null) {
                for (long hp : currentSelectedBossData.getHp()) {
                    sbHp.append(formatLong(hp)).append(", ");
                }
                if (sbHp.length() > 2) sbHp.setLength(sbHp.length() - 2);
            }
            txtBossHp.setText(sbHp.toString());
            txtBossDame.setText(formatLong(currentSelectedBossData.getDame()));

            short[] outfit = currentSelectedBossData.getOutfit();
            txtOutfitHead.setText("-1"); txtOutfitBody.setText("-1"); txtOutfitLeg.setText("-1");
            txtOutfitBag.setText("-1"); txtOutfitAura.setText("-1"); txtOutfitEff.setText("-1");

            if (outfit != null) {
                if (outfit.length > 0) txtOutfitHead.setText(String.valueOf(outfit[0]));
                if (outfit.length > 1) txtOutfitBody.setText(String.valueOf(outfit[1]));
                if (outfit.length > 2) txtOutfitLeg.setText(String.valueOf(outfit[2]));
                if (outfit.length > 3) txtOutfitBag.setText(String.valueOf(outfit[3]));
                if (outfit.length > 4) txtOutfitAura.setText(String.valueOf(outfit[4]));
                if (outfit.length > 5) txtOutfitEff.setText(String.valueOf(outfit[5]));
            }

            int[] maps = currentSelectedBossData.getMapJoin();
            StringBuilder sbMap = new StringBuilder();
            if (maps != null) {
                for (int m : maps) sbMap.append(m).append(",");
                if (sbMap.length() > 0) sbMap.setLength(sbMap.length() - 1);
            }
            txtBossMap.setText(sbMap.toString());

            int respawnVal = currentSelectedBossData.getSecondsRest();
            boolean found = false;
            for (int i = 0; i < cboBossRespawn.getItemCount(); i++) {
                RespawnOption opt = cboBossRespawn.getItemAt(i);
                if (opt.value == respawnVal) {
                    cboBossRespawn.setSelectedIndex(i);
                    found = true;
                    break;
                }
            }
            if (!found) {
                RespawnOption custom = respawnOptions.lastElement(); 
                custom.value = respawnVal;
                custom.label = "Custom: " + respawnVal;
                cboBossRespawn.setSelectedIndex(cboBossRespawn.getItemCount() - 1);
            }

            // LOAD SKILLS & AUTO-DETECT SETS
            modelSkills.setRowCount(0);
            int[][] skills = currentSelectedBossData.getSkillTemp();
            if (skills != null) {
                int i = 0;
                while (i < skills.length) {
                    // Kiểm tra xem 7 skill tiếp theo có phải là 1 Set không
                    String detectedSet = detectSet(skills, i);
                    if (detectedSet != null) {
                        modelSkills.addRow(new Object[]{"Set: " + detectedSet, "-", "-"});
                        i += 7; // Nhảy qua 7 skill đã được gom nhóm
                    } else {
                        // Skill lẻ bình thường
                        int[] sk = skills[i];
                        if (sk.length >= 3) {
                             modelSkills.addRow(new Object[]{getSkillName(sk[0]), sk[1], sk[2]});
                        } else if (sk.length == 2) {
                             modelSkills.addRow(new Object[]{getSkillName(sk[0]), sk[1], "1000"});
                        }
                        i++;
                    }
                }
            }

            StringBuilder sbChat = new StringBuilder();
            String[][] allChats = {currentSelectedBossData.getTextS(), currentSelectedBossData.getTextM(), currentSelectedBossData.getTextE()};
            for (String[] group : allChats) {
                if (group != null) {
                    for (String s : group) sbChat.append(s).append("\n");
                }
            }
            txtBossChat.setText(sbChat.toString());
        }
    }
    
    // Hàm nhận diện Set từ mảng int[][] raw
    private String detectSet(int[][] source, int startIndex) {
        if (startIndex + 7 > source.length) return null; // Không đủ 7 skill để check
        
        for (Map.Entry<String, int[][]> entry : FULL_SETS.entrySet()) {
            int[][] setSkills = entry.getValue();
            boolean match = true;
            for (int k = 0; k < 7; k++) {
                int[] srcSkill = source[startIndex + k];
                int[] setSkill = setSkills[k];
                // Check ID và Level (srcSkill[0] == setSkill[0] && srcSkill[1] == setSkill[1])
                if (srcSkill[0] != setSkill[0] || srcSkill[1] != setSkill[1]) {
                    match = false;
                    break;
                }
            }
            if (match) return entry.getKey(); // Trả về tên set (VD: FULL_DRAGON)
        }
        return null;
    }

    private void saveCurrentConfigToMemory(String keyToSave) {
        try {
            // 1. CHUẨN BỊ DỮ LIỆU THÔ (RAW DATA)
            String nameRaw = txtBossName.getText().trim();
            long dameRaw = parseLong(txtBossDame.getText());

            long[] hpRaw = parseHpArray(txtBossHp.getText());
            StringBuilder sbHpCode = new StringBuilder("new long[]{");
            for (int i = 0; i < hpRaw.length; i++) {
                sbHpCode.append(hpRaw[i]).append("L");
                if (i < hpRaw.length - 1) sbHpCode.append(", ");
            }
            sbHpCode.append("}");

            String[] outParts = {txtOutfitHead.getText(), txtOutfitBody.getText(), txtOutfitLeg.getText(),
                    txtOutfitBag.getText(), txtOutfitAura.getText(), txtOutfitEff.getText()};
            short[] outfitRaw = new short[6];
            StringBuilder sbOutCode = new StringBuilder("new short[]{");
            for (int i = 0; i < 6; i++) {
                String val = outParts[i].trim().isEmpty() ? "-1" : outParts[i].trim();
                try {
                    outfitRaw[i] = Short.parseShort(val);
                } catch (Exception e) {
                    outfitRaw[i] = -1;
                }
                sbOutCode.append(outfitRaw[i]).append(i < 5 ? ", " : "}");
            }

            String mapTxt = txtBossMap.getText().trim();
            int[] mapRaw;
            if (mapTxt.isEmpty()) mapRaw = new int[0];
            else {
                String[] mSplit = mapTxt.split(",");
                List<Integer> validMaps = new ArrayList<>();
                for (String m : mSplit) {
                    try {
                        String clean = m.trim();
                        if (!clean.isEmpty()) validMaps.add(Integer.parseInt(clean));
                    } catch (Exception ignored) {}
                }
                mapRaw = new int[validMaps.size()];
                for (int i = 0; i < validMaps.size(); i++) mapRaw[i] = validMaps.get(i);
            }
            StringBuilder sbMapCode = new StringBuilder("new int[]{");
            for (int i = 0; i < mapRaw.length; i++) {
                sbMapCode.append(mapRaw[i]);
                if (i < mapRaw.length - 1) sbMapCode.append(", ");
            }
            sbMapCode.append("}");
            String mapCode = sbMapCode.toString();

            // Skill Generation (Updated Logic: Mixed Manual & Full Sets)
            String skillCode = getSkillCodeFromTable(modelSkills);
            
            // Prepare raw int[][] for memory update (Must expand Full Sets back to raw)
            int[][] skillRaw = getRawSkillsFromTable(modelSkills);

            // Chat
            String[] chatLines = txtBossChat.getText().split("\n");
            List<String> validChats = new ArrayList<>();
            StringBuilder sbChatCode = new StringBuilder("new String[]{");
            boolean hasChat = false;
            for (String s : chatLines) {
                if (!s.trim().isEmpty()) {
                    validChats.add(s.trim());
                    if (hasChat) sbChatCode.append(", ");
                    sbChatCode.append("\"").append(s.trim().replace("\"", "\\\"")).append("\"");
                    hasChat = true;
                }
            }
            sbChatCode.append("}");
            String[] chatRaw = validChats.toArray(new String[0]);

            // Respawn
            RespawnOption selectedOpt = (RespawnOption) cboBossRespawn.getSelectedItem();
            int respawnValueRaw = (selectedOpt != null) ? selectedOpt.value : 1000;
            String respawnCodeStr;
            if (selectedOpt != null && !selectedOpt.codeConstant.isEmpty()) {
                respawnCodeStr = selectedOpt.codeConstant;
            } else {
                respawnCodeStr = String.valueOf(respawnValueRaw);
            }
            
            // Gender Logic
            byte genderRaw = (cboGender != null) ? (byte) cboGender.getSelectedIndex() : 0;
            if (genderRaw < 0 || genderRaw > 2) {
                if (currentSelectedBossData != null) genderRaw = currentSelectedBossData.getGender();
                else genderRaw = 0;
            }
            String genderCode = switch (genderRaw) {
                case 1 -> "ConstPlayer.NAMEC";
                case 2 -> "ConstPlayer.XAYDA";
                default -> "ConstPlayer.TRAI_DAT";
            };
            String nameCode = "\"" + nameRaw.replace("\"", "\\\"") + "\"";

            // 2. TẠO CODE STRING (ĐỂ LƯU FILE)
            StringBuilder newCode = new StringBuilder();
            newCode.append("public static final BossData ").append(keyToSave).append(" = new BossData(\n");
            newCode.append("            ").append(nameCode).append(", //name\n");
            newCode.append("            ").append(genderCode).append(", //gender\n");
            newCode.append("            ").append(sbOutCode).append(", //outfit\n");
            newCode.append("            ").append(dameRaw).append(", //dame\n");
            newCode.append("            ").append(sbHpCode).append(", //hp\n");
            newCode.append("            ").append(mapCode).append(", //map\n");
            newCode.append("            ").append(skillCode).append(", //skill\n");
            newCode.append("            new String[]{}, //text chat 1\n");
            newCode.append("            ").append(sbChatCode).append(", //text chat 2\n");
            newCode.append("            new String[]{}, //text chat 3\n");
            if (currentSelectedBossData != null && currentSelectedBossData.getBossesAppearTogether() != null && currentSelectedBossData.getBossesAppearTogether().length > 0) {
                StringBuilder sbBat = new StringBuilder(",\n            new int[]{");
                for (int i = 0; i < currentSelectedBossData.getBossesAppearTogether().length; i++) {
                    if (i > 0) sbBat.append(", ");
                    sbBat.append(currentSelectedBossData.getBossesAppearTogether()[i]);
                }
                sbBat.append("}");
                newCode.append("            ").append(respawnCodeStr).append(sbBat.toString()).append("\n");
            } else if (currentSelectedBossData != null && currentSelectedBossData.getTypeAppear() != null && currentSelectedBossData.getTypeAppear() != consts.AppearType.DEFAULT_APPEAR) {
                newCode.append("            AppearType.").append(currentSelectedBossData.getTypeAppear().name()).append("\n");
            } else {
                newCode.append("            ").append(respawnCodeStr).append("\n");
            }
            newCode.append("    )");
            pendingChanges.put(keyToSave, newCode.toString());
            
            // 3. CẬP NHẬT TRỰC TIẾP VÀO OBJECT TRONG RAM (ĐỂ UI KHÔNG BỊ RESET)
            if (currentBossKey != null && currentBossKey.equals(keyToSave) && currentSelectedBossData != null) {
                updateObjectInMemory(currentSelectedBossData, nameRaw, genderRaw, outfitRaw, dameRaw, hpRaw, mapRaw, skillRaw, chatRaw, respawnValueRaw);
            }
            try {
                Field field = BossesData.class.getField(keyToSave);
                BossData oldBossData = (BossData) field.get(null);
                if (oldBossData != null && oldBossData != currentSelectedBossData) {
                    updateObjectInMemory(oldBossData, nameRaw, genderRaw, outfitRaw, dameRaw, hpRaw, mapRaw, skillRaw, chatRaw, respawnValueRaw);
                }
            } catch(Exception ex) {}
            
        } catch (Exception e) {
            System.out.println("Lỗi parse data khi chuyển boss: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // --- HELPER: Tạo chuỗi code skill để lưu file ---
    private String getSkillCodeFromTable(DefaultTableModel model) {
        List<String> arrayParts = new ArrayList<>(); // Các Full Set (VD: FULL_DRAGON)
        List<String> manualParts = new ArrayList<>(); // Các Skill lẻ

        for (int i = 0; i < model.getRowCount(); i++) {
            String name = model.getValueAt(i, 0).toString();
            if (name.startsWith("Set: ")) {
                // Đây là dòng Full Set
                String setName = name.replace("Set: ", "").trim();
                arrayParts.add(setName);
            } else {
                // Đây là skill lẻ
                int id = getSkillIdFromName(name);
                int level = 7;
                int delay = 1000;
                try {
                    String lStr = model.getValueAt(i, 1) != null ? model.getValueAt(i, 1).toString().replaceAll("[^0-9]", "") : "";
                    if (!lStr.isEmpty()) level = Integer.parseInt(lStr);
                } catch (Exception ignored) {}
                try {
                    String dStr = model.getValueAt(i, 2) != null ? model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "") : "";
                    if (!dStr.isEmpty()) delay = Integer.parseInt(dStr);
                } catch (Exception ignored) {}
                String constName = getSkillConstantName(id);
                manualParts.add("{" + constName + ", " + level + ", " + delay + "}");
            }
        }

        // Tạo code cho phần skill lẻ: new int[][]{...}
        String manualArrayCode = "";
        if (!manualParts.isEmpty()) {
            manualArrayCode = "new int[][]{" + String.join(", ", manualParts) + "}";
        }

        // Trường hợp 1: Không có Set nào, chỉ có skill lẻ (hoặc trống)
        if (arrayParts.isEmpty()) {
            return manualParts.isEmpty() ? "new int[][]{}" : manualArrayCode;
        }

        // Trường hợp 2: Có Full Set -> Dùng Util.addArray(...)
        StringBuilder sb = new StringBuilder("(int[][]) Util.addArray(");
        for (String set : arrayParts) {
            sb.append(set).append(", ");
        }
        
        if (!manualArrayCode.isEmpty()) {
            // Có Set và có cả Skill lẻ
            sb.append(manualArrayCode);
        } else {
            // Chỉ có Set, xóa dấu phẩy thừa ở cuối
            sb.setLength(sb.length() - 2); 
        }
        sb.append(")");
        return sb.toString();
    }
    
    // --- HELPER: Tạo mảng int[][] thực tế để update RAM ---
    private int[][] getRawSkillsFromTable(DefaultTableModel model) {
        List<int[]> allSkills = new ArrayList<>();

        for (int i = 0; i < model.getRowCount(); i++) {
            String name = model.getValueAt(i, 0).toString();
            if (name.startsWith("Set: ")) {
                // Bung nén Set thành các skill lẻ
                String setName = name.replace("Set: ", "").trim();
                if (FULL_SETS.containsKey(setName)) {
                    int[][] set = FULL_SETS.get(setName);
                    for (int[] sk : set) {
                        int[] fullSk = new int[3];
                        fullSk[0] = sk[0]; // ID
                        fullSk[1] = sk[1]; // Level
                        fullSk[2] = (sk.length > 2) ? sk[2] : 1000; // Default Delay
                        allSkills.add(fullSk);
                    }
                }
            } else {
                int id = getSkillIdFromName(name);
                int level = 7;
                int delay = 1000;
                try {
                    String lStr = model.getValueAt(i, 1) != null ? model.getValueAt(i, 1).toString().replaceAll("[^0-9]", "") : "";
                    if (!lStr.isEmpty()) level = Integer.parseInt(lStr);
                } catch (Exception ignored) {}
                try {
                    String dStr = model.getValueAt(i, 2) != null ? model.getValueAt(i, 2).toString().replaceAll("[^0-9]", "") : "";
                    if (!dStr.isEmpty()) delay = Integer.parseInt(dStr);
                } catch (Exception ignored) {}
                allSkills.add(new int[]{id, level, delay});
            }
        }
        
        int[][] result = new int[allSkills.size()][3];
        for(int k=0; k<allSkills.size(); k++) result[k] = allSkills.get(k);
        return result;
    }

    // --- HELPER: Parse HP List / Array hỗ trợ cả format số thường và số có phân cách hàng nghìn ---
    private List<Long> parseHpList(String text) {
        List<Long> result = new ArrayList<>();
        if (text == null || text.trim().isEmpty()) {
            result.add(1000L);
            return result;
        }
        String s = text.trim();
        // Check trường hợp user nhập 1 số nguyên nhưng dùng dấu phẩy làm phân cách hàng nghìn (VD: 1,000,000)
        if (s.matches("^\\d{1,3}(,\\d{3})+$")) {
            try {
                result.add(parseLong(s));
                return result;
            } catch (Exception ignored) {}
        }
        // Trường hợp phân tách nhiều mốc máu bằng dấu phẩy hoặc chấm phẩy
        String[] parts = s.split("[,;]");
        for (String p : parts) {
            String clean = p.trim();
            if (!clean.isEmpty()) {
                try {
                    result.add(parseLong(clean));
                } catch (Exception ignored) {}
            }
        }
        if (result.isEmpty()) result.add(1000L);
        return result;
    }

    private long[] parseHpArray(String text) {
        List<Long> list = parseHpList(text);
        long[] res = new long[list.size()];
        for (int i = 0; i < list.size(); i++) {
            res[i] = list.get(i);
        }
        return res;
    }

    // --- CẬP NHẬT OBJECT TRONG RAM (LOMBOK SETTERS + FALLBACK REFLECTION) ---
    private void updateObjectInMemory(BossData data, String name, byte gender, short[] outfit, long dame, long[] hp, int[] map, int[][] skill, String[] chat, int respawn) {
        if (data == null) return;
        try {
            data.setName(name);
            data.setGender(gender);
            data.setOutfit(outfit);
            data.setDame(dame);
            data.setHp(hp);
            data.setMapJoin(map);
            data.setSkillTemp(skill);
            data.setTextM(chat);
            data.setSecondsRest(respawn);
        } catch (Exception e) {
            setField(data, "name", name);
            setField(data, "gender", gender);
            setField(data, "outfit", outfit);
            setField(data, "dame", dame);
            setField(data, "hp", hp);
            setField(data, "mapJoin", map);
            setField(data, "skillTemp", skill);
            setField(data, "textM", chat);
            setField(data, "secondsRest", respawn);
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        try {
            Field f = obj.getClass().getDeclaredField(fieldName);
            f.setAccessible(true);
            f.set(obj, value);
        } catch (Exception ignored) {}
    }

    // --- HELPER: GHI ĐÈ BLOCK KHAI BÁO BOSSDATA VÀO FILE NGUỒN BossesData.java ---
    private boolean updateBossInSourceFile(File sourceFile, String key, String newCode) {
        try {
            String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
            Pattern pattern = Pattern.compile("(?m)^[ \\t]*public\\s+static\\s+final\\s+BossData\\s+" + Pattern.quote(key) + "\\s*=\\s*new\\s+BossData\\s*\\(");
            Matcher m = pattern.matcher(content);

            if (m.find()) {
                int startIndex = m.start();
                int openParens = 0;
                int endIndex = -1;
                for (int i = m.end() - 1; i < content.length(); i++) {
                    char c = content.charAt(i);
                    if (c == '(') {
                        openParens++;
                    } else if (c == ')') {
                        openParens--;
                        if (openParens == 0) {
                            for (int j = i + 1; j < content.length(); j++) {
                                char c2 = content.charAt(j);
                                if (c2 == ';') {
                                    endIndex = j;
                                    break;
                                } else if (!Character.isWhitespace(c2)) {
                                    break;
                                }
                            }
                            break;
                        }
                    }
                }
                if (endIndex != -1) {
                    content = content.substring(0, startIndex) + "    " + newCode + ";" + content.substring(endIndex + 1);
                    Files.writeString(sourceFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
                    return true;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    // --- LƯU BOSS HIỆN TẠI (VỪA SỬA TRONG CODE VỪA ĐỒNG BỘ REALTIME NGAY LẬP TỨC) ---
    private void saveCurrentBossToFileAndRealtime() {
        if (currentBossKey == null || currentBossKey.isEmpty()) {
            lblStatus.setText("Chưa chọn Boss nào để lưu!");
            lblStatus.setForeground(WARNING);
            return;
        }

        // 1. Cập nhật dữ liệu vào RAM và chuẩn bị chuỗi code
        saveCurrentConfigToMemory(currentBossKey);

        String newCode = pendingChanges.get(currentBossKey);
        if (newCode == null) {
            lblStatus.setText("Không có dữ liệu code cho Boss này!");
            lblStatus.setForeground(DANGER);
            return;
        }

        // 2. Ghi trực tiếp vào file mã nguồn src/models/boss/BossesData.java
        File sourceFile = JavaSourceSync.findSourceFile(SOURCE_FILE_PATH);
        if (sourceFile == null || !sourceFile.exists()) {
            sourceFile = new File(SOURCE_FILE_PATH);
        }
        if (!sourceFile.exists()) {
            lblStatus.setText("Lỗi: Không tìm thấy file BossesData.java!");
            lblStatus.setForeground(DANGER);
            return;
        }

        boolean fileUpdated = updateBossInSourceFile(sourceFile, currentBossKey, newCode);
        if (fileUpdated) {
            pendingChanges.remove(currentBossKey);
        }

        // 3. Đồng bộ Realtime sang tất cả các cá thể Boss đang chạy trên server
        int syncedCount = syncRealtimeBoss(currentSelectedBossData, currentBossKey);

        listBosses.repaint();

        if (fileUpdated) {
            lblStatus.setText("Đã lưu Code & Realtime Boss: " + currentBossKey + " (" + syncedCount + " cá thể live)!");
            lblStatus.setForeground(SUCCESS);
        } else {
            lblStatus.setText("Không tìm thấy code của " + currentBossKey + " trong file, nhưng đã Realtime (" + syncedCount + " live)!");
            lblStatus.setForeground(WARNING);
        }

        javax.swing.Timer t = new javax.swing.Timer(4000, evt -> lblStatus.setText(""));
        t.setRepeats(false);
        t.start();
    }

    // --- NÚT ĐỒNG BỘ REALTIME (KHÔNG GHI FILE) ---
    private void syncCurrentBossRealtime() {
        if (currentBossKey == null || currentBossKey.isEmpty()) {
            lblStatus.setText("Chưa chọn Boss nào để đồng bộ!");
            lblStatus.setForeground(WARNING);
            return;
        }
        saveCurrentConfigToMemory(currentBossKey);
        int liveCount = syncRealtimeBoss(currentSelectedBossData, currentBossKey);

        lblStatus.setText("Đã đồng bộ Realtime Boss: " + currentBossKey + " (" + liveCount + " cá thể live)!");
        lblStatus.setForeground(INFO);

        javax.swing.Timer t = new javax.swing.Timer(4000, evt -> lblStatus.setText(""));
        t.setRepeats(false);
        t.start();
    }

    // --- LƯU TẤT CẢ BOSS ĐÃ SỬA VÀO CODE VÀ ĐỒNG BỘ REALTIME ---
    private void commitAllChangesToFile() {
        if (currentBossKey != null) {
            saveCurrentConfigToMemory(currentBossKey);
        }

        if (pendingChanges.isEmpty()) {
            lblStatus.setText("Không có thay đổi nào để lưu!");
            lblStatus.setForeground(WARNING);
            return;
        }

        File sourceFile = JavaSourceSync.findSourceFile(SOURCE_FILE_PATH);
        if (sourceFile == null || !sourceFile.exists()) {
            sourceFile = new File(SOURCE_FILE_PATH);
        }
        if (!sourceFile.exists()) {
            lblStatus.setText("Lỗi: Không tìm thấy file BossesData.java!");
            lblStatus.setForeground(DANGER);
            return;
        }

        try {
            String content = Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
            int countUpdated = 0;
            int totalLiveSynced = 0;

            for (Map.Entry<String, String> entry : new HashMap<>(pendingChanges).entrySet()) {
                String key = entry.getKey();
                String newCode = entry.getValue();

                Pattern pattern = Pattern.compile("(?m)^[ \\t]*public\\s+static\\s+final\\s+BossData\\s+" + Pattern.quote(key) + "\\s*=\\s*new\\s+BossData\\s*\\(");
                Matcher m = pattern.matcher(content);

                if (m.find()) {
                    int startIndex = m.start();
                    int openParens = 0;
                    int endIndex = -1;
                    for (int i = m.end() - 1; i < content.length(); i++) {
                        char c = content.charAt(i);
                        if (c == '(') {
                            openParens++;
                        } else if (c == ')') {
                            openParens--;
                            if (openParens == 0) {
                                for (int j = i + 1; j < content.length(); j++) {
                                    char c2 = content.charAt(j);
                                    if (c2 == ';') {
                                        endIndex = j;
                                        break;
                                    } else if (!Character.isWhitespace(c2)) {
                                        break;
                                    }
                                }
                                break;
                            }
                        }
                    }
                    if (endIndex != -1) {
                        content = content.substring(0, startIndex) + "    " + newCode + ";" + content.substring(endIndex + 1);
                        countUpdated++;

                        // Realtime sync cho Boss này
                        try {
                            Field field = BossesData.class.getField(key);
                            BossData bd = (BossData) field.get(null);
                            if (bd != null) {
                                totalLiveSynced += syncRealtimeBoss(bd, key);
                            }
                        } catch (Exception ignored) {}
                    }
                }
            }

            Files.writeString(sourceFile.toPath(), content, StandardCharsets.UTF_8, StandardOpenOption.TRUNCATE_EXISTING);
            pendingChanges.clear();

            listBosses.repaint();

            lblStatus.setText("Đã lưu Code & Realtime " + countUpdated + " Boss (" + totalLiveSynced + " cá thể live)!");
            lblStatus.setForeground(SUCCESS);

            javax.swing.Timer t = new javax.swing.Timer(4000, evt -> lblStatus.setText(""));
            t.setRepeats(false);
            t.start();

        } catch (Exception e) {
            e.printStackTrace();
            lblStatus.setText("Lỗi Exception: " + e.getMessage());
            lblStatus.setForeground(DANGER);
        }
    }

    // --- LẤY TẤT CẢ 19 BOSS MANAGERS ---
    public static List<BossManager> getAllBossManagers() {
        List<BossManager> mgrs = new ArrayList<>();
        try { if (BossManager.gI() != null) mgrs.add(BossManager.gI()); } catch (Throwable ignored) {}
        try { if (BrolyManager.gI() != null) mgrs.add(BrolyManager.gI()); } catch (Throwable ignored) {}
        try { if (FinalBossManager.gI() != null) mgrs.add(FinalBossManager.gI()); } catch (Throwable ignored) {}
        try { if (YardartManager.gI() != null) mgrs.add(YardartManager.gI()); } catch (Throwable ignored) {}
        try { if (OtherBossManager.gI() != null) mgrs.add(OtherBossManager.gI()); } catch (Throwable ignored) {}
        try { if (SkillSummonedManager.gI() != null) mgrs.add(SkillSummonedManager.gI()); } catch (Throwable ignored) {}
        try { if (Boss12hManager.gI() != null) mgrs.add(Boss12hManager.gI()); } catch (Throwable ignored) {}
        try { if (Boss14hManager.gI() != null) mgrs.add(Boss14hManager.gI()); } catch (Throwable ignored) {}
        try { if (Bktne.gI() != null) mgrs.add(Bktne.gI()); } catch (Throwable ignored) {}
        try { if (ChristmasEventManager.gI() != null) mgrs.add(ChristmasEventManager.gI()); } catch (Throwable ignored) {}
        try { if (GasDestroyManager.gI() != null) mgrs.add(GasDestroyManager.gI()); } catch (Throwable ignored) {}
        try { if (HitEventManager.gI() != null) mgrs.add(HitEventManager.gI()); } catch (Throwable ignored) {}
        try { if (HungVuongEventManager.gI() != null) mgrs.add(HungVuongEventManager.gI()); } catch (Throwable ignored) {}
        try { if (LunarNewYearEventManager.gI() != null) mgrs.add(LunarNewYearEventManager.gI()); } catch (Throwable ignored) {}
        try { if (RedRibbonHQManager.gI() != null) mgrs.add(RedRibbonHQManager.gI()); } catch (Throwable ignored) {}
        try { if (SnakeWayManager.gI() != null) mgrs.add(SnakeWayManager.gI()); } catch (Throwable ignored) {}
        try { if (TeaEventManager.gI() != null) mgrs.add(TeaEventManager.gI()); } catch (Throwable ignored) {}
        try { if (TreasureUnderSeaManager.gI() != null) mgrs.add(TreasureUnderSeaManager.gI()); } catch (Throwable ignored) {}
        try { if (TrungThuEventManager.gI() != null) mgrs.add(TrungThuEventManager.gI()); } catch (Throwable ignored) {}
        return mgrs;
    }

    // --- ĐỒNG BỘ REALTIME BOSS SANG TẤT CẢ THỰC THỂ ĐANG CHẠY TRÊN SERVER ---
    public static int syncRealtimeBoss(BossData targetData, String bossKey) {
        if (targetData == null) return 0;
        int count = 0;
        Set<Boss> processed = Collections.newSetFromMap(new IdentityHashMap<>());
        String targetName = targetData.getName();

        // 1. Quét qua toàn bộ 19 BossManagers
        List<BossManager> mgrs = getAllBossManagers();
        for (BossManager mgr : mgrs) {
            if (mgr == null) continue;
            List<Boss> bossList = null;
            try {
                bossList = mgr.getBosses();
            } catch (Throwable ignored) {}
            if (bossList == null) continue;

            List<Boss> copy;
            synchronized (bossList) {
                copy = new ArrayList<>(bossList);
            }
            for (Boss b : copy) {
                if (b == null || processed.contains(b)) continue;
                if (isBossMatchingData(b, targetData, targetName)) {
                    processed.add(b);
                    applyBossRealtimeStats(b, targetData);
                    count++;
                }
            }
        }

        // 2. Quét qua các Zone trong tất cả Map để không bỏ sót bất kỳ cá thể nào
        try {
            for (models.map.Map m : server.Manager.MAPS) {
                if (m == null || m.zones == null) continue;
                for (Zone z : m.zones) {
                    if (z == null || z.getBosses() == null) continue;
                    List<models.player.Player> zBosses = new ArrayList<>(z.getBosses());
                    for (models.player.Player pl : zBosses) {
                        if (pl instanceof Boss b) {
                            if (processed.contains(b)) continue;
                            if (isBossMatchingData(b, targetData, targetName)) {
                                processed.add(b);
                                applyBossRealtimeStats(b, targetData);
                                count++;
                            }
                        }
                    }
                }
            }
        } catch (Throwable ignored) {}

        return count;
    }

    private static boolean isBossMatchingData(Boss b, BossData targetData, String targetName) {
        if (b == null) return false;
        if (b.data != null) {
            for (BossData bd : b.data) {
                if (bd == targetData) return true;
                if (bd != null && targetName != null && targetName.equalsIgnoreCase(bd.getName())) return true;
            }
        }
        if (targetName != null && b.name != null) {
            if (b.name.equalsIgnoreCase(targetName) || b.name.startsWith(targetName)) {
                return true;
            }
        }
        return false;
    }

    private static void applyBossRealtimeStats(Boss b, BossData data) {
        try {
            // Tên & Giới tính
            if (data.getName() != null) {
                try {
                    if (data.getName().contains("%d")) {
                        b.name = String.format(data.getName(), utils.Util.nextInt(0, 100));
                    } else {
                        b.name = data.getName();
                    }
                } catch (Exception ex) {
                    b.name = data.getName();
                }
            }
            b.gender = data.getGender();

            // Thời gian hồi sinh
            b.secondsRest = data.getSecondsRest();

            // Máu & Sức đánh
            if (b.nPoint != null) {
                b.nPoint.dameg = data.getDame();
                if (data.getHp() != null && data.getHp().length > 0) {
                    long newHpMax = data.getHp()[0];
                    b.nPoint.hpg = newHpMax;
                    b.nPoint.hpMax = newHpMax;
                }
                b.nPoint.calPoint();
                if (b.nPoint.hp > b.nPoint.hpMax || b.nPoint.hp <= 0) {
                    b.nPoint.hp = b.nPoint.hpMax;
                }
            }

            // Kỹ năng
            b.initSkill();

            // Gửi packet cập nhật ngay lập tức cho các player trong khu vực nếu boss đang ở map
            if (b.zone != null) {
                try {
                    Service.gI().point(b);
                } catch (Throwable ignored) {}
                try {
                    Service.gI().reload_HP_NV(b);
                } catch (Throwable ignored) {}
                try {
                    Service.gI().Send_Caitrang(b);
                } catch (Throwable ignored) {}
                try {
                    Service.gI().sendFlagBag(b);
                } catch (Throwable ignored) {}
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JTextField createStyledTextField(Color textColor) {
        JTextField txt = new JTextField();
        txt.setFont(FONT_BODY);
        txt.setForeground(textColor);
        return txt;
    }

    private JTextField createStyledOutfitField(String title) {
        JTextField txt = new JTextField("-1");
        txt.setFont(FONT_BODY);
        txt.setHorizontalAlignment(JTextField.CENTER);
        txt.putClientProperty("outfit.title", title);
        return txt;
    }

    /** Nhãn nhỏ phía trên + ô nhập outfit (thay cho TitledBorder cũ). */
    private JPanel outfitFieldBox(JTextField field) {
        Object title = field.getClientProperty("outfit.title");
        JPanel box = new JPanel(new BorderLayout(0, SP_XS));
        box.setOpaque(false);
        JLabel lbl = label(title == null ? "" : title.toString(), FONT_TINY_B, TEXT_MUTED);
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        box.add(lbl, BorderLayout.NORTH);
        box.add(field, BorderLayout.CENTER);
        return box;
    }

    private void addFormRow(JPanel panel, GridBagConstraints g, int y, String labelText, JComponent comp) {
        g.gridx = 0; g.gridy = y; g.weightx = 0;
        JLabel lbl = label(labelText, FONT_SMALL_B, TEXT_SECONDARY);
        panel.add(lbl, g);
        g.gridx = 1; g.weightx = 1;
        panel.add(comp, g);
    }

    private String formatLong(long val) {
        return String.format("%,d", val).replace(",", "_");
    }

    private long parseLong(String val) throws NumberFormatException {
        if (val == null || val.trim().isEmpty()) return 0;
        // Xử lý format số Việt Nam: loại bỏ dấu chấm (.) và dấu phẩy (,) làm separator
        // Ví dụ: "9.000" -> "9000", "1.234.567" -> "1234567"
        String clean = val.replaceAll("[_,lL\\s]", "").trim();
        // Loại bỏ dấu chấm và dấu phẩy (có thể là separator hàng nghìn)
        clean = clean.replace(".", "").replace(",", "");
        try {
            return Long.parseLong(clean);
        } catch (NumberFormatException e) {
            System.err.println("Lỗi parse số: " + val + " -> " + clean);
            throw e;
        }
    }

    private int getSkillIdFromName(String name) {
        try { return Integer.parseInt(name.split("\\.")[0]); } catch (Exception e) { return 0; }
    }

    private String getSkillName(int id) {
        return switch (id) {
            case 0 -> "0. Dragon"; case 1 -> "1. Kamejoko"; case 2 -> "2. Demon";
            case 3 -> "3. Masenko"; case 4 -> "4. Galick"; case 5 -> "5. Antomic";
            case 6 -> "6. Thái Dương HS"; case 7 -> "7. Trị Thương"; case 8 -> "8. Tái Tạo NL";
            case 9 -> "9. Kaioken"; case 10 -> "10. QC Kênh Khi"; case 11 -> "11. Makankosappo";
            case 12 -> "12. Đẻ Trứng"; case 13 -> "13. Biến Khỉ"; case 14 -> "14. Tự Sát";
            case 17 -> "17. Liên Hoàn"; case 18 -> "18. Biến Socola"; case 19 -> "19. Khiên NL";
            case 20 -> "20. Dịch Chuyển"; case 21 -> "21. Huýt Sáo"; case 22 -> "22. Thôi Miên";
            case 23 -> "23. Trói"; 
            case 24 -> "24. Super Kame"; case 25 -> "25. Liên Hoàn Chưởng"; case 26 -> "26. Ma Phong Ba";
            case 27 -> "27. Biến Hình"; case 28 -> "28. Phân Thân";
            default -> id + ". Skill Khác";
        };
    }

    private String getSkillConstantName(int id) {
        return switch (id) {
            case 0 -> "Skill.DRAGON"; case 1 -> "Skill.KAMEJOKO"; case 2 -> "Skill.DEMON";
            case 3 -> "Skill.MASENKO"; case 4 -> "Skill.GALICK"; case 5 -> "Skill.ANTOMIC";
            case 6 -> "Skill.THAI_DUONG_HA_SAN"; case 7 -> "Skill.TRI_THUONG"; case 8 -> "Skill.TAI_TAO_NANG_LUONG";
            case 9 -> "Skill.KAIOKEN"; case 10 -> "Skill.QUA_CAU_KENH_KHI"; case 11 -> "Skill.MAKANKOSAPPO";
            case 12 -> "Skill.DE_TRUNG"; case 13 -> "Skill.BIEN_KHI"; case 14 -> "Skill.TU_SAT";
            case 17 -> "Skill.LIEN_HOAN"; case 18 -> "Skill.SOCOLA"; case 19 -> "Skill.KHIEN_NANG_LUONG";
            case 20 -> "Skill.DICH_CHUYEN_TUC_THOI"; case 21 -> "Skill.HUYT_SAO"; case 22 -> "Skill.THOI_MIEN";
            case 23 -> "Skill.TROI"; 
            case 24 -> "Skill.SUPER_KAME"; case 25 -> "Skill.LIEN_HOAN_CHUONG"; case 26 -> "Skill.MA_PHONG_BA";
            case 27 -> "Skill.BIEN_HINH"; case 28 -> "Skill.PHAN_THAN";
            default -> String.valueOf(id);
        };
    }
}