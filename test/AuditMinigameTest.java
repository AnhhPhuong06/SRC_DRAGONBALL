import models.Template;
import models.npc.TaiXiu;
import services.func.minigame.CSMM;
import server.Manager;

/** Bounded memory-only checks; no round workers or game server started. */
public final class AuditMinigameTest {
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    public static void main(String[] args){
        for(int id=0;id<1450;id++){var t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.name="fixture";Manager.ITEM_TEMPLATES.add(t);}
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"fixture",0));
        var player=ToriNguSacShopTest.player(0);player.inventory.ruby=100;
        var game=new CSMM();game.costRuby=200;game.Register(player,1);
        check(player.inventory.ruby==100&&game.listRegNumber.isEmpty(),"Configured ticket price cannot overdraw wallet");
        game.costRuby=100;
        for(int number:new int[]{-1,0,1000,Integer.MAX_VALUE})game.Register(player,number);
        check(player.inventory.ruby==100&&game.listRegNumber.isEmpty(),"Out-of-range lottery numbers rejected");
        game.totalRuby=Integer.MAX_VALUE-99;game.Register(player,1);
        check(player.inventory.ruby==100&&game.listRegNumber.isEmpty(),"Pot overflow rejected before debit");
        game.totalRuby=0;game.Register(player,1);game.Register(player,2);
        check(player.inventory.ruby==0&&game.totalRuby==100&&game.listRegNumber.size()==1,"One valid registration and one debit");
        var dice=new TaiXiu();
        for(long amount:new long[]{-1,0,Long.MIN_VALUE,100001,Long.MAX_VALUE}){dice.addTai(player,amount);dice.addXiu(player,amount);}
        check(dice.totalTai==0&&dice.totalXiu==0&&dice.playersTai.isEmpty()&&dice.playersXiu.isEmpty(),"Invalid wagers cannot alter round state");
        System.out.println("PASS: "+checks+" minigame price/range, pot overflow and invalid wager regressions");
    }
}
