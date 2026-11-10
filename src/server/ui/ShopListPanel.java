package server.ui;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.*;
import java.util.Vector;
import jdbc.DBConnecter;

import static server.ui.ServerGuiUtils.*;

public class ShopListPanel extends JPanel {

    private JTable table;
    private DefaultTableModel model;

    // Các ô nhập liệu để sửa
    private JTextField txtId, txtNpcId, txtName, txtType;

    public ShopListPanel() {
        setLayout(new BorderLayout(SP_SM, SP_SM));
        setBackground(BG);
        setBorder(pad(SP_SM));

        initUI();
        loadDataFromDB();
    }

    private Connection getConnection() throws SQLException {
        // Bảng shop nằm ở DB dữ liệu game (nro_data)
        return DBConnecter.getConnectionData();
    }

    private void initUI() {
        // 1. Form chỉnh sửa (Top)
        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setOpaque(false);

        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(5, 5, 5, 5);
        g.fill = GridBagConstraints.HORIZONTAL;

        txtId = new JTextField(5); txtId.setEditable(false); // ID không cho sửa
        txtNpcId = new JTextField(10);
        txtName = new JTextField(15);
        txtType = new JTextField(5);

        // Row 1
        g.gridx = 0; g.gridy = 0; formPanel.add(label("ID Shop:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 1; formPanel.add(txtId, g);

        g.gridx = 2; formPanel.add(label("NPC ID (Avatar):", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 3; formPanel.add(txtNpcId, g);

        g.gridx = 4; formPanel.add(label("Tên Shop:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 5; formPanel.add(txtName, g);

        g.gridx = 6; formPanel.add(label("Type:", FONT_SMALL_B, TEXT_SECONDARY), g);
        g.gridx = 7; formPanel.add(txtType, g);

        // Buttons
        JButton btnSave = button("Lưu Dòng Này", ButtonVariant.PRIMARY, "save");
        btnSave.addActionListener(e -> saveSelectedRow());

        JButton btnReload = button("Tải Lại", ButtonVariant.SECONDARY, "refresh");
        btnReload.addActionListener(e -> loadDataFromDB());

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        btnPanel.setOpaque(false);
        btnPanel.add(btnReload);
        btnPanel.add(btnSave);

        // Layout Form (card + section header)
        JPanel topContainer = card(new BorderLayout());
        topContainer.add(sectionHeader("Chỉnh sửa Shop (NPC Mapping)", "Chọn một dòng trong bảng để đổ dữ liệu lên form.", btnPanel), BorderLayout.NORTH);

        JPanel formWrap = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        formWrap.setOpaque(false);
        formWrap.add(formPanel);
        topContainer.add(formWrap, BorderLayout.CENTER);

        add(topContainer, BorderLayout.NORTH);

        // 2. Bảng dữ liệu (Center)
        String[] cols = {"ID", "NPC ID", "Tên Shop", "Type"};
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int column) { return false; }
        };

        table = new JTable(model);

        // Sự kiện click vào bảng -> đổ dữ liệu lên ô nhập
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    txtId.setText(model.getValueAt(row, 0).toString());
                    txtNpcId.setText(model.getValueAt(row, 1).toString());
                    txtName.setText(model.getValueAt(row, 2).toString());
                    txtType.setText(model.getValueAt(row, 3).toString());
                }
            }
        });

        JPanel tableCard = card(new BorderLayout());
        ((ShadowCard) tableCard).setContentPadding(SP_MD, SP_MD, SP_MD, SP_MD);
        tableCard.add(styleTable(table), BorderLayout.CENTER);

        add(tableCard, BorderLayout.CENTER);
    }

    private void loadDataFromDB() {
        model.setRowCount(0);
        new Thread(() -> {
            try (Connection conn = getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery("SELECT * FROM shop ORDER BY id ASC")) {

                while (rs.next()) {
                    Vector<Object> row = new Vector<>();
                    row.add(rs.getInt("id"));
                    row.add(rs.getInt("npc_id"));

                    // Xử lý tên cột (name hoặc tag_name)
                    try {
                        row.add(rs.getString("name"));
                    } catch (SQLException e) {
                        try { row.add(rs.getString("tag_name")); } catch (Exception ex) { row.add("Unknown"); }
                    }

                    // Xử lý cột type (type hoặc type_shop)
                    try {
                        row.add(rs.getInt("type"));
                    } catch (SQLException e) {
                        try { row.add(rs.getInt("type_shop")); } catch (Exception ex) { row.add(0); }
                    }

                    SwingUtilities.invokeLater(() -> model.addRow(row));
                }

            } catch (SQLException e) {
                e.printStackTrace();
                SwingUtilities.invokeLater(() ->
                    JOptionPane.showMessageDialog(this, "Lỗi tải dữ liệu bảng 'shop': " + e.getMessage())
                );
            }
        }).start();
    }

    private void saveSelectedRow() {
        String idStr = txtId.getText();
        if (idStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Vui lòng chọn một dòng trong bảng trước!");
            return;
        }

        try {
            int id = Integer.parseInt(idStr);
            int npcId = Integer.parseInt(txtNpcId.getText());
            String name = txtName.getText();
            int type = Integer.parseInt(txtType.getText());

            new Thread(() -> {
                // Query cập nhật (Thử tên cột phổ biến trước)
                String query = "UPDATE shop SET npc_id=?, name=?, type=? WHERE id=?";

                try (Connection conn = getConnection();
                     PreparedStatement pstmt = conn.prepareStatement(query)) {

                    pstmt.setInt(1, npcId);
                    pstmt.setString(2, name);
                    pstmt.setInt(3, type);
                    pstmt.setInt(4, id);

                    int affected = pstmt.executeUpdate();
                    SwingUtilities.invokeLater(() -> {
                        if (affected > 0) {
                            JOptionPane.showMessageDialog(this, "Cập nhật thành công Shop ID: " + id);
                            loadDataFromDB(); // Tải lại bảng để cập nhật hiển thị
                        } else {
                            JOptionPane.showMessageDialog(this, "Không tìm thấy ID để cập nhật.");
                        }
                    });

                } catch (SQLException e) {
                    // Nếu lỗi do tên cột, thử query khác (Fallback)
                    if (e.getMessage().contains("Unknown column")) {
                        saveWithAlternativeColumns(id, npcId, name, type);
                    } else {
                        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi SQL: " + e.getMessage()));
                    }
                }
            }).start();

        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Dữ liệu nhập vào không hợp lệ (Phải là số)!");
        }
    }

    // Hàm lưu dự phòng nếu cột tên khác
    private void saveWithAlternativeColumns(int id, int npcId, String name, int type) {
        String query = "UPDATE shop SET npc_id=?, tag_name=?, type_shop=? WHERE id=?";
        try (Connection conn = getConnection();
             PreparedStatement pstmt = conn.prepareStatement(query)) {
            pstmt.setInt(1, npcId);
            pstmt.setString(2, name);
            pstmt.setInt(3, type);
            pstmt.setInt(4, id);
            pstmt.executeUpdate();
            SwingUtilities.invokeLater(() -> {
                JOptionPane.showMessageDialog(this, "Cập nhật thành công (Backup Query) Shop ID: " + id);
                loadDataFromDB();
            });
        } catch (SQLException ex) {
            SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, "Lỗi cả 2 phương án lưu: " + ex.getMessage()));
        }
    }
}
