package models.boss.boss_list.Zeno;

import consts.BossID;
import database.daos.PlayerDAO;
import models.boss.Boss;
import models.boss.BossesData;
import models.map.ItemMap;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Util;

public class Zeno extends Boss {

    public Zeno() throws Exception {
        super(BossID.ZENO, BossesData.ZENO); 
    }
 
    @Override
    public void reward(Player plKill) {
        PlayerDAO.addBossPoint(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận được 1 điểm săn boss");
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
    }
}
