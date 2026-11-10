package models.boss.boss_list.NewBoss;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import consts.BossID;
import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import services.Service;
import services.map.MapService;
import services.player.PlayerService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class BaDo extends Boss {

    public BaDo() throws Exception {
        super(consts.BossID.O_DO, false, true, BossesData.O_DO_NEW);
    }

    private static final String[] textOdo = new String[]{
        "Hôi quá, tránh xa ta ra", "Biến đi", "Trời ơi đồ ở dơ",
        "Thúi quá", "Mùi gì hôi quá"
    };
    private long lastTimeOdo;
    private long lastTimeJoinMap;

    public void subHpWithOdo() {
        try {
            if (this.nPoint != null) {
                if (Util.canDoWithTime(lastTimeOdo, 15000)) {
                    for (int i = this.zone.getNotBosses().size() - 1; i >= 0; i--) {
                        Player pl = this.zone.getNotBosses().get(i);
                        if (pl != null && pl.nPoint != null && !pl.isDie()) {
                            long subHp = (long) ((long) pl.nPoint.hpMax * 20 / 100);
                            if (subHp >= pl.nPoint.hp) {
                                subHp = pl.nPoint.hp - 1;
                            }
                            Service.gI().chat(pl, textOdo[Util.nextInt(0, textOdo.length - 1)]);
                            PlayerService.gI().sendInfoHpMpMoney(pl);
                            Service.gI().Send_Info_NV(pl);
                            pl.injured(null, subHp, true, false);
                        }
                    }
                    this.lastTimeOdo = System.currentTimeMillis();
                }
            }
        } catch (Exception e) {
        }
    }

    @Override
    public void update() {
        super.update();
        subHpWithOdo();
    }

    // Danh sách map ngẫu nhiên cho NewBoss (0..20 trừ map 5 Đảo Kame)
    public static final int[] MAP_NEW_BOSS = {
        0, 1, 2, 3, 4, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20
    };

    // Lưu mapId hiện tại của từng boss để đảm bảo không boss nào vào trùng map
    public static final Map<Integer, Integer> currentBossMap = new ConcurrentHashMap<>();

    public static synchronized Zone getRandomZoneForNewBoss(Boss boss) {
        Set<Integer> occupiedMapIds = new HashSet<>();
        for (Map.Entry<Integer, Integer> entry : currentBossMap.entrySet()) {
            if (boss == null || entry.getKey() != (int) boss.id) {
                occupiedMapIds.add(entry.getValue());
            }
        }
        int[] otherBossIds = {BossID.O_DO, BossID.XINBATO, BossID.SOI_HEC_QUYN};
        for (int id : otherBossIds) {
            if (boss != null && (int) boss.id == id) {
                continue;
            }
            Boss b = managers.boss.BossManager.gI().getBossById(id);
            if (b != null && b.zone != null && b.zone.map != null && !b.isDie()) {
                occupiedMapIds.add(b.zone.map.mapId);
            }
        }

        List<Integer> availableMaps = new ArrayList<>();
        for (int m : MAP_NEW_BOSS) {
            if (!occupiedMapIds.contains(m)) {
                availableMaps.add(m);
            }
        }
        if (availableMaps.isEmpty()) {
            for (int m : MAP_NEW_BOSS) {
                availableMaps.add(m);
            }
        }

        Collections.shuffle(availableMaps);
        for (int mapId : availableMaps) {
            Zone z = MapService.gI().getMapWithRandZone(mapId);
            if (z != null && z.map.mapId != 5) {
                if (z.map.zones.size() > 1 && z.zoneId == 0) {
                    z = z.map.zones.get(Util.nextInt(1, z.map.zones.size() - 1));
                }
                if (boss != null) {
                    currentBossMap.put((int) boss.id, z.map.mapId);
                }
                return z;
            }
        }
        Zone fallback = Util.randomAllMap();
        if (fallback != null && boss != null) {
            currentBossMap.put((int) boss.id, fallback.map.mapId);
        }
        return fallback;
    }

    @Override
    public void joinMap() {
        Zone z = getRandomZoneForNewBoss(this);
        if (z != null) {
            this.zoneFinal = z;
            joinMapByZone(z);
            notifyJoinMap();
            changeStatus(BossStatus.CHAT_S);
            lastTimeJoinMap = System.currentTimeMillis();
        } else {
            changeStatus(BossStatus.RESPAWN);
        }
    }

    @Override
    public void leaveMap() {
        super.leaveMap();
        currentBossMap.remove((int) this.id);
        this.zone = null;
        this.zoneFinal = null;
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(lastTimeJoinMap, 900000)) {
            this.leaveMap();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            lastTimeJoinMap = System.currentTimeMillis();
        }
    }

    @Override
    public void reward(Player plKill) {
        dropRewardNewBoss(this, plKill);
    }

    // Quản lý rơi vật phẩm cho bộ 3: Ở Dơ (Ba Đô), Xinbatô (Xibachao), Sói Hẹc Quyn (Chó Rách / Trộm)
    // Sau thay đổi: không còn rơi vật phẩm, chỉ giữ logic nhiệm vụ (Ở Dơ)

    public static synchronized void dropRewardNewBoss(Boss boss, Player plKill) {
        if (boss.zone == null || boss.zone.map == null) {
            return;
        }

        if (boss.id == BossID.O_DO && plKill != null && plKill.playerTask != null && plKill.playerTask.taskdh != null) {
            if (plKill.playerTask.taskdh.ODo < 30) {
                int required = 30;
                int percentDone = (int) ((double) plKill.playerTask.taskdh.ODo / required * 100);
                plKill.playerTask.taskdh.ODo++;
                plKill.playerTask.taskdh.ResetTime = System.currentTimeMillis();
                Service.gI().sendThongBao(plKill, "Tiến độ hiện tại: " + percentDone + "%");
            }
        }

        services.TaskService.gI().checkDoneTaskKillBoss(plKill, boss);

    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = 1;
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            if (this.bossStatus != BossStatus.DIE && this.bossStatus != BossStatus.CHAT_E
                    && this.bossStatus != BossStatus.LEAVE_MAP && this.bossStatus != BossStatus.REST
                    && this.bossStatus != BossStatus.RESPAWN) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return 0;
        }
    }
}
