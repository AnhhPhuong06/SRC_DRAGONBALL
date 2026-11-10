package event;

/*
 * @Zalo : 0372.665.345
 */


import event.event_list.TopUp;
import event.event_list.TrungThu;
import event.event_list.HungVuong;
import event.event_list.Christmas;
import event.event_list.Halloween;
import event.event_list.LunarNewYear;
import event.event_list.Default;
import event.event_list.InternationalWomensDay;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import managers.boss.BossManager;
import utils.Logger;

public class EventManager {

    private static EventManager instance;

    public static boolean LUNNAR_NEW_YEAR = true;

    public static boolean INTERNATIONAL_WOMANS_DAY = true;

    public static boolean CHRISTMAS = true;

    public static volatile boolean HALLOWEEN = true;

    public static boolean HUNG_VUONG = true;

    public static boolean TRUNG_THU = true;

    public static boolean TOP_UP = true;

    public static EventManager gI() {
        if (instance == null) {
            instance = new EventManager();
        }
        return instance;
    }

    public void init() {
        loadHalloweenConfig();
        new Default().init();
        if (LUNNAR_NEW_YEAR) {
            new LunarNewYear().init();
        }
        if (INTERNATIONAL_WOMANS_DAY) {
            new InternationalWomensDay().init();
        }
        if (HALLOWEEN) {
            new Halloween().init();
        }
        if (CHRISTMAS) {
            new Christmas().init();
        }
        if (HUNG_VUONG) {
            new HungVuong().init();
        }
        if (TRUNG_THU) {
            new TrungThu().init();
        }
        if (TOP_UP) {
            new TopUp().init();
        }
    }

    public static void loadHalloweenConfig() {
        Path path = Path.of("active_event.txt");
        boolean enabled = false;
        try {
            if (Files.exists(path)) {
                String line = Files.readString(path).lines().findFirst().orElse("");
                for (String id : line.split("-")) {
                    if (id.trim().equals("1")) enabled = true;
                }
            }
            HALLOWEEN = enabled;
        } catch (IOException e) {
            HALLOWEEN = false;
            Logger.error("Không đọc được cấu hình Halloween trong active_event.txt.\n");
        }
    }

    public static synchronized void setHalloweenEnabled(boolean enabled) {
        boolean changed = HALLOWEEN != enabled;
        HALLOWEEN = enabled;
        if (!enabled) {
            BossManager.gI().removeHalloweenBosses();
        } else if (changed) {
            new Halloween().init();
        }
    }
}
