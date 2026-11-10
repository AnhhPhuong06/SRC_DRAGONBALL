package models.boss.boss_list.NewBoss;

import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import services.Service;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Xibachao extends Boss {

    private long lastTimeJoinMap;

    public Xibachao() throws Exception {
        super(consts.BossID.XINBATO, false, true, BossesData.XINBATO_NEW);
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
}
