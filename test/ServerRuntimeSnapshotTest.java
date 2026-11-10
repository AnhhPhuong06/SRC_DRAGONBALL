package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import consts.BossStatus;
import java.lang.reflect.Field;
import java.net.*;
import java.net.http.*;
import java.util.*;
import managers.boss.BossManager;
import models.boss.Boss;
import models.boss.BossData;
import sun.misc.Unsafe;

/** A dormant boss has currentLevel=-1 before its first spawn. Snapshot reads must tolerate it. */
public final class ServerRuntimeSnapshotTest {
    private static int checks;
    public static void main(String[] args) throws Exception {
        Field field=Unsafe.class.getDeclaredField("theUnsafe");field.setAccessible(true);
        Unsafe unsafe=(Unsafe)field.get(null);
        // Install an empty client fixture without starting Client's background game thread.
        server.Client clientFixture=(server.Client)unsafe.allocateInstance(server.Client.class);
        Field players=server.Client.class.getDeclaredField("players");players.setAccessible(true);
        players.set(clientFixture,new java.util.concurrent.CopyOnWriteArrayList<>());
        Field instance=server.Client.class.getDeclaredField("instance");instance.setAccessible(true);instance.set(null,clientFixture);
        TestBoss boss=(TestBoss)unsafe.allocateInstance(TestBoss.class);
        BossData first=new BossData("Dormant boss",(byte)0,new short[]{123,124,125},1,new long[]{100},
                new int[]{1},new int[0][],new String[0],new String[0],new String[0],60);
        BossData second=new BossData("Boss level 2",(byte)0,new short[]{223,224,225},1,new long[]{100},
                new int[]{1},new int[0][],new String[0],new String[0],new String[0],60);
        data(boss,new BossData[]{first,second});boss.currentLevel=-1;boss.bossStatus=BossStatus.REST;boss.name="Dormant";boss.id=-123;
        try{boss.getHead();throw new AssertionError("Fixture must reproduce old getHead failure");}
        catch(IndexOutOfBoundsException expected){checks++;}
        BossManager.gI().addBoss(boss);
        try {
            check(ServerRuntimeApi.bossHead(boss)==123 && boss.currentLevel==-1,"Dormant head falls back without changing gameplay level");
            Map<String,Object> snapshot=(Map<String,Object>)ServerRuntimeApi.snapshot();
            check(((List<?>)snapshot.get("bossList")).size()==1,"Snapshot includes dormant boss");
            check(((Number)snapshot.get("ramTotalMb")).longValue()>0 && ((Number)snapshot.get("threads")).intValue()>0,
                    "Snapshot returns real process metrics");
            check(((Number)((Map<?,?>)snapshot.get("bosses")).get("resting")).intValue()==1,"Resting count is accurate");
            boss.currentLevel=1;check(ServerRuntimeApi.bossHead(boss)==223,"Active boss preserves current-level outfit");
            boss.currentLevel=2;check(ServerRuntimeApi.bossHead(boss)==123,"Finished-level transition does not crash");
            data(boss,new BossData[]{null,second});boss.currentLevel=-1;
            check(ServerRuntimeApi.bossHead(boss)==223,"Null level entry does not crash fallback");
            data(boss,null);check(ServerRuntimeApi.bossHead(boss)==-1,"Missing boss data uses missing-image marker");
            data(boss,new BossData[]{first,second});boss.currentLevel=-1;
            HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
            server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
            try {
                String base="http://127.0.0.1:"+server.getAddress().getPort();
                HttpClient client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
                var login=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/auth?action=login"))
                        .header("Content-Type","application/json").header("Origin",base)
                        .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).build(),HttpResponse.BodyHandlers.ofString());
                check(login.statusCode()==200,"Fixture auth succeeds");
                var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/server")).GET().build(),HttpResponse.BodyHandlers.ofString());
                check(response.statusCode()==200,"Actual authenticated server endpoint returns HTTP 200 with dormant boss");
                JsonObject json=new JsonParser().parse(response.body()).getAsJsonObject();
                check(json.getAsJsonArray("bossList").get(0).getAsJsonObject().get("head").getAsInt()==123,"HTTP DTO includes valid resting boss head");
                check(json.has("ramUsedMb")&&json.has("cpuPercent")&&json.has("uptimeText")&&json.has("playerList"),"Runtime DTO remains complete");
            } finally {server.stop(0);}
        } finally {BossManager.gI().removeBoss(boss);}
        System.out.println("PASS: "+checks+" dormant boss, runtime metrics and authenticated server endpoint checks");
    }
    private static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    private static void data(Boss boss,BossData[] values)throws Exception{Field field=Boss.class.getDeclaredField("data");field.setAccessible(true);field.set(boss,values);}
    private static class TestBoss extends Boss {private TestBoss()throws Exception{super(0,(BossData)null);}}
}
