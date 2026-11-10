package server.ui;

import server.ServerManager;
import server.AutoSaveManager;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static server.ui.ServerGuiUtils.*;

public class ServerManagerUI extends JFrame {

    static {
        // Must run before the JFrame constructor so FlatLaf window decorations
        // (dark title bar) apply to this frame and all dialogs.
        ServerGuiUtils.setupTheme();
    }

    // --- Sidebar item model -----------------------------------------------
    private static final class NavEntry {
        final String name;   // display name (or section title)
        final String glyph;  // vector icon name
        final String key;    // CardLayout key; null for section headers
        final boolean section;

        static NavEntry section(String title) { return new NavEntry(title, null, null, true); }
        static NavEntry item(String name, String glyph, String key) {
            return new NavEntry(name, glyph, key, false);
        }

        private NavEntry(String name, String glyph, String key, boolean section) {
            this.name = name;
            this.glyph = glyph;
            this.key = key;
            this.section = section;
        }

        @Override public String toString() { return name; }
    }

    private final Instant serverStartTime;
    private JPanel contentPanel;
    private CardLayout cardLayout;
    private JList<NavEntry> sidebar;
    private JLabel pageTitleLbl;
    private JLabel breadcrumbLbl;
    private JLabel clockLbl;
    private JLabel playersChipLbl;

    public static volatile boolean REQUEST_AUTO_RESTART = false;

    public ServerManagerUI() {
        super("Ngọc Rồng — Server Control Panel");

        ServerGuiUtils.setupTheme(); // idempotent — static block already installed it

        this.serverStartTime = Instant.now();

        setIconImage(createAppIcon(64));
        initUI();

        setVisible(true);

        startServerProcesses();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // Đảm bảo socket được release khi JVM thoát — kể cả lúc bị kill đột
            // ngột (Ctrl+C, NetBeans force-quit), tránh TIME_WAIT giữ port làm
            // lần restart sau bind fail.
            try { network.Network.gI().close(); } catch (Exception ignored) {}
            if (REQUEST_AUTO_RESTART) {
                triggerRestartProcess();
            }
        }));
    }

    // --- Restart -----------------------------------------------------------
    public void triggerRestartProcess() {
        int seconds = 5;
        System.out.println(">>> Restarting Server in " + seconds + "s...");
        try {
            String currentDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (osName.contains("win")) {
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c",
                        "timeout /t " + seconds + " /nobreak && run.bat");
            } else {
                pb = new ProcessBuilder("bash", "-c", "sleep " + seconds + "; ./run.sh &");
            }
            pb.directory(new File(currentDir));
            pb.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initUI() {
        getContentPane().setBackground(BG);
        setLayout(new BorderLayout());

        NavEntry[] entries = {
            NavEntry.section("Tổng Quan"),
            NavEntry.item("Bảng Điều Khiển",      "dashboard", "Dashboard"),

            NavEntry.section("Quản Lý"),
            NavEntry.item("Quản Lý Tài Khoản",    "account",   "Account"),
            NavEntry.item("Danh Sách Người Chơi", "users",     "Players"),
            NavEntry.item("Cửa Hàng (Shop)",      "shop",      "ShopEditor"),
            NavEntry.item("Vật Phẩm",             "box",       "ItemSearch"),
            NavEntry.item("Cấu Hình Boss",        "boss",      "Boss Config"),
            NavEntry.item("Chỉnh Sửa Bản Đồ",     "map",       "MapEditor"),
            NavEntry.item("Bot AI",               "bot",       "BotAI"),
            NavEntry.item("Sự Kiện (Events)",     "calendar",  "Events"),
            NavEntry.item("Dọn Rác",              "broom",     "MobMeCleaner"),
            NavEntry.item("Tỉ Lệ Đập Đồ",         "bolt",      "CombineRate"),
            NavEntry.item("Tỉ Lệ Rớt Mob",        "box",       "MobDrop"),
            NavEntry.item("Vòng Quay Thỏi",       "gift",      "VongQuay"),

            NavEntry.section("Giao Dịch"),
            NavEntry.item("Quản Lý Giftcode",     "gift",      "Giftcode"),
            NavEntry.item("Gói Donate",           "diamond",   "DonatePackage"),
            NavEntry.item("Nạp Tiền",             "money",     "NapTien"),
            NavEntry.item("Buff VND",             "bolt",      "BuffVnd"),
            NavEntry.item("Gửi Thư",              "mail",      "Mail"),

            NavEntry.section("Theo Dõi"),
            NavEntry.item("Lịch Sử Người Chơi",   "history",   "History"),
        };

        add(buildSidebar(entries), BorderLayout.WEST);
        add(buildMainArea(), BorderLayout.CENTER);

        // Content panels — names must match NavEntry.key values above.
        // Mỗi panel được khởi tạo an toàn: nếu 1 panel lỗi (VD: mất kết nối DB)
        // thì hiện thẻ báo lỗi thay vì sập toàn bộ bảng điều khiển.
        addCard(DashboardPanel::new,    "Dashboard");
        addCard(AccountPanel::new,      "Account");
        addCard(PlayersPanel::new,      "Players");
        addCard(ShopEditorPanel::new,   "ShopEditor");
        addCard(GiftcodePanel::new,     "Giftcode");
        addCard(DonatePanel::new,       "DonatePackage");
        addCard(EventPanel::new,        "Events");
        addCard(BossEditorPanel::new,   "Boss Config");
        addCard(MapEditorPanel::new,    "MapEditor");
        addCard(BotAIPanel::new,        "BotAI");
        addCard(HistoryPanel::new,      "History");
        addCard(NapTienPanel::new,      "NapTien");
        addCard(BuffVndPanel::new,      "BuffVnd");
        addCard(MailPanel::new,         "Mail");
        addCard(ItemSearchPanel::new,   "ItemSearch");
        addCard(MobMeCleanerPanel::new, "MobMeCleaner");
        addCard(CombineRatePanel::new,  "CombineRate");

        addCard(VongQuayPanel::new,     "VongQuay");

        sidebar.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                NavEntry selected = sidebar.getSelectedValue();
                if (selected != null && !selected.section) {
                    cardLayout.show(contentPanel, selected.key);
                    if (pageTitleLbl != null)  pageTitleLbl.setText(selected.name);
                    if (breadcrumbLbl != null) breadcrumbLbl.setText("Server Control  /  " + selected.name);
                }
            }
        });

        setSize(1400, 880);
        setMinimumSize(new Dimension(1180, 760));
        setLocationRelativeTo(null);
        setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

        addWindowListener(new WindowAdapter() {
            @Override public void windowClosing(WindowEvent e) {
                int confirm = JOptionPane.showConfirmDialog(
                        ServerManagerUI.this,
                        "Bạn có chắc muốn dừng Server và thoát chương trình?",
                        "Xác nhận tắt Server",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE
                );
                if (confirm == JOptionPane.YES_OPTION) shutdownServer();
            }
        });
    }

    /** Khởi tạo panel an toàn — lỗi thì thay bằng thẻ báo lỗi, không sập UI. */
    private void addCard(java.util.function.Supplier<JComponent> factory, String key) {
        JComponent comp;
        try {
            comp = factory.get();
        } catch (Throwable t) {
            System.err.println(">> [ServerManagerUI] Panel '" + key + "' failed to init: " + t);
            comp = buildErrorCard(key, t);
        }
        contentPanel.add(comp, key);
    }

    private static JComponent buildErrorCard(String key, Throwable t) {
        JPanel page = new JPanel(new GridBagLayout());
        page.setBackground(BG);

        JPanel card = card();
        ((ServerGuiUtils.ShadowCard) card).setContentPadding(SP_XL, SP_2XL, SP_XL, SP_2XL);

        JPanel col = new JPanel();
        col.setOpaque(false);
        col.setLayout(new BoxLayout(col, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel(vectorIcon("warning", 40, WARNING));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel title = new JLabel("Không thể tải panel \"" + key + "\"");
        title.setFont(FONT_H2);
        title.setForeground(TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        title.setBorder(new EmptyBorder(SP_MD, 0, SP_XS, 0));

        JLabel msg = new JLabel(String.valueOf(t));
        msg.setFont(FONT_SMALL);
        msg.setForeground(TEXT_MUTED);
        msg.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel hint = new JLabel("Kiểm tra kết nối MySQL / cấu hình rồi khởi động lại panel.");
        hint.setFont(FONT_SMALL);
        hint.setForeground(TEXT_SECONDARY);
        hint.setAlignmentX(Component.CENTER_ALIGNMENT);
        hint.setBorder(new EmptyBorder(SP_XS, 0, 0, 0));

        col.add(icon);
        col.add(title);
        col.add(msg);
        col.add(hint);
        card.add(col, BorderLayout.CENTER);
        page.add(card);
        return page;
    }

    /** Gradient "NR" square used as the window/taskbar icon. */
    private static Image createAppIcon(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = img.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setPaint(new GradientPaint(0, 0, PRIMARY_GRAD_A, size, size, PRIMARY_GRAD_B));
        g2.fill(new RoundRectangle2D.Float(0, 0, size, size, size * 0.28f, size * 0.28f));
        g2.setColor(Color.WHITE);
        g2.setFont(new Font(FONT_FAMILY, Font.BOLD, Math.round(size * 0.42f)));
        FontMetrics fm = g2.getFontMetrics();
        String s = "NR";
        g2.drawString(s, (size - fm.stringWidth(s)) / 2f,
                (size + fm.getAscent() - fm.getDescent()) / 2f);
        g2.dispose();
        return img;
    }

    // ----------------------------------------------------------------------
    //  Sidebar
    // ----------------------------------------------------------------------
    private JComponent buildSidebar(NavEntry[] entries) {
        sidebar = new JList<>(entries);
        sidebar.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sidebar.setOpaque(false);
        sidebar.setFixedCellHeight(-1); // variable: sections vs items
        sidebar.setBorder(new EmptyBorder(SP_XS, SP_MD, SP_SM, SP_MD));
        sidebar.setCellRenderer(new SidebarRenderer());

        // Section headers are not selectable
        sidebar.setSelectionModel(new DefaultListSelectionModel() {
            @Override public void setSelectionInterval(int index0, int index1) {
                if (index1 >= 0 && index1 < entries.length && !entries[index1].section) {
                    super.setSelectionInterval(index1, index1);
                }
            }
        });
        sidebar.setSelectedIndex(1); // first nav item (after the first section header)

        // Hover state via MouseMotionListener
        sidebar.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int idx = sidebar.locationToIndex(e.getPoint());
                Integer prev = (Integer) sidebar.getClientProperty("hover.index");
                if (prev == null || prev != idx) {
                    sidebar.putClientProperty("hover.index", idx);
                    sidebar.repaint();
                }
            }
        });
        sidebar.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                sidebar.putClientProperty("hover.index", -1);
                sidebar.repaint();
            }
        });

        JScrollPane scroll = new JScrollPane(sidebar);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.getVerticalScrollBar().setUnitIncrement(14);
        // Thanh cuộn tối, trong suốt để hoà vào nền sidebar (FlatLaf style)
        scroll.getVerticalScrollBar().setOpaque(false);
        scroll.getVerticalScrollBar().putClientProperty("FlatLaf.style",
                "track: #00000000; background: #00000000; thumb: #FFFFFF2E;"
                + " hoverThumbColor: #FFFFFF55; pressedThumbColor: #FFFFFF66; width: 8");

        // Gradient-painted wrapper
        JPanel wrapper = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setPaint(new GradientPaint(0, 0, SIDEBAR_BG_TOP, 0, getHeight(), SIDEBAR_BG_BOTTOM));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        wrapper.setOpaque(false);
        wrapper.setPreferredSize(new Dimension(256, 0));

        wrapper.add(buildSidebarHeader(), BorderLayout.NORTH);
        wrapper.add(scroll, BorderLayout.CENTER);
        wrapper.add(buildSidebarFooter(), BorderLayout.SOUTH);
        return wrapper;
    }

    private JPanel buildSidebarHeader() {
        JPanel p = new JPanel(new BorderLayout(SP_MD, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(SP_LG, SP_LG, SP_LG, SP_LG));

        // Gradient logo tile
        JPanel logo = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_GRAD_A, getWidth(), getHeight(), PRIMARY_GRAD_B));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), 12, 12));
                g2.setColor(Color.WHITE);
                g2.setFont(new Font(FONT_FAMILY, Font.BOLD, 15));
                FontMetrics fm = g2.getFontMetrics();
                String s = "NR";
                int x = (getWidth() - fm.stringWidth(s)) / 2;
                int y = (getHeight() + fm.getAscent() - fm.getDescent()) / 2;
                g2.drawString(s, x, y);
                g2.dispose();
            }
        };
        logo.setPreferredSize(new Dimension(40, 40));
        logo.setOpaque(false);

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Ngọc Rồng");
        title.setFont(FONT_H2);
        title.setForeground(SIDEBAR_TEXT_ACTIVE);
        JLabel sub = new JLabel("Server Control Panel");
        sub.setFont(FONT_TINY);
        sub.setForeground(SIDEBAR_TEXT);
        labels.add(title);
        labels.add(Box.createVerticalStrut(1));
        labels.add(sub);

        JPanel inner = new JPanel(new BorderLayout(SP_MD, 0));
        inner.setOpaque(false);
        inner.add(logo, BorderLayout.WEST);
        inner.add(labels, BorderLayout.CENTER);

        // Hairline separator under the header
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(inner, BorderLayout.CENTER);

        JPanel sep = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                g.setColor(SIDEBAR_BORDER);
                g.fillRect(0, 0, getWidth(), getHeight());
            }
        };
        sep.setOpaque(false);
        sep.setPreferredSize(new Dimension(0, 1));

        p.add(wrap, BorderLayout.CENTER);
        p.add(sep, BorderLayout.SOUTH);
        p.setBorder(new EmptyBorder(SP_LG, SP_LG, SP_MD, SP_LG));
        return p;
    }

    private JPanel buildSidebarFooter() {
        JPanel footer = new JPanel(new BorderLayout(SP_MD, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(SP_MD, SP_LG, SP_LG, SP_LG));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel l1 = new JLabel("Developed by");
        l1.setFont(FONT_TINY);
        l1.setForeground(SIDEBAR_TEXT);
        l1.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel l2 = new JLabel("AnhhPhuongg");
        l2.setFont(FONT_SMALL_B);
        l2.setForeground(SIDEBAR_ACCENT);
        l2.setAlignmentX(Component.LEFT_ALIGNMENT);

        labels.add(l1);
        labels.add(Box.createVerticalStrut(2));
        labels.add(l2);

        footer.add(labels, BorderLayout.CENTER);
        return footer;
    }

    /** Sidebar cell: section headers + nav items with rounded selection pill. */
    private static class SidebarRenderer extends JPanel implements ListCellRenderer<NavEntry> {
        private final JLabel lbl = new JLabel();
        private boolean selected;
        private boolean hover;
        private boolean isSection;

        SidebarRenderer() {
            super(new BorderLayout());
            setOpaque(false);
            lbl.setIconTextGap(SP_MD);
            add(lbl, BorderLayout.CENTER);
        }

        @Override
        public Component getListCellRendererComponent(JList<? extends NavEntry> list, NavEntry value,
                                                      int index, boolean isSelected, boolean cellHasFocus) {
            this.isSection = value.section;
            this.selected = isSelected;

            Integer hoverIdx = (Integer) list.getClientProperty("hover.index");
            this.hover = hoverIdx != null && hoverIdx == index && !isSelected && !value.section;

            if (value.section) {
                lbl.setText(value.name.toUpperCase());
                lbl.setIcon(null);
                lbl.setFont(FONT_TINY_B);
                lbl.setForeground(SIDEBAR_SECTION);
                lbl.setBorder(new EmptyBorder(index == 0 ? SP_MD : SP_LG, SP_MD, SP_XS, SP_MD));
                setPreferredSize(new Dimension(0, index == 0 ? 30 : 38));
            } else {
                lbl.setText(value.name);
                lbl.setFont(isSelected ? FONT_BODY_B : FONT_BODY);
                lbl.setForeground(isSelected ? SIDEBAR_TEXT_ACTIVE : (hover ? Color.WHITE : SIDEBAR_TEXT));
                lbl.setIcon(vectorIcon(value.glyph, 17,
                        isSelected ? Color.WHITE : (hover ? Color.WHITE : SIDEBAR_TEXT)));
                lbl.setBorder(new EmptyBorder(0, SP_MD + 2, 0, SP_MD));
                setPreferredSize(new Dimension(0, 42));
            }
            return this;
        }

        @Override protected void paintComponent(Graphics g) {
            if (!isSection && (selected || hover)) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                RoundRectangle2D pill = new RoundRectangle2D.Float(
                        2, 3, getWidth() - 4f, getHeight() - 6f, 10, 10);
                if (selected) {
                    g2.setColor(SIDEBAR_SELECTED);
                    g2.fill(pill);
                    // Accent bar on the left edge of the pill
                    g2.setColor(SIDEBAR_ACCENT);
                    g2.fill(new RoundRectangle2D.Float(2, getHeight() / 2f - 8, 3, 16, 3, 3));
                } else {
                    g2.setColor(SIDEBAR_HOVER);
                    g2.fill(pill);
                }
                g2.dispose();
            }
            super.paintComponent(g);
        }
    }

    // ----------------------------------------------------------------------
    //  Main area
    // ----------------------------------------------------------------------
    private JComponent buildMainArea() {
        JPanel main = new JPanel(new BorderLayout());
        main.setBackground(BG);
        main.add(buildTopbar(), BorderLayout.NORTH);

        cardLayout = new CardLayout();
        contentPanel = new JPanel(cardLayout);
        contentPanel.setBackground(BG);
        main.add(contentPanel, BorderLayout.CENTER);
        return main;
    }

    private JPanel buildTopbar() {
        JPanel bar = new JPanel(new BorderLayout());
        bar.setBackground(SURFACE);
        bar.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER),
                new EmptyBorder(SP_MD, SP_XL, SP_MD, SP_XL)));

        // Left: breadcrumb + page title
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));

        breadcrumbLbl = new JLabel("Server Control  /  Bảng Điều Khiển");
        breadcrumbLbl.setFont(FONT_TINY);
        breadcrumbLbl.setForeground(TEXT_MUTED);
        breadcrumbLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        pageTitleLbl = new JLabel("Bảng Điều Khiển");
        pageTitleLbl.setFont(FONT_H1);
        pageTitleLbl.setForeground(TEXT);
        pageTitleLbl.setAlignmentX(Component.LEFT_ALIGNMENT);

        left.add(breadcrumbLbl);
        left.add(Box.createVerticalStrut(2));
        left.add(pageTitleLbl);
        bar.add(left, BorderLayout.WEST);

        // Right: clock • players chip • online pill
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        right.setOpaque(false);

        clockLbl = new JLabel();
        clockLbl.setFont(FONT_SMALL_B);
        clockLbl.setForeground(TEXT_SECONDARY);
        clockLbl.setIcon(vectorIcon("clock", 14, TEXT_MUTED));
        clockLbl.setIconTextGap(6);
        clockLbl.setBorder(new EmptyBorder(0, 0, 0, SP_SM));
        updateClock();
        new Timer(1000, e -> updateClock()).start();

        playersChipLbl = new JLabel("0 online");
        playersChipLbl.setFont(FONT_TINY_B);
        playersChipLbl.setForeground(new Color(0x3730A3));
        playersChipLbl.setIcon(vectorIcon("users", 13, new Color(0x4F46E5)));
        playersChipLbl.setIconTextGap(6);
        JPanel playersChip = pillWrap(playersChipLbl, PRIMARY_SOFT);
        new Timer(3000, e -> updatePlayersChip()).start();

        JButton btnWebPanel = button("Mở Web Panel", ButtonVariant.PRIMARY, "link");
        btnWebPanel.setFont(FONT_TINY_B);
        btnWebPanel.setToolTipText("Mở bảng điều khiển Web Admin trên trình duyệt");
        btnWebPanel.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new java.net.URI("http://localhost:" + server.web.WebAdminServer.gI().getPort()));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        });

        right.add(clockLbl);
        right.add(playersChip);
        right.add(new OnlinePill());
        right.add(btnWebPanel);
        bar.add(right, BorderLayout.EAST);
        return bar;
    }

    private static JPanel pillWrap(JComponent content, Color bg) {
        JPanel p = new JPanel(new BorderLayout()) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(bg);
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(5, 12, 5, 12));
        p.add(content, BorderLayout.CENTER);
        return p;
    }

    private void updateClock() {
        clockLbl.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss  •  dd/MM/yyyy")));
    }

    private void updatePlayersChip() {
        int count = 0;
        try {
            if (server.Client.gI() != null && server.Client.gI().getPlayers() != null) {
                count = server.Client.gI().getOnlineCount(); // gồm cả bot
            }
        } catch (Exception ignored) {
        }
        playersChipLbl.setText(count + " online");
    }

    /** "● ONLINE" pill with a smoothly pulsing status dot. */
    private static final class OnlinePill extends JPanel {
        private static final Color TEXT_ONLINE = new Color(0x0B5F46);
        private float phase = 0f;

        OnlinePill() {
            setOpaque(false);
            setPreferredSize(new Dimension(92, 26));
            new Timer(200, e -> { phase += 0.35f; repaint(); }).start();
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();

            g2.setColor(SUCCESS_SOFT);
            g2.fillRoundRect(0, 0, w, h, h, h);

            // Pulsing halo + solid dot
            float pulse = (float) (Math.sin(phase) + 1) / 2f; // 0..1
            int halo = Math.round(4 + 3 * pulse);
            int cx = 14, cy = h / 2;
            g2.setColor(withAlpha(SUCCESS, Math.round(70 * (1 - pulse)) + 20));
            g2.fillOval(cx - halo, cy - halo, halo * 2, halo * 2);
            g2.setColor(SUCCESS);
            g2.fillOval(cx - 3, cy - 3, 7, 7);

            g2.setColor(TEXT_ONLINE);
            g2.setFont(FONT_TINY_B);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString("ONLINE", 26, (h + fm.getAscent() - fm.getDescent()) / 2);
            g2.dispose();
        }
    }

    // ----------------------------------------------------------------------
    //  Server lifecycle
    // ----------------------------------------------------------------------
    private void startServerProcesses() {
        if (Boolean.getBoolean("ui.preview")) {
            // Chế độ xem trước giao diện: không khởi động engine (không bind port,
            // không chạy scheduler) — dùng khi cần kiểm tra UI độc lập.
            System.out.println(">> [ServerManagerUI] UI PREVIEW MODE - engine not started.");
            return;
        }
        System.out.println(">> [ServerManagerUI] Starting Server Engine in background...");
        new Thread(() -> {
            try {
                ServerManager.gI().run();
                System.out.println(">> [ServerManagerUI] Server Engine started successfully!");
            } catch (Exception e) {
                System.err.println(">> [ServerManagerUI] Error starting server: " + e.getMessage());
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this,
                        "Lỗi khởi động Server: " + e.getMessage(),
                        "Lỗi", JOptionPane.ERROR_MESSAGE));
            }
        }).start();
    }

    private void shutdownServer() {
        try {
            System.out.println(">> Đang lưu dữ liệu và đóng kết nối...");
            if (AutoSaveManager.getInstance() != null) {
                AutoSaveManager.getInstance().stopAutoSave();
            }
            try {
                network.Network.gI().close();
                System.out.println(">> Đã đóng server socket, port giải phóng.");
            } catch (Exception ex) {
                System.err.println(">> Lỗi đóng Network: " + ex.getMessage());
            }
            // Kick toàn bộ session đang active
            try {
                server.Client.gI().close();
            } catch (Exception ex) {
                System.err.println(">> Lỗi đóng Client: " + ex.getMessage());
            }
        } catch (Exception e) {
            System.err.println("Lỗi khi đóng tài nguyên: " + e.getMessage());
        }
        System.out.println(">> Server shutting down... Bye!");
        System.exit(0);
    }

    public static void main(String[] args) {
        EventQueue.invokeLater(ServerManagerUI::new);
    }
}
