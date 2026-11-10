package server.ui;

import database.daos.PlayerDAO;
import models.player.Player;
import services.Service;
import server.Client;
import utils.Logger;
import utils.Util;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.util.Optional;

import static server.ui.ServerGuiUtils.*;

public class BuffVndPanel extends JPanel {

    private JTextField accountField;
    private JTextField vndField;
    private JTextField danapField;
    private JTextField sotienField;

    public BuffVndPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBackground(BG);
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));

        initUI();
    }

    private void initUI() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionHeader("Buff VND cho tài khoản",
                "Cộng VNĐ (Cash), Tổng Nạp (danap) và Số Tiền (sotien) trực tiếp vào tài khoản — có ghi log admin", null));

        // ===== Form card =====
        JPanel formCard = card(new BorderLayout());
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(SP_SM, SP_SM, SP_SM, SP_SM);
        gbc.anchor = GridBagConstraints.WEST;

        // Tên tài khoản
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(label("Tài khoản / Nhân vật:", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        accountField = textField("Nhập tên tài khoản hoặc tên nhân vật...");
        accountField.setColumns(20);
        accountField.putClientProperty("JTextField.leadingIcon", vectorIcon("user", 14, TEXT_MUTED));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(accountField, gbc);

        // VNĐ (Cash)
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(label("VNĐ (Cash):", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        vndField = textField("0");
        vndField.setColumns(10);
        vndField.setToolTipText("Nhập số tiền VNĐ (Cash) muốn cộng");
        vndField.setFont(FONT_H2);
        vndField.setForeground(SUCCESS);
        vndField.putClientProperty("JTextField.leadingIcon", vectorIcon("coin", 14, SUCCESS));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(vndField, gbc);

        // Tổng nạp (danap)
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(label("Tổng Nạp (danap):", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        danapField = textField("0");
        danapField.setColumns(10);
        danapField.setToolTipText("Nhập số tiền Tổng Nạp (danap) muốn cộng");
        danapField.setFont(FONT_H2);
        danapField.setForeground(SUCCESS);
        danapField.putClientProperty("JTextField.leadingIcon", vectorIcon("money", 14, SUCCESS));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(danapField, gbc);

        // Số Tiền (sotien)
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0;
        formPanel.add(label("Số Tiền (sotien):", FONT_SMALL_B, TEXT_SECONDARY), gbc);
        sotienField = textField("0");
        sotienField.setColumns(10);
        sotienField.setToolTipText("Nhập Số Tiền (sotien) muốn cộng");
        sotienField.setFont(FONT_H2);
        sotienField.setForeground(PRIMARY);
        sotienField.putClientProperty("JTextField.leadingIcon", vectorIcon("diamond", 14, PRIMARY));
        gbc.gridx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1.0;
        formPanel.add(sotienField, gbc);

        // Buttons
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, SP_XS));
        buttonPanel.setOpaque(false);
        JButton confirmButton = button("Xác nhận Buff", ButtonVariant.PRIMARY, "check");
        confirmButton.addActionListener(e -> performBuff());

        JButton clearButton = button("Xóa", ButtonVariant.SECONDARY, "broom");
        clearButton.addActionListener(e -> clearForm());

        buttonPanel.add(clearButton);
        buttonPanel.add(confirmButton);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.EAST;
        gbc.weightx = 0;
        formPanel.add(buttonPanel, gbc);

        formCard.add(formPanel, BorderLayout.CENTER);
        top.add(formCard);
        add(top, BorderLayout.NORTH);

        // ===== Info card =====
        JPanel infoCard = card(new BorderLayout(0, SP_SM));
        JLabel infoTitle = label("Hướng dẫn", FONT_H3, TEXT_SECONDARY);
        infoTitle.setIcon(vectorIcon("info", 15, INFO));
        infoTitle.setIconTextGap(SP_SM);
        infoCard.add(infoTitle, BorderLayout.NORTH);

        JTextArea infoText = new JTextArea();
        infoText.setEditable(false);
        infoText.setFocusable(false);
        infoText.setOpaque(false);
        infoText.setForeground(TEXT_SECONDARY);
        infoText.setFont(FONT_BODY);
        infoText.setText("• Nhập tên tài khoản, tên nhân vật hoặc ID cần buff VND\n" +
                        "• Nhập số VNĐ (Cash), Tổng Nạp (danap) hoặc Số Tiền (sotien) muốn cộng\n" +
                        "• Có thể nhập riêng từng loại hoặc buff cả 3 loại cùng lúc\n" +
                        "• Tự động đồng bộ số dư Cash, Tổng Nạp, Số Tiền và thông báo người chơi\n" +
                        "• Giá trị không được âm\n" +
                        "• Lịch sử buff được ghi tại: log/buff_vnd_admin_log.txt");
        infoText.setBorder(new EmptyBorder(SP_XS, SP_XS, SP_XS, SP_XS));
        infoCard.add(infoText, BorderLayout.CENTER);

        add(infoCard, BorderLayout.CENTER);
    }

    private void performBuff() {
        try {
            String accountName = accountField.getText().trim();
            if (accountName.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập tên tài khoản hoặc tên nhân vật.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            int vndAmount = vndField.getText().trim().isEmpty() ? 0 : Integer.parseInt(vndField.getText().trim());
            int danapAmount = danapField.getText().trim().isEmpty() ? 0 : Integer.parseInt(danapField.getText().trim());
            int sotienAmount = sotienField.getText().trim().isEmpty() ? 0 : Integer.parseInt(sotienField.getText().trim());

            if (vndAmount < 0 || danapAmount < 0 || sotienAmount < 0) {
                JOptionPane.showMessageDialog(this, "Giá trị không được âm.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            if (vndAmount == 0 && danapAmount == 0 && sotienAmount == 0) {
                JOptionPane.showMessageDialog(this, "Vui lòng nhập ít nhất một giá trị lớn hơn 0.", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }

            PlayerDAO.AccountTarget target = PlayerDAO.resolveAccountTarget(accountName);
            if (target.accountId == -1) {
                Logger.fileLog("buff_vnd_admin", "BUFF FAIL | mục tiêu=" + accountName
                        + " | lý do=không tìm thấy tài khoản hoặc nhân vật"
                        + " | operator=" + System.getProperty("user.name"));
                JOptionPane.showMessageDialog(this, "Không tìm thấy tài khoản hoặc nhân vật: " + accountName, "Thất bại", JOptionPane.ERROR_MESSAGE);
                return;
            }

            boolean ok = PlayerDAO.buffVnd(accountName, vndAmount, danapAmount, sotienAmount);
            if (ok) {
                String targetName = !target.playerName.isEmpty() ? (target.playerName + " (TK: " + target.username + ")") : target.username;
                Logger.fileLog("buff_vnd_admin", "BUFF OK | target=" + targetName + " (accId=" + target.accountId + ")"
                        + " | VNĐ=" + vndAmount
                        + " | Tổng nạp=" + danapAmount
                        + " | Số tiền=" + sotienAmount
                        + " | operator=" + System.getProperty("user.name"));

                StringBuilder message = new StringBuilder("Đã buff thành công cho " + targetName + "!\n");
                if (vndAmount > 0) {
                    message.append("- VNĐ (Cash): ").append(Util.format(vndAmount)).append("\n");
                }
                if (danapAmount > 0) {
                    message.append("- Tổng nạp (danap): ").append(Util.format(danapAmount)).append("\n");
                }
                if (sotienAmount > 0) {
                    message.append("- Số tiền (sotien): ").append(Util.format(sotienAmount)).append("\n");
                }

                JOptionPane.showMessageDialog(this, message.toString(), "Thành công", JOptionPane.INFORMATION_MESSAGE);
                clearForm();
            } else {
                Logger.fileLog("buff_vnd_admin", "BUFF FAIL | target=" + accountName
                        + " | lý do=lỗi ghi database"
                        + " | operator=" + System.getProperty("user.name"));
                JOptionPane.showMessageDialog(this, "Buff thất bại. Có lỗi khi ghi vào database.", "Thất bại", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Vui lòng nhập đúng định dạng số.", "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Optional<Player> getPlayerByName(String name) {
        if (name == null || name.trim().isEmpty()) {
            return Optional.empty();
        }
        return (Client.gI() != null) ? Client.gI().getPlayers().stream()
                .filter(p -> p != null && p.name != null && p.name.equalsIgnoreCase(name.trim()))
                .findFirst() : Optional.empty();
    }

    private void clearForm() {
        accountField.setText("");
        vndField.setText("0");
        danapField.setText("0");
        sotienField.setText("0");
    }
}
