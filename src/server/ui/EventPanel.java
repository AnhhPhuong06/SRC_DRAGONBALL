package server.ui;

import utils.Logger;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static server.ui.ServerGuiUtils.*;

public class EventPanel extends JPanel {

    private JTextArea txtHistory;
    private final List<JToggleButton> checkBoxes = new ArrayList<>();

    // Danh sách tên sự kiện
    private static final String[] EVENT_NAMES = {
            "1. Halloween", "2. 8/3 Quốc Tế PN", "3. Giáng Sinh", "4. Tết Nguyên Đán",
            "5. Trung Thu", "6. Giỗ Tổ", "7. Top Up (Mặc định)", "8. Pokemon",
            "9. 20/11", "10. Phở Anh Hai"
    };

    public EventPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBorder(pad(SP_LG, SP_XL, SP_XL, SP_XL));
        setBackground(BG);
        initUI();
    }

    private void initUI() {
        // --- 1. Khu vực chọn sự kiện (toggle pill: bật = xanh, tắt = xám) ---
        JPanel togglePanel = new JPanel(new GridLayout(0, 2, SP_MD, SP_SM));
        togglePanel.setOpaque(false);

        List<Integer> savedIds = loadEventConfig();

        for (int i = 0; i < EVENT_NAMES.length; i++) {
            JToggleButton chk = toggle(EVENT_NAMES[i]);
            if (savedIds.contains(i + 1)) chk.setSelected(true);
            chk.putClientProperty("id", i + 1);
            checkBoxes.add(chk);
            togglePanel.add(chk);
        }

        // --- 2. Khu vực điều khiển ---
        JLabel lblHint = label("(*Lưu ý: Hệ thống sẽ tự động Restart sau 10 giây sau khi lưu thành công)",
                FONT_SMALL, DANGER);
        lblHint.setIcon(vectorIcon("warning", 14, DANGER));
        lblHint.setIconTextGap(SP_SM);

        JButton btnApply = button("LƯU CẤU HÌNH & KÍCH HOẠT NGAY", ButtonVariant.PRIMARY, "save");

        btnApply.addActionListener(e -> {
            List<Integer> selected = checkBoxes.stream()
                    .filter(JToggleButton::isSelected)
                    .map(chk -> (int) chk.getClientProperty("id"))
                    .collect(Collectors.toList());

            if (selected.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng chọn ít nhất 1 sự kiện!", "Cảnh báo", JOptionPane.WARNING_MESSAGE);
            } else {
                int confirm = JOptionPane.showConfirmDialog(this,
                    "Xác nhận lưu thay đổi?\nServer sẽ được khởi động lại sau 10 giây.",
                    "Xác nhận hệ thống", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

                if (confirm == JOptionPane.YES_OPTION) {
                    doiSuKienVaRestart(selected);
                }
            }
        });

        JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_MD, 0));
        actionBar.setOpaque(false);
        actionBar.setBorder(pad(SP_LG, 0, 0, 0));
        actionBar.add(lblHint);
        actionBar.add(btnApply);

        JPanel eventCard = card(new BorderLayout());
        eventCard.add(sectionHeader("Chọn sự kiện chạy song song",
                "Bật các sự kiện muốn kích hoạt cùng lúc trên server", null), BorderLayout.NORTH);
        eventCard.add(togglePanel, BorderLayout.CENTER);
        eventCard.add(actionBar, BorderLayout.SOUTH);

        // --- 3. Lịch sử hoạt động ---
        txtHistory = new JTextArea();
        JScrollPane scrollLog = styleConsole(txtHistory);
        scrollLog.setPreferredSize(new Dimension(0, 180));

        JPanel logCard = card(new BorderLayout());
        logCard.add(sectionHeader("Lịch sử hoạt động", "Nhật ký thay đổi cấu hình sự kiện", null), BorderLayout.NORTH);
        logCard.add(scrollLog, BorderLayout.CENTER);

        // Thêm vào EventPanel chính
        add(eventCard, BorderLayout.NORTH);
        add(logCard, BorderLayout.CENTER);

        log("Đã tải cấu hình sự kiện hiện tại: " + savedIds.toString());
    }

    public void doiSuKienVaRestart(List<Integer> eventIds) {
        log("Đang tiến hành lưu cấu hình vào active_event.txt...");
        if (!saveEventConfig(eventIds)) return;
        log("Cấu hình đã lưu. Chuẩn bị Restart...");
        // Gọi hàm restart riêng biệt
        triggerCustomRestart();
    }

    // --- HÀM RESTART ĐÃ SỬA LỖI DOUBLE RUN ---
    private void triggerCustomRestart() {
        int delaySeconds = 10;
        System.out.println(">>> [EventPanel] Requesting restart in " + delaySeconds + "s...");

        try {
            String currentDir = System.getProperty("user.dir");
            String osName = System.getProperty("os.name").toLowerCase();

            ProcessBuilder pb;
            if (osName.contains("win")) {
                // Windows: Mở CMD -> Đếm ngược 10s -> Chạy lại
                pb = new ProcessBuilder("cmd", "/c", "start", "cmd", "/c", "title Server Restarting... && echo Server se khoi dong lai sau 10s... && timeout /t " + delaySeconds + " /nobreak && run.bat");
            } else {
                // Linux
                pb = new ProcessBuilder("bash", "-c", "sleep " + delaySeconds + "; ./run.sh &");
            }

            pb.directory(new File(currentDir));
            pb.start();

            // [QUAN TRỌNG] Tắt cờ Auto Restart của ServerManagerUI để tránh Shutdown Hook chạy thêm lần nữa
            ServerManagerUI.REQUEST_AUTO_RESTART = false;

            System.out.println(">>> Bye bye! Exiting Java...");
            System.exit(0);

        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Lỗi thực thi Restart: " + e.getMessage());
        }
    }

    private boolean saveEventConfig(List<Integer> eventIds) {
        try {
            String line = eventIds.stream().map(String::valueOf).collect(Collectors.joining("-"));
            java.nio.file.Files.writeString(java.nio.file.Path.of("active_event.txt"), line + System.lineSeparator());
            event.EventManager.setHalloweenEnabled(eventIds.contains(1));
            return true;
        } catch (IOException e) {
            Logger.error("Lỗi lưu file active_event.txt: " + e.getMessage());
            log("Không lưu được cấu hình sự kiện. Vui lòng thử lại.");
            return false;
        }
    }

    public List<Integer> loadEventConfig() {
        List<Integer> ids = new ArrayList<>();
        File f = new File("active_event.txt");
        if (!f.exists()) { ids.add(7); return ids; }
        try (BufferedReader br = new BufferedReader(new FileReader(f))) {
            String line = br.readLine();
            if (line != null && !line.isEmpty()) {
                for (String part : line.split("-")) {
                    try { ids.add(Integer.parseInt(part.trim())); } catch (NumberFormatException ignored) {}
                }
            }
        } catch (Exception e) {}
        if (ids.isEmpty()) ids.add(7);
        return ids;
    }

    private void log(String msg) {
        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
        txtHistory.append("[" + time + "] " + msg + "\n");
        txtHistory.setCaretPosition(txtHistory.getDocument().getLength());
    }
}
