package models.boss.boss_list.Earth;

 
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import models.boss.BossesData;
import models.item.Item;
import java.util.List;
import models.map.ItemMap;
import models.player.Player;
import services.ItemService;
import services.Service;
import utils.Util;

public class BOJACK extends Boss {

    private long st;

    public BOJACK() throws Exception {
        super(BossID.BOJACK, false, true, BossesData.BOJACK, BossesData.SUPER_BOJACK);
    }

    // Quản lý rơi thỏi vàng trong 1 turn của nhóm Bojack (ngẫu nhiên 1-3 con rơi 1-5 TV không khóa)
    public static int cycleKilled = 0;
    public static int targetDropInCycle = Util.nextInt(1, 3);
    public static int droppedInCycle = 0;
    public static long lastTurnTime = System.currentTimeMillis();

    @Override
    public void reward(Player plKill) {}

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
        lastTurnTime = System.currentTimeMillis();
    }

    @Override
    public void update() {
        if (prepareBom) {
            return;
        } 
        super.updateInfo(); 
        this.nPoint.mp = this.nPoint.mpg; 
        if (this.isDie() && this.bossStatus != BossStatus.DIE && this.bossStatus != BossStatus.CHAT_E
                && this.bossStatus != BossStatus.LEAVE_MAP && this.bossStatus != BossStatus.REST
                && this.bossStatus != BossStatus.RESPAWN) {
            this.changeStatus(BossStatus.DIE);
        }
        boolean blockedByEffect
                = (this.effectSkill != null && this.effectSkill.isHaveEffectSkill())
                || (this.newSkill != null && this.newSkill.isStartSkillSpecial);

        if (blockedByEffect && this.bossStatus == BossStatus.ACTIVE) {
            return;
        } 
        switch (this.bossStatus) {
            case CHAT_S, AFK, ACTIVE ->
                this.autoLeaveMap();
            default -> { }
        }
        switch (this.bossStatus) {
            case REST ->
                this.rest();

            case RESPAWN -> {
                this.respawn();
                this.changeStatus(BossStatus.JOIN_MAP);
            }

            case JOIN_MAP ->
                this.joinMap();

            case CHAT_S -> {
                if (chatS()) {
                    this.doneChatS();
                    this.lastTimeChatM = System.currentTimeMillis();
                    this.timeChatM = 5000;
                    if (this.bossStatus != BossStatus.AFK) {
                        this.changeStatus(BossStatus.ACTIVE);
                    }
                }
            }

            case AFK ->
                this.afk();

            case ACTIVE -> {
                this.chatM();
                if ((this.effectSkill != null && this.effectSkill.isCharging && !Util.isTrue(1, 20))
                        || (this.effectSkill != null && this.effectSkill.useTroi)) {
                    return;
                }
                this.active();
            }

            case DIE ->
                this.changeStatus(BossStatus.CHAT_E);

            case CHAT_E -> {
                if (chatE()) {
                    this.finishDeathChat();
                }
            }

            case LEAVE_MAP ->
                this.leaveMap();
        }
    }

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.leaveMapNew();
        }
        if (this.zone != null && this.zone.getNumOfPlayers() > 0) {
            st = System.currentTimeMillis();
        }
    }
 

    @Override
    public void doneChatS() {
        if (this.currentLevel == 1) {
            return;
        }
        this.changeStatus(BossStatus.AFK);
    }
}
