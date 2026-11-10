package Bot;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import server.ServerManager;
import services.map.ChangeMapService;

/**
 * Vòng điều khiển Bot AI.
 *
 * <p>Bot được thêm/bớt từ nhiều luồng khác nhau (panel admin, {@link Sanb} khi
 * không tìm được boss, {@link Bot#getRandomZone}) trong lúc vòng update đang
 * duyệt danh sách, nên danh sách phải an toàn với truy cập song song.
 */
public class BotManager implements Runnable {

    public static BotManager i;

    /** Nhịp update của bot (ms). */
    private static final int TICK_MS = 150;

    /** Trần cứng: trên mức này thì id bot và bộ nhớ bắt đầu thành vấn đề thật. */
    public static final int MAX_BOT_TOI_DA = 5000;

    /**
     * Trần số bot. Đây là mức AN TOÀN mặc định chứ không phải giới hạn kỹ thuật —
     * chỉnh bằng {@link #datTranBot(int)} nếu máy chủ khoẻ. Mỗi bot là một nhân vật
     * trong khu (kèm đệ tử nếu bật), nên tăng trần là tăng tải mạng lẫn CPU.
     */
    public static volatile int MAX_BOT = 1000;

    /** Đổi trần bot lúc đang chạy. Trả về giá trị thực sự được áp (đã kẹp). */
    public static int datTranBot(int tran) {
        MAX_BOT = Math.max(1, Math.min(tran, MAX_BOT_TOI_DA));
        return MAX_BOT;
    }

    /** Số luồng chia nhau update đàn bot. Giữ nhỏ để không tranh CPU với luồng game. */
    private static final int SO_LUONG_THREAD =
            Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() / 2));

    private final java.util.concurrent.ExecutorService poolUpdate =
            java.util.concurrent.Executors.newFixedThreadPool(SO_LUONG_THREAD, r -> {
                Thread t = new Thread(r, "Bot-Update");
                t.setDaemon(true);
                return t;
            });

    /** Đếm số nhịp bị trễ để cảnh báo admin là đang quá tải bot. */
    private int soNhipTre;

    public List<Bot> bot = new CopyOnWriteArrayList<>();

    /** Chặn tạo nhiều lô cùng lúc — mỗi bot vào map là một đợt gói tin cho cả khu. */
    private final AtomicBoolean dangTao = new AtomicBoolean(false);

    /**
     * Tăng mỗi lần xoá sạch bot. Lô đang tạo dở so số này để tự dừng — nếu không, bấm
     * "Xóa tất cả" giữa chừng thì đám bot tạo sau lệnh xoá vẫn lọt vào và không ai quản.
     */
    private final java.util.concurrent.atomic.AtomicInteger theHe =
            new java.util.concurrent.atomic.AtomicInteger();

    public int theHeHienTai() {
        return theHe.get();
    }

    public static BotManager gI() {
        if (i == null) {
            i = new BotManager();
        }
        return i;
    }

    @Override
    public void run() {
        while (ServerManager.isRunning) {
            long st = System.currentTimeMillis();
            try {
                updateMotNhip();
            } catch (Exception e) {
                utils.Logger.logException(BotManager.class, e, "Lỗi nhịp update bot");
            }
            // Trước đây là sleep(150 - elapsed): khi đàn bot chạy quá 150ms thì tham số
            // âm -> ném lỗi -> bị nuốt -> vòng lặp quay tít ăn trọn một core CPU.
            long lam = System.currentTimeMillis() - st;
            long cho = TICK_MS - lam;
            if (cho <= 0) {
                canhBaoQuaTai(lam);
            }
            try {
                Thread.sleep(cho > 0 ? cho : 1);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        poolUpdate.shutdownNow();
    }

    /**
     * Chia đàn bot cho vài luồng cùng update rồi CHỜ tất cả xong mới sang nhịp sau.
     * Chờ đủ là điều kiện bắt buộc: không có nó thì cùng một bot có thể bị hai nhịp
     * update chồng lên nhau. Một luồng duy nhất chỉ kham nổi vài trăm bot.
     */
    private void updateMotNhip() throws InterruptedException {
        final List<Bot> snap = new ArrayList<>(this.bot);
        if (snap.isEmpty()) {
            return;
        }
        int soPhan = Math.min(SO_LUONG_THREAD, snap.size());
        if (soPhan <= 1) {
            for (Bot b : snap) {
                updateMotBot(b);
            }
            return;
        }
        int moiPhan = (snap.size() + soPhan - 1) / soPhan;
        java.util.concurrent.CountDownLatch latch = new java.util.concurrent.CountDownLatch(soPhan);
        for (int p = 0; p < soPhan; p++) {
            final int tu = p * moiPhan;
            final int den = Math.min(tu + moiPhan, snap.size());
            poolUpdate.execute(() -> {
                try {
                    for (int i = tu; i < den; i++) {
                        updateMotBot(snap.get(i));
                    }
                } finally {
                    latch.countDown();
                }
            });
        }
        latch.await();
    }

    private void updateMotBot(Bot b) {
        try {
            b.update();
        } catch (Exception e) {
            // Một bot lỗi không được làm hỏng nhịp update của cả đàn
        }
    }

    /** Nhịp trễ liên tục = đang nuôi quá nhiều bot so với sức máy. */
    private void canhBaoQuaTai(long lam) {
        soNhipTre++;
        if (soNhipTre % 200 == 0) { // ~30 giây một lần, đừng spam log
            utils.Logger.warning("[BotAI] Quá tải: nhịp update mất " + lam + "ms (mốc "
                    + TICK_MS + "ms) với " + this.bot.size() + " bot. Nên giảm số bot.\n");
        }
    }

    /**
     * Tạo một lô bot theo cấu hình từ panel Bot AI.
     *
     * <p>Chạy trên luồng riêng: mỗi bot phải vào map và phát gói cho cả khu, làm
     * thẳng trên luồng giao diện là treo panel.
     *
     * @param type  loại bot (xem {@link Bot#update()}: 0 train quái, 4 săn boss)
     * @param count số lượng yêu cầu
     * @param cfg   cấu hình chỉ số, {@code null} hoặc {@code !custom} = dùng mặc định
     * @return số bot thực sự sẽ được tạo (đã kẹp theo trần {@link #MAX_BOT})
     * @throws IllegalStateException khi đang tạo lô khác hoặc đã đầy bot
     */
    public int spawnBots(int type, int count, BotConfig cfg) {
        if (count < 1) {
            return 0;
        }
        int conTrong = MAX_BOT - this.bot.size();
        if (conTrong <= 0) {
            throw new IllegalStateException("Đã đạt trần " + MAX_BOT + " bot, hãy xóa bớt trước.");
        }
        if (!dangTao.compareAndSet(false, true)) {
            throw new IllegalStateException("Đang tạo lô bot trước, chờ xong đã.");
        }
        final int soTao = Math.min(count, conTrong);
        new Thread(() -> {
            try {
                NewBot.gI().runBot(type, soTao, cfg);
            } catch (Throwable t) {
                utils.Logger.logException(BotManager.class, new Exception(t), "Lỗi tạo bot");
            } finally {
                dangTao.set(false);
            }
        }, "Bot-Spawn").start();
        return soTao;
    }

    /**
     * Gỡ một bot khỏi CẢ hai danh sách. Chỉ xoá khỏi {@link #bot} mà quên danh sách
     * người chơi online sẽ để lại một "người chơi ma" đếm mãi không mất.
     */
    public void xoaBot(Bot b) {
        if (b == null) {
            return;
        }
        this.bot.remove(b);
        try {
            server.Client.gI().removeBot(b);
        } catch (Exception ignored) {
        }
        try {
            if (b.pet != null) {
                ChangeMapService.gI().exitMap(b.pet); // không gỡ thì đệ tử mồ côi nằm lại trong khu
            }
        } catch (Exception ignored) {
        }
        try {
            ChangeMapService.gI().exitMap(b);
        } catch (Exception ignored) {
        }
    }

    /** Đang có lô bot chờ tạo xong hay không. */
    public boolean isSpawning() {
        return dangTao.get();
    }

    /** Số bot hiện có trên server. */
    public int getBotCount() {
        return bot.size();
    }

    /**
     * Xoá toàn bộ bot khỏi server. Phải gỡ khỏi khu bằng {@code exitMap}, nếu chỉ
     * clear danh sách thì bot vẫn đứng trơ trên màn hình người chơi.
     *
     * @return số bot đã xoá
     */
    public int clearAllBots() {
        theHe.incrementAndGet(); // báo lô đang tạo dở dừng lại
        List<Bot> snap = new ArrayList<>(this.bot);
        this.bot.clear();
        for (Bot b : snap) {
            xoaBot(b);
        }
        return snap.size();
    }
}
