import models.Template.SkillTemplate;
import models.intrinsic.Intrinsic;
import models.player.Player;
import models.skill.Skill;

/** Exercise the actual combat calculation without a server, database or live player. */
public final class KaiokenIntrinsicTest {
    static int checks;
    public static void main(String[] args) {
        for(boolean mob:new boolean[]{false,true})for(int bonus:new int[]{5,15,25})for(int critical:new int[]{0,100}) {
            Player p=player(26,bonus,critical);
            p.nPoint.tlSDCM=35;
            long expected=150000L*(100+bonus)/100;
            if(critical==100)expected=expected*2*135/100;
            for(int hit=0;hit<30;hit++)damage(p,mob,expected,critical==100,"Kaioken applies actual intrinsic #26 and character critical chance");
        }
        for(boolean mob:new boolean[]{false,true}) {
            Player p=player(26,25,50);int crits=0;
            for(int hit=0;hit<2000;hit++) {
                int d=p.nPoint.getDameAttack(mob);if(p.nPoint.isCrit)crits++;
                range(d,p.nPoint.isCrit?375000:187500,"Mixed critical hits receive intrinsic damage before critical multiplier");
            }
            check(crits>700&&crits<1300,"50% character critical chance neither suppressed nor forced: "+crits+"/2000");
            System.out.println("Kaioken 50% critical sample (mob="+mob+"): "+crits+"/2000");
        }
        Player p=player(25,30,0);p.nPoint.hp=2000;
        damage(p,false,300000,true,"Low-HP intrinsic is available on first hit after load");
        p.playerIntrinsic.intrinsic=intrinsic(26,25);
        damage(p,false,187500,false,"Changing to Kaioken intrinsic immediately uses character chance, not cached low-HP guaranteed critical");
        p.playerIntrinsic.intrinsic=intrinsic(25,30);
        damage(p,false,300000,true,"Changing away from Kaioken activates current intrinsic immediately");
        p.nPoint.hp=9000;damage(p,false,150000,false,"Low-HP intrinsic stops above threshold");
        p.playerIntrinsic.intrinsic=intrinsic(26,5);
        damage(p,false,157500,false,"Current rolled Kaioken bonus is used immediately");
        p.playerIntrinsic.intrinsic=intrinsic(26,25);
        damage(p,false,187500,false,"New Kaioken bonus does not require an extra hit");

        p=player(26,25,0);p.nPoint.isCrit100=true;
        damage(p,false,375000,true,"One-shot guaranteed critical effect works with Kaioken intrinsic");
        check(!p.nPoint.isCrit100,"Guaranteed critical flag consumed once");
        damage(p,false,187500,false,"Next Kaioken hit returns to character critical chance");
        p.nPoint.isCritTele=true;
        damage(p,false,375000,true,"Teleport follow-up critical still works");
        check(!p.nPoint.isCritTele,"Teleport follow-up flag consumed once");
        damage(p,false,187500,false,"Teleport follow-up does not make all Kaioken hits critical");

        for(int skill:new int[]{Skill.DRAGON,Skill.KAMEJOKO,Skill.GALICK,Skill.DEMON,Skill.MASENKO,Skill.ANTOMIC,Skill.LIEN_HOAN}) {
            p=player(26,25,0);p.playerSkill.skillSelect.template.id=(byte)skill;
            damage(p,false,150000,false,"Kaioken intrinsic does not buff another attack skill");
            p.nPoint.crit=100;damage(p,false,300000,true,"Other skills retain character critical chance");
        }
        p=player(0,0,100);damage(p,false,300000,true,"Kaioken without damage intrinsic still crits normally");
        p=player(26,25,100);p.setClothes.kaioken=5;
        damage(p,false,750000,true,"Kaioken set damage bonus still combines with intrinsic and critical");
        p=player(26,25,100);p.setClothes.thanVuTruKaio=5;
        damage(p,false,487500,true,"Universe Kaio set bonus still combines with intrinsic and critical");
        System.out.println("PASS: "+checks+" Kaioken intrinsic damage, 0/50/100% critical, current intrinsic, critical buffs and other skill regression checks");
    }
    static Player player(int intrinsic,int bonus,int crit) {
        Player p=new Player();p.isPlayer=true;p.playerIntrinsic.intrinsic=intrinsic(intrinsic,bonus);
        p.nPoint.dame=100000;p.nPoint.crit=crit;p.nPoint.hpMax=10000;p.nPoint.hp=9000;
        Skill s=new Skill();s.template=new SkillTemplate();s.template.id=Skill.KAIOKEN;s.damage=150;
        p.playerSkill.skillSelect=s;return p;
    }
    static Intrinsic intrinsic(int id,int param){Intrinsic i=new Intrinsic();i.id=id;i.param1=(short)param;return i;}
    static void damage(Player p,boolean mob,long expected,boolean crit,String message){
        int actual=p.nPoint.getDameAttack(mob);check(p.nPoint.isCrit==crit,message+": critical flag");range(actual,expected,message+": damage");
    }
    static void range(long actual,long expected,String message){check(actual>=expected*95/100-2&&actual<=expected*105/100+2,message+" (got "+actual+", expected about "+expected+")");}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
