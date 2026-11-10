package server.ui;

import database.daos.GiftDataDAO;
import database.daos.GiftDataDAO.Reward;
import database.daos.PlayerDAO;
import jdbc.DBConnecter;
import models.Template;
import services.ItemService;
import services.func.GiftMailService;
import utils.Logger;

import javax.swing.*;
import java.awt.*;

import static server.ui.ServerGuiUtils.*;
import java.nio.charset.Charset;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gửi thư (mail) cho người chơi — mặt admin của opcode 99 sub 9.
 *
 * <p>Panel này là caller duy nhất của {@link GiftMailService#deliver} và
 * {@link GiftMailService#deliverTemplate}. Có hai đường gửi:</p>
 * <ul>
 *   <li><b>Thư tự soạn</b>: nhập tay người gửi / tiêu đề / nội dung / phần thưởng — dùng cho đền bù
 *       một lần, nội dung không lặp lại.</li>
 *   <li><b>Thư theo mẫu</b>: chọn 1 dòng {@code gift_mail_template} (chỉ {@code mailKind = 0}) —
 *       nên dùng khi nội dung được tái sử dụng, vì sửa mẫu không cần build lại server.</li>
 * </ul>
 *
 * <p><b>Người chơi offline.</b> Tên nhân vật được tra trong DB
 * ({@link PlayerDAO#findPlayerIdByName}) chứ KHÔNG phải trong danh sách online — thư chính là cơ chế
 * để với tới người đang không đăng nhập. {@code GiftMailService} chỉ đẩy badge (sub 9 kind 3) khi
 * người nhận đang online; offline thì hàng đã nằm trong DB và client đọc ở lần đăng nhập sau.</p>
 *
 * <p><b>Luồng (threading).</b> Các panel khác trong {@code server.ui} chạy JDBC thẳng trên EDT
 * (xem {@code BuffVndPanel.performBuff}), làm treo giao diện khi DB chậm. Panel này KHÔNG làm vậy:
 * mọi truy vấn (tra tên nhân vật, đọc {@code item_template}, {@code GiftDataDAO.snapshot()}, và cả
 * lệnh gửi thư) chạy trong {@link SwingWorker#doInBackground()}, chỉ cập nhật widget trong
 * {@link SwingWorker#done()} — tức là trên EDT.</p>
 */
public class MailPanel extends JPanel {

    /** Client đọc icon bằng {@code ItemTemplates.get((short) itemId)} — id vượt short sẽ không hiện. */
    private static final int MAX_ITEM_ID = Short.MAX_VALUE;

    /**
     * Trần an toàn cho chuỗi UTF: client đọc độ dài chuỗi bằng {@code short} CÓ DẤU, nên chuỗi quá
     * 32767 byte UTF-8 sẽ làm hỏng cả gói tin. Chặn sớm ở đây với biên dự phòng.
     */
    /**
     * Giới hạn theo ĐÚNG độ rộng cột trong gift_mail_player (GiftDataDAO.ensureTables):
     * sender VARCHAR(64), title VARCHAR(255), body TEXT. Chặn ở đây thay vì để MySQL xử lý:
     * với sql_mode nghiêm ngặt thì insert ném lỗi và admin chỉ thấy "mã trả về -1"; với sql_mode
     * lỏng thì MySQL cắt bớt IM LẶNG, thư gửi đi với tiêu đề cụt mà panel vẫn báo thành công.
     */
    /** Số phần thưởng tối đa: đếm trên dây là 1 byte không dấu. */
    private static final int MAX_REWARDS = 255;

    private static final int MAX_SENDER_CHARS = 64;

    private static final int MAX_TITLE_CHARS = 255;

    private static final int MAX_BODY_UTF_BYTES = 30000;

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private static final String DEFAULT_SENDER = "Hệ Thống";

    // ---- form thư tự soạn -------------------------------------------------
    private JTextField playerNameField;
    private JTextField senderField;
    private JTextField titleField;
    private JTextArea bodyArea;
    private JTextField expireField;
    private JTextField rewardField;
    private JTextArea rewardPreview;
    private JButton sendButton;

    // ---- form thư theo mẫu ------------------------------------------------
    private JComboBox<TemplateEntry> templateCombo;
    private JLabel templateInfoLabel;
    private JButton templateSendButton;

    /** id vật phẩm → tên, nạp thẳng từ {@code item_template} (nro_data). */
    private final Map<Integer, String> itemNames = new HashMap<>();

    /** false khi không tra cứu được bảng item_template lẫn template trong RAM. */
    private volatile boolean itemCatalogLoaded = false;

    public MailPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);

        initUI();

        loadItemNamesAsync();
        loadTemplatesAsync();
    }

    // ======================================================================
    //  UI
    // ======================================================================
    private void initUI() {
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        center.add(buildComposePanel());
        center.add(Box.createVerticalStrut(SP_MD));
        center.add(buildTemplatePanel());
        center.add(Box.createVerticalStrut(SP_MD));
        center.add(buildInfoPanel());

        add(pageScroll(center), BorderLayout.CENTER);
    }

    private JPanel buildComposePanel() {
        JPanel cardPanel = card(new BorderLayout());
        cardPanel.add(sectionHeader("Gửi thư tự soạn",
                "Đền bù một lần — nhập tay người gửi, tiêu đề, nội dung và phần thưởng", null),
                BorderLayout.NORTH);

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM);
        gbc.anchor = GridBagConstraints.WEST;

        int row = 0;

        // Tên nhân vật
        playerNameField = textField("Tên nhân vật trong game");
        playerNameField.setToolTipText("Tên NHÂN VẬT (không phải tên tài khoản). Tra trong DB nên gửi được cho cả người đang offline.");
        addRow(formPanel, gbc, row++, "Tên nhân vật:", playerNameField);

        // Người gửi
        senderField = textField("Tên hiển thị của người gửi");
        senderField.setText(DEFAULT_SENDER);
        senderField.setToolTipText("Tên hiển thị ở dòng trên của thư trong hòm thư");
        addRow(formPanel, gbc, row++, "Người gửi:", senderField);

        // Tiêu đề
        titleField = textField("Tiêu đề thư");
        titleField.setToolTipText("Dòng dưới của thư trong danh sách hòm thư");
        addRow(formPanel, gbc, row++, "Tiêu đề:", titleField);

        // Nội dung
        bodyArea = new JTextArea(6, 20);
        bodyArea.setLineWrap(true);
        bodyArea.setWrapStyleWord(true);
        bodyArea.setFont(FONT_BODY);
        JScrollPane bodyScroll = new JScrollPane(bodyArea);
        bodyScroll.setBorder(softBorder());
        bodyScroll.setPreferredSize(new Dimension(420, 120));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(label("Nội dung:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(bodyScroll, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        row++;

        // Hạn sử dụng
        expireField = textField("0 = không hết hạn");
        expireField.setToolTipText("Số ngày thư còn hiệu lực. Để trống hoặc 0 = không bao giờ hết hạn.");
        addRow(formPanel, gbc, row++, "Hết hạn sau (ngày):", expireField);

        // Phần thưởng
        rewardField = textField("457x2, 14x1");
        rewardField.setToolTipText("Định dạng: 457x2, 14x1 — id vật phẩm nhân số lượng, cách nhau bằng dấu phẩy. Bỏ trống nếu thư không đính kèm gì.");
        JPanel rewardRow = new JPanel(new BorderLayout(SP_SM, 0));
        rewardRow.setOpaque(false);
        rewardRow.add(rewardField, BorderLayout.CENTER);
        JButton checkButton = button("Kiểm tra", ButtonVariant.SECONDARY, "search");
        checkButton.addActionListener(e -> refreshRewardPreview());
        rewardRow.add(checkButton, BorderLayout.EAST);
        addRow(formPanel, gbc, row++, "Phần thưởng:", rewardRow);

        // Xem trước phần thưởng
        rewardPreview = new JTextArea(4, 20);
        rewardPreview.setEditable(false);
        rewardPreview.setBackground(SURFACE_ALT);
        rewardPreview.setForeground(TEXT_SECONDARY);
        rewardPreview.setFont(FONT_SMALL);
        rewardPreview.setBorder(pad(SP_SM, SP_MD, SP_SM, SP_MD));
        rewardPreview.setText("(chưa kiểm tra)");
        JScrollPane previewScroll = new JScrollPane(rewardPreview);
        previewScroll.setBorder(softBorder());
        previewScroll.getViewport().setBackground(SURFACE_ALT);
        previewScroll.setPreferredSize(new Dimension(420, 80));
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        formPanel.add(label("Vật phẩm nhận ra:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(previewScroll, gbc);
        gbc.anchor = GridBagConstraints.WEST;
        row++;

        // Nút
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, SP_XS));
        buttonPanel.setOpaque(false);
        sendButton = button("Gửi thư", ButtonVariant.PRIMARY, "send");
        sendButton.addActionListener(e -> performSend());

        JButton clearButton = button("Xóa", ButtonVariant.GHOST, "broom");
        clearButton.addActionListener(e -> clearForm());

        buttonPanel.add(clearButton);
        buttonPanel.add(sendButton);

        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        formPanel.add(buttonPanel, gbc);
        gbc.gridwidth = 1;

        cardPanel.add(formPanel, BorderLayout.CENTER);
        return cardPanel;
    }

    private JPanel buildTemplatePanel() {
        JPanel cardPanel = card(new BorderLayout());
        cardPanel.add(sectionHeader("Gửi thư theo mẫu có sẵn (gift_mail_template)",
                "Chỉ mẫu cá nhân (mailKind = 0) — sửa mẫu không cần build lại server", null),
                BorderLayout.NORTH);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM);
        gbc.anchor = GridBagConstraints.WEST;

        templateCombo = new JComboBox<>();
        templateCombo.setFont(FONT_BODY);
        templateCombo.addItem(new TemplateEntry(0, "(đang tải danh sách mẫu...)", null));
        templateCombo.setEnabled(false);
        templateCombo.addActionListener(e -> refreshTemplateInfo());

        JPanel comboRow = new JPanel(new BorderLayout(SP_SM, 0));
        comboRow.setOpaque(false);
        comboRow.add(templateCombo, BorderLayout.CENTER);
        JButton reloadButton = button("Tải lại", ButtonVariant.SECONDARY, "refresh");
        // Tải lại CẢ danh mục vật phẩm, không chỉ mẫu thư: nếu lần đọc item_template lúc khởi tạo
        // panel thất bại (pool chưa sẵn sàng — panel được dựng trước startServerProcesses), cờ
        // itemCatalogLoaded kẹt ở false và admin vĩnh viễn không đính kèm được phần thưởng nữa.
        reloadButton.addActionListener(e -> {
            loadTemplatesAsync();
            loadItemNamesAsync();
        });
        comboRow.add(reloadButton, BorderLayout.EAST);

        addRow(panel, gbc, 0, "Mẫu thư:", comboRow);

        templateInfoLabel = label(" ", FONT_SMALL, TEXT_MUTED);
        gbc.gridx = 1;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(templateInfoLabel, gbc);

        templateSendButton = button("Gửi theo mẫu", ButtonVariant.PRIMARY, "send");
        templateSendButton.setEnabled(false);
        templateSendButton.addActionListener(e -> performSendTemplate());

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, SP_XS));
        buttonPanel.setOpaque(false);
        buttonPanel.add(templateSendButton);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        panel.add(buttonPanel, gbc);

        cardPanel.add(panel, BorderLayout.CENTER);
        return cardPanel;
    }

    private JPanel buildInfoPanel() {
        JPanel infoPanel = card(new BorderLayout());
        infoPanel.add(sectionHeader("Hướng dẫn"), BorderLayout.NORTH);
        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setOpaque(false);
        infoText.setBackground(SURFACE);
        infoText.setForeground(TEXT_SECONDARY);
        infoText.setFont(FONT_SMALL);
        infoText.setText(
                "• Ô \"Tên nhân vật\" dùng chung cho CẢ HAI cách gửi (tự soạn và theo mẫu).\n"
                + "• Tên nhân vật được tra trong database nên gửi được cho người chơi ĐANG OFFLINE;\n"
                + "  nếu người nhận đang online thì badge hòm thư cập nhật ngay trong phiên.\n"
                + "• Tên không phân biệt hoa/thường (đúng như cách server so tên ở chỗ khác).\n"
                + "• Phần thưởng nhập theo dạng \"457x2, 14x1\" — id vật phẩm, chữ x, số lượng,\n"
                + "  các mục cách nhau bằng dấu phẩy. Chấp nhận cả \"457:2\" và \"457\" (mặc định x1).\n"
                + "• Bấm \"Kiểm tra\" để xem tên vật phẩm mà mỗi id tra ra TRƯỚC khi gửi.\n"
                + "• Id vật phẩm không tồn tại sẽ bị từ chối — thư không được gửi đi.\n"
                + "• \"Hết hạn sau (ngày)\" để trống hoặc 0 nghĩa là thư không bao giờ hết hạn.\n"
                + "• Gửi theo mẫu chỉ dùng được với dòng gift_mail_template có mailKind = 0;\n"
                + "  mẫu toàn server (mailKind = 1) đã tự hiện trong hòm thư mọi người.");
        infoText.setBorder(pad(0, SP_XS, 0, SP_XS));
        infoPanel.add(infoText, BorderLayout.CENTER);
        return infoPanel;
    }

    private void addRow(JPanel panel, GridBagConstraints gbc, int row, String labelText, Component field) {
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        panel.add(label(labelText, FONT_SMALL_B, TEXT_SECONDARY), gbc);
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        panel.add(field, gbc);
    }

    // ======================================================================
    //  Nạp dữ liệu nền (không chạy trên EDT)
    // ======================================================================

    /**
     * Nạp bảng tên vật phẩm. Ưu tiên đọc thẳng {@code item_template} trong nro_data (key theo id
     * thật) rồi mới dự phòng bằng template đã nạp trong RAM, giống {@code ItemSearchPanel}.
     */
    private void loadItemNamesAsync() {
        new SwingWorker<Map<Integer, String>, Void>() {
            @Override
            protected Map<Integer, String> doInBackground() {
                Map<Integer, String> names = new HashMap<>();
                try (Connection con = DBConnecter.getConnectionData();
                        PreparedStatement ps = con.prepareStatement("SELECT id, name FROM item_template");
                        ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        names.put(rs.getInt("id"), rs.getString("name"));
                    }
                } catch (Exception e) {
                    Logger.logException(MailPanel.class, e, "MailPanel: lỗi đọc item_template");
                }
                return names;
            }

            @Override
            protected void done() {
                try {
                    itemNames.putAll(get());
                } catch (Exception ignored) {
                }
                itemCatalogLoaded = !itemNames.isEmpty() || resolveItemName(0) != null;
                if (!itemCatalogLoaded) {
                    rewardPreview.setText("Không tra cứu được bảng item_template — không thể kiểm tra id vật phẩm.");
                }
            }
        }.execute();
    }

    /** Nạp danh sách mẫu thư cá nhân ({@code mailKind == 0}) từ snapshot của GiftDataDAO. */
    private void loadTemplatesAsync() {
        templateCombo.setEnabled(false);
        templateSendButton.setEnabled(false);
        new SwingWorker<List<TemplateEntry>, Void>() {
            @Override
            protected List<TemplateEntry> doInBackground() {
                List<TemplateEntry> entries = new ArrayList<>();
                try {
                    // snapshot() có thể chạm DB ở lần gọi đầu tiên — đó là lý do nó nằm ở đây.
                    for (GiftDataDAO.Mail mail : GiftDataDAO.snapshot().mailTemplates) {
                        if (mail != null && mail.mailKind == 0) {
                            entries.add(new TemplateEntry(mail.id, mail.title, mail));
                        }
                    }
                } catch (Exception e) {
                    Logger.logException(MailPanel.class, e, "MailPanel: lỗi đọc gift_mail_template");
                }
                return entries;
            }

            @Override
            protected void done() {
                List<TemplateEntry> entries;
                try {
                    entries = get();
                } catch (Exception e) {
                    entries = new ArrayList<>();
                }
                templateCombo.removeAllItems();
                if (entries.isEmpty()) {
                    templateCombo.addItem(new TemplateEntry(0, "(không có mẫu thư cá nhân nào)", null));
                    templateCombo.setEnabled(false);
                    templateSendButton.setEnabled(false);
                } else {
                    for (TemplateEntry entry : entries) {
                        templateCombo.addItem(entry);
                    }
                    templateCombo.setEnabled(true);
                    templateSendButton.setEnabled(true);
                }
                refreshTemplateInfo();
            }
        }.execute();
    }

    private void refreshTemplateInfo() {
        if (templateInfoLabel == null) {
            return;
        }
        TemplateEntry entry = (TemplateEntry) templateCombo.getSelectedItem();
        if (entry == null || entry.mail == null) {
            templateInfoLabel.setText(" ");
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Người gửi: ").append(entry.mail.sender.isEmpty() ? "(trống)" : entry.mail.sender);
        sb.append("  |  Hết hạn: ")
                .append(entry.mail.expireDays > 0 ? (entry.mail.expireDays + " ngày") : "không hết hạn");
        sb.append("  |  Đính kèm: ").append(entry.mail.rewards.size()).append(" vật phẩm");
        templateInfoLabel.setText(sb.toString());
    }

    // ======================================================================
    //  Phần thưởng
    // ======================================================================

    /** Kết quả phân tích ô phần thưởng. */
    private static final class ParsedRewards {

        final List<Reward> rewards = new ArrayList<>();
        final List<String> resolved = new ArrayList<>();
        final List<String> errors = new ArrayList<>();
    }

    /**
     * Phân tích chuỗi dạng {@code "457x2, 14x1"}. Cũng chấp nhận {@code 457:2}, {@code 457 x 2} và
     * {@code 457} (mặc định số lượng 1). Mọi id đều được đối chiếu với bảng vật phẩm; id không tồn
     * tại là LỖI chứ không bị bỏ qua âm thầm.
     */
    private ParsedRewards parseRewards(String raw) {
        ParsedRewards out = new ParsedRewards();
        if (raw == null || raw.trim().isEmpty()) {
            return out;
        }
        String[] parts = raw.split("[,;\\n]");
        for (String part : parts) {
            String token = part.trim();
            if (token.isEmpty()) {
                continue;
            }
            String idText = token;
            String qtyText = "1";
            int sep = indexOfSeparator(token);
            if (sep >= 0) {
                idText = token.substring(0, sep).trim();
                qtyText = token.substring(sep + 1).trim();
            }
            int itemId;
            int amount;
            try {
                itemId = Integer.parseInt(idText);
                amount = qtyText.isEmpty() ? 1 : Integer.parseInt(qtyText);
            } catch (NumberFormatException ex) {
                out.errors.add("\"" + token + "\": sai định dạng, phải là dạng \"457x2\"");
                continue;
            }
            if (itemId < 0 || itemId > MAX_ITEM_ID) {
                out.errors.add("\"" + token + "\": id vật phẩm phải nằm trong khoảng 0.." + MAX_ITEM_ID);
                continue;
            }
            if (amount <= 0) {
                out.errors.add("\"" + token + "\": số lượng phải lớn hơn 0");
                continue;
            }
            String name = resolveItemName(itemId);
            if (name == null) {
                out.errors.add("\"" + token + "\": không có vật phẩm id " + itemId);
                continue;
            }
            out.rewards.add(new Reward(itemId, amount));
            out.resolved.add(itemId + " x" + amount + "  →  " + name);
        }
        return out;
    }

    private static int indexOfSeparator(String token) {
        for (int i = 1; i < token.length(); i++) {
            char c = token.charAt(i);
            if (c == 'x' || c == 'X' || c == '*' || c == ':') {
                return i;
            }
        }
        return -1;
    }

    /** Tra tên vật phẩm: ưu tiên bảng DB, dự phòng template đã nạp trong RAM. null = không tồn tại. */
    private String resolveItemName(int itemId) {
        String name = itemNames.get(itemId);
        if (name == null || name.isEmpty()) {
            try {
                Template.ItemTemplate t = ItemService.gI().getTemplate(itemId);
                if (t != null && t.name != null) {
                    name = t.name;
                }
            } catch (Exception ignored) {
            }
        }
        return (name == null || name.isEmpty()) ? null : name;
    }

    private void refreshRewardPreview() {
        if (!itemCatalogLoaded) {
            rewardPreview.setText("Không tra cứu được bảng vật phẩm — không thể kiểm tra id.");
            return;
        }
        ParsedRewards parsed = parseRewards(rewardField.getText());
        StringBuilder sb = new StringBuilder();
        for (String line : parsed.resolved) {
            sb.append(line).append('\n');
        }
        for (String err : parsed.errors) {
            sb.append("LỖI — ").append(err).append('\n');
        }
        if (sb.length() == 0) {
            sb.append("(không đính kèm vật phẩm nào)");
        }
        rewardPreview.setText(sb.toString());
        rewardPreview.setCaretPosition(0);
    }

    // ======================================================================
    //  Gửi
    // ======================================================================

    private void performSend() {
        final String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            error("Vui lòng nhập tên nhân vật.");
            return;
        }
        final String sender = senderField.getText().trim().isEmpty()
                ? DEFAULT_SENDER : senderField.getText().trim();
        final String title = titleField.getText().trim();
        if (title.isEmpty()) {
            error("Vui lòng nhập tiêu đề thư.");
            return;
        }
        final String body = bodyArea.getText();

        String textProblem = checkChars("Người gửi", sender, MAX_SENDER_CHARS);
        if (textProblem == null) {
            textProblem = checkChars("Tiêu đề", title, MAX_TITLE_CHARS);
        }
        if (textProblem == null) {
            textProblem = checkText("Nội dung", body);
        }
        if (textProblem != null) {
            error(textProblem);
            return;
        }

        final int expireDays;
        String expireText = expireField.getText().trim();
        if (expireText.isEmpty()) {
            expireDays = 0;
        } else {
            try {
                expireDays = Integer.parseInt(expireText);
            } catch (NumberFormatException ex) {
                error("\"Hết hạn sau (ngày)\" phải là số nguyên, hoặc để trống.");
                return;
            }
            if (expireDays < 0) {
                error("\"Hết hạn sau (ngày)\" không được âm. Để trống hoặc 0 nghĩa là không hết hạn.");
                return;
            }
        }

        if (!itemCatalogLoaded && !rewardField.getText().trim().isEmpty()) {
            error("Chưa tra cứu được bảng vật phẩm nên không thể kiểm tra id.\n"
                    + "Bấm \"Tải lại\" hoặc gửi thư không đính kèm phần thưởng.");
            return;
        }
        final ParsedRewards parsed = parseRewards(rewardField.getText());
        refreshRewardPreview();
        if (!parsed.errors.isEmpty()) {
            StringBuilder sb = new StringBuilder("Phần thưởng không hợp lệ, chưa gửi thư:\n\n");
            for (String err : parsed.errors) {
                sb.append("• ").append(err).append('\n');
            }
            error(sb.toString());
            return;
        }
        // Số phần thưởng đi trên dây bằng 1 byte không dấu. Server clamp về 255 khi VẼ
        // (GiftSystemService.sanitize) nhưng khi TRAO thì duyệt hết danh sách, nên quá 255 sẽ
        // trao nhiều hơn những gì client từng hiển thị.
        if (parsed.rewards.size() > MAX_REWARDS) {
            error("Tối đa " + MAX_REWARDS + " phần thưởng mỗi thư (đang có "
                    + parsed.rewards.size() + ").");
            return;
        }

        setBusy(true);
        new SwingWorker<Outcome, Void>() {
            @Override
            protected Outcome doInBackground() {
                long playerId = PlayerDAO.findPlayerIdByName(playerName);
                Outcome bad = describeLookupFailure(playerId, playerName);
                if (bad != null) {
                    return bad;
                }
                try {
                    int mailId = GiftMailService.gI().deliver(playerId, sender, title, body,
                            parsed.rewards, expireDays);
                    if (mailId <= 0) {
                        return Outcome.fail("Ghi thư vào database thất bại (mã trả về " + mailId + ").\n"
                                + "Xem log server để biết chi tiết.");
                    }
                    return Outcome.ok("Đã gửi thư cho " + playerName + " (id " + playerId + ").\n"
                            + "- Mã thư: " + mailId + "\n"
                            + "- Đính kèm: " + parsed.rewards.size() + " vật phẩm\n"
                            + "- Hết hạn: " + (expireDays > 0 ? (expireDays + " ngày") : "không hết hạn"));
                } catch (Exception e) {
                    Logger.logException(MailPanel.class, e, "MailPanel: lỗi gửi thư cho " + playerName);
                    return Outcome.fail("Lỗi khi gửi thư. Xem log server để biết chi tiết.");
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                report(this, true);
            }
        }.execute();
    }

    private void performSendTemplate() {
        final String playerName = playerNameField.getText().trim();
        if (playerName.isEmpty()) {
            error("Vui lòng nhập tên nhân vật (dùng chung ô với phần thư tự soạn).");
            return;
        }
        final TemplateEntry entry = (TemplateEntry) templateCombo.getSelectedItem();
        if (entry == null || entry.mail == null) {
            error("Chưa chọn được mẫu thư nào.");
            return;
        }

        setBusy(true);
        new SwingWorker<Outcome, Void>() {
            @Override
            protected Outcome doInBackground() {
                long playerId = PlayerDAO.findPlayerIdByName(playerName);
                Outcome bad = describeLookupFailure(playerId, playerName);
                if (bad != null) {
                    return bad;
                }
                try {
                    int mailId = GiftMailService.gI().deliverTemplate(playerId, entry.id);
                    if (mailId <= 0) {
                        return Outcome.fail("Không gửi được mẫu thư id " + entry.id + ".\n"
                                + "Mẫu có thể đã bị tắt (active = 0), là thư toàn server (mailKind = 1),\n"
                                + "hoặc ghi database thất bại. Xem log server để biết chi tiết.");
                    }
                    return Outcome.ok("Đã gửi mẫu thư \"" + entry.title + "\" (id " + entry.id + ")\n"
                            + "cho " + playerName + " (id " + playerId + ").\n"
                            + "- Mã thư: " + mailId);
                } catch (Exception e) {
                    Logger.logException(MailPanel.class, e,
                            "MailPanel: lỗi gửi mẫu thư " + entry.id + " cho " + playerName);
                    return Outcome.fail("Lỗi khi gửi thư theo mẫu. Xem log server để biết chi tiết.");
                }
            }

            @Override
            protected void done() {
                setBusy(false);
                report(this, false);
            }
        }.execute();
    }

    /** Dịch mã lỗi tra tên của {@link PlayerDAO} thành thông báo cụ thể; null nghĩa là tra được. */
    private static Outcome describeLookupFailure(long playerId, String playerName) {
        if (playerId == PlayerDAO.PLAYER_NAME_NOT_FOUND) {
            return Outcome.fail("Không tìm thấy nhân vật tên \"" + playerName + "\".\n"
                    + "Lưu ý đây là tên NHÂN VẬT, không phải tên tài khoản.");
        }
        if (playerId == PlayerDAO.PLAYER_NAME_AMBIGUOUS) {
            return Outcome.fail("Có nhiều hơn một nhân vật tên \"" + playerName + "\".\n"
                    + "Không thể tự chọn hộ — hãy xử lý trùng tên trong database trước.");
        }
        if (playerId <= 0) {
            return Outcome.fail("Lỗi truy vấn database khi tra tên nhân vật.\n"
                    + "Xem log server để biết chi tiết.");
        }
        return null;
    }

    /** Đọc kết quả của worker và báo cho admin; xóa form khi gửi thành công. */
    private void report(SwingWorker<Outcome, Void> worker, boolean clearOnSuccess) {
        Outcome outcome;
        try {
            outcome = worker.get();
        } catch (Exception e) {
            Logger.logException(MailPanel.class, e, "MailPanel: lỗi lấy kết quả gửi thư");
            outcome = Outcome.fail("Lỗi không xác định khi gửi thư. Xem log server để biết chi tiết.");
        }
        if (outcome.success) {
            JOptionPane.showMessageDialog(this, outcome.message, "Thành công",
                    JOptionPane.INFORMATION_MESSAGE);
            if (clearOnSuccess) {
                clearForm();
            }
        } else {
            JOptionPane.showMessageDialog(this, outcome.message, "Thất bại", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Chặn chuỗi client không đọc được: chứa ký tự NUL, hoặc dài quá mức
     * {@code DataOutputStream.writeUTF} ghi được — client đọc tiền tố độ dài bằng short CÓ DẤU
     * ({@code myReader.readStringUTF}), nên vượt 32767 byte là độ dài âm và parse hỏng.
     *
     * <p>Đo bằng modified UTF-8 (đúng thứ {@code writeUTF} ghi ra), không phải UTF-8 chuẩn: ký tự
     * ngoài BMP được ghi thành 2 surrogate 3 byte = 6 byte, trong khi UTF-8 chuẩn chỉ 4.</p>
     */
    private static String checkText(String label, String value) {
        if (value == null) {
            return null;
        }
        if (value.indexOf(' ') >= 0) {
            return label + " chứa ký tự NUL — client không đọc được, vui lòng bỏ ký tự này.";
        }
        int bytes = modifiedUtfLength(value);
        if (bytes > MAX_BODY_UTF_BYTES) {
            return label + " quá dài (" + bytes + " byte, tối đa " + MAX_BODY_UTF_BYTES + ").";
        }
        return null;
    }

    /** Giới hạn theo số ký tự, cho các trường có cột VARCHAR cố định. */
    private static String checkChars(String label, String value, int maxChars) {
        String problem = checkText(label, value);
        if (problem != null) {
            return problem;
        }
        if (value != null && value.length() > maxChars) {
            return label + " quá dài (" + value.length() + " ký tự, tối đa " + maxChars
                    + ") — cột trong database sẽ cắt bớt.";
        }
        return null;
    }

    /** Số byte {@code DataOutputStream.writeUTF} sẽ ghi cho chuỗi này. */
    private static int modifiedUtfLength(String value) {
        int len = 0;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c >= 0x0001 && c <= 0x007F) {
                len += 1;
            } else if (c <= 0x07FF) {
                len += 2;
            } else {
                len += 3;
            }
        }
        return len;
    }

    private void setBusy(boolean busy) {
        sendButton.setEnabled(!busy);
        templateSendButton.setEnabled(!busy && templateCombo.isEnabled());
        setCursor(busy ? Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR) : Cursor.getDefaultCursor());
    }

    private void error(String message) {
        JOptionPane.showMessageDialog(this, message, "Lỗi", JOptionPane.ERROR_MESSAGE);
    }

    private void clearForm() {
        playerNameField.setText("");
        senderField.setText(DEFAULT_SENDER);
        titleField.setText("");
        bodyArea.setText("");
        expireField.setText("");
        rewardField.setText("");
        rewardPreview.setText("(chưa kiểm tra)");
    }

    // ======================================================================
    //  Kiểu dữ liệu phụ
    // ======================================================================

    /** Một dòng {@code gift_mail_template} hiển thị trong combo box. */
    private static final class TemplateEntry {

        final int id;
        final String title;
        final GiftDataDAO.Mail mail;

        TemplateEntry(int id, String title, GiftDataDAO.Mail mail) {
            this.id = id;
            this.title = (title == null || title.isEmpty()) ? "(không có tiêu đề)" : title;
            this.mail = mail;
        }

        @Override
        public String toString() {
            return mail == null ? title : ("#" + id + " — " + title);
        }
    }

    /** Kết quả một lần gửi, truyền từ luồng nền về EDT. */
    private static final class Outcome {

        final boolean success;
        final String message;

        private Outcome(boolean success, String message) {
            this.success = success;
            this.message = message;
        }

        static Outcome ok(String message) {
            return new Outcome(true, message);
        }

        static Outcome fail(String message) {
            return new Outcome(false, message);
        }
    }
}
