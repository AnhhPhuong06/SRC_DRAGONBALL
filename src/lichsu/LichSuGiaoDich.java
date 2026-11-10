package lichsu;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Lưu lịch sử giao dịch (trade) giữa các player ra file trong thư mục "lichsu/".
 *
 * <p>Mỗi player 1 file {@code lichsu/<id>_<tên>.txt} — mỗi giao dịch được ghi
 * nối tiếp vào file của CẢ HAI người tham gia, nên mở file theo id/tên là thấy
 * toàn bộ lịch sử của người đó. Nếu player đổi tên, file cũ sẽ tự được rename
 * sang tên mới. Chạy song song an toàn; mọi lỗi ghi file đều bị nuốt để không
 * bao giờ làm hỏng luồng giao dịch.
 */
public final class LichSuGiaoDich {

    private static final String FOLDER = "lichsu";
    private static final Object LOCK = new Object();

    private LichSuGiaoDich() {
    }

    /**
     * Ghi 1 giao dịch vào file của cả hai player (theo id + tên).
     */
    public static void luu(long id1, long id2,
                           String name1, String name2,
                           String player1, String player2,
                           String itemPlayer1, String itemPlayer2,
                           String bagBefore1, String bagBefore2,
                           String bagAfter1, String bagAfter2) {
        try {
            Date now = new Date();
            SimpleDateFormat fmtTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            File folder = new File(FOLDER);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String block = "==================================================\n"
                    + "[" + fmtTime.format(now) + "]\n"
                    + "Người 1: " + player1 + "\n"
                    + "   Đưa : " + safe(itemPlayer1) + "\n"
                    + "Người 2: " + player2 + "\n"
                    + "   Đưa : " + safe(itemPlayer2) + "\n"
                    + "Túi P1 trước: " + safe(bagBefore1) + "\n"
                    + "Túi P1 sau  : " + safe(bagAfter1) + "\n"
                    + "Túi P2 trước: " + safe(bagBefore2) + "\n"
                    + "Túi P2 sau  : " + safe(bagAfter2) + "\n"
                    + "==================================================\n\n";

            synchronized (LOCK) {
                ghiFilePlayer(folder, id1, name1, block);
                if (id2 != id1) {
                    ghiFilePlayer(folder, id2, name2, block);
                }
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * Ghi nối tiếp 1 khối giao dịch vào file {@code lichsu/<id>_<tên>.txt}.
     * Nếu đã có file của id đó (dù dạng {@code <id>.txt} cũ hoặc {@code <id>_<tên cũ>.txt}),
     * rename sang tên hiện tại và ghi tiếp — giữ nguyên lịch sử.
     */
    private static void ghiFilePlayer(File folder, long playerId, String playerName, String block) {
        try {
            String safeName = sanitize(playerName);
            File target = new File(folder, playerId + (safeName.isEmpty() ? "" : "_" + safeName) + ".txt");

            // Nếu chưa có file target, thử tìm file cũ của cùng id (định dạng cũ / tên cũ) để rename cho khớp.
            if (!target.exists()) {
                File existing = timFileTheoId(folder, playerId, target.getName());
                if (existing != null) {
                    if (!existing.renameTo(target)) {
                        // Rename lỗi (bị khóa hoặc khác volume) → cứ ghi vào file cũ để không mất lịch sử.
                        target = existing;
                    }
                }
            }

            try (Writer w = new OutputStreamWriter(new FileOutputStream(target, true), StandardCharsets.UTF_8)) {
                w.write(block);
            }
        } catch (Exception ignored) {
        }
    }

    /** Tìm file lịch sử hiện có của player id: khớp {@code <id>.txt} hoặc {@code <id>_*.txt}. */
    private static File timFileTheoId(File folder, long playerId, String excludeName) {
        String prefix = playerId + "_";
        String legacy = playerId + ".txt";
        File[] files = folder.listFiles();
        if (files == null) return null;
        for (File f : files) {
            if (!f.isFile()) continue;
            String n = f.getName();
            if (n.equals(excludeName)) continue;
            if (n.equals(legacy) || (n.startsWith(prefix) && n.toLowerCase().endsWith(".txt"))) {
                return f;
            }
        }
        return null;
    }

    /** Loại bỏ ký tự cấm trong tên file trên Windows/Linux; rút gọn để tránh path quá dài. */
    private static String sanitize(String name) {
        if (name == null) return "";
        String s = name.replaceAll("[\\\\/:*?\"<>|\\r\\n\\t]", "_").trim();
        if (s.length() > 40) s = s.substring(0, 40);
        return s;
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
