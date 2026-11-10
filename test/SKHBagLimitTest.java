import java.lang.reflect.Field;
import java.util.*;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import models.item.Item;
import models.player.Player;
import server.Manager;
import services.ItemService;
import utils.Util;

/** Regression for the shared SKH sampler: inventory contents must not block a set. */
public final class SKHBagLimitTest {
    static int checks;static Random random;static final ItemService ITEMS=ItemService.gI();
    static final int[][] SETS={{127,128,129,246},{130,131,132},{133,134,135}};
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        Field field=Util.class.getDeclaredField("rand");field.setAccessible(true);random=(Random)field.get(null);
        for(int i=0;i<252;i++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(i,"Option #",0));
        if(args.length==1&&args[0].equals("--empty-signature")){
            var signature=java.security.MessageDigest.getInstance("SHA-256");
            for(byte gender=0;gender<3;gender++)signature.update(Arrays.deepToString(sequence(gender,player(gender),912345L,1000)).getBytes(java.nio.charset.StandardCharsets.UTF_8));
            System.out.println("EMPTY_SIGNATURE="+HexFormat.of().formatHex(signature.digest()));return;
        }
        for(byte gender=0;gender<3;gender++){
            Player player=player(gender);int formerlyBlocked=new int[]{129,131,135}[gender];fill(player,4,formerlyBlocked);
            random.setSeed(20261022L);boolean found=false;
            for(int roll=0;roll<2000;roll++)if(ITEMS.randOptionItemKichHoat(gender,player)[0]==formerlyBlocked)found=true;
            check(found,"Set "+formerlyBlocked+" remains obtainable with four matching items in bag");
            player.inventory.itemsBag.clear();int[][] expected=sequence(gender,player,912345L,1000);
            for(int count:new int[]{0,1,3,4,5,20,100}){
                fill(player,count,formerlyBlocked);String before=snapshot(player);
                checkSequence(gender,player,expected,"Same set odds with "+count+" matching items");
                check(snapshot(player).equals(before),"Random selection never consumes/changes existing items");
            }
            player.inventory.itemsBag.clear();
            for(int i=0;i<12;i++)player.inventory.itemsBag.add(item(new int[]{129,131,135}[i%3],i+1));
            checkSequence(gender,player,expected,"Mixed items from all planets do not bias the set");
            checkSequence(gender,null,expected,"Sampler only needs the selected planet");
        }
        System.out.println("PASS: "+checks+" SKH reachability after four items, unchanged empty-bag odds, exact seeded sequences for 0/1/3/4/5/20/100 items, mixed planets and inventory preservation checks; no DB access");
    }
    static Player player(byte gender){var p=new Player();p.gender=gender;return p;}
    static Item item(int option,int quantity){var item=new Item();var template=new ItemTemplate();template.id=0;template.type=0;item.template=template;item.quantity=quantity;item.itemOptions.add(new Item.ItemOption(option,1));return item;}
    static void fill(Player p,int count,int option){p.inventory.itemsBag.clear();for(int i=0;i<count;i++)p.inventory.itemsBag.add(item(option,i+1));}
    static int[][] sequence(int gender,Player p,long seed,int count){random.setSeed(seed);int[][] result=new int[count][];for(int i=0;i<count;i++)result[i]=ITEMS.randOptionItemKichHoat(gender,p);return result;}
    static void checkSequence(int gender,Player p,int[][] expected,String message){int[][] actual=sequence(gender,p,912345L,expected.length);var seen=new HashSet<Integer>();for(int i=0;i<actual.length;i++){check(Arrays.equals(actual[i],expected[i]),message);int id=actual[i][0];check(Arrays.stream(SETS[gender]).anyMatch(value->value==id)&&actual[i][1]==ITEMS.optionIdSKH(id),"Valid planet option pair");seen.add(id);}check(seen.size()==SETS[gender].length,"Every set remains reachable");}
    static String snapshot(Player p){return p.inventory.itemsBag.stream().map(i->i.template.id+":"+i.quantity+":"+i.itemOptions.get(0).optionTemplate.id+":"+i.itemOptions.get(0).param).toList().toString();}
}
