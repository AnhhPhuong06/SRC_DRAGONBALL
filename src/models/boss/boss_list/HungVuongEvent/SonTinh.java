package models.boss.boss_list.HungVuongEvent;



import consts.BossID;
import consts.BossStatus;
import models.boss.*;
import static consts.BossType.HUNGVUONG_EVENT;
import event.EventManager;

import models.item.Item;
import java.util.ArrayList;
import java.util.List;
import models.map.ItemMap;
import models.player.Player;
import services.EffectSkillService;
import services.player.PlayerService;
import services.Service;
import services.SkillService;
import services.map.ChangeMapService;
import utils.Util;

public class SonTinh extends Boss {

    private long lastTimeMove;

    private int timeMove;

    private boolean isReward;

    private long lastTimeReward;

    public SonTinh() throws Exception {
        super(HUNGVUONG_EVENT, BossID.SON_TINH, true, false, BossesData.SON_TINH);
    }

    @Override
    public void reward(Player plKill) {
        models.task.Task_Kol.tick(plKill, 1, 23);
        // KOL 23 (tiêu diệt 10 Sơn Tinh)
        this.parentBoss.playerReward = plKill;
        this.parentBoss.changeStatus(BossStatus.AFK);
    }

    @Override
    public void afk() {

        if (playerReward.isPl() && !isReward && this.zone != null) {
        isReward = true;
        lastTimeReward = System.currentTimeMillis();
        this.chat("Được! hảo hán!");
        }

        if (Util.canDoWithTime(lastTimeReward, 3000)) {
            this.leaveMap();
        }    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (!piercing && damage > 1000000) {
                damage = Util.nextInt(900000, 1000000);
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
    public void update() {
        if (!EventManager.HUNG_VUONG) {
            if (this.bossStatus != BossStatus.REST) {
                this.leaveMap();
            }
            return;
        }
        super.update();
    }

    @Override
    public void rest() {
        if (!EventManager.HUNG_VUONG) {
            return;
        }
        super.rest();
    }

    @Override
    public void respawn() {
        if (!EventManager.HUNG_VUONG) {
            this.changeStatus(BossStatus.REST);
            return;
        }
        super.respawn();
    }

    @Override
    public void joinMap() {
        if (!EventManager.HUNG_VUONG) {
            this.leaveMap();
            return;
        }
        super.joinMap(); //To change body of generated methods, choose Tools | Templates.
        st = System.currentTimeMillis();
        Service.gI().changeFlag(this, 2);
    }

    private long st;

    @Override
    public void autoLeaveMap() {
        if (!EventManager.HUNG_VUONG) {
            this.leaveMap();
            return;
        }
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }

    @Override
    public void active() {
        this.attack();
    }

    @Override
    public Player getPlayerAttack() {
        List<Player> plNotVoHinh = new ArrayList();
        for (Player pl : this.zone.getNotBosses()) {
            if ((pl.effectSkin == null || !pl.effectSkin.isVoHinh) && pl.cFlag != this.cFlag) {
                plNotVoHinh.add(pl);
            }
        }
        for (Player pl : this.zone.getBosses()) {
            if (!pl.equals(this) && pl.cFlag == 1) {
                plNotVoHinh.add(pl);
            }
        }
        if (!plNotVoHinh.isEmpty()) {
            return plNotVoHinh.get(Util.nextInt(0, plNotVoHinh.size() - 1));
        }

        return null;
    }

    @Override
    public void attack() {
        if (this.effectSkill.isCharging) {
            return;
        }
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    if (Util.canDoWithTime(lastTimeMove, timeMove)) {
                        Player plRand = super.getPlayerAttack();
                        if (plRand != null) {
                            this.moveToPlayer(plRand);
                            this.lastTimeMove = System.currentTimeMillis();
                            this.timeMove = Util.nextInt(5000, 30000);
                        }
                    }
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                int dis = Util.getDistance(this, pl);
                if (dis > 450) {
                    move(pl.location.x - 24, pl.location.y);
                } else if (dis > 100) {
                    int dir = (this.location.x - pl.location.x < 0 ? 1 : -1);
                    int move = Util.nextInt(50, 100);
                    move(this.location.x + (dir == 1 ? move : -move), pl.location.y);
                } else {
                    if (Util.isTrue(30, 100)) {
                        int move = Util.nextInt(50);
                        move(pl.location.x + (Util.nextInt(0, 1) == 1 ? move : -move), this.location.y);
                    }
                    if (pl.isPl()) {
                        this.nPoint.dame = pl.nPoint.hpMax / 30;
                    } else {
                        this.nPoint.dame = 10000;
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                }
            } catch (Exception ex) {
//                ex.printStackTrace();
            }
        }
    }

    @Override
    public void moveTo(int x, int y) {
        byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
        byte move = (byte) Util.nextInt(50, 100);
        PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y);
    }

    @Override
    public void moveToPlayer(Player pl) {
        if (pl.location != null) {
            moveTo(pl.location.x, pl.location.y);
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.isReward = false;
        this.playerReward = null;
        this.changeStatus(BossStatus.REST);
    }
}
