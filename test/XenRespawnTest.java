import consts.BossID;
import consts.BossStatus;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import models.boss.Boss;
import models.boss.BossData;
import models.boss.BossesData;
import models.boss.boss_list.Cell.XenBoHung;
import models.map.Map;
import models.map.Zone;
import models.player.NPoint;
import models.player.Player;
import sun.misc.Unsafe;

/** In-memory lifecycle fixtures: no database, game threads or real player rewards. */
public final class XenRespawnTest {
    private static final Unsafe U;
    private static int checks;
    static {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            U = (Unsafe) field.get(null);
        } catch (Exception e) { throw new ExceptionInInitializerError(e); }
    }

    public static void main(String[] args) throws Exception {
        try {
            runChecks();
            System.exit(0); // Real map transport starts the trade cleanup thread.
        } catch (Throwable error) {
            error.printStackTrace();
            System.exit(1);
        }
    }

    private static void runChecks() throws Exception {
        Fixture boss = fixture();
        boss.currentLevel = 0;
        boss.bossStatus = BossStatus.JOIN_MAP;
        boss.joinMap();
        check(boss.bossStatus == BossStatus.JOIN_MAP, "Missing map retries joining, without advancing form");
        for (int i = 0; i < 20; i++) {
            allowRetry(boss);
            boss.update();
            check(boss.currentLevel == 0 && boss.bossStatus == BossStatus.JOIN_MAP,
                    "Repeated missing map cannot cycle forms or strand REST");
        }
        check(boss.initializations == 0, "Join retries do not initialize/roll a new form");

        FixtureMap map = map();
        boss.destination = map.zones.get(1);
        for (Zone zone : map.zones) {
            Player other = new Player(); other.isBoss = true; other.id = 123;
            zone.addPlayer(other);
        }
        allowRetry(boss);
        boss.joinMap();
        check(boss.currentLevel == 0 && boss.bossStatus == BossStatus.JOIN_MAP,
                "All boss zones occupied does not enter unrecoverable REST");
        for (Zone zone : map.zones) zone.getBosses().clear();
        allowRetry(boss);
        boss.joinMap();
        check(boss.bossStatus == BossStatus.CHAT_S && boss.zone.getBosses().contains(boss),
                "A zone becoming free allows actual map entry");
        check(boss.currentLevel == 0, "Retry did not skip Xen 1");

        // Exercise actual injured -> DIE -> CHAT_E -> LEAVE_MAP -> RESPAWN -> JOIN_MAP.
        for (int cycle = 0; cycle < 3; cycle++) {
            for (int level = 0; level < 3; level++) {
                boss.update();
                check(boss.currentLevel == level && boss.bossStatus == BossStatus.ACTIVE,
                        "Correct form activates in cycle " + cycle);
                Zone oldZone = boss.zone;
                boss.injured(null, boss.nPoint.hpMax, true, false);
                check(boss.bossStatus == BossStatus.DIE && boss.isDie(), "Lethal hit enters DIE");
                boss.update(); boss.update(); boss.update();
                if (level < 2) {
                    check(boss.bossStatus == BossStatus.RESPAWN, "Intermediate death advances form");
                    check(boss.zone == oldZone, "Transformation stays in same zone");
                    boss.update();
                    check(boss.currentLevel == level + 1 && boss.bossStatus == BossStatus.JOIN_MAP,
                            "Exactly one form advancement per death");
                    allowRetry(boss);
                    boss.update();
                    check(boss.zone == oldZone && boss.bossStatus == BossStatus.CHAT_S,
                            "Transformed boss appears in its original zone");
                } else {
                    check(boss.bossStatus == BossStatus.REST && boss.zone == null,
                            "Final death leaves map and rests");
                    check(!oldZone.getBosses().contains(boss), "Final corpse removed from zone");
                    long restStarted = boss.lastTimeRest;
                    boss.update();
                    check(boss.bossStatus == BossStatus.REST && boss.lastTimeRest == restStarted,
                            "Final form honors normal configured cooldown");
                    boss.lastTimeRest = System.currentTimeMillis()
                            - (BossesData.XEN_BO_HUNG_1.getSecondsRest() + 1L) * 1000L;
                    boss.update();
                    check(boss.bossStatus == BossStatus.RESPAWN, "Cooldown expiration schedules respawn");
                    boss.update();
                    check(boss.currentLevel == 0 && boss.bossStatus == BossStatus.JOIN_MAP,
                            "Next cycle restarts at Xen 1");
                    allowRetry(boss); boss.update();
                    check(boss.bossStatus == BossStatus.CHAT_S && !boss.isDie(),
                            "Boss reappears alive after final death");
                }
            }
        }
        check(boss.deaths == 9, "One death transition per lethal hit over three full cycles");

        for (int level = 0; level < 2; level++) {
            boss = fixture(); boss.currentLevel = level; boss.bossStatus = BossStatus.REST;
            boss.lastTimeRest = System.currentTimeMillis(); boss.rest();
            check(boss.bossStatus == BossStatus.JOIN_MAP && boss.currentLevel == level,
                    "Recover existing stranded REST on intermediate form");
        }
        for (int level = 0; level < 3; level++) {
            boss = fixture(); boss.currentLevel = level; boss.bossStatus = BossStatus.JOIN_MAP;
            map = map(); map.failAdmission = true; boss.destination = map.zones.get(1);
            boss.joinMap();
            check(boss.bossStatus == BossStatus.JOIN_MAP && boss.currentLevel == level,
                    "Map entry exception retries same form, including final form");
            map.failAdmission = false; allowRetry(boss); boss.joinMap();
            check(boss.bossStatus == BossStatus.CHAT_S && boss.zone.getBosses().contains(boss),
                    "Boss appears after transient map error resolves");
        }
        boss = fixture(); boss.rest();
        check(boss.bossStatus == BossStatus.RESPAWN && boss.currentLevel == -1,
                "Initial startup still uses normal first spawn");
        System.out.println("PASS: " + checks + " Xen map retry, transformations and repeated respawn checks");
    }

    private static Fixture fixture() throws Exception {
        Fixture boss = (Fixture) U.allocateInstance(Fixture.class);
        Player player = new Player();
        for (Field field : Player.class.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers())) continue;
            field.setAccessible(true); field.set(boss, field.get(player));
        }
        Field data = Boss.class.getDeclaredField("data"); data.setAccessible(true);
        data.set(boss, new BossData[]{BossesData.XEN_BO_HUNG_1, BossesData.XEN_BO_HUNG_2, BossesData.XEN_BO_HUNG_3});
        boss.currentLevel = -1; boss.id = BossID.XEN_BO_HUNG; boss.isBoss = true;
        boss.beforeDispose = true; // Avoid unrelated player tick services in this isolated fixture.
        boss.bossStatus = BossStatus.REST; boss.nPoint = new NPoint(boss);
        boss.nPoint.hp = boss.nPoint.hpMax = 100; boss.nPoint.mpg = 10;
        return boss;
    }

    private static FixtureMap map() throws Exception {
        FixtureMap map = (FixtureMap) U.allocateInstance(FixtureMap.class);
        map.mapId = 100; map.mapName = "Fixture Ginder"; map.mapWidth = 1000; map.mapHeight = 480;
        map.zones = new ArrayList<>();
        for (int i = 0; i < 3; i++) map.zones.add(new FixtureZone(map, i));
        return map;
    }

    private static void allowRetry(Fixture boss) throws Exception {
        // Old JAR has no retry timestamp: permit the regression test to run against it too.
        try {
            Field field = Boss.class.getDeclaredField("lastTimeJoinAttempt");
            field.setAccessible(true); field.setLong(boss, 0);
        } catch (NoSuchFieldException oldJar) { }
    }

    private static void check(boolean condition, String message) {
        checks++; if (!condition) throw new AssertionError(message);
    }

    public static class Fixture extends XenBoHung {
        Zone destination;
        int initializations, deaths;
        Fixture() throws Exception { super(); }
        @Override public Zone getMapJoin() { return destination; }
        @Override public void initBase() {
            initializations++; nPoint.hp = nPoint.hpMax = 100 + currentLevel;
            nPoint.mpg = 10; resetBase();
        }
        @Override protected void setDie(Player killer) { nPoint.hp = 0; }
        @Override public void die(Player killer) { deaths++; super.die(null); }
        @Override public void changeToTypePK() { typePk = 5; }
        @Override public void changeToTypeNonPK() { typePk = 0; }
        @Override protected void giuTrongMap() { }
        @Override protected void notifyJoinMap() { }
        @Override public boolean chatS() { return true; }
        @Override public boolean chatE() { return true; }
        @Override public void active() { }
    }

    public static class FixtureMap extends Map {
        boolean failAdmission;
        FixtureMap() { super(100, "Fixture", (byte)0, (byte)0, (byte)0, (byte)0,
                (byte)0, null, null, 0, 20, null, null); }
        @Override public int yPhysicInTop(int x, int y) {
            return 288;
        }
    }

    private static class FixtureZone extends Zone {
        FixtureZone(FixtureMap map, int id) { super(map, id, 20); }
        @Override public void addPlayer(Player player) {
            if (((FixtureMap) map).failAdmission)
                throw new IllegalStateException("Fixture map temporarily unavailable");
            super.addPlayer(player);
        }
    }
}
