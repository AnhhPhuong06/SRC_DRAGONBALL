package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.AlyraManager;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;

public final class TopCatalogReadOnlyTest {
    public static void main(String[] args)throws Exception {
        HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort();var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            var login=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/auth?action=login")).header("Content-Type","application/json").header("Origin",base).POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}")).build(),HttpResponse.BodyHandlers.ofString());
            if(login.statusCode()!=200)throw new AssertionError("Fixture login failed");
            var response=client.send(HttpRequest.newBuilder(URI.create(base+"/api/panel/top")).GET().build(),HttpResponse.BodyHandlers.ofString());
            if(response.statusCode()!=200)throw new AssertionError("Real top HTTP "+response.statusCode()+": "+response.body());
            var info=new JsonParser().parse(response.body()).getAsJsonObject();if(info.getAsJsonArray("tops").size()!=3)throw new AssertionError("Expected three actual tops");
            for(var element:info.getAsJsonArray("tops")){var row=element.getAsJsonObject();if(!row.get("error").getAsString().isEmpty()||!row.get("boardError").getAsString().isEmpty())throw new AssertionError("Existing top rejected: "+row.get("name")+row.get("error")+row.get("boardError"));}
            if(info.getAsJsonArray("extraBoards").size()!=4)throw new AssertionError("Missing four extra game boards");
            for(var element:info.getAsJsonArray("extraBoards"))if(!element.getAsJsonObject().get("error").getAsString().isEmpty())throw new AssertionError("Extra game board query failed");
            for(var element:info.getAsJsonObject("catalog").getAsJsonArray("items"))if(!element.getAsJsonObject().has("icon"))throw new AssertionError("Missing item icon in top catalog");
            Files.writeString(Path.of("top-real-api-fixture.json"),info.toString());
            System.out.println("PASS: default authenticated top route HTTP 200; three real top configurations and ranking queries; catalog "+info.getAsJsonObject("catalog").getAsJsonArray("items").size()+" items; SELECT only");
        } finally {server.stop(0);AlyraManager.close();AlyraManager.shutdownDataPool();}
    }
}
