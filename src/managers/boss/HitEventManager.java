package managers.boss;




public class HitEventManager extends BossManager {

    private static HitEventManager instance;

    public static HitEventManager gI() {
        if (instance == null) {
            instance = new HitEventManager();
        }
        return instance;
    }
}
