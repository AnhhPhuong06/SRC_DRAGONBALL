package managers;

 


import models.ShenronEvent;
import utils.Util;

import java.util.ArrayList;
import java.util.List;
import server.Maintenance;

public class ShenronEventManager implements Runnable {

    private static ShenronEventManager instance;
    private long lastUpdate;
    private static final List<ShenronEvent> list = new ArrayList<>();

    ;

    public static ShenronEventManager gI() {
        if (instance == null) {
            instance = new ShenronEventManager();
        }
        return instance;
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                long start = System.currentTimeMillis();
                update();
                long timeUpdate = System.currentTimeMillis() - start;
                if (1000 - timeUpdate > 0) {
                    Thread.sleep(1000 - timeUpdate);
                }
            } catch (InterruptedException ex) {
            }
        }
    }

    public void update() {
        if (Util.canDoWithTime(lastUpdate, 1000)) {
            lastUpdate = System.currentTimeMillis();
            List<ShenronEvent> listCopy = new ArrayList<>(list);

            for (ShenronEvent se : listCopy) {
                try {
                    se.update();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            listCopy.clear();
        }
    }

    public void add(ShenronEvent se) {
        list.add(se);
    }

    public void remove(ShenronEvent se) {
        list.remove(se);
    }

}
