package models.dungeon;

import utils.Functions;
import java.util.ArrayList;
import java.util.List;
import models.map.Zone;
import models.map.MaBuHold;
import models.player.Player;
import server.Maintenance;
import services.map.MapService;
import services.map.ChangeMapService;
import utils.TimeUtil;

public final class MajinBuu14H implements Runnable {

    public static final int MIN_INSTANCES = 3; // SÃ¡Â»â€˜ instance tÃ¡Â»â€˜i thiÃ¡Â»Æ’u
    public static final int MAX_INSTANCES = 10; // SÃ¡Â»â€˜ instance tÃ¡Â»â€˜i Ã„â€˜a
    public static final int PLAYERS_PER_INSTANCE = 5; // SÃ¡Â»â€˜ ngÃ†Â°Ã¡Â»Âi tÃ¡Â»â€˜i Ã„â€˜a mÃ¡Â»â€”i instance
    public static final long IDLE_TIMEOUT = 300000; // 5 phÃƒÂºt khÃƒÂ´ng hoÃ¡ÂºÂ¡t Ã„â€˜Ã¡Â»â„¢ng sÃ¡ÂºÂ½ Ã„â€˜ÃƒÂ³ng (ms)

    public int id;
    public final List<Zone> zones;
    private Thread thread;
    private volatile boolean running;
    private long lastActivityTime;

    public MajinBuu14H(int id) {
        this.id = id;
        this.zones = new ArrayList<>();
        this.running = false;
        this.lastActivityTime = System.currentTimeMillis();
    }

    public synchronized void start() {
        if (running || thread != null) {
            return;
        }
        running = true;
        thread = new Thread(this, "MajinBuu 14H - Id : " + id);
        thread.start();
    }

    /**
     * Stop thread cho instance nÃƒÂ y
     */
    public synchronized void stop() {
        if (!running) {
            return;
        }
        running = false;
        if (thread != null) {
            thread.interrupt();
            thread = null;
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int getTotalPlayers() {
        int count = 0;
        List<Zone> zonesCopy;
        synchronized (zones) {
            zonesCopy = new ArrayList<>(zones);
        }
        for (Zone zone : zonesCopy) {
            if (zone != null) {
                count += zone.getNumOfPlayers();
            }
        }
        return count;
    }

    public boolean isEmpty() {
        return getTotalPlayers() == 0;
    }

    public boolean isFull() {
        return getTotalPlayers() >= PLAYERS_PER_INSTANCE;
    }

    public boolean isIdleTooLong() {
        return isEmpty() && (System.currentTimeMillis() - lastActivityTime) > IDLE_TIMEOUT;
    }

    public void updateActivity() {
        this.lastActivityTime = System.currentTimeMillis();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning && running) {
            try {
                long startTime = System.currentTimeMillis();
                update();
                Functions.sleep(Math.max(150 - (System.currentTimeMillis() - startTime), 10));
            } catch (Exception e) {
                if (Thread.interrupted()) {
                    break;
                }
                e.printStackTrace();
            }
        }
        running = false;
    }

    public void update() {
        if (!TimeUtil.isMabu14HOpen()) {
            finish();
            return;
        }

        if (!isEmpty()) {
            updateActivity();
        }

        for (int j = zones.size() - 1; j >= 0; j--) {
            Zone zone = zones.get(j);
            for (MaBuHold hold : zone.maBuHolds) {
                if (hold.player != null && hold.player.maBuHold == null && hold.player.zone != null) {
                    hold.player = null;
                }
            }
        }
    }

    public MaBuHold getMaBuHold() {
        List<Zone> zonesCopy;
        synchronized (zones) {
            zonesCopy = new ArrayList<>(zones);
        }
        for (Zone zone : zonesCopy) {
            if (zone != null && zone.map != null && zone.map.mapId == 128) {
                for (MaBuHold hold : zone.maBuHolds) {
                    if (hold.player == null) {
                        return hold;
                    }
                }
            }
        }
        return null;
    }

    public Zone getMapById(int mapId) {
        List<Zone> zonesCopy;
        synchronized (zones) {
            zonesCopy = new ArrayList<>(zones);
        }
        for (Zone zone : zonesCopy) {
            if (zone != null && zone.map != null && zone.map.mapId == mapId) {
                return zone;
            }
        }
        return null;
    }

    private void finish() {
        for (int j = zones.size() - 1; j >= 0; j--) {
            Zone zone = zones.get(j);
            for (int i = zone.getPlayers().size() - 1; i >= 0; i--) {
                if (i < zone.getPlayers().size()) {
                    Player pl = zone.getPlayers().get(i);
                    kickOut(pl);
                }
            }
        }
    }

    private void kickOut(Player player) {
        if (MapService.gI().isMapMabu2H(player.zone.map.mapId) && !player.isAdmin()) {
            ChangeMapService.gI().changeMapBySpaceShip(player, player.gender + 21, -1, 336);
        }
    }

    public int getId() {
        return id;
    }

    public List<Zone> getZones() {
        return zones;
    }

    public long getLastActivityTime() {
        return lastActivityTime;
    }

}


