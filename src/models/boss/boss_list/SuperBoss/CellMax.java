package models.boss.boss_list.SuperBoss;

import consts.BossID;
import models.boss.Boss;
import models.boss.BossesData;
import models.player.Player;
import services.EffectSkillService;
import database.daos.PlayerDAO;
import models.item.Item;
import models.map.ItemMap;
import models.npc.npc_list.XenShop;
import services.ItemService;
import services.Service;
import utils.Util;

public class CellMax extends Boss {

    private long st;
    private int timeLeaveMap;

    public CellMax() throws Exception {
        super(BossID.CELL_MAX, false, true, BossesData.CELL_MAX);
    }

    @Override
    public void joinMap() {
        this.name = this.data[this.currentLevel].getName() + " " + Util.nextInt(1, 100);
        super.joinMap();
        st = System.currentTimeMillis();
        timeLeaveMap = Util.nextInt(600000, 900000);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage);
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
    public void reward(Player plKill) {
        if (plKill == null || this.zone == null) {
            return;
        }
        PlayerDAO.addBossPoint(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận được 1 điểm săn boss");
        XenShop.themDiem(plKill, 1);
        Service.gI().sendThongBao(plKill, "Bạn nhận +1 điểm sự kiện Xên! Tới NPC đảo Kame để đổi quà.");
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, timeLeaveMap)) {
            this.leaveMap();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
            timeLeaveMap = Util.nextInt(300000, 900000);
        }
    }
}
