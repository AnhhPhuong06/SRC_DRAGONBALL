/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.boss.boss_list.pocolo;

import consts.BossID;
import models.boss.Boss;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.map.Map;
import models.map.Zone;
import models.player.Player;
import network.io.Message;
import services.EffectSkillService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import services.map.MapService;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class Pocolo extends Boss {

    private long st;

    public Pocolo() throws Exception {
        super(BossID.POCOLONE, BossesData.POCOLONE);
    }

    @Override
    public void reward(Player plKill) {
        plKill.effect.addPointTrumSanBoss();
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        new Thread(() -> {
            Zone z = zone;
            for (int i = 30; i != 0; i--) {
                try {
                    Message msg = new Message(-25);
                    msg.writer().writeUTF("Boss " + name + " đã chết\ntiến hành rời map\nsau " + i + " giây");
                    Service.gI().sendMessAllPlayerInMap(z, msg);
                    msg.cleanup();
                    Thread.sleep(1500);
                } catch (Exception e) {
                }
            }
            for (int i = 0; i < z.getPlayers().size(); i++) {
                try {
                    ChangeMapService.gI().changeMapBySpaceShip(z.getPlayers().get(i), 5, -1, Util.nextInt(100));
                } catch (Exception e) {
                }
            }
        }).start();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
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
