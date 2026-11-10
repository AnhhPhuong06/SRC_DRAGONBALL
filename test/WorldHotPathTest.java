import consts.ConstMob;
import java.lang.reflect.Field;
import java.util.*;
import models.Template.ItemTemplate;
import models.map.ItemMap;
import models.map.Zone;
import models.mob.Mob;
import models.player.Player;
import models.player.Satellite;
import network.io.Message;
import sun.misc.Unsafe;

/** No database/game startup: exercise actual mob targeting and zone item filtering. */
public final class WorldHotPathTest {
    static int checks;
    static final Field ATTACK_TIME = field("lastTimeAttackPlayer"), DELAY = field("timeAttack");
    static Field field(String name) { try { Field f = Mob.class.getDeclaredField(name); f.setAccessible(true); return f; } catch (Exception e) { throw new AssertionError(e); } }
    static void check(boolean ok, String message) { checks++; if (!ok) throw new AssertionError(message); }
    static models.map.Map map(int id) throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe"); f.setAccessible(true);
        models.map.Map m = (models.map.Map)((Unsafe)f.get(null)).allocateInstance(models.map.Map.class); m.mapId = id; return m;
    }
    static final class CountingZone extends Zone {
        int humanoidScans, normalScans;
        CountingZone() throws Exception { super(map(1), 0, 100); }
        @Override public List<Player> getHumanoids() { humanoidScans++; return super.getHumanoids(); }
        @Override public List<Player> getNotBosses() { normalScans++; return super.getNotBosses(); }
        void reset() { humanoidScans = normalScans = 0; }
    }
    static final class Victim extends Player {
        int hits;
        @Override public long injured(Player attacker, long damage, boolean piercing, boolean mobAttack) { hits++; return 0; }
        @Override public void sendMessage(Message message) {}
    }
    static Victim player(CountingZone zone, int x) {
        Victim p = new Victim(); p.id = x; p.nPoint.hp = p.nPoint.hpMax = 1000;
        p.location.x = x; p.location.y = 0; p.zone = zone; zone.addPlayer(p); return p;
    }
    static Mob mob(CountingZone zone) {
        Mob m = new Mob(); m.tempId = 20; m.point.hp = m.point.maxHp = 100; m.point.dame = 10; m.zone = zone; return m;
    }
    public static void main(String[] args) throws Exception {
        CountingZone zone = new CountingZone(); Victim near = player(zone, 10), far = player(zone, 80);
        Mob mob = mob(zone);
        check(mob.getPlayerCanAttack() == near, "Normal mob still selects the closest eligible target");
        check(zone.humanoidScans == 0 && zone.normalScans == 1, "No retaliation list: skip the entire humanoid scan");
        check(DELAY.getLong(mob) == 2000, "Normal attack keeps two-second cooldown");
        mob.temporaryEnemies.add(far); zone.reset();
        check(mob.getPlayerCanAttack() == far && zone.humanoidScans == 1 && zone.normalScans == 0, "Retaliation target has priority over a closer non-attacker");
        check(DELAY.getLong(mob) == 1000, "Retaliation keeps one-second cooldown");
        far.effectSkin.isVoHinh = true;
        check(mob.getPlayerCanAttack() == near, "Invisible retaliation target falls back to normal target"); far.effectSkin.isVoHinh = false;
        far.satellite = new Satellite(); far.satellite.isDefend = true;
        check(mob.getPlayerCanAttack() == near, "Defended retaliation target is skipped"); far.satellite = null;
        far.nPoint.hp = 0; check(mob.getPlayerCanAttack() == near, "Dead retaliation target is skipped"); far.nPoint.hp = 1000;
        far.isBoss = true; check(mob.getPlayerCanAttack() == near, "Boss retaliation target is skipped"); far.isBoss = false;
        far.location.x = 301; check(mob.getPlayerCanAttack() == near, "Retaliation range remains 300 pixels");
        far.location.x = 300; check(mob.getPlayerCanAttack() == far, "Retaliation includes boundary at 300 pixels");
        mob.temporaryEnemies.clear(); far.location.x = 10;
        check(mob.getPlayerCanAttack() == far, "Equal-distance targets preserve last-in-list tie breaking");
        near.location.x = 101; far.location.x = 101;
        check(mob.getPlayerCanAttack() == null, "Ordinary target outside 100 pixels is ignored");
        near.location.x = 100; check(mob.getPlayerCanAttack() == near, "Ordinary target includes boundary at 100 pixels");
        near.location.x = 10; far.location.x = 80;
        near.effectSkin.isVoHinh = true; check(mob.getPlayerCanAttack() == far, "Normal scan skips invisible players"); near.effectSkin.isVoHinh = false;
        near.isNewPet = true; check(mob.getPlayerCanAttack() == far, "Normal scan excludes new pets"); near.isNewPet = false;

        int[] inert = {ConstMob.MOC_NHAN, ConstMob.BU_NHIN_MA_QUAI, ConstMob.CO_MAY_HUY_DIET,
                ConstMob.MAY_DO_SUC_MANH, ConstMob.BU_NHIN_MA_QUAI_118, ConstMob.HIRUDEGARN, ConstMob.VUA_BACH_TUOC,
                ConstMob.ROBOT_BAO_VE, ConstMob.GAU_TUONG_CUOP, ConstMob.VOI_CHIN_NGA, ConstMob.GA_CHIN_CUA,
                ConstMob.NGUA_CHIN_LMAO, ConstMob.PIANO};
        for (int id : inert) { Mob m = mob(zone); m.tempId = id; assertNoScan(m, zone, "Nonstandard/dummy type " + id); }
        Mob dead = mob(zone); dead.point.hp = 0; assertNoScan(dead, zone, "Dead mob");
        for (String name : List.of("isStun", "isAnTroi", "isThoiMien", "isBlindDCTT")) {
            Mob m = mob(zone); m.effectSkill.getClass().getField(name).setBoolean(m.effectSkill, true); assertNoScan(m, zone, name);
        }
        Mob elite = mob(zone); elite.lvMob = 1; assertNoScan(elite, zone, "Elite outside dungeon");
        zone.map.mapId = 141; zone.reset(); ATTACK_TIME.setLong(elite, System.currentTimeMillis()); elite.attack();
        check(zone.normalScans == 1, "Elite in a dungeon still resolves targets"); zone.map.mapId = 1;

        // A changed target must select the correct cooldown BEFORE checking elapsed time.
        mob.temporaryEnemies.add(far); DELAY.setLong(mob, 2000);
        ATTACK_TIME.setLong(mob, System.currentTimeMillis() - 1500); int hits = far.hits; mob.attack();
        check(far.hits == hits + 1, "New retaliation target can attack after 1.5s even when previous delay was 2s");
        mob.temporaryEnemies.clear(); DELAY.setLong(mob, 1000);
        long recent = System.currentTimeMillis() - 1100; ATTACK_TIME.setLong(mob, recent); hits = near.hits; mob.attack();
        check(near.hits == hits && ATTACK_TIME.getLong(mob) == recent, "Normal attack cannot use previous one-second retaliation cooldown");
        ATTACK_TIME.setLong(mob, System.currentTimeMillis() - 2500); mob.attack();
        check(near.hits == hits + 1, "Normal attack still fires once its two-second delay expires");
        zone.getNotBosses().clear(); mob.temporaryEnemies.clear(); ATTACK_TIME.setLong(mob, 0); mob.attack();
        check(ATTACK_TIME.getLong(mob) > 0, "Eligible empty-zone tick preserves cooldown reset even without a target");

        // Filtering preserves item order/ownership and returns an independent result collection.
        Zone itemsZone = new Zone(map(1), 0, 10); Player owner = new Player(); owner.id = 42;
        ItemTemplate normal = new ItemTemplate(); normal.id = 1;
        ItemTemplate owned = new ItemTemplate(); owned.id = 726;
        ItemMap first = new ItemMap(itemsZone, normal, 1, 1, 1, -1);
        ItemMap hidden = new ItemMap(itemsZone, owned, 1, 1, 1, 999);
        ItemMap mine = new ItemMap(itemsZone, owned, 1, 1, 1, owner.id);
        ItemMap disposed = new ItemMap(itemsZone, normal, 1, 1, 1, -1); disposed.itemTemplate = null;
        itemsZone.items.add(null);
        List<ItemMap> visible = itemsZone.getItemMapsForPlayer(owner);
        check(visible.equals(List.of(mine, first)) && !visible.contains(hidden), "Visible items retain order and skip missing/disposed/other-owner drops");
        itemsZone.removeItemMap(first); check(visible.contains(first), "Result collection remains stable after zone item removal");
        visible.clear(); check(itemsZone.items.contains(mine), "Editing returned list does not mutate zone collection");
        System.out.println("PASS: " + checks + " world hotpath checks; blocked mobs perform zero player scans, normal no-retaliation targeting uses one pass instead of two.");
    }
    static void assertNoScan(Mob mob, CountingZone zone, String label) throws Exception {
        zone.reset(); mob.attack();
        check(zone.humanoidScans == 0 && zone.normalScans == 0 && ATTACK_TIME.getLong(mob) == 0, label + " skips targeting without changing attack timestamp");
    }
}
