import consts.ConstMap;
import consts.ConstMob;
import consts.ConstPlayer;
import java.io.*;
import java.lang.reflect.*;
import models.Template.SkillTemplate;
import models.intrinsic.Intrinsic;
import models.map.Zone;
import models.mob.Mob;
import models.player.Pet;
import models.player.Player;
import models.skill.Skill;
import network.io.Message;
import server.Manager;
import services.SkillService;
import sun.misc.Unsafe;

/** Real SkillService/NPoint and attack packets; synthetic targets, no DB or sockets. */
public final class TroiCriticalTest {
    static int checks;static Zone zone;static Method hitPlayer,hitMob;
    static byte[] playerPacket,mobPacket;static long receivedDamage;
    static final int[] SKILLS={Skill.DRAGON,Skill.DEMON,Skill.GALICK,Skill.LIEN_HOAN,
            Skill.KAIOKEN,Skill.KAMEJOKO,Skill.MASENKO,Skill.ANTOMIC};
    public static void main(String[] args)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);
        var map=(models.map.Map)((Unsafe)f.get(null)).allocateInstance(models.map.Map.class);
        map.mapId=0;map.type=ConstMap.MAP_OFFLINE;Manager.MAP_BY_ID.put(0,map);zone=new Zone(map,0,20);
        hitPlayer=SkillService.class.getDeclaredMethod("playerAttackPlayer",Player.class,Player.class,boolean.class);hitPlayer.setAccessible(true);
        hitMob=SkillService.class.getDeclaredMethod("playerAttackMob",Player.class,Mob.class,boolean.class,boolean.class);hitMob.setAccessible(true);
        TestPlayer observer=new TestPlayer();observer.zone=zone;zone.addPlayer(observer);
        for(int kind=0;kind<4;kind++)for(int skill:SKILLS)for(boolean boss:new boolean[]{false,true}) {
            Player p=attacker(kind,skill);TestPlayer target=target(boss);
            // Anyone can benefit, including someone other than the player casting the rope.
            target.effectSkill.plTroi=observer;target.effectSkill.anTroi=true;
            for(int i=0;i<3;i++){playerHit(p,target,false);expect(p,true,true,base(skill)*3);}
            check(!p.nPoint.isCrit100,"Rope does not leave a one-shot buff on attacker");
            target.effectSkill.anTroi=false;playerHit(p,target,false);expect(p,true,false,base(skill));
            target.effectSkill.anTroi=true;TestPlayer other=target(false);playerHit(p,other,false);expect(p,true,false,base(skill));
            playerHit(p,target,false);expect(p,true,true,base(skill)*3);
            Mob mob=mob();mob.effectSkill.isAnTroi=true;
            for(int i=0;i<3;i++){mobHit(p,mob,false);expect(p,false,true,base(skill)*3);}
            mob.effectSkill.isAnTroi=false;mobHit(p,mob,false);expect(p,false,false,base(skill));
            check(!p.nPoint.isCrit100,"Mob rope bonus cannot leak to next attack");
            p.nPoint.crit=100;mobHit(p,mob,false);expect(p,false,true,base(skill)*3);
            target.effectSkill.anTroi=false;playerHit(p,target,false);expect(p,true,true,base(skill)*3);
            p.nPoint.crit=0;target.effectSkill.anTroi=true;playerHit(p,target,true);
            check(receivedDamage==0&&p.nPoint.isCrit,"Missed attacks still deal no damage");
        }
        Player p=attacker(0,Skill.KAIOKEN);TestPlayer target=target(false);Mob mob=mob();
        for(boolean pvp:new boolean[]{false,true}) {
            int crits=0;p.nPoint.crit=50;
            for(int i=0;i<2000;i++) {
                if(pvp)playerHit(p,target,false);else mobHit(p,mob,false);
                if(p.nPoint.isCrit)crits++;
            }
            check(crits>700&&crits<1300,"Unbound target retains 50% character critical chance: "+crits);
            System.out.println("Unbound 50% sample (pvp="+pvp+"): "+crits+"/2000");
        }
        p=attacker(0,Skill.KAIOKEN);p.playerIntrinsic.intrinsic=intrinsic(25,30);p.nPoint.hp=2000;
        target.effectSkill.anTroi=true;playerHit(p,target,false);expect(p,true,true,300000);
        p.playerIntrinsic.intrinsic=intrinsic(26,25);target.effectSkill.anTroi=false;
        playerHit(p,target,false);expect(p,true,false,125000);
        p.nPoint.isCrit100=true;mob.effectSkill.isAnTroi=true;mobHit(p,mob,false);expect(p,false,true,375000);
        check(!p.nPoint.isCrit100,"Existing single-hit critical buff is consumed normally");
        mob.effectSkill.isAnTroi=false;mobHit(p,mob,false);expect(p,false,false,125000);
        target.effectSkill=null;playerHit(p,target,false);expect(p,true,false,125000);
        mob.effectSkill=null;mobHit(p,mob,false);expect(p,false,false,125000);
        System.out.println("PASS: "+checks+" bound player/boss/mob, independent player/Pet/bot/boss attackers, damage, client flags, expiry, target switching and ordinary critical checks");
    }
    static Player attacker(int kind,int skill) {
        Player p=kind==1?new TestPet(new TestPlayer()):new TestPlayer();p.isPlayer=kind==0;p.isBot=kind==2;p.isBoss=kind==3;
        p.id=10+kind;p.zone=zone;p.playerIntrinsic.intrinsic=intrinsic(skill==Skill.KAIOKEN?26:0,25);
        p.nPoint.hp=p.nPoint.hpMax=10000;p.nPoint.dame=100000;p.nPoint.crit=0;p.nPoint.tlSDCM=50;
        Skill s=new Skill();s.template=new SkillTemplate();s.template.id=(byte)skill;s.damage=100;s.point=7;
        p.playerSkill.skillSelect=s;return p;
    }
    static TestPlayer target(boolean boss){var p=new TestPlayer();p.id=100;p.zone=zone;p.isBoss=boss;p.typePk=ConstPlayer.PK_ALL;p.nPoint.hp=p.nPoint.hpMax=100000000;return p;}
    static Mob mob(){var m=new Mob();m.id=1;m.zone=zone;m.tempId=ConstMob.MAY_DO_SUC_MANH;m.point.hp=m.point.maxHp=100000000;return m;}
    static Intrinsic intrinsic(int id,int param){var i=new Intrinsic();i.id=id;i.param1=(short)param;return i;}
    static long base(int skill){return skill==Skill.KAIOKEN?125000:100000;}
    static void playerHit(Player attacker,Player target,boolean miss)throws Exception {playerPacket=null;receivedDamage=-1;invoke(hitPlayer,attacker,target,miss);}
    static void mobHit(Player attacker,Mob target,boolean miss)throws Exception {mobPacket=null;invoke(hitMob,attacker,target,miss,false);}
    static void invoke(Method m,Object...args)throws Exception {try{m.invoke(SkillService.gI(),args);}catch(InvocationTargetException e){throw new AssertionError("Real combat route failed",e.getCause());}}
    static void expect(Player attacker,boolean pvp,boolean crit,long damage)throws Exception {
        check(attacker.nPoint.isCrit==crit,"Actual critical flag");byte[] bytes=pvp?playerPacket:mobPacket;check(bytes!=null,"Client receives combat packet");
        try(var r=new DataInputStream(new ByteArrayInputStream(bytes))) {
            long actual;boolean packetCrit;
            if(pvp){r.readInt();r.readByte();r.readByte();r.readInt();r.readByte();r.readByte();actual=r.readLong();r.readBoolean();packetCrit=r.readBoolean();check(actual==receivedDamage,"PvP packet equals damage applied to target");}
            else{r.readByte();r.readLong();actual=r.readLong();packetCrit=r.readBoolean();}
            check(packetCrit==crit,"Client critical flag matches server");
            check(actual>=damage*95/100-2&&actual<=damage*105/100+2,"Critical/noncritical damage multiplier including +50% critical damage: "+actual+" vs "+damage);
        }
    }
    static void record(Message m){if(m.command==-60)playerPacket=m.getData().clone();if(m.command==-9)mobPacket=m.getData().clone();}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static class TestPlayer extends Player {
        @Override public synchronized long injured(Player attacker,long damage,boolean piercing,boolean mob){receivedDamage=damage;return damage;}
        @Override public void sendMessage(Message m){record(m);}
    }
    static final class TestPet extends Pet {
        TestPet(Player master){super(master);}
        @Override public void sendMessage(Message m){record(m);}
    }
}
