package models.dungeon;

import utils.Functions;
import models.boss.boss_list.HitEvent.HitPBBoss;
import models.clan.Clan;
import models.map.Zone;
import models.mob.Mob;
import models.player.Player;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;
import server.Maintenance;

public class HalloweenDungeon implements Runnable {

    public static final int MAX_LEVEL = 20;
    public static final long TIME_LIMIT = 30 * 60 * 1000L; // 30p/lượt
    private static final long MOB_HP_PER_LEVEL = 5_000_000L;
    public static final int AVAILABLE = 10;

    public int id;
    public int level;
    public Zone zone;
    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;

    private boolean levelReady;
    private boolean bossSpawned;
    private HitPBBoss currentBoss;

    public HalloweenDungeon(int id) {
        this.id = id;
    }

    public void open(Player plOpen, Clan clan, Zone zone) {
        this.clan = clan;
        this.zone = zone;
        this.level = 1;
        this.levelReady = false;
        this.bossSpawned = false;
        this.currentBoss = null;
        this.lastTimeOpen = System.currentTimeMillis();
        this.isOpened = true;
        clan.halloweenDungeon = this;
        clan.lastTimeOpenHalloween = this.lastTimeOpen;
        clan.playerOpenHalloween = plOpen;
        new Thread(this, "Halloween Dungeon: " + clan.name).start();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long st = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - st), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private void update() {
        if (!isOpened) {
            return;
        }
        if (Util.canDoWithTime(lastTimeOpen, TIME_LIMIT) || zone == null) {
            finishAndDispose();
            return;
        }

        // Chờ có người chơi trong khu rồi mới setup + spawn (tránh spawn lúc zone trống -> client không thấy)
        if (zone.getPlayers().isEmpty()) {
            return;
        }

        if (!levelReady) {
            setupLevel();
            levelReady = true;
            bossSpawned = false;
            currentBoss = null;
        }

        // Spawn boss CÙNG LÚC với quái (retry nếu tạo lỗi -> bossSpawned vẫn false)
        if (!bossSpawned) {
            try {
                // Chỉ tạo boss (status REST). Manager HalloweenEvent tự chạy REST -> respawn() [init nPoint] -> JOIN_MAP -> joinMap().
                // joinMap() base dùng zoneFinal đã set -> boss hiện đúng zone. KHÔNG gọi joinMap() tay (sẽ bỏ qua respawn -> boss lỗi/không hiện).
                currentBoss = new HitPBBoss(zone, level, (level - 1) % 3);
                bossSpawned = true;
                for (Player pl : zone.getPlayers()) {
                    Service.gI().sendThongBao(pl, "Level " + level + " — quái và boss đã xuất hiện!");
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }

        // Qua level khi: hết quái VÀ boss đã chết
        for (Mob mob : zone.mobs) {
            if (!mob.isDie()) {
                return;
            }
        }
        if (currentBoss != null && !currentBoss.isDie()) {
            return;
        }
        onLevelCleared();
    }

    /** Hồi sinh + set HP quái theo level hiện tại. */
    private void setupLevel() {
        long hp = Math.min(MOB_HP_PER_LEVEL * level, 2_000_000_000L);
        for (Mob mob : zone.mobs) {
            mob.lvMob = 0;
            mob.point.maxHp = (int) hp;
            mob.point.dame = (int) Math.min(100L * level, 50_000_000L);
            mob.hoiSinh();
            mob.hoiSinhMobPhoBan();
        }
    }

    private void onLevelCleared() {
        if (currentBoss != null) {
            try {
                currentBoss.leaveMap();
            } catch (Exception e) {
            }
            currentBoss = null;
        }
        if (clan != null && level > clan.levelDoneHalloween) {
            clan.levelDoneHalloween = level;
        }
        for (Player pl : zone.getPlayers()) {
            Service.gI().sendThongBao(pl, "Đã qua level " + level + "! Ra NPC nhận thưởng level này.");
        }
        if (level >= MAX_LEVEL) {
            for (Player pl : zone.getPlayers()) {
                Service.gI().sendThongBao(pl, "Chúc mừng! Hoàn thành phó bản Halloween.");
            }
            finishAndDispose();
        } else {
            level++;
            levelReady = false; // trigger setup level kế tiếp
        }
    }

    private void finishAndDispose() {
        try {
            if (currentBoss != null && !currentBoss.isDie()) {
                currentBoss.leaveMap();
            }
        } catch (Exception e) {
        }
        currentBoss = null;
        // Chỉ đưa người chơi trong ZONE CỦA CLAN NÀY về nhà, không đụng zone của clan khác.
        if (zone != null) {
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    try {
                        Service.gI().sendThongBao(pl, "Hết giờ phó bản Halloween, đưa bạn về nhà!");
                        ChangeMapService.gI().changeMapNonSpaceship(pl, pl.gender + 21, 300, 336);
                    } catch (Exception e) {
                    }
                }
            }
        }
        isOpened = false;
        if (clan != null) {
            clan.halloweenDungeon = null;
            clan = null;
        }
    }
}
