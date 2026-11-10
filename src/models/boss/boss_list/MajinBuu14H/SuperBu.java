package models.boss.boss_list.MajinBuu14H;


import models.boss.Boss;
import consts.BossID;
import models.boss.BossesData;
import consts.BossStatus;
import static consts.BossType.BOSS14H;
import java.util.ArrayList;
import java.util.List;
import managers.boss.Boss14hManager;
import models.item.Item;
import models.map.ItemMap;
import models.player.Player;
import server.ServerNotify;
import services.EffectSkillService;
import services.ItemService;
import services.Service;
import services.SkillService;
import services.TaskService;
import services.map.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

public class SuperBu extends Boss {

    private long lastTimeUseSkill;
    private long timeUseSkill;

    public SuperBu() throws Exception {
        super(BOSS14H, BossID.SUPERBU, BossesData.SUPER_BU_BUNG);
    }

    @Override
    public void joinMap() {
        if (zoneFinal != null) {
            this.zone = zoneFinal;
        }
        int x = this.location.x > 0 ? this.location.x : (this.zone.map.mapWidth > 200 ? Util.nextInt(100, this.zone.map.mapWidth - 100) : this.zone.map.mapWidth / 2);
        int y = this.location.y > 0 ? this.location.y : this.zone.map.yPhysicInTop(x, 100);
        ChangeMapService.gI().changeMap(this, this.zone, x, y);
        this.changeStatus(BossStatus.ACTIVE);
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 100)) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                if (Util.canDoWithTime(lastTimeUseSkill, timeUseSkill)) {
                    Service.gI().sendMabuAttackSkill(this);
                    lastTimeUseSkill = System.currentTimeMillis();
                    timeUseSkill = Util.nextInt(5000, 10000);
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills
                        .get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)), pl.location.y);
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)), pl.location.y);
                        }
                    }
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else {
                    if (Util.isTrue(1, 2)) {
                        this.moveToPlayer(pl);
                    }
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void reward(Player plKill) {
        models.task.Task_Kol.tick(plKill, 1, 4, 8);
        // KOL 4/8 (hạ boss Majin Buu 14h)
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        Boss boss = Boss14hManager.gI().getBossById(BossID.MABU, 127, this.zone.zoneId);
        if (boss != null) {
            boss.injured(plAtt, damage, piercing, isMobAttack);
        }
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(10, 100)) {
                this.chat("Xí hụt");
                return 0;
            }

            damage = this.nPoint.subDameInjureWithDeff((long) damage);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            if (damage >= 30000000) {
                damage = 30000000 + Util.nextInt(-10000, 10000);
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
    public void die(Player plKill) {
        if (plKill != null) {
            List<Player> pls = new ArrayList<>();
            Boss boss = Boss14hManager.gI().getBossById(BossID.MABU, 127, this.zone.zoneId);
            if (boss != null) {
                Mabu2H mabu2H = (Mabu2H) boss;
                List<Player> players = mabu2H.maBuEat;
                for (Player pl : players) {
                    pls.add(pl);
                }
                for (Player pl : pls) {
                    if (pl != null && pl.zone != null && pl.zone.map.mapId == 128) {
                        if (pl.zone.zoneId == this.zone.zoneId) {
                            int originalZoneId = pl.zone.zoneId;
                            ChangeMapService.gI().changeMap(pl, 127, originalZoneId, -1, 312);
                        }
                    }
                }
                players.removeIf(pl -> {
                    if (pl == null || pl.zone == null) {
                        return true;
                    }
                    return pl.zone.map.mapId != 128 ||
                            (pl.zone.map.mapId == 127 && pl.zone.zoneId == this.zone.zoneId);
                });
            }
            rewardWithConfiguredDrops(plKill);
            ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
        }
        this.changeStatus(BossStatus.DIE);
    }

}

