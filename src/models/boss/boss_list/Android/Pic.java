package models.boss.boss_list.Android;

 


import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Util;

public class Pic extends Boss {

    public Pic() throws Exception {
        super(BossID.PIC, BossesData.PIC);
    }

    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
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
    public void joinMap() {
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
        st = System.currentTimeMillis();
    }
    private long st;

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
    }

    @Override
    public void doneChatE() {
        if (this.parentBoss == null) {
            return;
        }
        this.parentBoss.changeStatus(BossStatus.ACTIVE);
    }

}
