package services;

import models.player.Player;
import server.Client;
import utils.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Dọn "xác trứng" (mobMe) còn sót của người chơi. Chạy nền mỗi 30 phút khi bật.
 * Panel Dọn Rác (MobMeCleanerPanel) điều khiển qua singleton này.
 */
public class MobMeCleanerService {

    private static MobMeCleanerService instance;

    public static MobMeCleanerService gI() {
        if (instance == null) {
            instance = new MobMeCleanerService();
        }
        return instance;
    }

    private static final long INTERVAL_MS = 30 * 60_000L; // 30 phút

    private volatile boolean enabled = false;
    private volatile long lastRunTime = 0L;
    private volatile long lastStartedAt = 0L;
    private volatile int lastClearedCount = 0;
    private volatile long totalCleared = 0L;
    private Thread worker;

    public synchronized void setEnabled(boolean on) {
        if (on == enabled) {
            return;
        }
        enabled = on;
        if (on) {
            lastStartedAt = System.currentTimeMillis();
            startWorker();
        } else if (worker != null) {
            worker.interrupt();
            worker = null;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    private void startWorker() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        worker = new Thread(() -> {
            while (enabled) {
                try {
                    Thread.sleep(INTERVAL_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                if (enabled) {
                    cleanAll();
                    lastStartedAt = System.currentTimeMillis();
                }
            }
        }, "MobMeCleaner");
        worker.setDaemon(true);
        worker.start();
    }

    /** Dọn ngay toàn bộ mobMe của người chơi online. Trả về số xác đã dọn. */
    public int cleanAll() {
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
                if (pl.mobMe != null) {
                    pl.mobMe.mobMeDie();
                    n++;
                }
                if (pl.pet != null && pl.pet.mobMe != null) {
                    pl.pet.mobMe.mobMeDie();
                    n++;
                }
            } catch (Exception ignored) {
            }
        }
        lastRunTime = System.currentTimeMillis();
        lastClearedCount = n;
        totalCleared += n;
        Logger.success("[MobMeCleaner] Đã dọn " + n + " xác trứng\n");
        return n;
    }

    public long getLastRunTime() {
        return lastRunTime;
    }

    public int getLastClearedCount() {
        return lastClearedCount;
    }

    public long getTotalCleared() {
        return totalCleared;
    }

    /** Mili-giây còn lại tới lần dọn kế tiếp; -1 nếu đang tắt. */
    public long getNextRunMs() {
        if (!enabled) {
            return -1L;
        }
        long left = INTERVAL_MS - (System.currentTimeMillis() - lastStartedAt);
        return left < 0 ? 0 : left;
    }
}
