package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import consts.*;
import java.lang.reflect.Field;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import managers.boss.BossManager;
import models.boss.*;
import models.player.NPoint;
import services.BossSettings;
import sun.misc.Unsafe;

public final class BossPanelDataTest {
    private static int checks;
    private static String base,csrf;
    private static HttpClient client;
    public static void main(String[] args)throws Exception {
        var store=BossSettings.get();
        var defaults=store.config("BE_NA");
        Boss boss=fixture(-101,BossesData.BE_NA), dependent=fixture(-102,new BossData("Dependent",(byte)0,new short[]{1,2,3,-1,-1,-1},1,new long[]{100},new int[0],new int[0][],new String[0],new String[0],new String[0],AppearType.CALL_BY_ANOTHER));
        BossManager.gI().addBoss(boss);BossManager.gI().addBoss(dependent);
        boolean[] offline={false};
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,exchange)->{
            if(!resource.equals("boss"))throw PanelSupport.error(404,"Fixture route");
            return BossPanelData.handle(exchange,store,()->{if(offline[0])throw new java.sql.SQLException("Fixture offline");return List.of(Map.of("id",1,"name","Đồi hoa cúc"),Map.of("id",47,"name","Rừng Karin"),Map.of("id",5,"name","Đảo Kame"));});
        },System::currentTimeMillis,false));server.start();
        base="http://127.0.0.1:"+server.getAddress().getPort();
        client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        try {
            expect(401,"GET","boss",null,false);
            csrf=request("POST","auth?action=login","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",false).get("csrfToken").getAsString();
            expect(403,"POST","boss?action=save","{}",false);
            expect(405,"DELETE","boss","{}",true);
            JsonObject info=request("GET","boss",null,true);
            check(info.getAsJsonArray("templates").size()==170,"All source stages available without constructing new bosses");
            check(info.getAsJsonArray("runtime").size()==2,"Runtime includes resting and dependent bosses");
            check(info.getAsJsonArray("runtime").get(0).getAsJsonObject().getAsJsonArray("templateKeys").get(0).getAsString().equals("BE_NA"),"Runtime linked to matching source stage");
            check(!Files.exists(Path.of("data/config/boss_settings.json")),"GET does not seed persistent settings");
            Files.writeString(Path.of("boss-api-fixture.json"),info.toString());
            JsonObject change=new JsonParser().parse(new Gson().toJson(defaults)).getAsJsonObject();
            change.addProperty("key","BE_NA");change.addProperty("revision",0);change.addProperty("name","Boss <new> %d");
            change.addProperty("dame",123456789012L);change.add("hp",new JsonParser().parse("[1234567890123,1234567890124]"));
            change.add("mapJoin",new JsonParser().parse("[1,5,47]"));change.addProperty("secondsRest",99);
            request("POST","boss?action=save",change.toString(),true);
            check(BossesData.BE_NA.getHp()[0]==1234567890123L && BossesData.BE_NA.getSecondsRest()==99,"HTTP edit updates original runtime BossData");
            check(boss.nPoint.hp==80 && boss.nPoint.dame==7,"Editing template keeps current fight HP and damage");
            check(Files.readString(Path.of("data/config/boss_settings.json")).contains("1234567890123"),"HTTP save persists exact long stats");
            check(store.config("BE_NA").mapJoin().equals(List.of(1,5,47)),"Existing ignored Kame entry does not block adding a normal map");
            expect(409,"POST","boss?action=save",change.toString(),true);
            change.addProperty("revision",1);
            for(String map:List.of("[999]","[5]")) {var invalid=copy(change);invalid.add("mapJoin",new JsonParser().parse(map));expect(422,"POST","boss?action=save",invalid.toString(),true);}
            var invalid=copy(change);invalid.add("hp",new JsonParser().parse("[1.1]"));expect(400,"POST","boss?action=save",invalid.toString(),true);
            invalid=copy(change);invalid.add("outfit",new JsonParser().parse("[1,2,3]"));expect(400,"POST","boss?action=save",invalid.toString(),true);
            invalid=copy(change);invalid.addProperty("name","%999999999d");expect(400,"POST","boss?action=save",invalid.toString(),true);
            expect(404,"POST","boss?action=reset","{\"key\":\"../other\",\"revision\":1}",true);
            request("POST","boss?action=reset","{\"key\":\"BE_NA\",\"revision\":1}",true);
            check(store.config("BE_NA").equals(defaults),"HTTP reset restores original source values");
            offline[0]=true;
            info=request("GET","boss",null,true);
            check(!info.get("mapWarning").getAsString().isEmpty() && info.getAsJsonArray("templates").size()==170,"DB map outage does not blank boss management");
            request("POST","boss?action=summon","{\"id\":-101}",true);
            check(boss.bossStatus==BossStatus.RESPAWN && boss.lastTimeRest==0,"Summon uses natural manager respawn state");
            expect(409,"POST","boss?action=summon","{\"id\":-101}",true);
            expect(409,"POST","boss?action=summon","{\"id\":-102}",true);
            expect(404,"POST","boss?action=summon","{\"id\":-999}",true);
            System.out.println("PASS: "+checks+" authenticated boss API, validation, persistence, snapshot and summon checks");
        } finally {server.stop(0);BossManager.gI().removeBoss(boss);BossManager.gI().removeBoss(dependent);}
    }
    static Boss fixture(long id,BossData data)throws Exception {
        Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);var u=(Unsafe)f.get(null);
        Boss boss=(Boss)u.allocateInstance(Boss.class);Field field=Boss.class.getDeclaredField("data");field.setAccessible(true);field.set(boss,new BossData[]{data});
        boss.id=id;boss.name="Resting fixture";boss.currentLevel=-1;boss.bossStatus=BossStatus.REST;
        boss.nPoint=new NPoint(boss);boss.nPoint.hp=80;boss.nPoint.hpMax=100;boss.nPoint.dame=7;return boss;
    }
    static JsonObject copy(JsonObject value){return new JsonParser().parse(value.toString()).getAsJsonObject();}
    static JsonObject request(String method,String path,String body,boolean token)throws Exception{var response=send(method,path,body,token);check(response.statusCode()==200,path+" HTTP "+response.statusCode()+": "+response.body());return new JsonParser().parse(response.body()).getAsJsonObject();}
    static void expect(int status,String method,String path,String body,boolean token)throws Exception{var r=send(method,path,body,token);check(r.statusCode()==status,path+" expected "+status+", got "+r.statusCode()+": "+r.body());}
    static HttpResponse<String> send(String method,String path,String body,boolean token)throws Exception{
        var b=HttpRequest.newBuilder(URI.create(base+"/api/panel/"+path)).header("Origin",base);
        if(body!=null)b.header("Content-Type","application/json");if(token)b.header("X-CSRF-Token",csrf);
        return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
    }
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
