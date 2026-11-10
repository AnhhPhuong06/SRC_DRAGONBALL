package models.boss.boss_list.Cell;

 
import consts.BossID;
import consts.BossStatus;
import consts.ConstPlayer;
import models.boss.Boss;
import models.boss.BossesData;
import models.item.Item;
import models.map.ItemMap;
import models.npc.npc_list.XenShop;
import models.player.Player;
import services.EffectSkillService;
import services.ItemService;
import services.Service;
import services.TaskService;
import services.map.ChangeMapService;
import services.player.PlayerService;
import utils.Util;

public class XenBoHung extends Boss {

    private long lastTimeHapThu;
    private int timeHapThu;
    /** Điểm sự kiện nhận mỗi lần hạ boss — đổi quà tại NPC Đường Tăng (chung ví diemDT). Tăng số này nếu muốn thưởng nhiều hơn. */
    private static final int DIEM_SU_KIEN = 1;

    public XenBoHung() throws Exception {
        super(BossID.XEN_BO_HUNG, BossesData.XEN_BO_HUNG_1, BossesData.XEN_BO_HUNG_2, BossesData.XEN_BO_HUNG_3);
    }

    @Override
    public void rest() {
        // Khôi phục cả trạng thái REST bị kẹt từ một lần vào map thất bại trước đó.
        if (this.currentLevel >= 0 && this.currentLevel < this.data.length - 1) {
            this.changeStatus(BossStatus.JOIN_MAP);
            return;
        }
        super.rest();
    }

    @Override
    public void reward(Player plKill) {
        TaskService.gI().checkDoneTaskKillBoss(plKill, this);
        if (this.currentLevel < this.data.length - 1) {
            return;
        }
        models.task.Task_Kol.tick(plKill, 1, 40);
        // KOL 40 (tiêu diệt 150 con Xên Bọ Hùng)
        plKill.effect.addPointTrumSanBoss();
        int x = this.location.x;
        int y = this.zone.map.yPhysicInTop(x, this.location.y - 24);
        // Điểm sự kiện: mỗi lần hạ Xên Bọ Hùng +DIEM_SU_KIEN điểm — đổi quà tại NPC ở đảo Kame (map 5)
        XenShop.themDiem(plKill, DIEM_SU_KIEN);
        Service.gI().sendThongBao(plKill,
                "Bạn nhận +" + DIEM_SU_KIEN + " điểm sự kiện Xên! Tới NPC đảo Kame để đổi quà.");
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            this.changeToTypePK();
        }
        this.hapThu();
        this.attack();
    }

    private void hapThu() {
        if (!Util.canDoWithTime(this.lastTimeHapThu, this.timeHapThu) || !Util.isTrue(1, 100)) {
            return;
        }

        Player pl = this.zone.getRandomPlayerInMap();
        if (pl == null || pl.isDie()) {
            return;
        }
        ChangeMapService.gI().changeMapYardrat(this, this.zone, pl.location.x, pl.location.y);
        long addDame = (long) pl.nPoint.dame * 2L / 100L;
        long addHp = (long) pl.nPoint.hp * 1L / 100L;
        this.nPoint.dameg = (int) Math.min(2_000_000_000L, (long) this.nPoint.dameg + addDame);
        this.nPoint.hpg = (int) Math.min(2_000_000_000L, (long) this.nPoint.hpg + addHp);
        this.nPoint.critg++;
        this.nPoint.calPoint();
        PlayerService.gI().hoiPhuc(this, pl.nPoint.hp, 0);
        pl.injured(null, pl.nPoint.hpMax, true, false);
        Service.gI().sendThongBao(pl, "Bạn vừa bị " + this.name + " hấp thu!");
        this.chat(2, "Ui cha cha, kinh dị quá. " + pl.name + " vừa bị tên " + this.name + " nuốt chửng kìa!!!");
        this.chat("Haha, ngọt lắm đấy " + pl.name + "..");
        this.lastTimeHapThu = System.currentTimeMillis();
        this.timeHapThu = Util.nextInt(10000, 20000);
    }

    @Override
    public synchronized long injured(Player plAtt, long damage, boolean piercing, boolean isMobAttack) {
        if (!this.isDie()) {
            damage = this.nPoint.subDameInjureWithDeff(damage);
            if (!piercing && effectSkill.isShielding) {
                if (damage > nPoint.hpMax) {
                    EffectSkillService.gI().breakShield(this);
                }
                damage = damage;
            }
            this.nPoint.subHP(damage);
            if (isDie()) {
                this.setDie(plAtt);
                die(plAtt);
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

}
