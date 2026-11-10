package event.event_list;

/*
 * @Zalo : 0372.665.345
 */


import consts.BossID;
import event.Event;

public class LunarNewYear extends Event {

    @Override
    public void npc() {
        createNpc(0, 49, 0, 0);
    }

    @Override
    public void boss() {
//        createBoss(BossID.LAN_CON, 10);
    }
}
