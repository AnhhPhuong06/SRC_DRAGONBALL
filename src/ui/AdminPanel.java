package ui;

/*
 * Panel quản lý server (Swing) - hiển thị khi run server.
 * Tích hợp các lệnh console (bảo trì, thông báo, kick, lưu data, lưu bang)
 * thành nút bấm + bảng người chơi online + console log màu thời gian thực.
 */

import server.Client;
import server.GiftCodeAdmin;
import server.Maintenance;
import server.Manager;
import server.ServerManager;
import consts.AppearType;
import consts.BossStatus;
import database.daos.PlayerDAO;
import event.EventManager;
import event.event_list.Christmas;
import event.event_list.Halloween;
import event.event_list.HungVuong;
import event.event_list.InternationalWomensDay;
import event.event_list.LunarNewYear;
import event.event_list.TopUp;
import event.event_list.TrungThu;
import interfaces.ISession;
import managers.GiftCodeManager;
import managers.boss.BossManager;
import models.GiftCode;
import models.boss.Boss;
import models.player.Player;
import network.session.MySession;
import network.session.SessionManager;
import services.GiftCodeService;
import services.ItemService;
import services.Service;
import services.player.ClanService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import javax.swing.plaf.basic.BasicScrollBarUI;
import javax.swing.plaf.basic.BasicTabbedPaneUI;
import javax.swing.table.TableCellRenderer;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class AdminPanel {

    /* ===================== Theme ===================== */
    private static final Color BG = new Color(0x0D1117);
    private static final Color BG_PANEL = new Color(0x161B22);
    private static final Color BG_CHIP = new Color(0x21262D);
    private static final Color BG_HOVER = new Color(0x1F2937);
    private static final Color BG_INPUT = new Color(0x0D1117);
    private static final Color FG = new Color(0xE6EDF3);
    private static final Color FG_DIM = new Color(0x7D8590);
    private static final Color FG_FAINT = new Color(0x484F58);
    private static final Color ACCENT = new Color(0x58A6FF);
    private static final Color ACCENT_2 = new Color(0x3FB950);
    private static final Color BORDER = new Color(0x30363D);
    private static final Color GRID = new Color(0x21262D);

    private static final Color C_RED = new Color(0xF85149);
    private static final Color C_GREEN = new Color(0x3FB950);
    private static final Color C_YELLOW = new Color(0xD29922);
    private static final Color C_BLUE = new Color(0x58A6FF);
    private static final Color C_PURPLE = new Color(0xBC8CFF);

    private static final Color BTN_PRIMARY = new Color(0x238636);
    private static final Color BTN_PRIMARY_HOVER = new Color(0x2EA043);
    private static final Color BTN_DANGER = new Color(0xDA3633);
    private static final Color BTN_DANGER_HOVER = new Color(0xF85149);
    private static final Color BTN_NEUTRAL = new Color(0x21262D);
    private static final Color BTN_NEUTRAL_HOVER = new Color(0x30363D);

    private static final Font FONT_UI = new Font("Segoe UI", Font.PLAIN, 13);
    private static final Font FONT_UI_BOLD = new Font("Segoe UI", Font.BOLD, 13);
    private static final Font FONT_UI_SM = new Font("Segoe UI", Font.PLAIN, 11);
    private static final Font FONT_TITLE = new Font("Segoe UI", Font.BOLD, 16);
    private static final Font FONT_MONO = new Font(Font.MONOSPACED, Font.PLAIN, 13);

    /* ===================== Console redirect ===================== */
    private static final ConcurrentLinkedQueue<String> LOG_QUEUE = new ConcurrentLinkedQueue<>();
    /** Đếm size O(1) thay cho LOG_QUEUE.size() — ConcurrentLinkedQueue.size() là O(n). */
    private static final java.util.concurrent.atomic.AtomicInteger LOG_QUEUE_SIZE = new java.util.concurrent.atomic.AtomicInteger();
    private static final int LOG_QUEUE_CAP = 8000;
    private static final Pattern ANSI = Pattern.compile("\\[[0-9;]*m");
    /** Matcher dùng lại trên EDT trong drainLog để tránh allocate per-line. */
    private static Matcher ansiMatcher;
    private static volatile boolean redirectInstalled = false;

    /* ===================== Widgets ===================== */
    private static JFrame frame;
    private static JTextPane console;
    private static StyledDocument doc;
    private static JCheckBox autoScroll;
    private static JLabel lblStatus, lblPlayers, lblThreads, lblUptime, lblHeap;
    private static DefaultTableModel playerModel;
    private static JTable playerTable;
    private static JLabel lblPlayerCount;
    private static JTextField txtBroadcast;
    private static JSpinner spnMaint;
    private static DefaultTableModel giftcodeModel;
    private static JTable giftcodeTable;
    private static JLabel lblGiftCount;
    private static JTextField txtGiftPlayer;
    private static JTextField txtGiftCode;
    private static DefaultTableModel bossModel;
    private static JTable bossTable;
    private static JLabel lblBossCount;
    private static JSpinner spnExpRate;
    private static JLabel lblExpRateCurrent;

    private static final SimpleDateFormat TS_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private static final int UNLIMITED = 999999999;

    private static int tick = 0;

    /* ===================== Caches (giảm allocation trên hot path) ===================== */
    private static final java.util.Map<Color, SimpleAttributeSet> ATTRS = new java.util.HashMap<>();
    private static final Color ROW_ALT = new Color(0x10161E);
    private static final EmptyBorder CELL_PADDING = new EmptyBorder(0, 12, 0, 12);
    private static final ExecutorService BG_POOL = Executors.newFixedThreadPool(2, r -> {
        Thread t = new Thread(r, "AdminPanel-Action");
        t.setDaemon(true);
        return t;
    });

    /* ===================== State tracking (tránh refresh thừa) ===================== */
    private static volatile int activeTabIdx = 0;
    private static long lastPlayersHash;
    private static long lastBossHash;
    private static long lastGiftHash;
    private static int lastBossSize = -1;
    private static int lastGiftSize = -1;
    private static int lastPlayersSize = -1;
    private static String lastStatusText = "";
    private static String lastPlayersText = "";
    private static String lastThreadsText = "";
    private static String lastHeapText = "";
    private static String lastUptimeText = "";

    /** Event đã được init() trong session này (NPC/boss đã spawn ra map). */
    private static final java.util.Set<String> eventsRunning = java.util.Collections.synchronizedSet(new java.util.HashSet<>());

    /** Tự đồng bộ giftcode từ DB (bắt thay đổi do sửa trực tiếp trong SQL). */
    private static volatile boolean autoReloadGiftDb = true;
    private static volatile long lastGiftDbReloadAt = 0L;
    private static final long GIFT_DB_RELOAD_INTERVAL_MS = 5_000L;

    private AdminPanel() {
    }

    /** Gọi 1 lần khi server khởi động. An toàn khi chạy headless (sẽ bỏ qua GUI). */
    public static void launch() {
        if (GraphicsEnvironment.isHeadless()) {
            return;
        }
        installConsoleRedirect();
        SwingUtilities.invokeLater(AdminPanel::createAndShow);
    }

    /* ===================== Build UI ===================== */
    private static void createAndShow() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }

        frame = new JFrame("Bảng điều khiển - " + ServerManager.NAME
                + "  (" + ServerManager.IP + ":" + ServerManager.PORT + ")");
        frame.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
        frame.setSize(1280, 780);
        frame.setMinimumSize(new Dimension(1000, 600));
        frame.setLocationRelativeTo(null);
        frame.getContentPane().setBackground(BG);
        frame.setLayout(new BorderLayout(0, 0));

        frame.add(buildHeader(), BorderLayout.NORTH);

        JSplitPane dashboard = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                buildConsolePanel(), buildPlayerPanel());
        dashboard.setResizeWeight(0.68);
        dashboard.setBorder(null);
        dashboard.setBackground(BG);
        dashboard.setDividerSize(8);
        dashboard.setUI(new javax.swing.plaf.basic.BasicSplitPaneUI() {
            @Override
            public javax.swing.plaf.basic.BasicSplitPaneDivider createDefaultDivider() {
                return new javax.swing.plaf.basic.BasicSplitPaneDivider(this) {
                    @Override
                    public void paint(Graphics g) {
                        g.setColor(BG);
                        g.fillRect(0, 0, getWidth(), getHeight());
                    }
                };
            }
        });

        JTabbedPane tabs = new JTabbedPane();
        tabs.setUI(new FlatTabbedPaneUI());
        tabs.setBackground(BG);
        tabs.setForeground(FG);
        tabs.setFont(FONT_UI_BOLD);
        tabs.setBorder(BorderFactory.createEmptyBorder());
        tabs.addTab("Điều khiển", dashboard);
        tabs.addTab("Giftcode", buildGiftcodePanel());
        tabs.addTab("Boss", buildBossPanel());
        tabs.addTab("Sự kiện", buildEventPanel());
        tabs.addTab("Cấu hình", buildConfigPanel());
        tabs.addTab("Tỉ lệ đập đồ", new server.ui.CombineRatePanel());

        tabs.addTab("Vòng quay thỏi", new server.ui.VongQuayPanel());
        tabs.addTab("Bản đồ", new server.ui.MapEditorPanel());
        tabs.addChangeListener(e -> {
            activeTabIdx = tabs.getSelectedIndex();
            refreshActiveTab();
        });
        frame.add(tabs, BorderLayout.CENTER);

        frame.add(buildControlBar(), BorderLayout.SOUTH);

        frame.addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                onCloseRequest();
            }
        });

        // Console: drain ít hơn nhưng batch lớn, giảm overhead EDT
        new Timer(250, e -> drainLog()).start();
        // Stats nhẹ: 1s. Refresh table chỉ chạy cho tab đang xem.
        new Timer(1000, e -> {
            tick++;
            updateStats();
            if (tick % 3 == 0) {
                refreshActiveTab();
            }
        }).start();

        frame.setVisible(true);
        updateStats();
        refreshPlayers();
    }

    /* ---------- Header (thống kê) ---------- */
    private static JPanel buildHeader() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteBottomBorder(BORDER),
                new EmptyBorder(12, 18, 12, 18)));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleRow.setOpaque(false);
        titleRow.add(new GlowDot(ACCENT_2, 10));
        JLabel title = new JLabel(ServerManager.NAME);
        title.setFont(FONT_TITLE);
        title.setForeground(FG);
        titleRow.add(title);
        titleRow.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(ServerManager.IP + ":" + ServerManager.PORT);
        sub.setFont(FONT_UI_SM);
        sub.setForeground(FG_DIM);
        sub.setBorder(new EmptyBorder(2, 18, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(titleRow);
        left.add(sub);
        bar.add(left, BorderLayout.WEST);

        JPanel chips = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        chips.setOpaque(false);

        lblStatus = chip("TRẠNG THÁI", "—", ACCENT_2);
        lblPlayers = chip("ONLINE", "0", C_BLUE);
        lblThreads = chip("THREAD", "0", C_PURPLE);
        lblHeap = chip("RAM HEAP", "0", C_YELLOW);
        lblUptime = chip("UPTIME", "00:00:00", FG);

        chips.add(lblStatus.getParent());
        chips.add(lblPlayers.getParent());
        chips.add(lblThreads.getParent());
        chips.add(lblHeap.getParent());
        chips.add(lblUptime.getParent());
        bar.add(chips, BorderLayout.EAST);
        return bar;
    }

    /** Tạo 1 "chip" thống kê có dải màu nhỏ bên trái. */
    private static JLabel chip(String caption, String value, Color valueColor) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(BG_CHIP);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(valueColor);
                g2.fillRoundRect(0, 0, 3, getHeight(), 8, 8);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(7, 14, 7, 14));

        JPanel inner = new JPanel();
        inner.setOpaque(false);
        inner.setLayout(new BoxLayout(inner, BoxLayout.Y_AXIS));

        JLabel cap = new JLabel(caption);
        cap.setFont(new Font("Segoe UI", Font.PLAIN, 9));
        cap.setForeground(FG_DIM);
        cap.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel val = new JLabel(value);
        val.setFont(new Font("Segoe UI", Font.BOLD, 14));
        val.setForeground(valueColor);
        val.setAlignmentX(Component.LEFT_ALIGNMENT);

        inner.add(cap);
        inner.add(val);
        p.add(inner, BorderLayout.CENTER);
        return val;
    }

    /* ---------- Console log ---------- */
    private static JPanel buildConsolePanel() {
        JPanel card = card();
        card.setBorder(new EmptyBorder(14, 16, 8, 8));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        JLabel ic = new JLabel("☰");
        ic.setForeground(ACCENT);
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        JLabel t = new JLabel("Nhật ký máy chủ");
        t.setForeground(FG);
        t.setFont(FONT_UI_BOLD);
        titleBox.add(ic);
        titleBox.add(t);
        head.add(titleBox, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        autoScroll = new JCheckBox("Tự cuộn", true);
        autoScroll.setOpaque(false);
        autoScroll.setForeground(FG_DIM);
        autoScroll.setFont(FONT_UI);
        autoScroll.setFocusPainted(false);
        JButton clear = smallButton("Xoá log");
        clear.addActionListener(e -> {
            try {
                doc.remove(0, doc.getLength());
            } catch (BadLocationException ignored) {
            }
        });
        right.add(autoScroll);
        right.add(clear);
        head.add(right, BorderLayout.EAST);
        head.setBorder(new EmptyBorder(0, 0, 10, 0));
        inner.add(head, BorderLayout.NORTH);

        console = new JTextPane();
        console.setEditable(false);
        console.setBackground(BG_INPUT);
        console.setForeground(FG);
        console.setFont(FONT_MONO);
        console.setCaretColor(ACCENT);
        console.setBorder(new EmptyBorder(10, 12, 10, 12));
        doc = console.getStyledDocument();
        DefaultCaret caret = (DefaultCaret) console.getCaret();
        caret.setUpdatePolicy(DefaultCaret.NEVER_UPDATE);

        JScrollPane sp = new JScrollPane(console);
        sp.setBorder(new RoundedBorder(BORDER, 8));
        sp.getViewport().setBackground(BG_INPUT);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        styleScrollPane(sp);
        inner.add(sp, BorderLayout.CENTER);

        card.add(inner, BorderLayout.CENTER);
        return card;
    }

    /* ---------- Bảng người chơi ---------- */
    private static JPanel buildPlayerPanel() {
        JPanel card = card();
        card.setBorder(new EmptyBorder(14, 8, 8, 16));

        JPanel inner = new JPanel(new BorderLayout());
        inner.setOpaque(false);
        inner.setBorder(new EmptyBorder(14, 16, 14, 16));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        head.setBorder(new EmptyBorder(0, 4, 10, 4));
        JPanel titleBox = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        titleBox.setOpaque(false);
        JLabel ic = new JLabel("◉");
        ic.setForeground(ACCENT_2);
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 14));
        JLabel t = new JLabel("Người chơi online");
        t.setForeground(FG);
        t.setFont(FONT_UI_BOLD);
        titleBox.add(ic);
        titleBox.add(t);
        head.add(titleBox, BorderLayout.WEST);
        lblPlayerCount = pill("0", ACCENT);
        head.add(lblPlayerCount.getParent(), BorderLayout.EAST);
        inner.add(head, BorderLayout.NORTH);

        playerModel = new DefaultTableModel(new Object[]{"ID", "Tên", "Map"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        playerTable = makeStyledTable(playerModel);
        playerTable.getColumnModel().getColumn(0).setPreferredWidth(70);
        playerTable.getColumnModel().getColumn(1).setPreferredWidth(150);
        playerTable.getColumnModel().getColumn(2).setPreferredWidth(120);

        JScrollPane sp = new JScrollPane(playerTable);
        sp.setBorder(new RoundedBorder(BORDER, 8));
        sp.getViewport().setBackground(BG_INPUT);
        styleScrollPane(sp);
        inner.add(sp, BorderLayout.CENTER);

        JPanel actions = new JPanel(new GridLayout(1, 3, 8, 0));
        actions.setOpaque(false);
        actions.setBorder(new EmptyBorder(10, 0, 0, 0));
        JButton btnRefresh = smallButton("Làm mới");
        btnRefresh.addActionListener(e -> refreshPlayers());
        JButton btnSaveSel = smallButton("Lưu");
        btnSaveSel.addActionListener(e -> saveSelectedPlayer());
        JButton btnKickSel = dangerButton("Kick");
        btnKickSel.addActionListener(e -> kickSelectedPlayer());
        actions.add(btnRefresh);
        actions.add(btnSaveSel);
        actions.add(btnKickSel);
        inner.add(actions, BorderLayout.SOUTH);

        card.add(inner, BorderLayout.CENTER);
        card.setPreferredSize(new Dimension(380, 100));
        return card;
    }

    /* ---------- Tab Giftcode ---------- */
    private static JPanel buildGiftcodePanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.setBorder(new EmptyBorder(16, 18, 12, 18));

        JPanel toolbar = sectionHeader("🎁", "Quản lý giftcode");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        JButton btnReload = smallButton("Tải lại");
        btnReload.addActionListener(e -> reloadGiftcodes());
        JButton btnAddShare = successButton("🎁 Tạo Code Share (50)");
        btnAddShare.addActionListener(e -> openGiftcodeShareDialog());
        JButton btnAdd = primaryButton("+ Thêm mới");
        btnAdd.addActionListener(e -> openGiftcodeEditor(null, new ArrayList<>()));
        JButton btnEdit = smallButton("Sửa");
        btnEdit.addActionListener(e -> editSelectedGiftcode());
        JButton btnDel = dangerButton("Xoá");
        btnDel.addActionListener(e -> deleteSelectedGiftcode());
        JCheckBox cbAutoDb = new JCheckBox("Tự đồng bộ DB", autoReloadGiftDb);
        cbAutoDb.setOpaque(false);
        cbAutoDb.setForeground(FG_DIM);
        cbAutoDb.setFont(FONT_UI);
        cbAutoDb.setFocusPainted(false);
        cbAutoDb.setToolTipText("Định kỳ đọc lại bảng giftcode từ DB ("
                + (GIFT_DB_RELOAD_INTERVAL_MS / 1000) + "s) — bắt được thay đổi do sửa trực tiếp trong SQL");
        cbAutoDb.addActionListener(e -> autoReloadGiftDb = cbAutoDb.isSelected());
        btns.add(btnReload);
        btns.add(btnAddShare);
        btns.add(btnAdd);
        btns.add(btnEdit);
        btns.add(btnDel);
        btns.add(cbAutoDb);
        ((JPanel) toolbar.getComponent(0)).add(Box.createHorizontalStrut(14));
        ((JPanel) toolbar.getComponent(0)).add(btns);

        lblGiftCount = pill("0", ACCENT);
        toolbar.add(lblGiftCount.getParent(), BorderLayout.EAST);
        wrap.add(toolbar, BorderLayout.NORTH);

        giftcodeModel = new DefaultTableModel(
                new Object[]{"ID", "Code", "Còn lại", "Giới tính", "Player", "Ngày tạo", "Hết hạn", "Vật phẩm"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        giftcodeTable = makeStyledTable(giftcodeModel);
        int[] widths = {40, 120, 60, 80, 120, 130, 130, 220};
        for (int i = 0; i < widths.length; i++) {
            giftcodeTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        giftcodeTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editSelectedGiftcode();
                }
            }
        });
        // Click chọn row → auto-fill code vào ô tặng
        giftcodeTable.getSelectionModel().addListSelectionListener(e -> {
            if (e.getValueIsAdjusting() || txtGiftCode == null) {
                return;
            }
            GiftCode g = selectedGiftcode();
            if (g != null) {
                txtGiftCode.setText(g.code);
            }
        });

        JScrollPane sp = new JScrollPane(giftcodeTable);
        sp.setBorder(new RoundedBorder(BORDER, 8));
        sp.getViewport().setBackground(BG_INPUT);
        styleScrollPane(sp);
        wrap.add(sp, BorderLayout.CENTER);

        // ----- Khu nhập "Tặng cho player" -----
        JPanel south = new JPanel();
        south.setOpaque(false);
        south.setLayout(new BoxLayout(south, BoxLayout.Y_AXIS));
        south.setBorder(new EmptyBorder(12, 0, 0, 0));

        JPanel giveRow = new JPanel(new BorderLayout(8, 0));
        giveRow.setOpaque(false);

        JLabel ic = new JLabel("🎁");
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        ic.setForeground(C_YELLOW);
        ic.setBorder(new EmptyBorder(0, 4, 0, 6));
        giveRow.add(ic, BorderLayout.WEST);

        JPanel inputs = new JPanel(new GridBagLayout());
        inputs.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.fill = GridBagConstraints.HORIZONTAL;
        gc.insets = new Insets(0, 0, 0, 8);

        gc.gridx = 0;
        gc.weightx = 0;
        JLabel l1 = new JLabel("Tên player");
        l1.setForeground(FG_DIM);
        l1.setFont(FONT_UI);
        inputs.add(l1, gc);

        gc.gridx = 1;
        gc.weightx = 0.45;
        txtGiftPlayer = new JTextField();
        styleTextField(txtGiftPlayer);
        txtGiftPlayer.addActionListener(e -> doGiveGiftcode());
        inputs.add(txtGiftPlayer, gc);

        gc.gridx = 2;
        gc.weightx = 0;
        JLabel l2 = new JLabel("Code");
        l2.setForeground(FG_DIM);
        l2.setFont(FONT_UI);
        inputs.add(l2, gc);

        gc.gridx = 3;
        gc.weightx = 0.55;
        txtGiftCode = new JTextField();
        styleTextField(txtGiftCode);
        txtGiftCode.addActionListener(e -> doGiveGiftcode());
        inputs.add(txtGiftCode, gc);

        giveRow.add(inputs, BorderLayout.CENTER);

        JButton btnGive = primaryButton("🎁 Tặng");
        btnGive.addActionListener(e -> doGiveGiftcode());
        JPanel btnWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnWrap.setOpaque(false);
        btnWrap.add(Box.createHorizontalStrut(4));
        btnWrap.add(btnGive);
        giveRow.add(btnWrap, BorderLayout.EAST);

        south.add(giveRow);

        JLabel hint = new JLabel("<html>Click chọn 1 dòng để tự điền code. "
                + "Player phải đang <b>online</b>. Nhấp đúp để sửa giftcode. "
                + "ID vật phẩm: <b>-1</b> vàng, <b>-2</b> ngọc, <b>-3</b> ngọc khoá. "
                + "Options dạng <b>id:param</b> cách nhau dấu phẩy.</html>");
        hint.setForeground(FG_DIM);
        hint.setFont(FONT_UI_SM);
        hint.setBorder(new EmptyBorder(10, 4, 0, 4));
        south.add(hint);

        wrap.add(south, BorderLayout.SOUTH);
        return wrap;
    }

    /** Tặng giftcode cho player online qua GiftCodeService. */
    private static void doGiveGiftcode() {
        String name = txtGiftPlayer.getText().trim();
        String code = txtGiftCode.getText().trim();
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Hãy nhập tên player.");
            return;
        }
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Hãy nhập code (hoặc click chọn 1 dòng trong bảng).");
            return;
        }
        Player pl;
        try {
            pl = Client.gI().getPlayer(name);
        } catch (Exception ex) {
            pl = null;
        }
        if (pl == null) {
            JOptionPane.showMessageDialog(frame, "Không tìm thấy player \"" + name + "\" đang online.");
            return;
        }
        final Player target = pl;
        runBg(() -> {
            try {
                GiftCodeService.gI().giftCode(target, code);
                utils.Logger.success("[PANEL] Đã tặng code \"" + code + "\" cho " + target.name + "\n");
                SwingUtilities.invokeLater(() -> {
                    txtGiftPlayer.setText("");
                    refreshGiftcodeTable();
                });
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi tặng giftcode: " + ex.getMessage() + "\n");
            }
        });
    }

    /* ---------- Tab Boss ---------- */
    private static JPanel buildBossPanel() {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.setBorder(new EmptyBorder(16, 18, 12, 18));

        JPanel toolbar = sectionHeader("⚔", "Quản lý boss");
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btns.setOpaque(false);
        JButton btnReload = smallButton("Làm mới");
        btnReload.addActionListener(e -> refreshBossTable());
        btns.add(btnReload);
        ((JPanel) toolbar.getComponent(0)).add(Box.createHorizontalStrut(14));
        ((JPanel) toolbar.getComponent(0)).add(btns);

        lblBossCount = pill("0", ACCENT);
        toolbar.add(lblBossCount.getParent(), BorderLayout.EAST);
        wrap.add(toolbar, BorderLayout.NORTH);

        bossModel = new DefaultTableModel(
                new Object[]{"#", "Tên boss", "Trạng thái", "Map", "Hồi sinh sau"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        bossTable = makeStyledTable(bossModel);
        int[] widths = {50, 220, 130, 320, 130};
        for (int i = 0; i < widths.length; i++) {
            bossTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }
        bossTable.getColumnModel().getColumn(2).setCellRenderer(new BossStatusRenderer());
        // Đã khóa tính năng điều khiển boss qua panel

        JScrollPane sp = new JScrollPane(bossTable);
        sp.setBorder(new RoundedBorder(BORDER, 8));
        sp.getViewport().setBackground(BG_INPUT);
        styleScrollPane(sp);
        wrap.add(sp, BorderLayout.CENTER);

        JLabel hint = new JLabel("<html>Danh sách boss (chỉ xem). Tính năng điều khiển boss đã bị khóa.</html>");
        hint.setForeground(FG_DIM);
        hint.setFont(FONT_UI_SM);
        hint.setBorder(new EmptyBorder(10, 4, 0, 4));
        wrap.add(hint, BorderLayout.SOUTH);
        return wrap;
    }

    private static void refreshBossTable() {
        if (bossModel == null) {
            return;
        }
        List<Boss> bosses;
        try {
            bosses = new ArrayList<>(BossManager.gI().getBosses());
        } catch (Exception e) {
            return;
        }
        // Hash dựa trên status + map+zone của từng boss (đủ phát hiện thay đổi)
        long h = 1L;
        for (Boss b : bosses) {
            if (b == null) {
                continue;
            }
            int st = b.bossStatus == null ? -1 : b.bossStatus.ordinal();
            int loc = (b.zone != null && b.zone.map != null)
                    ? (b.zone.map.mapId << 8) | (b.zone.zoneId & 0xFF) : 0;
            h = h * 1315423911L ^ ((long) st << 32 | (loc & 0xFFFFFFFFL));
        }
        if (bosses.size() == lastBossSize && h == lastBossHash) {
            return;
        }
        lastBossSize = bosses.size();
        lastBossHash = h;

        int keepIdx = -1;
        int viewRow = bossTable.getSelectedRow();
        if (viewRow >= 0) {
            try {
                keepIdx = Integer.parseInt(String.valueOf(
                        bossModel.getValueAt(bossTable.convertRowIndexToModel(viewRow), 0)));
            } catch (Exception ignored) {
            }
        }
        lblBossCount.setText(String.valueOf(bosses.size()));
        bossModel.setRowCount(0);
        long now = System.currentTimeMillis();
        for (int i = 0; i < bosses.size(); i++) {
            Boss boss = bosses.get(i);
            if (boss == null || boss.data == null || boss.data.length == 0) {
                continue;
            }
            String name;
            try {
                name = boss.data[0].getName();
            } catch (Exception ex) {
                name = "?";
            }
            String status = boss.bossStatus == null ? "?" : boss.bossStatus.toString();
            String map;
            String respawn = "—";
            if (boss.zone != null && boss.zone.map != null) {
                map = boss.zone.map.mapName + " (" + boss.zone.map.mapId + ") khu " + boss.zone.zoneId;
            } else {
                map = "—";
                try {
                    long respawnMs = boss.lastTimeRest + boss.data[0].getSecondsRest() * 1000L;
                    long left = Math.max(0, respawnMs - now) / 1000;
                    respawn = left + "s";
                } catch (Exception ignored) {
                }
            }
            bossModel.addRow(new Object[]{i, name, status, map, respawn});
        }
        if (keepIdx >= 0) {
            for (int i = 0; i < bossModel.getRowCount(); i++) {
                if (String.valueOf(bossModel.getValueAt(i, 0)).equals(String.valueOf(keepIdx))) {
                    int vr = bossTable.convertRowIndexToView(i);
                    bossTable.setRowSelectionInterval(vr, vr);
                    break;
                }
            }
        }
    }

    private static Boss selectedBoss() {
        int row = bossTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int idx;
        try {
            idx = Integer.parseInt(String.valueOf(
                    bossModel.getValueAt(bossTable.convertRowIndexToModel(row), 0)));
        } catch (Exception e) {
            return null;
        }
        List<Boss> list = BossManager.gI().getBosses();
        if (idx < 0 || idx >= list.size()) {
            return null;
        }
        return list.get(idx);
    }

    private static void callSelectedBoss() {
        Boss boss = selectedBoss();
        if (boss == null) {
            JOptionPane.showMessageDialog(frame, "Hãy chọn một boss để triệu hồi.");
            return;
        }
        String name = "?";
        try {
            name = boss.data[0].getName();
        } catch (Exception ignored) {
        }
        if (!isSummonable(boss)) {
            JOptionPane.showMessageDialog(frame, "Boss \"" + name
                    + "\" không thể triệu hồi trực tiếp (loại boss con, được gọi bởi boss mẹ).");
            return;
        }
        if (boss.bossStatus != BossStatus.REST) {
            JOptionPane.showMessageDialog(frame, "Boss \"" + name + "\" đang ở trạng thái "
                    + boss.bossStatus + ", không thể triệu hồi.");
            return;
        }
        final String fname = name;
        runBg(() -> {
            try {
                boss.lastTimeRest = 0;
                boss.changeStatus(BossStatus.RESPAWN);
                utils.Logger.success("[PANEL] Đã triệu hồi boss \"" + fname + "\"\n");
                SwingUtilities.invokeLater(AdminPanel::refreshBossTable);
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi triệu hồi boss: " + ex.getMessage() + "\n");
            }
        });
    }

    /**
     * Boss chỉ tự spawn được khi appear type là DEFAULT_APPEAR.
     * Các loại khác (CALL_BY_ANOTHER, APPEAR_WITH_ANOTHER, ANOTHER_LEVEL) cần parentBoss
     * tự gọi nên joinMap() sẽ NPE nếu bị panel force RESPAWN.
     */
    private static boolean isSummonable(Boss b) {
        try {
            if (b == null || b.data == null || b.data.length == 0) {
                return false;
            }
            return b.data[0].getTypeAppear() == AppearType.DEFAULT_APPEAR;
        } catch (Exception e) {
            return false;
        }
    }

    private static void killSelectedBoss() {
        Boss boss = selectedBoss();
        if (boss == null) {
            JOptionPane.showMessageDialog(frame, "Hãy chọn một boss để hạ.");
            return;
        }
        String name = "?";
        try {
            name = boss.data[0].getName();
        } catch (Exception ignored) {
        }
        if (boss.bossStatus == BossStatus.REST) {
            JOptionPane.showMessageDialog(frame, "Boss \"" + name + "\" đang nghỉ, không cần hạ.");
            return;
        }
        if (!confirm("Hạ boss \"" + name + "\" ngay lập tức?")) {
            return;
        }
        final String fname = name;
        runBg(() -> {
            try {
                boss.nPoint.hp = 0;
                boss.changeStatus(BossStatus.DIE);
                utils.Logger.warning("[PANEL] Đã hạ boss \"" + fname + "\"\n");
                SwingUtilities.invokeLater(AdminPanel::refreshBossTable);
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi hạ boss: " + ex.getMessage() + "\n");
            }
        });
    }

    /** Triệu hồi tất cả boss đang REST (chỉ boss tự spawn được, bỏ qua boss con). */
    private static void callAllBosses() {
        List<Boss> bosses;
        try {
            bosses = new ArrayList<>(BossManager.gI().getBosses());
        } catch (Exception e) {
            return;
        }
        int restCount = 0;
        int skipChild = 0;
        for (Boss b : bosses) {
            if (b == null || b.bossStatus != BossStatus.REST) {
                continue;
            }
            if (isSummonable(b)) {
                restCount++;
            } else {
                skipChild++;
            }
        }
        if (restCount == 0) {
            JOptionPane.showMessageDialog(frame,
                    "Không có boss tự spawn nào đang nghỉ để triệu hồi."
                            + (skipChild > 0 ? "\n(" + skipChild + " boss con sẽ được boss mẹ tự gọi.)" : ""));
            return;
        }
        String msg = "Triệu hồi " + restCount + " boss đang nghỉ?";
        if (skipChild > 0) {
            msg += "\n(Bỏ qua " + skipChild + " boss con — chỉ spawn khi boss mẹ gọi.)";
        }
        if (!confirm(msg)) {
            return;
        }
        runBg(() -> {
            int n = 0;
            for (Boss b : bosses) {
                if (b == null || b.bossStatus != BossStatus.REST || !isSummonable(b)) {
                    continue;
                }
                try {
                    b.lastTimeRest = 0;
                    b.changeStatus(BossStatus.RESPAWN);
                    n++;
                } catch (Exception ignored) {
                }
            }
            utils.Logger.success("[PANEL] Đã triệu hồi " + n + " boss\n");
            SwingUtilities.invokeLater(AdminPanel::refreshBossTable);
        });
    }

    /**
     * Restart tất cả boss: hạ những boss đang active, đợi chu trình DIE→LEAVE_MAP→REST
     * hoàn tất, rồi triệu hồi lại toàn bộ. Toàn bộ chạy trên BG_POOL nên không khoá EDT.
     */
    private static void restartAllBosses() {
        List<Boss> bosses;
        try {
            bosses = new ArrayList<>(BossManager.gI().getBosses());
        } catch (Exception e) {
            return;
        }
        if (bosses.isEmpty()) {
            JOptionPane.showMessageDialog(frame, "Không có boss nào để restart.");
            return;
        }
        if (!confirm("Restart TẤT CẢ boss?\n"
                + "• Boss đang active sẽ bị hạ ngay\n"
                + "• Sau ~3 giây, toàn bộ boss được triệu hồi lại")) {
            return;
        }
        runBg(() -> {
            int killed = 0;
            // Pass 1: hạ các boss đang active (bao gồm cả boss con đang trong map)
            for (Boss b : bosses) {
                if (b == null || b.bossStatus == null || b.bossStatus == BossStatus.REST) {
                    continue;
                }
                try {
                    b.nPoint.hp = 0;
                    b.changeStatus(BossStatus.DIE);
                    killed++;
                } catch (Exception ignored) {
                }
            }
            SwingUtilities.invokeLater(AdminPanel::refreshBossTable);
            // Đợi để natural flow DIE→CHAT_E→LEAVE_MAP→REST hoàn tất
            try {
                Thread.sleep(3000);
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
            // Pass 2: triệu hồi boss đã về REST (chỉ những boss tự spawn được)
            int called = 0;
            List<Boss> fresh;
            try {
                fresh = new ArrayList<>(BossManager.gI().getBosses());
            } catch (Exception e) {
                fresh = bosses;
            }
            for (Boss b : fresh) {
                if (b == null || b.bossStatus != BossStatus.REST || !isSummonable(b)) {
                    continue;
                }
                try {
                    b.lastTimeRest = 0;
                    b.changeStatus(BossStatus.RESPAWN);
                    called++;
                } catch (Exception ignored) {
                }
            }
            utils.Logger.success("[PANEL] Restart boss: hạ " + killed + ", triệu hồi " + called + "\n");
            SwingUtilities.invokeLater(AdminPanel::refreshBossTable);
        });
    }

    /* ---------- Clear Sessions (dọn session rác) ---------- */
    private static volatile boolean autoClearSessions = false;
    private static Thread sessionCleanThread;
    private static final Object cleanLock = new Object();
    private static final long CLEAN_INTERVAL_MS = 60_000L;

    private static void setAutoClearSessions(boolean v) {
        synchronized (cleanLock) {
            autoClearSessions = v;
            if (v) {
                if (sessionCleanThread == null || !sessionCleanThread.isAlive()) {
                    Thread t = new Thread(() -> {
                        while (autoClearSessions) {
                            try {
                                Thread.sleep(CLEAN_INTERVAL_MS);
                            } catch (InterruptedException ie) {
                                Thread.currentThread().interrupt();
                                return;
                            }
                            if (autoClearSessions) {
                                clearSessionsNow();
                            }
                        }
                    }, "AdminPanel-Session-Cleaner");
                    t.setDaemon(true);
                    sessionCleanThread = t;
                    t.start();
                }
                utils.Logger.success("[PANEL] Tự xoá session rác: BẬT (mỗi " + (CLEAN_INTERVAL_MS / 1000) + "s)\n");
            } else {
                utils.Logger.warning("[PANEL] Tự xoá session rác: TẮT\n");
            }
        }
    }

    private static void clearSessionsNow() {
        runBg(() -> {
            try {
                java.util.List<ISession> sessions = SessionManager.gI().getSessions();
                for (int i = sessions.size() - 1; i >= 0; i--) {
                    ISession s;
                    try {
                        s = sessions.get(i);
                    } catch (Exception ex) {
                        continue;
                    }
                    if (s == null) {
                        sessions.remove(i);
                        continue;
                    }
                    if (!(s instanceof MySession m)) {
                        continue;
                    }
                    boolean isDead = !m.isConnected();
                    boolean isTrash = m.isConnected() && m.player == null && !m.joinedGame && m.timeWait <= 0;
                    if (!isDead && !isTrash) {
                        continue;
                    }
                    sessions.remove(i);
                    try {
                        Client.gI().kickSession(m);
                    } catch (Exception ignored) {
                    }
                }
            } catch (Exception ex) {
                utils.Logger.error("[PANEL] Lỗi xoá session: " + ex.getMessage() + "\n");
            }
        });
    }

    /* ---------- Tab Cấu hình ---------- */
    private static JPanel buildConfigPanel() {
        JPanel wrap = new JPanel();
        wrap.setBackground(BG);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel card = card();
        JPanel cardInner = new JPanel(new BorderLayout());
        cardInner.setOpaque(false);
        cardInner.setBorder(new EmptyBorder(18, 22, 20, 22));
        card.add(cardInner, BorderLayout.CENTER);

        JPanel head = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        head.setOpaque(false);
        JLabel ic = new JLabel("✦");
        ic.setForeground(C_YELLOW);
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        JLabel sec1 = new JLabel("Tỉ lệ EXP / Tiềm năng khi đánh quái");
        sec1.setFont(FONT_UI_BOLD);
        sec1.setForeground(FG);
        head.add(ic);
        head.add(sec1);

        JLabel sub = new JLabel("Hệ số sẽ nhân thẳng vào tiềm năng nhận được. Áp dụng tức thì.");
        sub.setFont(FONT_UI_SM);
        sub.setForeground(FG_DIM);
        sub.setBorder(new EmptyBorder(4, 22, 14, 0));

        JPanel headBox = new JPanel();
        headBox.setOpaque(false);
        headBox.setLayout(new BoxLayout(headBox, BoxLayout.Y_AXIS));
        head.setAlignmentX(Component.LEFT_ALIGNMENT);
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);
        headBox.add(head);
        headBox.add(sub);
        cardInner.add(headBox, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 10);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.NONE;

        gc.gridx = 0;
        gc.gridy = 0;
        JLabel l1 = new JLabel("Hệ số nhân EXP");
        l1.setForeground(FG_DIM);
        l1.setFont(FONT_UI);
        form.add(l1, gc);

        gc.gridx = 1;
        spnExpRate = new JSpinner(new SpinnerNumberModel((int) Manager.RATE_EXP_SERVER, 1, 1000, 1));
        styleSpinner(spnExpRate);
        ((JSpinner.DefaultEditor) spnExpRate.getEditor()).getTextField().setColumns(5);
        form.add(spnExpRate, gc);

        gc.gridx = 2;
        JButton btnApply = primaryButton("✓ Áp dụng");
        btnApply.addActionListener(e -> applyExpRate());
        form.add(btnApply, gc);

        gc.gridx = 3;
        gc.insets = new Insets(6, 24, 6, 6);
        JLabel l2 = new JLabel("Hiện tại:");
        l2.setForeground(FG_DIM);
        l2.setFont(FONT_UI);
        form.add(l2, gc);

        gc.gridx = 4;
        gc.insets = new Insets(6, 0, 6, 6);
        lblExpRateCurrent = new JLabel("x" + Manager.RATE_EXP_SERVER);
        lblExpRateCurrent.setForeground(ACCENT_2);
        lblExpRateCurrent.setFont(new Font("Segoe UI", Font.BOLD, 18));
        form.add(lblExpRateCurrent, gc);

        cardInner.add(form, BorderLayout.CENTER);

        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, card.getPreferredSize().height));
        card.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(card);
        wrap.add(Box.createVerticalStrut(14));

        // ===== Card 2: Dọn dẹp session rác =====
        JPanel card2 = card();
        JPanel inner2 = new JPanel(new BorderLayout());
        inner2.setOpaque(false);
        inner2.setBorder(new EmptyBorder(18, 22, 20, 22));
        card2.add(inner2, BorderLayout.CENTER);

        JPanel head2 = sectionTitle("🧹", "Dọn dẹp session rác",
                "Kick các session"
                        + (CLEAN_INTERVAL_MS / 1000) + " giây.");
        inner2.add(head2, BorderLayout.NORTH);

        JPanel form2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        form2.setOpaque(false);
        form2.setBorder(new EmptyBorder(12, 0, 0, 0));

        JCheckBox cbAuto = new JCheckBox("Tự động xoá mỗi " + (CLEAN_INTERVAL_MS / 1000) + "s", autoClearSessions);
        cbAuto.setOpaque(false);
        cbAuto.setForeground(FG);
        cbAuto.setFont(FONT_UI);
        cbAuto.setFocusPainted(false);
        cbAuto.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        cbAuto.addActionListener(e -> setAutoClearSessions(cbAuto.isSelected()));

        JButton btnClearNow = primaryButton("🧹 Xoá ngay");
        btnClearNow.addActionListener(e -> clearSessionsNow());

        form2.add(cbAuto);
        form2.add(btnClearNow);
        inner2.add(form2, BorderLayout.CENTER);

        card2.setMaximumSize(new Dimension(Integer.MAX_VALUE, card2.getPreferredSize().height));
        card2.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(card2);
        wrap.add(Box.createVerticalGlue());

        return wrap;
    }

    private static void applyExpRate() {
        int v = (int) spnExpRate.getValue();
        if (v < 1) {
            v = 1;
        }
        Manager.RATE_EXP_SERVER = v;
        lblExpRateCurrent.setText("x" + v);
        utils.Logger.success("[PANEL] Đã đặt RATE_EXP_SERVER = " + v + "\n");
    }

    /* ---------- Tab Sự kiện ---------- */
    private static JPanel buildEventPanel() {
        // Lần đầu vào tab: với event nào flag đang true, coi như đã init từ startup
        seedEventsRunningFromFlags();

        JPanel wrap = new JPanel();
        wrap.setBackground(BG);
        wrap.setLayout(new BoxLayout(wrap, BoxLayout.Y_AXIS));
        wrap.setBorder(new EmptyBorder(16, 18, 16, 18));

        // ===== Card 1: Sự kiện theo mùa =====
        JPanel card1 = card();
        JPanel inner1 = new JPanel(new BorderLayout());
        inner1.setOpaque(false);
        inner1.setBorder(new EmptyBorder(18, 22, 20, 22));
        card1.add(inner1, BorderLayout.CENTER);

        JPanel head1 = sectionTitle("🎉", "Sự kiện theo mùa",
                "Tick = chạy event ngay (spawn NPC/boss). Bỏ tick = tắt cho lần restart sau, NPC/boss đã spawn vẫn còn.");
        inner1.add(head1, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(0, 2, 14, 10));
        grid.setOpaque(false);
        grid.setBorder(new EmptyBorder(8, 0, 0, 0));

        grid.add(eventRow("🧧 Tết Nguyên Đán", "LUNNAR_NEW_YEAR",
                () -> EventManager.LUNNAR_NEW_YEAR,
                v -> EventManager.LUNNAR_NEW_YEAR = v,
                () -> new LunarNewYear().init()));
        grid.add(eventRow("🌷 Quốc tế Phụ nữ 8/3", "INTERNATIONAL_WOMANS_DAY",
                () -> EventManager.INTERNATIONAL_WOMANS_DAY,
                v -> EventManager.INTERNATIONAL_WOMANS_DAY = v,
                () -> new InternationalWomensDay().init()));
        grid.add(eventRow("🎃 Halloween", "HALLOWEEN",
                () -> EventManager.HALLOWEEN,
                EventManager::setHalloweenEnabled,
                () -> {}));
        grid.add(eventRow("🎄 Giáng Sinh", "CHRISTMAS",
                () -> EventManager.CHRISTMAS,
                v -> EventManager.CHRISTMAS = v,
                () -> new Christmas().init()));
        grid.add(eventRow("🏛 Giỗ Tổ Hùng Vương", "HUNG_VUONG",
                () -> EventManager.HUNG_VUONG,
                v -> EventManager.HUNG_VUONG = v,
                () -> new HungVuong().init()));
        grid.add(eventRow("🥮 Trung Thu", "TRUNG_THU",
                () -> EventManager.TRUNG_THU,
                v -> EventManager.TRUNG_THU = v,
                () -> new TrungThu().init()));
        grid.add(eventRow("💰 TopUp", "TOP_UP",
                () -> EventManager.TOP_UP,
                v -> EventManager.TOP_UP = v,
                () -> new TopUp().init()));

        inner1.add(grid, BorderLayout.CENTER);
        card1.setMaximumSize(new Dimension(Integer.MAX_VALUE, card1.getPreferredSize().height));
        card1.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(card1);
        wrap.add(Box.createVerticalStrut(14));

        // ===== Card 2: Sự kiện đang chạy (Manager.SU_KIEN) =====
        JPanel card2 = card();
        JPanel inner2 = new JPanel(new BorderLayout());
        inner2.setOpaque(false);
        inner2.setBorder(new EmptyBorder(18, 22, 20, 22));
        card2.add(inner2, BorderLayout.CENTER);

        JPanel head2 = sectionTitle("✦", "Mã sự kiện hiện tại",
                "Một số NPC/shop trong game đọc giá trị này để bật ưu đãi. Áp dụng tức thì.");
        inner2.add(head2, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        form.setBorder(new EmptyBorder(8, 0, 0, 0));
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, 6, 6, 10);
        gc.anchor = GridBagConstraints.WEST;

        gc.gridx = 0;
        gc.gridy = 0;
        JLabel l1 = new JLabel("SU_KIEN");
        l1.setForeground(FG_DIM);
        l1.setFont(FONT_UI);
        form.add(l1, gc);

        gc.gridx = 1;
        JSpinner spnSk = new JSpinner(new SpinnerNumberModel((int) Manager.SU_KIEN, 0, 127, 1));
        styleSpinner(spnSk);
        ((JSpinner.DefaultEditor) spnSk.getEditor()).getTextField().setColumns(4);
        form.add(spnSk, gc);

        gc.gridx = 2;
        JButton btnApply = primaryButton("✓ Áp dụng");
        JLabel lblCur = new JLabel(String.valueOf(Manager.SU_KIEN));
        btnApply.addActionListener(e -> {
            int v = (int) spnSk.getValue();
            Manager.SU_KIEN = (byte) v;
            lblCur.setText(String.valueOf(v));
            utils.Logger.success("[PANEL] Đã đặt SU_KIEN = " + v + "\n");
        });
        form.add(btnApply, gc);

        gc.gridx = 3;
        gc.insets = new Insets(6, 24, 6, 6);
        JLabel l2 = new JLabel("Hiện tại:");
        l2.setForeground(FG_DIM);
        l2.setFont(FONT_UI);
        form.add(l2, gc);

        gc.gridx = 4;
        gc.insets = new Insets(6, 0, 6, 6);
        lblCur.setForeground(ACCENT_2);
        lblCur.setFont(new Font("Segoe UI", Font.BOLD, 18));
        form.add(lblCur, gc);

        inner2.add(form, BorderLayout.CENTER);
        card2.setMaximumSize(new Dimension(Integer.MAX_VALUE, card2.getPreferredSize().height));
        card2.setAlignmentX(Component.LEFT_ALIGNMENT);
        wrap.add(card2);

        wrap.add(Box.createVerticalGlue());
        return wrap;
    }

    /** Section title block: icon + tiêu đề + subtitle. */
    private static JPanel sectionTitle(String icon, String title, String subtitle) {
        JPanel box = new JPanel();
        box.setOpaque(false);
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        row.setOpaque(false);
        JLabel ic = new JLabel(icon);
        ic.setForeground(C_YELLOW);
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 16));
        JLabel t = new JLabel(title);
        t.setForeground(FG);
        t.setFont(FONT_UI_BOLD);
        row.add(ic);
        row.add(t);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel sub = new JLabel(subtitle);
        sub.setForeground(FG_DIM);
        sub.setFont(FONT_UI_SM);
        sub.setBorder(new EmptyBorder(4, 26, 0, 0));
        sub.setAlignmentX(Component.LEFT_ALIGNMENT);

        box.add(row);
        box.add(sub);
        return box;
    }

    /**
     * Khi panel build lần đầu: flag nào đang true tức là đã được init từ startup,
     * nên đánh dấu running để badge hiển thị đúng.
     */
    private static void seedEventsRunningFromFlags() {
        if (EventManager.LUNNAR_NEW_YEAR) eventsRunning.add("LUNNAR_NEW_YEAR");
        if (EventManager.INTERNATIONAL_WOMANS_DAY) eventsRunning.add("INTERNATIONAL_WOMANS_DAY");
        if (EventManager.HALLOWEEN) eventsRunning.add("HALLOWEEN");
        if (EventManager.CHRISTMAS) eventsRunning.add("CHRISTMAS");
        if (EventManager.HUNG_VUONG) eventsRunning.add("HUNG_VUONG");
        if (EventManager.TRUNG_THU) eventsRunning.add("TRUNG_THU");
        if (EventManager.TOP_UP) eventsRunning.add("TOP_UP");
    }

    /**
     * Một dòng event: [checkbox label] [badge trạng thái].
     * Tick ON: ghi flag + nếu chưa chạy session này thì gọi starter.run() (spawn NPC/boss).
     * Tick OFF: ghi flag = false (NPC/boss đã spawn vẫn còn vì không có cách gỡ an toàn).
     */
    private static JPanel eventRow(String label, String key,
                                   java.util.function.BooleanSupplier getter,
                                   java.util.function.Consumer<Boolean> setter,
                                   Runnable starter) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);

        JCheckBox cb = new JCheckBox(label, getter.getAsBoolean());
        cb.setOpaque(false);
        cb.setForeground(FG);
        cb.setFont(FONT_UI);
        cb.setFocusPainted(false);
        cb.setBorder(new EmptyBorder(4, 4, 4, 4));
        cb.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel badge = new JLabel();
        badge.setFont(new Font("Segoe UI", Font.BOLD, 10));
        badge.setBorder(new EmptyBorder(2, 8, 2, 8));
        badge.setOpaque(true);
        updateEventBadge(badge, eventsRunning.contains(key));

        cb.addActionListener(e -> {
            boolean v = cb.isSelected();
            setter.accept(v);
            if (v) {
                if (eventsRunning.contains(key)) {
                    utils.Logger.primary("[PANEL] Event \"" + label + "\" đã đang chạy\n");
                    return;
                }
                cb.setEnabled(false);
                runBg(() -> {
                    try {
                        starter.run();
                        eventsRunning.add(key);
                        SwingUtilities.invokeLater(() -> {
                            updateEventBadge(badge, true);
                            cb.setEnabled(true);
                        });
                        utils.Logger.success("[PANEL] Đã bật event \"" + label + "\"\n");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                        SwingUtilities.invokeLater(() -> {
                            cb.setSelected(false);
                            cb.setEnabled(true);
                        });
                        utils.Logger.error("[PANEL] Lỗi bật event \"" + label + "\": "
                                + ex.getMessage() + "\n");
                    }
                });
            } else {
                eventsRunning.remove(key);
                updateEventBadge(badge, false);
                if ("HUNG_VUONG".equals(key)) {
                    managers.boss.HungVuongEventManager.gI().removeAllBossesFromMap();
                }
                utils.Logger.warning("[PANEL] Đã tắt event \"" + label + "\"\n");
            }
        });

        row.add(cb, BorderLayout.CENTER);
        row.add(badge, BorderLayout.EAST);
        return row;
    }

    private static void updateEventBadge(JLabel badge, boolean running) {
        if (running) {
            badge.setText("ĐANG CHẠY");
            badge.setBackground(new Color(ACCENT_2.getRed(), ACCENT_2.getGreen(), ACCENT_2.getBlue(), 48));
            badge.setForeground(ACCENT_2);
        } else {
            badge.setText("TẮT");
            badge.setBackground(BG_CHIP);
            badge.setForeground(FG_DIM);
        }
    }

    private static void refreshGiftcodeTable() {
        if (giftcodeModel == null) {
            return;
        }
        List<GiftCode> list;
        try {
            list = new ArrayList<>(GiftCodeManager.gI().listGiftCode);
        } catch (Exception e) {
            return;
        }
        long h = 1L;
        for (GiftCode g : list) {
            if (g == null) {
                continue;
            }
            h = h * 1315423911L ^ ((long) g.id << 32 | (g.countLeft & 0xFFFFFFFFL));
            h = h * 1315423911L ^ (g.playerName == null ? 0 : g.playerName.hashCode());
            h = h * 1315423911L ^ (g.code == null ? 0 : g.code.hashCode());
            h = h * 1315423911L ^ (g.allGender ? 1L : 0L);
            h = h * 1315423911L ^ (g.datecreate == null ? 0L : g.datecreate.getTime());
            h = h * 1315423911L ^ (g.dateexpired == null ? 0L : g.dateexpired.getTime());
            h = h * 1315423911L ^ (g.detail == null ? 0 : g.detail.hashCode());
        }
        if (list.size() == lastGiftSize && h == lastGiftHash) {
            return;
        }
        lastGiftSize = list.size();
        lastGiftHash = h;

        int keepId = -1;
        int viewRow = giftcodeTable.getSelectedRow();
        if (viewRow >= 0) {
            try {
                keepId = Integer.parseInt(String.valueOf(
                        giftcodeModel.getValueAt(giftcodeTable.convertRowIndexToModel(viewRow), 0)));
            } catch (Exception ignored) {
            }
        }
        lblGiftCount.setText(String.valueOf(list.size()));
        giftcodeModel.setRowCount(0);
        for (GiftCode g : list) {
            if (g == null) {
                continue;
            }
            String count = g.countLeft >= UNLIMITED ? "∞" : String.valueOf(g.countLeft);
            String gender = g.allGender ? "Tất cả" : "Theo giới tính";
            String pn = formatPlayerNames(g.playerName);
            giftcodeModel.addRow(new Object[]{
                    g.id, g.code, count, gender, pn,
                    formatTs(g.datecreate), formatTs(g.dateexpired), itemsSummary(g)});
        }
        if (keepId != -1) {
            for (int i = 0; i < giftcodeModel.getRowCount(); i++) {
                if (String.valueOf(giftcodeModel.getValueAt(i, 0)).equals(String.valueOf(keepId))) {
                    int vr = giftcodeTable.convertRowIndexToView(i);
                    giftcodeTable.setRowSelectionInterval(vr, vr);
                    break;
                }
            }
        }
    }

    private static GiftCode selectedGiftcode() {
        int row = giftcodeTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        int id;
        try {
            id = Integer.parseInt(String.valueOf(
                    giftcodeModel.getValueAt(giftcodeTable.convertRowIndexToModel(row), 0)));
        } catch (Exception e) {
            return null;
        }
        for (GiftCode g : new ArrayList<>(GiftCodeManager.gI().listGiftCode)) {
            if (g != null && g.id == id) {
                return g;
            }
        }
        return null;
    }

    private static void reloadGiftcodes() {
        runBg(() -> {
            try {
                GiftCodeAdmin.reload();
                SwingUtilities.invokeLater(AdminPanel::refreshGiftcodeTable);
                utils.Logger.success("[PANEL] Đã tải lại " + GiftCodeManager.gI().listGiftCode.size() + " giftcode\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi tải lại giftcode: " + ex.getMessage() + "\n");
            }
        });
    }

    /**
     * Reload giftcode từ DB không in log (dùng cho auto-sync định kỳ).
     * Chạy trên BG_POOL, refresh UI trên EDT. Bỏ qua nếu vừa reload gần đây.
     */
    private static void autoReloadGiftcodesFromDb() {
        if (!autoReloadGiftDb) {
            return;
        }
        long now = System.currentTimeMillis();
        if (now - lastGiftDbReloadAt < GIFT_DB_RELOAD_INTERVAL_MS) {
            return;
        }
        lastGiftDbReloadAt = now;
        runBg(() -> {
            try {
                GiftCodeAdmin.reload();
                SwingUtilities.invokeLater(AdminPanel::refreshGiftcodeTable);
            } catch (Exception ex) {
                utils.Logger.error("[PANEL] Auto-sync giftcode lỗi: " + ex.getMessage() + "\n");
            }
        });
    }

    private static void editSelectedGiftcode() {
        GiftCode g = selectedGiftcode();
        if (g == null) {
            JOptionPane.showMessageDialog(frame, "Hãy chọn một giftcode để sửa.");
            return;
        }
        final int id = g.id;
        runBg(() -> {
            List<GiftCodeAdmin.DetailRow> rows;
            try {
                rows = GiftCodeAdmin.parseDetailJson(GiftCodeAdmin.fetchDetailJson(id));
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Không đọc được chi tiết giftcode, mở với danh sách trống\n");
                rows = new ArrayList<>();
            }
            final List<GiftCodeAdmin.DetailRow> fr = rows;
            SwingUtilities.invokeLater(() -> openGiftcodeEditor(g, fr));
        });
    }

    private static void deleteSelectedGiftcode() {
        GiftCode g = selectedGiftcode();
        if (g == null) {
            JOptionPane.showMessageDialog(frame, "Hãy chọn một giftcode để xoá.");
            return;
        }
        if (!confirm("Xoá giftcode \"" + g.code + "\" (ID " + g.id + ")?")) {
            return;
        }
        final int id = g.id;
        runBg(() -> {
            try {
                GiftCodeAdmin.delete(id);
                GiftCodeAdmin.reload();
                SwingUtilities.invokeLater(AdminPanel::refreshGiftcodeTable);
                utils.Logger.warning("[PANEL] Đã xoá giftcode ID " + id + "\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi xoá giftcode: " + ex.getMessage() + "\n");
            }
        });
    }

    /* ---------- Dialog thêm/sửa giftcode ---------- */
    private static void openGiftcodeEditor(GiftCode existing, List<GiftCodeAdmin.DetailRow> initialRows) {
        final boolean isEdit = existing != null;
        JDialog dlg = new JDialog(frame, isEdit ? "Sửa giftcode #" + existing.id : "Thêm giftcode mới", true);
        dlg.setSize(780, 580);
        dlg.setLocationRelativeTo(frame);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 14, 12, 14));
        dlg.setContentPane(root);

        // ----- Form trên -----
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtCode = new JTextField(18);
        JSpinner spnCount = new JSpinner(new SpinnerNumberModel(100, 0, Integer.MAX_VALUE, 1));
        JCheckBox chkUnlimited = new JCheckBox("Vô hạn");
        JCheckBox chkAllGender = new JCheckBox("Tất cả giới tính", true);
        JTextField txtCreate = new JTextField(16);
        JTextField txtExpire = new JTextField(16);
        JTextField txtPlayerName = new JTextField(18);

        long now = System.currentTimeMillis();
        txtCreate.setText(TS_FMT.format(new Date(now)));
        txtExpire.setText(TS_FMT.format(new Date(now + 30L * 24 * 3600 * 1000)));

        if (isEdit) {
            txtCode.setText(existing.code);
            if (existing.countLeft >= UNLIMITED) {
                chkUnlimited.setSelected(true);
                spnCount.setEnabled(false);
            } else {
                spnCount.setValue(existing.countLeft);
            }
            chkAllGender.setSelected(existing.allGender);
            if (existing.datecreate != null) {
                txtCreate.setText(TS_FMT.format(existing.datecreate));
            }
            if (existing.dateexpired != null) {
                txtExpire.setText(TS_FMT.format(existing.dateexpired));
            }
            if (existing.playerName != null) {
                txtPlayerName.setText(existing.playerName);
            }
        }
        chkUnlimited.addActionListener(e -> spnCount.setEnabled(!chkUnlimited.isSelected()));

        int r = 0;
        gc.gridx = 0;
        gc.gridy = r;
        form.add(new JLabel("Code:"), gc);
        gc.gridx = 1;
        gc.gridwidth = 3;
        form.add(txtCode, gc);
        gc.gridwidth = 1;
        r++;
        gc.gridx = 0;
        gc.gridy = r;
        form.add(new JLabel("Số lượng:"), gc);
        gc.gridx = 1;
        form.add(spnCount, gc);
        gc.gridx = 2;
        form.add(chkUnlimited, gc);
        gc.gridx = 3;
        form.add(chkAllGender, gc);
        r++;
        gc.gridx = 0;
        gc.gridy = r;
        form.add(new JLabel("Ngày tạo:"), gc);
        gc.gridx = 1;
        form.add(txtCreate, gc);
        gc.gridx = 2;
        form.add(new JLabel("Hết hạn:"), gc);
        gc.gridx = 3;
        form.add(txtExpire, gc);
        r++;
        gc.gridx = 0;
        gc.gridy = r;
        form.add(new JLabel("Tên player:"), gc);
        gc.gridx = 1;
        gc.gridwidth = 3;
        form.add(txtPlayerName, gc);
        gc.gridwidth = 1;
        r++;
        gc.gridx = 1;
        gc.gridy = r;
        gc.gridwidth = 3;
        JLabel hintPn = new JLabel("<html>(để trống = ai cũng nhập được. "
                + "Nhập <b>nhiều tên</b> cách nhau bằng dấu phẩy: <i>vd: nick1, nick2, nick3</i>)</html>");
        hintPn.setForeground(FG_DIM);
        hintPn.setFont(FONT_UI_SM);
        form.add(hintPn, gc);
        gc.gridwidth = 1;
        root.add(form, BorderLayout.NORTH);

        // ----- Bảng vật phẩm -----
        JPanel itemsWrap = new JPanel(new BorderLayout(0, 6));
        JLabel itemsLbl = new JLabel("Vật phẩm trong giftcode");
        itemsLbl.setFont(FONT_UI_BOLD);
        itemsWrap.add(itemsLbl, BorderLayout.NORTH);

        DefaultTableModel itemModel = new DefaultTableModel(
                new Object[]{"ID", "Tên vật phẩm", "Số lượng", "Options (id:param,...)", "Mô tả option"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 0 || c == 2 || c == 3; // cột Tên & Mô tả chỉ đọc, tự điền
            }
        };
        for (GiftCodeAdmin.DetailRow dr : initialRows) {
            String opts = optionsToString(dr.options);
            itemModel.addRow(new Object[]{dr.itemId, itemName(dr.itemId), dr.quantity, opts, optionDesc(opts)});
        }
        // Tự điền Tên / Mô tả khi sửa ID hoặc Options
        itemModel.addTableModelListener(ev -> {
            if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) {
                return;
            }
            int row = ev.getFirstRow();
            int col = ev.getColumn();
            if (row < 0 || row >= itemModel.getRowCount()) {
                return;
            }
            if (col == 0) {
                itemModel.setValueAt(itemName(parseIntSafe(itemModel.getValueAt(row, 0))), row, 1);
            } else if (col == 3) {
                Object o = itemModel.getValueAt(row, 3);
                itemModel.setValueAt(optionDesc(o == null ? "" : o.toString()), row, 4);
            }
        });
        JTable itemTable = new JTable(itemModel);
        itemTable.setRowHeight(24);
        int[] iw = {44, 150, 70, 185, 185};
        for (int i = 0; i < iw.length; i++) {
            itemTable.getColumnModel().getColumn(i).setPreferredWidth(iw[i]);
        }
        itemsWrap.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        JPanel itemBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnAddRow = smallButton("Thêm dòng");
        btnAddRow.addActionListener(e -> itemModel.addRow(new Object[]{0, itemName(0), 1, "", ""}));
        JButton btnDelRow = smallButton("Xoá dòng");
        btnDelRow.addActionListener(e -> {
            int sr = itemTable.getSelectedRow();
            if (sr >= 0) {
                if (itemTable.isEditing()) {
                    itemTable.getCellEditor().stopCellEditing();
                }
                itemModel.removeRow(itemTable.convertRowIndexToModel(sr));
            }
        });
        itemBtns.add(btnAddRow);
        itemBtns.add(btnDelRow);
        itemsWrap.add(itemBtns, BorderLayout.SOUTH);
        root.add(itemsWrap, BorderLayout.CENTER);

        // ----- Nút Lưu/Huỷ -----
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancel = smallButton("Huỷ");
        btnCancel.addActionListener(e -> dlg.dispose());
        JButton btnSave = primaryButton(isEdit ? "Lưu thay đổi" : "Tạo giftcode");
        btnSave.addActionListener(e -> {
            if (itemTable.isEditing()) {
                itemTable.getCellEditor().stopCellEditing();
            }
            saveGiftcode(dlg, existing, txtCode, spnCount, chkUnlimited, chkAllGender,
                    txtCreate, txtExpire, txtPlayerName, itemModel, btnSave);
        });
        south.add(btnCancel);
        south.add(btnSave);
        root.add(south, BorderLayout.SOUTH);

        dlg.setVisible(true);
    }

    private static void saveGiftcode(JDialog dlg, GiftCode existing, JTextField txtCode, JSpinner spnCount,
                                     JCheckBox chkUnlimited, JCheckBox chkAllGender, JTextField txtCreate,
                                     JTextField txtExpire, JTextField txtPlayerName,
                                     DefaultTableModel itemModel, JButton btnSave) {
        String code = txtCode.getText().trim();
        if (code.isEmpty()) {
            JOptionPane.showMessageDialog(dlg, "Code không được để trống.");
            return;
        }
        final Timestamp tsCreate, tsExpire;
        try {
            tsCreate = Timestamp.valueOf(txtCreate.getText().trim());
            tsExpire = Timestamp.valueOf(txtExpire.getText().trim());
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(dlg, "Sai định dạng ngày. Dùng: yyyy-MM-dd HH:mm:ss");
            return;
        }
        final List<GiftCodeAdmin.DetailRow> rows = new ArrayList<>();
        for (int i = 0; i < itemModel.getRowCount(); i++) {
            String idStr = String.valueOf(itemModel.getValueAt(i, 0)).trim();
            String qtyStr = String.valueOf(itemModel.getValueAt(i, 2)).trim();
            String optStr = itemModel.getValueAt(i, 3) == null ? "" : String.valueOf(itemModel.getValueAt(i, 3)).trim();
            if (idStr.isEmpty() && qtyStr.isEmpty()) {
                continue;
            }
            GiftCodeAdmin.DetailRow dr = new GiftCodeAdmin.DetailRow();
            try {
                dr.itemId = Integer.parseInt(idStr);
                dr.quantity = Integer.parseInt(qtyStr);
                dr.options.addAll(parseOptions(optStr));
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Dòng vật phẩm " + (i + 1) + " sai định dạng: " + ex.getMessage());
                return;
            }
            rows.add(dr);
        }

        final int countLeft = chkUnlimited.isSelected() ? -1 : (int) spnCount.getValue();
        final boolean allGender = chkAllGender.isSelected();
        final String detailJson = GiftCodeAdmin.buildDetailJson(rows);
        final String playerName = txtPlayerName.getText().trim();
        btnSave.setEnabled(false);
        runBg(() -> {
            try {
                if (existing == null) {
                    GiftCodeAdmin.insert(code, countLeft, allGender, tsCreate, tsExpire, detailJson, playerName);
                } else {
                    GiftCodeAdmin.update(existing.id, code, countLeft, allGender, tsCreate, tsExpire, detailJson, playerName);
                }
                GiftCodeAdmin.reload();
                SwingUtilities.invokeLater(() -> {
                    refreshGiftcodeTable();
                    dlg.dispose();
                });
                utils.Logger.success("[PANEL] Đã lưu giftcode \"" + code + "\"\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    btnSave.setEnabled(true);
                    JOptionPane.showMessageDialog(dlg, "Lỗi lưu giftcode: " + ex.getMessage());
                });
                utils.Logger.error("[PANEL] Lỗi lưu giftcode: " + ex.getMessage() + "\n");
            }
        });
    }

    /* ---------- Dialog tạo giftcode share (50 mã, 1 lượt nhập/mã) ---------- */
    private static void openGiftcodeShareDialog() {
        JDialog dlg = new JDialog(frame, "Tạo hàng loạt Giftcode Share", true);
        dlg.setSize(800, 600);
        dlg.setLocationRelativeTo(frame);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 14, 12, 14));
        dlg.setContentPane(root);

        // ----- Form trên -----
        JPanel form = new JPanel(new GridBagLayout());
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(4, 4, 4, 4);
        gc.anchor = GridBagConstraints.WEST;
        gc.fill = GridBagConstraints.HORIZONTAL;

        JTextField txtPrefix = new JTextField("SHARE_", 12);
        JSpinner spnTotal = new JSpinner(new SpinnerNumberModel(50, 1, 1000, 1));
        JTextField txtCount = new JTextField("1 lượt / mã (Share)", 14);
        txtCount.setEditable(false);
        txtCount.setEnabled(false);

        JCheckBox chkAllGender = new JCheckBox("Tất cả giới tính", true);
        JTextField txtCreate = new JTextField(16);
        JTextField txtExpire = new JTextField(16);
        JTextField txtPlayerName = new JTextField(18);

        long now = System.currentTimeMillis();
        txtCreate.setText(TS_FMT.format(new Date(now)));
        txtExpire.setText(TS_FMT.format(new Date(now + 30L * 24 * 3600 * 1000)));

        int r = 0;
        gc.gridx = 0; gc.gridy = r;
        form.add(new JLabel("Tiền tố (Prefix):"), gc);
        gc.gridx = 1;
        form.add(txtPrefix, gc);
        gc.gridx = 2;
        form.add(new JLabel("Số lượng mã:"), gc);
        gc.gridx = 3;
        form.add(spnTotal, gc);
        r++;

        gc.gridx = 0; gc.gridy = r;
        form.add(new JLabel("Lượt nhập:"), gc);
        gc.gridx = 1;
        form.add(txtCount, gc);
        gc.gridx = 2; gc.gridwidth = 2;
        form.add(chkAllGender, gc);
        gc.gridwidth = 1;
        r++;

        gc.gridx = 0; gc.gridy = r;
        form.add(new JLabel("Ngày tạo:"), gc);
        gc.gridx = 1;
        form.add(txtCreate, gc);
        gc.gridx = 2;
        form.add(new JLabel("Hết hạn:"), gc);
        gc.gridx = 3;
        form.add(txtExpire, gc);
        r++;

        gc.gridx = 0; gc.gridy = r;
        form.add(new JLabel("Tên player:"), gc);
        gc.gridx = 1; gc.gridwidth = 3;
        form.add(txtPlayerName, gc);
        gc.gridwidth = 1;
        r++;

        gc.gridx = 1; gc.gridy = r; gc.gridwidth = 3;
        JLabel hint = new JLabel("<html>Mã sinh ra dạng: PREFIX + 8 ký tự ngẫu nhiên (VD: SHARE_8F2K9M1P). Mỗi mã dùng đúng 1 lần.</html>");
        hint.setForeground(FG_DIM);
        hint.setFont(FONT_UI_SM);
        form.add(hint, gc);
        gc.gridwidth = 1;

        root.add(form, BorderLayout.NORTH);

        // ----- Bảng vật phẩm -----
        JPanel itemsWrap = new JPanel(new BorderLayout(0, 6));
        JLabel itemsLbl = new JLabel("Vật phẩm quà tặng cho toàn bộ mã share");
        itemsLbl.setFont(FONT_UI_BOLD);
        itemsWrap.add(itemsLbl, BorderLayout.NORTH);

        DefaultTableModel itemModel = new DefaultTableModel(
                new Object[]{"ID", "Tên vật phẩm", "Số lượng", "Options (id:param,...)", "Mô tả option"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 0 || c == 2 || c == 3;
            }
        };

        // Nếu ngoài bảng đang chọn 1 giftcode, tự nạp item mẫu
        GiftCode sel = selectedGiftcode();
        if (sel != null) {
            try {
                List<GiftCodeAdmin.DetailRow> rows = GiftCodeAdmin.parseDetailJson(GiftCodeAdmin.fetchDetailJson(sel.id));
                for (GiftCodeAdmin.DetailRow dr : rows) {
                    String opts = optionsToString(dr.options);
                    itemModel.addRow(new Object[]{dr.itemId, itemName(dr.itemId), dr.quantity, opts, optionDesc(opts)});
                }
            } catch (Exception ignored) {}
        }

        itemModel.addTableModelListener(ev -> {
            if (ev.getType() != javax.swing.event.TableModelEvent.UPDATE) return;
            int row = ev.getFirstRow();
            int col = ev.getColumn();
            if (row < 0 || row >= itemModel.getRowCount()) return;
            if (col == 0) {
                itemModel.setValueAt(itemName(parseIntSafe(itemModel.getValueAt(row, 0))), row, 1);
            } else if (col == 3) {
                Object o = itemModel.getValueAt(row, 3);
                itemModel.setValueAt(optionDesc(o == null ? "" : o.toString()), row, 4);
            }
        });

        JTable itemTable = new JTable(itemModel);
        itemTable.setRowHeight(24);
        int[] iw = {44, 150, 70, 185, 185};
        for (int i = 0; i < iw.length; i++) {
            itemTable.getColumnModel().getColumn(i).setPreferredWidth(iw[i]);
        }
        itemsWrap.add(new JScrollPane(itemTable), BorderLayout.CENTER);

        JPanel itemBtns = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JButton btnAddRow = smallButton("Thêm dòng");
        btnAddRow.addActionListener(e -> itemModel.addRow(new Object[]{0, itemName(0), 1, "", ""}));
        JButton btnDelRow = smallButton("Xoá dòng");
        btnDelRow.addActionListener(e -> {
            int sr = itemTable.getSelectedRow();
            if (sr >= 0) {
                if (itemTable.isEditing()) itemTable.getCellEditor().stopCellEditing();
                itemModel.removeRow(itemTable.convertRowIndexToModel(sr));
            }
        });
        itemBtns.add(btnAddRow);
        itemBtns.add(btnDelRow);
        itemsWrap.add(itemBtns, BorderLayout.SOUTH);
        root.add(itemsWrap, BorderLayout.CENTER);

        // ----- Nút Lưu/Huỷ -----
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCancel = smallButton("Huỷ");
        btnCancel.addActionListener(e -> dlg.dispose());
        JButton btnSave = successButton("🎁 Tạo & Lưu 50 Mã Share");
        spnTotal.addChangeListener(e -> {
            btnSave.setText("🎁 Tạo & Lưu " + spnTotal.getValue() + " Mã Share");
        });

        btnSave.addActionListener(e -> {
            if (itemTable.isEditing()) itemTable.getCellEditor().stopCellEditing();
            int total = (Integer) spnTotal.getValue();
            if (total <= 0 || total > 1000) {
                JOptionPane.showMessageDialog(dlg, "Số lượng mã cần tạo phải từ 1 đến 1000.");
                return;
            }
            String prefix = txtPrefix.getText().trim();
            if (prefix.isEmpty()) prefix = "SHARE_";
            if (!prefix.matches("^[a-zA-Z0-9_-]+$")) {
                JOptionPane.showMessageDialog(dlg, "Tiền tố chỉ được chứa chữ cái, số, gạch dưới hoặc gạch ngang.");
                return;
            }
            final Timestamp tsCreate, tsExpire;
            try {
                tsCreate = Timestamp.valueOf(txtCreate.getText().trim());
                tsExpire = Timestamp.valueOf(txtExpire.getText().trim());
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(dlg, "Sai định dạng ngày. Dùng: yyyy-MM-dd HH:mm:ss");
                return;
            }

            final List<GiftCodeAdmin.DetailRow> rows = new ArrayList<>();
            for (int i = 0; i < itemModel.getRowCount(); i++) {
                String idStr = String.valueOf(itemModel.getValueAt(i, 0)).trim();
                String qtyStr = String.valueOf(itemModel.getValueAt(i, 2)).trim();
                String optStr = itemModel.getValueAt(i, 3) == null ? "" : String.valueOf(itemModel.getValueAt(i, 3)).trim();
                if (idStr.isEmpty() && qtyStr.isEmpty()) continue;
                GiftCodeAdmin.DetailRow dr = new GiftCodeAdmin.DetailRow();
                try {
                    dr.itemId = Integer.parseInt(idStr);
                    dr.quantity = Integer.parseInt(qtyStr);
                    dr.options.addAll(parseOptions(optStr));
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(dlg, "Dòng vật phẩm " + (i + 1) + " sai định dạng: " + ex.getMessage());
                    return;
                }
                rows.add(dr);
            }

            final String detailJson = GiftCodeAdmin.buildDetailJson(rows);
            final boolean allGender = chkAllGender.isSelected();
            final String playerName = txtPlayerName.getText().trim();
            final String cleanPrefix = prefix.toUpperCase();

            btnSave.setEnabled(false);
            btnSave.setText("Đang tạo " + total + " mã...");

            runBg(() -> {
                try {
                    final String CHARS = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ";
                    java.security.SecureRandom rng = new java.security.SecureRandom();
                    java.util.Set<String> generatedCodes = new java.util.LinkedHashSet<>();
                    while (generatedCodes.size() < total) {
                        StringBuilder sb = new StringBuilder(cleanPrefix);
                        for (int i = 0; i < 8; i++) {
                            sb.append(CHARS.charAt(rng.nextInt(CHARS.length())));
                        }
                        generatedCodes.add(sb.toString());
                    }
                    List<String> codeList = new ArrayList<>(generatedCodes);
                    GiftCodeAdmin.insertBatchShare(codeList, 1, allGender, tsCreate, tsExpire, detailJson, playerName);

                    SwingUtilities.invokeLater(() -> {
                        dlg.dispose();
                        refreshGiftcodeTable();
                        showAdminShareCodesResultDialog(codeList);
                    });
                } catch (Exception ex) {
                    ex.printStackTrace();
                    SwingUtilities.invokeLater(() -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("🎁 Tạo & Lưu " + total + " Mã Share");
                        JOptionPane.showMessageDialog(dlg, "Lỗi tạo giftcode share: " + ex.getMessage());
                    });
                }
            });
        });

        south.add(btnCancel);
        south.add(btnSave);
        root.add(south, BorderLayout.SOUTH);
        dlg.setVisible(true);
    }

    private static void showAdminShareCodesResultDialog(List<String> codes) {
        JDialog d = new JDialog(frame, "Đã tạo thành công " + codes.size() + " Giftcode Share", true);
        d.setSize(600, 520);
        d.setLocationRelativeTo(frame);
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(new EmptyBorder(12, 14, 12, 14));
        d.setContentPane(root);

        JLabel lbl = new JLabel("<html><b>Đã tạo thành công " + codes.size() + " mã Giftcode Share (mỗi mã 1 lượt nhập)!</b><br>"
                + "Bạn có thể sao chép hoặc xuất ra file để chia sẻ cho người chơi:</html>");
        lbl.setFont(FONT_UI);
        root.add(lbl, BorderLayout.NORTH);

        String fullText = String.join("\n", codes);
        JTextArea txt = new JTextArea(fullText);
        txt.setFont(FONT_MONO);
        txt.setEditable(false);
        txt.setBackground(BG_INPUT);
        txt.setForeground(FG);
        txt.setCaretPosition(0);
        txt.setBorder(new EmptyBorder(8, 8, 8, 8));

        JScrollPane sp = new JScrollPane(txt);
        styleScrollPane(sp);
        root.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        JButton btnCopy = primaryButton("📋 Sao chép tất cả (" + codes.size() + " mã)");
        btnCopy.addActionListener(e -> {
            try {
                java.awt.datatransfer.StringSelection sel = new java.awt.datatransfer.StringSelection(fullText);
                java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, null);
                JOptionPane.showMessageDialog(d, "Đã sao chép " + codes.size() + " mã vào Clipboard!");
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(d, "Lỗi sao chép: " + ex.getMessage());
            }
        });

        JButton btnExport = smallButton("💾 Xuất file .txt");
        btnExport.addActionListener(e -> {
            JFileChooser fc = new JFileChooser();
            fc.setSelectedFile(new File("giftcode_share_" + codes.size() + ".txt"));
            if (fc.showSaveDialog(d) == JFileChooser.APPROVE_OPTION) {
                File f = fc.getSelectedFile();
                try (java.io.PrintWriter pw = new java.io.PrintWriter(new java.io.FileWriter(f))) {
                    for (String c : codes) pw.println(c);
                    JOptionPane.showMessageDialog(d, "Đã xuất ra file: " + f.getAbsolutePath());
                } catch (Exception ex) {
                    JOptionPane.showMessageDialog(d, "Lỗi ghi file: " + ex.getMessage());
                }
            }
        });

        JButton btnClose = smallButton("Đóng");
        btnClose.addActionListener(e -> d.dispose());

        south.add(btnExport);
        south.add(btnCopy);
        south.add(btnClose);
        root.add(south, BorderLayout.SOUTH);
        d.setVisible(true);
    }

    private static List<int[]> parseOptions(String s) {
        List<int[]> out = new ArrayList<>();
        if (s == null || s.trim().isEmpty()) {
            return out;
        }
        for (String part : s.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] kv = part.split(":");
            if (kv.length != 2) {
                throw new IllegalArgumentException("options phải dạng id:param");
            }
            out.add(new int[]{Integer.parseInt(kv[0].trim()), Integer.parseInt(kv[1].trim())});
        }
        return out;
    }

    private static String optionsToString(List<int[]> options) {
        StringBuilder sb = new StringBuilder();
        for (int[] op : options) {
            if (sb.length() > 0) {
                sb.append(",");
            }
            sb.append(op[0]).append(":").append(op[1]);
        }
        return sb.toString();
    }

    private static String formatTs(Timestamp ts) {
        return ts == null ? "" : TS_FMT.format(ts);
    }

    /** Hiển thị tên player ở bảng giftcode: "—" nếu trống, "name1, name2 (+N)" nếu nhiều. */
    private static String formatPlayerNames(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return "— (mọi người)";
        }
        String[] parts = raw.split(",");
        int n = 0;
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            String s = p.trim();
            if (s.isEmpty()) {
                continue;
            }
            if (n == 0) {
                sb.append(s);
            } else if (n == 1) {
                sb.append(", ").append(s);
            }
            n++;
        }
        if (n > 2) {
            sb.append(" (+").append(n - 2).append(")");
        }
        return sb.toString();
    }

    /* ---------- Tra tên vật phẩm / option ---------- */
    private static String itemName(int id) {
        switch (id) {
            case -1:
                return "Vàng";
            case -2:
                return "Ngọc";
            case -3:
                return "Ngọc khoá";
            default:
                try {
                    var t = ItemService.gI().getTemplate(id);
                    return t != null && t.name != null ? t.name : "";
                } catch (Exception e) {
                    return "";
                }
        }
    }

    private static String optionName(int id) {
        try {
            var t = ItemService.gI().getItemOptionTemplate(id);
            return t != null && t.name != null ? t.name : "";
        } catch (Exception e) {
            return "";
        }
    }

    /** Chuyển "id:param,id:param" thành mô tả người đọc được, vd "Sức đánh +1%, HP +5%". */
    private static String optionDesc(String optionsStr) {
        if (optionsStr == null || optionsStr.trim().isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : optionsStr.split(",")) {
            part = part.trim();
            if (part.isEmpty()) {
                continue;
            }
            String[] kv = part.split(":");
            if (kv.length != 2) {
                continue;
            }
            try {
                int oid = Integer.parseInt(kv[0].trim());
                int param = Integer.parseInt(kv[1].trim());
                String name = optionName(oid);
                String disp = name.isEmpty() ? (oid + ":" + param) : name.replace("#", String.valueOf(param));
                if (sb.length() > 0) {
                    sb.append(", ");
                }
                sb.append(disp);
            } catch (NumberFormatException ignored) {
            }
        }
        return sb.toString();
    }

    /** Tóm tắt tên các vật phẩm của 1 giftcode để hiện ở bảng danh sách. */
    private static String itemsSummary(GiftCode g) {
        StringBuilder sb = new StringBuilder();
        for (java.util.Map.Entry<Integer, Integer> en : g.detail.entrySet()) {
            String nm = itemName(en.getKey());
            if (nm.isEmpty()) {
                nm = "#" + en.getKey();
            }
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(nm);
            if (sb.length() > 55) {
                sb.append("…");
                break;
            }
        }
        return sb.toString();
    }

    private static int parseIntSafe(Object o) {
        try {
            return Integer.parseInt(String.valueOf(o).trim());
        } catch (Exception e) {
            return 0;
        }
    }

    /* ---------- Thanh điều khiển ---------- */
    private static JPanel buildControlBar() {
        JPanel bar = new JPanel();
        bar.setLayout(new BoxLayout(bar, BoxLayout.Y_AXIS));
        bar.setBackground(BG_PANEL);
        bar.setBorder(BorderFactory.createCompoundBorder(
                new MatteTopBorder(BORDER),
                new EmptyBorder(12, 16, 12, 16)));

        // Hàng 1: thông báo toàn server
        JPanel row1 = new JPanel(new BorderLayout(8, 0));
        row1.setOpaque(false);
        JLabel bcLbl = new JLabel("Thông báo");
        bcLbl.setForeground(FG_DIM);
        bcLbl.setFont(FONT_UI);
        bcLbl.setBorder(new EmptyBorder(0, 4, 0, 10));
        txtBroadcast = new JTextField();
        styleTextField(txtBroadcast);
        txtBroadcast.addActionListener(e -> doBroadcast());
        JButton btnBc = primaryButton("➤ Gửi tất cả");
        btnBc.addActionListener(e -> doBroadcast());
        row1.add(bcLbl, BorderLayout.WEST);
        row1.add(txtBroadcast, BorderLayout.CENTER);
        JPanel sendWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        sendWrap.setOpaque(false);
        sendWrap.add(Box.createHorizontalStrut(8));
        sendWrap.add(btnBc);
        row1.add(sendWrap, BorderLayout.EAST);
        bar.add(row1);
        bar.add(Box.createVerticalStrut(10));

        // Hàng 2: bảo trì + lưu + kick
        JPanel row2 = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        row2.setOpaque(false);

        JLabel mLbl = new JLabel("Bảo trì sau");
        mLbl.setForeground(FG_DIM);
        mLbl.setFont(FONT_UI);
        spnMaint = new JSpinner(new SpinnerNumberModel(5, 0, 1440, 1));
        styleSpinner(spnMaint);
        ((JSpinner.DefaultEditor) spnMaint.getEditor()).getTextField().setColumns(3);
        JLabel mLbl2 = new JLabel("phút");
        mLbl2.setForeground(FG_DIM);
        mLbl2.setFont(FONT_UI);
        JButton btnMaint = primaryButton("⏱ Hẹn bảo trì");
        btnMaint.addActionListener(e -> doMaintenance());
        JButton btnMaintNow = dangerButton("⚠ Bảo trì ngay");
        btnMaintNow.addActionListener(e -> doMaintenanceNow());

        JButton btnSavePl = smallButton("💾 Lưu player");
        btnSavePl.addActionListener(e -> doSaveAllPlayers());
        JButton btnSaveClan = smallButton("💾 Lưu bang hội");
        btnSaveClan.addActionListener(e -> doSaveClans());
        JButton btnKickAll = dangerButton("✕ Kick all");
        btnKickAll.addActionListener(e -> doKickAll());

        row2.add(mLbl);
        row2.add(spnMaint);
        row2.add(mLbl2);
        row2.add(btnMaint);
        row2.add(btnMaintNow);
        row2.add(sep());
        row2.add(btnSavePl);
        row2.add(btnSaveClan);
        row2.add(btnKickAll);
        bar.add(row2);
        return bar;
    }

    /* ===================== Hành động ===================== */
    private static void doBroadcast() {
        String msg = txtBroadcast.getText().trim();
        if (msg.isEmpty()) {
            return;
        }
        runBg(() -> Service.gI().sendThongBaoAllPlayer(msg));
        utils.Logger.primary("[PANEL] Đã gửi thông báo: " + msg + "\n");
        txtBroadcast.setText("");
    }

    private static void doMaintenance() {
        int min = (int) spnMaint.getValue();
        if (!confirm("Hẹn bảo trì sau " + min + " phút? Người chơi sẽ được thông báo đếm ngược.")) {
            return;
        }
        Maintenance.gI().start(min);
    }

    private static void doMaintenanceNow() {
        if (!confirm("BẢO TRÌ NGAY LẬP TỨC?\nServer sẽ lưu toàn bộ dữ liệu, kick người chơi và tắt.")) {
            return;
        }
        runBg(() -> Maintenance.gI().startImmediately());
    }

    private static void doSaveAllPlayers() {
        runBg(() -> {
            try {
                Client.gI().cloneMySessionNotConnect();
                utils.Logger.warning("[PANEL] Đang lưu dữ liệu người chơi...\n");
                List<Player> snapshot = new ArrayList<>(Client.gI().getPlayers());
                int n = 0;
                for (Player pl : snapshot) {
                    try {
                        if (pl != null && pl.isPl()) {
                            PlayerDAO.updatePlayer(pl);
                            n++;
                            Thread.sleep(40);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                utils.Logger.success("[PANEL] Đã lưu " + n + " người chơi\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi lưu dữ liệu người chơi\n");
            }
        });
    }

    private static void doSaveClans() {
        runBg(() -> {
            try {
                ClanService.gI().close();
                utils.Logger.success("[PANEL] Đã lưu " + Manager.CLANS.size() + " bang hội\n");
            } catch (Exception ex) {
                ex.printStackTrace();
                utils.Logger.error("[PANEL] Lỗi lưu dữ liệu bang hội\n");
            }
        });
    }

    private static void doKickAll() {
        if (!confirm("Kick toàn bộ người chơi đang online? (Dữ liệu sẽ được lưu)")) {
            return;
        }
        runBg(() -> Client.gI().close());
    }

    private static void saveSelectedPlayer() {
        Player p = selectedPlayer();
        if (p == null) {
            return;
        }
        runBg(() -> {
            try {
                PlayerDAO.updatePlayer(p);
                utils.Logger.success("[PANEL] Đã lưu người chơi " + p.name + "\n");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private static void kickSelectedPlayer() {
        Player p = selectedPlayer();
        if (p == null) {
            return;
        }
        if (!confirm("Kick người chơi " + p.name + "?")) {
            return;
        }
        runBg(() -> {
            try {
                if (p.getSession() != null) {
                    Client.gI().kickSession(p.getSession());
                }
                utils.Logger.warning("[PANEL] Đã kick người chơi " + p.name + "\n");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });
    }

    private static Player selectedPlayer() {
        int row = playerTable.getSelectedRow();
        if (row < 0) {
            return null;
        }
        try {
            long id = Long.parseLong(String.valueOf(
                    playerModel.getValueAt(playerTable.convertRowIndexToModel(row), 0)));
            return Client.gI().getPlayer(id);
        } catch (Exception e) {
            return null;
        }
    }

    /** Refresh dữ liệu của tab hiện đang xem. Skip khi panel iconified/ẩn. */
    private static void refreshActiveTab() {
        if (!isFrameUserVisible()) {
            return;
        }
        switch (activeTabIdx) {
            case 0 -> refreshPlayers();
            case 1 -> {
                autoReloadGiftcodesFromDb();
                refreshGiftcodeTable();
            }
            case 2 -> refreshBossTable();
            default -> {
            }
        }
    }

    /** True khi panel đang user-visible (không ẩn, không iconified). */
    private static boolean isFrameUserVisible() {
        JFrame f = frame;
        if (f == null || !f.isShowing()) {
            return false;
        }
        return (f.getExtendedState() & Frame.ICONIFIED) == 0;
    }

    /* ===================== Cập nhật dữ liệu ===================== */
    /** Update các chip thống kê. Phần đắt (threads/heap) giảm tần suất xuống tick%3. */
    private static void updateStats() {
        if (!isFrameUserVisible()) {
            return;
        }
        try {
            boolean maint = Maintenance.isRunning;
            String statusText = maint ? "ĐANG BẢO TRÌ" : "ĐANG CHẠY";
            if (!statusText.equals(lastStatusText)) {
                lblStatus.setText(statusText);
                lblStatus.setForeground(maint ? C_YELLOW : ACCENT_2);
                lastStatusText = statusText;
            }

            int players = 0;
            try {
                players = Client.gI().getPlayers().size();
            } catch (Exception ignored) {
            }
            String pTxt = String.valueOf(players);
            if (!pTxt.equals(lastPlayersText)) {
                lblPlayers.setText(pTxt);
                lastPlayersText = pTxt;
            }

            long startMs = ServerManager.timeStartMillis;
            String uTxt = startMs <= 0 ? "00:00:00"
                    : formatUptime(System.currentTimeMillis() - startMs);
            if (!uTxt.equals(lastUptimeText)) {
                lblUptime.setText(uTxt);
                lastUptimeText = uTxt;
            }

            // Thread.activeCount() walks ThreadGroup → đắt. Heap stat: Runtime.totalMemory()
            // không trivial. Cập nhật mỗi 3s là đủ cho UI.
            if (tick % 3 == 0) {
                String tTxt = String.valueOf(Thread.activeCount());
                if (!tTxt.equals(lastThreadsText)) {
                    lblThreads.setText(tTxt);
                    lastThreadsText = tTxt;
                }

                Runtime rt = Runtime.getRuntime();
                long usedHeap = (rt.totalMemory() - rt.freeMemory()) / (1024 * 1024);
                long maxHeap = rt.maxMemory() / (1024 * 1024);
                String hTxt = usedHeap + " / " + maxHeap + " MB";
                if (!hTxt.equals(lastHeapText)) {
                    lblHeap.setText(hTxt);
                    lastHeapText = hTxt;
                }
            }
        } catch (Exception ignored) {
        }
    }

    private static void refreshPlayers() {
        if (playerModel == null) {
            return;
        }
        List<Player> snapshot;
        try {
            snapshot = new ArrayList<>(Client.gI().getPlayers());
        } catch (Exception e) {
            return;
        }
        // Skip nếu kích thước và hash không đổi (xác suất collision rất thấp)
        int limit = Math.min(snapshot.size(), 1000);
        long h = 1L;
        for (int i = 0; i < limit; i++) {
            Player p = snapshot.get(i);
            if (p == null) {
                continue;
            }
            int mapId = 0;
            try {
                if (p.zone != null && p.zone.map != null) {
                    mapId = p.zone.map.mapId;
                }
            } catch (Exception ignored) {
            }
            h = h * 1315423911L ^ (p.id * 1024L + mapId);
        }
        if (snapshot.size() == lastPlayersSize && h == lastPlayersHash) {
            return;
        }
        lastPlayersSize = snapshot.size();
        lastPlayersHash = h;

        lblPlayerCount.setText(String.valueOf(snapshot.size()));
        playerModel.setRowCount(0);
        for (int i = 0; i < limit; i++) {
            Player p = snapshot.get(i);
            if (p == null) {
                continue;
            }
            String map = "?";
            try {
                if (p.zone != null && p.zone.map != null) {
                    map = p.zone.map.mapName;
                }
            } catch (Exception ignored) {
            }
            playerModel.addRow(new Object[]{p.id, p.name, map});
        }
    }

    private static String formatUptime(long ms) {
        if (ms < 0) {
            ms = 0;
        }
        long s = ms / 1000;
        long d = s / 86400;
        long h = (s % 86400) / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        if (d > 0) {
            return String.format("%dd %02d:%02d:%02d", d, h, m, sec);
        }
        return String.format("%02d:%02d:%02d", h, m, sec);
    }

    /* ===================== Console redirect ===================== */
    private static void installConsoleRedirect() {
        if (redirectInstalled) {
            return;
        }
        redirectInstalled = true;
        PrintStream out0 = System.out;
        PrintStream err0 = System.err;
        try {
            System.setOut(new PrintStream(new TeeStream(out0), true, StandardCharsets.UTF_8));
            System.setErr(new PrintStream(new TeeStream(err0), true, StandardCharsets.UTF_8));
        } catch (Exception e) {
            System.setOut(out0);
            System.setErr(err0);
        }
    }

    /** Buffer dùng lại trên EDT — drainLog luôn chạy trên EDT nên không cần đồng bộ. */
    private static final StringBuilder DRAIN_BUF = new StringBuilder(4096);

    /** Đẩy log từ hàng đợi ra console JTextPane (chạy trên EDT). */
    private static void drainLog() {
        if (doc == null) {
            return;
        }
        // Skip khi frame iconified — log sẽ tích trong queue, vẫn capped bởi LOG_QUEUE_CAP.
        if (!isFrameUserVisible()) {
            return;
        }
        String first = pollLog();
        if (first == null) {
            return;
        }
        StringBuilder buf = DRAIN_BUF;
        buf.setLength(0);
        Color curColor = FG;
        int count = 0;
        curColor = appendAnsiToBatch(first, buf, curColor);
        count++;
        String line;
        while (count < 1500 && (line = pollLog()) != null) {
            curColor = appendAnsiToBatch(line, buf, curColor);
            count++;
        }
        flushBatch(buf, curColor);

        if (doc.getLength() > 200_000) {
            try {
                doc.remove(0, doc.getLength() - 150_000);
            } catch (BadLocationException ignored) {
            }
        }
        if (autoScroll != null && autoScroll.isSelected()) {
            console.setCaretPosition(doc.getLength());
        }
    }

    private static String pollLog() {
        String s = LOG_QUEUE.poll();
        if (s != null) {
            LOG_QUEUE_SIZE.decrementAndGet();
        }
        return s;
    }

    /**
     * Append 1 dòng vào buffer theo color hiện tại. Khi gặp đổi màu thì flush
     * buffer rồi mới đổi. Trả về màu hiện tại sau khi xử lý xong dòng.
     * Reuse 1 Matcher cho mọi dòng — drainLog chỉ chạy trên EDT.
     */
    private static Color appendAnsiToBatch(String line, StringBuilder buf, Color cur) {
        Matcher m = ansiMatcher;
        if (m == null) {
            m = ANSI.matcher(line);
            ansiMatcher = m;
        } else {
            m.reset(line);
        }
        int last = 0;
        while (m.find()) {
            if (m.start() > last) {
                buf.append(line, last, m.start());
            }
            Color next = colorFor(m.group());
            if (next != cur) {
                flushBatch(buf, cur);
                cur = next;
            }
            last = m.end();
        }
        if (last < line.length()) {
            buf.append(line, last, line.length());
        }
        return cur;
    }

    private static void flushBatch(StringBuilder buf, Color color) {
        if (buf.length() == 0) {
            return;
        }
        try {
            doc.insertString(doc.getLength(), buf.toString(), attrFor(color));
        } catch (BadLocationException ignored) {
        }
        buf.setLength(0);
    }

    /** Cache SimpleAttributeSet theo màu để tránh allocate trong vòng lặp log. */
    private static SimpleAttributeSet attrFor(Color color) {
        SimpleAttributeSet a = ATTRS.get(color);
        if (a == null) {
            a = new SimpleAttributeSet();
            StyleConstants.setForeground(a, color);
            ATTRS.put(color, a);
        }
        return a;
    }

    private static Color colorFor(String code) {
        if (code.contains("31")) {
            return C_RED;
        }
        if (code.contains("32")) {
            return C_GREEN;
        }
        if (code.contains("33")) {
            return C_YELLOW;
        }
        if (code.contains("34")) {
            return C_BLUE;
        }
        if (code.contains("35")) {
            return C_PURPLE;
        }
        return FG;
    }

    /** Tee: ghi ra console gốc + đẩy vào hàng đợi GUI theo từng dòng. */
    private static final class TeeStream extends OutputStream {
        private final OutputStream orig;
        private final ByteArrayOutputStream buf = new ByteArrayOutputStream(256);

        TeeStream(OutputStream orig) {
            this.orig = orig;
        }

        @Override
        public synchronized void write(int b) throws IOException {
            orig.write(b);
            buf.write(b);
            if (b == '\n') {
                flushLine();
            }
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) throws IOException {
            orig.write(b, off, len);
            // Quét chunk-wise: ghi từng đoạn vào buf bằng một lệnh duy nhất thay vì byte-by-byte.
            int start = off;
            int end = off + len;
            for (int i = off; i < end; i++) {
                if (b[i] == '\n') {
                    buf.write(b, start, i - start + 1);
                    flushLine();
                    start = i + 1;
                }
            }
            if (start < end) {
                buf.write(b, start, end - start);
            }
        }

        @Override
        public void flush() throws IOException {
            orig.flush();
        }

        private void flushLine() {
            String line = buf.toString(StandardCharsets.UTF_8);
            buf.reset();
            LOG_QUEUE.offer(line);
            // size() trên ConcurrentLinkedQueue là O(n) — dùng AtomicInteger để O(1).
            if (LOG_QUEUE_SIZE.incrementAndGet() > LOG_QUEUE_CAP) {
                if (LOG_QUEUE.poll() != null) {
                    LOG_QUEUE_SIZE.decrementAndGet();
                }
            }
        }
    }

    /* ===================== Helpers ===================== */
    private static void runBg(Runnable r) {
        BG_POOL.submit(() -> {
            try {
                r.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private static boolean confirm(String msg) {
        return JOptionPane.showConfirmDialog(frame, msg, "Xác nhận",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private static void onCloseRequest() {
        Object[] options = {"Tắt server an toàn", "Ẩn panel", "Huỷ"};
        int r = JOptionPane.showOptionDialog(frame,
                "Bạn muốn làm gì?\n• Tắt server an toàn: lưu dữ liệu, kick người chơi rồi thoát.\n• Ẩn panel: server vẫn chạy nền.",
                "Đóng bảng điều khiển", JOptionPane.DEFAULT_OPTION,
                JOptionPane.WARNING_MESSAGE, null, options, options[2]);
        if (r == 0) {
            runBg(() -> Maintenance.gI().startImmediately());
        } else if (r == 1) {
            frame.setState(Frame.ICONIFIED);
        }
    }

    private static Component sep() {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(1, 22));
        p.setBackground(BORDER);
        p.setBorder(BorderFactory.createMatteBorder(0, 1, 0, 0, BORDER));
        return p;
    }

    private static JButton primaryButton(String text) {
        return new RoundButton(text, BTN_PRIMARY, BTN_PRIMARY_HOVER, Color.WHITE);
    }

    private static JButton dangerButton(String text) {
        return new RoundButton(text, BTN_DANGER, BTN_DANGER_HOVER, Color.WHITE);
    }

    private static JButton smallButton(String text) {
        return new RoundButton(text, BTN_NEUTRAL, BTN_NEUTRAL_HOVER, FG);
    }

    private static JButton successButton(String text) {
        return new RoundButton(text, new Color(0x238636), new Color(0x2EA043), Color.WHITE);
    }

    /* ===================== UI helpers (cards, pills, sections) ===================== */
    /**
     * Card: nền BG_PANEL bo tròn + viền BORDER, paint TRONG vùng border-insets,
     * nên setBorder(EmptyBorder(...)) trên card sẽ tạo MARGIN ngoài (chỗ trống ăn ra ngoài card).
     * Nội dung add vào BorderLayout.CENTER và đặt padding riêng cho nó.
     */
    private static JPanel card() {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                Insets in = getInsets();
                int x = in.left;
                int y = in.top;
                int w = getWidth() - in.left - in.right;
                int h = getHeight() - in.top - in.bottom;
                g2.setColor(BG_PANEL);
                g2.fillRoundRect(x, y, w, h, 12, 12);
                g2.setColor(BORDER);
                g2.setStroke(new BasicStroke(1f));
                g2.drawRoundRect(x, y, w - 1, h - 1, 12, 12);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        return p;
    }

    private static JLabel pill(String text, Color color) {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 38));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                g2.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(3, 12, 3, 12));
        JLabel l = new JLabel(text);
        l.setForeground(color);
        l.setFont(FONT_UI_BOLD);
        p.add(l);
        return l;
    }

    private static JPanel sectionHeader(String icon, String title) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 12, 0));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        left.setOpaque(false);
        JLabel ic = new JLabel(icon);
        ic.setForeground(ACCENT);
        ic.setFont(new Font("Segoe UI Symbol", Font.PLAIN, 18));
        JLabel t = new JLabel(title);
        t.setForeground(FG);
        t.setFont(FONT_TITLE);
        left.add(ic);
        left.add(t);
        wrap.add(left, BorderLayout.WEST);
        return wrap;
    }

    private static JTable makeStyledTable(DefaultTableModel m) {
        JTable t = new JTable(m) {
            @Override
            public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
                Component c = super.prepareRenderer(renderer, row, column);
                if (!isRowSelected(row)) {
                    c.setBackground((row & 1) == 0 ? BG_INPUT : ROW_ALT);
                    c.setForeground(FG);
                }
                if (c instanceof JComponent jc) {
                    jc.setBorder(CELL_PADDING);
                }
                return c;
            }
        };
        t.setBackground(BG_INPUT);
        t.setForeground(FG);
        t.setGridColor(GRID);
        t.setShowVerticalLines(false);
        t.setShowHorizontalLines(true);
        t.setRowHeight(30);
        t.setFont(FONT_UI);
        t.setSelectionBackground(new Color(0x1F6FEB));
        t.setSelectionForeground(Color.WHITE);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setAutoCreateRowSorter(true);
        t.setFillsViewportHeight(true);

        JTableHeader th = t.getTableHeader();
        th.setBackground(BG_PANEL);
        th.setForeground(FG_DIM);
        th.setFont(new Font("Segoe UI", Font.BOLD, 11));
        th.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        th.setPreferredSize(new Dimension(th.getPreferredSize().width, 32));
        ((DefaultTableCellRenderer) th.getDefaultRenderer()).setHorizontalAlignment(SwingConstants.LEFT);
        DefaultTableCellRenderer headerRdr = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean sel, boolean foc, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, sel, foc, r, c);
                l.setBackground(BG_PANEL);
                l.setForeground(FG_DIM);
                l.setFont(new Font("Segoe UI", Font.BOLD, 11));
                l.setBorder(new EmptyBorder(0, 12, 0, 12));
                l.setHorizontalAlignment(SwingConstants.LEFT);
                return l;
            }
        };
        for (int i = 0; i < t.getColumnCount(); i++) {
            t.getColumnModel().getColumn(i).setHeaderRenderer(headerRdr);
        }
        return t;
    }

    private static void styleScrollPane(JScrollPane sp) {
        sp.getVerticalScrollBar().setUI(new ThinScrollBarUI());
        sp.getHorizontalScrollBar().setUI(new ThinScrollBarUI());
        sp.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        sp.getHorizontalScrollBar().setPreferredSize(new Dimension(10, 10));
        sp.getVerticalScrollBar().setUnitIncrement(20);
    }

    private static void styleTextField(JTextField tf) {
        tf.setBackground(BG_INPUT);
        tf.setForeground(FG);
        tf.setCaretColor(FG);
        tf.setFont(FONT_UI);
        tf.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(BORDER, 8),
                new EmptyBorder(7, 12, 7, 12)));
    }

    private static void styleSpinner(JSpinner s) {
        s.setBorder(new RoundedBorder(BORDER, 6));
        JComponent ed = s.getEditor();
        if (ed instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setBackground(BG_INPUT);
            de.getTextField().setForeground(FG);
            de.getTextField().setCaretColor(FG);
            de.getTextField().setBorder(new EmptyBorder(5, 8, 5, 8));
            de.getTextField().setFont(FONT_UI);
        }
    }

    /* ===================== Custom UI classes ===================== */

    /** Button bo tròn có hiệu ứng hover mượt. */
    private static final class RoundButton extends JButton {
        private final Color base;
        private final Color hover;
        private boolean mouseOver;

        RoundButton(String text, Color base, Color hover, Color fg) {
            super(text);
            this.base = base;
            this.hover = hover;
            setForeground(fg);
            setFont(FONT_UI_BOLD);
            setFocusPainted(false);
            setContentAreaFilled(false);
            setBorder(new EmptyBorder(7, 16, 7, 16));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            setOpaque(false);
            addMouseListener(new MouseAdapter() {
                @Override
                public void mouseEntered(MouseEvent e) {
                    mouseOver = true;
                    repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    mouseOver = false;
                    repaint();
                }
            });
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            Color bg = mouseOver ? hover : base;
            if (!isEnabled()) {
                bg = new Color(bg.getRed(), bg.getGreen(), bg.getBlue(), 110);
            }
            g2.setColor(bg);
            g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 8, 8));
            g2.dispose();
            super.paintComponent(g);
        }

        @Override
        protected void paintBorder(Graphics g) {
        }
    }

    /** Border bo tròn 1px. */
    private static final class RoundedBorder implements javax.swing.border.Border {
        private final Color color;
        private final int radius;

        RoundedBorder(Color color, int radius) {
            this.color = color;
            this.radius = radius;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color);
            g2.drawRoundRect(x, y, w - 1, h - 1, radius, radius);
            g2.dispose();
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 1, 1, 1);
        }

        @Override
        public boolean isBorderOpaque() {
            return false;
        }
    }

    /** Đường viền dưới 1px. */
    private static final class MatteBottomBorder implements javax.swing.border.Border {
        private final Color color;

        MatteBottomBorder(Color color) {
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(color);
            g.fillRect(x, y + h - 1, w, 1);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(0, 0, 1, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    /** Đường viền trên 1px. */
    private static final class MatteTopBorder implements javax.swing.border.Border {
        private final Color color;

        MatteTopBorder(Color color) {
            this.color = color;
        }

        @Override
        public void paintBorder(Component c, Graphics g, int x, int y, int w, int h) {
            g.setColor(color);
            g.fillRect(x, y, w, 1);
        }

        @Override
        public Insets getBorderInsets(Component c) {
            return new Insets(1, 0, 0, 0);
        }

        @Override
        public boolean isBorderOpaque() {
            return true;
        }
    }

    /** Đốm sáng nhỏ cho status indicator. */
    private static final class GlowDot extends JComponent {
        private final Color color;
        private final int size;

        GlowDot(Color color, int size) {
            this.color = color;
            this.size = size;
            setPreferredSize(new Dimension(size + 6, size + 6));
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int cx = getWidth() / 2;
            int cy = getHeight() / 2;
            g2.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 60));
            g2.fillOval(cx - size, cy - size, size * 2, size * 2);
            g2.setColor(color);
            g2.fillOval(cx - size / 2, cy - size / 2, size, size);
            g2.dispose();
        }
    }

    /** Tab phẳng có thanh accent dưới đáy khi active. */
    private static final class FlatTabbedPaneUI extends BasicTabbedPaneUI {
        @Override
        protected void installDefaults() {
            super.installDefaults();
            tabAreaInsets = new Insets(0, 16, 0, 16);
            contentBorderInsets = new Insets(0, 0, 0, 0);
            tabInsets = new Insets(10, 18, 10, 18);
            selectedTabPadInsets = new Insets(0, 0, 0, 0);
        }

        @Override
        protected void paintTabBackground(Graphics g, int placement, int tabIndex,
                                          int x, int y, int w, int h, boolean isSelected) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BG);
            g2.fillRect(x, y, w, h);
            if (isSelected) {
                g2.setColor(ACCENT);
                g2.fillRect(x + 6, y + h - 3, w - 12, 3);
            }
            g2.dispose();
        }

        @Override
        protected void paintTabBorder(Graphics g, int placement, int tabIndex,
                                      int x, int y, int w, int h, boolean isSelected) {
        }

        @Override
        protected void paintContentBorder(Graphics g, int placement, int selectedIndex) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setColor(BORDER);
            int y = tabPane.getBoundsAt(0).y + tabPane.getBoundsAt(0).height;
            g2.fillRect(0, y - 1, tabPane.getWidth(), 1);
            g2.dispose();
        }

        @Override
        protected void paintText(Graphics g, int tabPlacement, Font font, FontMetrics metrics,
                                 int tabIndex, String title, Rectangle textRect, boolean isSelected) {
            g.setFont(font);
            g.setColor(isSelected ? FG : FG_DIM);
            g.drawString(title, textRect.x, textRect.y + metrics.getAscent());
        }

        @Override
        protected void paintFocusIndicator(Graphics g, int tabPlacement, Rectangle[] rects,
                                           int tabIndex, Rectangle iconRect, Rectangle textRect, boolean isSelected) {
        }
    }

    /** Scrollbar mảnh kiểu hiện đại. */
    private static final class ThinScrollBarUI extends BasicScrollBarUI {
        @Override
        protected void configureScrollBarColors() {
            this.thumbColor = new Color(0x484F58);
            this.trackColor = BG;
        }

        @Override
        protected JButton createDecreaseButton(int o) {
            return zeroButton();
        }

        @Override
        protected JButton createIncreaseButton(int o) {
            return zeroButton();
        }

        private JButton zeroButton() {
            JButton b = new JButton();
            b.setPreferredSize(new Dimension(0, 0));
            b.setMinimumSize(new Dimension(0, 0));
            b.setMaximumSize(new Dimension(0, 0));
            return b;
        }

        @Override
        protected void paintTrack(Graphics g, JComponent c, Rectangle r) {
            g.setColor(BG_PANEL);
            g.fillRect(r.x, r.y, r.width, r.height);
        }

        @Override
        protected void paintThumb(Graphics g, JComponent c, Rectangle r) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(thumbColor);
            int pad = 2;
            g2.fillRoundRect(r.x + pad, r.y + pad, r.width - pad * 2, r.height - pad * 2, 6, 6);
            g2.dispose();
        }
    }

    /** Renderer tô màu cho cột trạng thái boss. */
    private static final class BossStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int r, int c) {
            JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, r, c);
            String s = v == null ? "" : v.toString();
            Color fg;
            switch (s) {
                case "ACTIVE", "JOIN_MAP", "CHAT_S" -> fg = ACCENT_2;
                case "REST" -> fg = FG_DIM;
                case "DIE", "CHAT_E", "LEAVE_MAP" -> fg = C_RED;
                case "RESPAWN" -> fg = C_YELLOW;
                default -> fg = FG;
            }
            if (!sel) {
                l.setForeground(fg);
            }
            l.setFont(FONT_UI_BOLD);
            return l;
        }
    }
}
