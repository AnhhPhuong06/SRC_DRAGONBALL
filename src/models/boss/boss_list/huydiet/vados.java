package models.boss.boss_list.huydiet;

 


import database.daos.PlayerDAO;
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.mob.Mob;
import models.player.Player;
import server.Manager;
import services.ItemService;
import services.Service;
import services.TaskService;
import utils.TimeUtil;
import utils.Util;

public class vados extends Boss {

    public vados() throws Exception {
        super(BossID.VADOS, BossesData.VADOS);
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
    public void chatM() {
        if (Util.isTrue(60, 61)) {
            super.chatM();
            return;
        }
        if (this.bossAppearTogether == null || this.bossAppearTogether[this.currentLevel] == null) {
            return;
        }
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            if (boss.id == BossID.CHAMPA && !boss.isDie()) {
                this.chat("Hút năng lượng của nó, mau lên");
                boss.chat("Tuân lệnh đại ca, hê hê hê");
                break;
            }
        }
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

//    @Override
//    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
//        if (plAtt != null) {
//            switch (plAtt.playerSkill.skillSelect.template.id) {
//                case Skill.KAMEJOKO:
//                case Skill.MASENKO:
//                case Skill.ANTOMIC:
//                    PlayerService.gI().hoiPhuc(this, damage, 0);
//                    if (Util.isTrue(1, 5)) {
//                        this.chat("Hấp thụ.. các ngươi nghĩ sao vậy?");
//                    }
//                    return 0;
//            }
//        }
//        return super.injured(plAtt, damage, piercing, isMobAttack);
//    }

    @Override
    public void doneChatS() {
        for (Boss boss : this.bossAppearTogether[this.currentLevel]) {
            if (boss.id == BossID.CHAMPA) {
                boss.changeToTypePK();
                break;
            }
        }
    }

    @Override
    public void changeToTypePK() {
        super.changeToTypePK();
        this.chat("Mau đền mạng cho thằng em trai ta");
    }
}
