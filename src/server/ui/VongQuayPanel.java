package server.ui;

import jdbc.DBConnecter;
import services.VongQuayThoiVangConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static server.ui.ServerGuiUtils.*;

public class VongQuayPanel extends JPanel {

    // 1. Tỉ lệ & Mốc mở quà VIP
    private JSpinner spnIdVatPhamQuay;
    private JSpinner spnSoLuongVatPhamQuay;
    private JSpinner spnMocMoQuaHiem;
    private JSpinner spnTiLeSieuHiem;
    private JSpinner spnTiLeNhom5;
    private JSpinner spnTiLeNhom15;
    private JSpinner spnSoLuongHongNgoc;
    private JSpinner spnVangMin;
    private JSpinner spnVangMax;

    // 2. Danh sách ID vật phẩm vòng quay & Icon Previews
    private JTextField txtNhom15;
    private JPanel pnlNhom15Icons;

    private JTextField txtNhom5;
    private JPanel pnlNhom5Icons;

    private JTextField txtNhom5SauMoc;
    private JPanel pnlNhom5SauMocIcons;

    private JTextField txtNhomSieuHiem;
    private JPanel pnlNhomSieuHiemIcons;

    private JTextField txtNhomSieuHiemSauMoc;
    private JPanel pnlNhomSieuHiemSauMocIcons;

    private JTextField txtBoTro;
    private JPanel pnlBoTroIcons;

    // 3. Mốc Thượng Đế (Số lượt & Chuỗi phần thưởng linh hoạt kèm Icon)
    private JSpinner spnMoc1Spins;
    private JTextArea txtMoc1Rewards;
    private JPanel pnlMoc1Preview;

    private JSpinner spnMoc2Spins;
    private JTextArea txtMoc2Rewards;
    private JPanel pnlMoc2Preview;

    private JSpinner spnMoc3Spins;
    private JTextArea txtMoc3Rewards;
    private JPanel pnlMoc3Preview;

    private JSpinner spnMoc4Spins;
    private JTextArea txtMoc4Rewards;
    private JPanel pnlMoc4Preview;

    private JLabel lblStatusMsg;

    public VongQuayPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));
        setBackground(BG);
        initUI();
        loadValuesFromConfig();
    }

    private void initUI() {
        // ===== Header with action buttons =====
        JPanel headerActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        headerActions.setOpaque(false);

        JButton btnResetMocServerTop = button("Đặt lại mốc toàn SV", ButtonVariant.DANGER, "trash");
        btnResetMocServerTop.addActionListener(e -> onResetMocAllServer());

        JButton btnReset = button("Khôi phục mặc định", ButtonVariant.SECONDARY, "refresh");
        btnReset.addActionListener(e -> onResetDefaults());

        JButton btnSave = button("Lưu & Áp dụng ngay", ButtonVariant.SUCCESS, "check");
        btnSave.addActionListener(e -> onSaveAndApply());

        headerActions.add(btnResetMocServerTop);
        headerActions.add(btnReset);
        headerActions.add(btnSave);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(sectionHeader("Quản Lý Vòng Quay Thỏi Vàng & Mốc Thưởng",
                "Chỉnh sửa trực tiếp tỉ lệ rơi, danh sách quà vòng quay và mốc Thượng Đế. Khi sửa sẽ TỰ ĐỘNG ĐỔI TRỰC TIẾP VÀO CODE JAVA (src/services/VongQuayThoiVangConfig.java).",
                headerActions));

        lblStatusMsg = new JLabel(" ");
        lblStatusMsg.setFont(FONT_SMALL_B);
        lblStatusMsg.setForeground(SUCCESS);
        lblStatusMsg.setBorder(new EmptyBorder(SP_XS, SP_MD, 0, 0));
        header.add(lblStatusMsg);

        add(header, BorderLayout.NORTH);

        // ===== Main Scroll Content =====
        JPanel contentBox = new JPanel();
        contentBox.setLayout(new BoxLayout(contentBox, BoxLayout.Y_AXIS));
        contentBox.setOpaque(false);

        contentBox.add(buildCardTiLeVaMoc());
        contentBox.add(Box.createVerticalStrut(SP_LG));

        contentBox.add(buildCardDanhSachItem());
        contentBox.add(Box.createVerticalStrut(SP_LG));

        contentBox.add(buildCardMocThuongDe());
        contentBox.add(Box.createVerticalStrut(SP_XL));

        JScrollPane scroll = new JScrollPane(contentBox);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    /** Card 1: Tỉ Lệ & Mốc Mở Quà VIP */
    private JPanel buildCardTiLeVaMoc() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("1. Tỉ Lệ & Mốc Mở Quà VIP (Vòng Quay)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("diamond", 18, PRIMARY));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Cấu hình điều kiện lượt quay mở quà VIP, tỉ lệ các nhóm quà (Siêu hiếm, 15%, 5%, 80%), vàng và hồng ngọc.", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(5, 2, SP_LG, SP_MD));
        grid.setOpaque(false);

        spnIdVatPhamQuay = createSpinner(821, 1, 100_000, 1);
        spnSoLuongVatPhamQuay = createSpinner(1, 1, 10_000, 1);
        spnMocMoQuaHiem = createSpinner(2000, 1, 1_000_000, 100);
        spnTiLeSieuHiem = createSpinner(1, 0, 100, 1);
        spnTiLeNhom5 = createSpinner(5, 0, 100, 1);
        spnTiLeNhom15 = createSpinner(15, 0, 100, 1);
        spnSoLuongHongNgoc = createSpinner(5, 1, 100_000, 1);
        spnVangMin = createSpinner(5_000_000, 0, 2_000_000_000, 1_000_000);
        spnVangMax = createSpinner(15_000_000, 0, 2_000_000_000, 1_000_000);

        grid.add(createFieldRowWithIcon("Vật phẩm quay (Vé ID):", spnIdVatPhamQuay, "ID", 821));
        grid.add(createFieldRowWithIcon("Số lượng vé cần / lượt:", spnSoLuongVatPhamQuay, "vé", 821));
        grid.add(createFieldRowWithIcon("Mốc kích hoạt quà VIP:", spnMocMoQuaHiem, "lượt quay", 1618));
        grid.add(createFieldRowWithIcon("Tỉ lệ Siêu Hiếm (mỗi món):", spnTiLeSieuHiem, "%", 1357));
        grid.add(createFieldRowWithIcon("Tỉ lệ Nhóm 15% (Ra 1 món):", spnTiLeNhom15, "%", 987));
        grid.add(createFieldRowWithIcon("Tỉ lệ Nhóm 5% (Ra 1 món):", spnTiLeNhom5, "%", 18));
        grid.add(createFieldRowWithIcon("Hồng ngọc rơi (Nhóm thường):", spnSoLuongHongNgoc, "viên", 861));
        grid.add(createFieldRowWithIcon("Vàng rơi tối thiểu:", spnVangMin, "vàng", 457));
        grid.add(createFieldRowWithIcon("Vàng rơi tối đa:", spnVangMax, "vàng", 457));
        grid.add(new JPanel()); // ô đệm cho grid cân đối

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Card 2: Danh Sách Vật Phẩm Vòng Quay kèm Icon hiển thị trực tiếp */
    private JPanel buildCardDanhSachItem() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("2. Danh Sách ID Vật Phẩm Vòng Quay (Kèm Icon)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("box", 18, VIOLET));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Nhập các ID vật phẩm cách nhau bằng dấu phẩy (,). Icon hình ảnh và tên vật phẩm sẽ hiển thị ngay bên dưới.", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel list = new JPanel();
        list.setLayout(new BoxLayout(list, BoxLayout.Y_AXIS));
        list.setOpaque(false);

        txtNhom15 = textField("");
        pnlNhom15Icons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Nhóm 15% (Ví dụ: 20, 987, 1338):", txtNhom15, pnlNhom15Icons));
        list.add(Box.createVerticalStrut(SP_SM));

        txtNhom5 = textField("");
        pnlNhom5Icons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Nhóm 5% trước mốc (Ví dụ: 18, 19):", txtNhom5, pnlNhom5Icons));
        list.add(Box.createVerticalStrut(SP_SM));

        txtNhom5SauMoc = textField("");
        pnlNhom5SauMocIcons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Nhóm 5% sau mốc (Ví dụ: 18, 19, 1959, 1454, 674, 17):", txtNhom5SauMoc, pnlNhom5SauMocIcons));
        list.add(Box.createVerticalStrut(SP_SM));

        txtNhomSieuHiem = textField("");
        pnlNhomSieuHiemIcons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Nhóm Siêu Hiếm trước mốc (Ví dụ: 1357):", txtNhomSieuHiem, pnlNhomSieuHiemIcons));
        list.add(Box.createVerticalStrut(SP_SM));

        txtNhomSieuHiemSauMoc = textField("");
        pnlNhomSieuHiemSauMocIcons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Nhóm Siêu Hiếm sau mốc (Ví dụ: 1357, 1358, 1359):", txtNhomSieuHiemSauMoc, pnlNhomSieuHiemSauMocIcons));
        list.add(Box.createVerticalStrut(SP_SM));

        txtBoTro = textField("");
        pnlBoTroIcons = createIconRowContainer();
        list.add(createGroupInputWithIcon("Vật phẩm bổ trợ nhóm 80% (Ví dụ: 381, 382, 383, 384, 385):", txtBoTro, pnlBoTroIcons));

        card.add(list, BorderLayout.CENTER);
        return card;
    }

    /** Card 3: Cấu Hình Mốc Thưởng & Thay Đồ Ở Mốc */
    private JPanel buildCardMocThuongDe() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("3. Cấu Hình Mốc Thưởng & Thay Đồ Ở Mốc (NPC Thượng Đế)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("gift", 18, PRIMARY));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Chỉnh sửa số lượt yêu cầu và tự do thay đổi, thêm/bớt bất kỳ vật phẩm, trang bị, cải trang nào tại 4 mốc đổi quà.", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);

        JButton btnResetMocServer = button("Đặt lại mốc toàn SV", ButtonVariant.DANGER, "trash");
        btnResetMocServer.addActionListener(e -> onResetMocAllServer());
        head.add(btnResetMocServer, BorderLayout.EAST);

        card.add(head, BorderLayout.NORTH);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);

        // Hướng dẫn cú pháp
        content.add(buildGuideBanner());
        content.add(Box.createVerticalStrut(SP_MD));

        // Mốc 1
        spnMoc1Spins = createSpinner(3000, 1, 1_000_000, 500);
        txtMoc1Rewards = createRewardTextArea();
        pnlMoc1Preview = createMocPreviewPanel();
        attachLivePreview(txtMoc1Rewards, pnlMoc1Preview);
        content.add(buildMocSection(1, "MỐC 1", spnMoc1Spins, txtMoc1Rewards, pnlMoc1Preview,
                "457:100, 1618:5", null));
        content.add(Box.createVerticalStrut(SP_MD));

        // Mốc 2
        spnMoc2Spins = createSpinner(8000, 1, 1_000_000, 500);
        txtMoc2Rewards = createRewardTextArea();
        pnlMoc2Preview = createMocPreviewPanel();
        attachLivePreview(txtMoc2Rewards, pnlMoc2Preview);
        content.add(buildMocSection(2, "MỐC 2", spnMoc2Spins, txtMoc2Rewards, pnlMoc2Preview,
                "457:200, 16:5:30-0", null));
        content.add(Box.createVerticalStrut(SP_MD));

        // Mốc 3
        spnMoc3Spins = createSpinner(15000, 1, 1_000_000, 1000);
        txtMoc3Rewards = createRewardTextArea();
        pnlMoc3Preview = createMocPreviewPanel();
        attachLivePreview(txtMoc3Rewards, pnlMoc3Preview);
        content.add(buildMocSection(3, "MỐC 3", spnMoc3Spins, txtMoc3Rewards, pnlMoc3Preview,
                "457:3000, 987:800, 1277:500, 1177:120, 1593:1:50-30;77-30;103-30;5-25;14-15;95-12;96-12;97-15;30-0", "30"));
        content.add(Box.createVerticalStrut(SP_MD));

        // Mốc 4
        spnMoc4Spins = createSpinner(20000, 1, 1_000_000, 1000);
        txtMoc4Rewards = createRewardTextArea();
        pnlMoc4Preview = createMocPreviewPanel();
        attachLivePreview(txtMoc4Rewards, pnlMoc4Preview);
        content.add(buildMocSection(4, "MỐC 4", spnMoc4Spins, txtMoc4Rewards, pnlMoc4Preview,
                "457:5000, 987:1500, 1277:1000, 1177:250, 1241:1:50-35;77-35;103-35;5-35;14-20;95-18;96-18;97-25;108-20;106-0;30-0", "35"));

        card.add(content, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildGuideBanner() {
        JPanel banner = new JPanel(new BorderLayout(SP_SM, 0));
        banner.setBackground(PRIMARY_SOFT);
        banner.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(PRIMARY_GRAD_A, 1, true),
                new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD)
        ));

        JLabel icon = new JLabel(vectorIcon("info", 20, PRIMARY));
        banner.add(icon, BorderLayout.WEST);

        String html = "<html><body style='font-family:Segoe UI, sans-serif; font-size:12px; color:#1e293b;'>"
                + "<b>💡 HƯỚNG DẪN THAY ĐỒ & CHỈ SỐ Ở TỪNG MỐC:</b><br/>"
                + "• <b>Định dạng 1 món đồ:</b> <code style='color:#4338ca;'>ID:SốLượng</code> (VD: <code>457:100</code> = 100 Thỏi Vàng, <code>1618:5</code> = 5 Hộp Ván Bay).<br/>"
                + "• <b>Thêm chỉ số hoặc Khóa giao dịch:</b> <code style='color:#7c3aed;'>ID:SốLượng:OptionId-ChỉSố;OptionId-ChỉSố...</code><br/>"
                + "&nbsp;&nbsp;Ví dụ: <code>16:5:30-0</code> (5 viên Ngọc Rồng 3 sao Khóa, option 30 là Không thể GD).<br/>"
                + "&nbsp;&nbsp;Ví dụ: <code>1593:1:50-30;77-30;30-0</code> (1 Cải trang có Sức đánh 30%, HP 30%, Khóa giao dịch).<br/>"
                + "• <b>Phân cách nhiều món bằng dấu phẩy (,)</b>. Bạn có thể nhấn <b>'➕ Thêm đồ'</b> để chọn trực quan có kèm icon hình ảnh và xem trước!"
                + "</body></html>";

        JLabel lbl = new JLabel(html);
        banner.add(lbl, BorderLayout.CENTER);
        return banner;
    }

    private JPanel buildMocSection(int mocNumber, String mocTitle, JSpinner spnSpins,
                                   JTextArea txtRewards, JPanel pnlPreview,
                                   String defaultRewardStr, String disguisePreset) {
        JPanel box = new JPanel();
        box.setLayout(new BoxLayout(box, BoxLayout.Y_AXIS));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(SP_MD, SP_MD, SP_MD, SP_MD)
        ));

        // Header Row: Tiêu đề + Nút thao tác nhanh
        JPanel topBar = new JPanel(new BorderLayout(SP_SM, 0));
        topBar.setOpaque(false);

        JLabel lblTitle = label(mocTitle, FONT_H3, PRIMARY);
        lblTitle.setIcon(vectorIcon("award", 16, PRIMARY));
        lblTitle.setIconTextGap(SP_XS);
        topBar.add(lblTitle, BorderLayout.WEST);

        JPanel btnActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_XS, 0));
        btnActions.setOpaque(false);

        JButton btnAdd = button("➕ Thêm đồ...", ButtonVariant.PRIMARY, null);
        btnAdd.setFont(FONT_SMALL_B);
        btnAdd.setPreferredSize(new Dimension(110, 28));
        btnAdd.addActionListener(e -> showAddItemDialog(txtRewards, pnlPreview));
        btnActions.add(btnAdd);

        if ("30".equals(disguisePreset)) {
            JButton btnCt30 = button("👕 Mẫu CT 30%", ButtonVariant.SECONDARY, null);
            btnCt30.setFont(FONT_SMALL_B);
            btnCt30.setPreferredSize(new Dimension(115, 28));
            btnCt30.addActionListener(e -> onAddDisguisePreset(txtRewards, pnlPreview, 1593, VongQuayThoiVangConfig.PRESET_CAI_TRANG_30));
            btnActions.add(btnCt30);
        } else if ("35".equals(disguisePreset)) {
            JButton btnCt35 = button("🔥 Mẫu CT 35%", ButtonVariant.SECONDARY, null);
            btnCt35.setFont(FONT_SMALL_B);
            btnCt35.setPreferredSize(new Dimension(115, 28));
            btnCt35.addActionListener(e -> onAddDisguisePreset(txtRewards, pnlPreview, 1241, VongQuayThoiVangConfig.PRESET_CAI_TRANG_35));
            btnActions.add(btnCt35);
        }

        JButton btnResetMoc = button("🔄 Mặc định", ButtonVariant.SECONDARY, null);
        btnResetMoc.setFont(FONT_SMALL_B);
        btnResetMoc.setPreferredSize(new Dimension(95, 28));
        btnResetMoc.addActionListener(e -> {
            txtRewards.setText(defaultRewardStr);
            updateMocPreview(txtRewards, pnlPreview);
        });
        btnActions.add(btnResetMoc);

        topBar.add(btnActions, BorderLayout.EAST);
        box.add(topBar);
        box.add(Box.createVerticalStrut(SP_SM));

        // Row 1: Số lượt yêu cầu
        JPanel rowSpins = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        rowSpins.setOpaque(false);
        rowSpins.add(label("Số lượt quay yêu cầu:", FONT_BODY_B, TEXT_SECONDARY));
        rowSpins.add(spnSpins);
        rowSpins.add(label("lượt", FONT_BODY, TEXT_MUTED));
        box.add(rowSpins);
        box.add(Box.createVerticalStrut(SP_SM));

        // Row 2: Danh sách phần thưởng
        JPanel rowRewards = new JPanel(new BorderLayout(0, SP_XS));
        rowRewards.setOpaque(false);
        rowRewards.add(label("Chuỗi cấu hình phần thưởng (ID:SốLượng hoặc ID:SốLượng:Option-ChỉSố):", FONT_SMALL_B, TEXT_SECONDARY), BorderLayout.NORTH);

        JScrollPane scrollArea = new JScrollPane(txtRewards);
        scrollArea.setBorder(BorderFactory.createLineBorder(BORDER, 1));
        scrollArea.setPreferredSize(new Dimension(500, 52));
        rowRewards.add(scrollArea, BorderLayout.CENTER);
        box.add(rowRewards);
        box.add(Box.createVerticalStrut(SP_XS));

        // Row 3: Live Preview kèm Icon trực tiếp
        JPanel previewBox = new JPanel(new BorderLayout());
        previewBox.setBackground(SURFACE_ALT);
        previewBox.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(SP_XS, SP_SM, SP_XS, SP_SM)
        ));

        JLabel lblBadge = label("📦 Xem trước quà thực tế (Có Icon):", FONT_SMALL_B, SUCCESS);
        previewBox.add(lblBadge, BorderLayout.NORTH);
        previewBox.add(pnlPreview, BorderLayout.CENTER);
        box.add(previewBox);

        return box;
    }

    private void onAddDisguisePreset(JTextArea txtRewards, JPanel pnlPreview, int defaultId, String presetOptions) {
        String inputId = JOptionPane.showInputDialog(this,
                "Nhập ID Cải Trang muốn trao tặng (Mặc định: " + defaultId + " - " + ItemIconHelper.getItemName(defaultId) + "):",
                String.valueOf(defaultId));
        if (inputId != null && !inputId.trim().isEmpty()) {
            try {
                int ctId = Integer.parseInt(inputId.trim());
                String itemStr = ctId + ":1:" + presetOptions;
                String cur = txtRewards.getText().trim();
                txtRewards.setText(cur.isEmpty() ? itemStr : cur + ", " + itemStr);
                updateMocPreview(txtRewards, pnlPreview);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "ID cải trang không hợp lệ!");
            }
        }
    }

    private void showAddItemDialog(JTextArea targetTxt, JPanel previewPanel) {
        Window parent = SwingUtilities.getWindowAncestor(this);
        JDialog dialog = new JDialog(parent instanceof Frame ? (Frame) parent : null, "Thêm Món Đồ Vào Mốc (Kèm Icon)", Dialog.ModalityType.APPLICATION_MODAL);
        dialog.setLayout(new BorderLayout(0, SP_MD));
        dialog.getContentPane().setBackground(BG);

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(SP_MD, SP_LG, SP_SM, SP_LG));

        // Row 1: ID vật phẩm kèm Icon hiển thị trực quan
        JSpinner spnId = createSpinner(457, 0, 100_000, 1);
        JLabel lblIconPreview = new JLabel(ItemIconHelper.getItemIcon(457, 32));
        lblIconPreview.setPreferredSize(new Dimension(34, 34));
        lblIconPreview.setHorizontalAlignment(SwingConstants.CENTER);
        lblIconPreview.setBorder(BorderFactory.createLineBorder(BORDER_STRONG, 1, true));

        JLabel lblItemName = label("👉 " + ItemIconHelper.getItemName(457), FONT_BODY_B, PRIMARY);

        spnId.addChangeListener(e -> {
            int id = ((Number) spnId.getValue()).intValue();
            lblIconPreview.setIcon(ItemIconHelper.getItemIcon(id, 32));
            lblItemName.setText("👉 " + ItemIconHelper.getItemName(id) + " (Icon: " + ItemIconHelper.getIconId(id) + ")");
        });

        JPanel rowId = new JPanel(new BorderLayout(SP_MD, 0));
        rowId.setOpaque(false);
        rowId.add(label("ID Vật phẩm:", FONT_BODY_B, TEXT), BorderLayout.WEST);

        JPanel idRight = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        idRight.setOpaque(false);
        idRight.add(spnId);
        idRight.add(lblIconPreview);
        idRight.add(lblItemName);
        rowId.add(idRight, BorderLayout.CENTER);
        content.add(rowId);
        content.add(Box.createVerticalStrut(SP_SM));

        // Row 2: Số lượng
        JSpinner spnQty = createSpinner(1, 1, 100_000, 1);
        JPanel rowQty = new JPanel(new BorderLayout(SP_MD, 0));
        rowQty.setOpaque(false);
        rowQty.add(label("Số lượng:", FONT_BODY_B, TEXT), BorderLayout.WEST);
        JPanel qtyRight = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        qtyRight.setOpaque(false);
        qtyRight.add(spnQty);
        rowQty.add(qtyRight, BorderLayout.CENTER);
        content.add(rowQty);
        content.add(Box.createVerticalStrut(SP_SM));

        // Row 3: Mẫu option
        String[] presets = {
                "0. Không thêm chỉ số đặc biệt (Đồ / Vật phẩm thường)",
                "1. Khóa giao dịch (Không thể giao dịch - Option 30: 0)",
                "2. Cải trang VIP 30% (Chỉ số 30% SĐ, HP, KI + STCM, TLCM, Hút, Phản, Khóa)",
                "3. Cải trang VIP 35% (Chỉ số 35% SĐ, HP, KI + STCM, TLCM, Hút, Phản, Né, Khóa)",
                "4. Tùy chỉnh chuỗi chỉ số (Tự nhập dạng ID-Param;ID-Param...)"
        };
        JComboBox<String> cbPreset = new JComboBox<>(presets);
        cbPreset.setFont(FONT_BODY);

        JTextField txtCustomOpt = textField("Ví dụ: 50-20;77-20;30-0");
        txtCustomOpt.setEnabled(false);

        cbPreset.addActionListener(e -> {
            txtCustomOpt.setEnabled(cbPreset.getSelectedIndex() == 4);
        });

        JPanel rowPreset = new JPanel(new BorderLayout(SP_MD, SP_XS));
        rowPreset.setOpaque(false);
        rowPreset.add(label("Mẫu chỉ số (Option):", FONT_BODY_B, TEXT), BorderLayout.NORTH);
        rowPreset.add(cbPreset, BorderLayout.CENTER);
        content.add(rowPreset);
        content.add(Box.createVerticalStrut(SP_SM));

        JPanel rowCustom = new JPanel(new BorderLayout(SP_MD, SP_XS));
        rowCustom.setOpaque(false);
        rowCustom.add(label("Chuỗi option tự nhập (khi chọn mục 4):", FONT_SMALL, TEXT_MUTED), BorderLayout.NORTH);
        rowCustom.add(txtCustomOpt, BorderLayout.CENTER);
        content.add(rowCustom);

        dialog.add(content, BorderLayout.CENTER);

        // Buttons
        JPanel btnBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, SP_MD));
        btnBar.setOpaque(false);
        btnBar.setBorder(new EmptyBorder(0, SP_LG, SP_MD, SP_LG));

        JButton btnCancel = button("Hủy bỏ", ButtonVariant.SECONDARY, "x");
        btnCancel.addActionListener(e -> dialog.dispose());

        JButton btnAdd = button("Chèn vào mốc này", ButtonVariant.SUCCESS, "plus");
        btnAdd.addActionListener(e -> {
            int id = ((Number) spnId.getValue()).intValue();
            int qty = ((Number) spnQty.getValue()).intValue();
            int presetIdx = cbPreset.getSelectedIndex();

            String itemStr = id + ":" + qty;
            if (presetIdx == 1) {
                itemStr += ":" + VongQuayThoiVangConfig.PRESET_KHOA;
            } else if (presetIdx == 2) {
                itemStr += ":" + VongQuayThoiVangConfig.PRESET_CAI_TRANG_30;
            } else if (presetIdx == 3) {
                itemStr += ":" + VongQuayThoiVangConfig.PRESET_CAI_TRANG_35;
            } else if (presetIdx == 4) {
                String cust = txtCustomOpt.getText().trim();
                if (!cust.isEmpty()) {
                    itemStr += ":" + cust;
                }
            }

            String current = targetTxt.getText().trim();
            if (current.isEmpty()) {
                targetTxt.setText(itemStr);
            } else {
                targetTxt.setText(current + ", " + itemStr);
            }
            updateMocPreview(targetTxt, previewPanel);
            dialog.dispose();
        });

        btnBar.add(btnCancel);
        btnBar.add(btnAdd);
        dialog.add(btnBar, BorderLayout.SOUTH);

        dialog.setResizable(false);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        dialog.setVisible(true);
    }

    private JTextArea createRewardTextArea() {
        JTextArea area = new JTextArea(2, 40);
        area.setFont(FONT_MONO);
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBackground(SURFACE);
        area.setForeground(TEXT);
        area.setBorder(new EmptyBorder(SP_XS, SP_SM, SP_XS, SP_SM));
        return area;
    }

    private JPanel createMocPreviewPanel() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        return p;
    }

    private JPanel createIconRowContainer() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(2, 0, 4, 0));
        return p;
    }

    private void updateMocPreview(JTextArea area, JPanel previewPanel) {
        previewPanel.removeAll();
        previewPanel.add(ItemIconHelper.renderRewardsPanel(area.getText()), BorderLayout.CENTER);
        previewPanel.revalidate();
        previewPanel.repaint();
    }

    private void attachLivePreview(JTextArea area, JPanel previewPanel) {
        area.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateMocPreview(area, previewPanel);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateMocPreview(area, previewPanel);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateMocPreview(area, previewPanel);
            }
        });
    }

    private void updateGroupIcons(JTextField txt, JPanel pnl) {
        pnl.removeAll();
        int[] ids = VongQuayThoiVangConfig.parseStringToIntArray(txt.getText(), new int[0]);
        pnl.add(ItemIconHelper.renderItemGroupIcons(ids), BorderLayout.CENTER);
        pnl.revalidate();
        pnl.repaint();
    }

    private void attachGroupIconsLiveUpdate(JTextField txt, JPanel pnl) {
        txt.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                updateGroupIcons(txt, pnl);
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                updateGroupIcons(txt, pnl);
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                updateGroupIcons(txt, pnl);
            }
        });
    }

    private JSpinner createSpinner(int def, int min, int max, int step) {
        JSpinner spn = new JSpinner(new SpinnerNumberModel(def, min, max, step));
        styleSpinner(spn);
        return spn;
    }

    private void styleSpinner(JSpinner s) {
        s.setPreferredSize(new Dimension(100, 32));
        JComponent ed = s.getEditor();
        if (ed instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setFont(FONT_BODY_B);
            de.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    private JPanel createFieldRowWithIcon(String title, JSpinner spinner, String unitText, int previewItemId) {
        JPanel row = new JPanel(new BorderLayout(SP_SM, 0));
        row.setOpaque(false);
        row.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD)
        ));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_XS, 0));
        left.setOpaque(false);

        ImageIcon icon = ItemIconHelper.getItemIcon(previewItemId, 20);
        if (icon != null) {
            left.add(new JLabel(icon));
        }

        JLabel lbl = label(title, FONT_BODY_B, TEXT_SECONDARY);
        left.add(lbl);

        JLabel unit = label(unitText, FONT_BODY, TEXT_MUTED);
        unit.setPreferredSize(new Dimension(65, 20));

        JPanel right = new JPanel(new BorderLayout(SP_XS, 0));
        right.setOpaque(false);
        right.add(spinner, BorderLayout.CENTER);
        right.add(unit, BorderLayout.EAST);

        row.add(left, BorderLayout.WEST);
        row.add(right, BorderLayout.CENTER);
        return row;
    }

    private JPanel createGroupInputWithIcon(String title, JTextField textField, JPanel iconContainer) {
        JPanel container = new JPanel();
        container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
        container.setOpaque(false);
        container.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(SP_XS, SP_MD, SP_XS, SP_MD)
        ));

        JPanel row = new JPanel(new BorderLayout(SP_SM, 0));
        row.setOpaque(false);

        JLabel lbl = label(title, FONT_BODY_B, TEXT_SECONDARY);
        lbl.setPreferredSize(new Dimension(300, 24));

        row.add(lbl, BorderLayout.WEST);
        row.add(textField, BorderLayout.CENTER);
        container.add(row);

        container.add(iconContainer);
        attachGroupIconsLiveUpdate(textField, iconContainer);
        return container;
    }

    private void loadValuesFromConfig() {
        VongQuayThoiVangConfig.load();

        spnIdVatPhamQuay.setValue(VongQuayThoiVangConfig.idVatPhamQuay);
        spnSoLuongVatPhamQuay.setValue(VongQuayThoiVangConfig.soLuongVatPhamQuay);
        spnMocMoQuaHiem.setValue(VongQuayThoiVangConfig.mocMoQuaHiem);
        spnTiLeSieuHiem.setValue(VongQuayThoiVangConfig.tiLeMoiMonSieuHiem);
        spnTiLeNhom5.setValue(VongQuayThoiVangConfig.tiLeNhom5);
        spnTiLeNhom15.setValue(VongQuayThoiVangConfig.tiLeNhom15);
        spnSoLuongHongNgoc.setValue(VongQuayThoiVangConfig.soLuongHongNgoc);
        spnVangMin.setValue(VongQuayThoiVangConfig.vangMin);
        spnVangMax.setValue(VongQuayThoiVangConfig.vangMax);

        txtNhom15.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idNhom15));
        updateGroupIcons(txtNhom15, pnlNhom15Icons);

        txtNhom5.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idNhom5));
        updateGroupIcons(txtNhom5, pnlNhom5Icons);

        txtNhom5SauMoc.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idNhom5SauMoc));
        updateGroupIcons(txtNhom5SauMoc, pnlNhom5SauMocIcons);

        txtNhomSieuHiem.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idNhomSieuHiem));
        updateGroupIcons(txtNhomSieuHiem, pnlNhomSieuHiemIcons);

        txtNhomSieuHiemSauMoc.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idNhomSieuHiemSauMoc));
        updateGroupIcons(txtNhomSieuHiemSauMoc, pnlNhomSieuHiemSauMocIcons);

        txtBoTro.setText(VongQuayThoiVangConfig.intArrayToString(VongQuayThoiVangConfig.idVatPhamBoTro));
        updateGroupIcons(txtBoTro, pnlBoTroIcons);

        spnMoc1Spins.setValue(VongQuayThoiVangConfig.moc1Spins);
        txtMoc1Rewards.setText(VongQuayThoiVangConfig.moc1RewardsStr);
        updateMocPreview(txtMoc1Rewards, pnlMoc1Preview);

        spnMoc2Spins.setValue(VongQuayThoiVangConfig.moc2Spins);
        txtMoc2Rewards.setText(VongQuayThoiVangConfig.moc2RewardsStr);
        updateMocPreview(txtMoc2Rewards, pnlMoc2Preview);

        spnMoc3Spins.setValue(VongQuayThoiVangConfig.moc3Spins);
        txtMoc3Rewards.setText(VongQuayThoiVangConfig.moc3RewardsStr);
        updateMocPreview(txtMoc3Rewards, pnlMoc3Preview);

        spnMoc4Spins.setValue(VongQuayThoiVangConfig.moc4Spins);
        txtMoc4Rewards.setText(VongQuayThoiVangConfig.moc4RewardsStr);
        updateMocPreview(txtMoc4Rewards, pnlMoc4Preview);
    }

    private void onSaveAndApply() {
        VongQuayThoiVangConfig.idVatPhamQuay = ((Number) spnIdVatPhamQuay.getValue()).intValue();
        VongQuayThoiVangConfig.soLuongVatPhamQuay = ((Number) spnSoLuongVatPhamQuay.getValue()).intValue();
        VongQuayThoiVangConfig.mocMoQuaHiem = ((Number) spnMocMoQuaHiem.getValue()).intValue();
        VongQuayThoiVangConfig.tiLeMoiMonSieuHiem = ((Number) spnTiLeSieuHiem.getValue()).intValue();
        VongQuayThoiVangConfig.tiLeNhom5 = ((Number) spnTiLeNhom5.getValue()).intValue();
        VongQuayThoiVangConfig.tiLeNhom15 = ((Number) spnTiLeNhom15.getValue()).intValue();
        VongQuayThoiVangConfig.soLuongHongNgoc = ((Number) spnSoLuongHongNgoc.getValue()).intValue();
        VongQuayThoiVangConfig.vangMin = ((Number) spnVangMin.getValue()).intValue();
        VongQuayThoiVangConfig.vangMax = ((Number) spnVangMax.getValue()).intValue();

        VongQuayThoiVangConfig.idNhom15 = VongQuayThoiVangConfig.parseStringToIntArray(txtNhom15.getText(), VongQuayThoiVangConfig.idNhom15);
        VongQuayThoiVangConfig.idNhom5 = VongQuayThoiVangConfig.parseStringToIntArray(txtNhom5.getText(), VongQuayThoiVangConfig.idNhom5);
        VongQuayThoiVangConfig.idNhom5SauMoc = VongQuayThoiVangConfig.parseStringToIntArray(txtNhom5SauMoc.getText(), VongQuayThoiVangConfig.idNhom5SauMoc);
        VongQuayThoiVangConfig.idNhomSieuHiem = VongQuayThoiVangConfig.parseStringToIntArray(txtNhomSieuHiem.getText(), VongQuayThoiVangConfig.idNhomSieuHiem);
        VongQuayThoiVangConfig.idNhomSieuHiemSauMoc = VongQuayThoiVangConfig.parseStringToIntArray(txtNhomSieuHiemSauMoc.getText(), VongQuayThoiVangConfig.idNhomSieuHiemSauMoc);
        VongQuayThoiVangConfig.idVatPhamBoTro = VongQuayThoiVangConfig.parseStringToIntArray(txtBoTro.getText(), VongQuayThoiVangConfig.idVatPhamBoTro);

        VongQuayThoiVangConfig.moc1Spins = ((Number) spnMoc1Spins.getValue()).intValue();
        VongQuayThoiVangConfig.moc1RewardsStr = txtMoc1Rewards.getText().trim();

        VongQuayThoiVangConfig.moc2Spins = ((Number) spnMoc2Spins.getValue()).intValue();
        VongQuayThoiVangConfig.moc2RewardsStr = txtMoc2Rewards.getText().trim();

        VongQuayThoiVangConfig.moc3Spins = ((Number) spnMoc3Spins.getValue()).intValue();
        VongQuayThoiVangConfig.moc3RewardsStr = txtMoc3Rewards.getText().trim();

        VongQuayThoiVangConfig.moc4Spins = ((Number) spnMoc4Spins.getValue()).intValue();
        VongQuayThoiVangConfig.moc4RewardsStr = txtMoc4Rewards.getText().trim();

        boolean ok = VongQuayThoiVangConfig.save();
        if (ok) {
            lblStatusMsg.setText("✓ Đã lưu cấu hình và tự động đổi trực tiếp trong code Java (VongQuayThoiVangConfig.java) thành công! (" + new java.util.Date() + ")");
            lblStatusMsg.setForeground(SUCCESS);
        } else {
            lblStatusMsg.setText("Lỗi: Không thể lưu file data/config/vong_quay_thoi_vang.properties");
            lblStatusMsg.setForeground(DANGER);
        }
    }

    private void onResetDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn khôi phục toàn bộ Vòng Quay & Mốc Thượng Đế về giá trị mặc định?",
                "Xác nhận", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm == JOptionPane.YES_OPTION) {
            VongQuayThoiVangConfig.resetDefaults();
            VongQuayThoiVangConfig.save();
            loadValuesFromConfig();
            lblStatusMsg.setText("Đã khôi phục toàn bộ Vòng Quay & Mốc Thượng Đế về mặc định!");
            lblStatusMsg.setForeground(SUCCESS);
        }
    }

    private void onResetMocAllServer() {
        Object[] options = {
            "Đặt lại Cả Lượt & Mốc (Về 0)",
            "Chỉ đặt lại Mốc (Giữ nguyên lượt)",
            "Hủy bỏ"
        };
        int choice = JOptionPane.showOptionDialog(
                this,
                "<html><b style='font-size:13px; color:#ef4444;'>CẢNH BÁO: Thao tác này sẽ áp dụng cho TOÀN BỘ NGƯỜI CHƠI TRÊN SERVER!</b><br><br>"
                + "Vui lòng chọn hình thức đặt lại:<br>"
                + "• <b>Đặt lại Cả Lượt & Mốc:</b> Đặt lại cả số lượt quay tích lũy và trạng thái đã nhận mốc về 0 (bắt đầu lại vòng quay từ đầu).<br>"
                + "• <b>Chỉ đặt lại Mốc:</b> Giữ nguyên số lượt quay tích lũy của người chơi, chỉ xóa trạng thái đã nhận mốc (cho phép nhận lại quà mốc mới).<br><br>"
                + "<i>Dữ liệu của cả người chơi đang Online và trong Database sẽ được cập nhật ngay lập tức.</i></html>",
                "Xác nhận Đặt lại mốc Vòng Quay Thượng Đế toàn Server",
                JOptionPane.YES_NO_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE,
                null,
                options,
                options[2]
        );

        if (choice == 0) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có CHẮC CHẮN muốn đặt lại CẢ LƯỢT QUAY & MỐC ĐÃ NHẬN của toàn bộ người chơi về 0 không?",
                    "Xác nhận lần cuối",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                executeResetMocAllServer(true);
            }
        } else if (choice == 1) {
            int confirm = JOptionPane.showConfirmDialog(
                    this,
                    "Bạn có CHẮC CHẮN muốn đặt lại MỐC ĐÃ NHẬN (giữ nguyên số lượt quay) của toàn bộ người chơi không?",
                    "Xác nhận lần cuối",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE
            );
            if (confirm == JOptionPane.YES_OPTION) {
                executeResetMocAllServer(false);
            }
        }
    }

    private void executeResetMocAllServer(boolean resetSpinsAndMoc) {
        new Thread(() -> {
            try {
                // 1. Cập nhật người chơi đang Online
                int onlineUpdated = 0;
                if (server.Client.gI() != null && server.Client.gI().getPlayers() != null) {
                    for (models.player.Player pl : server.Client.gI().getPlayers()) {
                        if (pl != null) {
                            if (resetSpinsAndMoc) {
                                pl.lixi = 0;
                            }
                            pl.mocVongQuay = 0;
                            database.daos.PlayerDAO.updatePlayer(pl);
                            services.Service.gI().sendThongBao(pl, "Mốc Vòng Quay Thượng Đế đã được đặt lại!");
                            onlineUpdated++;
                        }
                    }
                }

                // 2. Cập nhật Database toàn bộ người chơi
                int dbUpdated = 0;
                try (java.sql.Connection conn = DBConnecter.getConnectionServer()) {
                    String sql;
                    if (resetSpinsAndMoc) {
                        sql = "UPDATE player SET dien_sukien = CONCAT("
                                + "'[', "
                                + "COALESCE(JSON_EXTRACT(dien_sukien, '$[0]'), 0), ',', "
                                + "COALESCE(JSON_EXTRACT(dien_sukien, '$[1]'), 0), ',', "
                                + "'0,0]')";
                    } else {
                        sql = "UPDATE player SET dien_sukien = CONCAT("
                                + "'[', "
                                + "COALESCE(JSON_EXTRACT(dien_sukien, '$[0]'), 0), ',', "
                                + "COALESCE(JSON_EXTRACT(dien_sukien, '$[1]'), 0), ',', "
                                + "COALESCE(JSON_EXTRACT(dien_sukien, '$[2]'), 0), ',0]')";
                    }
                    try (java.sql.PreparedStatement ps = conn.prepareStatement(sql)) {
                        dbUpdated = ps.executeUpdate();
                    }
                }

                // 3. Làm mới BXH Top quay nếu reset cả lượt
                if (resetSpinsAndMoc) {
                    services.top.TopQuay.getInstance().load(true);
                }

                final int fOnline = onlineUpdated;
                final int fDb = dbUpdated;
                final String modeText = resetSpinsAndMoc ? "cả Lượt quay & Mốc nhận thưởng" : "Mốc nhận thưởng";

                SwingUtilities.invokeLater(() -> {
                    lblStatusMsg.setText("✓ Đã đặt lại " + modeText + " cho toàn server thành công! (" + fDb + " tài khoản DB, " + fOnline + " online)");
                    lblStatusMsg.setForeground(SUCCESS);
                    JOptionPane.showMessageDialog(this,
                            "Đặt lại thành công!\n"
                            + "- Chế độ: Đặt lại " + modeText + "\n"
                            + "- Số người chơi Online đã cập nhật: " + fOnline + "\n"
                            + "- Số bản ghi Database đã cập nhật: " + fDb,
                            "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                });

            } catch (Exception ex) {
                ex.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    lblStatusMsg.setText("Lỗi khi đặt lại mốc: " + ex.getMessage());
                    lblStatusMsg.setForeground(DANGER);
                    JOptionPane.showMessageDialog(this,
                            "Đã xảy ra lỗi khi đặt lại mốc toàn server:\n" + ex.getMessage(),
                            "Lỗi", JOptionPane.ERROR_MESSAGE);
                });
            }
        }).start();
    }
}
