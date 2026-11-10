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
import services.ItemTimeService;
import services.Service;
import services.SkillService;
import services.TaskService;
import services.map.ChangeMapService;
import models.skill.Skill;
import utils.SkillUtil;
import utils.Util;

public class Mabu2H extends Boss {

    private long lastTimeEat;

    private long lastTimeUseSkill;
    private long timeUseSkill;
    public List<Player> maBuEat = new ArrayList<>();

    public Mabu2H() throws Exception {
        super(BOSS14H, BossID.MABU, BossesData.MABU, BossesData.SUPER_BU, BossesData.BU_TENK, BossesData.BU_HAN, BossesData.KID_BU);
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

    private void eatPlayersInTheMap() {
        int numPlayers = 0;
        // Chỉ nuốt người chơi trong cùng zone với boss
        for (Player pl : this.zone.getPlayers()) {
            if (pl != null && pl.zone != null && pl.zone == this.zone && pl.isPl() && !pl.isBoss
                    && !pl.isMabuHold && pl.maBuHold == null && !this.maBuEat.contains(pl)) {
                if (Util.isTrue(3, 30)) {
                    pl.isMabuHold = true;
                    Service.gI().sendMabuEat(this, pl);
                    this.maBuEat.add(pl);
                    numPlayers++;
                }
            }
        }
        if (numPlayers > 0) {
            this.chat("Măm măm");
        }
    }

    private void petrifyPlayersInTheMap() {
        for (Player pl : this.zone.getNotBosses()) {
            if (Util.isTrue(1, 5)) {
                this.chat("Úm ba la xì bùa");
                EffectSkillService.gI().setSocola(pl, System.currentTimeMillis(), 30000);
                Service.gI().Send_Caitrang(pl);
                ItemTimeService.gI().sendItemTime(pl, 4133, 30);
            }
        }
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
                // Giảm tần suất skill nuốt: chỉ sử dụng mỗi 45-60 giây
                if (Util.canDoWithTime(lastTimeEat, 25000)) {
                    eatPlayersInTheMap();
                    if (this.currentLevel == 0) {
                        petrifyPlayersInTheMap();
                    }
                    this.lastTimeEat = System.currentTimeMillis();
                }
                if (this.currentLevel > 0) {
                    if (Util.canDoWithTime(lastTimeUseSkill, timeUseSkill)) {
                        Service.gI().sendMabuAttackSkill(this);
                        lastTimeUseSkill = System.currentTimeMillis();
                        timeUseSkill = Util.nextInt(5000, 10000);
                        return;
                    }
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
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            // Kiểm tra chỉ tính người chơi trong bụng thuộc zone này
            boolean hasPlayersInBelly = false;
            for (Player pl : this.maBuEat) {
                if (pl != null && pl.zone != null && pl.zone.map.mapId == 128 && pl.zone.zoneId == this.zone.zoneId) {
                    hasPlayersInBelly = true;
                    break;
                }
            }

            if (hasPlayersInBelly) {
                damage = 1;
                this.nPoint.subHP((long) damage);
                if (isDie()) {
                    this.setDie(plAtt);
                    Boss boss = Boss14hManager.gI().getBossById(BossID.SUPERBU, 128, this.zone.zoneId);
                    if (boss != null) {
                        boss.changeStatus(BossStatus.DIE);
                    }
                    die(plAtt);
                }
                return damage;
            }

            if (!piercing && Util.isTrue(10, 100)) {
                this.chat("Xí hụt");
                return 0;
            }

            if (plAtt.isPl() && Util.isTrue(1, 5)) {
                plAtt.fightMabu.changePercentPoint((byte) 1);
            }

            damage = this.nPoint.subDameInjureWithDeff((long) damage);

            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = 1;
            }
            // Cơ chế KidBu: khi HP <= 250k
            if (this.currentLevel == this.data.length - 1) {
                int skillId = -1;
                if (plAtt.playerSkill != null && plAtt.playerSkill.skillSelect != null
                        && plAtt.playerSkill.skillSelect.template != null) {
                    skillId = plAtt.playerSkill.skillSelect.template.id;
                }

                if (this.nPoint.hp <= 250_000L) {
                    if (skillId != Skill.MAKANKOSAPPO && skillId != Skill.TU_SAT
                            && skillId != Skill.QUA_CAU_KENH_KHI) {
                        this.chat("Xí hụt");
                        return 1;
                    }
                } else {
                    if (this.nPoint.hp <= 31_000_000L
                            && plAtt.playerSkill != null
                            && plAtt.playerSkill.skillSelect != null
                            && plAtt.playerSkill.skillSelect.template != null
                            && plAtt.playerSkill.skillSelect.template.id != Skill.QUA_CAU_KENH_KHI) {
                        damage = damage >= this.nPoint.hp ? 200_000 : damage;
                    }
                }
            }
            if (damage >= 30000000) {
                damage = 30000000 + Util.nextInt(-10000, 10000);
            }
            this.nPoint.subHP((long) damage);
            if (isDie()) {
                this.setDie(plAtt);
                Boss boss = Boss14hManager.gI().getBossById(BossID.SUPERBU, 128, this.zone.zoneId);
                if (boss != null) {
                    boss.changeStatus(BossStatus.DIE);
                }
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
            List<Player> players = this.maBuEat;
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
                return pl.zone.map.mapId != 128
                        || (pl.zone.map.mapId == 127 && pl.zone.zoneId == this.zone.zoneId);
            });
            rewardWithConfiguredDrops(plKill);
            ServerNotify.gI().notify(plKill.name + ": Đã tiêu diệt được " + this.name + " mọi người đều ngưỡng mộ.");
        }
        this.changeStatus(BossStatus.DIE);
    }

}
