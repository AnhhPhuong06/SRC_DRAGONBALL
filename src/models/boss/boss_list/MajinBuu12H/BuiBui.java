package models.boss.boss_list.MajinBuu12H;

import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.BossesData;
import static consts.BossType.BOSS12H;
import static consts.BossType.FINAL;
import models.item.Item;
import java.util.List;
import models.map.ItemMap;

import models.player.Player;
import services.Service;
import utils.Util;

import server.ServerNotify;
import services.EffectSkillService;
import services.ItemService;
import services.TaskService;
import models.skill.Skill;

public class BuiBui extends Boss {

    private long lastTimeAfk;

    private long lastTimeChatAfk;

    private int timeChat;

    public BuiBui() throws Exception {
        super(BOSS12H, BossID.BUI_BUI, BossesData.BUI_BUI);
    }

    @Override
    public void reward(Player plKill) {
        plKill.fightMabu.changePoint((byte) 10);
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(200, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (plAtt != null) {
                switch (plAtt.playerSkill.skillSelect.template.id) {
                    case Skill.KAMEJOKO:
                    case Skill.MASENKO:
                    case Skill.ANTOMIC:
                    case Skill.LIEN_HOAN:
                        return 0;
                }
            }

            if (plAtt != null && plAtt.isPl() && Util.isTrue(1, 5)) {
                plAtt.fightMabu.changePercentPoint((byte) 1);
            }

            damage = this.nPoint.subDameInjureWithDeff((long) damage);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }

            this.nPoint.subHP((long) damage);

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
    public void afk() {
        if (Util.canDoWithTime(lastTimeChatAfk, timeChat)) {
            this.chat("Đừng vội mừng, ta sẽ hồi sinh và thịt hết bọn mi");
            this.lastTimeChatAfk = System.currentTimeMillis();
            this.timeChat = Util.nextInt(10000, 15000);
        }
        if (Util.canDoWithTime(lastTimeAfk, 60000)) {
            Service.gI().hsChar(this, this.nPoint.hpMax, this.nPoint.mpMax);
            this.changeStatus(BossStatus.CHAT_S);
        }
    }

    @Override
    public void die(Player plKill) {
        if (plKill != null) {
            rewardWithConfiguredDrops(plKill);
            ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
        }
        this.lastTimeAfk = System.currentTimeMillis();
        this.changeStatus(BossStatus.AFK);
    }

}


