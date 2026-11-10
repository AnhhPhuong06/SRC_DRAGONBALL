import consts.AppearType;
import consts.BossStatus;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import models.boss.*;
import models.map.Map;
import models.map.Zone;
import models.player.NPoint;
import models.player.Player;
import sun.misc.Unsafe;

/** Full template lifecycle fixtures, without constructing game bosses or accessing the DB. */
public final class BossRespawnAuditTest {
    static Unsafe unsafe;
    static int checks, stages, chains;
    public static void main(String[] args) {
        try {
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);unsafe=(Unsafe)f.get(null);
            checkJoinFailures(); checkDeathCallbacks();
            for(Field field:BossesData.class.getFields())if(field.getType()==BossData.class) {
                BossData data=(BossData)field.get(null);Fixture boss=fixture(Fixture.class,data);
                boss.currentLevel=0;boss.zone=map().zones.get(1);boss.zone.addPlayer(boss);
                boss.nPoint.hp=0;boss.changeStatus(BossStatus.LEAVE_MAP);boss.update();
                check(boss.bossStatus==BossStatus.REST&&boss.zone==null,"Final death leaves map: "+field.getName());
                boolean automatic=data.getTypeAppear()==AppearType.DEFAULT_APPEAR;
                if(data.getSecondsRest()>0||!automatic) {
                    boss.rest();check(boss.bossStatus==BossStatus.REST,"Respects cooldown or external trigger: "+field.getName());
                }
                boss.lastTimeRest=System.currentTimeMillis()-(data.getSecondsRest()+1L)*1000L;
                boss.rest();
                check(boss.bossStatus==(automatic?BossStatus.RESPAWN:BossStatus.REST),"Correct automatic/dependent appearance: "+field.getName());
                if(!automatic)boss.changeStatus(BossStatus.RESPAWN); // Simulate its existing parent/event trigger.
                boss.destination=map().zones.get(1);boss.update();allowRetry(boss);boss.update();
                check(boss.currentLevel==0&&boss.bossStatus==BossStatus.CHAT_S&&!boss.isDie()
                        &&boss.zone.getBosses().contains(boss),"Appears alive when authorized: "+field.getName());
                stages++;
            }
            for(String line:Files.readAllLines(Path.of(args[0]))) {
                if(line.isBlank())continue;
                var names=line.split(",");BossData[] data=new BossData[names.length];
                for(int i=0;i<names.length;i++)data[i]=(BossData)BossesData.class.getField(names[i]).get(null);
                checkChain(data);chains++;
            }
            System.out.println("PASS: "+checks+" boss lifecycle checks across "+stages+" stages, "+chains
                    +" multi-form chains; missing/full/error map retries, death callbacks and dependent spawn rules");
            System.exit(0); // Real map transport starts a trade-cleanup thread.
        } catch(Throwable error){error.printStackTrace();System.exit(1);}
    }

    static void checkJoinFailures()throws Exception {
        Fixture boss=fixture(Fixture.class,BossesData.FIDE_DAI_CA_1,BossesData.FIDE_DAI_CA_2,BossesData.FIDE_DAI_CA_3);
        boss.currentLevel=0;boss.bossStatus=BossStatus.JOIN_MAP;boss.joinMap();
        check(boss.bossStatus==BossStatus.JOIN_MAP&&boss.currentLevel==0,"Missing map must retry the same form, never RESPAWN");
        for(int i=0;i<10;i++){allowRetry(boss);boss.update();check(boss.currentLevel==0&&boss.bossStatus==BossStatus.JOIN_MAP,"Missing map cannot cycle forms or strand REST");}
        FixtureMap map=map();boss.destination=map.zones.get(1);
        for(Zone zone:map.zones){Player other=new Player();other.isBoss=true;zone.addPlayer(other);}
        allowRetry(boss);boss.joinMap();
        check(boss.bossStatus==BossStatus.JOIN_MAP&&boss.zone==null,"Full map stays pending without advancing level");
        map.zones.get(1).getBosses().clear();
        allowRetry(boss);boss.joinMap();
        check(boss.bossStatus==BossStatus.CHAT_S&&boss.zone==map.zones.get(1),"Search wraps around to the only free zone");
        // All random starting positions must still find an earlier free zone.
        for(int i=0;i<30;i++){
            Fixture next=fixture(Fixture.class,BossesData.FIDE_DAI_CA_1);next.currentLevel=0;next.bossStatus=BossStatus.JOIN_MAP;
            FixtureMap crowded=map();Player other=new Player();other.isBoss=true;crowded.zones.get(2).addPlayer(other);
            next.destination=crowded.zones.get(2);next.joinMap();
            check(next.bossStatus==BossStatus.CHAT_S&&next.zone==crowded.zones.get(1),"Never falsely reports full when an earlier zone is free");
        }
        for(int level=0;level<3;level++){
            boss=fixture(Fixture.class,BossesData.FIDE_DAI_CA_1,BossesData.FIDE_DAI_CA_2,BossesData.FIDE_DAI_CA_3);
            boss.currentLevel=level;boss.bossStatus=BossStatus.JOIN_MAP;map=map();map.failAdmission=true;boss.destination=map.zones.get(1);
            boss.joinMap();check(boss.bossStatus==BossStatus.JOIN_MAP&&boss.currentLevel==level,"Transient map error retains current form");
            map.failAdmission=false;allowRetry(boss);boss.joinMap();
            check(boss.bossStatus==BossStatus.CHAT_S&&boss.zone.getBosses().contains(boss),"Recover from admission failure on every form");
        }
        boss=fixture(Fixture.class,BossesData.BE_NA);boss.currentLevel=0;boss.bossStatus=BossStatus.JOIN_MAP;
        map=map();boss.destination=map.zones.get(1);boss.failNotify=true;boss.joinMap();
        Zone admitted=boss.zone;check(boss.bossStatus==BossStatus.JOIN_MAP&&admitted.getBosses().contains(boss),"Failure after admission remains retryable");
        boss.failNotify=false;allowRetry(boss);boss.joinMap();
        int appearances=0;for(Zone z:map.zones)if(z.getBosses().contains(boss))appearances++;
        check(boss.zone==admitted&&boss.bossStatus==BossStatus.CHAT_S&&appearances==1,"Retry after partial admission cannot orphan a duplicate in another zone");
        Fixture parent=fixture(Fixture.class,BossesData.BE_NA);parent.zone=map().zones.get(1);
        boss=fixture(Fixture.class,BossesData.PIC);boss.currentLevel=0;boss.parent(parent);boss.bossStatus=BossStatus.JOIN_MAP;
        parent.zone=null;boss.joinMap();check(boss.bossStatus==BossStatus.JOIN_MAP,"Dependent boss waits for missing parent zone instead of REST");
        parent.zone=map().zones.get(1);allowRetry(boss);boss.joinMap();
        check(boss.bossStatus==BossStatus.CHAT_S&&boss.zone==parent.zone,"Dependent joins once parent is available");
    }

    static void checkDeathCallbacks()throws Exception {
        Fixture boss=fixture(Fixture.class,BossesData.BE_NA);boss.currentLevel=0;boss.failDeathCallback=true;
        boss.nPoint.hp=0;boss.bossStatus=BossStatus.CHAT_E;boss.update();
        check(boss.bossStatus==BossStatus.LEAVE_MAP,"Throwing death callback cannot strand CHAT_E");
        boss.update();check(boss.bossStatus==BossStatus.REST,"Callback error still completes leave and cooldown");
        List<String> children=new ArrayList<>();
        for(int i=1;i<=7;i++)children.add("Cell.XENCON"+i);
        for(String group:List.of("GinyuForce","NamekGinyuForce"))for(int i=1;i<=4;i++)children.add(group+".SO"+i+(group.startsWith("Namek")?"_NM":""));
        children.addAll(List.of("Android.Poc","Android.Pic","Rocket.Musashi","Rocket.Kochiro"));
        for(String name:children){
            Class<? extends Boss> type=Class.forName("models.boss.boss_list."+name).asSubclass(Boss.class);
            Boss child=fixture(type,BossesData.XEN_CON_1);child.currentLevel=0;child.zone=map().zones.get(1);child.zone.addPlayer(child);
            Fixture parent=fixture(Fixture.class,BossesData.BE_NA);parent.currentLevel=1;parent.bossAppearTogether=new Boss[1][];
            Field owner=Boss.class.getDeclaredField("parentBoss");owner.setAccessible(true);owner.set(child,parent);
            child.nPoint.hp=0;child.changeStatus(BossStatus.CHAT_E);
            Field chat=Boss.class.getDeclaredField("lastTimeStartChatE");chat.setAccessible(true);chat.setLong(child,System.currentTimeMillis()-5000);
            child.update();check(child.bossStatus==BossStatus.LEAVE_MAP,"Departed parent cannot strand child: "+name);
            child.update();check(child.bossStatus==BossStatus.REST&&child.zone==null,"Dead child removed and waits for next call: "+name);
        }
        Boss bojack=fixture(models.boss.boss_list.Earth.BOJACK.class,BossesData.BOJACK,BossesData.SUPER_BOJACK);
        bojack.currentLevel=1;bojack.nPoint.hp=0;bojack.effectSkill.isStun=true;bojack.bossStatus=BossStatus.ACTIVE;
        bojack.update();check(bojack.bossStatus==BossStatus.CHAT_E,"Bojack death is not blocked by a remaining control effect");
    }

    static void checkChain(BossData[] data)throws Exception{
        Fixture boss=fixture(Fixture.class,data);boss.destination=map().zones.get(1);boss.bossStatus=BossStatus.RESPAWN;
        boss.update();allowRetry(boss);boss.update();
        for(int cycle=0;cycle<3;cycle++)for(int level=0;level<data.length;level++){
            boss.update();check(boss.currentLevel==level&&boss.bossStatus==BossStatus.ACTIVE,"Correct active form in multi-form chain");
            Zone previous=boss.zone;boss.injured(null,boss.nPoint.hpMax,true,false);
            check(boss.bossStatus==BossStatus.DIE,"Lethal hit enters death flow");
            boss.update();boss.update();boss.update();
            if(level<data.length-1){
                check(boss.bossStatus==BossStatus.RESPAWN&&boss.zone==previous,"Intermediate death transforms in same zone");
                boss.update();check(boss.currentLevel==level+1,"Advance one form per death");
            }else{
                check(boss.bossStatus==BossStatus.REST&&boss.zone==null,"Final death leaves map for cooldown");
                boss.lastTimeRest=System.currentTimeMillis()-(data[0].getSecondsRest()+1L)*1000L;
                boss.rest();check(boss.bossStatus==BossStatus.RESPAWN,"Multi-form leader respawns after configured cooldown");
                boss.update();check(boss.currentLevel==0,"Restart chain at first form");
            }
            allowRetry(boss);boss.update();check(boss.bossStatus==BossStatus.CHAT_S&&!boss.isDie(),"Reappearance after death is alive");
        }
    }

    static <T extends Boss>T fixture(Class<T> type,BossData... data)throws Exception{
        T boss=(T)unsafe.allocateInstance(type);Player base=new Player();
        for(Field field:Player.class.getDeclaredFields())if(!Modifier.isStatic(field.getModifiers())){field.setAccessible(true);field.set(boss,field.get(base));}
        Field field=Boss.class.getDeclaredField("data");field.setAccessible(true);field.set(boss,data);
        boss.currentLevel=-1;boss.isBoss=true;boss.beforeDispose=true;boss.name="Fixture";boss.id=-123;
        boss.nPoint=new NPoint(boss);boss.nPoint.hp=boss.nPoint.hpMax=100;boss.nPoint.mpg=10;boss.bossStatus=BossStatus.REST;
        return boss;
    }
    static void allowRetry(Boss boss)throws Exception{
        try{Field field=Boss.class.getDeclaredField("lastTimeJoinAttempt");field.setAccessible(true);field.setLong(boss,0);}catch(NoSuchFieldException oldJar){}
    }
    static FixtureMap map()throws Exception{
        FixtureMap map=(FixtureMap)unsafe.allocateInstance(FixtureMap.class);map.mapId=100;map.mapWidth=1000;map.mapHeight=480;
        map.mapName="Fixture";map.zones=new ArrayList<>();for(int i=0;i<3;i++)map.zones.add(new FixtureZone(map,i));return map;
    }
    static void check(boolean condition,String message){checks++;if(!condition)throw new AssertionError(message);}
    public static class Fixture extends Boss{
        Zone destination;boolean failDeathCallback,failNotify;
        Fixture()throws Exception{super(-123,BossesData.BE_NA);}
        void parent(Boss parent){parentBoss=parent;}
        @Override public Zone getMapJoin(){return destination;}
        @Override public void initBase(){nPoint.hp=nPoint.hpMax=100+currentLevel;nPoint.mpg=10;resetBase();}
        @Override public void changeToTypePK(){typePk=5;}
        @Override public void changeToTypeNonPK(){typePk=0;}
        @Override protected void setDie(Player killer){nPoint.hp=0;}
        @Override protected void notifyJoinMap(){if(failNotify)throw new IllegalStateException("Fixture notification error");}
        @Override protected void giuTrongMap(){}
        @Override public boolean chatS(){return true;}
        @Override public boolean chatE(){return true;}
        @Override public void active(){}
        @Override public void doneChatE(){if(failDeathCallback)throw new IllegalStateException("Fixture callback error");}
    }
    public static class FixtureMap extends Map{
        boolean failAdmission;
        FixtureMap(){super(100,"Fixture",(byte)0,(byte)0,(byte)0,(byte)0,(byte)0,null,null,0,20,null,null);}
        @Override public int yPhysicInTop(int x,int y){return 288;}
    }
    static class FixtureZone extends Zone{
        FixtureZone(FixtureMap map,int id){super(map,id,20);}
        @Override public void addPlayer(Player player){if(((FixtureMap)map).failAdmission)throw new IllegalStateException("Fixture map error");super.addPlayer(player);}
    }
}
