package managers.boss;

 


public class HungVuongEventManager extends BossManager {

    private static HungVuongEventManager instance;

    public static HungVuongEventManager gI() {
        if (instance == null) {
            instance = new HungVuongEventManager();
        }
        return instance;
    }

    public void removeAllBossesFromMap() {
        for (models.boss.Boss b : new java.util.ArrayList<>(this.bosses)) {
            if (b != null && b.bossStatus != consts.BossStatus.REST) {
                try {
                    b.leaveMap();
                } catch (Exception ignored) {
                }
            }
        }
    }

}
