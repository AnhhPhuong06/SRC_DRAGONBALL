package models.boss.boss_list.huydiet;

 
import models.boss.boss_list.Android.*;
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import database.daos.PlayerDAO;
import java.util.Random;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.mob.Mob;
import models.player.Player;
import models.skill.Skill;
import server.Manager;
import services.ItemService;
import services.player.PlayerService;
import services.Service;
import services.TaskService;
import utils.TimeUtil;
import utils.Util;

public class berus extends Boss {

    public berus() throws Exception {
        super(BossID.BERUS, BossesData.BERUS);
    }

    @Override
    public void reward(Player plKill) {
        PlayerDAO.addBossPoint(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận được 1 điểm săn boss");
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        plKill.effect.addPointTrumSanBoss();
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public void autoLeaveMap() {
        if (!TimeUtil.is23H()) {
            this.leaveMap();
        }
    }

    @Override
    public void joinMap() {
        if (TimeUtil.is23H()) {
            this.name = this.data[this.currentLevel].getName() + " " + Util.nextInt(1, 100);
            super.joinMap();
            if (this.zone != null) {
                for (Mob mob : this.zone.mobs) {
                    mob.injured(this, 99999999, true);
                }
            }
        } else {
            this.changeStatus(BossStatus.REST);
        }
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
        if (this.parentBoss != null) {
            this.parentBoss.changeToTypePK();
        }
    }

}
