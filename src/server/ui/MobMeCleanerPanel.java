package server.ui;

import services.MobMeCleanerService;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

import static server.ui.ServerGuiUtils.*;

public class MobMeCleanerPanel extends JPanel {

    private JToggleButton chkEnable;
    private JLabel lblStatus;
    private JLabel lblLastRun;
    private JLabel lblNextRun;
    private JLabel lblTotal;
    private final SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss dd/MM/yyyy");

    public MobMeCleanerPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));
        setBackground(BG);
        initUI();

        // Tự refresh trạng thái mỗi 2s
        Timer refreshTimer = new Timer(2000, e -> updateStatus());
        refreshTimer.start();
    }

    private void initUI() {
        // ===== Header =====
        JPanel header = new JPanel();
        header.setLayout(new BoxLayout(header, BoxLayout.Y_AXIS));
        header.setOpaque(false);
        header.add(sectionHeader("Dọn Rác Đẻ Trứng (MobMe)",
                "<html>Tự động dọn xác trứng (mobMe) còn sót sau khi player offline,"
                + " đổi map, hoặc mặc Pikkoro 5-set (mobMe bất tử).<br>"
                + "Chu kỳ cố định: <b>30 phút/lần</b>. Bật/tắt qua công tắc bên dưới.</html>", null));

        // ===== Control card =====
        JPanel controlCard = card(new BorderLayout(0, SP_MD));

        JLabel controlTitle = label("Cấu hình", FONT_H3, TEXT_SECONDARY);
        controlTitle.setIcon(vectorIcon("settings", 15, TEXT_SECONDARY));
        controlTitle.setIconTextGap(SP_SM);
        controlCard.add(controlTitle, BorderLayout.NORTH);

        JPanel controlRow = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_MD, 0));
        controlRow.setOpaque(false);

        chkEnable = toggle("Bật auto-clear (mỗi 30 phút)");
        chkEnable.setSelected(MobMeCleanerService.gI().isEnabled());
        chkEnable.addActionListener(e -> {
            MobMeCleanerService.gI().setEnabled(chkEnable.isSelected());
            updateStatus();
        });
        controlRow.add(chkEnable);

        JButton btnClean = button("DỌN NGAY (CHẠY 1 LẦN)", ButtonVariant.DANGER, "broom");
        btnClean.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Dọn ngay toàn bộ mobMe trên server?\nPlayer đang dùng skill Đẻ Trứng sẽ mất xác.",
                    "Xác nhận", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (confirm != JOptionPane.OK_OPTION) {
                return;
            }
            int n = MobMeCleanerService.gI().cleanAll();
            JOptionPane.showMessageDialog(this,
                    "Đã dọn " + n + " xác trứng.",
                    "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            updateStatus();
        });
        controlRow.add(btnClean);

        controlCard.add(controlRow, BorderLayout.CENTER);

        // ===== Status card =====
        JPanel statusCard = card(new BorderLayout(0, SP_MD));

        JLabel statusTitle = label("Trạng thái", FONT_H3, TEXT_SECONDARY);
        statusTitle.setIcon(vectorIcon("chart", 15, TEXT_SECONDARY));
        statusTitle.setIconTextGap(SP_SM);
        statusCard.add(statusTitle, BorderLayout.NORTH);

        JPanel statusPanel = new JPanel();
        statusPanel.setLayout(new BoxLayout(statusPanel, BoxLayout.Y_AXIS));
        statusPanel.setOpaque(false);

        lblStatus = makeLabel();
        lblStatus.setIcon(vectorIcon("power", 14, TEXT_MUTED));
        lblLastRun = makeLabel();
        lblLastRun.setIcon(vectorIcon("history", 14, TEXT_MUTED));
        lblNextRun = makeLabel();
        lblNextRun.setIcon(vectorIcon("clock", 14, TEXT_MUTED));
        lblTotal = makeLabel();
        lblTotal.setIcon(vectorIcon("broom", 14, TEXT_MUTED));
        statusPanel.add(lblStatus);
        statusPanel.add(Box.createVerticalStrut(SP_SM));
        statusPanel.add(lblLastRun);
        statusPanel.add(Box.createVerticalStrut(SP_SM));
        statusPanel.add(lblNextRun);
        statusPanel.add(Box.createVerticalStrut(SP_SM));
        statusPanel.add(lblTotal);

        statusCard.add(statusPanel, BorderLayout.CENTER);

        // ===== Layout =====
        JPanel center = new JPanel();
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.setOpaque(false);
        center.add(controlCard);
        center.add(Box.createVerticalStrut(SP_SM));
        center.add(statusCard);
        center.add(Box.createVerticalGlue());

        add(header, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        updateStatus();
    }

    private JLabel makeLabel() {
        JLabel lbl = new JLabel();
        lbl.setFont(FONT_BODY);
        lbl.setForeground(TEXT_SECONDARY);
        lbl.setIconTextGap(SP_SM);
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private void updateStatus() {
        MobMeCleanerService svc = MobMeCleanerService.gI();

        boolean on = svc.isEnabled();
        lblStatus.setText("Trạng thái: " + (on ? "ĐANG BẬT" : "ĐANG TẮT"));
        lblStatus.setFont(FONT_BODY_B);
        lblStatus.setForeground(on ? SUCCESS : TEXT_MUTED);
        lblStatus.setIcon(vectorIcon("power", 14, on ? SUCCESS : TEXT_MUTED));

        long last = svc.getLastRunTime();
        if (last == 0L) {
            lblLastRun.setText("Lần dọn cuối: (chưa dọn)");
        } else {
            lblLastRun.setText("Lần dọn cuối: " + sdf.format(new Date(last))
                    + "  —  dọn " + svc.getLastClearedCount() + " xác");
        }

        long nextMs = svc.getNextRunMs();
        if (nextMs < 0L) {
            lblNextRun.setText("Lần dọn tiếp: (tắt)");
            lblNextRun.setForeground(TEXT_MUTED);
        } else {
            long mins = nextMs / 60_000L;
            long secs = (nextMs % 60_000L) / 1000L;
            lblNextRun.setText("Lần dọn tiếp: còn " + mins + " phút " + secs + " giây");
            lblNextRun.setForeground(PRIMARY);
        }

        lblTotal.setText("Tổng đã dọn từ lúc khởi động: " + svc.getTotalCleared() + " xác trứng");
    }
}
