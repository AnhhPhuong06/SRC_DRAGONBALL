package server.ui;

import Bot.BotConfig;
import Bot.BotManager;
import models.intrinsic.Intrinsic;
import services.player.IntrinsicService;

import javax.swing.*;
import java.awt.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import static server.ui.ServerGuiUtils.*;

/**
 * Bảng điều khiển Bot AI.
 *
 * <p>Tạo bot tự động (train quái / săn boss), tùy chỉnh chỉ số + trang phục +
 * nội tại hoặc dùng mặc định. Bot tự di chuyển, tự đánh quái/boss, tự nhặt đồ và
 * chat thế giới (do {@link Bot.BotManager} điều khiển). Bot hệ Trái Đất khi thấy
 * boss "ăn được" sẽ dịch chuyển tức thời rồi bắn Kame.
 */
public class BotAIPanel extends JPanel {

    // Phải khớp switch trong Bot.update(): 0=Mobb train quái, 1=Nappa, 2=Tương Lai,
    // 3=Cold, 4=Sanb săn boss. Trước đây săn boss để 2 nên bấm "Săn Boss" lại ra bot
    // Tương Lai.
    private static final int TYPE_TRAIN_MOB = 0;
    private static final int TYPE_HUNT_BOSS = 4;
    private static final int TYPE_LIKE_PLAYER = Bot.Bot.TYPE_NHU_PLAYER;

    private JTextArea txtHistory;
    private JLabel lblBotCount;
    private JComboBox<String> cboType;
    private JSpinner spnCount;

    // Tùy chỉnh
    private JToggleButton chkCustom;
    private JComboBox<String> cboGender;
    private JComboBox<String> cboIntrinsic;
    private final List<Integer> intrinsicIds = new ArrayList<>();
    private JTextField txtHp, txtDame, txtMp, txtPower, txtTiemNang;
    private JSpinner spnCrit, spnDef;
    private JTextField txtHead, txtBody, txtLeg;
    private JPanel customPanel;
    private JPanel customFields;
    private JCheckBox chkAccMoi, chkPet, chkPk, chkChat;
    private JSpinner spnMaxBot;

    // Bảng theo dõi bot
    private JTable tblBots;
    private javax.swing.table.DefaultTableModel modelBots;
    private JLabel lblBotTableInfo;

    public BotAIPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBorder(pad(SP_LG, SP_XL, SP_XL, SP_XL));
        setBackground(BG);
        initUI();
        startCountUpdater();
    }

    private void initUI() {
        txtHistory = new JTextArea();
        JScrollPane scrollLog = styleConsole(txtHistory);
        scrollLog.setPreferredSize(new Dimension(0, 200));

        JPanel logCard = card(new BorderLayout());
        logCard.add(sectionHeader("Lịch sử hoạt động", "Nhật ký thao tác của Bot AI", null), BorderLayout.NORTH);
        logCard.add(scrollLog, BorderLayout.CENTER);

        JPanel stack = new JPanel();
        stack.setLayout(new BoxLayout(stack, BoxLayout.Y_AXIS));
        stack.setOpaque(false);
        for (JPanel c : new JPanel[]{buildCreatePanel(), buildCustomPanel(), buildBotTablePanel(), logCard}) {
            c.setAlignmentX(LEFT_ALIGNMENT);
            if (stack.getComponentCount() > 0) {
                stack.add(Box.createVerticalStrut(SP_SM));
            }
            stack.add(c);
        }

        // Cho các thẻ giữ đúng chiều cao tự nhiên (đặt ở NORTH) rồi bọc thanh cuộn:
        // cửa sổ thấp thì cuộn, thay vì bóp ô nhật ký thành một vạch đen như trước.
        JPanel content = new JPanel(new BorderLayout());
        content.setOpaque(false);
        content.add(stack, BorderLayout.NORTH);

        JScrollPane page = new JScrollPane(content,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        page.setBorder(null);
        page.setOpaque(false);
        page.getViewport().setOpaque(false);
        page.getVerticalScrollBar().setUnitIncrement(16);

        add(page, BorderLayout.CENTER);

        setCustomEnabled(false);
        log("Bảng điều khiển Bot AI đã sẵn sàng.");
    }

    private JPanel buildCreatePanel() {
        JPanel control = card(new GridBagLayout());

        GridBagConstraints gc = baseGc();

        lblBotCount = dotBadge("Bot đang hoạt động: 0", SUCCESS);
        JPanel badgeWrap = new JPanel(new GridBagLayout());
        badgeWrap.setOpaque(false);
        badgeWrap.add(lblBotCount);

        gc.gridx = 0; gc.gridy = 0; gc.gridwidth = 6;
        gc.fill = GridBagConstraints.HORIZONTAL; gc.weightx = 1;
        control.add(sectionHeader("Tạo Bot AI",
                "Tạo bot tự động train quái / săn boss", badgeWrap), gc);
        gc.fill = GridBagConstraints.NONE; gc.weightx = 0; gc.gridwidth = 1;

        gc.gridx = 0; gc.gridy = 1;
        control.add(plain("Loại bot:"), gc);
        cboType = new JComboBox<>(new String[]{
                "Như người chơi thật (train + đệ tử + boss + PK)",
                "Train quái (tự đánh quái + nhặt đồ)",
                "Săn Boss (tự tìm & đánh boss)"
        });
        gc.gridx = 1; gc.gridy = 1;
        control.add(cboType, gc);

        gc.gridx = 2; gc.gridy = 1;
        control.add(plain("Số lượng:"), gc);
        spnCount = new JSpinner(new SpinnerNumberModel(10, 1, BotManager.MAX_BOT_TOI_DA, 1));
        JSpinner.NumberEditor edCount = new JSpinner.NumberEditor(spnCount, "#0"); // bỏ dấu phẩy "1,000"
        spnCount.setEditor(edCount);
        edCount.getTextField().setColumns(5);
        gc.gridx = 3; gc.gridy = 1;
        control.add(spnCount, gc);

        gc.gridx = 4; gc.gridy = 1;
        control.add(plain("Trần bot:"), gc);
        spnMaxBot = new JSpinner(new SpinnerNumberModel(
                BotManager.MAX_BOT, 1, BotManager.MAX_BOT_TOI_DA, 50));
        JSpinner.NumberEditor edMax = new JSpinner.NumberEditor(spnMaxBot, "#0");
        spnMaxBot.setEditor(edMax);
        edMax.getTextField().setColumns(5);
        spnMaxBot.setToolTipText("Số bot tối đa cho phép tồn tại cùng lúc. "
                + "Máy khoẻ thì tăng, thấy giật thì giảm.");
        spnMaxBot.addChangeListener(e -> {
            int moi = BotManager.datTranBot((Integer) spnMaxBot.getValue());
            log("Đã đặt trần bot = " + moi);
        });
        gc.gridx = 5; gc.gridy = 1;
        control.add(spnMaxBot, gc);

        chkAccMoi = new JCheckBox("Như acc mới (tự cày lên)", true);
        chkAccMoi.setToolTipText("Bot sinh ra yếu và tay trắng, tự cày → mua đồ → đập/ép "
                + "→ đủ mạnh mới săn boss. Bỏ chọn thì bot mạnh sẵn ngay từ đầu.");
        chkPet = new JCheckBox("Nuôi đệ tử", true);
        chkPk = new JCheckBox("Phá người chơi (PK)", false);
        chkChat = new JCheckBox("Chat thế giới", true);
        JPanel hanhVi = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        hanhVi.setOpaque(false);
        for (JCheckBox cb : new JCheckBox[]{chkAccMoi, chkPet, chkPk, chkChat}) {
            cb.setOpaque(false);
            cb.setFont(FONT_SMALL_B);
            cb.setForeground(TEXT_SECONDARY);
            hanhVi.add(cb);
        }
        gc.gridx = 0; gc.gridy = 2; gc.gridwidth = 6;
        control.add(hanhVi, gc);
        gc.gridwidth = 1;

        chkCustom = toggle("Tùy chỉnh chỉ số / trang phục / nội tại (bỏ chọn = dùng mặc định)");
        chkCustom.addActionListener(e -> setCustomEnabled(chkCustom.isSelected()));
        gc.gridx = 0; gc.gridy = 3; gc.gridwidth = 6;
        control.add(chkCustom, gc);
        gc.gridwidth = 1;

        JButton btnCreate = button("TẠO BOT", ButtonVariant.PRIMARY, "bot");
        btnCreate.addActionListener(e -> onCreate());

        JButton btnClear = button("XÓA TẤT CẢ BOT", ButtonVariant.DANGER, "broom");
        btnClear.addActionListener(e -> onClear());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        buttons.setOpaque(false);
        buttons.add(btnCreate);
        buttons.add(btnClear);
        gc.gridx = 0; gc.gridy = 4; gc.gridwidth = 6;
        control.add(buttons, gc);
        gc.gridwidth = 1;

        JLabel hint = new JLabel("<html>(*) Bot tự di chuyển, đánh quái/boss và tự đổi map. Không cần restart. "
                + "Trần bot chỉnh được ngay bên trên (tối đa " + BotManager.MAX_BOT_TOI_DA + "); "
                + "mỗi bot là một nhân vật thật trong khu nên càng nhiều càng nặng — "
                + "nếu log báo \"Quá tải\" thì giảm bớt.</html>");
        hint.setFont(FONT_SMALL);
        hint.setForeground(TEXT_MUTED);
        gc.gridx = 0; gc.gridy = 5; gc.gridwidth = 6;
        control.add(hint, gc);

        return control;
    }

    private JPanel buildCustomPanel() {
        customPanel = card(new BorderLayout());
        customPanel.add(sectionHeader("Chỉ số / Trang phục / Nội tại tùy chỉnh"), BorderLayout.NORTH);

        // Các ô nhập nằm riêng khỏi tiêu đề: khi bỏ chọn tùy chỉnh chỉ làm mờ ô nhập,
        // tiêu đề vẫn đọc được (trước đây mờ cả tiêu đề nên nhìn như panel bị hỏng).
        customFields = new JPanel(new GridBagLayout());
        customFields.setOpaque(false);
        customPanel.add(customFields, BorderLayout.CENTER);

        GridBagConstraints gc = baseGc();

        // Giới tính
        gc.gridx = 0; gc.gridy = 1;
        customFields.add(plain("Giới tính:"), gc);
        cboGender = new JComboBox<>(new String[]{"Ngẫu nhiên", "Trái Đất", "Namek", "Xayda"});
        cboGender.addActionListener(e -> populateIntrinsics());
        gc.gridx = 1; gc.gridy = 1;
        customFields.add(cboGender, gc);

        // Nội tại
        gc.gridx = 2; gc.gridy = 1;
        customFields.add(plain("Nội tại:"), gc);
        cboIntrinsic = new JComboBox<>();
        cboIntrinsic.setPrototypeDisplayValue("xxxxxxxxxxxxxxxxxxxxxxxxxxxxxx");
        gc.gridx = 3; gc.gridy = 1;
        customFields.add(cboIntrinsic, gc);

        // HP - Sát thương
        gc.gridx = 0; gc.gridy = 2;
        customFields.add(plain("HP tối đa:"), gc);
        txtHp = numField(BotConfig.DEF_HP);
        gc.gridx = 1; gc.gridy = 2;
        customFields.add(txtHp, gc);

        gc.gridx = 2; gc.gridy = 2;
        customFields.add(plain("Sát thương:"), gc);
        txtDame = numField(BotConfig.DEF_DAME);
        gc.gridx = 3; gc.gridy = 2;
        customFields.add(txtDame, gc);

        // MP - Sức mạnh
        gc.gridx = 0; gc.gridy = 3;
        customFields.add(plain("MP tối đa:"), gc);
        txtMp = numField(BotConfig.DEF_MP);
        gc.gridx = 1; gc.gridy = 3;
        customFields.add(txtMp, gc);

        gc.gridx = 2; gc.gridy = 3;
        customFields.add(plain("Sức mạnh:"), gc);
        txtPower = numField(BotConfig.DEF_POWER);
        gc.gridx = 3; gc.gridy = 3;
        customFields.add(txtPower, gc);

        // Tiềm năng - Chí mạng
        gc.gridx = 0; gc.gridy = 4;
        customFields.add(plain("Tiềm năng:"), gc);
        txtTiemNang = numField(BotConfig.DEF_TIEMNANG);
        gc.gridx = 1; gc.gridy = 4;
        customFields.add(txtTiemNang, gc);

        gc.gridx = 2; gc.gridy = 4;
        customFields.add(plain("Chí mạng (%):"), gc);
        spnCrit = new JSpinner(new SpinnerNumberModel(BotConfig.DEF_CRIT, 0, 100, 1));
        gc.gridx = 3; gc.gridy = 4;
        customFields.add(spnCrit, gc);

        // Def
        gc.gridx = 0; gc.gridy = 5;
        customFields.add(plain("Giáp (def):"), gc);
        spnDef = new JSpinner(new SpinnerNumberModel(BotConfig.DEF_DEF, 0, 1000000, 1));
        gc.gridx = 1; gc.gridy = 5;
        customFields.add(spnDef, gc);

        // Trang phục
        gc.gridx = 0; gc.gridy = 6;
        customFields.add(plain("Trang phục (id, -1 = ngẫu nhiên):"), gc);
        JPanel outfit = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        outfit.setOpaque(false);
        txtHead = numField(-1); txtHead.setColumns(5);
        txtBody = numField(-1); txtBody.setColumns(5);
        txtLeg = numField(-1); txtLeg.setColumns(5);
        outfit.add(plain("Đầu:")); outfit.add(txtHead);
        outfit.add(plain("Thân:")); outfit.add(txtBody);
        outfit.add(plain("Chân:")); outfit.add(txtLeg);
        gc.gridx = 1; gc.gridy = 6; gc.gridwidth = 3;
        customFields.add(outfit, gc);
        gc.gridwidth = 1;

        return customPanel;
    }

    /** Bảng theo dõi: mỗi bot đang ở map/khu nào, toạ độ, đang làm gì. */
    private JPanel buildBotTablePanel() {
        JPanel p = card(new BorderLayout());

        lblBotTableInfo = label("Chưa có bot nào", FONT_SMALL, TEXT_MUTED);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        right.setOpaque(false);
        right.add(lblBotTableInfo);
        p.add(sectionHeader("Bot đang ở đâu", "Vị trí và trạng thái từng bot (tự làm mới)", right),
                BorderLayout.NORTH);

        String[] cols = {"Tên", "Hệ", "Giai đoạn", "Map", "Khu", "Toạ độ", "Sức mạnh",
            "Vàng", "HP", "Đệ tử", "Nhiệm vụ", "Việc đang làm"};
        modelBots = new javax.swing.table.DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false; // chỉ để xem
            }
        };
        tblBots = new JTable(modelBots);
        JScrollPane sp = styleTable(tblBots);
        sp.setPreferredSize(new Dimension(0, 260));
        p.add(sp, BorderLayout.CENTER);
        return p;
    }

    private static final String[] TEN_HE = {"Trái Đất", "Namek", "Xayda"};

    /** Trần số dòng vẽ ra bảng — bảng nghìn dòng làm treo luồng giao diện. */
    private static final int MAX_DONG_BANG = 200;

    /** Đổ lại bảng bot. Gọi từ Swing Timer nên chạy trên luồng giao diện. */
    private void refreshBotTable() {
        java.util.List<Bot.Bot> ds;
        try {
            ds = new ArrayList<>(BotManager.gI().bot);
        } catch (Throwable t) {
            return;
        }
        // Giữ nguyên dòng đang chọn khi bảng được vẽ lại
        int chon = tblBots.getSelectedRow();
        modelBots.setRowCount(0);
        int soCoDeTu = 0;
        int soDong = 0;
        for (Bot.Bot b : ds) {
            try {
                boolean coDeTu = b.pet != null;
                if (coDeTu) {
                    soCoDeTu++;
                }
                // Dựng cả nghìn dòng mỗi giây trên luồng giao diện là treo panel;
                // vẫn đếm hết ở trên để con số tổng luôn đúng.
                if (soDong >= MAX_DONG_BANG) {
                    continue;
                }
                soDong++;
                modelBots.addRow(new Object[]{
                    b.name,
                    (b.gender >= 0 && b.gender < TEN_HE.length) ? TEN_HE[b.gender] : "?",
                    b.brain != null ? b.brain.tienTrinh.tenGiaiDoan() : "-",
                    tenMap(b),
                    b.zone != null ? b.zone.zoneId : "-",
                    b.location != null ? (b.location.x + ", " + b.location.y) : "-",
                    b.nPoint != null ? utils.Util.numberToMoney(b.nPoint.power) : "-",
                    b.inventory != null ? utils.Util.numberToMoney(b.inventory.gold) : "-",
                    b.nPoint != null ? utils.Util.numberToMoney(b.nPoint.hp) : "-",
                    coDeTu ? "Có" : "-",
                    tenNhiemVu(b),
                    viecDangLam(b)
                });
            } catch (Exception ignored) {
            }
        }
        if (chon >= 0 && chon < modelBots.getRowCount()) {
            tblBots.setRowSelectionInterval(chon, chon);
        }
        lblBotTableInfo.setText(ds.isEmpty() ? "Chưa có bot nào"
                : ds.size() + " bot • " + soCoDeTu + " có đệ tử"
                + (ds.size() > MAX_DONG_BANG ? " • hiện " + MAX_DONG_BANG + " dòng đầu" : ""));
    }

    /** Nhiệm vụ chính bot đang làm, kèm bước hiện tại — để kiểm chứng bot có tiến không. */
    private static String tenNhiemVu(Bot.Bot b) {
        try {
            if (b.playerTask == null || b.playerTask.taskMain == null) {
                return "-";
            }
            models.task.TaskMain tm = b.playerTask.taskMain;
            int tong = (tm.subTasks == null) ? 0 : tm.subTasks.size();
            String ten = (tm.name == null || tm.name.isEmpty()) ? ("NV " + tm.id) : tm.name;
            return ten + " (" + (tm.index + 1) + "/" + Math.max(tong, 1) + ")";
        } catch (Exception e) {
            return "-";
        }
    }

    private static String tenMap(Bot.Bot b) {
        try {
            if (b.zone == null || b.zone.map == null) {
                return "(chưa vào map)";
            }
            String ten = b.zone.map.mapName;
            return (ten == null || ten.isEmpty()) ? ("Map " + b.zone.map.mapId)
                    : (ten + " [" + b.zone.map.mapId + "]");
        } catch (Exception e) {
            return "-";
        }
    }

    /** Suy ra việc bot đang làm từ trạng thái hiện tại, cho admin nhìn phát biết ngay. */
    private static String viecDangLam(Bot.Bot b) {
        try {
            if (b.zone == null) {
                return "Đang vào map";
            }
            if (b.isDie()) {
                return "Đã chết, chờ hồi";
            }
            if (b.pet == null && b.cfg != null && b.cfg.nuoiDeTu) {
                return "Đang tìm đệ tử";
            }
            for (models.player.Player p : b.zone.getBosses()) {
                if (p != null && !p.isDie()) {
                    return "Đánh boss " + p.name;
                }
            }
            if (b.cfg != null && b.cfg.phaPlayer) {
                for (models.player.Player p : b.zone.getPlayers()) {
                    if (p != null && p != b && p.isPl() && !p.isDie()) {
                        return "PK " + p.name;
                    }
                }
            }
            return b.zone.mobs.isEmpty() ? "Rảnh / chờ đổi map" : "Train quái";
        } catch (Exception e) {
            return "-";
        }
    }

    // ---- Hành động ----

    private void onCreate() {
        int sel = cboType.getSelectedIndex();
        int type = (sel == 2) ? TYPE_HUNT_BOSS : (sel == 1) ? TYPE_TRAIN_MOB : TYPE_LIKE_PLAYER;
        String typeName = (sel == 2) ? "Săn Boss" : (sel == 1) ? "Train quái" : "Như người chơi thật";
        int count = (Integer) spnCount.getValue();

        // Cấu hình hành vi luôn được gửi kèm (không phụ thuộc nút "Tùy chỉnh chỉ số")
        BotConfig cfg = new BotConfig();
        cfg.nhuAccMoi = chkAccMoi.isSelected();
        cfg.nuoiDeTu = chkPet.isSelected();
        cfg.phaPlayer = chkPk.isSelected();
        cfg.chatTheGioi = chkChat.isSelected();
        if (chkCustom.isSelected() && !fillStats(cfg)) {
            return;
        }

        String moTaHanhVi = (cfg.nuoiDeTu ? "đệ tử, " : "") + (cfg.phaPlayer ? "PK, " : "")
                + (cfg.chatTheGioi ? "chat, " : "");
        int confirm = JOptionPane.showConfirmDialog(this,
                "Tạo " + count + " bot [" + typeName + "]"
                        + (cfg.custom ? " với chỉ số tùy chỉnh" : " (chỉ số mặc định)")
                        + (moTaHanhVi.isEmpty() ? "" : "\nHành vi: " + moTaHanhVi.substring(0, moTaHanhVi.length() - 2))
                        + " ?",
                "Xác nhận tạo Bot", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }

        try {
            int soTao = BotManager.gI().spawnBots(type, count, cfg);
            if (soTao < count) {
                log("Chỉ tạo được " + soTao + "/" + count + " bot (trần " + BotManager.MAX_BOT + " bot).");
            }
            log("Đang tạo " + soTao + " bot [" + typeName + "]"
                    + (cfg.custom ? " | HP=" + cfg.hp + " Dame=" + cfg.dame + " Power=" + cfg.power
                          + (cfg.intrinsicId >= 0 ? " | Nội tại #" + cfg.intrinsicId : "")
                          : " | chỉ số mặc định")
                    + (cfg.nuoiDeTu ? " | đệ tử" : "") + (cfg.phaPlayer ? " | PK" : "") + "...");
        } catch (IllegalStateException ex) {
            log(ex.getMessage());
            JOptionPane.showMessageDialog(this, ex.getMessage(),
                    "Không tạo được bot", JOptionPane.WARNING_MESSAGE);
        } catch (Throwable t) {
            log("LỖI khi tạo bot: " + t.getMessage());
            JOptionPane.showMessageDialog(this, "Lỗi tạo bot: " + t.getMessage(),
                    "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    /** Đổ chỉ số tùy chỉnh vào cfg. @return false nếu nhập sai (đã báo cho người dùng). */
    private boolean fillStats(BotConfig cfg) {
        cfg.custom = true;
        try {
            cfg.gender = cboGender.getSelectedIndex() - 1; // -1 = ngẫu nhiên
            cfg.hp = parsePositiveLong(txtHp, "HP tối đa");
            cfg.mp = parsePositiveLong(txtMp, "MP tối đa");
            cfg.dame = parsePositiveLong(txtDame, "Sát thương");
            cfg.power = parsePositiveLong(txtPower, "Sức mạnh");
            cfg.tiemNang = parseNonNegLong(txtTiemNang, "Tiềm năng");
            cfg.crit = (Integer) spnCrit.getValue();
            cfg.def = (Integer) spnDef.getValue();
            cfg.head = parseIntOrMinus1(txtHead);
            cfg.body = parseIntOrMinus1(txtBody);
            cfg.leg = parseIntOrMinus1(txtLeg);
            // Nội tại
            int sel = cboIntrinsic.getSelectedIndex();
            if (cboIntrinsic.isEnabled() && sel >= 0 && sel < intrinsicIds.size()) {
                cfg.intrinsicId = intrinsicIds.get(sel);
            } else {
                cfg.intrinsicId = -1;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, ex.getMessage(), "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    private void onClear() {
        int current = safeBotCount();
        if (current == 0) {
            JOptionPane.showMessageDialog(this, "Hiện không có bot nào.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        int confirm = JOptionPane.showConfirmDialog(this,
                "Xóa toàn bộ " + current + " bot khỏi server?",
                "Xác nhận xóa Bot", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (confirm != JOptionPane.YES_OPTION) {
            return;
        }
        try {
            int removed = BotManager.gI().clearAllBots();
            log("Đã xóa " + removed + " bot.");
        } catch (Throwable t) {
            log("LỖI khi xóa bot: " + t.getMessage());
        }
    }

    // ---- Nội tại ----

    /** Nạp danh sách nội tại theo giới tính đang chọn. */
    private void populateIntrinsics() {
        if (cboIntrinsic == null) {
            return;
        }
        cboIntrinsic.removeAllItems();
        intrinsicIds.clear();
        cboIntrinsic.addItem("Không áp nội tại");
        intrinsicIds.add(-1);

        int gIdx = cboGender.getSelectedIndex(); // 0 = ngẫu nhiên
        boolean enable = chkCustom.isSelected() && gIdx >= 1;
        if (gIdx >= 1) {
            try {
                List<Intrinsic> list = IntrinsicService.gI().getIntrinsics((byte) (gIdx - 1));
                if (list != null) {
                    for (Intrinsic it : list) {
                        if (it == null || it.id == 0) {
                            continue; // bỏ "chưa kích hoạt"
                        }
                        cboIntrinsic.addItem("#" + it.id + " - " + safeDesc(it));
                        intrinsicIds.add(it.id);
                    }
                }
            } catch (Exception ignored) {
            }
        }
        cboIntrinsic.setEnabled(enable);
        if (cboIntrinsic.getItemCount() > 0) {
            cboIntrinsic.setSelectedIndex(0);
        }
    }

    private static String safeDesc(Intrinsic it) {
        try {
            return it.getDescription();
        } catch (Exception e) {
            return it.name == null ? "" : it.name;
        }
    }

    // ---- Tiện ích ----

    private void setCustomEnabled(boolean en) {
        if (customFields != null) {
            setEnabledRecursive(customFields, en);
        }
        // Nội tại còn phụ thuộc giới tính -> nạp lại để áp đúng trạng thái enable
        populateIntrinsics();
    }

    private static void setEnabledRecursive(Container c, boolean en) {
        for (Component comp : c.getComponents()) {
            comp.setEnabled(en);
            if (comp instanceof Container) {
                setEnabledRecursive((Container) comp, en);
            }
        }
    }

    private long parsePositiveLong(JTextField f, String name) {
        long v = parseLongStrict(f, name);
        if (v < 1) {
            throw new NumberFormatException(name + " phải >= 1");
        }
        return v;
    }

    private long parseNonNegLong(JTextField f, String name) {
        long v = parseLongStrict(f, name);
        if (v < 0) {
            throw new NumberFormatException(name + " không được âm");
        }
        return v;
    }

    /** Chấp nhận "5000000", "5.000.000", "5,000,000"; từ chối số thập phân ("5.5"). */
    private long parseLongStrict(JTextField f, String name) {
        String raw = f.getText().trim().replace(" ", "").replace("_", "");
        if (raw.isEmpty()) {
            throw new NumberFormatException(name + " không được để trống");
        }
        String s;
        if (raw.matches("\\d+")) {
            s = raw;
        } else if (raw.matches("\\d{1,3}(\\.\\d{3})+")) {
            s = raw.replace(".", "");
        } else if (raw.matches("\\d{1,3}(,\\d{3})+")) {
            s = raw.replace(",", "");
        } else {
            throw new NumberFormatException(name + " không hợp lệ: \"" + f.getText() + "\" (chỉ nhập số nguyên)");
        }
        try {
            return Long.parseLong(s);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(name + " quá lớn hoặc không hợp lệ: \"" + f.getText() + "\"");
        }
    }

    private int parseIntOrMinus1(JTextField f) {
        String s = f.getText().trim();
        if (s.isEmpty()) {
            return -1;
        }
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int safeBotCount() {
        try {
            return BotManager.gI().getBotCount();
        } catch (Throwable t) {
            return 0;
        }
    }

    private void startCountUpdater() {
        Timer timer = new Timer(1000, e -> {
            lblBotCount.setText("Bot đang hoạt động: " + safeBotCount());
            // Bảng chỉ vẽ lại khi tab Bot AI đang hiển thị — panel ẩn mà vẫn dựng vài
            // trăm dòng mỗi giây là phí CPU của luồng giao diện.
            if (isShowing()) {
                refreshBotTable();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    private static GridBagConstraints baseGc() {
        GridBagConstraints gc = new GridBagConstraints();
        gc.insets = new Insets(6, SP_SM, 6, SP_SM);
        gc.anchor = GridBagConstraints.WEST;
        return gc;
    }

    private static JLabel plain(String text) {
        return label(text, FONT_SMALL_B, TEXT_SECONDARY);
    }

    private static JTextField numField(long value) {
        JTextField f = new JTextField(String.valueOf(value));
        f.setFont(FONT_BODY);
        f.setColumns(12);
        return f;
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtHistory.append("[" + time + "] " + msg + "\n");
        txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
    }
}
