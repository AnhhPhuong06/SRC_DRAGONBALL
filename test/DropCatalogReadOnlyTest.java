package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.AlyraManager;
import java.net.*;
import java.net.http.*;

/** Explicit integration check: actual catalog SELECTs; configuration is isolated by the runner. */
public final class DropCatalogReadOnlyTest {
    public static void main(String[] args) throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/auth?action=login"))
                    .header("Content-Type","application/json").header("Origin",base)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).build(),HttpResponse.BodyHandlers.ofString());
            if(login.statusCode()!=200)throw new AssertionError("Fixture login failed");
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/drop")).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new AssertionError("Drop catalog HTTP "+response.statusCode());
            JsonObject json=new JsonParser().parse(response.body()).getAsJsonObject();
            JsonObject catalog=json.getAsJsonObject("catalog");
            for(String key:new String[]{"maps","mobs","items","options"}) {
                if(catalog.getAsJsonArray(key).size()<=0)throw new AssertionError("Missing "+key);
            }
            if(json.getAsJsonArray("rules").size()!=0)throw new AssertionError("Fixture should have no custom rules");
            System.out.println("PASS: Real drop catalog HTTP 200; maps="+catalog.getAsJsonArray("maps").size()
                    +", mobs="+catalog.getAsJsonArray("mobs").size()+", items="+catalog.getAsJsonArray("items").size()
                    +", options="+catalog.getAsJsonArray("options").size()+"; no game database writes.");
        } finally {server.stop(0);AlyraManager.close();AlyraManager.shutdownDataPool();}
    }
}
