package models.boss;

import consts.BossID;
import consts.ConstPlayer;
import models.player.Player;
import models.skill.Skill;
import services.EffectSkillService;
import services.player.PlayerService;
import services.Service;
import services.SkillService;
import utils.Util;
import models.item.Item;
import models.map.ItemMap;

public class MatTroi extends Boss {

    private int heatPercent;
    private long lastHeatTime; 
    private static final String[] TEXT = {
        "Sao nóng thế nhỉ?",
        "Nóng quá đi mất!",
        "Đi mua cho tôi cốc nước mía!"
    };

    public MatTroi() throws Exception {
        super(-998877,
                new BossData(
                        "Sun", ConstPlayer.XAYDA,
                        new short[]{1684, 1685, 1686, -1, 1, -1},
                        100,
                        new long[]{10000},
                        new int[]{42, 43, 44},
                        new int[][]{{Skill.TAI_TAO_NANG_LUONG, 5, 60_000}},
                        new String[]{},
                        new String[]{"|-1|Phù phù"},
                        new String[]{},
                        600));
        heatPercent = 0;
        lastHeatTime = System.currentTimeMillis(); 
    }

    private void subhp() {
        if (this.playerTarger.itemTime.miakl || this.playerTarger.itemTime.miathom || this.playerTarger.itemTime.miasaurieng) {
            return;
        }
        if (nPoint == null) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!Util.canDoWithTime(lastHeatTime, 5_000)) {
            return;
        }
        heatPercent = Math.min(heatPercent + 1, 15);
        int idx = heatPercent < 5 ? 0 : heatPercent < 10 ? 1 : 2;

        for (Player pl : zone.getNotBosses()) {
            if (pl == null || pl.nPoint == null || pl.isDie()) {
                continue;
            }
            long dmg = pl.nPoint.hpMax * heatPercent / 100;
            dmg = (dmg >= pl.nPoint.hp) ? pl.nPoint.hp - 1 : dmg;
            Service.gI().chat(pl, TEXT[idx]);
            pl.injured(null, dmg, true, false);
            PlayerService.gI().sendInfoHpMpMoney(pl);
            Service.gI().Send_Info_NV_do_Injure(pl);
        }
        lastHeatTime = now;
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 500) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.nPoint.dame = pl.nPoint.hpMax / Util.nextInt(30, 50);
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(5, 20)) {
                        int dx = Util.getOne(-1, 1) * Util.nextInt(10, 200);
                        int dy = Util.nextInt(2) == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70);
                        this.moveTo(pl.location.x + dx, dy);
                    }
                    subhp();
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else if (Util.isTrue(1, 2)) {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (isDie()) {
            return 0;
        }
        damage = nPoint.subDameInjureWithDeff(damage);
        if (!piercing && effectSkill.isShielding) {
            if (damage > nPoint.hpMax) {
                EffectSkillService.gI().breakShield(this);
            }
            damage = 1;
        }
        damage = Math.min(damage, 100);
        if (damage >= nPoint.hp) {
            rewardWithConfiguredDrops(plAtt);
            nPoint.hp = 0;
            leaveMap();
            return 0;
        }
        nPoint.subHP(damage);
        if (isDie()) {
            setDie(plAtt);
            die(plAtt);
        }
        return 0;
    }

    @Override
    public void reward(Player plKill) {}
}
