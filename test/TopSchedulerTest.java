package server.web.panel;

import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.*;
import services.top.*;
import sun.misc.Unsafe;

public final class TopSchedulerTest {
    public static void main(String[] args)throws Exception {
        Field uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);Unsafe unsafe=(Unsafe)uf.get(null);
        var top=(TOPAUTO)unsafe.allocateInstance(TOPAUTO.class);top.template=new TopManager.TopTemplate(0,"Fixture top","SELECT id FROM player");
        top.date=Timestamp.valueOf("2040-01-01 00:00:00");top.items=new HashMap<>();top.users=new LinkedHashMap<>();top.isAuto=false;
        TopManager.AUTO_GIFT_TOPS.add(top);
        Field timers=TopAutoService.class.getDeclaredField("timers");timers.setAccessible(true);
        try {
            TopAutoService.gI().activeAuto();List<Timer> first=new ArrayList<>((List<Timer>)timers.get(TopAutoService.gI()));
            top.date=Timestamp.valueOf("2041-01-01 00:00:00");TopAutoService.gI().activeAuto();
            List<Timer> current=(List<Timer>)timers.get(TopAutoService.gI());
            if(first.size()!=1||current.size()!=1||first.get(0)==current.get(0))throw new AssertionError("Reschedule did not replace timers");
            try{first.get(0).schedule(new TimerTask(){public void run(){}},100000);throw new AssertionError("Previous timer still accepts tasks");}
            catch(IllegalStateException cancelled){}
            TopAutoService.gI().activeAuto();if(((List<?>)timers.get(TopAutoService.gI())).size()!=1)throw new AssertionError("Repeated schedule duplicated timers");
            System.out.println("PASS: edited schedules replace/cancel old timers and repeated setup does not duplicate award jobs; no award executed");
        } finally {for(Timer timer:(List<Timer>)timers.get(TopAutoService.gI()))timer.cancel();TopManager.AUTO_GIFT_TOPS.remove(top);}
    }
}
