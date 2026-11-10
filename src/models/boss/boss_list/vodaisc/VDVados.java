package models.boss.boss_list.vodaisc;

import consts.BossID;
import database.daos.PlayerDAO;
import models.boss.BossesData;
import models.map.ItemMap;
import models.npc.npc_list.XenShop;
import models.player.Player;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Util;

public class VDVados extends VDArenaBoss {

    public VDVados() throws Exception {
        super(BossID.VD_VADOS, BossesData.VD_VADOS);
    }

    @Override
    public void reward(Player plKill) {
        PlayerDAO.addBossPoint(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận được 1 điểm săn boss");
        XenShop.themDiem(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận +1 điểm sự kiện Xên! Tới NPC đảo Kame để đổi quà.");
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
    }
}
