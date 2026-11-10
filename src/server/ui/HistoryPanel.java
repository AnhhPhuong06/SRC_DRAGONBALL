package server.ui;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static server.ui.ServerGuiUtils.*;


public class HistoryPanel extends JPanel {

    private static final String FOLDER = "lichsu";

    private JComboBox<String> fileCombo;
    private JTextField playerSearchField;
    private JTextArea textArea;
    private JTextField searchField;
    private JLabel statusLbl;
    private String currentContent = "";
    private final List<String> allFileNames = new ArrayList<>();
    private boolean updatingCombo = false;

    public HistoryPanel() {
        setLayout(new BorderLayout());
        setBackground(BG);
        setBorder(new EmptyBorder(SP_XL, SP_XL, SP_XL, SP_XL));
        initUI();
        loadFileList();
    }

    private void initUI() {
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(sectionHeader("Lịch Sử Người Chơi",
                "Lịch sử giao dịch lưu trong thư mục lichsu/ — mỗi player 1 file .txt theo id", null));
        top.add(Box.createVerticalStrut(SP_LG));
        top.add(buildToolbar());
        top.add(Box.createVerticalStrut(SP_LG));

        add(top, BorderLayout.NORTH);
        add(buildContent(), BorderLayout.CENTER);
    }

    private JPanel buildToolbar() {
        JPanel card = card(new BorderLayout(SP_LG, 0));
        card.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JPanel left = new JPanel(new FlowLayout(FlowLayout.LEFT, SP_SM, 0));
        left.setOpaque(false);
        left.add(label("Player id (file):", FONT_SMALL_B, TEXT_SECONDARY));
        fileCombo = new JComboBox<>();
        fileCombo.setPreferredSize(new Dimension(200, 32));
        fileCombo.addActionListener(e -> { if (!updatingCombo) loadSelectedFile(); });
        left.add(fileCombo);
        JButton btnReload = button("Tải lại DS", ButtonVariant.PRIMARY, "refresh");
        btnReload.addActionListener(e -> loadFileList());
        left.add(btnReload);

        // Tìm player theo tên (hoặc id): lọc ngay danh sách file trong dropdown
        left.add(label("Tìm player:", FONT_SMALL_B, TEXT_SECONDARY));
        playerSearchField = searchField("Tên hoặc id player...");
        playerSearchField.setPreferredSize(new Dimension(160, 32));
        playerSearchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { populateCombo(); }
            @Override public void removeUpdate(DocumentEvent e) { populateCombo(); }
            @Override public void changedUpdate(DocumentEvent e) { populateCombo(); }
        });
        left.add(playerSearchField);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, SP_SM, 0));
        right.setOpaque(false);
        right.add(label("Lọc:", FONT_SMALL_B, TEXT_SECONDARY));
        searchField = searchField("Gõ tên player / vật phẩm để lọc...");
        searchField.setPreferredSize(new Dimension(240, 32));
        searchField.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void removeUpdate(DocumentEvent e) { applyFilter(); }
            @Override public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        right.add(searchField);

        card.add(left, BorderLayout.WEST);
        card.add(right, BorderLayout.EAST);
        return card;
    }

    private JPanel buildContent() {
        JPanel card = card(new BorderLayout(0, SP_MD));

        statusLbl = label("Chọn một ngày để xem lịch sử.", FONT_SMALL, TEXT_SECONDARY);
        statusLbl.setIcon(vectorIcon("history", 14, TEXT_MUTED));
        statusLbl.setIconTextGap(SP_SM);
        statusLbl.setBorder(new EmptyBorder(0, 0, SP_SM, 0));

        textArea = new JTextArea();
        textArea.setTabSize(4);

        JScrollPane sp = styleConsole(textArea);
        sp.getVerticalScrollBar().setUnitIncrement(20);
        sp.setPreferredSize(new Dimension(0, 520));

        card.add(statusLbl, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        return card;
    }

    // ----------------------------------------------------------------------

    private void loadFileList() {
        SwingUtilities.invokeLater(() -> {
            allFileNames.clear();
            File dir = new File(FOLDER);
            if (!dir.exists() || !dir.isDirectory()) {
                populateCombo();
                statusLbl.setText("Chưa có thư mục 'lichsu/' — chưa có giao dịch nào được lưu.");
                textArea.setText("");
                currentContent = "";
                return;
            }
            File[] files = dir.listFiles((d, name) -> name.toLowerCase().endsWith(".txt"));
            if (files == null || files.length == 0) {
                populateCombo();
                statusLbl.setText("Thư mục 'lichsu/' chưa có file .txt nào.");
                textArea.setText("");
                currentContent = "";
                return;
            }
            // Tên file dạng <id>.txt → sort tăng dần theo id (số) cho dễ tìm; fallback theo tên.
            Arrays.sort(files, (a, b) -> {
                Long ida = idOfFile(a.getName()), idb = idOfFile(b.getName());
                if (ida != null && idb != null) return Long.compare(ida, idb);
                return a.getName().compareToIgnoreCase(b.getName());
            });
            for (File f : files) {
                allFileNames.add(f.getName());
            }
            populateCombo();
        });
    }

    /**
     * Đổ danh sách file vào dropdown, lọc theo tên/id đang gõ ở ô "Tìm player".
     * Tự chọn và mở file khớp đầu tiên; báo rõ khi không có player nào khớp.
     */
    private void populateCombo() {
        String raw = playerSearchField == null ? "" : playerSearchField.getText().trim();
        String q = raw.toLowerCase();
        updatingCombo = true;
        fileCombo.removeAllItems();
        int matches = 0;
        for (String fileName : allFileNames) {
            // Tên file dạng <id>_<tên>.txt nên khớp cả theo tên player lẫn theo id.
            if (q.isEmpty() || fileName.toLowerCase().contains(q)) {
                fileCombo.addItem(fileName);
                matches++;
            }
        }
        if (matches > 0) {
            fileCombo.setSelectedIndex(0);
        }
        updatingCombo = false;

        if (matches > 0) {
            loadSelectedFile();
        } else {
            currentContent = "";
            if (allFileNames.isEmpty()) {
                textArea.setText("");
            } else {
                textArea.setText("(Không có player nào khớp \"" + raw + "\")");
                statusLbl.setText("Không tìm thấy player khớp \"" + raw + "\" — thử tên khác hoặc bấm Tải lại DS.");
            }
        }
    }

    private void loadSelectedFile() {
        String name = (String) fileCombo.getSelectedItem();
        if (name == null) {
            return;
        }
        File f = new File(FOLDER, name);
        try {
            byte[] bytes = Files.readAllBytes(f.toPath());
            currentContent = new String(bytes, StandardCharsets.UTF_8);
            applyFilter();
            statusLbl.setText("File: lichsu/" + name + "   •   " + formatSize(f.length()));
        } catch (Exception ex) {
            currentContent = "";
            textArea.setText("Lỗi đọc file: " + ex.getMessage());
            statusLbl.setText("Lỗi đọc file lichsu/" + name);
        }
    }

    /** Lọc theo từng KHỐI giao dịch (ngăn bởi dòng trống): giữ khối chứa từ khoá. */
    private void applyFilter() {
        String q = searchField == null ? "" : searchField.getText().trim().toLowerCase();
        if (q.isEmpty()) {
            textArea.setText(currentContent);
        } else {
            StringBuilder out = new StringBuilder();
            for (String block : currentContent.split("\\r?\\n\\r?\\n")) {
                if (block.toLowerCase().contains(q)) {
                    out.append(block).append("\n\n");
                }
            }
            textArea.setText(out.length() > 0 ? out.toString()
                    : "(Không có giao dịch nào khớp \"" + searchField.getText().trim() + "\")");
        }
        textArea.setCaretPosition(0);
    }

    /** Lấy id từ tên file "&lt;id&gt;.txt" hoặc "&lt;id&gt;_&lt;tên&gt;.txt"; null nếu không phải dạng số. */
    private static Long idOfFile(String fileName) {
        try {
            String stem = fileName.replaceAll("(?i)\\.txt$", "");
            int us = stem.indexOf('_');
            String idStr = us > 0 ? stem.substring(0, us) : stem;
            return Long.parseLong(idStr);
        } catch (Exception e) {
            return null;
        }
    }

    private static String formatSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        }
        if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        }
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}
