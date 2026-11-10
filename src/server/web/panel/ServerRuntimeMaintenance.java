package server.web.panel;

import java.time.*;
import java.util.concurrent.*;
import server.AutoMaintenance;
import server.Maintenance;
import static server.web.panel.PanelSupport.*;

/** Shutdown-only maintenance. The legacy automatic task would launch run.bat on close. */
final class ServerRuntimeMaintenance {
    private static boolean enabled;
    private static int hour=AutoMaintenance.hours, minute=AutoMaintenance.mins;
    private static LocalDate lastRun;
    private static ScheduledExecutorService scheduler;
    private ServerRuntimeMaintenance() {}
    static synchronized boolean enabled(){return enabled;}
    static synchronized String time(){return String.format(java.util.Locale.ROOT,"%02d:%02d",hour,minute);}
    static synchronized void configure(boolean on,int h,int m){
        WorldData.range(h,0,23,"hour");WorldData.range(m,0,59,"minute");
        if(Maintenance.isRunning || AutoMaintenance.isRunning) throw error(409,"Bảo trì đã bắt đầu; không thay đổi lịch");
        AutoMaintenance.AutoMaintenance=false;
        hour=h;minute=m;enabled=on;
        if(on && scheduler==null){
            scheduler=Executors.newSingleThreadScheduledExecutor(r->{Thread t=new Thread(r,"Panel-Shutdown-Maintenance");t.setDaemon(true);return t;});
            scheduler.scheduleWithFixedDelay(ServerRuntimeMaintenance::tick,1,1,TimeUnit.SECONDS);
        }
    }
    static synchronized int hour(){return hour;}
    static synchronized int minute(){return minute;}
    private static synchronized void tick(){
        try {
            if(!enabled || Maintenance.isRunning || AutoMaintenance.isRunning) return;
            // Own this schedule, never permit the legacy worker to request a process restart.
            AutoMaintenance.AutoMaintenance=false;
            LocalDateTime now=LocalDateTime.now();
            if(now.getHour()==hour && now.getMinute()==minute && !now.toLocalDate().equals(lastRun)){
                lastRun=now.toLocalDate();enabled=false;Maintenance.gI().startNew(60);
            }
        }catch(RuntimeException failure){enabled=false;utils.Logger.logException(ServerRuntimeMaintenance.class,failure);}
    }
    static synchronized void start(int seconds){
        WorldData.range(seconds,5,86400,"seconds");
        if(Maintenance.isRunning || AutoMaintenance.isRunning) throw error(409,"Bảo trì đã được lên lịch/đang chạy");
        enabled=false;AutoMaintenance.AutoMaintenance=false;
        // startNew avoids restarting a Thread instance which was already used.
        Maintenance.gI().startNew(seconds);
    }
}
