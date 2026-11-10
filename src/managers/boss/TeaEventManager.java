package managers.boss;

 


public class TeaEventManager extends BossManager {

    private static TeaEventManager instance;

    public static TeaEventManager gI() {
        if (instance == null) {
            instance = new TeaEventManager();
        }
        return instance;
    }
}
