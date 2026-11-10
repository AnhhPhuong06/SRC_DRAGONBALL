package server.ui;

import jdbc.DBConnecter;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static server.ui.ServerGuiUtils.*;

public class NapTienPanel extends JPanel {

    private DefaultTableModel tableModel;
    private JTable table;
    private TableRowSorter<DefaultTableModel> sorter;
    private JTextField searchField;
    private JComboBox<String> playerCombo;
    private JComboBox<String> typeCombo;
    private List<Object[]> allData = new ArrayList<>();

    public NapTienPanel() {
        setLayout(new BorderLayout(0, SP_MD));
        setBackground(BG);
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));

        initUI();
        loadPlayers();
    }

    private void initUI() {
        // ===== Header + toolbar =====
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionHeader("Lịch Sử Nạp Tiền",
                "Tra cứu giao dịch nạp ATM / Card theo tài khoản hoặc nhân vật", null));

        JPanel toolbar = card(new BorderLayout(SP_LG, 0));

        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        filterPanel.setOpaque(false);
        filterPanel.add(label("Tài khoản/Nhân vật:", FONT_SMALL_B, TEXT_SECONDARY));
        playerCombo = new JComboBox<>();
        playerCombo.setPreferredSize(new Dimension(200, 32));
        playerCombo.setEditable(true);
        playerCombo.addActionListener(e -> refreshTable());
        filterPanel.add(playerCombo);

        filterPanel.add(label("Loại nạp:", FONT_SMALL_B, TEXT_SECONDARY));
        typeCombo = new JComboBox<>(new String[]{"Tất cả", "ATM", "Card"});
        typeCombo.setPreferredSize(new Dimension(120, 32));
        typeCombo.addActionListener(e -> refreshTable());
        filterPanel.add(typeCombo);

        JButton btnReload = button("Tải DS Player", ButtonVariant.SECONDARY, "users");
        btnReload.addActionListener(e -> loadPlayers());
        filterPanel.add(btnReload);

        toolbar.add(filterPanel, BorderLayout.WEST);

        JPanel searchPanel = new JPanel(new BorderLayout(SP_SM, 0));
        searchPanel.setOpaque(false);
        searchField = searchField("Tìm kiếm...");
        searchField.setPreferredSize(new Dimension(220, 32));
        searchPanel.add(searchField, BorderLayout.CENTER);

        JButton btnRefresh = button("Làm mới", ButtonVariant.PRIMARY, "refresh");
        btnRefresh.addActionListener(e -> refreshTable());
        searchPanel.add(btnRefresh, BorderLayout.EAST);

        toolbar.add(searchPanel, BorderLayout.EAST);
        top.add(toolbar);
        add(top, BorderLayout.NORTH);

        // ===== Table =====
        String[] columns = {"STT", "Thời gian", "Tài khoản", "Tên nhân vật", "Số tiền", "Loại nạp", "Chi tiết"};
        tableModel = new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        table = new JTable(tableModel);
        sorter = new TableRowSorter<>(tableModel);
        table.setRowSorter(sorter);
        table.getColumnModel().getColumn(0).setPreferredWidth(50);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(120);
        table.getColumnModel().getColumn(5).setPreferredWidth(80);
        table.getColumnModel().getColumn(6).setPreferredWidth(250);

        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0) {
                        int modelRow = table.convertRowIndexToModel(row);
                        showDetails(modelRow);
                    }
                }
            }
        });

        JScrollPane scrollPane = styleTable(table);

        // Cột "Số tiền" nổi bật: đậm + accent SUCCESS, giữ zebra/hover của styleTable
        DefaultTableCellRenderer moneyRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object value, boolean isSelected,
                                                           boolean hasFocus, int row, int column) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, value, isSelected, hasFocus, row, column);
                lbl.setBorder(new EmptyBorder(0, SP_MD, 0, SP_MD));
                Object hoverObj = t.getClientProperty("ui.hoverRow");
                int hover = hoverObj instanceof Integer ? (Integer) hoverObj : -1;
                if (isSelected) {
                    lbl.setBackground(PRIMARY_SOFT);
                } else if (row == hover) {
                    lbl.setBackground(HOVER_ROW);
                } else {
                    lbl.setBackground(row % 2 == 0 ? SURFACE : SURFACE_ALT);
                }
                lbl.setForeground(SUCCESS);
                lbl.setFont(FONT_H2);
                lbl.setHorizontalAlignment(SwingConstants.LEFT);
                return lbl;
            }
        };
        table.getColumnModel().getColumn(4).setCellRenderer(moneyRenderer);

        JPanel tableCard = card(new BorderLayout(0, SP_MD));
        JLabel tableTitle = label("Lịch sử nạp tiền (mới nhất ở trên)", FONT_H3, TEXT_SECONDARY);
        tableTitle.setIcon(vectorIcon("history", 15, TEXT_SECONDARY));
        tableTitle.setIconTextGap(SP_SM);
        tableCard.add(tableTitle, BorderLayout.NORTH);
        tableCard.add(scrollPane, BorderLayout.CENTER);
        add(tableCard, BorderLayout.CENTER);

        // Search filter
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                String text = searchField.getText();
                if (text != null && text.trim().length() >= 2) {
                    refreshTable();
                } else {
                    filter();
                }
            }
            @Override
            public void removeUpdate(DocumentEvent e) {
                String text = searchField.getText();
                if (text != null && text.trim().length() >= 2) {
                    refreshTable();
                } else {
                    filter();
                }
            }
            @Override
            public void changedUpdate(DocumentEvent e) {
                filter();
            }

            private void filter() {
                String text = searchField.getText();
                if (text.trim().isEmpty()) {
                    sorter.setRowFilter(null);
                } else {
                    sorter.setRowFilter(javax.swing.RowFilter.regexFilter("(?i)" + text, 2, 3, 6));
                }
            }
        });
    }

    private void loadPlayers() {
        SwingUtilities.invokeLater(() -> {
            playerCombo.removeAllItems();
            playerCombo.addItem("-- Chọn tài khoản/nhân vật --");

            new Thread(() -> {
                try (Connection con = DBConnecter.getConnectionServer()) {
                    if (con == null) return;

                    // Schema này không có bảng 'transactions' (chỉ có 'napthe') — chỉ lọc theo napthe
                    String sql = "SELECT DISTINCT a.username, p.name " +
                                 "FROM account a " +
                                 "LEFT JOIN player p ON a.id = p.account_id " +
                                 "WHERE EXISTS (SELECT 1 FROM napthe WHERE user_nap = a.username) " +
                                 "ORDER BY a.username";

                    PreparedStatement ps = con.prepareStatement(sql);
                    ResultSet rs = ps.executeQuery();

                    while (rs.next()) {
                        String username = rs.getString("username");
                        String playerName = rs.getString("name");
                        String display = playerName != null && !playerName.isEmpty()
                            ? username + " (" + playerName + ")" : username;
                        SwingUtilities.invokeLater(() -> playerCombo.addItem(display));
                    }

                    rs.close();
                    ps.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void refreshTable() {
        SwingUtilities.invokeLater(() -> {
            tableModel.setRowCount(0);
            allData.clear();

            String selectedPlayer = (String) playerCombo.getSelectedItem();
            if (selectedPlayer == null || selectedPlayer.startsWith("--")) {
                return;
            }

            String typeFilter = (String) typeCombo.getSelectedItem();

            new Thread(() -> {
                try (Connection con = DBConnecter.getConnectionServer()) {
                    if (con == null) return;

                    // Extract username from combo selection
                    String username = selectedPlayer.contains("(")
                        ? selectedPlayer.substring(0, selectedPlayer.indexOf("(")).trim()
                        : selectedPlayer;

                    // Load from transactions table
                    if (!"Card".equals(typeFilter)) {
                        loadTransactions(con, username);
                    }

                    // Load from napthe table
                    if (!"ATM".equals(typeFilter)) {
                        loadNapthe(con, username);
                    }

                    // Sort by time descending
                    allData.sort((a, b) -> {
                        String timeA = (String) a[1];
                        String timeB = (String) b[1];
                        return timeB.compareTo(timeA);
                    });

                    // Add to table
                    int stt = 1;
                    for (Object[] row : allData) {
                        Object[] tableRow = new Object[]{stt++, row[1], row[2], row[3], row[4], row[5], row[6]};
                        SwingUtilities.invokeLater(() -> tableModel.addRow(tableRow));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }).start();
        });
    }

    private void loadTransactions(Connection con, String username) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT created_at, username, amount, type FROM transactions WHERE username = ? ORDER BY created_at DESC"
            );
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                allData.add(new Object[]{
                    null, // STT will be added later
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(rs.getTimestamp("created_at")),
                    rs.getString("username"),
                    "", // Player name - could be joined
                    formatMoney(rs.getLong("amount")),
                    "ATM",
                    rs.getString("type")
                });
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    private void loadNapthe(Connection con, String username) {
        try {
            PreparedStatement ps = con.prepareStatement(
                "SELECT created_at, user_nap, amount, telco FROM napthe WHERE user_nap = ? ORDER BY created_at DESC"
            );
            ps.setString(1, username);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                allData.add(new Object[]{
                    null,
                    new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(rs.getTimestamp("created_at")),
                    rs.getString("user_nap"),
                    "",
                    formatMoney(rs.getLong("amount")),
                    "Card",
                    rs.getString("telco")
                });
            }

            rs.close();
            ps.close();
        } catch (Exception e) {
            // Ignore
        }
    }

    private String formatMoney(long amount) {
        return NumberFormat.getInstance(new Locale("vi", "VN")).format(amount) + " VND";
    }

    private void showDetails(int modelRow) {
        if (modelRow >= 0 && modelRow < allData.size()) {
            Object[] row = allData.get(modelRow);
            JOptionPane.showMessageDialog(this,
                "Thời gian: " + row[1] + "\n" +
                "Tài khoản: " + row[2] + "\n" +
                "Số tiền: " + row[4] + "\n" +
                "Loại nạp: " + row[5] + "\n" +
                "Chi tiết: " + row[6],
                "Chi tiết nạp tiền",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
}
