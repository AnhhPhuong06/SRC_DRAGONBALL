package models.boss;

import consts.BossID;
import consts.BossStatus;
import models.item.Item;
import models.map.ItemMap;
import models.player.Player;
import services.Service;
import utils.Util;

public class ubu extends Boss {

    private long st;

    public ubu() throws Exception {
        super(BossID.UBU, false, true, BossesData.UBU);
    }

    @Override
    public void joinMap() {
        super.joinMap();
        st = System.currentTimeMillis();
    }

    @Override
    public void reward(Player plKill) {}

    @Override
    public void autoLeaveMap() {
        if (Util.canDoWithTime(st, 900000)) {
            this.changeStatus(BossStatus.LEAVE_MAP);
        }
    }
}
