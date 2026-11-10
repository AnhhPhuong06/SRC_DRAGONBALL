import managers.GiftCodeManager;
import models.GiftCode;
import java.lang.reflect.Field;
import services.GiftCodeService;
import server.Manager;
import models.Template;

/** Real redemption and currency grant path using a fake reservation, never JDBC. */
public final class AuditGiftcodeBoundaryTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    static final class Memory extends GiftCodeManager {
        int reservations;
        @Override protected boolean reserveGiftCodeUse(GiftCode gift){reservations++;return true;}
    }
    public static void main(String[] args)throws Exception{
        for(int id=0;id<1450;id++){
            var t=new Template.ItemTemplate();t.id=(short)id;t.type=27;t.gender=3;t.name="fixture";
            Manager.ITEM_TEMPLATES.add(t);
        }
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new Template.ItemOptionTemplate(id,"fixture",0));
        Field field=GiftCodeManager.class.getDeclaredField("instance");field.setAccessible(true);
        Object original=field.get(null);
        try{
            for(int currency:new int[]{-2,-3}){
                Memory db=new Memory();field.set(null,db);GiftCode gift=new GiftCode();gift.code="fixture";gift.countLeft=1;
                gift.detail.put(currency,Integer.MAX_VALUE);db.listGiftCode.add(gift);
                var player=ToriNguSacShopTest.player(0);
                GiftCodeService.gI().giftCode(player,"fixture");
                check((currency==-2?player.inventory.gem:player.inventory.ruby)==200000000,"Large currency reward saturates without signed int overflow");
                check(db.reservations==1&&gift.countLeft==0,"One reservation");
                GiftCodeService.gI().giftCode(player,"fixture");
                check(db.reservations==1,"Immediate replay rejected");
            }
            for(int quantity:new int[]{-1,Integer.MIN_VALUE,0}){
                Memory db=new Memory();GiftCode gift=new GiftCode();gift.code="invalid";gift.countLeft=2;gift.detail.put(-2,quantity);db.listGiftCode.add(gift);
                check(db.checkUseGiftCode(ToriNguSacShopTest.player(0),"invalid")==null&&db.reservations==0&&gift.countLeft==2,"Invalid reward rejected before decrement");
            }
            System.out.println("PASS: "+checks+" giftcode overflow, invalid rewards and immediate replay regressions");
        }finally{field.set(null,original);}
    }
}
