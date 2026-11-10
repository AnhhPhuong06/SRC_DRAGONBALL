package models.dungeon;

/*
 * @Author: 0789305260
 *
 */
import utils.Functions;
import models.boss.Boss;
import models.boss.boss_list.DestronGas.DrLychee;
import models.clan.Clan;
import models.map.Zone;
import models.mob.Mob;
import models.player.Player;
import services.ItemTimeService;
import services.map.MapService;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;
import server.Manager;
import models.Template;

import java.util.ArrayList;
import java.util.List;
import lombok.Data;
import server.Maintenance;
import services.map.ItemMapService;
import utils.TimeUtil;

@Data
public class DestronGas implements Runnable {

    public static final long POWER_CAN_GO_TO_KHI_GAS_HUY_DIET = 2000000000;
    /** Mỗi bang có phòng riêng, tối đa 50 phòng đồng thời. */
    public static final int AVAILABLE = 50;
    public static final int TIME_KHI_GAS_HUY_DIET = 1800000;
    public static final int N_PLAYER_CLAN = 0;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private long lastTimeUpdateMessage;
    private boolean kickoutkghd;
    private long timeKickOutKGHD;
    public List<Boss> bosses = new ArrayList<>();
    private boolean callBoss;
    public boolean hatchiyatchDead;

    public DestronGas(int id) {
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
            if (Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET) || (kickoutkghd && Util.canDoWithTime(timeKickOutKGHD, 60000))) {
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
            }

            if (allCharactersDead && !callBoss) {
                try {
                    long bossDamage = (1000 * level);
                    long bossMaxHealth = (15000000 * level);
                    bossDamage = Math.min(bossDamage, 200000000L);
                    bossMaxHealth = Math.min(bossMaxHealth, 2000000000L);
                    bosses.add(new DrLychee(getMapById(148), clan, level, (int) bossDamage, (int) bossMaxHealth));
                    callBoss = true;
                } catch (Exception exception) {
                }
            }

            if (!kickoutkghd && (hatchiyatchDead || Util.canDoWithTime(lastTimeOpen, TIME_KHI_GAS_HUY_DIET - 60000))) {
                kickoutkghd = true;
                timeKickOutKGHD = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Nơi này sắp nổ tung mau chạy đi");
                    }
                }
            }
            if (kickoutkghd && Util.canDoWithTime(lastTimeUpdateMessage, 10000)) {
                lastTimeUpdateMessage = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Về làng Aru sau " + TimeUtil.getTimeLeft(timeKickOutKGHD, 60) + " nữa");
                    }
                }
            }

        }
    }

    public void openKhiGasHuyDiet(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenKhiGasHuyDiet = this.lastTimeOpen;
            this.clan.playerOpenKhiGasHuyDiet = plOpen;
            this.clan.KhiGasHuyDiet = this;
            this.callBoss = false;
            this.isOpened = true;
            this.init();
            sendTextKhiGasHuyDiet();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenKhiGasHuyDiet = 0;
            this.dispose();
        }
    }

    public void sendThanhTichKhiGaHuyDiet(Player pl) {
        long timeDoneKGHD;
        timeDoneKGHD = System.currentTimeMillis() - pl.clan.lastTimeOpenBanDoKhoBau;
        int levelDoneKGHD;
        levelDoneKGHD = pl.clan.KhiGasHuyDiet.level;
        if (levelDoneKGHD > pl.clan.levelDoneKhiGaHuyDiet) {
            pl.clan.levelDoneKhiGaHuyDiet = levelDoneKGHD;
            pl.clan.thoiGianHoanThanhKhiGaHuyDiet = levelDoneKGHD;
        } else if (levelDoneKGHD == pl.clan.levelDoneKhiGaHuyDiet) {
            if (timeDoneKGHD < pl.clan.thoiGianHoanThanhKhiGaHuyDiet) {
                pl.clan.thoiGianHoanThanhKhiGaHuyDiet = timeDoneKGHD;
            }
        }
        pl.clan.updatethanhTich(pl.clan.id);
        pl.clan.updatethanhTichForLeader();
    }

    private void init() {
        for (Zone zone : this.zones) {
            List<Mob> mobs = zone.mobs;
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                if (((i == 0) && zone.map.mapId == 147)
                        || ((i == 7) && zone.map.mapId == 149)
                        || ((i == 0) && zone.map.mapId == 151)
                        || ((i == 0) && zone.map.mapId == 152)
                        || ((i == 33) && zone.map.mapId == 152)) {
                    mob.lvMob = 1;
                    mob.point.dame = (int) Math.min(level * 600L * mob.tempId * 10, 2_000_000_000L);
                    mob.point.maxHp = (int) Math.min(level * 4697L * mob.tempId * 10, 2_000_000_000L);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                } else {
                    mob.lvMob = mob.tempId == 76 ? 1 : 0;
                    mob.point.dame = (int) Math.min(level * 200L * mob.tempId, 2_000_000_000L);
                    mob.point.maxHp = (int) Math.min(level * 4697L * mob.tempId, 2_000_000_000L);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }
        }
        new Thread(this, "Khí Gas Hủy Diệt: " + this.clan.name).start();
    }

    public void finish() {
        try {
            if (this.clan != null) {
                long timeDoneKGHD = System.currentTimeMillis() - this.clan.lastTimeOpenKhiGasHuyDiet;
                int levelDoneKGHD = this.level;
                if (levelDoneKGHD > this.clan.levelDoneKhiGaHuyDiet) {
                    this.clan.levelDoneKhiGaHuyDiet = levelDoneKGHD;
                    this.clan.thoiGianHoanThanhKhiGaHuyDiet = timeDoneKGHD;
                } else if (levelDoneKGHD == this.clan.levelDoneKhiGaHuyDiet) {
                    if (timeDoneKGHD < this.clan.thoiGianHoanThanhKhiGaHuyDiet || this.clan.thoiGianHoanThanhKhiGaHuyDiet <= 0) {
                        this.clan.thoiGianHoanThanhKhiGaHuyDiet = timeDoneKGHD;
                    }
                }
                this.clan.updatethanhTich(this.clan.id);
                this.clan.updatethanhTichForLeader();
            }
        } catch (Exception ignore) {
        }
        for (Zone zone : zones) {
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    kickOutOfKGHD(pl);
                }
            }

        }
    }

    private void kickOutOfKGHD(Player player) {
        if (MapService.gI().isMapKhiGasHuyDiet(player.zone.map.mapId)) {
            ChangeMapService.gI().changeMapBySpaceShip(player, 0, -1, -1);
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

    private void sendTextKhiGasHuyDiet() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextKhiGasHuyDiet(pl);
        }
    }

    private void removeTextKhiGasHuyDiet() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextKhiGasHuyDiet(pl);
            sendThanhTichKhiGaHuyDiet(pl);
        }
    }

    public void dispose() {
        for (Zone zone : zones) {
            for (int i = zone.items.size() - 1; i >= 0; i--) {
                if (i < zone.items.size()) {
                    ItemMapService.gI().removeItemMap(zone.items.get(i));
                }
            }
        }
        for (Boss boss : bosses) {
            if (!boss.isDie()) {
                boss.leaveMap();
            }
        }
        this.removeTextKhiGasHuyDiet();
        this.bosses.clear();
        this.isOpened = false;
        this.clan.updateKhiGasHuyDiet();
        this.clan.KhiGasHuyDiet = null;
        this.clan = null;
        this.kickoutkghd = false;
        this.hatchiyatchDead = false;
    }
}
