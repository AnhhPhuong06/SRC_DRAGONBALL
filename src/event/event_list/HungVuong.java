package event.event_list;

 


import consts.BossID;
import event.Event;
import event.EventManager;
import managers.boss.HungVuongEventManager;

public class HungVuong extends Event {

    @Override
    public void boss() {
        if (!EventManager.HUNG_VUONG) {
            return;
        }
        if (HungVuongEventManager.gI().getBosses().isEmpty()) {
            createBoss(BossID.THUY_TINH, 10);
        } else {
            HungVuongEventManager.gI().respawnAllRestingBosses();
        }
    }
}
