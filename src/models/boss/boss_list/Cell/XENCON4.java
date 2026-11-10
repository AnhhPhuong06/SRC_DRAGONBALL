package models.boss.boss_list.Cell;

 


import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.BossesData;
import models.map.ItemMap;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import utils.Util;

public class XENCON4 extends Boss {

    private long st;

    public XENCON4() throws Exception {
        super(BossID.XEN_CON_4, BossesData.XEN_CON_4);
    }

    @Override
     public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void joinMap() {
        st = System.currentTimeMillis();
        this.zone = this.parentBoss.zone;
        ChangeMapService.gI().changeMap(this, this.zone,
                this.parentBoss.location.x + Util.nextInt(-100, 100), this.parentBoss.location.y);
        Service.gI().sendFlagBag(this);
        this.notifyJoinMap();
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null || this.parentBoss.bossAppearTogether == null
                || this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.parentBoss.bossAppearTogether[this.parentBoss.currentLevel]) {
            if ((boss.id == BossID.XEN_CON_2 || boss.id == BossID.XEN_CON_3 || boss.id == BossID.XEN_CON_1 || boss.id == BossID.XEN_CON_5 || boss.id == BossID.XEN_CON_6 || boss.id == BossID.XEN_CON_7) && !boss.isDie()) {
                return;
            }
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
//        BossManager.gI().removeBoss(this);
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
}
