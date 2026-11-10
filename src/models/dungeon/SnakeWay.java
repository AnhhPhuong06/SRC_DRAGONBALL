package models.dungeon;

import consts.BossStatus;
import utils.Functions;
import models.boss.boss_list.SnakeWay.SAIBAMEN;
import models.boss.boss_list.SnakeWay.NADIC;
import models.boss.boss_list.SnakeWay.CADICH;
import models.boss.Boss;
import models.clan.Clan;
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
public class SnakeWay implements Runnable {

    public static final long POWER_CAN_GO_TO_CDRD = 2000000000;
    public static final int AVAILABLE = 50;
    public static final int TIME_CON_DUONG_RAN_DOC = 1800000;

    public int id;
    public byte level;
    public final List<Zone> zones;

    public Clan clan;
    public boolean isOpened;
    private long lastTimeOpen;
    private long lastTimeUpdateMessage;
    private boolean kickoutcdrd;
    private long timeKickOutCDRD;
    public List<Boss> bosses = new ArrayList<>();
    public boolean endCDRD;
    public boolean allMobsDead;

    public void addZone(Zone zone) {
        this.zones.add(zone);
    }

    public SnakeWay(int id) {
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
            if (Util.canDoWithTime(lastTimeOpen, TIME_CON_DUONG_RAN_DOC)
                    || (kickoutcdrd && Util.canDoWithTime(timeKickOutCDRD, 60000))) {
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
            if (allCharactersDead) {
                allMobsDead = true;
            }

            if (!kickoutcdrd && (endCDRD || Util.canDoWithTime(lastTimeOpen, TIME_CON_DUONG_RAN_DOC - 60000))) {
                kickoutcdrd = true;
                timeKickOutCDRD = System.currentTimeMillis();
            }
            if (kickoutcdrd && Util.canDoWithTime(lastTimeUpdateMessage, 10000)) {
                lastTimeUpdateMessage = System.currentTimeMillis();
                for (Zone zone : zones) {
                    List<Player> players = zone.getPlayers();
                    for (Player pl : players) {
                        Service.gI().sendThongBao(pl, "Trận chiến với người Xayda sẽ kết thúc sau "
                                + TimeUtil.getTimeLeft(timeKickOutCDRD, 60) + " nữa");
                    }

                }
            }

        }
    }

    public void openConDuongRanDoc(Player plOpen, Clan clan, byte level) {
        try {
            this.level = level;
            this.lastTimeOpen = System.currentTimeMillis();
            this.clan = clan;
            this.clan.lastTimeOpenConDuongRanDoc = this.lastTimeOpen;
            this.clan.playerOpenConDuongRanDoc = plOpen;
            this.clan.ConDuongRanDoc = this;
            this.isOpened = true;
            this.init();
            sendTextConDuongRanDoc();
        } catch (Exception e) {
            plOpen.clan.lastTimeOpenConDuongRanDoc = 0;
            this.dispose();
        }
    }

    private void init() {
        for (Zone zone : this.zones) {
            List<Mob> mobs = zone.mobs;
            for (int i = 0; i < mobs.size(); i++) {
                Mob mob = mobs.get(i);
                if (i == 5) {
                    mob.lvMob = 1;
                    mob.point.dame = (int) Math.min(level * 600L * mob.tempId * 10, 2_000_000_000L);
                    mob.point.maxHp = (int) Math.min(level * 4697L * mob.tempId * 10, 2_000_000_000L);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                } else {
                    mob.lvMob = 0;
                    mob.point.dame = (int) Math.min(level * 200L * mob.tempId, 2_000_000_000L);
                    mob.point.maxHp = (int) Math.min(level * 4697L * mob.tempId, 2_000_000_000L);
                    mob.hoiSinh();
                    mob.hoiSinhMobPhoBan();
                }
            }

            if (zone.map.mapId == 144) {
                try {
                    long bossDamage = Math.min(200000L * level, 200_000_000L);
                    final int BOSS_HP = 2_000_000_000; // 2 tỷ HP cứng
                    for (int i = 6; i > 0; i--) {
                        bosses.add(new SAIBAMEN(zone, clan, i, (int) bossDamage, BOSS_HP));
                    }
                    bossDamage = Math.min(bossDamage * 5, 200_000_000L);
                    bosses.add(new NADIC(zone, clan, (int) bossDamage, BOSS_HP));
                    bossDamage = Math.min(bossDamage * 10, 200_000_000L);
                    bosses.add(new CADICH(zone, clan, (int) bossDamage, BOSS_HP));
                } catch (Exception exception) {
                }
            }
        }
        new Thread(this, "Con Đường Rắn Độc: " + this.clan.name).start();
    }

    public void sendThanhTichConDuongRanDoc(Player pl) {
        long timeDoneCDRD;
        timeDoneCDRD = System.currentTimeMillis() - pl.clan.lastTimeOpenConDuongRanDoc;
        int levelDoneCDRD;
        levelDoneCDRD = pl.clan.ConDuongRanDoc.level;
        if (levelDoneCDRD > pl.clan.levelDoneConDuongRanDoc) {
            pl.clan.levelDoneConDuongRanDoc = levelDoneCDRD;
            pl.clan.thoiGianHoanThanhConDuongRanDoc = timeDoneCDRD;
        } else if (levelDoneCDRD == pl.clan.levelDoneConDuongRanDoc) {
            if (timeDoneCDRD < pl.clan.thoiGianHoanThanhConDuongRanDoc) {
                pl.clan.thoiGianHoanThanhConDuongRanDoc = timeDoneCDRD;
            }
        }
        pl.clan.updatethanhTich(pl.clan.id);
        pl.clan.updatethanhTichForLeader();
    }

    public void finish() {
        for (Zone zone : zones) {
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    kickOutOfCDRD(pl);
                }
            }

        }
    }

    private void kickOutOfCDRD(Player player) {
        if (MapService.gI().isMapConDuongRanDoc(player.zone.map.mapId)) {
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

    private void sendTextConDuongRanDoc() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().sendTextConDuongRanDoc(pl);
        }
    }

    private void removeTextConDuongRanDoc() {
        for (Player pl : this.clan.membersInGame) {
            ItemTimeService.gI().removeTextConDuongRanDoc(pl);
        }
    }

    public long getNumBossAlive() {
        return bosses.stream().filter(boss -> boss.bossStatus != BossStatus.REST).count();
    }

    public void dispose() {
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
        this.removeTextConDuongRanDoc();
        this.bosses.clear();
        this.allMobsDead = false;
        this.endCDRD = false;
        this.isOpened = false;
        this.clan.updateConDuongRanDoc();
        this.clan.ConDuongRanDoc = null;
        this.clan = null;
        this.kickoutcdrd = false;
    }
}
