package Bot;

import java.util.Random;
import models.mob.Mob;
import services.SkillService;
import services.player.PlayerService;
import utils.Util;

public class BotCold1 {
     private Mob mAttack;
     
     public long lastTimeChanM;
     
     public Bot botcold;
     
     
     public BotCold1(Bot b){
         this.botcold = b;
     }
     
     public void update(){
        this.Attack();
        this.chanGeMap();
     }
     
     public void GetMobAttack(){
         if(this.botcold.zone.mobs.size() >= 1){
            if(this.mAttack == null || this.mAttack.isDie()){
            mAttack = this.botcold.zone.mobs.get(new Random().nextInt(this.botcold.zone.mobs.size()));
         }
      }
    }
     
     
     public void Attack(){
         this.GetMobAttack();
             if(Util.isTrue(50 , 100)){
                this.botcold.playerSkill.skillSelect = this.botcold.playerSkill.skills.get(0);
             } else {
                this.botcold.playerSkill.skillSelect = this.botcold.playerSkill.skills.get(1);
             }
       if(this.mAttack != null){
           if(this.botcold.UseLastTimeSkill()){
               PlayerService.gI().playerMove(this.botcold, this.mAttack.location.x, this.mAttack.location.y);
               SkillService.gI().useSkill(this.botcold, null, this.mAttack, -1,null);
           }
      }
    }
    
        
   public void chanGeMap(){
       if(this.lastTimeChanM < ((System.currentTimeMillis() - 150000) -  new Random().nextInt(150000))){
           this.botcold.joinMapCold();
       }
   }
    
}