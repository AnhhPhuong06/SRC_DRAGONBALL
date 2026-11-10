package models.boss.boss_list.NewBoss;

import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossesData;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import services.Service;
import services.map.ChangeMapService;
import services.map.ItemMapService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class ChoRach extends Boss {

    private long lastTimeJoinMap;

    public ChoRach() throws Exception {
        super(consts.BossID.SOI_HEC_QUYN, false, true, BossesData.SOI_HEC_QUYN_NEW);
    }

    @Override
    public void joinMap() {
        Zone z = BaDo.getRandomZoneForNewBoss(this);
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
        BaDo.currentBossMap.remove((int) this.id);
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

    public void checkCucXuong() {
        if (this.zone != null) {
            if (this.zone.getItemMapByTempId(460) != null) {
                try {
                    ItemMap cucXuongNe = this.zone.getItemMapByTempId(460);
                    if (cucXuongNe != null) {
                        super.moveTo(cucXuongNe.x, cucXuongNe.y);
                        Service.gI().chat(this, "Xương gì ngon quá măm măm");
                        Thread.sleep(3000);
                        int[] itemne = {441, 442, 443, 444, 445, 446, 447};
                        Service.gI().chat(this, "Tặng ngươi món quà nè");
                        Thread.sleep(3000);
                        Service.gI().dropItemMap(this.zone, Util.saoPhaLe(zone, Util.isTrue(80, 100) ? itemne[Util.nextInt(itemne.length - 1)] : itemne[itemne.length - 1], 1, this.location.x, this.zone.map.yPhysicInTop(this.location.x, this.location.y), (int) cucXuongNe.playerId));
                        ItemMapService.gI().removeItemMapAndSendClient(cucXuongNe);
                        Service.gI().chat(this, "Úm ba la a la ba trap manbo hiếu thứ hai");
                        Thread.sleep(2000);
                        Zone newZone = BaDo.getRandomZoneForNewBoss(this);
                        if (newZone != null) {
                            this.zone = newZone;
                            this.zoneFinal = newZone;
                            ChangeMapService.gI().changeMapYardrat(this, newZone, newZone.map.mapWidth + Util.nextInt(-50, 50), newZone.map.yPhysicInTop(this.location.x, this.location.y));
                            notifyJoinMap();
                        }
                    }
                } catch (Exception e) {
                }
            }
        }
    }
    @Override
    public void reward(Player plKill) {
        BaDo.dropRewardNewBoss(this, plKill);
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

    @Override
    public void attack() {
        checkCucXuong();
        super.attack();
    }
}
