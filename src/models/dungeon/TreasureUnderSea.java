package models.dungeon;

/*
 * @Author: 0789305260
 *
 */
import utils.Functions;
import models.boss.Boss;
import models.boss.boss_list.TreasureUnderSea.TrungUyXanhLo;
import models.clan.Clan;
import models.map.TrapMap;
import models.map.Zone;
import models.mob.Mob;
import models.player.Player;
import services.ItemTimeService;
import services.map.MapService;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import server.Maintenance;
import services.map.ItemMapService;
import utils.TimeUtil;

@Data
public class TreasureUnderSea implements Runnable {

    public static final long POWER_CAN_GO_TO_DBKB = 2_000_000_000;
    public static final int AVAILABLE = 50;
    public static final int TIME_BAN_DO_KHO_BAU = 1800000;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private boolean kickoutbdkb;
    private long timeKickOutBDKB;
    private Boss boss;
    private long lastTimeSendNotify;
    private boolean allCharactersDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public TreasureUnderSea(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
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

    public void update() {
        if (isOpened) {
            if (Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU)
                    || (kickoutbdkb && Util.canDoWithTime(timeKickOutBDKB, 60000))) {
                finish();
                dispose();
            }

            allCharactersDead = true;
            for (Zone zone : zones) {

                if (zone.map.mapId == 135) {
                    for (Player pl : zone.getNotBosses()) {
                        if (pl != null) {
                            TrapMap trap = zone.isInTrap(pl);
                            if (trap != null) {
                                trap.doPlayer(pl);
                            }
                        }
                    }
                }

                for (Mob mob : zone.mobs) {
                    if (!mob.isDie()) {
                        allCharactersDead = false;
                        break;
                    }
                }

                if (allCharactersDead) {
                    for (Player cBoss : zone.getBosses()) {
                        if (!cBoss.isDie()) {
                            allCharactersDead = false;
                            break;
                        }
                    }
                }
            }
            if (!kickoutbdkb && (allCharactersDead || Util.canDoWithTime(lastTimeOpen, TIME_BAN_DO_KHO_BAU - 60000))) {
                kickoutbdkb = true;
                timeKickOutBDKB = System.currentTimeMillis();
            }

            if (kickoutbdkb && Util.canDoWithTime(lastTimeSendNotify, 10000)) {
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        sendThanhTichBanDoKhoBau(pl);
                        Service.gI().sendThongBao(pl, "Cái hang này sắp sập rồi, chúng ta phải rời khỏi đây ngay " + TimeUtil.getTimeLeft(timeKickOutBDKB, 60) + " nữa");
                    }
                    lastTimeSendNotify = System.currentTimeMillis();
                }
            }
        }
    }

    public void sendThanhTichBanDoKhoBau(Player pl) {
        long timeDoneBDKB;
        timeDoneBDKB = System.currentTimeMillis() - pl.clan.lastTimeOpenBanDoKhoBau;
        int levelDoneBDKB;
        levelDoneBDKB = pl.clan.BanDoKhoBau.level;
        if (levelDoneBDKB > pl.clan.levelDoneBanDoKhoBau) {
            pl.clan.levelDoneBanDoKhoBau = levelDoneBDKB;
            pl.clan.thoiGianHoanThanhBDKB = timeDoneBDKB;
        } else if (levelDoneBDKB == pl.clan.levelDoneBanDoKhoBau) {
            if (timeDoneBDKB < pl.clan.thoiGianHoanThanhBDKB) {
                pl.clan.thoiGianHoanThanhBDKB = timeDoneBDKB;
            }
        }
        pl.clan.updatethanhTich(pl.clan.id);
        pl.clan.updatethanhTichForLeader();
        pl.clan.updateThongTinLeader(pl.clan.id);
    }

    public void openBanDoKhoBau(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenBanDoKhoBau = this.lastTimeOpen;
            this.clan.playerOpenBanDoKhoBau = plOpen;
            this.clan.BanDoKhoBau = this;
            this.kickoutbdkb = false;
            this.isOpened = true;
            this.allCharactersDead = false;
            this.init();
            ChangeMapService.gI().goToDBKB(plOpen);
            sendTextBanDoKhoBau();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenBanDoKhoBau = 0;
            this.dispose();
        }
    }

    private void init() {
        for (Zone zone : this.zones) {
            for (TrapMap trap : zone.trapMaps) {
                trap.dame = this.level * 100000;
            }
            if (zone.map.mapId == 135 || zone.map.mapId == 136 || zone.map.mapId == 137) {
                List<Mob> mobs = zone.mobs;
                for (int i = 0; i < mobs.size(); i++) {
                    Mob mob = mobs.get(i);
                    if (((i == 5 || i == 10) && zone.map.mapId == 135) || (i == 5 && zone.map.mapId == 136) || (i == 5 && zone.map.mapId == 137)) {
                        mob.lvMob = 1;
                        mob.point.dame = (int) Math.min(level * 600L * mob.tempId * 10, 2_000_000_000);
                        mob.point.maxHp = 500_000_000; 
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    } else {
                        mob.lvMob = 0;
                        mob.point.dame = (int) Math.min(level * 200L * mob.tempId, 2_000_000_000);
                        mob.point.maxHp = 500_000_000; 
                        mob.hoiSinh();
                        mob.hoiSinhMobPhoBan();
                    }
                }
            } else {
                for (Mob mob : zone.mobs) {
                    mob.point.dame = (int) Math.min(level * 31L * 50 * mob.tempId, 2_000_000_000);
                    mob.point.maxHp = 500_000_000; 
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }

            if (zone.map.mapId == 137) {
                try {
                    long bossDamage = (200000 * level);
                    long bossMaxHealth = 2_000_000_000L;
                    bossDamage = Math.min(bossDamage, 200000000L);
                    boss = new TrungUyXanhLo(zone, level, (int) bossDamage, (int) bossMaxHealth);
                } catch (Exception exception) {
                }
            }
        }
        new Thread(this, "Bản Đồ Kho Báu: " + this.clan.name).start();
    }

    public void finish() {
        try {
            if (this.clan != null) {
                long timeDoneBDKB = System.currentTimeMillis() - this.clan.lastTimeOpenBanDoKhoBau;
                int levelDoneBDKB = this.level;
                if (levelDoneBDKB > this.clan.levelDoneBanDoKhoBau) {
                    this.clan.levelDoneBanDoKhoBau = levelDoneBDKB;
                    this.clan.thoiGianHoanThanhBDKB = timeDoneBDKB;
                } else if (levelDoneBDKB == this.clan.levelDoneBanDoKhoBau) {
                    if (timeDoneBDKB < this.clan.thoiGianHoanThanhBDKB || this.clan.thoiGianHoanThanhBDKB <= 0) {
                        this.clan.thoiGianHoanThanhBDKB = timeDoneBDKB;
                    }
                }
                this.clan.updatethanhTich(this.clan.id);
                this.clan.updatethanhTichForLeader();
                this.clan.updateThongTinLeader(this.clan.id);
            }
        } catch (Exception ignore) {
        }
        for (Zone zone : zones) {
            if (zone == null) continue;
            // Snapshot iteration: zone.players is COW (iterator snapshot-safe)
            // but index-based size()/get() still races when a player leaves
            // mid-loop.
            for (Player pl : new ArrayList<>(zone.getPlayers())) {
                if (pl == null || pl.beforeDispose) continue;
                kickOutOfBDKB(pl);
            }

        }
    }

    private void kickOutOfBDKB(Player player) {
        if (player == null || player.beforeDispose
                || player.zone == null || player.zone.map == null) {
            return;
        }
        if (MapService.gI().isMapBanDoKhoBau(player.zone.map.mapId)) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 5, -1, 1038);
        }
    }

    public Zone getMapById(int mapId) {
        for (Zone zone : this.zones) {
            if (zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void sendTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextBanDoKhoBau(pl);
        }
    }

    private void removeTextBanDoKhoBau() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextBanDoKhoBau(pl);
        }
    }

    public void dispose() {
        if (boss != null) {
            this.boss.leaveMap();
        }
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        this.removeTextBanDoKhoBau();
        this.allCharactersDead = false;
        this.boss = null;
        this.isOpened = false;
        this.clan.updateBanDoKhoBau();
        this.clan.BanDoKhoBau = null;
        this.clan = null;
        this.kickoutbdkb = false;
    }
}
