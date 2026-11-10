package server;

import database.daos.PlayerDAO;
import models.player.Player;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Tự động lưu dữ liệu người chơi online theo chu kỳ. Singleton.
 * Panel Dashboard / ServerManagerUI bật/tắt và gọi lưu thủ công qua đây.
 */
public class AutoSaveManager {

    private static AutoSaveManager instance;

    public static AutoSaveManager getInstance() {
        if (instance == null) {
            instance = new AutoSaveManager();
        }
        return instance;
    }

    /** Chu kỳ lưu mặc định: 1 phút để dữ liệu người chơi cập nhật nhanh lên BXH. */
    private static final long INTERVAL_MS = 60_000L;

    private volatile boolean running = false;
    private Thread worker;
    private volatile long lastSaveAt = 0L;
    private volatile int lastSavedCount = 0;

    public synchronized void startAutoSave() {
        if (running) {
            return;
        }
        running = true;
        worker = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (running) {
                    handleAutoSave();
                }
            }
        }, "AutoSaveManager");
        worker.setDaemon(true);
        worker.start();
        Logger.success("[AutoSave] Bật tự động lưu mỗi " + (INTERVAL_MS / 1000) + " giây\n");
    }

    public synchronized void stopAutoSave() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        Logger.warning("[AutoSave] Tắt tự động lưu\n");
    }

    public boolean isRunning() {
        return running;
    }

    /** Lưu ngay toàn bộ người chơi online. Trả về số người đã lưu. */
    public int handleAutoSave() {
        int n = 0;
        List<Player> snapshot;
        try {
            snapshot = new ArrayList<>(Client.gI().getPlayers());
        } catch (Exception e) {
            return 0;
        }
        for (Player pl : snapshot) {
            if (pl == null) {
                continue;
            }
            try {
                PlayerDAO.updatePlayer(pl);
                n++;
            } catch (Exception ignored) {
            }
        }
        lastSaveAt = System.currentTimeMillis();
        lastSavedCount = n;
        Logger.success("[AutoSave] Đã lưu " + n + " người chơi\n");
        return n;
    }

    public long getLastSaveAt() {
        return lastSaveAt;
    }

    public int getLastSavedCount() {
        return lastSavedCount;
    }
}
