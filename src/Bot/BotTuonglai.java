package Bot;

import java.util.Random;
import models.mob.Mob;
import services.SkillService;
import services.player.PlayerService;
import utils.Util;

public class BotTuonglai {
     private Mob mAttack;
     
     public long lastTimeChanM;
     
     public Bot bottuonglai;
     
     
     public BotTuonglai(Bot b){
         this.bottuonglai = b;
     }
     
     public void update(){
        this.Attack();
        this.chanGeMap();
     }
     
     public void GetMobAttack(){
         if(this.bottuonglai.zone.mobs.size() >= 1){
            if(this.mAttack == null || this.mAttack.isDie()){
            mAttack = this.bottuonglai.zone.mobs.get(new Random().nextInt(this.bottuonglai.zone.mobs.size()));
         }
      }
    }
     
     
     public void Attack(){
         this.GetMobAttack();
             if(Util.isTrue(50 , 100)){
                this.bottuonglai.playerSkill.skillSelect = this.bottuonglai.playerSkill.skills.get(0);
             } else {
                this.bottuonglai.playerSkill.skillSelect = this.bottuonglai.playerSkill.skills.get(1);
             }
       if(this.mAttack != null){
           if(this.bottuonglai.UseLastTimeSkill()){
               PlayerService.gI().playerMove(this.bottuonglai, this.mAttack.location.x, this.mAttack.location.y);
               SkillService.gI().useSkill(this.bottuonglai, null, this.mAttack, -1,null);
           }
      }
    }
    
        
   public void chanGeMap(){
       if(this.lastTimeChanM < ((System.currentTimeMillis() - 150000) -  new Random().nextInt(150000))){
           this.bottuonglai.joinMapTuonglai();
       }
   }
    
}