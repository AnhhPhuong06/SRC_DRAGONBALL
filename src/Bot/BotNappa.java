package Bot;

import java.util.Random;
import models.mob.Mob;
import services.SkillService;
import services.player.PlayerService;
import utils.Util;

public class BotNappa {
     private Mob mAttack;
     
     public long lastTimeChanM;
     
     public Bot botnappa;
     
     
     public BotNappa(Bot b){
         this.botnappa = b;
     }
     
     public void update(){
        this.Attack();
        this.chanGeMap();
     }
     
     public void GetMobAttack(){
         if(this.botnappa.zone.mobs.size() >= 1){
            if(this.mAttack == null || this.mAttack.isDie()){
            mAttack = this.botnappa.zone.mobs.get(new Random().nextInt(this.botnappa.zone.mobs.size()));
         }
      }
    }
     
     
     public void Attack(){
         this.GetMobAttack();
             if(Util.isTrue(50 , 100)){
                this.botnappa.playerSkill.skillSelect = this.botnappa.playerSkill.skills.get(0);
             } else {
                this.botnappa.playerSkill.skillSelect = this.botnappa.playerSkill.skills.get(1);
             }
       if(this.mAttack != null){
           if(this.botnappa.UseLastTimeSkill()){
               PlayerService.gI().playerMove(this.botnappa, this.mAttack.location.x, this.mAttack.location.y);
               SkillService.gI().useSkill(this.botnappa, null, this.mAttack, -1,null);
           }
      }
    }
    
        
   public void chanGeMap(){
       if(this.lastTimeChanM < ((System.currentTimeMillis() - 150000) -  new Random().nextInt(150000))){
           this.botnappa.joinMapNappa();
       }
   }
    
}