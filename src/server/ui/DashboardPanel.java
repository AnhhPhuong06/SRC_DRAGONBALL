package server.ui;

import consts.BossID;
import managers.boss.BossManager;
import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossData;
import models.boss.BossesData;
import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import models.item.Item.ItemOption;
import managers.GiftCodeManager;
import managers.ConsignShopManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Connection;
import java.sql.Statement;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.JSONArray;
import jdbc.DBConnecter;
import network.session.SessionManager;
import server.AutoSaveManager;
import interfaces.ISession;
import server.Client;
import network.session.MySession;
import server.Maintenance;
import server.Manager;
import models.player.Player;
import services.Service;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import static server.ui.ServerGuiUtils.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.Dimension;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import database.daos.ShopDAO;
import models.GiftCode;
import models.Template.NpcTemplate;
import models.shop.Shop;

public class DashboardPanel extends JPanel {

    // --- UI Components ---
    private JLabel lblStatus, lblPlayerCount, lblCountdown;
    private JLabel lblCpuUsage, lblRamUsage, lblThreadCount, lblSessionCount;
    private JLabel lblIpStats; // Thống kê IP
    private JLabel lblDelayStat; // Độ trễ xử lý (scheduler jitter)
    private JLabel lblGiftcodeInfo, lblConsignItemsCount, lblBossStatus, lblUptime;
    // Stat tiles for the redesigned dashboard
    private ServerGuiUtils.StatTile tileCpu, tileRam, tileThreads, tileSessions, tileIps, tileDelay;

    // Log Component
    private JTextArea txtLog;

    // Config Components (Maintenance)
    private JComboBox<Integer> cbHour, cbMinute, cbSecond;
    private JCheckBox chkAutoRestart;

    // Config Components (Optimization)
    private JCheckBox chkAutoOptimize;
    private JComboBox<String> cbOptimizeInterval;
    private JLabel lblOptStatus;

    // Action Buttons
    private JToggleButton btnToggleAutoSave;
    private JToggleButton btnToggleAutoClean;

    // Graphs
    private final MiniGraph graphCpu = new MiniGraph(PRIMARY);
    private final MiniGraph graphRam = new MiniGraph(VIOLET);
    private final MiniGraph graphThread = new MiniGraph(SUCCESS);
    private final MiniGraph graphSession = new MiniGraph(WARNING);

    // --- Logic Variables ---
    private final Instant serverStartTime = Instant.now();
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);
    private final AtomicBoolean isAutoSaveEnabled = new AtomicBoolean(true);

    // Mốc thời gian tick trước của vòng giám sát, để tính độ trễ xử lý (ms)
    private long lastStatsTickMs = 0;

    // Maintenance Logic
    public static boolean REQUEST_AUTO_RESTART = false;
    private ScheduledFuture<?> activeMaintenanceJobFuture = null;
    private ScheduledFuture<?> activeCountdownDisplayFuture = null;

    // Optimize Logic
    private ScheduledFuture<?> activeAutoOptimizeFuture = null;

    // --- Boss Icon Logic ---
    private static final String ICON_FOLDER = "data/icon/";
    private static final String BOSS_MANAGER_PATH = "src/managers/boss/BossManager.java"; // Đường dẫn file code
    private final Map<Integer, Integer> partIconMap = new HashMap<>();
    private final Map<Integer, ImageIcon> iconImageCache = new HashMap<>();
    private List<BossSummonEntry> cachedBossEntries = new ArrayList<>();

    public DashboardPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);

        loadPartDataFromDB();
        initMainLayout();
        if (lblStatus != null)    lblStatus.setText("ONLINE");
        if (lblCountdown != null) lblCountdown.setText("Chưa đặt lịch");
        if (lblPlayerCount != null) lblPlayerCount.setText("0");
        startMonitoring();
        loadMaintenanceConfig();

        chkAutoOptimize.setSelected(false);
        addLog("Dashboard initialized. Monitoring Server specific resources.");
    }

    // --- ICON & BOSS DATA LOADING ---
    private void loadPartDataFromDB() {
        new Thread(() -> {
            try (Connection conn = DBConnecter.getConnectionData(); Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery("SELECT id, DATA FROM part WHERE TYPE = 0")) {

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
                    } catch (Exception e) {
                    }
                }
            } catch (Exception e) {
                addLog("Error loading Part DB for Icons: " + e.getMessage());
            }
        }).start();
    }

    private ImageIcon getIconByIconId(int iconId, int size) {
        if (iconId <= -1) {
            return null;
        }
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
                    iconImageCache.put(iconId, icon);
                    return icon;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // Helper Class cho Boss List
    private static class BossSummonEntry {

        String keyName; // Tên biến trong BossID (VD: TIEU_DOI_TRUONG)
        int id;         // ID thực tế
        String displayName;
        int headIconId;

        public BossSummonEntry(String keyName, int id, String displayName, int headIconId) {
            this.keyName = keyName;
            this.id = id;
            this.displayName = displayName;
            this.headIconId = headIconId;
        }

        @Override
        public String toString() {
            return displayName;
        }
    }

    // --- [NEW LOGIC] CHỈ LOAD BOSS CÓ TRONG HÀM loadBoss() ---
    private void prepareBossData() {
        cachedBossEntries.clear();
        try {
            File file = services.JavaSourceSync.findSourceFile(BOSS_MANAGER_PATH);
            if (file == null || !file.exists()) {
                file = new File(BOSS_MANAGER_PATH);
            }
            if (!file.exists()) {
                addLog("Warning: Không tìm thấy file source " + BOSS_MANAGER_PATH + ". Load tất cả boss.");
                prepareBossDataFallback(); // Fallback nếu không có source code
                return;
            }

            String content = Files.readString(file.toPath());

            int startIndex = content.indexOf("public void loadBoss()");
            if (startIndex == -1) {
                addLog("Warning: Không tìm thấy hàm loadBoss().");
                prepareBossDataFallback();
                return;
            }

            int braceCount = 0;
            int endIndex = -1;
            for (int i = content.indexOf('{', startIndex); i < content.length(); i++) {
                if (content.charAt(i) == '{') {
                    braceCount++;
                } else if (content.charAt(i) == '}') {
                    braceCount--;
                    if (braceCount == 0) {
                        endIndex = i;
                        break;
                    }
                }
            }

            String methodBody = (endIndex != -1) ? content.substring(startIndex, endIndex) : content;

            Pattern pattern = Pattern.compile("createBoss\\s*\\(\\s*BossID\\.([A-Z0-9_]+)");
            Matcher matcher = pattern.matcher(methodBody);

            Set<String> foundBossKeys = new HashSet<>();
            while (matcher.find()) {
                foundBossKeys.add(matcher.group(1));
            }

            addLog("Found " + foundBossKeys.size() + " bosses in loadBoss() method.");

            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }

            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (foundBossKeys.contains(f.getName()) && f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());

                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }

                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }

        } catch (Exception e) {
            addLog("Error analyzing BossManager code: " + e.getMessage());
            prepareBossDataFallback();
        }
    }

    private void prepareBossDataFallback() {
        try {
            Field[] idFields = BossID.class.getFields();
            Map<String, Integer> idMap = new HashMap<>();
            for (Field f : idFields) {
                if (Modifier.isStatic(f.getModifiers()) && f.getType() == int.class) {
                    idMap.put(f.getName(), f.getInt(null));
                }
            }
            Field[] dataFields = BossesData.class.getFields();
            for (Field f : dataFields) {
                if (f.getType() == BossData.class && idMap.containsKey(f.getName())) {
                    BossData data = (BossData) f.get(null);
                    int bossId = idMap.get(f.getName());
                    int iconId = -1;
                    if (data.getOutfit() != null && data.getOutfit().length > 0) {
                        int headPart = data.getOutfit()[0];
                        iconId = partIconMap.getOrDefault(headPart, headPart);
                    }
                    cachedBossEntries.add(new BossSummonEntry(f.getName(), bossId, data.getName(), iconId));
                }
            }
        } catch (Exception e) {
        }
    }

    // --- MAIN UI ---
    private void loadMaintenanceConfig() {

        File f = new File(System.getProperty("user.dir"),
                "maintenanceConfig.txt");

        if (!f.exists()) {
            addLog("[Auto Maintenance] No config found.");
            return;
        }

        try (BufferedReader br
                = new BufferedReader(new FileReader(f))) {

            int h = Integer.parseInt(br.readLine());
            int m = Integer.parseInt(br.readLine());
            int s = Integer.parseInt(br.readLine());
            boolean auto
                    = Boolean.parseBoolean(br.readLine());

            cbHour.setSelectedItem(h);
            cbMinute.setSelectedItem(m);
            cbSecond.setSelectedItem(s);
            chkAutoRestart.setSelected(auto);

            REQUEST_AUTO_RESTART = auto;

            addLog("[Auto Maintenance] Loaded config: "
                    + String.format("%02d:%02d:%02d", h, m, s));

            // ✅ schedule sau khi UI ready
            SwingUtilities.invokeLater(()
                    -> scheduleMaintenance());

        } catch (Exception e) {
            addLog("Error loading config: "
                    + e.getMessage());
        }
    }

    private void initMainLayout() {
        JPanel column = new JPanel();
        column.setOpaque(false);
        column.setLayout(new BoxLayout(column, BoxLayout.Y_AXIS));

        // 1. Header (status snapshot)
        column.add(createHeaderPanel());
        column.add(Box.createVerticalStrut(SP_LG));

        // 2. System stat tiles (CPU/RAM/Threads/Sessions/IPs)
        column.add(createSystemStatsPanel());
        column.add(Box.createVerticalStrut(SP_LG));

        // 3. Quick action bar
        column.add(createActionPanel());
        column.add(Box.createVerticalStrut(SP_LG));

        // 4. Game statistics
        column.add(createGameStatsPanel());
        column.add(Box.createVerticalStrut(SP_LG));

        // 5. Boss manager + server config side-by-side
        JPanel midPanel = new JPanel(new GridLayout(1, 2, SP_LG, 0));
        midPanel.setOpaque(false);
        midPanel.add(createBossManagementPanel());
        midPanel.add(createConfigurationPanel());
        column.add(midPanel);
        column.add(Box.createVerticalStrut(SP_LG));

        // 6. Optimization controls
        column.add(createOptimizationPanel());
        column.add(Box.createVerticalStrut(SP_LG));

        // 7. Log panel
        column.add(createLogPanel());

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));
        wrap.add(column, BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(wrap);
        scroll.setBorder(null);
        scroll.setBackground(BG);
        scroll.getViewport().setBackground(BG);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);
    }

    // ================= UI PARTS =================
    private JPanel createHeaderPanel() {
        // Status card row: server status, online count, countdown — wraps when narrow
        JPanel row = new JPanel(new ServerGuiUtils.WrapLayout(FlowLayout.LEFT, SP_LG, SP_SM));
        row.setOpaque(false);

        StatTile status   = statTile("Server Status",  "● ONLINE",  SUCCESS, "power");
        StatTile players  = statTile("Players Online", "0",          PRIMARY, "users");
        StatTile countdown= statTile("Next Maintenance", "Sẵn sàng",  WARNING, "settings");

        // Keep legacy labels in sync with these tiles
        lblStatus       = mirrorLabel(status);
        lblPlayerCount  = mirrorLabel(players);
        lblCountdown    = mirrorLabel(countdown);

        row.add(status);
        row.add(players);
        row.add(countdown);
        return row;
    }

    /**
     * Returns a JLabel that, when setText is called, forwards the value to the
     * given StatTile's value area. Keeps the existing monitoring code working
     * unchanged while still rendering through the new design system.
     */
    private JLabel mirrorLabel(StatTile tile) {
        return new JLabel() {
            @Override public void setText(String text) {
                super.setText(text);
                if (tile != null) {
                    // Strip "Label: " prefix if any so the tile shows just the value
                    String v = text;
                    int colon = v.indexOf(": ");
                    if (colon >= 0 && colon < v.length() - 2) v = v.substring(colon + 2);
                    tile.setValue(v);
                }
            }
            @Override public void setForeground(Color fg) { super.setForeground(fg); /* tile keeps brand color */ }
        };
    }

    private JPanel createSystemStatsPanel() {
        // WrapLayout: tiles tự xuống dòng khi cửa sổ hẹp, không bị cắt
        JPanel row = new JPanel(new ServerGuiUtils.WrapLayout(FlowLayout.LEFT, SP_LG, SP_SM));
        row.setOpaque(false);

        tileCpu      = statTile("Server CPU",     "0%",    INFO,    "chart");
        tileRam      = statTile("JVM RAM (Heap)", "0 MB",  VIOLET,  "database");
        tileThreads  = statTile("Threads",        "0",     SUCCESS, "bolt");
        tileSessions = statTile("Sessions",       "0",     WARNING, "globe");

        // Sparkline realtime ngay trong tile — dữ liệu do vòng giám sát đẩy vào
        tileCpu.setSparkline(graphCpu);
        tileRam.setSparkline(graphRam);
        tileThreads.setSparkline(graphThread);
        tileSessions.setSparkline(graphSession);
        tileIps      = statTile("IPs / Max-per-IP", "0 / " + Manager.MAX_PER_IP, PRIMARY, "shield");
        tileDelay    = statTile("Server Delay",   "0 ms",  DANGER,  "clock"); // đỏ - độ trễ xử lý

        // Make the IPs tile clickable to open the per-IP dialog
        tileIps.setCursor(new Cursor(Cursor.HAND_CURSOR));
        tileIps.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseClicked(java.awt.event.MouseEvent evt) { showPlayersByIpDialog(); }
        });

        // Keep legacy field labels — monitoring code reads from these names
        lblCpuUsage    = mirrorLabel(tileCpu);
        lblRamUsage    = mirrorLabel(tileRam);
        lblThreadCount = mirrorLabel(tileThreads);
        lblSessionCount= mirrorLabel(tileSessions);
        lblIpStats     = mirrorLabel(tileIps);
        lblDelayStat   = mirrorLabel(tileDelay);

        row.add(tileCpu);
        row.add(tileRam);
        row.add(tileThreads);
        row.add(tileSessions);
        row.add(tileIps);
        row.add(tileDelay);
        return row;
    }

    /** Legacy helper kept in case other code creates ad-hoc graph cards. */
    private JPanel createGraphCard(String title, JLabel info, MiniGraph graph) {
        JPanel cardP = card();
        cardP.setLayout(new BorderLayout(0, SP_SM));
        JLabel titleLbl = label(title.toUpperCase(), FONT_TINY.deriveFont(Font.BOLD), TEXT_SECONDARY);
        info.setFont(FONT_H1);
        info.setForeground(TEXT);
        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        top.add(titleLbl, BorderLayout.NORTH);
        top.add(info, BorderLayout.CENTER);
        cardP.add(top, BorderLayout.NORTH);
        cardP.add(graph, BorderLayout.CENTER);
        return cardP;
    }

    private JPanel createActionPanel() {
        JPanel card = card(new BorderLayout(0, SP_SM));
        card.add(sectionHeader("Quick Actions", "Các thao tác nhanh thường dùng", null), BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        row.setOpaque(false);

        JButton btnMaint = button("Bảo Trì (Tùy Chỉnh)", ButtonVariant.SECONDARY, "settings");
        btnMaint.addActionListener(e -> confirmMaintenance());

        JButton btnMaintNow = button("Bảo Trì Ngay", ButtonVariant.DANGER, "warning");
        btnMaintNow.addActionListener(e -> confirmMaintenanceNow());

        JButton btnReload = button("Tải lại DB", ButtonVariant.PRIMARY, "refresh");
        btnReload.addActionListener(e -> showReloadOptions());

        JButton btnClean = button("Dọn Session", ButtonVariant.SECONDARY, "broom");
        btnClean.addActionListener(e -> cleanupDeadSessions());

        btnToggleAutoSave = toggle("AutoSave: ON");
        btnToggleAutoSave.setSelected(true);
        btnToggleAutoSave.addActionListener(e -> {
            boolean enable = isAutoSaveEnabled.get();
            if (enable) {
                AutoSaveManager.getInstance().stopAutoSave();
                btnToggleAutoSave.setText("AutoSave: OFF");
                addLog("System: AutoSave has been DISABLED.");
            } else {
                AutoSaveManager.getInstance().startAutoSave();
                btnToggleAutoSave.setText("AutoSave: ON");
                addLog("System: AutoSave has been ENABLED.");
            }
            isAutoSaveEnabled.set(!enable);
            btnToggleAutoSave.setSelected(!enable);
        });

        // Toggle auto-clear session/socket chết (Client.AUTO_CLEAR_DEAD_SESSION)
        boolean autoCleanOn = server.Client.AUTO_CLEAR_DEAD_SESSION;
        btnToggleAutoClean = toggle(autoCleanOn ? "AutoClean SS: ON" : "AutoClean SS: OFF");
        btnToggleAutoClean.setSelected(autoCleanOn);
        btnToggleAutoClean.addActionListener(e -> {
            boolean enable = !server.Client.AUTO_CLEAR_DEAD_SESSION;
            server.Client.AUTO_CLEAR_DEAD_SESSION = enable;
            if (enable) {
                btnToggleAutoClean.setText("AutoClean SS: ON");
                addLog("System: Auto-clear session/socket chết đã BẬT.");
            } else {
                btnToggleAutoClean.setText("AutoClean SS: OFF");
                addLog("System: Auto-clear session/socket chết đã TẮT.");
            }
            btnToggleAutoClean.setSelected(enable);
        });

        row.add(btnMaint);
        row.add(btnMaintNow);
        row.add(btnReload);
        row.add(btnClean);
        row.add(btnToggleAutoSave);
        row.add(btnToggleAutoClean);
        card.add(row, BorderLayout.CENTER);
        return card;
    }

    private JPanel createBossManagementPanel() {
        JPanel card = card(new BorderLayout(0, SP_MD));
        JLabel bossGlyph = new JLabel(vectorIcon("boss", 20, VIOLET));
        card.add(sectionHeader("Boss Manager", "Triệu hồi, reset, hồi sinh boss", bossGlyph), BorderLayout.NORTH);

        JButton btnOpenSummon = button("Mở Menu Triệu Hồi (Search & Call Boss)", ButtonVariant.PRIMARY, "boss");
        btnOpenSummon.setPreferredSize(new Dimension(0, 40));
        btnOpenSummon.addActionListener(e -> showBossSummonDialog());

        JPanel subBtn = new JPanel(new GridLayout(1, 2, SP_SM, 0));
        subBtn.setOpaque(false);

        JButton btnResetBoss = button("Reset All Boss", ButtonVariant.DANGER, "refresh");
        btnResetBoss.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Reset TẤT CẢ BOSS?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().resetAllBosses();
                addLog("Boss Manager: Đã reset tất cả boss.");
            }
        });

        JButton btnRespawn = button("Hồi Sinh Boss Chờ", ButtonVariant.SUCCESS, "play");
        btnRespawn.addActionListener(e -> {
            if (JOptionPane.showConfirmDialog(this, "Hồi sinh tất cả Boss đang chờ?", "Xác nhận", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
                BossManager.gI().respawnAllRestingBosses();
                addLog("Boss Manager: Đã hồi sinh các boss đang chờ.");
            }
        });

        subBtn.add(btnResetBoss);
        subBtn.add(btnRespawn);

        JPanel content = new JPanel(new BorderLayout(0, SP_SM));
        content.setOpaque(false);
        content.add(btnOpenSummon, BorderLayout.NORTH);
        content.add(subBtn, BorderLayout.CENTER);
        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel createConfigurationPanel() {
        JPanel p = card(new BorderLayout(0, SP_MD));
        JLabel cfgGlyph = new JLabel(vectorIcon("settings", 20, TEXT_MUTED));
        p.add(sectionHeader("Server Configuration", "Đăng nhập, EXP, giới hạn IP, lịch bảo trì", cfgGlyph), BorderLayout.NORTH);

        JPanel container = new JPanel(new GridLayout(0, 1, 0, SP_SM));
        container.setOpaque(false);

        // 1. Exp Config
        JPanel pExp = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pExp.setOpaque(false);
        JTextField txtExp = new JTextField(String.valueOf(Manager.RATE_EXP_SERVER), 4);
        JButton btnUpdateExp = button("Set EXP", ButtonVariant.SECONDARY, "bolt");
        btnUpdateExp.addActionListener(e -> {
            try {
                double rate = Double.parseDouble(txtExp.getText().trim());
                if (rate > 0) {
                    Manager.RATE_EXP_SERVER = (int) rate;
                    addLog("Config: EXP Rate updated to x" + Manager.RATE_EXP_SERVER);
                    Service.gI().sendThongBaoAllPlayer("Server EXP Rate: x" + Manager.RATE_EXP_SERVER);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số không hợp lệ");
            }
        });
        pExp.add(label("Rate: x", FONT_SMALL_B, TEXT_SECONDARY));
        pExp.add(txtExp);
        pExp.add(btnUpdateExp);

        // 2. IP Limit Config
        JPanel pIpLimit = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pIpLimit.setOpaque(false);
        
        JCheckBox chkEnableIpLimit = new JCheckBox("Bật Giới Hạn IP");
        chkEnableIpLimit.setSelected(Manager.ENABLE_IP_LIMIT);
        chkEnableIpLimit.setOpaque(false);
        chkEnableIpLimit.setFont(FONT_SMALL_B);

        JTextField txtMaxPerIp = new JTextField(String.valueOf(Manager.MAX_PER_IP), 4);
        JButton btnUpdateIpLimit = button("Set IP Limit", ButtonVariant.SECONDARY, "shield");
        btnUpdateIpLimit.addActionListener(e -> {
            try {
                int maxPerIp = Integer.parseInt(txtMaxPerIp.getText().trim());
                if (maxPerIp > 0) {
                    Manager.MAX_PER_IP = maxPerIp;
                    addLog("Config: MAX_PER_IP updated to " + Manager.MAX_PER_IP);
                    // Cập nhật hiển thị label nếu đã tạo
                    if (lblIpStats != null) {
                        lblIpStats.setText("IPs: 0 | Max/IP: " + Manager.MAX_PER_IP);
                    }
                    JOptionPane.showMessageDialog(this, "Đã cập nhật giới hạn IP: " + Manager.MAX_PER_IP);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Số không hợp lệ");
            }
        });
        
        chkEnableIpLimit.addActionListener(e -> {
            Manager.ENABLE_IP_LIMIT = chkEnableIpLimit.isSelected();
            addLog("Config: ENABLE_IP_LIMIT " + (Manager.ENABLE_IP_LIMIT ? "ENABLED" : "DISABLED"));
            txtMaxPerIp.setEnabled(Manager.ENABLE_IP_LIMIT);
            btnUpdateIpLimit.setEnabled(Manager.ENABLE_IP_LIMIT);
        });
        
        pIpLimit.add(chkEnableIpLimit);
        pIpLimit.add(label("Max/IP:", FONT_SMALL_B, TEXT_SECONDARY));
        pIpLimit.add(txtMaxPerIp);
        pIpLimit.add(btnUpdateIpLimit);

        // 3. Scheduler Config
        JPanel pSched = new JPanel(new FlowLayout(FlowLayout.LEFT));
        pSched.setOpaque(false);

        cbHour = new JComboBox<>();
        for (int i = -1; i < 24; i++) {
            cbHour.addItem(i);
        }
        cbMinute = new JComboBox<>();
        for (int i = -1; i < 60; i++) {
            cbMinute.addItem(i);
        }
        cbSecond = new JComboBox<>();
        for (int i = -1; i < 60; i++) {
            cbSecond.addItem(i);
        }

        chkAutoRestart = new JCheckBox("AutoRestart");
        chkAutoRestart.setSelected(true);
        chkAutoRestart.setOpaque(false);

        JButton btnSetSched = button("Lưu Cấu Hình", ButtonVariant.PRIMARY, "save");
        btnSetSched.addActionListener(e -> scheduleMaintenance());

        pSched.add(label("Hẹn giờ:", FONT_SMALL_B, TEXT_SECONDARY));
        pSched.add(cbHour);
        pSched.add(label(":", FONT_SMALL_B, TEXT_SECONDARY));
        pSched.add(cbMinute);
        pSched.add(chkAutoRestart);
        pSched.add(btnSetSched);

        container.add(new LoginAccessPanel(this::addLog));
        container.add(pExp);
        container.add(pIpLimit);
        container.add(pSched);

        p.add(container, BorderLayout.CENTER);
        return p;
    }

    private JPanel createOptimizationPanel() {
        JPanel p = card(new BorderLayout(0, SP_MD));
        p.add(sectionHeader("System Optimization", "Tối ưu RAM/CPU, dọn log", null), BorderLayout.NORTH);

        JPanel pBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pBtns.setOpaque(false);

        JButton btnOptRam = button("Dọn dẹp JVM RAM", ButtonVariant.SUCCESS);
        btnOptRam.addActionListener(e -> performRamCleanup());

        JButton btnOptCpu = button("Tối ưu CPU & VPS", ButtonVariant.PRIMARY);
        btnOptCpu.addActionListener(e -> performCpuOptimization());

        JButton btnFlushLog = button("Xóa Log Cache", ButtonVariant.GHOST);
        btnFlushLog.addActionListener(e -> {
            txtLog.setText("");
            addLog("System: Log cache cleared to free memory.");
        });

        JPanel pAuto = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        pAuto.setOpaque(false);
        pAuto.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));

        chkAutoOptimize = new JCheckBox("Tự động tối ưu hóa");
        chkAutoOptimize.setOpaque(false);
        chkAutoOptimize.setFont(FONT_BODY_B);
        chkAutoOptimize.setForeground(TEXT);

        String[] intervals = {"5 Phút", "10 Phút", "30 Phút", "60 Phút"};
        cbOptimizeInterval = new JComboBox<>(intervals);
        cbOptimizeInterval.setSelectedIndex(1);

        lblOptStatus = new JLabel("Trạng thái: Tắt");
        lblOptStatus.setForeground(DANGER);
        lblOptStatus.setFont(FONT_SMALL_B);

        chkAutoOptimize.addActionListener(e -> toggleAutoOptimization());
        cbOptimizeInterval.addActionListener(e -> {
            if (chkAutoOptimize.isSelected()) toggleAutoOptimization();
        });

        pBtns.add(btnOptRam);
        pBtns.add(btnOptCpu);
        pBtns.add(btnFlushLog);

        pAuto.add(chkAutoOptimize);
        pAuto.add(label("Mỗi:", FONT_SMALL, TEXT_SECONDARY));
        pAuto.add(cbOptimizeInterval);
        pAuto.add(Box.createHorizontalStrut(SP_LG));
        pAuto.add(lblOptStatus);

        JPanel content = new JPanel(new BorderLayout(0, SP_SM));
        content.setOpaque(false);
        content.add(pBtns, BorderLayout.NORTH);
        content.add(pAuto, BorderLayout.CENTER);
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private JPanel createGameStatsPanel() {
        JPanel card = card(new BorderLayout(0, SP_MD));
        card.add(sectionHeader("Game Statistics", "Tổng quan dữ liệu game", null), BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 2, SP_LG, SP_SM));
        grid.setOpaque(false);

        lblGiftcodeInfo      = makeStatLine("Giftcodes",     "Loading...");
        lblConsignItemsCount = makeStatLine("Consign Items", "Loading...");
        lblBossStatus        = makeStatLine("Boss Status",   "Loading...");
        lblUptime            = makeStatLine("Uptime",        "Calculating...");

        grid.add(wrapStatLine("Giftcodes",     lblGiftcodeInfo));
        grid.add(wrapStatLine("Consign Items", lblConsignItemsCount));
        grid.add(wrapStatLine("Boss Status",   lblBossStatus));
        grid.add(wrapStatLine("Uptime",        lblUptime));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JLabel makeStatLine(String label, String value) {
        JLabel l = new JLabel(value);
        l.setFont(FONT_H2);
        l.setForeground(TEXT);
        return l;
    }

    private JPanel wrapStatLine(String labelText, JLabel valueLbl) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(labelText.toUpperCase());
        lbl.setFont(FONT_TINY.deriveFont(Font.BOLD));
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        valueLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        p.add(lbl);
        p.add(Box.createVerticalStrut(SP_XS));
        p.add(valueLbl);
        return p;
    }

    // --- [NEW] DIALOG TRIỆU HỒI BOSS VỚI ICON & SEARCH ---
    private void showBossSummonDialog() {
        JDialog d = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Triệu Hồi Boss (Searchable)", true);
        d.setSize(500, 600);
        d.setLocationRelativeTo(this);
        d.setLayout(new BorderLayout(SP_SM, SP_SM));
        d.getContentPane().setBackground(BG);
        ((JPanel) d.getContentPane()).setBorder(pad(SP_MD, SP_MD, SP_MD, SP_MD));

        // 1. Prepare Data
        if (cachedBossEntries.isEmpty()) {
            prepareBossData();
        }

        // 2. Components
        JTextField txtSearch = searchField("Nhập tên boss để tìm...");

        DefaultListModel<BossSummonEntry> listModel = new DefaultListModel<>();
        cachedBossEntries.forEach(listModel::addElement);

        JList<BossSummonEntry> list = new JList<>(listModel);
        list.setCellRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(FONT_BODY);
                lbl.setBorder(new EmptyBorder(SP_XS, SP_SM, SP_XS, SP_SM));
                if (value instanceof BossSummonEntry) {
                    BossSummonEntry entry = (BossSummonEntry) value;
                    lbl.setText(entry.displayName);
                    if (entry.headIconId != -1) {
                        ImageIcon icon = getIconByIconId(entry.headIconId, 25);
                        if (icon != null) {
                            lbl.setIcon(icon);
                        }
                    }
                    lbl.setIconTextGap(10);
                }
                return lbl;
            }
        });

        // 3. Search Logic
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                filter();
            }

            public void removeUpdate(DocumentEvent e) {
                filter();
            }

            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            void filter() {
                String text = txtSearch.getText().toLowerCase();
                listModel.clear();
                for (BossSummonEntry entry : cachedBossEntries) {
                    if (entry.displayName.toLowerCase().contains(text) || entry.keyName.toLowerCase().contains(text)) {
                        listModel.addElement(entry);
                    }
                }
            }
        });

        // 4. Action
        JButton btnSummon = button("TRIỆU HỒI NGAY", ButtonVariant.SUCCESS, "boss");
        btnSummon.setPreferredSize(new Dimension(0, 44));

        Runnable doSummon = () -> {
            BossSummonEntry selected = list.getSelectedValue();
            if (selected != null) {
                summonSpecificBoss(selected.id, selected.displayName);
                d.dispose();
            } else {
                JOptionPane.showMessageDialog(d, "Vui lòng chọn một Boss!");
            }
        };

        btnSummon.addActionListener(e -> doSummon.run());
        list.addMouseListener(new MouseAdapter() {
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    doSummon.run();
                }
            }
        });

        list.setBackground(SURFACE);
        JScrollPane listScroll = new JScrollPane(list);
        listScroll.setBorder(softBorder());
        listScroll.getViewport().setBackground(SURFACE);
        listScroll.getVerticalScrollBar().setUnitIncrement(16);

        d.add(txtSearch, BorderLayout.NORTH);
        d.add(listScroll, BorderLayout.CENTER);
        d.add(btnSummon, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private JPanel createLogPanel() {
        JPanel p = card(new BorderLayout(0, SP_MD));

        JButton clearBtn = button("Clear", ButtonVariant.GHOST, "broom");
        clearBtn.addActionListener(e -> { if (txtLog != null) txtLog.setText(""); });
        p.add(sectionHeader("Server Logs", "Output realtime của server", clearBtn), BorderLayout.NORTH);

        txtLog = new JTextArea(10, 50);
        JScrollPane scrollLog = styleConsole(txtLog);
        scrollLog.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
        scrollLog.setPreferredSize(new Dimension(0, 220));

        p.add(scrollLog, BorderLayout.CENTER);
        return p;
    }

    /**
     * Quét danh sách session trong SessionManager:
     *  - Bỏ entry null khỏi list.
     *  - Với session đã !isConnected(): kick qua Client.kickSession để player được
     *    remove khỏi map online + đóng socket + lưu DB.
     * Báo cụ thể số entry null / số session dead đã dọn.
     */
    private void cleanupDeadSessions() {
        addLog("Đang quét sessions...");
        new Thread(() -> {
            int removedNull = 0;
            int kickedDead = 0;
            int scanned = 0;
            try {
                java.util.List<ISession> all = SessionManager.gI().getSessions();
                if (all == null) {
                    SwingUtilities.invokeLater(() -> addLog("SessionManager trả về list null."));
                    return;
                }
                // Copy snapshot để tránh ConcurrentModification
                java.util.List<ISession> snapshot;
                synchronized (all) {
                    snapshot = new java.util.ArrayList<>(all);
                }
                scanned = snapshot.size();
                for (ISession s : snapshot) {
                    if (s == null) {
                        synchronized (all) { all.remove(null); }
                        removedNull++;
                        continue;
                    }
                    try {
                        if (!s.isConnected()) {
                            if (s instanceof MySession ms) {
                                Client.gI().kickSession(ms);
                            } else {
                                SessionManager.gI().removeSession(s);
                            }
                            kickedDead++;
                        }
                    } catch (Exception ex) {
                        // Session lỗi khi check trạng thái — coi như chết, gỡ luôn
                        try {
                            if (s instanceof MySession ms) Client.gI().kickSession(ms);
                            else SessionManager.gI().removeSession(s);
                            kickedDead++;
                        } catch (Exception ignored) {}
                    }
                }
            } catch (Exception ex) {
                final String msg = ex.getMessage();
                SwingUtilities.invokeLater(() -> addLog("Lỗi khi dọn session: " + msg));
                return;
            }
            final int sScanned = scanned;
            final int sRemovedNull = removedNull;
            final int sKickedDead = kickedDead;
            SwingUtilities.invokeLater(() -> addLog(String.format(
                    "Cleanup: quét %d sessions — đã gỡ %d null + kick %d session chết.",
                    sScanned, sRemovedNull, sKickedDead)));
        }, "SessionCleanup").start();
    }

    // --- [LOGIC] Optimization Methods ---
    private void performRamCleanup() {
        new Thread(() -> {
            // Sử dụng Runtime để tính RAM của riêng Java JVM
            long before = Runtime.getRuntime().freeMemory();
            System.gc(); // Trigger Java Garbage Collector
            long after = Runtime.getRuntime().freeMemory();

            // Tính lượng RAM giải phóng được trong Heap
            long freed = after - before;

            // Format sang MB
            long freedMB = freed / 1024 / 1024;

            String msg = (freed > 0)
                    ? "OPTIMIZE: Đã dọn dẹp JVM Heap. Giải phóng: " + freedMB + " MB."
                    : "OPTIMIZE: JVM RAM đã ở trạng thái tối ưu.";
            addLog(msg);
        }).start();
    }

    private void performCpuOptimization() {
        new Thread(() -> {
            if (txtLog.getDocument().getLength() > 50000) {
                SwingUtilities.invokeLater(() -> {
                    txtLog.setText("");
                    addLog("CPU OPT: Đã xóa bộ đệm Log để giảm tải UI.");
                });
            }

            SwingUtilities.invokeLater(() -> {
                addLog("CPU OPT: Đã kiểm tra và tối ưu bộ đệm Log.");
            });

        }).start();
    }

    private void toggleAutoOptimization() {
        if (activeAutoOptimizeFuture != null) {
            activeAutoOptimizeFuture.cancel(false);
            activeAutoOptimizeFuture = null;
        }

        if (chkAutoOptimize.isSelected()) {
            String selected = (String) cbOptimizeInterval.getSelectedItem();
            int minutes = 10;
            if (selected.contains("5")) {
                minutes = 5;
            } else if (selected.contains("30")) {
                minutes = 30;
            } else if (selected.contains("60")) {
                minutes = 60;
            }

            lblOptStatus.setText("Trạng thái: Đang chạy (" + minutes + "p/lần)");
            lblOptStatus.setForeground(SUCCESS);

            activeAutoOptimizeFuture = scheduler.scheduleAtFixedRate(() -> {
                addLog("AUTO-OPT: Bắt đầu chu trình tối ưu tự động...");
                performRamCleanup();
            }, minutes, minutes, TimeUnit.MINUTES);

            addLog("SYSTEM: Đã bật tự động tối ưu hóa (" + minutes + " phút/lần).");
        } else {
            lblOptStatus.setText("Trạng thái: Tắt");
            lblOptStatus.setForeground(DANGER);
            addLog("SYSTEM: Đã tắt tự động tối ưu hóa.");
        }
    }

    public void addLog(String message) {
        if (txtLog != null) {
            String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            SwingUtilities.invokeLater(() -> {
                txtLog.append("[" + time + "] " + message + "\n");
                txtLog.setCaretPosition(txtLog.getDocument().getLength());
            });
        }
    }

    // ================= MONITORING =================
    private void startMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            // Độ trễ xử lý: khoảng cách thực tế giữa 2 lần tick trừ đi mốc 1000ms.
            // CPU/luồng quá tải -> tick bị trễ -> số này tăng lên.
            long nowMs = System.currentTimeMillis();
            boolean firstTick = (lastStatsTickMs == 0);
            final long delayMs = firstTick ? 0 : Math.max(0, (nowMs - lastStatsTickMs) - 1000);
            lastStatsTickMs = nowMs;

            // 1. Get System Stats
            double cpu = 0;
            try {
                // Ép kiểu rõ ràng để tránh lỗi import với java.lang.management.OperatingSystemMXBean
                OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
                if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                    double load = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad();
                    // Load trả về 0.0 -> 1.0. Nếu lỗi trả về -1
                    if (!Double.isNaN(load) && load >= 0) {
                        cpu = load * 100;
                    }
                }
            } catch (Exception e) {
                cpu = 0; // Fallback an toàn nếu lỗi
            }

            // Tính toán RAM dựa trên Runtime (JVM Memory)
            long maxMemory = Runtime.getRuntime().maxMemory() / (1024 * 1024);
            long totalMemory = Runtime.getRuntime().totalMemory() / (1024 * 1024);
            long freeMemory = Runtime.getRuntime().freeMemory() / (1024 * 1024);
            long usedMemory = totalMemory - freeMemory;

            int threads = Thread.activeCount();
            final String threadInfo = threadBreakdown();
            // In ra console 1 lần lúc đầu tiên để biết chính xác 789 chia thế nào
            if (firstTick) {
                System.out.println(">> Thread breakdown: " + threadInfo);
            }
            int sessions = (SessionManager.gI() != null) ? SessionManager.gI().getSessions().size() : 0;

            // Đếm số IP duy nhất đang kết nối
            java.util.Set<String> ipSet = new java.util.HashSet<>();
            if (Client.gI() != null && Client.gI().getPlayers() != null) {
                for (Player p : Client.gI().getPlayers()) {
                    if (p.getSession() != null && p.getSession().ipAddress != null 
                            && !p.getSession().ipAddress.isEmpty()) {
                        ipSet.add(p.getSession().ipAddress);
                    }
                }
            }
            final int uniqueIPs = ipSet.size();

            // 2. Get Game Stats
            int playerCount = (Client.gI() != null) ? Client.gI().getOnlineCount() : 0; // gồm cả bot
            int giftCodeCount = (GiftCodeManager.gI() != null) ? GiftCodeManager.gI().listGiftCode.size() : 0;
            int consignCount = (ConsignShopManager.gI() != null) ? ConsignShopManager.gI().listItem.size() : 0;

            String bossStats = "N/A";
            if (BossManager.gI() != null) {
                try {
                    int[] stats = BossManager.gI().getBossStatusCounts();
                    bossStats = String.format("Boss: %d Alive | %d Respawn | %d Wait", stats[0], stats[1], stats[2]);
                } catch (Exception e) {
                }
            }

            // 3. Update UI
            String finalBossStats = bossStats;
            final double finalCpu = cpu;

            SwingUtilities.invokeLater(() -> {
                lblCpuUsage.setText(String.format("Server CPU: %.1f%%", finalCpu));
                lblRamUsage.setText(usedMemory + " / " + maxMemory + " MB");

                lblThreadCount.setText(String.valueOf(threads));
                if (tileThreads != null) {
                    tileThreads.setToolTipText(threadInfo);
                }
                lblSessionCount.setText(String.valueOf(sessions));
                lblIpStats.setText("IPs: " + uniqueIPs + " | Max/IP: " + Manager.MAX_PER_IP);
                lblDelayStat.setText(delayMs + " ms");

                lblPlayerCount.setText("Online: " + playerCount);
                lblGiftcodeInfo.setText("Giftcodes: " + giftCodeCount);
                lblConsignItemsCount.setText("Consign Items: " + consignCount);
                lblBossStatus.setText(finalBossStats);

                updateUptime();

                graphCpu.addValue((int) finalCpu);

                // Biểu đồ RAM: Tính % dựa trên Max Memory
                int ramPercent = (maxMemory > 0) ? (int) ((usedMemory * 100) / maxMemory) : 0;
                graphRam.addValue(ramPercent);

                graphThread.addValue(Math.min(threads, 100));
                graphSession.addValue(Math.min(sessions, 100));
            });
        }, 0, 1, TimeUnit.SECONDS);
    }

    /** Đếm số luồng theo nhóm (boss / map / player / khác) để biết 787 luồng đến từ đâu. */
    private static String threadBreakdown() {
        ThreadGroup root = Thread.currentThread().getThreadGroup();
        while (root != null && root.getParent() != null) {
            root = root.getParent();
        }
        if (root == null) {
            return "N/A";
        }
        Thread[] arr = new Thread[root.activeCount() + 200];
        int n = root.enumerate(arr, true);
        int boss = 0, map = 0, player = 0, other = 0;
        for (int i = 0; i < n; i++) {
            Thread t = arr[i];
            if (t == null) {
                continue;
            }
            String nm = t.getName();
            if (nm != null && nm.startsWith("bossgrp-")) {
                boss++;
            } else if (nm != null && nm.startsWith("map-")) {
                map++;
            } else if (nm != null && nm.startsWith("player-")) {
                player++;
            } else {
                other++;
            }
        }
        return "Boss: " + boss + " | Map: " + map + " | Player: " + player
                + " | Khác: " + other + " | Tổng: " + n;
    }

    private void updateUptime() {
        Duration d = Duration.between(serverStartTime, Instant.now());
        lblUptime.setText(String.format("Uptime: %dd %02dh %02dm %02ds",
                d.toDays(), d.toHoursPart(), d.toMinutesPart(), d.toSecondsPart()));
    }

    // ================= ACTIONS LOGIC =================
    private void confirmMaintenance() {
        // Hỏi admin số giây trước khi bảo trì (mặc định 120 giây = 2 phút).
        String input = JOptionPane.showInputDialog(this,
                "Nhập số giây trước khi bảo trì (VD: 120 = 2 phút):",
                "120");
        if (input == null) {
            return; // Admin bấm Cancel
        }
        int seconds;
        try {
            seconds = Integer.parseInt(input.trim());
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Số giây không hợp lệ, vui lòng nhập một số nguyên.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (seconds <= 0) {
            JOptionPane.showMessageDialog(this, "Số giây phải lớn hơn 0.",
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Bắt đầu bảo trì sau " + seconds + " giây?", "Xác nhận",
                JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
            cancelAllScheduledTasks();
            Maintenance.gI().start(seconds); // Gọi hàm bảo trì của server (đơn vị: giây)
            lblStatus.setText("MAINTENANCE STARTING...");
            lblStatus.setForeground(DANGER);
            addLog("ACTION: Bắt đầu chu trình bảo trì (" + seconds + " giây).");
        }
    }
    private void confirmMaintenanceNow() {
        int onlineCount = Client.gI().getPlayers().size();
        int confirm = JOptionPane.showConfirmDialog(this,
                "⚠ BẢO TRÌ NGAY LẬP TỨC?\n\n"
                + "Hiện có " + onlineCount + " người chơi online.\n"
                + "Quy trình: SAVE TẤT CẢ → KICK TẤT CẢ → ĐÓNG SERVER.\n\n"
                + "Bạn có chắc chắn?",
                "Xác nhận bảo trì NGAY",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        cancelAllScheduledTasks();
        lblStatus.setText("MAINTENANCE NOW (saving...)");
        lblStatus.setForeground(DANGER);
        addLog("ACTION: BẢO TRÌ NGAY — bắt đầu save + kick players...");

        // Chạy trên background thread để không freeze UI
        new Thread(() -> {
            int savedCount = 0;
            int kickedCount = 0;
            try {
                // 1) Gửi thông báo cho tất cả player (best effort)
                try {
                    Service.gI().sendThongBaoAllPlayer("Server bảo trì khẩn cấp! Dữ liệu của bạn đã được lưu.");
                } catch (Exception ignored) {}

                // 2) Save tất cả player qua AutoSaveManager
                try {
                    AutoSaveManager.getInstance().handleAutoSave();
                    savedCount = Client.gI().getPlayers().size();
                    SwingUtilities.invokeLater(() -> addLog("ACTION: Đã save " + Client.gI().getPlayers().size() + " players."));
                } catch (Exception ex) {
                    SwingUtilities.invokeLater(() -> addLog("ERROR khi save: " + ex.getMessage()));
                }

                // 3) Kick tất cả player (copy list để tránh ConcurrentModification)
                java.util.List<Player> snapshot = new java.util.ArrayList<>(Client.gI().getPlayers());
//                for (Player p : snapshot) {
//                    try {
//                        if (p != null && p.getSession() instanceof MySession ms) {
//                            Client.gI().kickSession(ms);
//                            kickedCount++;
//                        }
//                    } catch (Exception ignored) {}
//                }
                final int fKicked = kickedCount;
                SwingUtilities.invokeLater(() -> addLog("ACTION: Đã kick " + fKicked + " sessions."));

                // 4) Chờ 1s cho TCP buffer flush rồi đóng server
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                Maintenance.gI().startImmediately();

                SwingUtilities.invokeLater(() -> {
                    lblStatus.setText("MAINTENANCE NOW (shutting down JVM...)");
                    addLog("ACTION: BẢO TRÌ NGAY hoàn tất. JVM sẽ thoát sau 2s...");
                });

                // 5) Tắt hẳn JVM sau 2s để các thread auto-save / log kịp flush
                try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
                System.exit(0);
            } catch (Exception ex) {
                SwingUtilities.invokeLater(() -> addLog("ERROR trong maintenance now: " + ex.getMessage()));
                // Vẫn cố exit dù có lỗi
                try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
                System.exit(1);
            }
        }, "MaintenanceNow-Worker").start();
    }
    private void showReloadOptions() {
        String[] opts = {"GiftCode", "Shop", "NPCs"};
        int c = JOptionPane.showOptionDialog(this, "Chọn dữ liệu cần tải lại:", "Reload DB", 0, 3, null, opts, opts[0]);
        switch (c) {
            case 0 -> {
                loadGiftcode();
            }
            case 1 -> {
                loadShop();
            }
            case 2 -> {
                loadNpcs();
            }
        }
    }

    public void loadGiftcode() {
        GiftCodeManager.gI().listGiftCode.clear();
        String sql = "SELECT * FROM giftcode";
        try (Connection con = DBConnecter.getConnectionServer(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                GiftCode giftcode = new GiftCode();
                giftcode.code = rs.getString("code");
                giftcode.id = rs.getInt("id");
                giftcode.countLeft = rs.getInt("count_left");
                if (giftcode.countLeft == -1) giftcode.countLeft = 999999999;
                // allGender + player_name: KHÔNG được load ở version cũ → gây bug "có người không nhập được"
                // sau khi admin bấm reload panel. Load đầy đủ để match Manager.loadDatabase.
                try {
                    String ag = rs.getString("allGender");
                    giftcode.allGender = "all".equals(ag) || rs.getBoolean("allGender");
                } catch (Exception ignored) {
                    giftcode.allGender = true; // fallback: cho tất cả hành tinh
                }
                try {
                    giftcode.playerName = rs.getString("player_name");
                } catch (Exception ignored) {
                    giftcode.playerName = null;
                }
                giftcode.datecreate = rs.getTimestamp("datecreate");
                giftcode.dateexpired = rs.getTimestamp("expired");
                String detailJson = rs.getString("detail");
                if (detailJson != null && !detailJson.isEmpty() && JSONValue.parse(detailJson) instanceof JSONArray jar) {
                    for (Object itemObj : jar) {
                        JSONObject jsonObj = (JSONObject) itemObj;
                        int itemId = getJsonInt(jsonObj, "id");
                        int quantity = getJsonInt(jsonObj, "quantity");
                        JSONArray optionArray = (JSONArray) jsonObj.get("options");
                        ArrayList<ItemOption> optionList = new ArrayList<>();
                        if (optionArray != null) {
                            for (Object optionObj : optionArray) {
                                JSONObject optionJson = (JSONObject) optionObj;
                                optionList.add(new ItemOption(getJsonInt(optionJson, "id"), getJsonInt(optionJson, "param")));
                            }
                        }
                        giftcode.option.put(itemId, optionList);
                        giftcode.detail.put(itemId, quantity);
                    }
                }
                GiftCodeManager.gI().listGiftCode.add(giftcode);
            }
            addLog("Success: Reloaded " + GiftCodeManager.gI().listGiftCode.size() + " giftcodes.");
        } catch (Exception e) {
            addLog("Error: Failed to reload giftcode. " + e.getMessage());
        }
    }

    public void loadNpcs() {
        String sql = "SELECT * FROM npc_template";
        try (Connection con = DBConnecter.getConnectionData(); PreparedStatement ps = con.prepareStatement(sql); ResultSet rs = ps.executeQuery()) {
            List<NpcTemplate> list = new ArrayList<>();
            while (rs.next()) {
                NpcTemplate npcTemp = new NpcTemplate();
                npcTemp.id = rs.getByte("id");
                npcTemp.name = rs.getString("name");
                npcTemp.head = rs.getShort("head");
                npcTemp.body = rs.getShort("body");
                npcTemp.leg = rs.getShort("leg");
                npcTemp.avatar = rs.getInt("avatar");
                list.add(npcTemp);
            }
            if (!list.isEmpty()) {
                Manager.NPC_TEMPLATES.clear();
                Manager.NPC_TEMPLATES.addAll(list);
                addLog("Success: Reloaded " + Manager.NPC_TEMPLATES.size() + " NPCs.");
            } else {
                addLog("Warning: Không tìm thấy NPC nào từ DB dữ liệu!");
            }
        } catch (Exception e) {
            addLog("Error: Failed to reload NPCs. " + e.getMessage());
        }
    }

    public void loadShop() {
        try (Connection con = DBConnecter.getConnectionData()) {
            List<Shop> list = ShopDAO.getShops(con);
            if (list != null && !list.isEmpty()) {
                Manager.SHOPS = list;
                addLog("Success: Reloaded " + Manager.SHOPS.size() + " shops.");
            } else {
                addLog("Warning: Không tải được shop nào từ DB dữ liệu!");
            }
        } catch (Exception e) {
            addLog("Error: Failed to reload shops. " + e.getMessage());
        }
    }

    private int getJsonInt(JSONObject obj, String key) {
        if (obj != null && obj.containsKey(key)) {
            try {
                return Integer.parseInt(obj.get(key).toString());
            } catch (NumberFormatException e) {
            }
        }
        return 0;
    }

    private void summonSpecificBoss(int id, String name) {
        try {
            Boss b = BossManager.gI().createBoss(id);
            b.changeStatus(BossStatus.RESPAWN);
            addLog("Boss Action: Đã triệu hồi thành công " + name);
        } catch (Exception e) {
            addLog("Boss Error: Lỗi triệu hồi " + name + ": " + e.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi triệu hồi boss: " + e.getMessage());
        }
    }

    private void scheduleMaintenance() {
        Integer h = (Integer) cbHour.getSelectedItem();
        Integer m = (Integer) cbMinute.getSelectedItem();
        Integer s = (Integer) cbSecond.getSelectedItem();

        // ✅ Validate đầy đủ tránh crash
        // ✅ Nếu chọn -1 = TẮT scheduler
        if (h == -1 || m == -1 || s == -1) {

            cancelAllScheduledTasks();

            lblStatus.setText("Maintenance OFF");
            lblStatus.setForeground(TEXT_MUTED);
            lblCountdown.setText("Tắt");

            addLog("Scheduler: Đã tắt");

            // vẫn save config để nhớ trạng thái OFF
            File f = new File(System.getProperty("user.dir"),
                    "maintenanceConfig.txt");

            try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                pw.println(-1);
                pw.println(-1);
                pw.println(-1);
                pw.println(chkAutoRestart.isSelected());
            } catch (IOException e) {
                e.printStackTrace();
            }

            return;
        }

        REQUEST_AUTO_RESTART = chkAutoRestart.isSelected();

        long nowSeconds = LocalTime.now().toSecondOfDay();
        long targetSeconds = LocalTime.of(h, m, s).toSecondOfDay();
        long delay = targetSeconds - nowSeconds;
        if (delay < 0) {
            delay += 86400;
        }

        cancelAllScheduledTasks();

        lblStatus.setText("Maintenance Scheduled");
        lblStatus.setForeground(WARNING);

        long triggerTime = System.currentTimeMillis() + (delay * 1000);

        // ✅ Schedule job
        activeMaintenanceJobFuture
                = scheduler.schedule(() -> Maintenance.gI().start(1),
                        delay, TimeUnit.SECONDS);

        // ✅ Countdown
        activeCountdownDisplayFuture
                = scheduler.scheduleAtFixedRate(() -> {

                    long remain = (triggerTime - System.currentTimeMillis()) / 1000;

                    if (remain <= 0) {
                        activeCountdownDisplayFuture.cancel(false);
                        SwingUtilities.invokeLater(()
                                -> lblCountdown.setText("Executing..."));
                        return;
                    }

                    SwingUtilities.invokeLater(()
                            -> lblCountdown.setText(
                                    String.format("%02d:%02d:%02d",
                                            remain / 3600,
                                            (remain % 3600) / 60,
                                            remain % 60)));

                }, 0, 1, TimeUnit.SECONDS);

        addLog("Scheduler: Đặt lịch bảo trì lúc "
                + String.format("%02d:%02d:%02d", h, m, s)
                + " (AutoRestart: " + REQUEST_AUTO_RESTART + ")");

        // ✅ Save file đúng path
        File f = new File(System.getProperty("user.dir"),
                "maintenanceConfig.txt");

        try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
            pw.println(h);
            pw.println(m);
            pw.println(s);
            pw.println(REQUEST_AUTO_RESTART);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void cancelAllScheduledTasks() {

        if (activeMaintenanceJobFuture != null
                && !activeMaintenanceJobFuture.isCancelled()) {
            activeMaintenanceJobFuture.cancel(false);
        }

        if (activeCountdownDisplayFuture != null
                && !activeCountdownDisplayFuture.isCancelled()) {
            activeCountdownDisplayFuture.cancel(false);
        }

        lblCountdown.setText("Sẵn sàng");
    }

    // ================= INNER CLASSES =================
    // Optimized MiniGraph Class
    private static class MiniGraph extends JPanel {

        private final ArrayList<Integer> values = new ArrayList<>();
        private static final int MAX_POINTS = 60;
        private final Color primaryColor;
        private final Color fillColor;

        public MiniGraph(Color color) {
            this.primaryColor = color;
            this.fillColor = withAlpha(color, 46);
            setOpaque(false);
        }

        public void addValue(int v) {
            if (values.size() >= MAX_POINTS) {
                values.remove(0);
            }
            values.add(v);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (values.isEmpty()) {
                return;
            }

            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);

            int w = getWidth();
            int h = getHeight();
            int maxVal = 100;
            float step = (float) w / (MAX_POINTS - 1);

            int n = values.size();
            float[] xs = new float[n];
            float[] ys = new float[n];
            float pad = 3f; // giữ đường line không chạm mép trên/dưới
            for (int i = 0; i < n; i++) {
                xs[i] = i * step;
                ys[i] = h - pad - ((float) values.get(i) / maxVal * (h - pad * 2));
            }

            // Smooth polyline: nối qua trung điểm bằng đường cong bậc 2
            Path2D.Float line = new Path2D.Float();
            line.moveTo(xs[0], ys[0]);
            for (int i = 1; i < n; i++) {
                float mx = (xs[i - 1] + xs[i]) / 2f;
                float my = (ys[i - 1] + ys[i]) / 2f;
                line.quadTo(xs[i - 1], ys[i - 1], mx, my);
            }
            line.lineTo(xs[n - 1], ys[n - 1]);

            // Vùng gradient mờ dần bên dưới đường line
            Path2D.Float area = new Path2D.Float(line);
            area.lineTo(xs[n - 1], h);
            area.lineTo(xs[0], h);
            area.closePath();
            g2.setPaint(new GradientPaint(0, 0, fillColor, 0, h, withAlpha(primaryColor, 0)));
            g2.fill(area);

            // Baseline mảnh, kín đáo ở đáy biểu đồ
            g2.setColor(withAlpha(BORDER_STRONG, 130));
            g2.setStroke(new BasicStroke(1f));
            g2.drawLine(0, h - 1, w, h - 1);

            // Đường line chính
            g2.setColor(primaryColor);
            g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(line);
            g2.dispose();
        }
    }
    
        // Hiển thị danh sách player theo IP
    private void showPlayersByIpDialog() {
        if (Client.gI() == null || Client.gI().getPlayers() == null) {
            JOptionPane.showMessageDialog(this, "Không có người chơi nào online");
            return;
        }
        
        // Map IP -> List of players
        java.util.Map<String, java.util.List<String>> ipToPlayers = new java.util.LinkedHashMap<>();
        
        for (Player p : Client.gI().getPlayers()) {
            if (p.getSession() != null && p.getSession().ipAddress != null 
                    && !p.getSession().ipAddress.isEmpty()) {
                String ip = p.getSession().ipAddress;
                String playerName = p.name != null ? p.name : "Unknown";
                
                ipToPlayers.computeIfAbsent(ip, k -> new java.util.ArrayList<>()).add(playerName);
            }
        }
        
        if (ipToPlayers.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Không có dữ liệu IP");
            return;
        }
        
        // Tạo dialog
        javax.swing.JDialog dialog = new javax.swing.JDialog((java.awt.Frame) null, "Danh Sách Player Theo IP", true);
        dialog.setSize(500, 400);
        dialog.setLocationRelativeTo(null);
        
        JPanel contentPanel = new JPanel(new BorderLayout(SP_MD, SP_MD));
        contentPanel.setBorder(pad(SP_LG, SP_LG, SP_LG, SP_LG));
        contentPanel.setBackground(BG);

        // Tạo danh sách IP và player
        JPanel listPanel = new JPanel();
        listPanel.setLayout(new BoxLayout(listPanel, BoxLayout.Y_AXIS));
        listPanel.setBackground(BG);

        JScrollPane scrollPane = new JScrollPane(listPanel);
        scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.getViewport().setBackground(BG);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        for (java.util.Map.Entry<String, java.util.List<String>> entry : ipToPlayers.entrySet()) {
            String ip = entry.getKey();
            java.util.List<String> players = entry.getValue();

            JPanel ipPanel = new JPanel(new BorderLayout(SP_XS, SP_XS));
            ipPanel.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER),
                    pad(SP_SM, SP_SM, SP_SM, SP_SM)));
            ipPanel.setMaximumSize(new Dimension(450, 60 + players.size() * 20));
            ipPanel.setBackground(SURFACE);

            JLabel ipLabel = new JLabel(ip + " (" + players.size() + " người)");
            ipLabel.setFont(FONT_H3);
            ipLabel.setForeground(PRIMARY);
            ipLabel.setIcon(vectorIcon("globe", 14, PRIMARY));
            ipLabel.setIconTextGap(SP_SM);
            ipPanel.add(ipLabel, BorderLayout.NORTH);

            StringBuilder playerList = new StringBuilder();
            for (int i = 0; i < players.size(); i++) {
                playerList.append("    • ").append(players.get(i));
                if (i < players.size() - 1) playerList.append("\n");
            }

            JTextArea playerText = new JTextArea(playerList.toString());
            playerText.setEditable(false);
            playerText.setFont(FONT_SMALL);
            playerText.setForeground(TEXT_SECONDARY);
            playerText.setBackground(SURFACE);
            playerText.setBorder(null);
            ipPanel.add(playerText, BorderLayout.CENTER);

            listPanel.add(ipPanel);
            listPanel.add(Box.createVerticalStrut(SP_SM));
        }

        // Tạo panel chính để chứa scrollPane và nút đóng
        JPanel mainPanel = new JPanel(new BorderLayout(SP_MD, SP_MD));
        mainPanel.setOpaque(false);
        mainPanel.add(scrollPane, BorderLayout.CENTER);

        JButton btnClose = button("Đóng", ButtonVariant.SECONDARY, "x");
        btnClose.addActionListener(e -> dialog.dispose());
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        btnPanel.add(btnClose);
        mainPanel.add(btnPanel, BorderLayout.SOUTH);
        
        contentPanel.add(mainPanel, BorderLayout.CENTER);
        
        dialog.setContentPane(contentPanel);
        dialog.setVisible(true);
    }
}
