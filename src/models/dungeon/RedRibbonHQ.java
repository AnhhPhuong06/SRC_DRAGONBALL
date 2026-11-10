package models.dungeon;

/*
 * @Author: 0789305260
 */
import utils.Functions;
import models.boss.Boss;
import models.boss.boss_list.RedRibbonHQ.NinjaAoTim;
import models.boss.boss_list.RedRibbonHQ.RobotVeSi;
import models.boss.boss_list.RedRibbonHQ.TrungUyThep;
import models.boss.boss_list.RedRibbonHQ.TrungUyTrang;
import models.boss.boss_list.RedRibbonHQ.TrungUyXanhLo;
import models.clan.Clan;
import models.map.Zone;
import models.mob.Mob;
import models.player.Player;
import services.map.ChangeMapService;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import models.map.ItemMap;
import server.Maintenance;
import services.map.ItemMapService;
import services.ItemTimeService;
import services.map.MapService;
import services.Service;
import utils.Util;

@Data
public class RedRibbonHQ implements Runnable {

    public static final int N_PLAYER_CLAN = 5;
    public static final int N_PLAYER_MAP = 1;
    public static final int AVAILABLE = 50;
    public static final int TIME_DOANH_TRAI = 1800000;
    public static final int TIME_PICK_DOANH_TRAI = 300000;

    public int id;
    public final List<Zone> zones;
    private Clan clan;

    private long lastTimeOpen;
    public boolean isOpened;
    public boolean isTimePicking;
    public long lastTimePick;
    public boolean winDT;
    public List<Boss> bosses = new ArrayList<>();

    public RedRibbonHQ(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
    }

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && isOpened) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    public void openDoanhTrai(Player player) {
        if (player == null || player.clan == null) {
            return;
        }
        try {
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = player.clan;
            player.clan.doanhTrai = this;
            player.clan.playerOpenDoanhTrai = player;
            player.clan.lastTimeOpenDoanhTrai = this.lastTimeOpen;
            player.clan.haveGoneDoanhTrai = false;
            sendTextDoanhTrai();
            this.isOpened = true;
            this.init();
        } catch (Exception e) {
            e.printStackTrace();
            if (player.clan != null) {
                player.clan.lastTimeOpenDoanhTrai = 0;
                player.clan.haveGoneDoanhTrai = false;
            }
            this.dispose();
            return;
        }
        List<Player> plJoinDT = new ArrayList<>();
        for (Player pl : player.zone.getPlayers()) {
            if (pl != null && !pl.equals(player) && pl.clan != null
                    && pl.clan.equals(player.clan) && pl.location.x >= 1285
                    && pl.location.x <= 1645) {
                plJoinDT.add(pl);
            }
        }
        for (Player pl : plJoinDT) {
            if (pl.clanMember.getNumDateFromJoinTimeToToday() < 1 || pl.isDie()) {
                continue;
            }
            pl.lastTimeJoinDT = System.currentTimeMillis();
            ChangeMapService.gI().changeMapInYard(pl, 53, -1, 60);
        }
        player.lastTimeJoinDT = System.currentTimeMillis();
        ChangeMapService.gI().changeMapInYard(player, 53, -1, 60);
    }

    private void init() {
        long totalDamage = 0;
        long totalHp = 0;

        for (Player player : this.clan.membersInGame) {
            if (player != null && player.nPoint != null) {
                totalDamage += player.nPoint.dame;
                totalHp += player.nPoint.hpMax;
            }
        }
        for (Zone zone : this.zones) {
            for (Mob mob : zone.mobs) {
                long mobTempId = mob.tempId;
                mob.point.dame = (int) ((mobTempId != 0) ? Math.min(totalHp / mobTempId, 2_000_000_000L) : 0);
                mob.point.maxHp = (int) ((mobTempId != 0) ? Math.min(totalDamage * mobTempId, 2_000_000_000L) : 0);
                mob.lvMob = 0;
                mob.hoiSinh();
                mob.hoiSinhMobPhoBan();
            }

            long dame = totalHp / 20;
            long hp = totalDamage * 50;

            if (zone.map.mapId == 59) {
                try {
                    long bossDamage = Math.min(dame, 200_000_000L);
                    long bossMaxHealth = Math.min(hp, 2_000_000_000L);
                    bosses.add(new TrungUyTrang(zone, (int) bossDamage, (int) bossMaxHealth));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 62) {
                try {
                    long bossDamage = Math.min(dame * 11L / 10, 200_000_000L);
                    long bossMaxHealth = Math.min(hp * 11L / 10, 2_000_000_000L);
                    bosses.add(new TrungUyXanhLo(zone, (int) bossDamage, (int) bossMaxHealth));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 55) {
                try {
                    long bossDamage = Math.min(dame * 115L / 100, 200_000_000L);
                    long bossMaxHealth = Math.min(hp * 115L / 100, 2_000_000_000L);
                    bosses.add(new TrungUyThep(zone, (int) bossDamage, (int) bossMaxHealth));
                } catch (Exception e) {
                }
            }
            if (zone.map.mapId == 54) {
                try {
                    long bossDamage = Math.min(dame * 12L / 10, 200_000_000L);
                    long bossMaxHealth = Math.min(hp * 12L / 10, 2_000_000_000L);
                    bosses.add(new NinjaAoTim(zone, clan, (int) bossDamage, (int) bossMaxHealth));
                } catch (Exception e) {
                }
            }

            if (zone.map.mapId == 57) {
                try {
                    long bossDamage = Math.min(dame * 13L / 10, 200_000_000L);
                    long bossMaxHealth = Math.min(hp * 13L / 10, 2_000_000_000L);
                    for (int i = 0; i < 4; i++) {
                        bosses.add(new RobotVeSi(zone, i, (int) bossDamage, (int) bossMaxHealth));
                    }
                } catch (Exception e) {
                }
            }
        }
        new Thread(this, "Doanh Trại: " + this.clan.name).start();
    }

    public void update() {
        if (isOpened) {
            if ((!isTimePicking && Util.canDoWithTime(lastTimeOpen, TIME_DOANH_TRAI)) || (isTimePicking && Util.canDoWithTime(lastTimePick, TIME_PICK_DOANH_TRAI))) {
                finish();
                dispose();
            }

            boolean allCharactersDead = true;
            for (Zone zone : zones) {
                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }

                if (allCharactersDead) {
                    for (Player boss : zone.getBosses()) {
                        if (!boss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }
            if (allCharactersDead && !winDT) {
                winDT = true;
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Mau đi tìm Độc Nhãn");
                    }
                }
            }

        }
    }

    public ItemMap NR(Zone zone) {
        int x = Util.nextInt(100, zone.map.mapWidth - 100);
        int y = zone.map.yPhysicInTop(x, 100);
        int nr = Util.nextInt(18, 20); // chỉ ra ngọc rồng 5-7 sao (id 18=5s, 19=6s, 20=7s)
        ItemMap it = new ItemMap(zone, nr, 1, x, y, -1);
        return it;
    }

    public void randomNR() {
        for (Zone zone : zones) {
            Service.gI().dropItemMap(zone, NR(zone));
            Service.gI().dropItemMap(zone, NR(zone));
            Service.gI().dropItemMap(zone, NR(zone));
            if (Util.isTrue(1, 2)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 3)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 4)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
            if (Util.isTrue(1, 5)) {
                Service.gI().dropItemMap(zone, NR(zone));
            }
        }
    }

    public void finish() {
        // Snapshot per zone before iterating. zone.players is CopyOnWriteArrayList
        // (iterator is snapshot-safe) but index-based access via size()/get()
        // still has TOCTOU window if a player leaves mid-loop. Snapshot fixes
        // that and lets us drop the racy `i < size()` re-check.
        for (Zone zone : zones) {
            if (zone == null) continue;
            for (Player pl : new ArrayList<>(zone.getPlayers())) {
                if (pl == null || pl.beforeDispose) continue;
                kickOutOfDT(pl);
            }
        }
    }

    private void kickOutOfDT(Player player) {
        // Guard player.zone.map — player may be mid-changeMap (zone briefly null)
        // or already disposed when finish() iterates over a stale snapshot.
        if (player == null || player.beforeDispose
                || player.zone == null || player.zone.map == null) {
            return;
        }
        if (MapService.gI().isMapDoanhTrai(player.zone.map.mapId)) {
            Service.gI().sendThongBao(player, "Đã hết thời gian, bạn sẽ được đưa về nhà");
            ChangeMapService.gI().changeMapBySpaceShip(player, 21 + player.gender, -1, -1);
        }
    }

    private void sendTextDoanhTrai() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextDoanhTrai(pl);
        }
    }

    public void sendTextTimePickDoanhTrai() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextTimePickDoanhTrai(pl);
        }
    }

    private void removeTextDoanhTrai() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextDoanhTrai(pl);
        }
    }

    public void updateHPDame() {
        long totalDame = 0;
        long totalHp = 0;
        for (Player pl : this.clan.membersInGame) {
            if (pl != null && pl.nPoint != null) {
                totalDame += pl.nPoint.dame;
                totalHp += pl.nPoint.hpMax;
            }
        }
        for (Zone zone : this.zones) {
            for (Mob mob : zone.mobs) {
                if (mob.isDie()) {
                    continue;
                }
                mob.point.dame = (int) (totalHp / mob.tempId < 2_000_000_000 ? totalHp / mob.tempId : 2_000_000_000);
                mob.point.maxHp = (int) (totalDame / 3 * mob.tempId < 2_000_000_000 ? totalDame * mob.tempId : 2_000_000_000);
                mob.point.hp = mob.point.maxHp;
                mob.setTiemNang();
            }
        }

        long dame = totalHp / 20;
        long hp = totalDame * 50;
        for (Boss boss : bosses) {
            if (boss.isDie()) {
                continue;
            }
            if (boss.zone.map.mapId == 59) {
                try {
                    long bossDamage = (dame);
                    long bossMaxHealth = (hp);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 62) {
                try {
                    long bossDamage = Math.min(dame * 11L / 10, 200000000L);
                    long bossMaxHealth = Math.min(hp * 11L / 10, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 55) {
                try {
                    long bossDamage = Math.min(dame * 115L / 100, 200000000L);
                    long bossMaxHealth = Math.min(hp * 115L / 100, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
            if (boss.zone.map.mapId == 54) {
                try {
                    long bossDamage = Math.min(dame * 12L / 10, 200000000L);
                    long bossMaxHealth = Math.min(hp * 12L / 10, 2000000000L);
                    if (boss.id >= -14 && boss.id <= -9) {
                        bossDamage /= 10;
                        bossMaxHealth /= 10;
                    }
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }

            if (boss.zone.map.mapId == 57) {
                try {
                    long bossDamage = Math.min(dame * 13L / 10, 200000000L);
                    long bossMaxHealth = Math.min(hp * 13L / 10, 2000000000L);
                    boss.nPoint.hpMax = (int) bossMaxHealth;
                    boss.nPoint.dame = (int) bossDamage;
                    boss.nPoint.hp = boss.nPoint.hpMax;
                } catch (Exception exception) {
                }
            }
        }

    }

    public void dispose() {
        this.removeTextDoanhTrai();
        for (Boss boss : bosses) {
            if (!boss.isDie()) {
                boss.leaveMap();
            }
        }
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.bosses.clear();
        this.winDT = false;
        this.isOpened = false;
        this.clan.haveGoneDoanhTrai = true;
        this.clan.updateDoanhTrai();
        this.clan.doanhTrai = null;
        this.clan = null;
        this.isTimePicking = false;
    }
}
