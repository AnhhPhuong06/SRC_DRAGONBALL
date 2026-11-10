package server.ui;

import services.CombineRateConfig;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.DecimalFormat;

import static server.ui.ServerGuiUtils.*;

public class CombineRatePanel extends JPanel {

    // 1. Nâng cấp trang bị bằng Đá (+1 đến +8)
    private final JSpinner[] spnNangCapDo = new JSpinner[8];

    // 2. Pha lê hóa trang bị (1 đến 10 sao)
    private final JSpinner[] spnPhaLeHoa = new JSpinner[10];

    // 3. Tỉ lệ SKH
    private JSpinner spnSkhVip1;
    private JSpinner spnSkhVip2;
    private JSpinner spnSkhThuong;
    private JLabel lblSkhTotal;

    // 4. Các loại đập đồ khác
    private JSpinner spnBongTai2;
    private JSpinner spnBongTai3;
    private JSpinner spnTrungDeTu1;
    private JSpinner spnTrungDeTu2;
    private JSpinner spnTrungDeTu3;
    private JSpinner spnZeno;

    private JLabel lblStatusMsg;

    public CombineRatePanel() {
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

        JButton btnReset = button("Khôi phục mặc định", ButtonVariant.SECONDARY, "refresh");
        btnReset.addActionListener(e -> onResetDefaults());

        JButton btnSave = button("Lưu & Áp dụng ngay", ButtonVariant.SUCCESS, "check");
        btnSave.addActionListener(e -> onSaveAndApply());

        headerActions.add(btnReset);
        headerActions.add(btnSave);

        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(sectionHeader("Quản Lý Tỉ Lệ Đập Đồ",
                "Chỉnh sửa trực tiếp tỉ lệ % thành công. Khi sửa sẽ TỰ ĐỘNG ĐỔI TRỰC TIẾP VÀO CODE JAVA (src/services/CombineRateConfig.java). Áp dụng ngay không cần restart server.",
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

        contentBox.add(buildCardNangCapDo());
        contentBox.add(Box.createVerticalStrut(SP_LG));

        contentBox.add(buildCardPhaLeHoa());
        contentBox.add(Box.createVerticalStrut(SP_LG));

        contentBox.add(buildCardSKH());
        contentBox.add(Box.createVerticalStrut(SP_LG));

        contentBox.add(buildCardKhac());
        contentBox.add(Box.createVerticalStrut(SP_XL));

        JScrollPane scroll = new JScrollPane(contentBox);
        scroll.setBorder(null);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        add(scroll, BorderLayout.CENTER);
    }

    /** Card 1: Nâng cấp trang bị bằng Đá (+1 đến +8) */
    private JPanel buildCardNangCapDo() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("1. Nâng cấp trang bị bằng Đá (+1 đến +8)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("box", 18, PRIMARY));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Tỉ lệ % đập đồ (Áo, Quần, Găng, Giày, Rada) bằng đá nâng cấp. (Hàm CombineUtil.getTileNangCapDo)", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 4, SP_LG, SP_MD));
        grid.setOpaque(false);

        for (int i = 0; i < 8; i++) {
            JPanel itemBox = new JPanel(new BorderLayout(SP_SM, 0));
            itemBox.setOpaque(false);
            itemBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD)
            ));

            JLabel lbl = label("Cấp +" + (i + 1) + ":", FONT_BODY_B, TEXT_SECONDARY);
            spnNangCapDo[i] = new JSpinner(new SpinnerNumberModel(80.0, 0.01, 100.0, 0.5));
            styleSpinner(spnNangCapDo[i]);

            JLabel unit = label("%", FONT_BODY, TEXT_MUTED);

            JPanel right = new JPanel(new BorderLayout(SP_XS, 0));
            right.setOpaque(false);
            right.add(spnNangCapDo[i], BorderLayout.CENTER);
            right.add(unit, BorderLayout.EAST);

            itemBox.add(lbl, BorderLayout.WEST);
            itemBox.add(right, BorderLayout.CENTER);
            grid.add(itemBox);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Card 2: Pha lê hóa trang bị (1 đến 9 sao) */
    private JPanel buildCardPhaLeHoa() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("2. Pha lê hóa trang bị (1 đến 9 sao)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("diamond", 18, VIOLET));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Tỉ lệ mở ô Sao Pha Lê từ 1 sao đến 9 sao. (Hàm PhaLeHoaTrangBi.getRatio)", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 5, SP_LG, SP_MD));
        grid.setOpaque(false);

        for (int i = 0; i < 10; i++) {
            JPanel itemBox = new JPanel(new BorderLayout(SP_SM, 0));
            itemBox.setOpaque(false);
            itemBox.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER, 1, true),
                    new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD)
            ));

            String saoText = (i < 9) ? (i + 1) + " Sao:" : "10 Sao:";
            JLabel lbl = label(saoText, FONT_BODY_B, TEXT_SECONDARY);
            spnPhaLeHoa[i] = new JSpinner(new SpinnerNumberModel(50.0, 0.01, 100.0, 0.5));
            styleSpinner(spnPhaLeHoa[i]);

            JLabel unit = label("%", FONT_BODY, TEXT_MUTED);

            JPanel right = new JPanel(new BorderLayout(SP_XS, 0));
            right.setOpaque(false);
            right.add(spnPhaLeHoa[i], BorderLayout.CENTER);
            right.add(unit, BorderLayout.EAST);

            itemBox.add(lbl, BorderLayout.WEST);
            itemBox.add(right, BorderLayout.CENTER);
            grid.add(itemBox);
        }

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    /** Card 3: Tỉ lệ ra Set Kích Hoạt */
    private JPanel buildCardSKH() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("3. Tỉ lệ ra loại Set Kích Hoạt (SKH Thường & VIP)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("bolt", 18, WARNING));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Trọng số xác suất rơi ra SKH VIP 1, SKH VIP 2, SKH Thường khi đập đồ SKH hoặc dùng capsule SKH. (Hàm ItemService.randomSKHId)", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_LG, 0));
        row.setOpaque(false);

        // SKH VIP 1
        row.add(createInputBox("SKH VIP 1 (Kakarot, Pikkoro, Cađic):", spnSkhVip1 = new JSpinner(new SpinnerNumberModel(25, 1, 1000, 1))));
        // SKH VIP 2
        row.add(createInputBox("SKH VIP 2 (Songoku, Ốc Tiêu, Nappa):", spnSkhVip2 = new JSpinner(new SpinnerNumberModel(35, 1, 1000, 1))));
        // SKH Thường
        row.add(createInputBox("SKH Thường (Kirin, Kamê, v.v.):", spnSkhThuong = new JSpinner(new SpinnerNumberModel(40, 1, 1000, 1))));

        lblSkhTotal = label("Tổng: 100", FONT_BODY_B, SUCCESS);
        lblSkhTotal.setBorder(new EmptyBorder(0, SP_MD, 0, 0));
        row.add(lblSkhTotal);

        autoUpdateSkhTotal();

        card.add(row, BorderLayout.CENTER);
        return card;
    }

    /** Card 4: Các loại đập đồ khác */
    private JPanel buildCardKhac() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = label("4. Các loại đập đồ khác (Bông tai, Trứng đệ, Zeno)", FONT_H2, TEXT);
        title.setIcon(vectorIcon("settings", 18, SUCCESS));
        title.setIconTextGap(SP_SM);
        JLabel sub = label("Tỉ lệ thành công của các chức năng ghép đồ và nâng cấp khác.", FONT_SMALL, TEXT_MUTED);
        head.add(title, BorderLayout.NORTH);
        head.add(sub, BorderLayout.SOUTH);
        card.add(head, BorderLayout.NORTH);

        JPanel grid = new JPanel(new GridLayout(2, 3, SP_LG, SP_MD));
        grid.setOpaque(false);

        grid.add(createInputBox("Bông Tai Cấp 2 (%):", spnBongTai2 = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1))));
        grid.add(createInputBox("Bông Tai Cấp 3 (%):", spnBongTai3 = new JSpinner(new SpinnerNumberModel(20, 1, 100, 1))));
        grid.add(createInputBox("Nâng Cấp Zeno (%):", spnZeno = new JSpinner(new SpinnerNumberModel(30, 1, 100, 1))));

        grid.add(createInputBox("Trứng Mabư -> Ubu (%):", spnTrungDeTu1 = new JSpinner(new SpinnerNumberModel(80, 1, 100, 1))));
        grid.add(createInputBox("Trứng Ubu -> Black (%):", spnTrungDeTu2 = new JSpinner(new SpinnerNumberModel(70, 1, 100, 1))));
        grid.add(createInputBox("Trứng Black -> Beerus (%):", spnTrungDeTu3 = new JSpinner(new SpinnerNumberModel(50, 1, 100, 1))));

        card.add(grid, BorderLayout.CENTER);
        return card;
    }

    private JPanel createInputBox(String title, JSpinner spinner) {
        JPanel box = new JPanel(new BorderLayout(SP_SM, 0));
        box.setOpaque(false);
        box.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                new EmptyBorder(SP_SM, SP_MD, SP_SM, SP_MD)
        ));

        JLabel lbl = label(title, FONT_BODY_B, TEXT_SECONDARY);
        styleSpinner(spinner);

        box.add(lbl, BorderLayout.WEST);
        box.add(spinner, BorderLayout.CENTER);
        return box;
    }

    private void autoUpdateSkhTotal() {
        javax.swing.event.ChangeListener cl = e -> {
            int v1 = (int) spnSkhVip1.getValue();
            int v2 = (int) spnSkhVip2.getValue();
            int v3 = (int) spnSkhThuong.getValue();
            int total = v1 + v2 + v3;
            lblSkhTotal.setText("Tổng: " + total + (total == 100 ? " (Chuẩn 100%)" : ""));
            lblSkhTotal.setForeground(total == 100 ? SUCCESS : WARNING);
        };
        spnSkhVip1.addChangeListener(cl);
        spnSkhVip2.addChangeListener(cl);
        spnSkhThuong.addChangeListener(cl);
    }

    private void styleSpinner(JSpinner s) {
        s.setPreferredSize(new Dimension(80, 32));
        JComponent ed = s.getEditor();
        if (ed instanceof JSpinner.DefaultEditor de) {
            de.getTextField().setFont(FONT_BODY_B);
            de.getTextField().setHorizontalAlignment(JTextField.RIGHT);
        }
    }

    private void loadValuesFromConfig() {
        CombineRateConfig.load();

        // 1. Nâng cấp đồ
        for (int i = 0; i < 8; i++) {
            if (spnNangCapDo[i] != null) {
                spnNangCapDo[i].setValue(CombineRateConfig.rateNangCapDo[i]);
            }
        }
        // 2. Pha lê hóa
        for (int i = 0; i < 10; i++) {
            if (spnPhaLeHoa[i] != null) {
                spnPhaLeHoa[i].setValue((double) CombineRateConfig.ratePhaLeHoa[i]);
            }
        }
        // 3. SKH
        if (spnSkhVip1 != null) spnSkhVip1.setValue(CombineRateConfig.skhVip1);
        if (spnSkhVip2 != null) spnSkhVip2.setValue(CombineRateConfig.skhVip2);
        if (spnSkhThuong != null) spnSkhThuong.setValue(CombineRateConfig.skhThuong);

        // 4. Khác
        if (spnBongTai2 != null) spnBongTai2.setValue(CombineRateConfig.rateBongTai2);
        if (spnBongTai3 != null) spnBongTai3.setValue(CombineRateConfig.rateBongTai3);
        if (spnTrungDeTu1 != null) spnTrungDeTu1.setValue(CombineRateConfig.rateTrungDeTu1);
        if (spnTrungDeTu2 != null) spnTrungDeTu2.setValue(CombineRateConfig.rateTrungDeTu2);
        if (spnTrungDeTu3 != null) spnTrungDeTu3.setValue(CombineRateConfig.rateTrungDeTu3);
        if (spnZeno != null) spnZeno.setValue(CombineRateConfig.rateZeno);
    }

    private void onSaveAndApply() {
        try {
            // 1. Nâng cấp đồ
            for (int i = 0; i < 8; i++) {
                CombineRateConfig.rateNangCapDo[i] = ((Number) spnNangCapDo[i].getValue()).doubleValue();
            }
            // 2. Pha lê hóa
            for (int i = 0; i < 10; i++) {
                CombineRateConfig.ratePhaLeHoa[i] = ((Number) spnPhaLeHoa[i].getValue()).floatValue();
            }
            // 3. SKH
            CombineRateConfig.skhVip1 = (int) spnSkhVip1.getValue();
            CombineRateConfig.skhVip2 = (int) spnSkhVip2.getValue();
            CombineRateConfig.skhThuong = (int) spnSkhThuong.getValue();

            // 4. Khác
            CombineRateConfig.rateBongTai2 = (int) spnBongTai2.getValue();
            CombineRateConfig.rateBongTai3 = (int) spnBongTai3.getValue();
            CombineRateConfig.rateTrungDeTu1 = (int) spnTrungDeTu1.getValue();
            CombineRateConfig.rateTrungDeTu2 = (int) spnTrungDeTu2.getValue();
            CombineRateConfig.rateTrungDeTu3 = (int) spnTrungDeTu3.getValue();
            CombineRateConfig.rateZeno = (int) spnZeno.getValue();

            boolean ok = CombineRateConfig.save();
            if (ok) {
                lblStatusMsg.setText("✓ Đã lưu và tự động đổi trực tiếp trong code Java (CombineRateConfig.java) thành công! (" + new java.util.Date() + ")");
                lblStatusMsg.setForeground(SUCCESS);
                JOptionPane.showMessageDialog(this,
                        "Đã lưu và áp dụng toàn bộ tỉ lệ đập đồ thành công!\n(Đã tự động đổi và đồng bộ trực tiếp vào file code Java: CombineRateConfig.java)",
                        "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            } else {
                lblStatusMsg.setText("⚠ Có lỗi xảy ra khi ghi file cấu hình!");
                lblStatusMsg.setForeground(DANGER);
            }
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Lỗi: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void onResetDefaults() {
        int confirm = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn khôi phục tất cả tỉ lệ về giá trị mặc định ban đầu?",
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            CombineRateConfig.resetDefaults();
            loadValuesFromConfig();
            lblStatusMsg.setText("✓ Đã khôi phục về tỉ lệ mặc định!");
            lblStatusMsg.setForeground(INFO);
        }
    }
}
