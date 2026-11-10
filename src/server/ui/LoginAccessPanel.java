package server.ui;

import java.awt.BorderLayout;
import java.io.IOException;
import java.util.function.Consumer;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import services.LoginAccessConfig;
import static server.ui.ServerGuiUtils.*;

public class LoginAccessPanel extends JPanel {
    private final JCheckBox adminOnly = new JCheckBox("Chỉ admin được vào game");
    private final JLabel status = label("", FONT_SMALL, TEXT_MUTED);

    public LoginAccessPanel(Consumer<String> log) {
        super(new BorderLayout(0, SP_XS));
        setOpaque(false);
        adminOnly.setOpaque(false);
        adminOnly.setFont(FONT_SMALL_B);
        adminOnly.setForeground(TEXT_SECONDARY);
        adminOnly.setToolTipText("Bật/tắt quyền đăng nhập và tự động lưu cấu hình.");
        add(adminOnly, BorderLayout.NORTH);
        add(status, BorderLayout.SOUTH);
        try {
            LoginAccessConfig.load();
            updateStatus();
        } catch (IOException | NumberFormatException e) {
            adminOnly.setSelected(LoginAccessConfig.isAdminOnly());
            status.setText("Không tải được cấu hình đăng nhập.");
            status.setForeground(DANGER);
            log.accept("Không tải được cấu hình đăng nhập.");
        }
        adminOnly.addActionListener(e -> {
            try {
                LoginAccessConfig.setAdminOnly(adminOnly.isSelected());
                updateStatus();
                log.accept(LoginAccessConfig.isAdminOnly()
                        ? "Đã bật chế độ chỉ admin được đăng nhập."
                        : "Đã mở đăng nhập cho tất cả tài khoản.");
            } catch (IOException ex) {
                adminOnly.setSelected(LoginAccessConfig.isAdminOnly());
                status.setText("Không lưu được cấu hình. Vui lòng thử lại.");
                status.setForeground(DANGER);
                log.accept("Không lưu được chế độ đăng nhập; giữ trạng thái hiện tại.");
            }
        });
    }

    private void updateStatus() {
        boolean enabled = LoginAccessConfig.isAdminOnly();
        adminOnly.setSelected(enabled);
        status.setText(enabled ? "Đang bật: chỉ tài khoản admin được đăng nhập." : "Đang tắt: mở đăng nhập cho tất cả tài khoản.");
        status.setForeground(enabled ? WARNING : SUCCESS);
    }
}
