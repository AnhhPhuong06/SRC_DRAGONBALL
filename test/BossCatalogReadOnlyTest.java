package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.AlyraManager;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;

/** Default router and actual map SELECT; no game state/database mutations. */
public final class BossCatalogReadOnlyTest {
    public static void main(String[] args)throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();
            var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/auth?action=login"))
                    .header("Content-Type","application/json").header("Origin",base)
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).build(),HttpResponse.BodyHandlers.ofString());
            if(login.statusCode()!=200)throw new AssertionError("Login failed");
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/boss")).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new AssertionError("Boss HTTP "+response.statusCode());
            var info=new JsonParser().parse(response.body()).getAsJsonObject();
            if(info.getAsJsonArray("templates").size()!=170 || info.getAsJsonArray("maps").size()!=190 || !info.get("mapWarning").getAsString().isEmpty())throw new AssertionError("Catalog incomplete");
            if(Files.exists(Path.of("data/config/boss_settings.json")))throw new AssertionError("GET wrote production-like config");
            System.out.println("PASS: real authenticated boss endpoint HTTP 200, 170 boss stages, 190 DB maps; no database writes");
        } finally {server.stop(0);AlyraManager.close();AlyraManager.shutdownDataPool();}
    }
}
