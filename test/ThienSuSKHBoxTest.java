package services;

import consts.ConstNpc;
import java.io.*;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import models.Template.*;
import models.item.Item;
import models.npc.Npc;
import models.npc.NpcFactory;
import models.player.Player;
import network.io.Message;
import network.session.MySession;
import server.Manager;
import services.func.UseItem;
import sun.misc.Unsafe;

/** Actual item-type-75 dispatch, cat menus and inventory mutation; isolated templates, no DB. */
public final class ThienSuSKHBoxTest {
    static int checks,nextId=100;static Unsafe unsafe;static Npc cat;static Method use;
    static final int[][] GEAR={{1048,1051,1054,1057,1060},{1049,1052,1055,1058,1061},{1050,1053,1056,1059,1062}};
    static final int[][] SETS={{127,128,129,246},{130,131,132},{133,134,135}};
    static final String[][] COUNTERS={{"thienXinHang","kirin","songoku","kaioken"},{"picolo","ocTieu","pikkoroDaimao"},{"kakarot","cadic","nappa"}};
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{
        var uf=Unsafe.class.getDeclaredField("theUnsafe");uf.setAccessible(true);unsafe=(Unsafe)uf.get(null);
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        for(short id=0;id<=1495;id++){var t=new ItemTemplate();t.id=id;t.name="Item "+id;t.description="";t.type=27;t.gender=3;Manager.ITEM_TEMPLATES.add(t);}
        for(int planet=0;planet<3;planet++)for(int slot=0;slot<5;slot++){var t=Manager.ITEM_TEMPLATES.get(GEAR[planet][slot]);t.type=(byte)slot;t.gender=(byte)planet;}
        for(int id:new int[]{1477,1478}){var t=Manager.ITEM_TEMPLATES.get(id);t.type=75;t.iconID=15921;t.isUpToUp=true;t.name=id==1477?"Hộp SKH Thiên Sứ":"Hộp SKH Thiên Sứ Random";}
        NpcFactory.createNpcConMeo();cat=Manager.NPCS.stream().filter(n->n.tempId==ConstNpc.CON_MEO).findFirst().orElseThrow();
        use=UseItem.class.getDeclaredMethod("useItem",Player.class,Item.class,int.class);use.setAccessible(true);
        for(int planet=0;planet<3;planet++)for(int set=0;set<SETS[planet].length;set++)for(int quantity:new int[]{1,3}){
            var p=player(1477,quantity,5);p.gender=(byte)((planet+1)%3);Item box=p.inventory.itemsBag.get(0);
            use(p,box);menu(p,ConstNpc.MENU_HOP_SKH_THIEN_SU_PLANET,4,"Trái Đất","Namếc","Xayda");check(boxes(p,1477)==quantity,"Opening 1477 does not consume");
            p.idMark.setPlanetSkh((planet+2)%3);p.idMark.setSkhOptionId(243);
            cat.confirmMenu(p,planet);menu(p,ConstNpc.MENU_HOP_SKH_THIEN_SU_SET,SETS[planet].length+1);check(rewards(p).isEmpty()&&boxes(p,1477)==quantity,"Planet selection only opens set menu");
            cat.confirmMenu(p,set);check(boxes(p,1477)==quantity-1&&rewards(p).size()==5,"One 1477 gives exactly five pieces and consumes exactly one box");
            check(rewards(p).stream().map(i->(int)i.template.id).toList().equals(Arrays.stream(GEAR[planet]).boxed().toList()),"All five heavenly slots from selected planet");
            for(int slot=0;slot<5;slot++)gear(rewards(p).get(slot),planet,slot,SETS[planet][set]);
            p.inventory.itemsBody.addAll(rewards(p));p.setClothes.setup();check(p.setClothes.getClass().getField(COUNTERS[planet][set]).getByte(p.setClothes)==5,"Bundle activates the selected set");
            finished(p);int packets=p.bagPackets;cat.confirmMenu(p,set);check(p.bagPackets==packets&&boxes(p,1477)==quantity-1,"Replayed set confirmation cannot open another box");
        }
        for(int planet=0;planet<3;planet++)for(int slot=0;slot<5;slot++)for(int set=0;set<SETS[planet].length;set++){
            var p=player(1478,3,1);p.gender=(byte)((planet+1)%3);use(p,p.inventory.itemsBag.get(0));menu(p,ConstNpc.MENU_HOP_SKH_THIEN_SU_PLANET,4);
            int[] choices={slot,set},cursor={0};ThienSuSKHBox.selectPlanet(p,planet,(min,max)->{int value=choices[cursor[0]++];check(min==0&&value<=max,"Random slot/set bounds");return value;});
            check(cursor[0]==2&&boxes(p,1478)==2&&rewards(p).size()==1,"1478 randomizes exactly one slot and one set");gear(rewards(p).get(0),planet,slot,SETS[planet][set]);finished(p);
        }
        for(int planet=0;planet<3;planet++){var p=player(1478,2,1);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,planet);check(rewards(p).size()==1&&Arrays.stream(GEAR[planet]).anyMatch(id->id==rewards(p).get(0).template.id),"Actual random cat-menu dispatch uses selected planet");finished(p);}
        rejections();concurrency();clientTemplates();
        System.out.println("PASS: "+checks+" 1477/1478 type-75 dispatch, every planet/set/slot, full-set activation, random rewards, cancel/stale/ownership, bag/template failures, fees and duplicate/concurrent confirmation checks; no live DB access");
    }
    static void rejections()throws Exception{
        for(int id:new int[]{1477,1478}){
            int required=id==1477?5:1;
            for(int space=0;space<required;space++){var p=player(id,2,space);String before=snapshot(p);use(p,p.inventory.itemsBag.get(0));check(snapshot(p).equals(before)&&!NpcFactory.PLAYERID_OBJECT.containsKey(p.id),"Insufficient initial space never consumes/opens selection");}
            for(int invalid:new int[]{-1,3,4,255}){var p=player(id,2,required);use(p,p.inventory.itemsBag.get(0));String before=snapshot(p);cat.confirmMenu(p,invalid);check(snapshot(p).equals(before)&&p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU,"Cancel/invalid planet has no cost");check(!NpcFactory.PLAYERID_OBJECT.containsKey(p.id),"Cancel removes pending choice");}
            var p=player(id,2,required);Item selected=p.inventory.itemsBag.get(0);use(p,selected);p.inventory.itemsBag.set(0,ItemService.gI().createNewItem((short)id,7));String before=snapshot(p);cat.confirmMenu(p,0);check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Moved/replaced selected stack cannot consume another stack");
            p=player(id,2,required);use(p,p.inventory.itemsBag.get(0));p.inventory.itemsBag.get(0).quantity=0;before=snapshot(p);cat.confirmMenu(p,0);check(snapshot(p).equals(before),"Exhausted box cannot grant rewards");
            p=player(id,2,required);use(p,p.inventory.itemsBag.get(0));Object unrelated=new Object();NpcFactory.PLAYERID_OBJECT.put(p.id,unrelated);before=snapshot(p);cat.confirmMenu(p,0);check(snapshot(p).equals(before)&&NpcFactory.PLAYERID_OBJECT.get(p.id)==unrelated,"Wrong menu context cannot grant or remove another NPC context");
        }
        for(int invalid:new int[]{-1,4,5,255}){var p=player(1477,2,5);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,0);String before=snapshot(p);cat.confirmMenu(p,invalid);check(snapshot(p).equals(before)&&!NpcFactory.PLAYERID_OBJECT.containsKey(p.id),"Cancel/invalid set cannot consume");}
        var p=player(1477,2,5);use(p,p.inventory.itemsBag.get(0));String before=snapshot(p);ThienSuSKHBox.selectSet(p,0);check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Set choice cannot skip planet stage");
        p=player(1477,2,5);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,0);p.inventory.itemsBag.set(5,ItemService.gI().createNewItem((short)100));before=snapshot(p);cat.confirmMenu(p,0);check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Bag is checked again on final confirmation");
        p=player(1478,2,1);use(p,p.inventory.itemsBag.get(0));p.inventory.itemsBag.set(1,ItemService.gI().createNewItem((short)100));before=snapshot(p);ThienSuSKHBox.selectPlanet(p,0,(a,b)->{throw new AssertionError("Full bag must not roll");});check(snapshot(p).equals(before),"Random box checks space before RNG");
        p=player(1477,2,5);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,0);var original=Manager.ITEM_TEMPLATES.set(1060,null);before=snapshot(p);cat.confirmMenu(p,0);Manager.ITEM_TEMPLATES.set(1060,original);check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Missing last template grants none of the first four pieces and consumes nothing");
        p=player(1477,2,5);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,0);byte type=original.type;original.type=27;before=snapshot(p);cat.confirmMenu(p,0);original.type=type;check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Incorrect equipment type cannot enter bundle");
        p=player(1478,2,1);use(p,p.inventory.itemsBag.get(0));var option=Manager.ITEM_OPTION_TEMPLATES.set(139,null);before=snapshot(p);ThienSuSKHBox.selectPlanet(p,0,(a,b)->0);Manager.ITEM_OPTION_TEMPLATES.set(139,option);check(snapshot(p).equals(before)&&rewards(p).isEmpty(),"Missing activation option preserves box");
        p=player(1477,2,5);Item fake=ItemService.gI().createNewItem((short)1477);ThienSuSKHBox.open(p,fake);check(!NpcFactory.PLAYERID_OBJECT.containsKey(p.id),"Forged/out-of-bag item cannot start selection");
    }
    static void concurrency()throws Exception{
        var p=player(1477,3,10);use(p,p.inventory.itemsBag.get(0));cat.confirmMenu(p,2);var start=new CountDownLatch(1);var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
        Runnable confirm=()->{try{start.await();ThienSuSKHBox.selectSet(p,2);}catch(Throwable e){failure.set(e);}};var a=new Thread(confirm);var b=new Thread(confirm);a.start();b.start();start.countDown();a.join();b.join();check(failure.get()==null&&boxes(p,1477)==2&&rewards(p).size()==5&&p.bagPackets==1,"Concurrent duplicate confirmation grants one bundle and charges one box");
        var random=player(1478,3,2);use(random,random.inventory.itemsBag.get(0));var gate=new CountDownLatch(1);failure.set(null);Runnable choose=()->{try{gate.await();ThienSuSKHBox.selectPlanet(random,0,(min,max)->0);}catch(Throwable e){failure.set(e);}};a=new Thread(choose);b=new Thread(choose);a.start();b.start();gate.countDown();a.join();b.join();check(failure.get()==null&&boxes(random,1478)==2&&rewards(random).size()==1&&random.bagPackets==1,"Concurrent random confirmation consumes/grants exactly once");
    }
    static void clientTemplates()throws Exception{
        CaptureSession session=(CaptureSession)unsafe.allocateInstance(CaptureSession.class);var send=data.ItemData.class.getDeclaredMethod("updateItemTemplate",MySession.class,int.class,int.class);send.setAccessible(true);send.invoke(null,session,750,Manager.ITEM_TEMPLATES.size());
        try(var in=new DataInputStream(new ByteArrayInputStream(session.data))){check(in.readUnsignedByte()==8&&in.readByte()==data.DataGame.vsItem&&data.DataGame.vsItem>=16&&in.readUnsignedByte()==2,"Client item data version is bumped and transmitted");int start=in.readUnsignedShort(),end=in.readUnsignedShort();for(int id=start;id<end;id++){int type=in.readUnsignedByte(),gender=in.readUnsignedByte();String name=in.readUTF();in.readUTF();in.readByte();in.readInt();int icon=in.readShort();in.readShort();boolean stack=in.readBoolean();if(id==1477||id==1478)check(type==75&&gender==3&&icon==15921&&stack&&name.equals(Manager.ITEM_TEMPLATES.get(id).name),"Client receives correct box name/type/icon");}check(in.available()==0,"Item template packet fields remain aligned");}
    }
    static void gear(Item item,int planet,int slot,int set){check(item.template.id==GEAR[planet][slot]&&item.template.type==slot&&item.quantity==1&&item.isDTS(),"Correct heavenly equipment identity/slot/quantity");check(item.template.gender==planet||item.template.gender==3,"Chosen planet controls reward");check(item.hasOption(set)&&item.hasOption(ItemService.gI().optionIdSKH(set)),"Matching set marker and activation pair");check(item.getOptionParam(21)==80&&item.hasOption(30),"Heavenly power requirement and trade lock");check(item.hasOption(new int[]{47,22,0,23,14}[slot]),"Heavenly base stat present");}
    static void finished(Capture p){check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU&&!NpcFactory.PLAYERID_OBJECT.containsKey(p.id),"Successful choice is closed and cannot be replayed");check(p.bagPackets==1&&p.inventory.gold==1234&&p.inventory.gem==567&&p.inventory.ruby==890,"One bag refresh and no currency fees: packets="+p.bagPackets+", gold="+p.inventory.gold+", gem="+p.inventory.gem+", ruby="+p.inventory.ruby);}
    static Capture player(int id,int quantity,int empty)throws Exception{var p=new Capture();p.id=nextId++;p.isPlayer=true;p.nPoint.power=100_000_000_000L;p.inventory.gold=1234;p.inventory.gem=567;p.inventory.ruby=890;p.session=(MySession)unsafe.allocateInstance(MySession.class);p.session.version=240;p.inventory.itemsBag.add(ItemService.gI().createNewItem((short)id,quantity));for(int i=0;i<empty;i++)p.inventory.itemsBag.add(new Item());return p;}
    static void use(Capture p,Item box)throws Exception{try{use.invoke(UseItem.gI(),p,box,p.inventory.itemsBag.indexOf(box));}catch(InvocationTargetException e){throw new AssertionError(e.getCause());}}
    static int boxes(Player p,int id){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.template.id==id).mapToInt(i->i.quantity).sum();}
    static List<Item> rewards(Player p){return p.inventory.itemsBag.stream().filter(i->i.isNotNullItem()&&i.isDTS()).toList();}
    static String snapshot(Player p){return p.inventory.itemsBag.stream().map(i->i.isNotNullItem()?i.template.id+":"+i.quantity+":"+i.getInfo():"empty").toList().toString()+p.inventory.gold+":"+p.inventory.gem+":"+p.inventory.ruby;}
    static void menu(Capture p,int id,int count,String... names)throws Exception{check(p.idMark.getIndexMenu()==id,"Correct selection stage");try(var in=new DataInputStream(new ByteArrayInputStream(p.menuPacket))){check(in.readShort()==ConstNpc.CON_MEO,"Uses the standard choice NPC");check(in.readUTF().contains("Thiên Sứ"),"Menu describes heavenly reward");check(in.readUnsignedByte()==count,"Correct menu button count");for(String name:names)check(in.readUTF().equals(name),"All planet choices");}}
    static final class Capture extends Player{byte[] menuPacket;int bagPackets;@Override public void sendMessage(Message message){if(message.command==32)menuPacket=message.getData().clone();if(message.command==-36)bagPackets++;}}
    static final class CaptureSession extends MySession{byte[] data;private CaptureSession(){super(null);}@Override public void doSendMessage(Message message){data=message.getData().clone();}}
}
