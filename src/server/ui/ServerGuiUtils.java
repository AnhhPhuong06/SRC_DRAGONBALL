package server.ui;

import com.formdev.flatlaf.FlatLightLaf;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Arc2D;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.net.URL;

/**
 * Design system &amp; helper UI factory for the admin panel.
 *
 * <p>Premium flat design: dark navy sidebar, indigo→violet brand gradient,
 * rounded shadow cards, pill badges, custom-painted buttons and a coherent
 * stroke-based vector icon set. Every panel builds from these tokens and
 * factories so the whole app shares one look.
 */
public final class ServerGuiUtils {

    private ServerGuiUtils() {}

    // ============================================================
    //  THEME — colors, fonts, spacing
    // ============================================================

    // -- Brand & accent colors ---------------------------------------------
    public static final Color PRIMARY        = new Color(0x4F46E5); // indigo 600
    public static final Color PRIMARY_HOVER  = new Color(0x4338CA); // indigo 700
    public static final Color PRIMARY_SOFT   = new Color(0xEEF2FF); // indigo 50
    public static final Color PRIMARY_GRAD_A = new Color(0x6366F1); // indigo 500
    public static final Color PRIMARY_GRAD_B = new Color(0x8B5CF6); // violet 500
    public static final Color SUCCESS        = new Color(0x059669); // emerald 600
    public static final Color SUCCESS_HOVER  = new Color(0x047857); // emerald 700
    public static final Color SUCCESS_SOFT   = new Color(0xD9F5EA); // emerald 50+
    public static final Color WARNING        = new Color(0xD97706); // amber 600
    public static final Color WARNING_SOFT   = new Color(0xFEF3C7); // amber 100
    public static final Color DANGER         = new Color(0xE11D48); // rose 600
    public static final Color DANGER_HOVER   = new Color(0xBE123C); // rose 700
    public static final Color DANGER_SOFT    = new Color(0xFFE4E6); // rose 100
    public static final Color INFO           = new Color(0x0284C7); // sky 600
    public static final Color INFO_SOFT      = new Color(0xE0F2FE); // sky 100
    public static final Color VIOLET         = new Color(0x7C3AED); // violet 600
    public static final Color VIOLET_SOFT    = new Color(0xEDE9FE); // violet 100

    // -- Neutrals ------------------------------------------------------------
    public static final Color BG             = new Color(0xF4F6FB); // cool page bg
    public static final Color SURFACE        = new Color(0xFFFFFF);
    public static final Color SURFACE_ALT    = new Color(0xF7F9FC);
    public static final Color BORDER         = new Color(0xE7EAF3);
    public static final Color BORDER_STRONG  = new Color(0xD3DAE6);
    public static final Color TEXT           = new Color(0x111C33); // navy ink
    public static final Color TEXT_SECONDARY = new Color(0x51607C);
    public static final Color TEXT_MUTED     = new Color(0x93A1B8);
    public static final Color HOVER_ROW      = new Color(0xF3F6FF); // table row hover

    // -- Sidebar (dark navy) -------------------------------------------------
    public static final Color SIDEBAR_BG          = new Color(0x101827);
    public static final Color SIDEBAR_BG_TOP      = new Color(0x141D31);
    public static final Color SIDEBAR_BG_BOTTOM   = new Color(0x0C1220);
    public static final Color SIDEBAR_HOVER       = new Color(255, 255, 255, 18);
    public static final Color SIDEBAR_SELECTED    = new Color(99, 102, 241, 42);
    public static final Color SIDEBAR_ACCENT      = new Color(0x818CF8); // indigo 400
    public static final Color SIDEBAR_TEXT        = new Color(0xA6B0C6);
    public static final Color SIDEBAR_TEXT_ACTIVE = Color.WHITE;
    public static final Color SIDEBAR_SECTION     = new Color(0x606D89);
    public static final Color SIDEBAR_BORDER      = new Color(255, 255, 255, 16);

    // -- Console (log viewer) -------------------------------------------------
    public static final Color CONSOLE_BG   = new Color(0x0D1526);
    public static final Color CONSOLE_TEXT = new Color(0xC7D4EA);

    // -- Fonts -----------------------------------------------------------------
    public static final String FONT_FAMILY = "Segoe UI";
    public static final Font FONT_DISPLAY  = new Font(FONT_FAMILY, Font.BOLD,  24);
    public static final Font FONT_H1       = new Font(FONT_FAMILY, Font.BOLD,  18);
    public static final Font FONT_H2       = new Font(FONT_FAMILY, Font.BOLD,  15);
    public static final Font FONT_H3       = new Font(FONT_FAMILY, Font.BOLD,  13);
    public static final Font FONT_BODY     = new Font(FONT_FAMILY, Font.PLAIN, 13);
    public static final Font FONT_BODY_B   = new Font(FONT_FAMILY, Font.BOLD,  13);
    public static final Font FONT_SMALL    = new Font(FONT_FAMILY, Font.PLAIN, 12);
    public static final Font FONT_SMALL_B  = new Font(FONT_FAMILY, Font.BOLD,  12);
    public static final Font FONT_TINY     = new Font(FONT_FAMILY, Font.PLAIN, 11);
    public static final Font FONT_TINY_B   = new Font(FONT_FAMILY, Font.BOLD,  11);
    public static final Font FONT_MONO     = pickMonoFont(12);

    private static Font pickMonoFont(int size) {
        String[] preferred = { "Cascadia Code", "Consolas" };
        String[] available = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getAvailableFontFamilyNames();
        for (String want : preferred) {
            for (String have : available) {
                if (have.equalsIgnoreCase(want)) return new Font(have, Font.PLAIN, size);
            }
        }
        return new Font(Font.MONOSPACED, Font.PLAIN, size);
    }

    // -- Spacing scale (4-based) ------------------------------------------------
    public static final int SP_XS  = 4;
    public static final int SP_SM  = 8;
    public static final int SP_MD  = 12;
    public static final int SP_LG  = 16;
    public static final int SP_XL  = 24;
    public static final int SP_2XL = 32;

    // -- Radius -------------------------------------------------------------------
    public static final int RADIUS    = 12;
    public static final int RADIUS_SM = 8;
    public static final int RADIUS_LG = 16;

    // -- Shadow ---------------------------------------------------------------------
    /** Pixels reserved around a shadow card for its drop shadow. */
    public static final int SHADOW_SIZE = 5;
    private static final Color SHADOW_BASE = new Color(0x16, 0x1E, 0x33);

    // ============================================================
    //  COLOR UTILITIES
    // ============================================================

    public static Color withAlpha(Color c, int alpha) {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), alpha);
    }

    /** Linear blend a→b by t (0..1). */
    public static Color mix(Color a, Color b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        return new Color(
                Math.round(a.getRed()   + (b.getRed()   - a.getRed())   * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue()  + (b.getBlue()  - a.getBlue())  * t));
    }

    public static Color darken(Color c, float t)  { return mix(c, Color.BLACK, t); }
    public static Color lighten(Color c, float t) { return mix(c, Color.WHITE, t); }

    /** Very light tint of an accent — used for icon bubbles & soft chips. */
    public static Color soften(Color accent) { return mix(accent, Color.WHITE, 0.88f); }

    private static boolean isDarkColor(Color c) {
        return (c.getRed() * 299 + c.getGreen() * 587 + c.getBlue() * 114) / 1000 < 140;
    }

    // ============================================================
    //  THEME SETUP
    // ============================================================

    private static boolean themeInstalled = false;

    public static void setupTheme() {
        if (themeInstalled) return;
        themeInstalled = true;
        try {
            // Flat window decorations — dark title bar unified with the sidebar.
            JFrame.setDefaultLookAndFeelDecorated(true);
            JDialog.setDefaultLookAndFeelDecorated(true);

            UIManager.setLookAndFeel(new FlatLightLaf());

            // -- Window title pane (FlatLaf decorations) --
            UIManager.put("TitlePane.background",           SIDEBAR_BG_TOP);
            UIManager.put("TitlePane.inactiveBackground",   SIDEBAR_BG_TOP);
            UIManager.put("TitlePane.foreground",           Color.WHITE);
            UIManager.put("TitlePane.inactiveForeground",   SIDEBAR_TEXT);
            UIManager.put("TitlePane.unifiedBackground",    false);
            UIManager.put("TitlePane.buttonHoverBackground",   new Color(255, 255, 255, 25));
            UIManager.put("TitlePane.buttonPressedBackground", new Color(255, 255, 255, 40));
            UIManager.put("TitlePane.font", FONT_SMALL_B);

            // -- Base component styling --
            UIManager.put("Component.arc",          RADIUS_SM);
            UIManager.put("Button.arc",             RADIUS_SM + 2);
            UIManager.put("TextComponent.arc",      RADIUS_SM + 2);
            UIManager.put("ComboBox.arc",           RADIUS_SM + 2);
            UIManager.put("CheckBox.arc",           6);
            UIManager.put("ProgressBar.arc",        RADIUS);
            UIManager.put("Component.focusWidth",   1);
            UIManager.put("Component.innerFocusWidth", 0);
            UIManager.put("Component.focusColor",   withAlpha(PRIMARY, 110));
            UIManager.put("Component.focusedBorderColor", PRIMARY_GRAD_A);
            UIManager.put("Component.borderColor",  BORDER_STRONG);

            UIManager.put("ScrollBar.width",        10);
            UIManager.put("ScrollBar.thumbArc",     999);
            UIManager.put("ScrollBar.thumbInsets",  new Insets(2, 2, 2, 2));
            UIManager.put("ScrollBar.track",        new Color(0, 0, 0, 0));
            UIManager.put("ScrollBar.thumb",        BORDER_STRONG);
            UIManager.put("ScrollBar.hoverThumbColor", TEXT_MUTED);

            UIManager.put("Panel.background",       BG);
            UIManager.put("OptionPane.background",  SURFACE);

            // -- Tables --
            UIManager.put("Table.background",          SURFACE);
            UIManager.put("Table.alternateRowColor",   SURFACE_ALT);
            UIManager.put("Table.gridColor",           BORDER);
            UIManager.put("Table.rowHeight",           32);
            UIManager.put("Table.intercellSpacing",    new Dimension(0, 0));
            UIManager.put("Table.selectionBackground", PRIMARY_SOFT);
            UIManager.put("Table.selectionForeground", TEXT);
            UIManager.put("Table.selectionInactiveBackground", SURFACE_ALT);
            UIManager.put("Table.selectionInactiveForeground", TEXT);

            UIManager.put("TableHeader.background",  SURFACE);
            UIManager.put("TableHeader.foreground",  TEXT_MUTED);
            UIManager.put("TableHeader.font",        FONT_TINY_B);
            UIManager.put("TableHeader.height",      38);
            UIManager.put("TableHeader.separatorColor",       BORDER);
            UIManager.put("TableHeader.bottomSeparatorColor", BORDER);

            // -- Typography --
            UIManager.put("TextField.font",   FONT_BODY);
            UIManager.put("TextArea.font",    FONT_BODY);
            UIManager.put("Label.font",       FONT_BODY);
            UIManager.put("Button.font",      FONT_BODY_B);
            UIManager.put("ToggleButton.font", FONT_BODY_B);
            UIManager.put("ComboBox.font",    FONT_BODY);
            UIManager.put("CheckBox.font",    FONT_BODY);
            UIManager.put("RadioButton.font", FONT_BODY);
            UIManager.put("TabbedPane.font",  FONT_BODY_B);
            UIManager.put("TitledBorder.font", FONT_H3);
            UIManager.put("TitledBorder.titleColor", TEXT_SECONDARY);

            // -- Tabs --
            UIManager.put("TabbedPane.tabSeparatorsFullHeight", false);
            UIManager.put("TabbedPane.selectedBackground",      SURFACE);
            UIManager.put("TabbedPane.underlineColor",          PRIMARY);
            UIManager.put("TabbedPane.inactiveUnderlineColor",  withAlpha(PRIMARY, 120));
            UIManager.put("TabbedPane.hoverColor",              SURFACE_ALT);
            UIManager.put("TabbedPane.tabHeight",               36);
            UIManager.put("TabbedPane.tabSelectionHeight",      3);

            // -- Inputs --
            UIManager.put("TextComponent.selectionBackground", withAlpha(PRIMARY_GRAD_A, 90));
            UIManager.put("ComboBox.buttonStyle", "auto");
            UIManager.put("ComboBox.background", SURFACE);
            UIManager.put("Spinner.buttonStyle", "auto");

            // -- Popups & tooltips --
            UIManager.put("PopupMenu.borderCornerRadius", RADIUS_SM);
            UIManager.put("Popup.dropShadowPainted", true);
            UIManager.put("MenuItem.selectionBackground", PRIMARY_SOFT);
            UIManager.put("MenuItem.selectionForeground", TEXT);
            UIManager.put("ToolTip.background", new Color(0x18213A));
            UIManager.put("ToolTip.foreground", Color.WHITE);
            UIManager.put("ToolTip.font",       FONT_SMALL);
            UIManager.put("ToolTip.border",     new EmptyBorder(6, 10, 6, 10));
        } catch (Exception e) {
            System.err.println("Failed to initialize LaF: " + e.getMessage());
        }
    }

    // ============================================================
    //  BORDERS
    // ============================================================

    /** Soft 1px border in the brand neutral tone. */
    public static Border softBorder() {
        return new LineBorder(BORDER, 1, true);
    }

    /** Section title border using the brand fonts; legacy callers keep working. */
    public static TitledBorder createSectionBorder(String title) {
        TitledBorder b = BorderFactory.createTitledBorder(
                new LineBorder(BORDER, 1, true), title,
                TitledBorder.LEFT, TitledBorder.TOP,
                FONT_H3, TEXT_SECONDARY);
        b.setTitleColor(TEXT_SECONDARY);
        return b;
    }

    /** Padding-only border; convenience for spacing components. */
    public static Border pad(int top, int left, int bottom, int right) {
        return new EmptyBorder(top, left, bottom, right);
    }
    public static Border pad(int all)    { return new EmptyBorder(all, all, all, all); }
    public static Border padV(int v)     { return new EmptyBorder(v, 0, v, 0); }
    public static Border padH(int h)     { return new EmptyBorder(0, h, 0, h); }

    // ============================================================
    //  CARDS & CONTAINERS
    // ============================================================

    /**
     * Rounded surface panel with a soft layered drop shadow and hairline
     * border. The shadow is painted inside this component's own bounds, so
     * an outer margin of {@link #SHADOW_SIZE} px is reserved automatically.
     */
    public static class ShadowCard extends JPanel {
        private int arc = RADIUS;

        public ShadowCard() {
            super(new BorderLayout());
            setOpaque(false);
            setBackground(SURFACE);
            setBorder(pad(SHADOW_SIZE + SP_LG, SHADOW_SIZE + SP_LG,
                          SHADOW_SIZE + SP_LG, SHADOW_SIZE + SP_LG));
        }

        public void setArc(int arc) { this.arc = arc; repaint(); }

        /** Replace the default inner padding while keeping the shadow margin. */
        public void setContentPadding(int top, int left, int bottom, int right) {
            setBorder(pad(SHADOW_SIZE + top, SHADOW_SIZE + left,
                          SHADOW_SIZE + bottom, SHADOW_SIZE + right));
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int s = SHADOW_SIZE;
            int w = getWidth(), h = getHeight();

            // Layered soft shadow, biased downward.
            for (int i = s; i >= 1; i--) {
                float f = (s - i + 1) / (float) s;         // 1/s .. 1
                int alpha = Math.round(3 + 7 * f * f);
                g2.setColor(withAlpha(SHADOW_BASE, alpha));
                g2.fill(new RoundRectangle2D.Float(
                        s - i, s - i + 1, w - 2 * (s - i) - 1, h - 2 * (s - i) - 1,
                        arc + i, arc + i));
            }

            // Surface
            g2.setColor(getBackground());
            g2.fill(new RoundRectangle2D.Float(s, s, w - 2 * s - 1, h - 2 * s - 1, arc, arc));

            // Hairline border
            g2.setColor(BORDER);
            g2.setStroke(new BasicStroke(1f));
            g2.draw(new RoundRectangle2D.Float(s, s, w - 2 * s - 1, h - 2 * s - 1, arc, arc));
            g2.dispose();
        }
    }

    /** White rounded shadow card with default inner padding. */
    public static JPanel card() {
        return new ShadowCard();
    }

    /** Card with a specific layout manager. */
    public static JPanel card(LayoutManager layout) {
        JPanel p = card();
        p.setLayout(layout);
        return p;
    }

    /** Header section: gradient accent bar + title + optional subtitle + right action. */
    public static JPanel sectionHeader(String title, String subtitle, Component rightAction) {
        JPanel header = new JPanel(new BorderLayout(SP_MD, 0));
        header.setOpaque(false);

        // Gradient accent bar
        JPanel accentBar = new JPanel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, PRIMARY_GRAD_A, 0, getHeight(), PRIMARY_GRAD_B));
                g2.fill(new RoundRectangle2D.Float(0, 2, 4, getHeight() - 4f, 4, 4));
                g2.dispose();
            }
        };
        accentBar.setOpaque(false);
        accentBar.setPreferredSize(new Dimension(10, 0));

        JPanel labels = new JPanel();
        labels.setOpaque(false);
        labels.setLayout(new BoxLayout(labels, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel(title);
        lblTitle.setFont(FONT_H1);
        lblTitle.setForeground(TEXT);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        labels.add(lblTitle);

        if (subtitle != null && !subtitle.isEmpty()) {
            JLabel lblSub = new JLabel(subtitle);
            lblSub.setFont(FONT_SMALL);
            lblSub.setForeground(TEXT_MUTED);
            lblSub.setBorder(new EmptyBorder(SP_XS, 0, 0, 0));
            lblSub.setAlignmentX(Component.LEFT_ALIGNMENT);
            labels.add(lblSub);
        }

        JPanel left = new JPanel(new BorderLayout());
        left.setOpaque(false);
        left.add(accentBar, BorderLayout.WEST);
        left.add(labels, BorderLayout.CENTER);

        header.add(left, BorderLayout.WEST);
        if (rightAction != null) {
            header.add(rightAction, BorderLayout.EAST);
        }
        header.setBorder(new EmptyBorder(0, 0, SP_LG, 0));
        return header;
    }

    public static JPanel sectionHeader(String title) {
        return sectionHeader(title, null, null);
    }

    /** Outer scrollable page wrapper that gives the panel proper margins. */
    public static JScrollPane pageScroll(JComponent content) {
        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setBackground(BG);
        wrap.setBorder(new EmptyBorder(SP_LG, SP_XL, SP_XL, SP_XL));
        wrap.add(content, BorderLayout.NORTH);

        JScrollPane sp = new JScrollPane(wrap);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.setBackground(BG);
        sp.getViewport().setBackground(BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ============================================================
    //  LAYOUT — WrapLayout (FlowLayout that wraps)
    // ============================================================

    /**
     * Drop-in replacement for FlowLayout that correctly computes its own
     * preferred size so child components wrap to a new row when the container
     * width is too small.
     */
    public static class WrapLayout extends FlowLayout {
        public WrapLayout() { super(); }
        public WrapLayout(int align) { super(align); }
        public WrapLayout(int align, int hgap, int vgap) { super(align, hgap, vgap); }

        @Override public Dimension preferredLayoutSize(Container target) { return layoutSize(target, true); }
        @Override public Dimension minimumLayoutSize(Container target) {
            Dimension minimum = layoutSize(target, false);
            minimum.width -= (getHgap() + 1);
            return minimum;
        }

        private Dimension layoutSize(Container target, boolean preferred) {
            synchronized (target.getTreeLock()) {
                int targetWidth = target.getSize().width;
                Container c = target;
                while (c.getSize().width == 0 && c.getParent() != null) c = c.getParent();
                targetWidth = c.getSize().width;
                if (targetWidth == 0) targetWidth = Integer.MAX_VALUE;

                int hgap = getHgap();
                int vgap = getVgap();
                Insets insets = target.getInsets();
                int horizontalInsetsAndGap = insets.left + insets.right + (hgap * 2);
                int maxWidth = targetWidth - horizontalInsetsAndGap;

                Dimension dim = new Dimension(0, 0);
                int rowWidth = 0, rowHeight = 0;
                int n = target.getComponentCount();
                for (int i = 0; i < n; i++) {
                    Component m = target.getComponent(i);
                    if (!m.isVisible()) continue;
                    Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                    if (rowWidth + d.width > maxWidth) {
                        addRow(dim, rowWidth, rowHeight);
                        rowWidth = 0;
                        rowHeight = 0;
                    }
                    if (rowWidth != 0) rowWidth += hgap;
                    rowWidth += d.width;
                    rowHeight = Math.max(rowHeight, d.height);
                }
                addRow(dim, rowWidth, rowHeight);
                dim.width += horizontalInsetsAndGap;
                dim.height += insets.top + insets.bottom + vgap * 2;
                return dim;
            }
        }

        private void addRow(Dimension dim, int rowWidth, int rowHeight) {
            dim.width = Math.max(dim.width, rowWidth);
            if (dim.height > 0) dim.height += getVgap();
            dim.height += rowHeight;
        }
    }

    // ============================================================
    //  STAT TILES (KPI cards)
    // ============================================================

    public static StatTile statTile(String label, String value, Color accent) {
        return new StatTile(label, value, accent, null);
    }

    /** Stat tile with an explicit vector glyph (see {@link #vectorIcon}). */
    public static StatTile statTile(String label, String value, Color accent, String glyph) {
        return new StatTile(label, value, accent, glyph);
    }

    /**
     * KPI card: tinted icon bubble on the left, uppercase label + big value +
     * optional sub-text on the right. Call setValue()/setSubText() to update.
     */
    public static final class StatTile extends ShadowCard {
        private static final int TILE_MIN_WIDTH  = 200;
        private static final int TILE_PREF_WIDTH = 236;
        private static final int TILE_HEIGHT     = 96;

        private final JLabel valueLbl;
        private final JLabel labelLbl;
        private final JLabel subLbl;
        private final Color accent;
        private final JComponent bubble;

        public StatTile(String label, String value, Color accent) {
            this(label, value, accent, null);
        }

        public StatTile(String label, String value, Color accentIn, String glyphIn) {
            this.accent = accentIn != null ? accentIn : PRIMARY;
            String glyph = glyphIn != null ? glyphIn : guessGlyph(label);
            setContentPadding(SP_MD, SP_LG, SP_MD, SP_MD);
            setMinimumSize(new Dimension(TILE_MIN_WIDTH, TILE_HEIGHT));
            setPreferredSize(new Dimension(TILE_PREF_WIDTH, TILE_HEIGHT));

            // Icon bubble
            final Icon icon = vectorIcon(glyph, 20, this.accent);
            bubble = new JComponent() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    int sz = Math.min(getWidth(), getHeight());
                    g2.setColor(soften(accent));
                    g2.fill(new RoundRectangle2D.Float(0, (getHeight() - sz) / 2f, sz, sz, 14, 14));
                    icon.paintIcon(this, g2, (sz - icon.getIconWidth()) / 2,
                            (getHeight() - icon.getIconHeight()) / 2);
                    g2.dispose();
                }
                @Override public Dimension getPreferredSize() { return new Dimension(44, 44); }
            };

            labelLbl = new JLabel(label.toUpperCase());
            labelLbl.setFont(FONT_TINY_B);
            labelLbl.setForeground(TEXT_MUTED);

            valueLbl = new JLabel(value);
            valueLbl.setFont(FONT_H1);
            valueLbl.setForeground(TEXT);
            valueLbl.setBorder(new EmptyBorder(2, 0, 0, 0));

            subLbl = new JLabel(" ");
            subLbl.setFont(FONT_TINY);
            subLbl.setForeground(TEXT_MUTED);

            JPanel content = new JPanel();
            content.setOpaque(false);
            content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
            content.add(labelLbl);
            content.add(valueLbl);
            content.add(subLbl);

            JPanel bubbleWrap = new JPanel(new GridBagLayout());
            bubbleWrap.setOpaque(false);
            bubbleWrap.add(bubble);
            bubbleWrap.setBorder(new EmptyBorder(0, 0, 0, SP_MD));

            add(bubbleWrap, BorderLayout.WEST);

            JPanel centerWrap = new JPanel(new GridBagLayout());
            centerWrap.setOpaque(false);
            GridBagConstraints gc = new GridBagConstraints();
            gc.anchor = GridBagConstraints.WEST;
            gc.weightx = 1;
            gc.fill = GridBagConstraints.HORIZONTAL;
            centerWrap.add(content, gc);
            add(centerWrap, BorderLayout.CENTER);
        }

        private static String guessGlyph(String label) {
            String l = label == null ? "" : label.toLowerCase();
            if (l.contains("cpu"))                          return "chart";
            if (l.contains("ram") || l.contains("heap"))    return "database";
            if (l.contains("thread"))                       return "bolt";
            if (l.contains("session"))                      return "globe";
            if (l.contains("ip"))                           return "shield";
            if (l.contains("delay") || l.contains("ping"))  return "clock";
            if (l.contains("player") || l.contains("chơi")) return "users";
            if (l.contains("status") || l.contains("trạng")) return "power";
            if (l.contains("uptime"))                       return "clock";
            if (l.contains("boss"))                         return "boss";
            if (l.contains("gift"))                         return "gift";
            if (l.contains("money") || l.contains("nạp") || l.contains("tiền")) return "money";
            if (l.contains("mail") || l.contains("thư"))    return "mail";
            if (l.contains("shop") || l.contains("hàng"))   return "shop";
            if (l.contains("event") || l.contains("kiện"))  return "calendar";
            if (l.contains("maintenance") || l.contains("bảo trì")) return "settings";
            return "chart";
        }

        public void setValue(String v) {
            valueLbl.setText(v);
            int len = v == null ? 0 : v.length();
            if (len > 18)      valueLbl.setFont(FONT_H3);
            else if (len > 12) valueLbl.setFont(FONT_H2);
            else               valueLbl.setFont(FONT_H1);
        }
        public void setLabel(String v)   { labelLbl.setText(v.toUpperCase()); }
        public void setSubText(String v) { subLbl.setText(v == null || v.isEmpty() ? " " : v); }
        public Color getAccent()         { return accent; }

        /** Attach a small live chart (sparkline) to the bottom of the tile. */
        public void setSparkline(JComponent spark) {
            spark.setPreferredSize(new Dimension(0, 26));
            JPanel wrap = new JPanel(new BorderLayout());
            wrap.setOpaque(false);
            wrap.setBorder(new EmptyBorder(SP_SM, 0, 0, 0));
            wrap.add(spark, BorderLayout.CENTER);
            add(wrap, BorderLayout.SOUTH);
            setMinimumSize(new Dimension(TILE_MIN_WIDTH, TILE_HEIGHT + 32));
            setPreferredSize(new Dimension(TILE_PREF_WIDTH, TILE_HEIGHT + 32));
            revalidate();
        }
    }

    // ============================================================
    //  BUTTONS — variants
    // ============================================================

    public enum ButtonVariant { PRIMARY, SECONDARY, GHOST, DANGER, SUCCESS }

    /**
     * Custom-painted rounded button. Base fill comes from getBackground() so
     * legacy call sites that override the background keep working; hover and
     * pressed states are painted as translucent overlays on top.
     */
    public static class ModernButton extends JButton {
        private final ButtonVariant variant;
        private boolean outlined;

        ModernButton(String text, ButtonVariant variant, boolean outlined) {
            super(text);
            this.variant = variant;
            this.outlined = outlined;
            setContentAreaFilled(false);
            setFocusPainted(false);
            setRolloverEnabled(true);
            setOpaque(false);
            setFont(FONT_BODY_B);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
            setBorder(new EmptyBorder(SP_SM + 1, SP_LG, SP_SM + 1, SP_LG));
            setIconTextGap(SP_SM);
        }

        @Override protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth(), h = getHeight();
            int arc = RADIUS_SM + 2;
            RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, arc, arc);

            Color base = getBackground();
            boolean ghost = (variant == ButtonVariant.GHOST);

            if (!ghost) {
                if (variant == ButtonVariant.PRIMARY && PRIMARY.equals(base)) {
                    g2.setPaint(new GradientPaint(0, 0, PRIMARY_GRAD_A, w, h, PRIMARY_GRAD_B));
                } else {
                    g2.setPaint(base);
                }
                g2.fill(shape);
            }

            // Hover / pressed overlays
            boolean dark = !ghost && isDarkColor(base) ||
                           (variant == ButtonVariant.PRIMARY && PRIMARY.equals(base));
            if (getModel().isPressed()) {
                g2.setColor(dark ? new Color(0, 0, 0, 50) : new Color(17, 28, 51, 26));
                g2.fill(shape);
            } else if (getModel().isRollover()) {
                g2.setColor(dark ? new Color(255, 255, 255, 32) : new Color(17, 28, 51, 12));
                g2.fill(shape);
            }

            if (outlined) {
                g2.setColor(BORDER_STRONG);
                g2.setStroke(new BasicStroke(1f));
                g2.draw(shape);
            }

            if (isFocusOwner()) {
                g2.setColor(withAlpha(PRIMARY, 90));
                g2.setStroke(new BasicStroke(1.6f));
                g2.draw(new RoundRectangle2D.Float(0.8f, 0.8f, w - 2.6f, h - 2.6f, arc, arc));
            }
            g2.dispose();
            super.paintComponent(g);
        }
    }

    public static JButton button(String text, ButtonVariant variant) {
        return button(text, variant, null);
    }

    /** Button with a leading vector icon (see {@link #vectorIcon} names). */
    public static JButton button(String text, ButtonVariant variant, String glyph) {
        boolean outlined = variant == ButtonVariant.SECONDARY;
        ModernButton b = new ModernButton(text, variant, outlined);
        switch (variant) {
            case PRIMARY -> { b.setBackground(PRIMARY);      b.setForeground(Color.WHITE); }
            case SECONDARY -> { b.setBackground(SURFACE);    b.setForeground(TEXT); }
            case GHOST -> { b.setBackground(BG);             b.setForeground(TEXT_SECONDARY); }
            case DANGER -> { b.setBackground(DANGER);        b.setForeground(Color.WHITE); }
            case SUCCESS -> { b.setBackground(SUCCESS);      b.setForeground(Color.WHITE); }
        }
        if (glyph != null) {
            Color iconColor = (variant == ButtonVariant.SECONDARY) ? TEXT_SECONDARY
                    : (variant == ButtonVariant.GHOST) ? TEXT_SECONDARY : Color.WHITE;
            b.setIcon(vectorIcon(glyph, 15, iconColor));
        }
        return b;
    }

    /** Legacy 3-arg button kept for backward compat — now rounded & hover-aware. */
    public static JButton createStyledButton(String text, Color bg, Color fg) {
        ModernButton b = new ModernButton(text, ButtonVariant.SECONDARY, false);
        b.setBackground(bg);
        b.setForeground(fg);
        return b;
    }

    /** Rounded pill toggle: ON = solid success, OFF = neutral outline. */
    public static JToggleButton toggle(String text) {
        JToggleButton t = new JToggleButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth(), h = getHeight();
                RoundRectangle2D shape = new RoundRectangle2D.Float(0, 0, w - 1, h - 1, h - 1, h - 1);
                if (isSelected()) {
                    g2.setPaint(new GradientPaint(0, 0, SUCCESS, w, h, new Color(0x0BA97C)));
                    g2.fill(shape);
                    if (getModel().isRollover()) {
                        g2.setColor(new Color(255, 255, 255, 28));
                        g2.fill(shape);
                    }
                    setForeground(Color.WHITE);
                } else {
                    g2.setColor(SURFACE_ALT);
                    g2.fill(shape);
                    g2.setColor(BORDER_STRONG);
                    g2.draw(shape);
                    if (getModel().isRollover()) {
                        g2.setColor(new Color(17, 28, 51, 10));
                        g2.fill(shape);
                    }
                    setForeground(TEXT_SECONDARY);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        t.setContentAreaFilled(false);
        t.setFocusPainted(false);
        t.setRolloverEnabled(true);
        t.setOpaque(false);
        t.setFont(FONT_SMALL_B);
        t.setCursor(new Cursor(Cursor.HAND_CURSOR));
        t.setBorder(new EmptyBorder(SP_SM, SP_LG, SP_SM, SP_LG));
        return t;
    }

    // ============================================================
    //  LABELS
    // ============================================================

    public static JLabel label(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    /** Legacy helper used by older panels. */
    public static JLabel createStyledLabel(String text, int size, boolean bold) {
        JLabel l = new JLabel(text);
        l.setFont(new Font(FONT_FAMILY, bold ? Font.BOLD : Font.PLAIN, size));
        l.setForeground(TEXT);
        return l;
    }

    // ============================================================
    //  BADGES / PILLS
    // ============================================================

    /** Pill-shaped status label painted with fully rounded corners. */
    public static JLabel badge(String text, Color fg, Color bg) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getBackground());
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight()));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(FONT_TINY_B);
        l.setForeground(fg);
        l.setOpaque(false);
        l.setBackground(bg);
        l.setBorder(new EmptyBorder(3, 10, 3, 10));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    public static JLabel badgeSuccess(String text) { return badge(text, new Color(0x0B5F46), SUCCESS_SOFT); }
    public static JLabel badgeWarning(String text) { return badge(text, new Color(0x8A4B04), WARNING_SOFT); }
    public static JLabel badgeDanger(String text)  { return badge(text, new Color(0x9F1239), DANGER_SOFT); }
    public static JLabel badgeInfo(String text)    { return badge(text, new Color(0x075985), INFO_SOFT); }

    /** Pill badge with a leading status dot; soft tint derived from the color. */
    public static JLabel dotBadge(String text, Color color) {
        JLabel l = new JLabel(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(soften(color));
                g2.fill(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(),
                        getHeight(), getHeight()));
                g2.setColor(color);
                int d = 6;
                g2.fill(new Ellipse2D.Float(10, (getHeight() - d) / 2f, d, d));
                g2.dispose();
                super.paintComponent(g);
            }
        };
        l.setFont(FONT_TINY_B);
        l.setForeground(darken(color, 0.25f));
        l.setOpaque(false);
        l.setBorder(new EmptyBorder(4, 22, 4, 12));
        l.setHorizontalAlignment(SwingConstants.CENTER);
        return l;
    }

    // ============================================================
    //  TABLE STYLING
    // ============================================================

    /**
     * Apply the consistent admin-panel table style: zebra rows, hover
     * highlight, borderless grid, padded cells and an uppercase muted header.
     */
    public static JScrollPane styleTable(JTable table) {
        table.setRowHeight(34);
        table.setShowVerticalLines(false);
        table.setShowHorizontalLines(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(SURFACE);
        table.setSelectionBackground(PRIMARY_SOFT);
        table.setSelectionForeground(TEXT);
        table.setFont(FONT_BODY);
        table.setFillsViewportHeight(true);
        table.putClientProperty("ui.hoverRow", -1);

        // Row hover tracking
        table.addMouseMotionListener(new MouseAdapter() {
            @Override public void mouseMoved(MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                Object prev = table.getClientProperty("ui.hoverRow");
                if (prev == null || (int) prev != row) {
                    table.putClientProperty("ui.hoverRow", row);
                    table.repaint();
                }
            }
        });
        table.addMouseListener(new MouseAdapter() {
            @Override public void mouseExited(MouseEvent e) {
                table.putClientProperty("ui.hoverRow", -1);
                table.repaint();
            }
        });

        JTableHeader h = table.getTableHeader();
        h.setBackground(SURFACE);
        h.setForeground(TEXT_MUTED);
        h.setFont(FONT_TINY_B);
        h.setPreferredSize(new Dimension(h.getPreferredSize().width, 38));
        h.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, BORDER));
        h.setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(
                        t, value == null ? "" : value.toString().toUpperCase(),
                        isSelected, hasFocus, row, column);
                lbl.setBackground(SURFACE);
                lbl.setForeground(TEXT_MUTED);
                lbl.setFont(FONT_TINY_B);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                return lbl;
            }
        };
        h.setDefaultRenderer(headerRenderer);

        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                Object hoverObj = t.getClientProperty("ui.hoverRow");
                int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
                if (isSelected) {
                    lbl.setBackground(PRIMARY_SOFT);
                    lbl.setForeground(TEXT);
                } else if (row == hover) {
                    lbl.setBackground(HOVER_ROW);
                    lbl.setForeground(TEXT);
                } else {
                    lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                    lbl.setForeground(TEXT);
                }
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);
        for (int i = 0; i < table.getColumnCount(); i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(softBorder());
        sp.getViewport().setBackground(SURFACE);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ============================================================
    //  CONSOLE (log viewer)
    // ============================================================

    /** Style a JTextArea as a dark console and wrap it in a scroll pane. */
    public static JScrollPane styleConsole(JTextArea area) {
        area.setEditable(false);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(CONSOLE_BG);
        area.setForeground(CONSOLE_TEXT);
        area.setCaretColor(CONSOLE_TEXT);
        area.setSelectionColor(new Color(0x2A3A5C));
        area.setSelectedTextColor(Color.WHITE);
        area.setFont(FONT_MONO);
        area.setBorder(new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD));

        JScrollPane sp = new JScrollPane(area);
        sp.setBorder(new LineBorder(new Color(0x1E2A45), 1, true));
        sp.getViewport().setBackground(CONSOLE_BG);
        sp.getVerticalScrollBar().setUnitIncrement(16);
        return sp;
    }

    // ============================================================
    //  INPUTS
    // ============================================================

    /** Text field with FlatLaf placeholder text. */
    public static JTextField textField(String placeholder) {
        JTextField f = new JTextField();
        f.putClientProperty("JTextField.placeholderText", placeholder);
        f.setFont(FONT_BODY);
        return f;
    }

    /** Search field: placeholder + leading magnifier icon. */
    public static JTextField searchField(String placeholder) {
        JTextField f = textField(placeholder);
        f.putClientProperty("JTextField.leadingIcon", vectorIcon("search", 14, TEXT_MUTED));
        f.putClientProperty("JTextField.showClearButton", true);
        return f;
    }

    // ============================================================
    //  ICONS — coherent stroke-based vector set
    // ============================================================

    /** Load a bundled icon; falls back to the vector set based on the file name. */
    public static Icon loadIcon(String path) {
        try {
            URL url = ServerGuiUtils.class.getResource(path);
            if (url != null) {
                ImageIcon icon = new ImageIcon(url);
                Image img = icon.getImage().getScaledInstance(20, 20, Image.SCALE_SMOOTH);
                return new ImageIcon(img);
            }
        } catch (Exception ignored) {
        }
        String p = path == null ? "" : path.toLowerCase();
        return vectorIcon(glyphForPath(p), 20, pickIconColor(p));
    }

    private static String glyphForPath(String p) {
        if (p.contains("dashboard"))                      return "dashboard";
        if (p.contains("user2") || p.contains("player"))  return "users";
        if (p.contains("user"))                           return "user";
        if (p.contains("account"))                        return "account";
        if (p.contains("shop"))                           return "shop";
        if (p.contains("gift"))                           return "gift";
        if (p.contains("money"))                          return "money";
        if (p.contains("buff"))                           return "bolt";
        if (p.contains("item"))                           return "box";
        if (p.contains("history"))                        return "history";
        if (p.contains("calendar") || p.contains("event")) return "calendar";
        if (p.contains("monster") || p.contains("boss"))  return "boss";
        if (p.contains("mail") || p.contains("thu"))      return "mail";
        if (p.contains("shield") || p.contains("security")) return "shield";
        if (p.contains("bot"))                            return "bot";
        return "box";
    }

    private static Color pickIconColor(String p) {
        if (p.contains("dashboard"))                    return PRIMARY;
        if (p.contains("user") || p.contains("player")) return new Color(0x059669);
        if (p.contains("account"))                      return new Color(0x4F46E5);
        if (p.contains("shop"))                         return new Color(0xEA580C);
        if (p.contains("gift"))                         return new Color(0xDB2777);
        if (p.contains("money"))                        return SUCCESS;
        if (p.contains("buff"))                         return WARNING;
        if (p.contains("item"))                         return INFO;
        if (p.contains("history"))                      return TEXT_SECONDARY;
        if (p.contains("calendar") || p.contains("event")) return new Color(0x9333EA);
        if (p.contains("monster") || p.contains("boss"))  return new Color(0x4B5563);
        if (p.contains("shield") || p.contains("security")) return DANGER;
        return TEXT_MUTED;
    }

    /**
     * Crisp stroke-based vector icon. Known names: dashboard, users, user,
     * account, shop, gift, box, calendar, boss, bot, history, clock, money,
     * coin, bolt, mail, broom, search, power, server, chart, shield, settings,
     * refresh, save, database, send, diamond, globe, check, x, plus, edit,
     * trash, warning, info, star, play, pause, download, upload.
     */
    public static Icon vectorIcon(String name, int size, Color color) {
        final String n = name == null ? "box" : name.toLowerCase();
        return new Icon() {
            @Override public int getIconWidth()  { return size; }
            @Override public int getIconHeight() { return size; }
            @Override public void paintIcon(Component c, Graphics g, int x, int y) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
                g2.translate(x, y);
                double s = size / 24.0;
                g2.scale(s, s);
                g2.setColor(color);
                float sw = 1.9f;
                g2.setStroke(new BasicStroke(sw, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                drawGlyph(g2, n);
                g2.dispose();
            }
        };
    }

    // Draws on a normalized 24x24 grid.
    private static void drawGlyph(Graphics2D g, String n) {
        switch (n) {
            case "dashboard" -> {
                g.draw(new RoundRectangle2D.Double(3, 3, 7.5, 7.5, 3, 3));
                g.draw(new RoundRectangle2D.Double(13.5, 3, 7.5, 7.5, 3, 3));
                g.draw(new RoundRectangle2D.Double(3, 13.5, 7.5, 7.5, 3, 3));
                g.fill(new RoundRectangle2D.Double(13.5, 13.5, 7.5, 7.5, 3, 3));
            }
            case "users" -> {
                g.draw(new Ellipse2D.Double(5, 4, 7, 7));
                g.draw(new Arc2D.Double(2.5, 13, 12, 10, 0, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14, 4.5, 6, 6, -60, 200, Arc2D.OPEN));
                g.draw(new Arc2D.Double(14.5, 13, 7.5, 10, 20, 140, Arc2D.OPEN));
            }
            case "user" -> {
                g.draw(new Ellipse2D.Double(7.5, 3.5, 9, 9));
                g.draw(new Arc2D.Double(4, 14.5, 16, 12, 0, 180, Arc2D.OPEN));
            }
            case "account" -> {
                g.draw(new RoundRectangle2D.Double(2.5, 5, 19, 14, 4, 4));
                g.draw(new Ellipse2D.Double(5.5, 8.5, 4.5, 4.5));
                g.draw(new Line2D.Double(13, 9.5, 19, 9.5));
                g.draw(new Line2D.Double(13, 13.5, 17, 13.5));
                g.draw(new Arc2D.Double(4.5, 14.2, 6.5, 5, 0, 180, Arc2D.OPEN));
            }
            case "shop" -> {
                Path2D roof = new Path2D.Double();
                roof.moveTo(4, 8.5); roof.lineTo(5, 3.5); roof.lineTo(19, 3.5); roof.lineTo(20, 8.5);
                roof.curveTo(20, 10.3, 18.6, 11.2, 17.3, 11.2);
                roof.curveTo(16, 11.2, 14.9, 10.3, 14.9, 8.8);
                roof.curveTo(14.9, 10.3, 13.6, 11.2, 12.3, 11.2);
                g.draw(roof);
                g.draw(new Line2D.Double(5, 11, 5, 20.5));
                g.draw(new Line2D.Double(19, 11, 19, 20.5));
                g.draw(new Line2D.Double(3.5, 20.5, 20.5, 20.5));
                g.draw(new RoundRectangle2D.Double(9, 14.5, 6, 6, 1.5, 1.5));
            }
            case "gift" -> {
                g.draw(new RoundRectangle2D.Double(3.5, 8, 17, 4, 2, 2));
                g.draw(new Line2D.Double(5, 12, 5, 19.5));
                g.draw(new Line2D.Double(19, 12, 19, 19.5));
                g.draw(new Line2D.Double(5, 20, 19, 20));
                g.draw(new Line2D.Double(12, 8, 12, 20));
                g.draw(new Arc2D.Double(7, 3, 5, 5, 90, 210, Arc2D.OPEN));
                g.draw(new Arc2D.Double(12, 3, 5, 5, -120, 210, Arc2D.OPEN));
            }
            case "box", "item" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 2.8); p.lineTo(20.5, 7.4); p.lineTo(20.5, 16.6);
                p.lineTo(12, 21.2); p.lineTo(3.5, 16.6); p.lineTo(3.5, 7.4); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(3.8, 7.6, 12, 12));
                g.draw(new Line2D.Double(20.2, 7.6, 12, 12));
                g.draw(new Line2D.Double(12, 12, 12, 21));
            }
            case "calendar", "event" -> {
                g.draw(new RoundRectangle2D.Double(3, 4.5, 18, 16.5, 4, 4));
                g.draw(new Line2D.Double(3, 9.5, 21, 9.5));
                g.draw(new Line2D.Double(8, 2.5, 8, 6.5));
                g.draw(new Line2D.Double(16, 2.5, 16, 6.5));
                g.fill(new Ellipse2D.Double(7, 12.5, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(11, 12.5, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(15, 12.5, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(7, 16.5, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(11, 16.5, 2.2, 2.2));
            }
            case "boss", "monster" -> {
                Path2D skull = new Path2D.Double();
                skull.moveTo(12, 2.5);
                skull.curveTo(6.5, 2.5, 3.5, 6.5, 3.5, 11);
                skull.curveTo(3.5, 14, 5, 16, 7, 17.2);
                skull.lineTo(7, 20.5);
                skull.lineTo(17, 20.5);
                skull.lineTo(17, 17.2);
                skull.curveTo(19, 16, 20.5, 14, 20.5, 11);
                skull.curveTo(20.5, 6.5, 17.5, 2.5, 12, 2.5);
                skull.closePath();
                g.draw(skull);
                g.fill(new Ellipse2D.Double(7, 9.5, 3.6, 3.6));
                g.fill(new Ellipse2D.Double(13.4, 9.5, 3.6, 3.6));
                g.draw(new Line2D.Double(10.5, 17, 10.5, 20));
                g.draw(new Line2D.Double(13.5, 17, 13.5, 20));
            }
            case "bot" -> {
                g.draw(new RoundRectangle2D.Double(4, 8, 16, 12, 5, 5));
                g.draw(new Line2D.Double(12, 4.5, 12, 8));
                g.fill(new Ellipse2D.Double(10.7, 2, 2.6, 2.6));
                g.fill(new Ellipse2D.Double(7.5, 12, 2.6, 2.6));
                g.fill(new Ellipse2D.Double(13.9, 12, 2.6, 2.6));
                g.draw(new Line2D.Double(9, 16.8, 15, 16.8));
            }
            case "history" -> {
                g.draw(new Arc2D.Double(3.5, 3.5, 17, 17, 100, 320, Arc2D.OPEN));
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(2.2, 5.2); arrow.lineTo(2.6, 9.8); arrow.lineTo(7.0, 8.6);
                g.draw(arrow);
                g.draw(new Line2D.Double(12, 7.5, 12, 12.5));
                g.draw(new Line2D.Double(12, 12.5, 15.8, 14.6));
            }
            case "clock" -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Line2D.Double(12, 7, 12, 12.5));
                g.draw(new Line2D.Double(12, 12.5, 16, 14.8));
            }
            case "money" -> {
                g.draw(new RoundRectangle2D.Double(2.5, 6, 19, 12.5, 3, 3));
                g.draw(new Ellipse2D.Double(9, 9.2, 6, 6));
                g.fill(new Ellipse2D.Double(5.2, 11.2, 2, 2));
                g.fill(new Ellipse2D.Double(16.8, 11.2, 2, 2));
            }
            case "coin" -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                Font f = new Font(FONT_FAMILY, Font.BOLD, 12);
                g.setFont(f);
                FontMetrics fm = g.getFontMetrics();
                g.drawString("$", 12 - fm.stringWidth("$") / 2, 12 + (fm.getAscent() - fm.getDescent()) / 2);
            }
            case "bolt", "buff" -> {
                Path2D bolt = new Path2D.Double();
                bolt.moveTo(13.2, 2.5); bolt.lineTo(4.8, 13.4); bolt.lineTo(10.8, 13.4);
                bolt.lineTo(9.6, 21.5); bolt.lineTo(19.2, 9.8); bolt.lineTo(12.8, 9.8);
                bolt.closePath();
                g.fill(bolt);
            }
            case "mail" -> {
                g.draw(new RoundRectangle2D.Double(2.5, 5, 19, 14.5, 4, 4));
                Path2D flap = new Path2D.Double();
                flap.moveTo(3.5, 6.8); flap.lineTo(12, 13.2); flap.lineTo(20.5, 6.8);
                g.draw(flap);
            }
            case "broom", "trash" -> {
                g.draw(new Line2D.Double(4.5, 6.5, 19.5, 6.5));
                g.draw(new Line2D.Double(9.5, 6, 9.5, 4));
                g.draw(new Line2D.Double(9.5, 4, 14.5, 4));
                g.draw(new Line2D.Double(14.5, 4, 14.5, 6));
                Path2D can = new Path2D.Double();
                can.moveTo(6, 6.8); can.lineTo(7, 20.5); can.lineTo(17, 20.5); can.lineTo(18, 6.8);
                g.draw(can);
                g.draw(new Line2D.Double(10, 10, 10.3, 17.2));
                g.draw(new Line2D.Double(14, 10, 13.7, 17.2));
            }
            case "search" -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 12.5, 12.5));
                g.draw(new Line2D.Double(15.4, 15.4, 20.7, 20.7));
            }
            case "power" -> {
                g.draw(new Arc2D.Double(4, 5, 16, 16, 125, 290, Arc2D.OPEN));
                g.draw(new Line2D.Double(12, 2.5, 12, 10.5));
            }
            case "server" -> {
                g.draw(new RoundRectangle2D.Double(3, 3.5, 18, 7, 3, 3));
                g.draw(new RoundRectangle2D.Double(3, 13.5, 18, 7, 3, 3));
                g.fill(new Ellipse2D.Double(6, 6, 2.2, 2.2));
                g.fill(new Ellipse2D.Double(6, 16, 2.2, 2.2));
                g.draw(new Line2D.Double(11, 7, 17.5, 7));
                g.draw(new Line2D.Double(11, 17, 17.5, 17));
            }
            case "chart", "cpu" -> {
                g.draw(new Line2D.Double(3.5, 3.5, 3.5, 20.5));
                g.draw(new Line2D.Double(3.5, 20.5, 20.5, 20.5));
                Path2D line = new Path2D.Double();
                line.moveTo(6.5, 15.5); line.lineTo(10.5, 10.5); line.lineTo(13.5, 13);
                line.lineTo(19, 6);
                g.draw(line);
                g.fill(new Ellipse2D.Double(17.9, 4.9, 2.4, 2.4));
            }
            case "shield" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 2.5); p.lineTo(20, 5.5); p.lineTo(20, 11);
                p.curveTo(20, 17, 12, 21.5, 12, 21.5);
                p.curveTo(12, 21.5, 4, 17, 4, 11);
                p.lineTo(4, 5.5); p.closePath();
                g.draw(p);
                Path2D check = new Path2D.Double();
                check.moveTo(8.5, 11.5); check.lineTo(11, 14); check.lineTo(15.5, 8.8);
                g.draw(check);
            }
            case "settings", "gear" -> {
                g.draw(new Ellipse2D.Double(8.5, 8.5, 7, 7));
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    double x1 = 12 + Math.cos(a) * 8.2, y1 = 12 + Math.sin(a) * 8.2;
                    double x2 = 12 + Math.cos(a) * 10.4, y2 = 12 + Math.sin(a) * 10.4;
                    g.draw(new Line2D.Double(x1, y1, x2, y2));
                }
            }
            case "refresh" -> {
                g.draw(new Arc2D.Double(4, 4, 16, 16, 30, 300, Arc2D.OPEN));
                Path2D arrow = new Path2D.Double();
                arrow.moveTo(21.5, 4.5); arrow.lineTo(21.1, 9.5); arrow.lineTo(16.4, 8.2);
                g.draw(arrow);
            }
            case "save" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(4.5, 4.5); p.lineTo(16.5, 4.5); p.lineTo(19.5, 7.5);
                p.lineTo(19.5, 19.5); p.lineTo(4.5, 19.5); p.closePath();
                g.draw(p);
                g.draw(new RoundRectangle2D.Double(8, 13, 8, 6.5, 1, 1));
                g.draw(new Line2D.Double(8.5, 4.5, 8.5, 8.5));
                g.draw(new Line2D.Double(8.5, 8.5, 14.5, 8.5));
                g.draw(new Line2D.Double(14.5, 8.5, 14.5, 4.5));
            }
            case "database", "ram" -> {
                g.draw(new Ellipse2D.Double(4, 3, 16, 6));
                g.draw(new Arc2D.Double(4, 9, 16, 6, 180, 180, Arc2D.OPEN));
                g.draw(new Arc2D.Double(4, 15, 16, 6, 180, 180, Arc2D.OPEN));
                g.draw(new Line2D.Double(4, 6, 4, 18));
                g.draw(new Line2D.Double(20, 6, 20, 18));
            }
            case "send" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(21, 3); p.lineTo(3, 10.5); p.lineTo(10, 13.5);
                p.lineTo(13, 20.5); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(10, 13.5, 21, 3));
            }
            case "diamond", "donate" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(7, 3.5); p.lineTo(17, 3.5); p.lineTo(21.5, 9.5);
                p.lineTo(12, 20.5); p.lineTo(2.5, 9.5); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(2.8, 9.5, 21.2, 9.5));
                g.draw(new Line2D.Double(8.5, 9.5, 12, 20));
                g.draw(new Line2D.Double(15.5, 9.5, 12, 20));
            }
            case "globe" -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.draw(new Ellipse2D.Double(8.5, 3.5, 7, 17));
                g.draw(new Line2D.Double(4, 12, 20, 12));
            }
            case "map" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(3.5, 6.5); p.lineTo(9, 3.5); p.lineTo(15, 6.5); p.lineTo(20.5, 3.5);
                p.lineTo(20.5, 17.5); p.lineTo(15, 20.5); p.lineTo(9, 17.5); p.lineTo(3.5, 20.5);
                p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(9, 3.5, 9, 17.5));
                g.draw(new Line2D.Double(15, 6.5, 15, 20.5));
            }
            case "check" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(4.5, 12.5); p.lineTo(9.5, 17.5); p.lineTo(19.5, 6);
                g.draw(p);
            }
            case "x" -> {
                g.draw(new Line2D.Double(6, 6, 18, 18));
                g.draw(new Line2D.Double(18, 6, 6, 18));
            }
            case "plus" -> {
                g.draw(new Line2D.Double(12, 4.5, 12, 19.5));
                g.draw(new Line2D.Double(4.5, 12, 19.5, 12));
            }
            case "edit" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(15.5, 4.5); p.lineTo(19.5, 8.5); p.lineTo(8.5, 19.5);
                p.lineTo(4, 20); p.lineTo(4.5, 15.5); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(13.5, 6.5, 17.5, 10.5));
            }
            case "warning" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(12, 3.5); p.lineTo(21.5, 20); p.lineTo(2.5, 20); p.closePath();
                g.draw(p);
                g.draw(new Line2D.Double(12, 9.5, 12, 14.5));
                g.fill(new Ellipse2D.Double(10.9, 16.4, 2.2, 2.2));
            }
            case "info" -> {
                g.draw(new Ellipse2D.Double(3.5, 3.5, 17, 17));
                g.fill(new Ellipse2D.Double(10.9, 6.9, 2.2, 2.2));
                g.draw(new Line2D.Double(12, 11.5, 12, 16.5));
            }
            case "star" -> {
                Path2D p = new Path2D.Double();
                for (int i = 0; i < 10; i++) {
                    double r = (i % 2 == 0) ? 9.5 : 4.2;
                    double a = -Math.PI / 2 + Math.PI / 5 * i;
                    double px = 12 + Math.cos(a) * r, py = 12 + Math.sin(a) * r;
                    if (i == 0) p.moveTo(px, py); else p.lineTo(px, py);
                }
                p.closePath();
                g.draw(p);
            }
            case "play" -> {
                Path2D p = new Path2D.Double();
                p.moveTo(7, 4.5); p.lineTo(19.5, 12); p.lineTo(7, 19.5); p.closePath();
                g.fill(p);
            }
            case "pause" -> {
                g.fill(new RoundRectangle2D.Double(6, 4.5, 4, 15, 2, 2));
                g.fill(new RoundRectangle2D.Double(14, 4.5, 4, 15, 2, 2));
            }
            case "download" -> {
                g.draw(new Line2D.Double(12, 3.5, 12, 14.5));
                Path2D a = new Path2D.Double();
                a.moveTo(7, 10); a.lineTo(12, 15); a.lineTo(17, 10);
                g.draw(a);
                g.draw(new Line2D.Double(4.5, 19.5, 19.5, 19.5));
            }
            case "upload" -> {
                g.draw(new Line2D.Double(12, 15.5, 12, 4.5));
                Path2D a = new Path2D.Double();
                a.moveTo(7, 9); a.lineTo(12, 4); a.lineTo(17, 9);
                g.draw(a);
                g.draw(new Line2D.Double(4.5, 19.5, 19.5, 19.5));
            }
            default -> {
                g.draw(new RoundRectangle2D.Double(4, 4, 16, 16, 5, 5));
                g.draw(new Line2D.Double(4, 10, 20, 10));
            }
        }
    }
}
