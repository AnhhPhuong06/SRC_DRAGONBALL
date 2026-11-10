package services.top;

import consts.ConstNpc;
import java.io.*;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.sql.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import models.item.Item;
import models.npc.npc_list.DaiThienSu;
import models.player.Player;
import network.io.Message;
import network.session.MySession;
import server.Client;
import services.Service;
import sun.misc.Unsafe;

/** Isolated JDBC/player/session fixtures. No server threads, real SQL or score changes. */
public final class BossHuntRankingTest {
    static int checks;
    static Unsafe unsafe;
    static Map<Long,Player> online = new ConcurrentHashMap<>();
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}

    public static void main(String[] args)throws Exception{
        var field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);unsafe=(Unsafe)field.get(null);
        var client=(Client)unsafe.allocateInstance(Client.class);set(client,"players_id",online);set(client,"players_userId",new ConcurrentHashMap<>());set(client,"players_name",new ConcurrentHashMap<>());set(client,"players",new java.util.concurrent.CopyOnWriteArrayList<>());
        var instance=Client.class.getDeclaredField("instance");instance.setAccessible(true);Object previous=instance.get(null);instance.set(null,client);
        try{ranking();menu();}finally{instance.set(null,previous);}
        System.out.println("PASS: "+checks+" hunting ranking, persistent bigint score, online/offline appearance, filters, ties/limit, packet compatibility and Đại Thiên Sứ menu checks; no live DB access");
    }
    static void set(Object target,String field,Object value)throws Exception{var f=Client.class.getDeclaredField(field);f.setAccessible(true);f.set(target,value);}
    static MySession session(int version)throws Exception{var session=(MySession)unsafe.allocateInstance(MySession.class);session.version=version;return session;}
    static Player online(int id,String name)throws Exception{var p=new Player();p.id=id;p.name=name;p.head=48;p.gender=0;p.session=session(240);for(int i=0;i<10;i++)p.inventory.itemsBody.add(new Item());online.put(p.id,p);return p;}

    static void ranking()throws Exception{
        Memory db=new Memory();db.add(10,50);db.add(5,50);db.add(8,100);db.add(2,3_000_000_000L);db.add(3,Long.MAX_VALUE);db.add(20,0);db.add(21,-1);db.add(30,900).put("admin",true);db.add(31,800).put("ban",true);
        Player live=online(8,"Online Hunter");live.sanBoss=0;
        var table=BossHuntRanking.load(db.connection());
        check(table.name.equals("Top Săn Boss")&&table.id==-1,"Dedicated display board does not claim an automatic-reward template ID");
        check(table.tops.stream().map(TOP::getId_player).toList().equals(List.of(3,2,8,5,10)),"Only positive scores, descending score then ascending ID; admin/banned excluded");
        check(table.tops.get(0).getValue()==Long.MAX_VALUE&&table.tops.get(1).getValue()==3_000_000_000L,"BIGINT hunting score never truncates to Player.sanBoss int");
        check(table.tops.get(2).getPlayer()==live&&table.tops.get(2).getValue()==100,"Online RAM score zero cannot override committed database score");
        check(table.tops.get(0).getPlayer().inventory.itemsBody.size()>=10,"Offline appearance is safe to render");
        check(table.tops.get(2).getInfo1().contains("100")&&table.tops.get(2).getInfo1().contains("Điểm săn boss"),"Ranking shows the hunting score");
        check(db.timeouts==1&&db.writes==0,"Ranking is bounded by a query timeout and performs only SELECTs");
        for(int version:new int[]{3,213,214,240})packet(table,version);
        db.rows.get(8).put("score",1000L);var refreshed=BossHuntRanking.load(db.connection());check(refreshed.tops.get(2).getValue()==1000&&live.sanBoss==0,"Reopening reads new persisted points without saving or mutating online players");
        Memory empty=new Memory();var noEntries=BossHuntRanking.load(empty.connection());check(noEntries.tops.isEmpty(),"Empty ranking produces an empty list normally");packet(noEntries,240);
        Memory many=new Memory();for(int i=1;i<=150;i++)many.add(i,i);var capped=BossHuntRanking.load(many.connection());check(capped.tops.size()==100&&capped.tops.get(0).getId_player()==150&&capped.tops.get(99).getId_player()==51,"Top 100 cap matches client count and SQL limit");
        Memory edge=new Memory();edge.add(200,4);edge.add(201,3);edge.add(202,2);edge.add(203,1);Player bot=online(200,"Bot");bot.isBot=true;Player admin=online(201,"Live admin");admin.session.isAdmin=true;edge.rows.get(202).put("head","broken");
        var guarded=BossHuntRanking.load(edge.connection());check(guarded.tops.size()==1&&guarded.tops.get(0).getId_player()==203,"Live bots/admins and one malformed player cannot break remaining ranks");
        db.failQuery=true;try{BossHuntRanking.load(db.connection());throw new AssertionError("Query failure was hidden");}catch(SQLException expected){check(true,"SQL failure is propagated instead of reporting an empty successful ranking");}
        check(TopManager.AUTO_GIFT_TOPS.isEmpty()&&TopManager.TOP_TEMPLATE.isEmpty(),"Reading does not register campaigns, reset points or award gifts");
    }
    static void packet(TopManager.TopTemplate table,int version)throws Exception{
        var viewer=new CapturePlayer();viewer.session=session(version);Service.gI().showListTop(viewer,table);
        check(viewer.command==-96&&viewer.bytes!=null,"Uses existing client ranking packet for version "+version);
        var reader=new DataInputStream(new ByteArrayInputStream(viewer.bytes));check(reader.readUnsignedByte()==0&&reader.readUTF().equals("Top Săn Boss"),"Correct packet action/title");int count=reader.readUnsignedByte();check(count==table.tops.size(),"Exact list count");
        for(int i=0;i<count;i++){var top=table.tops.get(i);check(reader.readInt()==i+1&&reader.readInt()==top.getId_player(),"Rank uses sequential position and real player ID");reader.readShort();if(version>=214)check(reader.readShort()==-1,"Modern appearance extension");reader.readShort();reader.readShort();check(reader.readUTF().contains(top.getPlayer().name)&&reader.readUTF().equals(top.getInfo1())&&reader.readUTF().equals(top.getInfo2()),"Name/appearance/score strings align with client reader");}
        check(reader.available()==0,"Packet has no trailing or missing fields");
    }
    static void menu()throws Exception{
        FakeNpc npc=new FakeNpc();var p=new CapturePlayer();p.session=session(240);TopManager.TOP_TEMPLATE.add(new TopManager.TopTemplate(1,"Sức mạnh","fixture unused"));TopManager.setSelectNpc();
        npc.openBaseMenu(p);check(Arrays.equals(npc.buttons,new String[]{"Bảng\nXếp Hạng","Top\nSăn Boss"}),"NPC root retains old board action and adds dedicated hunting button");
        npc.confirmMenu(p,1);check(npc.huntCalls==1&&npc.lastViewer==p,"New root action routes to hunting ranking");
        npc.confirmMenu(p,-1);npc.confirmMenu(p,99);check(npc.huntCalls==1,"Invalid root selection never opens a board");
        npc.confirmMenu(p,0);check(p.idMark.getIndexMenu()==ConstNpc.IGNORE_MENU&&npc.buttons[0].contains("Sức mạnh"),"Original configurable board submenu remains intact");
        npc.allowed=false;int calls=npc.menuCalls;npc.openBaseMenu(p);npc.confirmMenu(p,1);check(npc.menuCalls==calls&&npc.huntCalls==1,"NPC distance/map access guard still applies");
        npc.allowed=true;TopManager.TOP_TEMPLATE.clear();TopManager.SELECT=new String[0];npc.openBaseMenu(p);npc.confirmMenu(p,1);check(npc.huntCalls==2,"Hunting remains accessible when no other top templates are configured");
    }
    static final class CapturePlayer extends Player{byte command;byte[] bytes;@Override public void sendMessage(Message message){command=message.command;bytes=message.getData().clone();}}
    static final class FakeNpc extends DaiThienSu{
        boolean allowed=true;String[] buttons;int huntCalls,menuCalls;Player lastViewer;
        FakeNpc(){super(5,1,100,300,ConstNpc.DAI_THIEN_SU,0);}
        @Override public boolean canOpenNpc(Player p){return allowed;}
        @Override public void createOtherMenu(Player p,int index,String text,String...choices){p.idMark.setIndexMenu(index);buttons=choices;menuCalls++;}
        @Override protected void showBossHuntRanking(Player p){huntCalls++;lastViewer=p;}
    }
    static final class Memory{
        Map<Integer,Map<String,Object>> rows=new LinkedHashMap<>();int writes,timeouts;boolean failQuery;
        Map<String,Object> add(int id,long score){var row=new LinkedHashMap<String,Object>();row.put("id",id);row.put("score",score);row.put("name","Offline "+id);row.put("head",48);row.put("gender",0);row.put("items_body","[]");row.put("admin",false);row.put("ban",false);rows.put(id,row);return row;}
        Connection connection(){return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            if(m.getName().equals("close"))return null;
            if(!m.getName().equals("prepareStatement"))throw new AssertionError("Unexpected connection method: "+m.getName());
            String sql=(String)a[0];return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(ps,pm,pa)->{
                if(pm.getName().equals("close"))return null;if(pm.getName().equals("setQueryTimeout")){check((int)pa[0]==5,"Query timeout is five seconds");timeouts++;return null;}
                if(pm.getName().equals("executeQuery")){
                    if(failQuery)throw new SQLException("Fixture offline");
                    if(sql.equals(BossHuntRanking.QUERY))return result(rows.values().stream().filter(r->(long)r.get("score")>0&&!(boolean)r.get("admin")&&!(boolean)r.get("ban")).sorted(Comparator.<Map<String,Object>>comparingLong(r->(long)r.get("score")).reversed().thenComparingInt(r->(int)r.get("id"))).limit(100).toList());
                    if(sql.startsWith("select * from player where id = '")){int id=Integer.parseInt(sql.split("'")[1]);return result(rows.containsKey(id)?List.of(rows.get(id)):List.of());}
                    throw new AssertionError("Unexpected SELECT: "+sql);
                }
                writes++;throw new AssertionError("Unexpected statement method: "+pm.getName());
            });
        });}
        static ResultSet result(List<Map<String,Object>> rows){int[] cursor={-1};return (ResultSet)Proxy.newProxyInstance(Memory.class.getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->{
            if(m.getName().equals("close"))return null;if(m.getName().equals("next"))return ++cursor[0]<rows.size();if(m.getName().equals("first")){cursor[0]=0;return !rows.isEmpty();}
            Object value=rows.get(cursor[0]).get(a[0]);if(value instanceof String&&m.getName().equals("getShort"))throw new SQLException("Fixture malformed head");
            return switch(m.getName()){case "getInt"->((Number)value).intValue();case "getLong"->((Number)value).longValue();case "getShort"->((Number)value).shortValue();case "getByte"->((Number)value).byteValue();case "getString"->value==null?null:value.toString();default->throw new AssertionError("Unexpected result method: "+m.getName());};
        });}
    }
}
