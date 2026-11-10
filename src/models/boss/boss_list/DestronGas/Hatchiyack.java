package models.boss.boss_list.DestronGas;

 


import consts.BossID;
import consts.BossStatus;
import consts.ConstPlayer;
import managers.boss.GasDestroyManager;
import models.boss.*;
import static consts.BossType.PHOBANKGHD;
import models.clan.Clan;
import models.item.Item;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import services.EffectSkillService;
import models.skill.Skill;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;

public class Hatchiyack extends Boss {

    private final int level;
    private Clan clan;

    private static final int[][] FULL_DEMON = new int[][]{{Skill.DEMON, 1}, {Skill.DEMON, 2}, {Skill.DEMON, 3}, {Skill.DEMON, 4}, {Skill.DEMON, 5}, {Skill.DEMON, 6}, {Skill.DEMON, 7}};

    public Hatchiyack(Zone zone, Clan clan, int level, long dame, long hp) throws Exception {
        super(PHOBANKGHD, BossID.HATCHIYACK, new BossData(
                "Hatchiyack",
                ConstPlayer.TRAI_DAT,
                new short[]{639, 640, 641, -1, -1, -1},
                (dame),
                new long[]{hp},
                new int[]{148},
                (int[][]) Util.addArray(FULL_DEMON),
                new String[]{"|-1|Các ngươi dám hạ sư phụ ta",
                    "|-1|Ta sẽ tiêu diệt hết các ngươi"},
                new String[]{"|-1|Đại bác báo thù...",
                    "|-1|Heyyyyyyyy Yaaaaa"},
                new String[]{"|-1|Các ngươi khó mà rời khỏi nơi đây"},
                60
        ));
        this.zone = zone;
        this.level = level;
        this.clan = clan;
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (plAtt != null && plAtt.idNRNM != -1) {
                return 1;
            }

            damage = this.nPoint.subDameInjureWithDeff(damage + Util.nextInt(-100 * this.level, 0));

            damage -= damage / 100 * (this.level / 5);

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
    public void reward(Player plKill) {}

    @Override
    public void joinMap() {
        ChangeMapService.gI().changeMap(this, this.zone, 480, 295);
        this.moveTo(480, 480);
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void die(Player plKill) {
        if (plKill != null) {
            rewardWithConfiguredDrops(plKill);
        }
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void leaveMap() {
        if (clan != null && clan.KhiGasHuyDiet != null) {
            clan.KhiGasHuyDiet.hatchiyatchDead = true;
        }
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        GasDestroyManager.gI().removeBoss(this);
        this.dispose();
    }
}
