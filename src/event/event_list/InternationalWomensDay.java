package event.event_list;

/*
 * @Zalo : 0372.665.345
 */


import event.Event;
import database.daos.EventDAO;

public class InternationalWomensDay extends Event {

    @Override
    public void init() {
        super.init();
        EventDAO.loadInternationalWomensDayEvent();
    }

    @Override
    public void npc() {
    }
}
