package services.dungeon;

import models.boss.Boss;
import models.boss.boss_list.MajinBuu14H.Mabu2H;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import models.dungeon.MajinBuu14H;
import models.map.Zone;
import models.map.MaBuHold;
import models.player.Player;
import server.Maintenance;
import services.map.MapService;
import services.map.ChangeMapService;
import utils.Functions;
import utils.Logger;
import managers.MajinBuu14HMonitor;

public class MajinBuu14HService implements Runnable {

    private static MajinBuu14HService instance;

    public static MajinBuu14HService gI() {
        if (instance == null) {
            instance = new MajinBuu14HService();
        }
        return instance;
    }

    public List<MajinBuu14H> maBu2Hs;
    private int nextId = 0;
    private Thread managerThread;
    private static final long SCALE_CHECK_INTERVAL = 30000;

    private MajinBuu14HService() {
        this.maBu2Hs = new CopyOnWriteArrayList<>();

        for (int i = 0; i < MajinBuu14H.MIN_INSTANCES; i++) {
            createAndStartInstance();
        }
        startAutoScalingManager();
    }

    private void startAutoScalingManager() {
        managerThread = new Thread(this, "MajinBuu14H-AutoScaler");
        managerThread.start();
    }

    @Override
    public void run() {
        while (!Maintenance.isRunning) {
            try {
                autoScale();
                Functions.sleep(SCALE_CHECK_INTERVAL);
            } catch (Exception e) {
                Logger.logException(MajinBuu14HService.class, e);
            }
        }
    }

    private synchronized void autoScale() {
        try {
            scaleDown();
            scaleUp();
            updateMonitoring();
        } catch (Exception e) {
            Logger.logException(MajinBuu14HService.class, e);
        }
    }

    private void updateMonitoring() {
        int totalPlayers = 0;
        for (MajinBuu14H inst : maBu2Hs) {
            totalPlayers += inst.getTotalPlayers();
        }
        MajinBuu14HMonitor.gI().updateStats(maBu2Hs.size(), totalPlayers);
    }

    private void scaleUp() {
        boolean needMore = true;

        for (MajinBuu14H inst : maBu2Hs) {
            if (!inst.isFull()) {
                needMore = false;
                break;
            }
        }
        if (needMore && maBu2Hs.size() < MajinBuu14H.MAX_INSTANCES) {
            MajinBuu14H newInst = createAndStartInstance();
            MajinBuu14HMonitor.gI().recordScaleUp(maBu2Hs.size());
        }
    }

    private void scaleDown() {
        if (maBu2Hs.size() <= MajinBuu14H.MIN_INSTANCES) {
            return;
        }
        List<MajinBuu14H> toRemove = new ArrayList<>();

        for (MajinBuu14H inst : maBu2Hs) {
            if (inst.isIdleTooLong() && maBu2Hs.size() > MajinBuu14H.MIN_INSTANCES) {
                toRemove.add(inst);
            }
        }

        for (MajinBuu14H inst : toRemove) {
            removeInstance(inst);
            MajinBuu14HMonitor.gI().recordScaleDown(maBu2Hs.size());
        }
    }

    private MajinBuu14H createAndStartInstance() {
        MajinBuu14H inst = new MajinBuu14H(nextId++);
        maBu2Hs.add(inst);
        inst.start();
        return inst;
    }

    private void removeInstance(MajinBuu14H inst) {
        inst.stop();
        maBu2Hs.remove(inst);
    }

    public void shutdown() {
        for (MajinBuu14H inst : maBu2Hs) {
            inst.stop();
        }
        if (managerThread != null) {
            managerThread.interrupt();
        }
    }

    public void addMapMaBu2H(int id, Zone zone) {
        if (zone.map.mapId == 128) {
            for (int slot = 0; slot < 4; slot++) {
                zone.maBuHolds.add(new MaBuHold(slot, null));
            }
        }
        findOrCreateInstance(id).getZones().add(zone);
    }

    private MajinBuu14H findOrCreateInstance(int id) {
        for (MajinBuu14H inst : maBu2Hs) {
            if (inst.getId() == id) {
                return inst;
            }
        }
        MajinBuu14H newInst = new MajinBuu14H(id);
        maBu2Hs.add(newInst);
        newInst.start();

        if (id >= nextId) {
            nextId = id + 1;
        }

        return newInst;
    }

    public void joinMaBu2H(Player player) {
        MajinBuu14HMonitor.gI().recordPlayerJoin();
        for (MajinBuu14H inst : this.maBu2Hs) {
            for (Zone zone : inst.getZones()) {
                if (zone.map.mapId == 127 && zone.getBosses().stream().anyMatch(b -> b != null && !b.isDie())) {
                    inst.updateActivity();
                    ChangeMapService.gI().changeMap(player, zone, -1, 312);
                    return;
                }
            }
        }

        if (maBu2Hs.size() < MajinBuu14H.MAX_INSTANCES) {
            scaleUp();
        }

        ChangeMapService.gI().changeMap(player, MapService.gI().getMapWithRandZone(127), -1, 312);
    }

    public String getStats() {
        return MajinBuu14HMonitor.gI().getCompactStats();
    }

    public String getDetailedReport() {
        return MajinBuu14HMonitor.gI().getDetailedReport();
    }

    public String getInstanceDetails() {
        return MajinBuu14HMonitor.gI().getInstanceDetails();
    }

    public void resetMetrics() {
        MajinBuu14HMonitor.gI().reset();
    }

}


