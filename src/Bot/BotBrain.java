package Bot;

import java.util.List;
import java.util.Random;
import models.boss.Boss;
import models.mob.Mob;
import models.player.Pet;
import models.player.Player;
import services.PetService;
import services.Service;
import services.SkillService;
import services.map.MapService;
import services.player.PlayerService;
import utils.Util;

public class BotBrain {

    private static boolean laVungAnToan(int mapId) {
        MapService ms = MapService.gI();
        try {
            return ms.isHome(mapId) || mapId == 0 || ms.isMapOffline(mapId)
                    || ms.isMapDoanhTrai(mapId) || ms.isMapBlackBallWar(mapId)
                    || ms.isMapBanDoKhoBau(mapId) || ms.isMapMaBu(mapId)
                    || ms.isMapConDuongRanDoc(mapId) || ms.isMapYardart(mapId);
        } catch (Exception e) {
            return true; // không chắc thì coi như cấm
        }
    }

    private static final String[] CAU_CHAT = {
        "ai bán đồ ko", "train mệt quá", "boss đâu rồi mn", "ai team ko",
        "sv đông ghê", "vừa ăn ngọc rồng", "up đệ mãi ko lên", "ai pk ko",
        "hnay drop ngon phết", "mn ơi cho hỏi map này bao nhiêu sm"
    };

    /** Trong tầm này thì đứng yên mà đánh, khỏi phát gói di chuyển thừa cho cả khu. */
    private static final int TAM_DANH = 200;

    /** Bot đứng lệch quanh mục tiêu chừng này px cho tự nhiên, không chồng lên nhau. */
    private static final int TAM_DUNG_CANH = 60;

    private final Bot bot;
    private final Random rd = new Random();
    /** Vòng đời: cày -> mua đồ -> đập/ép -> đủ mạnh mới săn boss. */
    public final BotProgress tienTrinh;

    private Mob mobTarget;
    private Player pkTarget;

    private long lastDoiMap;
    private long lastChat;
    private long lastUpDeTu;
    private long lastTimDeTu;
    private long lastDoiPkTarget;
    private long lastDoiMob;
    private long lastTimBoss;
    private long lastNhiemVu;

    /** Mốc đổi map ngẫu nhiên để cả đàn không đổi map cùng lúc. */
    private long hanDoiMap;

    public BotBrain(Bot bot) {
        this.bot = bot;
        this.tienTrinh = new BotProgress(bot);
        this.lastDoiMap = System.currentTimeMillis();
        this.hanDoiMap = 120_000 + rd.nextInt(180_000);
    }

    public void update() {
        if (bot.zone == null || bot.zone.map == null) {
            return;
        }
        tienTrinh.capNhat();
        loDeTu();
        tuLamNhiemVu();
        if (phaPlayer()) {
            return;
        }
        if (danhBoss()) {
            return;
        }
        trainQuai();
        doiMapNeuCan();
        chatVuVo();
    }

    private void loDeTu() {
        if (!cfgNuoiDeTu()) {
            return;
        }
        if (bot.pet == null) {
            // Đừng gọi liên tục: createNormalPet mở hẳn một thread và ngủ 1 giây
            if (System.currentTimeMillis() - lastTimDeTu < 30_000) {
                return;
            }
            lastTimDeTu = System.currentTimeMillis();
            try {
                PetService.gI().createNormalPet(bot, bot.gender);
            } catch (Exception ignored) {
            }
            return;
        }
        Pet pet = bot.pet;
        // Đệ tử mới nhận còn đứng ngoài map -> kéo vào khu của chủ
        if (pet.zone == null) {
            try {
                pet.status = Pet.FOLLOW;
                pet.joinMapMaster();
            } catch (Exception ignored) {
            }
            return;
        }
        if (System.currentTimeMillis() - lastUpDeTu < 10_000) {
            return;
        }
        lastUpDeTu = System.currentTimeMillis();
        upChiSoDeTu(pet);
    }

    private void upChiSoDeTu(Pet pet) {
        if (pet.nPoint == null) {
            return;
        }
        try {
            if (Util.isTrue(50, 100)) {
                int point = 100;
                long can = point * (2L * (pet.nPoint.hpg + 1000) + point * 20L - 20) / 2;
                if (pet.nPoint.tiemNang >= can) {
                    pet.nPoint.tiemNang -= can;
                    pet.nPoint.hpMax += point;
                    pet.nPoint.hpg += point;
                    Service.gI().point(pet);
                }
            } else {
                int point = 10;
                long can = point * (2L * pet.nPoint.dameg + point - 1) / 2 * 100;
                if (pet.nPoint.tiemNang >= can) {
                    pet.nPoint.tiemNang -= can;
                    pet.nPoint.dameg += point;
                    Service.gI().point(pet);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void tuLamNhiemVu() {
        if (System.currentTimeMillis() - lastNhiemVu < 180_000) { // 3 phút một bước
            return;
        }
        lastNhiemVu = System.currentTimeMillis();
        try {
            if (bot.playerTask == null) {
                return;
            }
            if (bot.playerTask.taskMain == null) {
                bot.playerTask.taskMain = services.TaskService.gI().getTaskMainById(bot, 0);
                return;
            }
            models.task.TaskMain tm = bot.playerTask.taskMain;
            if (tm.subTasks == null || tm.subTasks.isEmpty()) {
                return;
            }
            if (tm.index < tm.subTasks.size() - 1) {
                tm.index++;
                return;
            }
            // Xong nhiệm vụ hiện tại -> sang nhiệm vụ kế
            models.task.TaskMain sau = services.TaskService.gI().getTaskMainById(bot, tm.id + 1);
            if (sau != null && sau.id != tm.id) {
                sau.index = 0;
                bot.playerTask.taskMain = sau;
            }
        } catch (Exception ignored) {
        }
    }

    private boolean phaPlayer() {
        if (!cfgPhaPlayer() || laVungAnToan(bot.zone.map.mapId)) {
            return false;
        }
        if (pkTarget == null || pkTarget.zone != bot.zone || pkTarget.isDie()
                || System.currentTimeMillis() - lastDoiPkTarget > 30_000) {
            pkTarget = timNguoiChoiThat();
            lastDoiPkTarget = System.currentTimeMillis();
        }
        if (pkTarget == null) {
            return false;
        }

        chonChieu();
        if (!bot.UseLastTimeSkill()) {
            return true; // đang chờ hồi chiêu nhưng vẫn coi là đang bận PK
        }
        try {
            int lechPk = rd.nextInt(2 * TAM_DUNG_CANH + 1) - TAM_DUNG_CANH;
            PlayerService.gI().playerMove(bot, pkTarget.location.x + lechPk, pkTarget.location.y);
            SkillService.gI().useSkill(bot, pkTarget, null, -1, null);
        } catch (Exception ignored) {
        }
        return true;
    }

    private Player timNguoiChoiThat() {
        try {
            List<Player> ds = bot.zone.getPlayers();
            for (Player p : ds) {
                if (p != null && p != bot && p.isPl() && !p.isDie()) {
                    return p;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private int getGroundY(int targetX, int targetY) {
        try {
            if (bot.zone == null || bot.zone.map == null) {
                return targetY;
            }
            if (bot.zone.map.mapId == 72) {
                return 312;
            }
            int groundY = bot.zone.map.yPhysicInTop(targetX, Math.max(0, targetY - 24));
            if (groundY <= 0) {
                groundY = bot.zone.map.yPhysicInTop(targetX, 0);
            }
            if (groundY <= 0 && bot.zone.mobs != null && !bot.zone.mobs.isEmpty()) {
                for (Mob m : bot.zone.mobs) {
                    if (m != null && m.location != null && m.location.y > groundY) {
                        groundY = m.location.y;
                    }
                }
            }
            if (groundY <= 0) {
                groundY = (bot.location != null && bot.location.y > 0) ? bot.location.y : targetY;
            }
            return groundY;
        } catch (Exception e) {
            return targetY;
        }
    }

    private boolean danhBoss() {
        Boss b = bossTrongKhu();
        if (b == null) {
            b = diTimBoss(); // không có boss ở đây thì lặn lội đi tìm
        }
        if (b == null) {
            return false;
        }
        chonChieu(); // trước UseLastTimeSkill, xem ghi chú ở phaPlayer()
        if (!bot.UseLastTimeSkill()) {
            return true;
        }
        try {
            int targetX = b.location.x + (bot.location.x > b.location.x ? 40 : -40);
            if (bot.zone != null && bot.zone.map != null) {
                targetX = Math.max(24, Math.min(targetX, bot.zone.map.mapWidth - 24));
            }
            int targetY = getGroundY(targetX, b.location.y);
            if (Math.abs(bot.location.x - b.location.x) > TAM_DUNG_CANH || Math.abs(bot.location.y - targetY) > 50) {
                PlayerService.gI().playerMove(bot, targetX, targetY);
            }
            SkillService.gI().useSkill(bot, b, null, -1, null);
        } catch (Exception ignored) {
        }
        return true;
    }
    private Boss diTimBoss() {
        // Chưa đủ cứng thì lo cày đã, đi boss chỉ tổ chết
        if (!tienTrinh.duocSanBoss()) {
            return null;
        }
        if (System.currentTimeMillis() - lastTimBoss < 20_000) {
            return null;
        }
        lastTimBoss = System.currentTimeMillis();
        try {
            java.util.List<Boss> ds = managers.boss.BossManager.gI().getBosses();
            if (ds == null || ds.isEmpty()) {
                return null;
            }
            // Bắt đầu từ một vị trí ngẫu nhiên để mỗi bot chọn một con khác nhau
            int n = ds.size();
            int batDau = rd.nextInt(n);
            for (int i = 0; i < n; i++) {
                Boss bs = ds.get((batDau + i) % n);
                if (bs == null || bs.isDie() || bs.zone == null || bs.zone.map == null) {
                    continue;
                }
                // Đừng chen vào phó bản/sự kiện, và đừng lấy chỗ của người chơi thật
                if (laVungAnToan(bs.zone.map.mapId) || bs.zone.isFullPlayer()) {
                    continue;
                }
                if (bs.zone == bot.zone) {
                    return bs;
                }
                services.map.ChangeMapService.gI().goToMap(bot, bs.zone);
                bot.datViTriNgauNhien();
                bot.zone.load_Me_To_Another(bot);
                mobTarget = null;
                pkTarget = null;
                return bs;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private Boss bossTrongKhu() {
        try {
            for (Player p : bot.zone.getBosses()) {
                if (p instanceof Boss bs && !bs.isDie()) {
                    return bs;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    // ---------------- Train quái ----------------

    private void trainQuai() {
        try {
            if (bot.zone.mobs.isEmpty()) {
                return;
            }

            boolean hetHan = System.currentTimeMillis() - lastDoiMob > 15_000;
            if (mobTarget == null || mobTarget.isDie() || hetHan) {
                mobTarget = bot.zone.mobs.get(rd.nextInt(bot.zone.mobs.size()));
                lastDoiMob = System.currentTimeMillis();
            }
            if (mobTarget == null || mobTarget.isDie()) {
                return;
            }
            chonChieu(); 
            if (!bot.UseLastTimeSkill()) {
                return;
            }

            int lech = rd.nextInt(2 * TAM_DUNG_CANH + 1) - TAM_DUNG_CANH;
            PlayerService.gI().playerMove(bot, mobTarget.location.x + lech, mobTarget.location.y);
            SkillService.gI().useSkill(bot, null, mobTarget, -1, null);
        } catch (Exception ignored) {
        }
    }

    // ---------------- Linh tinh ----------------

    private void chonChieu() {
        try {
            int n = bot.playerSkill.skills.size();
            if (n <= 0) {
                return;
            }
            bot.playerSkill.skillSelect = bot.playerSkill.skills.get(rd.nextInt(Math.min(n, 2)));
        } catch (Exception ignored) {
        }
    }

    private void doiMapNeuCan() {
        if (System.currentTimeMillis() - lastDoiMap < hanDoiMap) {
            return;
        }
        lastDoiMap = System.currentTimeMillis();
        hanDoiMap = 120_000 + rd.nextInt(180_000);
        mobTarget = null;
        pkTarget = null;
        try {
            bot.joinMap();
        } catch (Exception ignored) {
        }
    }

    private void chatVuVo() {
        if (!cfgChat() || System.currentTimeMillis() - lastChat < 300_000) {
            return;
        }
        lastChat = System.currentTimeMillis() + rd.nextInt(300_000);
        if (!Util.isTrue(1, 4)) {
            return;
        }
        try {
            Service.gI().chat(bot, CAU_CHAT[rd.nextInt(CAU_CHAT.length)]);
        } catch (Exception ignored) {
        }
    }

    // ---------------- Cấu hình ----------------

    private boolean cfgNuoiDeTu() {
        return bot.cfg == null || bot.cfg.nuoiDeTu;
    }

    private boolean cfgPhaPlayer() {
        return bot.cfg != null && bot.cfg.phaPlayer;
    }

    private boolean cfgChat() {
        return bot.cfg == null || bot.cfg.chatTheGioi;
    }
}
