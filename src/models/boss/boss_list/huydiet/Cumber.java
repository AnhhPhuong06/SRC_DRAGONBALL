package models.boss.boss_list.huydiet;

import models.boss.Boss;
import consts.BossID;
import database.daos.PlayerDAO;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.npc.npc_list.XenShop;
import models.player.Player;
import services.EffectSkillService;
import services.Service;
import utils.Util;

import java.util.Random;
import server.Manager;
import services.ItemService;
import services.RewardService;
import services.TaskService;

public class Cumber extends Boss {

    private long st;

    public Cumber() throws Exception {
        super(BossID.CUMBER, BossesData.CUMBER, BossesData.CUMBER_2);
    }

    @Override
    public void reward(Player plKill) {
        PlayerDAO.addBossPoint(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận được 1 điểm săn boss");
        XenShop.themDiem(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận +1 điểm sự kiện Xên! Tới NPC đảo Kame để đổi quà.");
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        plKill.effect.addPointTrumSanBoss();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage *= 0.5;
            if (piercing) {
                damage /= 100;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (effectSkill.isShielding) {
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
