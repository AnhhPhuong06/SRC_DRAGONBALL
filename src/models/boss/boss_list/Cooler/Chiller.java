package models.boss.boss_list.Cooler;

 
import consts.BossID;
import database.daos.PlayerDAO;
import java.util.Random;
import models.boss.Boss;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.npc.npc_list.XenShop;
import models.player.Player;
import server.Manager;
import services.EffectSkillService;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.Util;

public class Chiller extends Boss {

    private long st;

    public Chiller() throws Exception {
        super(BossID.CHILLER, BossesData.CHILLER, BossesData.CHILLER_2);
    }

    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        XenShop.themDiem(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận +1 điểm sự kiện Xên! Tới NPC đảo Kame để đổi quà.");
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        PlayerDAO.addBossPoint(plKill, 1);
        // cộng 5 điểm săn boss cho người tiêu diệt boss
        Service.gI().sendThongBao(plKill, "Bạn nhận thêm được 1 điểm săn Boss");
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage *= 0.5;
            if (this.currentLevel != 0) {
                damage /= 2;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage - Util.nextInt(50000));
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return damage;
        } else {
            return 0;
        }
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

}
