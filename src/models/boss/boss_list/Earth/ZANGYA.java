package models.boss.boss_list.Earth;

 
import consts.BossID;
import consts.BossStatus;
import models.boss.Boss;
import models.boss.BossesData;
import models.player.Player;
import utils.Util;

public class ZANGYA extends Boss {

    private long st;

    public ZANGYA() throws Exception {
        super(BossID.ZANGYA, false, true, BossesData.ZANGYA);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

    @Override
    public void reward(Player plKill) {}

    @Override
    public void rest() {
        super.rest();
    }

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null
                || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.currentLevel < 0
                || this.parentBoss.currentLevel >= this.parentBoss.bossAppearTogether.length
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.BUJIN || boss.id == BossID.KOGU || boss.id == BossID.BIDO) && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
