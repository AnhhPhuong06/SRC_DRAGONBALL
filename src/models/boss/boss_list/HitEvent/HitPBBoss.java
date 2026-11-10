package models.boss.boss_list.HitEvent;


import consts.BossID;
import consts.BossStatus;
import consts.ConstPlayer;
import java.util.concurrent.ConcurrentHashMap;
import models.boss.*;
import static consts.BossType.HIT_EVENT;
import models.map.ItemMap;
import models.map.Zone;
import models.player.Player;
import models.skill.Skill;
import services.EffectSkillService;
import services.Service;
import services.SkillService;
import services.map.ChangeMapService;
import utils.Util;
import services.ItemService;

public class HitPBBoss extends Boss {

    public static final int MAX_LEVEL = 20;
    private static final long HP_PER_LEVEL = 1_000_000_000L;
    private static final long NEXT_LEVEL_DELAY = 3000L;
    private static final int[] NGOC_RONG_DROP = { 19, 20};
    private static final double GIAP_DAME_NHAN = 0.4;
    private static final ConcurrentHashMap<Long, HitPBBoss> ACTIVE = new ConcurrentHashMap<>();
    private static final int[] HIT_MAT_DROP = {1018, 1019, 1020};

    public final int level;
    private Player owner;
    private long st;

    private static final int[][] SKILLS = new int[][]{
        {Skill.DEMON, 1}, {Skill.DEMON, 2}, {Skill.DEMON, 3}, {Skill.DEMON, 4},
        {Skill.DEMON, 5}, {Skill.DEMON, 6}, {Skill.DEMON, 7}
    };
    private static final short[] HIT_OUTFIT = {520, 521, 522, -1, -1, -1};
    private static final String HIT_NAME = "Hit";


    public static void summon(Player pl) {
        if (pl == null || pl.zone == null) {
            return;
        }
        HitPBBoss cur = ACTIVE.get(pl.id);
        if (cur != null && !cur.isDie()) {
            Service.gI().sendThongBao(pl, "Boss của bạn còn sống, hãy tiêu diệt nó trước!");
            return;
        }
        if (pl.hitBossLv20Time != 0 && !Util.isAfterMidnight(pl.hitBossLv20Time)) {
            Service.gI().sendThongBao(pl, "Bạn đã hạ boss Hit cấp tối đa hôm nay, quay lại sau 00h!");
            return;
        }
        int level = Math.min(pl.halloweenLevelDone + 1, MAX_LEVEL);
        try {
            HitPBBoss boss = new HitPBBoss(pl.zone, pl, level);
            ACTIVE.put(pl.id, boss);
            Service.gI().sendThongBao(pl, "Boss " + HIT_NAME + " cấp " + level + " đã xuất hiện!");
        } catch (Exception e) {
            Service.gI().sendThongBao(pl, "Không thể gọi boss lúc này, thử lại sau!");
        }
    }

    public HitPBBoss(Zone zone, Player owner, int level) throws Exception {
        super(HIT_EVENT, BossID.HIT_PB, true, true, new BossData(
                HIT_NAME + " Lv" + level,
                ConstPlayer.TRAI_DAT,
                HIT_OUTFIT,
                dameFor(level),
                new long[]{hpFor(level)},
                new int[]{181},
                (int[][]) Util.addArray(SKILLS),
                new String[]{"|-1|Ai dám bén mảng vào đây?"},
                new String[]{"|-1|Hùuuu!!!"},
                new String[]{"|-1|Ngươi... khá lắm..."},
                60
        ));
        this.zone = zone;
        this.zoneFinal = zone; 
        this.level = level;
        this.owner = owner;
        this.st = System.currentTimeMillis();
    }

    public HitPBBoss(Zone zone, int level, int variant) throws Exception {
        this(zone, null, level);
    }

    private static long hpFor(int level) {
        return HP_PER_LEVEL * level;
    }

    private static int dameFor(int level) {
        return (int) Math.min(1_000L * level, 200_000_000L);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = (long) (damage * GIAP_DAME_NHAN);
            damage = this.nPoint.subDameInjureWithDeff(damage);
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
        }
        return 0;
    }

    @Override
    public void attack() {
        if (Util.canDoWithTime(this.lastTimeAttack, 800) && this.typePk == ConstPlayer.PK_ALL) {
            this.lastTimeAttack = System.currentTimeMillis();
            try {
                Player pl = getPlayerAttack();
                if (pl == null || pl.isDie()) {
                    return;
                }
                this.nPoint.dame = pl.nPoint.hpMax / Math.max(5, 25 - level);
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, pl) <= this.getRangeCanAttackWithSkillSelect()) {
                    SkillService.gI().useSkill(this, pl, null, -1, null);
                    checkPlayerDie(pl);
                } else if (Util.isTrue(1, 2)) {
                    this.moveToPlayer(pl);
                }
            } catch (Exception ex) {
            }
        }
    }

    @Override
    public void die(Player plKill) {
        if (owner != null) {
            ACTIVE.remove(owner.id);
            if (level > owner.halloweenLevelDone) {
                owner.halloweenLevelDone = level;
            }
            if (level >= MAX_LEVEL) {
                owner.hitBossLv20Time = System.currentTimeMillis();
                Service.gI().sendThongBao(owner, "Chúc mừng! Bạn đã chinh phục boss Hit cấp tối đa (" + MAX_LEVEL + ")! Hẹn gặp lại sau 00h.");
            } else {
                Service.gI().sendThongBao(owner, "Đã hạ boss cấp " + level + "! Boss cấp " + (level + 1) + " sắp xuất hiện...");
                autoSummonNext(owner);
            }
        }
        this.changeStatus(BossStatus.DIE);
    }

    private static void autoSummonNext(final Player pl) {
        new Thread(() -> {
            try {
                Thread.sleep(NEXT_LEVEL_DELAY);
            } catch (InterruptedException ignored) {
            }
            if (pl != null && !pl.isDie() && pl.zone != null && pl.zone.map != null
                    && pl.zone.map.mapId == services.dungeon.HalloweenDungeonService.MAP_ID) {
                summon(pl);
            }
        }, "HitPBBoss-AutoNext-" + pl.id).start();
    }

    @Override
    public void autoLeaveMap() {
        if (owner == null || owner.beforeDispose || owner.zone != this.zone) {
            this.leaveMapNew();
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
    public void leaveMap() {
        // Override hoàn toàn để boss KHÔNG vào lại chu trình REST -> RESPAWN (không tự hồi sinh).
        if (owner != null) {
            ACTIVE.remove(owner.id);
        }
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        managers.boss.HitEventManager.gI().removeBoss(this);
        this.dispose();
    }
}
