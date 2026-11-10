package models.boss.boss_list.SnakeWay;

 


import consts.BossID;
import consts.BossStatus;
import managers.boss.SnakeWayManager;
import utils.Functions;
import consts.ConstPlayer;
import models.boss.*;
import static consts.BossType.PHOBANCDRD;
import models.clan.Clan;
import network.io.Message;
import java.util.List;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import services.EffectSkillService;
import services.map.MapService;
import services.player.PlayerService;
import models.skill.Skill;
import services.Service;
import services.map.ChangeMapService;
import utils.Util;

public class SAIBAMEN extends Boss {

    private Clan clan;
    private int idboss;

    private static final int[][] FULL_GALICK = new int[][]{{Skill.GALICK, 1}, {Skill.GALICK, 2}, {Skill.GALICK, 3}, {Skill.GALICK, 4}, {Skill.GALICK, 5}, {Skill.GALICK, 6}, {Skill.GALICK, 7}};

    public SAIBAMEN(Zone zone, Clan clan, int id, long dame, long hp) throws Exception {
        super(PHOBANCDRD, BossID.SAIBAMEN - id, new BossData(
                "Số " + id,
                ConstPlayer.XAYDA,
                new short[]{642, 643, 644, -1, -1, -1},
                ((10000 + dame)),
                new long[]{((500000 + hp))},
                new int[]{144},
                (int[][]) Util.addArray(FULL_GALICK),
                new String[]{},
                new String[]{},
                new String[]{},
                60
        ));
        this.zone = zone;
        this.clan = clan;
        this.idboss = id;
    }

    @Override
    public void reward(Player plKill) {}

    @Override
    public void afk() {
        if (this.clan == null || this.clan.ConDuongRanDoc == null) {
            this.leaveMap();
            return;
        }
        if (this.idboss == 1) {
            Player pl = getPlayerAttack();
            if (pl == null || pl.isDie()) {
                return;
            }

            this.changeToTypePK();
            Functions.sleep(1500);
            this.changeStatus(BossStatus.ACTIVE);
        } else if (9 - this.clan.ConDuongRanDoc.getNumBossAlive() == this.idboss) {
            this.changeStatus(BossStatus.ACTIVE);
        }
    }

    @Override
    public void joinMap() {
        ChangeMapService.gI().changeMap(this, this.zone, 420 + (this.idboss * 15), 342);
        this.changeStatus(BossStatus.AFK);
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
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        SnakeWayManager.gI().removeBoss(this);
        this.dispose();
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            if (!piercing && Util.isTrue(this.nPoint.tlNeDon, 1000)) {
                this.chat("Xí hụt");
                return 0;
            }
            damage = this.nPoint.subDameInjureWithDeff(damage / 7);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage / 4;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                setBom(plAtt);
            }
            return damage;
        } else {
            if (this.bossStatus != BossStatus.DIE && this.bossStatus != BossStatus.CHAT_E
                    && this.bossStatus != BossStatus.LEAVE_MAP && this.bossStatus != BossStatus.REST
                    && this.bossStatus != BossStatus.RESPAWN) {
                this.setDie(plAtt);
                die(plAtt);
            }
            return 0;
        }
    }

    @Override
    public void setBom(Player plAtt) {
        if (!this.playerSkill.prepareTuSat) {
            for (Player pl : this.zone.getPlayers()) {
                Service.gI().sendThongBao(pl, pl.name + " coi chừng đấy!");
            }
            Service.gI().chat(plAtt, "Trời ơi muộn mất rồi");
            EffectSkillService.gI().startStun(plAtt, System.currentTimeMillis(), 3500);
            //gồng tự sát
            this.playerSkill.prepareTuSat = true;
            this.playerSkill.lastTimePrepareTuSat = System.currentTimeMillis();
            try {
                Message msg = new Message(-45);
                msg.writer().writeByte(7);
                msg.writer().writeInt((int) this.id);
                msg.writer().writeShort(104);
                msg.writer().writeShort(2000);
                Service.gI().sendMessAllPlayerInMap(this, msg);
                msg.cleanup();
            } catch (Exception e) {
            }
            this.nPoint.hp = 0;
            Service.gI().chat(this, "He he he");
            new Thread(() -> {
                try {
                    Thread.sleep(2500);
                    setDie(this);
                    die(plAtt);
                    if (this.zone != null) {
                        long dame = (long) this.nPoint.hpMax * 100L;
                        List<Player> playersMap = this.zone.getNotBosses();
                        if (!MapService.gI().isMapOffline(this.zone.map.mapId)) {
                            for (int i = playersMap.size() - 1; i >= 0; i--) {
                                Player pl = playersMap.get(i);
                                if (pl != null && !this.equals(pl)) {
                                    pl.injured(this, dame, false, false);
                                    PlayerService.gI().sendInfoHpMpMoney(pl);
                                    Service.gI().Send_Info_NV(pl);
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    setDie(this);
                    die(plAtt);
                } finally {
                    this.playerSkill.prepareTuSat = false;
                }
            }, "Saibamen SetBom").start();
        }
    }
}
