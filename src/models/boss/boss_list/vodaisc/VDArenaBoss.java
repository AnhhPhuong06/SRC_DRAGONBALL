package models.boss.boss_list.vodaisc;

import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossData;
import models.player.Player;
import utils.Util;

public abstract class VDArenaBoss extends Boss {

    private long stLeave;

    public VDArenaBoss(int id, BossData... data) throws Exception {
        super(id, data);
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void doneChatE() {
        this.activateNext();
    }

    protected void activateNext() {
        Boss leader = this.parentBoss != null ? this.parentBoss : this;
        if (leader.bossAppearTogether == null
                || leader.currentLevel < 0 || leader.currentLevel >= leader.bossAppearTogether.length
                || leader.bossAppearTogether[leader.currentLevel] == null) {
            return;
        }
        Boss[] group = leader.bossAppearTogether[leader.currentLevel];
        int from = (this == leader) ? 0 : this.lv + 1;
        for (int i = from; i < group.length; i++) {
            if (group[i] != null && !group[i].isDie()) {
                group[i].changeStatus(BossStatus.ACTIVE);
                return;
            }
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (this.bossStatus == BossStatus.AFK) {
            return 0;
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        this.stLeave = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            this.stLeave = System.currentTimeMillis();
        } else if (Util.canDoWithTime(this.stLeave, 900000)) {
            this.leaveMapNew();
        }
    }
}
