package managers;

import models.dungeon.MajinBuu14H;
import services.dungeon.MajinBuu14HService;
import utils.Logger;

import java.util.concurrent.atomic.AtomicLong;

public class MajinBuu14HMonitor {

    private static MajinBuu14HMonitor instance;

    public static MajinBuu14HMonitor gI() {
        if (instance == null) {
            instance = new MajinBuu14HMonitor();
        }
        return instance;
    }

    private final AtomicLong scaleUpCount = new AtomicLong(0);
    private final AtomicLong scaleDownCount = new AtomicLong(0);
    private final AtomicLong totalPlayersServed = new AtomicLong(0);
    private final AtomicLong peakInstanceCount = new AtomicLong(0);
    private final AtomicLong peakPlayerCount = new AtomicLong(0);

    private long monitorStartTime;

    private MajinBuu14HMonitor() {
        this.monitorStartTime = System.currentTimeMillis();
    }

    public void reset() {
        scaleUpCount.set(0);
        scaleDownCount.set(0);
        totalPlayersServed.set(0);
        peakInstanceCount.set(0);
        peakPlayerCount.set(0);
        monitorStartTime = System.currentTimeMillis();
    }

    public void recordScaleUp(int newInstanceCount) {
        scaleUpCount.incrementAndGet();
        updatePeakInstanceCount(newInstanceCount);
    }

    public void recordScaleDown(int newInstanceCount) {
        scaleDownCount.incrementAndGet();
    }

    private void updatePeakInstanceCount(int count) {
        long current = peakInstanceCount.get();
        if (count > current) {
            peakInstanceCount.set(count);
        }
    }

    private void updatePeakPlayerCount(int count) {
        long current = peakPlayerCount.get();
        if (count > current) {
            peakPlayerCount.set(count);
        }
    }

    public void recordPlayerJoin() {
        totalPlayersServed.incrementAndGet();
    }

    public void updateStats(int currentInstances, int currentPlayers) {
        updatePeakInstanceCount(currentInstances);
        updatePeakPlayerCount(currentPlayers);
    }

    public String getDetailedReport() {
        long uptime = System.currentTimeMillis() - monitorStartTime;
        int currentInstances = MajinBuu14HService.gI().maBu2Hs.size();
        int currentPlayers = 0;
        int runningInstances = 0;

        for (MajinBuu14H inst : MajinBuu14HService.gI().maBu2Hs) {
            currentPlayers += inst.getTotalPlayers();
            if (inst.isRunning()) {
                runningInstances++;
            }
        }

        StringBuilder report = new StringBuilder();
        report.append("\nâ•”â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•—\n");
        report.append("â•‘          MAJIN BUU 14H AUTO-SCALING MONITOR REPORT          â•‘\n");
        report.append("â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£\n");

        // System Info
        report.append(String.format("â•‘ Uptime: %s                                   â•‘\n", formatUptime(uptime)));
        report.append("â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£\n");

        // Current Status
        report.append("â•‘ CURRENT STATUS                                                â•‘\n");
        report.append(String.format("â•‘   Instances: %d/%d running (Min: %d, Max: %d)              â•‘\n",
                runningInstances, currentInstances,
                MajinBuu14H.MIN_INSTANCES, MajinBuu14H.MAX_INSTANCES));
        report.append(
                String.format("â•‘   Active Players: %d                                        â•‘\n", currentPlayers));
        report.append(String.format("â•‘   Avg Players/Instance: %.1f                               â•‘\n",
                currentInstances > 0 ? (double) currentPlayers / currentInstances : 0.0));
        report.append("â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£\n");

        // Scaling Events
        report.append("â•‘ SCALING EVENTS                                                â•‘\n");
        report.append(
                String.format("â•‘   Scale UP events: %d                                      â•‘\n", scaleUpCount.get()));
        report.append(String.format("â•‘   Scale DOWN events: %d                                    â•‘\n",
                scaleDownCount.get()));
        report.append(String.format("â•‘   Total scaling operations: %d                             â•‘\n",
                scaleUpCount.get() + scaleDownCount.get()));
        report.append("â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£\n");

        // Peak Statistics
        report.append("â•‘ PEAK STATISTICS                                               â•‘\n");
        report.append(String.format("â•‘   Peak instances: %d                                       â•‘\n",
                peakInstanceCount.get()));
        report.append(String.format("â•‘   Peak players: %d                                         â•‘\n",
                peakPlayerCount.get()));
        report.append(String.format("â•‘   Total players served: %d                                 â•‘\n",
                totalPlayersServed.get()));
        report.append("â• â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•£\n");

        // Resource Efficiency
        double avgScaleFrequency = uptime > 0
                ? (double) (scaleUpCount.get() + scaleDownCount.get()) / (uptime / 3600000.0)
                : 0;
        report.append("â•‘ RESOURCE EFFICIENCY                                           â•‘\n");
        report.append(
                String.format("â•‘   Avg scaling frequency: %.2f events/hour                  â•‘\n", avgScaleFrequency));
        report.append(String.format("â•‘   Instance utilization: %.1f%%                             â•‘\n",
                currentInstances > 0
                        ? (double) currentPlayers / (currentInstances * MajinBuu14H.PLAYERS_PER_INSTANCE) * 100
                        : 0));
        report.append("â•šâ•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•â•\n");

        return report.toString();
    }

    /**
     * Get compact stats (for regular logging)
     */
    public String getCompactStats() {
        int currentInstances = MajinBuu14HService.gI().maBu2Hs.size();
        int currentPlayers = 0;

        for (MajinBuu14H inst : MajinBuu14HService.gI().maBu2Hs) {
            currentPlayers += inst.getTotalPlayers();
        }

        return String.format("[Monitor] Instances: %d | Players: %d | Scale: â†‘%d â†“%d | Peak: I=%d P=%d",
                currentInstances, currentPlayers,
                scaleUpCount.get(), scaleDownCount.get(),
                peakInstanceCount.get(), peakPlayerCount.get());
    }

    /**
     * Format uptime thÃ nh human-readable string
     */
    private String formatUptime(long millis) {
        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;

        if (days > 0) {
            return String.format("%dd %dh %dm", days, hours % 24, minutes % 60);
        } else if (hours > 0) {
            return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
        } else if (minutes > 0) {
            return String.format("%dm %ds", minutes, seconds % 60);
        } else {
            return String.format("%ds", seconds);
        }
    }

    /**
     * Get instance details
     */
    public String getInstanceDetails() {
        StringBuilder details = new StringBuilder();
        details.append("\n[Instance Details]\n");

        for (MajinBuu14H inst : MajinBuu14HService.gI().maBu2Hs) {
            long idleTime = System.currentTimeMillis() - inst.getLastActivityTime();
            details.append(String.format("  #%d: %s | Players: %d/%d | Zones: %d | Idle: %s\n",
                    inst.getId(),
                    inst.isRunning() ? "RUNNING" : "STOPPED",
                    inst.getTotalPlayers(),
                    MajinBuu14H.PLAYERS_PER_INSTANCE,
                    inst.getZones().size(),
                    formatUptime(idleTime)));
        }

        return details.toString();
    }

    // Getters cho metrics
    public long getScaleUpCount() {
        return scaleUpCount.get();
    }

    public long getScaleDownCount() {
        return scaleDownCount.get();
    }

    public long getTotalPlayersServed() {
        return totalPlayersServed.get();
    }

    public long getPeakInstanceCount() {
        return peakInstanceCount.get();
    }

    public long getPeakPlayerCount() {
        return peakPlayerCount.get();
    }
}

