package models.boss.boss_list.Commeson;

 


import models.boss.Boss;
import models.boss.BossData;
import managers.boss.BossManager;
import consts.BossStatus;
import consts.ConstPlayer;
import consts.ConstRatio;
import models.item.Item;
import models.map.ItemMap;
import models.matches.pvp.PKCommeson;
import models.player.Player;
import server.ServerNotify;
import services.player.PlayerService;
import services.Service;
import services.SkillService;
import services.map.ChangeMapService;
import utils.SkillUtil;
import utils.Util;

public class NhanBan extends Boss {

    private Player playerAtt;
    private long timeJoinMap;

    public NhanBan(Player player, BossData bossData) throws Exception {
        super(Util.createIdBossClone((int) player.id), bossData);
        this.playerAtt = player;
        this.isCopy = true;
    }

    @Override
    public void reward(Player plKill) {
    }

    @Override
    public void active() {
        if (this.typePk == ConstPlayer.NON_PK) {
            new PKCommeson(playerAtt, this);
        }
        this.attack();
    }

    @Override
    public void joinMap() {
        this.zone = this.playerAtt.zone;
        ChangeMapService.gI().changeMap(this, this.zone,
                this.playerAtt.location.x + Util.nextInt(-200, 200), this.playerAtt.location.y);
        this.changeStatus(BossStatus.CHAT_S);
    }

    @Override
    public void goToXY(int x, int y, boolean isTeleport) {
        if (!isTeleport) {
            byte dir = (byte) (this.location.x - x < 0 ? 1 : -1);
            byte move = (byte) Util.nextInt(50, 100);
            PlayerService.gI().playerMove(this, this.location.x + (dir == 1 ? move : -move), y);
        } else {
            ChangeMapService.gI().changeMapYardrat(this, this.zone, x, y);
        }
    }

    private void goToPlayer(Player pl, boolean isTeleport) {
        goToXY(pl.location.x, pl.location.y, isTeleport);
    }

    @Override
    public void attack() {
        try {
            if (playerAtt.typePk == ConstPlayer.NON_PK) {
                PlayerService.gI().changeAndSendTypePK(playerAtt, ConstPlayer.PK_PVP);
            }
            if (this.typePk == ConstPlayer.NON_PK) {
                PlayerService.gI().changeAndSendTypePK(this, ConstPlayer.PK_PVP);
            }
            if (playerAtt.location != null && playerAtt != null && playerAtt.zone != null && this.zone != null && this.zone.equals(playerAtt.zone)) {
                if (this.isDie()) {
                    return;
                }
                this.playerSkill.skillSelect = this.playerSkill.skills.get(Util.nextInt(0, this.playerSkill.skills.size() - 1));
                if (Util.getDistance(this, playerAtt) <= this.getRangeCanAttackWithSkillSelect()) {
                    if (Util.isTrue(15, ConstRatio.PER100) && SkillUtil.isUseSkillChuong(this)) {
                        goToXY(playerAtt.location.x + (Util.getOne(-1, 1) * Util.nextInt(20, 80)), Util.nextInt(10) % 2 == 0 ? playerAtt.location.y : playerAtt.location.y - Util.nextInt(0, 50), false);
                    }
                    SkillService.gI().useSkill(this, playerAtt, null, -1, null);
                    checkPlayerDie(playerAtt);
                } else {
                    goToPlayer(playerAtt, false);
                }
            }
        } catch (Exception ex) {
//            ex.printStackTrace();
        }
    }

    @Override
    public void die(Player plKill) {
        if (plKill != null) {
            rewardWithConfiguredDrops(plKill);
            ServerNotify.gI().notify(plKill.name + " đã đánh bại bản sao Commeson, mọi người đều ngưỡng mộ");
        }
        this.changeStatus(BossStatus.DIE);
    }

    @Override
    public void autoLeaveMap() {
        if (playerAtt == null || playerAtt.effectSkill == null) {
            this.leaveMap();
            return;
        }
        if (!playerAtt.effectSkill.isPKCommeson) {
            Service.gI().sendThongBao(playerAtt, "Bạn đã thất bại, ngày mai hãy thử sức tiếp");
            this.leaveMap();
        }
    }

    @Override
    public void leaveMap() {
        ChangeMapService.gI().exitMap(this);
        this.lastZone = null;
        this.lastTimeRest = System.currentTimeMillis();
        this.changeStatus(BossStatus.REST);
        BossManager.gI().removeBoss(this);
        this.dispose();
    }
}
