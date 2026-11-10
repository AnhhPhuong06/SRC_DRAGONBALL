package event.event_list;

/*
 * @Zalo : 0372.665.345
 */


import consts.BossID;
import event.Event;
import event.EventManager;

public class Halloween extends Event {

    @Override
    public void npc() {
    }

    @Override
    public void boss() {
        if (!EventManager.HALLOWEEN) return;
        createBoss(BossID.MATROI, 3);
        createBoss(BossID.DOI, 3);
        createBoss(BossID.BIMA, 3);
        createBoss(BossID.JANEMBA, 1);
    }

    public static boolean isBoss(int id) {
        return id == BossID.MATROI || id == BossID.DOI || id == BossID.BIMA || id == BossID.JANEMBA;
    }
}
