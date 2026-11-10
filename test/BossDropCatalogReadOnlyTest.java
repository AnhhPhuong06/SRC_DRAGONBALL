package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.AlyraManager;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;

public final class BossDropCatalogReadOnlyTest {
    public static void main(String[] args)throws Exception {
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/auth?action=login")).header("Content-Type","application/json").header("Origin",base).POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).build(),HttpResponse.BodyHandlers.ofString());
            if(login.statusCode()!=200)throw new AssertionError("Login failed");
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/boss-drop")).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new AssertionError("Real boss drop HTTP "+response.statusCode()+": "+response.body());
            var info=new JsonParser().parse(response.body()).getAsJsonObject();var catalog=info.getAsJsonObject("catalog");
            if(catalog.getAsJsonArray("bosses").size()<100||catalog.getAsJsonArray("items").size()<1000||catalog.getAsJsonArray("maps").size()<100)throw new AssertionError("Actual game catalog incomplete");
            if(Files.exists(Path.of("data/config/boss_drop_rules.json")))throw new AssertionError("GET created config");
            Files.writeString(Path.of("boss-drop-real-api-fixture.json"),info.toString());
            System.out.println("PASS: actual authenticated boss drop route; "+catalog.getAsJsonArray("bosses").size()+" real boss stages and full game catalog; GET/SELECT only");
        }finally{server.stop(0);AlyraManager.close();AlyraManager.shutdownDataPool();}
    }
}
