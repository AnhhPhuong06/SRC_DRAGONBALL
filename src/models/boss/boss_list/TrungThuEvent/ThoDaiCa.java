/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package models.boss.boss_list.TrungThuEvent;

import consts.BossID;
import models.boss.*;
import consts.ConstPlayer;
import models.item.ItemTime;
import models.map.ItemMap;

import models.player.Player;
import models.skill.Skill;
import services.EffectSkillService;
import services.ItemTimeService;
import services.Service;
import services.SkillService;
import utils.SkillUtil;
import utils.Util;

/**
 *
 * @author Administrator
 */
public class ThoDaiCa extends Boss {

    public ThoDaiCa() throws Exception {
        super(BossID.AN_TROM, new BossData(
                "Thỏ đại ca " + Util.nextInt(1, 100),
                ConstPlayer.TRAI_DAT,
                new short[]{403, 404, 405, -1, -1, -1},
                1,
                new long[]{50000},
                new int[]{3, 4, 5, 6, 27, 28, 29, 30,
                    9, 11, 12, 13, 10, 34, 33, 32, 31,
                    16, 17, 18, 19, 20, 37, 38, 36, 35,
                    24, 25, 26},
                new int[][]{{Skill.DRAGON, 3, 20000}},
                new String[]{"|-1|Ta bắt tay làm quen nhé", "|-1|Bắt tay làm quen nhé"},
                new String[]{"|-1|Muốn biến hành củ cà rốt thì cứ nhào vô", "|-1|Ta thách ngươi đấy nhóc con"},
                new String[]{"|-1| Ôi xin ông đừng nấu cà ri con!!!"},
                300
        ));
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
                        if (SkillUtil.isUseSkillChuong(this)) {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 200)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 70));
                        } else {
                            this.moveTo(pl.location.x + (Util.getOne(-1, 1) * Util.nextInt(10, 40)),
                                    Util.nextInt(10) % 2 == 0 ? pl.location.y : pl.location.y - Util.nextInt(0, 50));
                        }
                    }
                    if (!pl.itemTime.isXuong) {
                        pl.itemTime.isXuong = true;
                        pl.itemTime.iconXuong = 4082;
                        pl.itemTime.lastTimeXuong = System.currentTimeMillis();
                        ItemTimeService.gI().sendItemTime(pl, pl.itemTime.iconXuong, (int) ((ItemTime.TIME_MAY_DO - (System.currentTimeMillis() - pl.itemTime.lastTimeXuong)) / 1000));
                        Service.gI().Send_Caitrang(pl);
                        Service.gI().point(pl);
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
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = this.nPoint.subDameInjureWithDeff(damage / 7);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 1;
            }
            if (damage > 1000) {
                damage = 1000;
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
}
