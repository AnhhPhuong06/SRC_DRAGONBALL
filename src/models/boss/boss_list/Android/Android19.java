package models.boss.boss_list.Android;

 


import models.boss.Boss;
import consts.BossID;
import java.util.Random;
import models.boss.BossesData;
import models.map.ItemMap;
import models.player.Player;
import models.skill.Skill;
import services.player.PlayerService;
import services.Service;
import services.TaskService;
import utils.Util;

public class Android19 extends Boss {

    public Android19() throws Exception {
        super(BossID.ANDROID_19, BossesData.ANDROID_19);
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        plKill.effect.addPointTrumSanBoss();
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
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (plAtt != null) {
            switch (plAtt.playerSkill.skillSelect.template.id) {
                case Skill.KAMEJOKO:
                case Skill.MASENKO:
                case Skill.ANTOMIC:
                    int hpHoi = (int) ((long) damage * 80 / 100);
                    PlayerService.gI().hoiPhuc(this, hpHoi, 0);
                    if (Util.isTrue(1, 5)) {
                        this.chat("Hấp thụ.. các ngươi nghĩ sao vậy?");
                    }
                    return 0;
            }
        }
        return super.injured(plAtt, damage, piercing, isMobAttack);
    }

    @Override
    public void wakeupAnotherBossWhenDisappear() {
        if (this.parentBoss != null) {
            this.parentBoss.changeToTypePK();
        }
    }

}
