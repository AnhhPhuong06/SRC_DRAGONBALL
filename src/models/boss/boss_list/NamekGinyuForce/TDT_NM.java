package models.boss.boss_list.NamekGinyuForce;

 
import models.boss.Boss;
import consts.BossID;
import consts.BossStatus;
import java.util.Random;
import models.boss.BossesData;
import models.item.Item.ItemOption;
import models.map.ItemMap;
import models.player.Player;
import services.Service;
import utils.Util;

public class TDT_NM extends Boss {

    private long st;

    public TDT_NM() throws Exception {
        super(BossID.TIEU_DOI_TRUONG_NM, false, true, BossesData.TIEU_DOI_TRUONG_NM);
    }

    @Override
    public void moveTo(int x, int y) {
        if (this.currentLevel == 1) {
            return;
        }
        super.moveTo(x, y);
    }

    // Quản lý rơi thỏi vàng trong 1 turn của TDST Namek (ngẫu nhiên 1-3 con rơi 1-5 TV)
    public static int cycleKilled = 0;
    public static int targetDropInCycle = Util.nextInt(1, 3);
    public static int droppedInCycle = 0;
    public static long lastTurnTime = System.currentTimeMillis();

    @Override
    public void reward(Player plKill) {}

    @Override
    protected void notifyJoinMap() {
        if (this.currentLevel == 1) {
            return;
        }
        super.notifyJoinMap();
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
        lastTurnTime = System.currentTimeMillis();
    }

    @Override
    public void doneChatS() {
        this.changeStatus(BossStatus.AFK);
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
}
