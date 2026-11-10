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

public class DrLychee extends Boss {

    private final int level;
    private Clan clan;

    private static final int[][] FULL_DEMON = new int[][]{{Skill.DEMON, 1}, {Skill.DEMON, 2}, {Skill.DEMON, 3}, {Skill.DEMON, 4}, {Skill.DEMON, 5}, {Skill.DEMON, 6}, {Skill.DEMON, 7}};

    public DrLychee(Zone zone, Clan clan, int level, long dame, long hp) throws Exception {
        super(PHOBANKGHD, BossID.DR_LYCHEE, new BossData(
                "Dr Lychee",
                ConstPlayer.TRAI_DAT,
                new short[]{742, 743, 744, -1, -1, -1},
                ((10000 + dame)),
                new long[]{((1000000 + hp))},
                new int[]{148},
                (int[][]) Util.addArray(FULL_DEMON),
                new String[]{"|-1|Ta đợi các ngươi mãi",
                    "|-1|Bọn xayda các ngươi mau đền tội đi"},
                new String[]{"|-1|Đại bác báo thù...",
                    "|-1|Heyyyyyyyy Yaaaaa"},
                new String[]{"|-1|Các ngươi khá lắm",
                    "|-1|Hatchiyack sẽ báo thù cho ta"},
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

            damage = this.nPoint.subDameInjureWithDeff(damage + Util.nextInt(-50 * this.level, 0));

            damage -= damage / 100 * (this.level / 10);

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
        long bossDamage = Math.min((long) (this.nPoint.dame * 1.5), 200000000L);
        long bossMaxHealth = Math.min((long) (this.nPoint.hpMax * 1.5), 2000000000L);
        if (clan != null && clan.KhiGasHuyDiet != null && clan.KhiGasHuyDiet.bosses != null) {
            try {
                clan.KhiGasHuyDiet.bosses.add(new Hatchiyack(
                        zone,
                        clan,
                        level,
                        bossDamage,
                        bossMaxHealth
                ));
            } catch (Exception ex) {
                utils.Logger.logException(DrLychee.class, ex, "Lỗi spawn Hatchiyack sau khi DrLychee rời map");
            }
        }
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        GasDestroyManager.gI().removeBoss(this);
        this.dispose();
    }
}
