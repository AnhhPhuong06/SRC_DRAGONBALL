package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import managers.GiftCodeManager;
import models.GiftCode;
import models.Template.*;
import models.item.Item;
import models.player.Player;
import server.Manager;

/** Transactional in-memory JDBC, real batch staging/router and real redemption guards. */
public final class GiftcodeBatchTest {
    static int checks;static final CatalogData catalog=new CatalogData();
    public static void main(String[] args)throws Exception {
        for(int id=0;id<252;id++)Manager.ITEM_OPTION_TEMPLATES.add(new ItemOptionTemplate(id,"Option #",0));
        for(int id=0;id<=568;id++){var t=new ItemTemplate();t.id=(short)id;t.type=27;t.gender=3;t.name="Item "+id;Manager.ITEM_TEMPLATES.add(t);}
        catalog.items.put(568,Map.of("id",568,"name","Trứng","icon",2164));catalog.options.put(50,Map.of("id",50,"name","SĐ #%"));
        Memory db=new Memory();db.saved.add(row(1,"han-0000000000000001",5,"[{\"id\":-2,\"quantity\":1,\"options\":[]}]"));
        TestManager manager=new TestManager(db);AtomicInteger sequence=new AtomicInteger();
        var result=(Map<?,?>)GiftcodeApi.createBatch(payload(),catalog,db.connection(),manager,()->String.format("%016d",sequence.incrementAndGet()));
        List<?> codes=(List<?>)result.get("codes");check(codes.size()==50&&new HashSet<>(codes).size()==50,"Exactly fifty unique codes");
        check(result.get("count").equals(50)&&result.get("countLeft").equals(1)&&result.get("synced").equals(true),"Response describes global single-use batch");
        check(sequence.get()==51,"Existing code collision is checked case-insensitively");
        check(db.saved.size()==51&&db.commits==1&&db.rollbacks==0,"One atomic commit for entire batch");
        check(manager.listGiftCode.size()==51,"All active codes published after commit");
        for(int i=0;i<5;i++)check(manager.checkUseGiftCode(player("legacy"+i),"han-0000000000000001")!=null,"Existing multi-use giftcodes keep their configured stock");
        check(manager.checkUseGiftCode(player("legacy-extra"),"han-0000000000000001")==null,"Existing stock still has an exact limit");
        for(var r:db.saved)if(((Number)r.get("id")).intValue()!=1) {
            check(r.get("count_left").equals(1)&&r.get("allGender").equals("all")&&r.get("player_name")==null,"Each new code has only one global use");
            check(r.get("detail").equals("[{\"id\":568,\"quantity\":75,\"options\":[{\"id\":50,\"param\":25}]}]"),"Shared item quantities and individual options stored intact");
            check(((Timestamp)r.get("expired")).after((Timestamp)r.get("datecreate")),"Valid common expiry");
        }
        Player p=player("winner");String first=codes.get(0).toString();
        check(manager.checkUseGiftCode(p,"  "+first.toLowerCase(Locale.ROOT)+"  ")!=null,"First player claims code regardless of input case");
        check(db.count(first)==0&&p.giftCode.rewards.size()==1,"Persistent use claimed before recording redemption");
        check(manager.checkUseGiftCode(player("second"),first)==null&&manager.checkUseGiftCode(p,first)==null,"Neither another player nor same player can claim again");
        // A stale runtime copy after reload/restart still cannot re-use a zero-stock DB row.
        var restarted=new TestManager(db);restarted.listGiftCode.add(cloneGift(manager.listGiftCode.stream().filter(g->g.code.equals(first)).findFirst().orElseThrow(),1));
        Player retry=player("restart");check(restarted.checkUseGiftCode(retry,first)==null&&retry.giftCode.rewards.isEmpty(),"Restart/stale cache cannot resurrect exhausted code");
        String race=codes.get(1).toString();GiftCode gift=manager.listGiftCode.stream().filter(g->g.code.equals(race)).findFirst().orElseThrow();
        var secondServer=new TestManager(db);secondServer.listGiftCode.add(cloneGift(gift,1));
        var pool=Executors.newFixedThreadPool(12);List<Future<Boolean>> claims=new ArrayList<>();
        for(int i=0;i<40;i++){final int index=i;claims.add(pool.submit(()->(index%2==0?manager:secondServer).checkUseGiftCode(player("race"+index),race)!=null));}
        pool.shutdown();check(pool.awaitTermination(10,TimeUnit.SECONDS),"Concurrent claims finish");int accepted=0;for(var claim:claims)if(claim.get())accepted++;
        check(accepted==1&&db.count(race)==0,"Exactly one winner across forty concurrent claims and two runtime managers");
        String recover=codes.get(2).toString();manager.failClaim=true;Player recoverPlayer=player("recover");
        check(manager.checkUseGiftCode(recoverPlayer,recover)==null&&db.count(recover)==1&&recoverPlayer.giftCode.rewards.isEmpty(),"Failed persistence does not consume local or DB use/history");
        manager.failClaim=false;check(manager.checkUseGiftCode(recoverPlayer,recover)!=null&&db.count(recover)==0,"Failed persistence can be retried successfully");
        String full=codes.get(3).toString();Player fullBag=player("full");fullBag.inventory.itemsBag.clear();
        check(manager.checkUseGiftCode(fullBag,full)==null&&db.count(full)==1,"Full bag does not consume code");
        String whitelist=codes.get(4).toString();GiftCode restricted=manager.listGiftCode.stream().filter(g->g.code.equals(whitelist)).findFirst().orElseThrow();restricted.playerName="allowed";
        check(manager.checkUseGiftCode(player("wrong"),whitelist)==null&&db.count(whitelist)==1,"Name restriction still applies before claiming use");
        check(manager.checkUseGiftCode(player("allowed"),whitelist)!=null,"Allowed player can redeem");
        GiftCode expired=manager.listGiftCode.stream().filter(g->g.code.equals(codes.get(5))).findFirst().orElseThrow();expired.dateexpired=new Timestamp(1);
        check(manager.checkUseGiftCode(player("late"),expired.code)==null&&db.count(expired.code)==1,"Expired runtime gift cannot be claimed");

        for(int fail:new int[]{1,25,50,51}) {
            Memory broken=new Memory();TestManager current=new TestManager(broken);GiftCode old=new GiftCode();old.code="keep";current.listGiftCode.add(old);
            if(fail==51)broken.failCommit=true;else broken.failAt=fail;
            try{GiftcodeApi.createBatch(payload(),catalog,broken.connection(),current,counter());throw new AssertionError("Failed batch committed");}catch(SQLException expected){}
            check(broken.saved.isEmpty()&&broken.rollbacks==1&&current.listGiftCode.equals(List.of(old)),"Insert/commit failure rolls back all rows and preserves live manager: "+fail);
        }
        Memory collision=new Memory();var collisionManager=new TestManager(collision);
        try{GiftcodeApi.createBatch(payload(),catalog,collision.connection(),collisionManager,()->"SAME");throw new AssertionError("Repeated suffix accepted");}catch(PanelSupport.PanelException e){check(e.status==409,"Bounded generator rejects unresolved collisions");}
        check(collision.saved.isEmpty()&&collision.rollbacks==1&&collisionManager.listGiftCode.isEmpty(),"Cannot publish partial batch");
        Memory invalidStored=new Memory();invalidStored.saved.add(row(1,"old",1,"[{\"id\":9999,\"quantity\":1,\"options\":[]}]"));var unchanged=new TestManager(invalidStored);GiftCode keep=new GiftCode();keep.code="keep";unchanged.listGiftCode.add(keep);
        try{GiftcodeApi.createBatch(payload(),catalog,invalidStored.connection(),unchanged,counter());throw new AssertionError("Invalid staged runtime data accepted");}catch(PanelSupport.PanelException expected){}
        check(invalidStored.saved.size()==1&&invalidStored.rollbacks==1&&unchanged.listGiftCode.equals(List.of(keep)),"Runtime validation failure rolls back new codes and preserves live state");
        Memory noPrefix=new Memory();JsonObject automatic=new JsonParser().parse("{\"items\":[{\"id\":-2,\"quantity\":100,\"options\":[]}]}").getAsJsonObject();
        var automaticResult=(Map<?,?>)GiftcodeApi.createBatch(automatic,catalog,noPrefix.connection(),new TestManager(noPrefix),counter());
        check(((List<?>)automaticResult.get("codes")).stream().allMatch(code->code.toString().length()==16),"Prefix is optional; default expiry and currency rewards are supported");
        for(int invalid=0;invalid<10;invalid++) {
            JsonObject b=payload();switch(invalid){case 0->b.addProperty("prefix","<bad>");case 1->b.addProperty("prefix","x".repeat(33));case 2->b.addProperty("countLeft",100);case 3->b.addProperty("count",1000);case 4->b.add("items",new JsonArray());case 5->b.getAsJsonArray("items").get(0).getAsJsonObject().addProperty("id",9999);case 6->b.getAsJsonArray("items").get(0).getAsJsonObject().addProperty("quantity",0);case 7->b.addProperty("expired","2020-01-01T00:00:00");case 8->b.add("prefix",JsonNull.INSTANCE);case 9->b.addProperty("expired","bad");}
            Memory bad=new Memory();try{GiftcodeApi.createBatch(b,catalog,bad.connection(),new TestManager(bad),counter());throw new AssertionError("Invalid batch accepted");}catch(PanelSupport.PanelException expected){check(bad.saved.isEmpty()&&bad.commits==0,"Invalid request makes no writes: "+invalid);}
        }
        Method random=GiftcodeApi.class.getDeclaredMethod("randomSuffix");random.setAccessible(true);Set<String> suffixes=new HashSet<>();
        for(int i=0;i<1000;i++){String s=(String)random.invoke(null);check(s.matches("[A-HJ-NP-Z2-9]{16}")&&suffixes.add(s),"Production generator produces unique long random suffixes");}
        http();System.out.println("PASS: "+checks+" batch creation, transactional rollback, uniqueness, live sync, global one-use redemption, concurrency/restart and authenticated API checks; no live DB access");
    }
    static void http()throws Exception {
        Memory db=new Memory();TestManager manager=new TestManager(db);var suffix=counter();var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,e)->{check(resource.equals("giftcode/batch"),"New resource routed");if(!e.getRequestMethod().equals("POST"))return GiftcodeApi.handle(resource,e);return GiftcodeApi.createBatch(PanelSupport.body(e),catalog,db.connection(),manager,suffix);},System::currentTimeMillis,false));server.start();
        try {
            var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();String base="http://127.0.0.1:"+server.getAddress().getPort()+"/api/panel/";
            var anonymous=client.send(HttpRequest.newBuilder(URI.create(base+"giftcode/batch")).POST(HttpRequest.BodyPublishers.ofString(payload().toString())).header("Content-Type","application/json").build(),HttpResponse.BodyHandlers.ofString());check(anonymous.statusCode()==401,"Batch requires panel login");
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"auth?action=login")).POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).header("Content-Type","application/json").build(),HttpResponse.BodyHandlers.ofString());String csrf=new JsonParser().parse(login.body()).getAsJsonObject().get("csrfToken").getAsString();
            var denied=client.send(HttpRequest.newBuilder(URI.create(base+"giftcode/batch")).POST(HttpRequest.BodyPublishers.ofString(payload().toString())).header("Content-Type","application/json").build(),HttpResponse.BodyHandlers.ofString());check(denied.statusCode()==403&&db.saved.isEmpty(),"Batch requires CSRF and rejection creates nothing");
            var wrong=client.send(HttpRequest.newBuilder(URI.create(base+"giftcode/batch")).GET().build(),HttpResponse.BodyHandlers.ofString());check(wrong.statusCode()==405,"GET cannot create giftcodes");
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"giftcode/batch")).POST(HttpRequest.BodyPublishers.ofString(payload().toString())).header("Content-Type","application/json").header("X-CSRF-Token",csrf).build(),HttpResponse.BodyHandlers.ofString());
            check(response.statusCode()==200&&db.saved.size()==50,"Authenticated POST creates one full batch");check(new JsonParser().parse(response.body()).getAsJsonObject().getAsJsonArray("codes").size()==50,"HTTP response returns all fifty codes for export");
        }finally{server.stop(0);}
    }
    static java.util.function.Supplier<String> counter(){var n=new AtomicInteger();return ()->String.format("%016d",n.incrementAndGet());}
    static JsonObject payload(){return new JsonParser().parse("{\"prefix\":\"HAN\",\"items\":[{\"id\":568,\"quantity\":75,\"options\":[{\"id\":50,\"param\":25}]}]}").getAsJsonObject();}
    static Map<String,Object> row(int id,String code,int count,String detail){var r=new LinkedHashMap<String,Object>();r.put("id",id);r.put("code",code);r.put("count_left",count);r.put("detail",detail);r.put("allGender","all");r.put("datecreate",Timestamp.valueOf(LocalDateTime.now()));r.put("expired",Timestamp.valueOf(LocalDateTime.now().plusDays(30)));r.put("player_name",null);return r;}
    static Player player(String name){var p=new Player();p.name=name;p.inventory.itemsBag.add(new Item());return p;}
    static GiftCode cloneGift(GiftCode g,int count){var c=new GiftCode();c.id=g.id;c.code=g.code;c.countLeft=count;c.dateexpired=g.dateexpired;c.allGender=g.allGender;c.detail.putAll(g.detail);c.option.putAll(g.option);return c;}
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    static final class TestManager extends GiftCodeManager {final Memory db;boolean failClaim;TestManager(Memory db){this.db=db;}@Override protected boolean reserveGiftCodeUse(GiftCode gift)throws Exception {if(failClaim)throw new SQLException("Fixture DB outage");return db.reserve(gift);}}
    static final class Memory {
        List<Map<String,Object>> saved=new ArrayList<>();int commits,rollbacks,failAt;boolean failCommit;
        synchronized int count(String code){return saved.stream().filter(r->r.get("code").equals(code)).mapToInt(r->((Number)r.get("count_left")).intValue()).findFirst().orElseThrow();}
        synchronized boolean reserve(GiftCode g){for(var r:saved)if(r.get("id").equals(g.id)&&r.get("code").toString().equalsIgnoreCase(g.code)&&((Number)r.get("count_left")).intValue()>0&&((Timestamp)r.get("expired")).getTime()>System.currentTimeMillis()){r.put("count_left",((Number)r.get("count_left")).intValue()-1);return true;}return false;}
        static List<Map<String,Object>> copy(List<Map<String,Object>> rows){return rows.stream().map(r->(Map<String,Object>)new LinkedHashMap<>(r)).collect(java.util.stream.Collectors.toCollection(ArrayList::new));}
        Connection connection(){class Tx{List<Map<String,Object>> rows=copy(saved);int inserts,lastId;}var tx=new Tx();
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                switch(m.getName()) {
                    case "setAutoCommit","close"->{return null;}
                    case "commit"->{if(failCommit)throw new SQLException("Fixture commit failure");saved=copy(tx.rows);commits++;return null;}
                    case "rollback"->{tx.rows=copy(saved);rollbacks++;return null;}
                    case "prepareStatement"->{String sql=(String)a[0];var params=new HashMap<Integer,Object>();return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(s,sm,sa)->{
                        switch(sm.getName()) {
                            case "setObject"->{params.put((Integer)sa[0],sa[1]);return null;}
                            case "close"->{return null;}
                            case "executeUpdate"->{check(sql.startsWith("INSERT INTO giftcode("),"Only expected insert mutation");if(++tx.inserts==failAt)throw new SQLException("Fixture insert failure");check(params.size()==7&&params.get(2).equals(1),"Every inserted row explicitly has one global use");tx.lastId=tx.rows.stream().mapToInt(r->((Number)r.get("id")).intValue()).max().orElse(0)+1;var r=row(tx.lastId,params.get(1).toString(),1,params.get(3).toString());r.put("allGender",params.get(4));r.put("datecreate",params.get(5));r.put("expired",params.get(6));r.put("player_name",params.get(7));tx.rows.add(r);return 1;}
                            case "getGeneratedKeys"->{return result(List.of(Map.of("id",tx.lastId)));}
                            case "executeQuery"->{if(sql.equals("SELECT code FROM giftcode FOR UPDATE"))return result(tx.rows);check(sql.startsWith("SELECT * FROM giftcode WHERE count_left>0"),"Stage active codes before commit");return result(tx.rows.stream().filter(r->((Number)r.get("count_left")).intValue()>0&&((Timestamp)r.get("expired")).getTime()>System.currentTimeMillis()).toList());}
                            default->throw new AssertionError("Unexpected statement method "+sm.getName());
                        }
                    });}
                    default->throw new AssertionError("Unexpected connection method "+m.getName());
                }
            });
        }
        ResultSet result(List<Map<String,Object>> rows){int[] index={-1};return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->{
            if(m.getName().equals("next"))return ++index[0]<rows.size();if(m.getName().equals("close"))return null;
            Object value=rows.get(index[0]).get(a[0] instanceof Integer?"id":a[0]);return switch(m.getName()){case "getInt"->((Number)value).intValue();case "getString"->value==null?null:value.toString();case "getTimestamp"->value;default->throw new AssertionError("Unexpected result method "+m.getName());};
        });}
    }
}
