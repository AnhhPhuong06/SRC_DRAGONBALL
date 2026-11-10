/*
 * Nhiệm vụ KOL — 40 mốc.
 * KHUNG: target (maxCount) + mô tả từng mốc đã dựng.
 * TODO còn lại (đợt sau): (1) gắn hook đếm tiến độ cho từng mốc, (2) phần thưởng thường + VIP cho đủ 40 mốc.
 */
package models.task;

import database.daos.PlayerDAO;
import java.util.ArrayList;
import java.util.List;
import models.item.Item;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.player.InventoryService;

public class Task_Kol {

    public static final int MAX_LEVEL = 40;

    public long count;
    public long maxCount;
    public int level;
    public boolean[] isVipRewarded = new boolean[MAX_LEVEL];
    public boolean[] isRewarded = new boolean[MAX_LEVEL];
    private static Task_Kol instance;

    public static Task_Kol gI() {
        if (instance == null) {
            instance = new Task_Kol();
        }
        return instance;
    }

    public Task_Kol() {
        this(1);
    }

    public Task_Kol(int level) {
        this.level = level;
        this.count = 0;
        updateMaxCount();
    }

    /** Số lượng mục tiêu (target) của từng mốc KOL 1..40. */
    private static final long[] MAX_COUNTS = {
        30,             // KOL 1  : 30 nhiệm vụ Siêu khó
        10,             // KOL 2  : nhặt 10 viên NR 7 sao
        50_000,         // KOL 3  : 50k quái
        1,              // KOL 4  : 1 Bư Han
        1,              // KOL 5  : ước NR 1 sao
        30_000,         // KOL 6  : giết 30.000 con Sên
        99,             // KOL 7  : 99 nhiệm vụ Dễ
        3,              // KOL 8  : 3 Bư Han
        100_000_000_000L, // KOL 9 : 100 tỷ TN
        5_000,          // KOL 10 : tiêu 5.000 Thỏi Vàng
        1,              // KOL 11 : nâng Chí Mạng lên 3
        10,             // KOL 12 : tiêu diệt 10 Ăn Trộm
        10,             // KOL 13 : tiêu diệt 10 Ở Dơ
        1,              // KOL 14 : nâng Chí Mạng lên 5
        5,              // KOL 15 : ước 5 viên NR 1 sao
        99,             // KOL 16 : nhặt 99 viên NR 7 sao
        10_000,         // KOL 17 : úp 10k quái cùng 1 thành viên tổ đội
        50,             // KOL 18 : mở 50 Rương Huyền Bí (id 1009)
        20,             // KOL 19 : vào phó bản Bản Đồ Kho Báu 20 lần
        500,            // KOL 20 : nhặt 500 viên CSKB
        50,             // KOL 21 : 50 nhiệm vụ Siêu khó
        10,             // KOL 22 : tiêu diệt 10 Thủy Tinh
        10,             // KOL 23 : tiêu diệt 10 Sơn Tinh
        100_000,        // KOL 24 : 100k quái
        1,              // KOL 25 : ước NR 1 sao
        10,             // KOL 26 : tiêu diệt 10 boss Black Goku
        5_000,          // KOL 27 : nhặt 5000 Capsule kì bí (id 380)
        100,            // KOL 28 : có 100 điểm Đường Tăng
        1,              // KOL 29 : đạt 90 tỷ sức mạnh
        50_000,         // KOL 30 : nạp 50k
        10,             // KOL 31 : đi phó bản Khí Gas 10 lần
        150_000,        // KOL 32 : 150k quái
        5,              // KOL 33 : pem 5 Drabula
        1,              // KOL 34 : pem 1 Black Goku
        10,             // KOL 35 : đi phó bản Halloween 10 lần
        99,             // KOL 36 : dùng 99 Cuồng Nộ
        10,             // KOL 37 : mở 10 Rương Ngọc Rồng (id 1150)
        200,            // KOL 38 : 200 nhiệm vụ Bò Mộng
        2,              // KOL 39 : mở 2 Rương Gỗ (id 570)
        150             // KOL 40 : tiêu diệt 150 con Xên Bọ Hùng
    };

    /** Mô tả nhiệm vụ từng mốc KOL 1..40. */
    private static final String[] TASKS = {
        "Hoàn thành 30 nhiệm vụ Siêu khó",
        "Nhặt 10 viên Ngọc Rồng 7 sao",
        "Hạ 50.000 quái",
        "Tham gia hạ boss Majin Buu 1 lần",
        "Ước Ngọc Rồng 1 sao",
        "Tiêu diệt 30.000 con Sên",
        "Hoàn thành 99 nhiệm vụ Dễ",
        "Tham gia hạ boss Majin Buu 3 lần",
        "Kiếm 100 tỷ tiềm năng",
        "Tiêu 5.000 Thỏi Vàng",
        "Nâng Chí Mạng lên 3",
        "Tiêu diệt 10 Ăn Trộm",
        "Tiêu diệt 10 Ở Dơ",
        "Nâng Chí Mạng lên 5",
        "Ước 5 viên Ngọc Rồng 1 sao",
        "Nhặt 99 viên Ngọc Rồng 7 sao",
        "Úp 10.000 quái cùng 1 thành viên tổ đội",
        "Mở 50 Rương Huyền Bí",
        "Vào phó bản Bản Đồ Kho Báu 20 lần",
        "Nhặt 500 viên CSKB",
        "Hoàn thành 50 nhiệm vụ Siêu khó",
        "Tiêu diệt 10 Thủy Tinh",
        "Tiêu diệt 10 Sơn Tinh",
        "Hạ 100.000 quái",
        "Ước Ngọc Rồng 1 sao",
        "Tiêu diệt 10 boss Black Goku",
        "Nhặt 5000 Capsule kì bí",
        "Có 100 điểm Đường Tăng",
        "Đạt 90 tỷ sức mạnh",
        "Nạp 50.000",
        "Đi phó bản Khí Gas 10 lần",
        "Hạ 150.000 quái",
        "Pem 5 Drabula",
        "Pem 1 Black Goku",
        "Đi phó bản Halloween 10 lần",
        "Dùng 99 Cuồng Nộ",
        "Mở 10 Rương Ngọc Rồng",
        "Hoàn thành 200 nhiệm vụ Bò Mộng",
        "Mở 2 Rương Gỗ",
        "Tiêu diệt 150 con Xên Bọ Hùng"
    };

    private void updateMaxCount() {
        int idx = this.level - 1;
        this.maxCount = (idx >= 0 && idx < MAX_COUNTS.length) ? MAX_COUNTS[idx] : 0;
    }
    public void sanitize() {
        if (this.level < 1 || this.level > MAX_LEVEL) {
            this.level = 1;
            this.count = 0;
        }
        updateMaxCount(); // maxCount luôn suy ra từ level, bỏ giá trị lưu sai
        if (this.count < 0) {
            this.count = 0;
        }
        if (this.count > this.maxCount) {
            this.count = this.maxCount;
        }
    }
    public boolean isDone() {
        return this.count >= this.maxCount;
    }

    public void levelUp() {
        if (isDone() && this.level < MAX_LEVEL) {
            this.level++;
            this.count = 0;
            updateMaxCount();
        }
    }

    public int getPercentProcess() {
        return (maxCount <= 0 || count >= maxCount) ? 100 : (int) (count * 100 / maxCount);
    }

    public long isDoneCount() {
        return (count < maxCount) ? ++count : count;
    }

    /**
     * Tăng tiến độ KOL thêm 'amount' NẾU mốc hiện tại của player nằm trong 'levels'.
     * Không lưu DB mỗi lần (tránh spam ghi với event tần suất cao); count lưu theo autosave/logout.
     */
    public static void tick(Player pl, long amount, int... levels) {
        if (pl == null || pl.isBoss || pl.isPet || pl.playerTask == null || pl.playerTask.task_kol == null) {
            return;
        }
        Task_Kol t = pl.playerTask.task_kol;
        if (t.isDone()) {
            return;
        }
        for (int lv : levels) {
            if (t.level == lv) {
                t.count = Math.min(t.count + amount, t.maxCount);
                return;
            }
        }
    }

    /** Đặt tiến độ = max (hoàn thành ngay) nếu đang ở mốc 'forLevel' — dùng cho mốc kiểu đạt-mốc (nâng chí mạng, đạt SM). */
    public static void complete(Player pl, int forLevel) {
        if (pl == null || pl.isBoss || pl.isPet || pl.playerTask == null || pl.playerTask.task_kol == null) {
            return;
        }
        Task_Kol t = pl.playerTask.task_kol;
        if (t.level == forLevel && !t.isDone()) {
            t.count = t.maxCount;
        }
    }

    /**
     * Kiểm tra các mốc "đạt ngưỡng" theo trạng thái HIỆN TẠI (không cần chờ sự kiện tăng).
     * Gọi khi mở menu KOL để mốc tự hoàn thành nếu người chơi đã vượt ngưỡng từ trước.
     */
    public static void checkThresholds(Player pl) {
        if (pl == null || pl.isBoss || pl.isPet || pl.playerTask == null || pl.playerTask.task_kol == null) {
            return;
        }
        if (pl.nPoint != null) {
            if (pl.nPoint.power >= 90_000_000_000L) complete(pl, 29); // KOL 29
            if (pl.nPoint.critg >= 3) complete(pl, 11);                // KOL 11
            if (pl.nPoint.critg >= 5) complete(pl, 14);                // KOL 14
        }
        if (pl.inventory != null && pl.inventory.diemDT >= 100) complete(pl, 28); // KOL 28
        try {
            if (pl.getSession() != null && pl.getSession().danap >= 50000) complete(pl, 30); // KOL 30
        } catch (Exception ignored) {
        }
    }

    public String getLevelDescriptionBase(int level, boolean isVip) {
        int idx = level - 1;
        if (idx < 0 || idx >= TASKS.length) {
            return "";
        }
        return String.format("KOL %d :\n%s\nPhần thưởng: %s", level, TASKS[idx], rewardDesc(level));
    }

    public void NhanKOL(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 3 ô trống hành trang");
            return;
        }
        int idx = player.playerTask.task_kol.level - 1;
        if (idx < 0 || idx >= player.playerTask.task_kol.isRewarded.length || player.playerTask.task_kol.isRewarded[idx]) {
            return;
        }

        // Thưởng theo mốc (mặc định 50 Thỏi Vàng; 10/20/30 = 5 Rương Huyền Bí).
        for (Item it : rewardItems(player.playerTask.task_kol.level)) {
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + it.template.name);
            InventoryService.gI().addItemBag(player, it);
        }
        player.playerTask.task_kol.isRewarded[idx] = true;
        InventoryService.gI().sendItemBags(player);
        player.playerTask.task_kol.levelUp();
        PlayerDAO.updatePlayer(player);
    }

    public void NhanKOLVip(Player player) {
        if (InventoryService.gI().getCountEmptyBag(player) < 3) {
            Service.gI().sendThongBao(player, "Bạn phải có ít nhất 3 ô trống hành trang");
            return;
        }
        List<Integer> pendingVip = new ArrayList<>();
        for (int i = 0; i < player.playerTask.task_kol.isRewarded.length; i++) {
            // Dùng flag của player (hàm gọi trên singleton Task_Kol.gI() nên "this.isVipRewarded" sẽ sai).
            if (player.playerTask.task_kol.isRewarded[i] && !player.playerTask.task_kol.isVipRewarded[i]) {
                pendingVip.add(i + 1);
            }
        }
        if (pendingVip.isEmpty()) {
            return;
        }
        int claimLevel = pendingVip.get(0);
        int idx = claimLevel - 1;
        if (player.playerTask.task_kol.isVipRewarded[idx]) {
            return;
        }
        if (InventoryService.gI().findItemBag(player, 1175) == null) {
            Service.gI().sendThongBao(player, "Chỗ Anh Em Khuyên Thật Ủng Hộ Cái Vé Tháng Đi");
            return;
        }
        // Thưởng VIP theo mốc (mặc định 50 Thỏi Vàng; 10/20/30 = 5 Rương Huyền Bí).
        for (Item it : rewardItems(claimLevel)) {
            Service.gI().sendThongBao(player, "Bạn đã nhận được " + it.template.name);
            InventoryService.gI().addItemBag(player, it);
        }
        player.playerTask.task_kol.isVipRewarded[idx] = true;
        InventoryService.gI().sendItemBags(player);
        PlayerDAO.updatePlayer(player);
    }

    /** Phần thưởng theo mốc KOL. Mọi mốc: 50 Thỏi Vàng; riêng 10/20/30 thêm 5 Rương Huyền Bí. Đều khóa giao dịch (opt 30=0). */
    private Item[] rewardItems(int level) {
        List<Item> items = new ArrayList<>();
        items.add(createItem(457, 50, new int[][]{{30, 0}})); // 50 Thỏi Vàng cho mọi mốc
        if (level == 10 || level == 20 || level == 30) {
            items.add(createItem(1009, 5, new int[][]{{30, 0}})); // + 5 Rương Huyền Bí
        }
        return items.toArray(new Item[0]);
    }

    /** Mô tả phần thưởng (hiển thị menu) theo mốc. */
    private static String rewardDesc(int level) {
        return switch (level) {
            case 10, 20, 30 -> "50 Thỏi Vàng + 5 Rương Huyền Bí";
            default -> "50 Thỏi Vàng";
        };
    }

    private Item createItem(int id, int quantity, int[][] options) {
        Item item = ItemService.gI().createNewItem((short) id, quantity);
        for (int[] opt : options) {
            if (opt != null && opt.length >= 2) {
                item.itemOptions.add(new Item.ItemOption(opt[0], opt[1]));
            }
        }
        return item;
    }

}
