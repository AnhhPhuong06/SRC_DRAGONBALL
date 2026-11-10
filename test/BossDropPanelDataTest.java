package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.util.*;
import services.BossDropRules;

public final class BossDropPanelDataTest {
    static String base,csrf;static HttpClient client;static int checks;
    public static void main(String[] args)throws Exception {
        var store=new BossDropRules(Path.of("data/config/panel_boss_drop.json"));
        var catalog=new BossDropPanelData.Catalog(List.of(Map.of("id",1,"name","Đồi hoa cúc")),
                List.of(Map.of("key","FIDE_DAI_CA_1","name","Fide 1"),Map.of("key","BLACK_GOKU","name","Black Goku")),
                List.of(Map.of("id",568,"name","Quả Trứng","icon",2164),Map.of("id",674,"name","Đá ngũ sắc","icon",6467)),
                List.of(Map.of("id",50,"name","SĐ #%"),Map.of("id",205,"name","Siêu hóa #/20 lần")));
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,exchange)->{
            if(!resource.equals("boss-drop"))throw PanelSupport.error(404,"Fixture route");return BossDropPanelData.handle(exchange,store,()->catalog);
        },System::currentTimeMillis,false));server.start();
        base="http://127.0.0.1:"+server.getAddress().getPort();client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        try {
            send(401,"GET","boss-drop",null,false);
            csrf=send(200,"POST","auth?action=login","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",false).get("csrfToken").getAsString();
            send(403,"POST","boss-drop?action=rule-save","{}",false);send(405,"PUT","boss-drop","{}",true);
            var empty=send(200,"GET","boss-drop",null,true);Files.writeString(Path.of("boss-drop-api-fixture.json"),empty.toString());
            check(empty.getAsJsonObject("catalog").getAsJsonArray("bosses").size()==2&&!Files.exists(Path.of("data/config/panel_boss_drop.json")),"Catalog GET does not write config");
            var rule=new JsonParser().parse("{\"id\":0,\"revision\":0,\"name\":\"Trứng Fide\",\"enabled\":true,\"mapId\":1,\"bossKey\":\"FIDE_DAI_CA_1\",\"itemId\":568,\"chance\":1.25,\"minQuantity\":1,\"maxQuantity\":3,\"options\":[{\"id\":50,\"param\":12}]}").getAsJsonObject();
            send(200,"POST","boss-drop?action=rule-save",rule.toString(),true);var info=send(200,"GET","boss-drop",null,true);
            check(info.getAsJsonArray("rules").get(0).getAsJsonObject().get("icon").getAsInt()==2164,"Rule DTO includes actual game icon");
            check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->0,(a,b)->a).size()==1,"API save applies to actual roll");
            send(409,"POST","boss-drop?action=rule-save",rule.toString(),true);rule.addProperty("id",1);rule.addProperty("revision",1);
            byte[] old=Files.readAllBytes(Path.of("data/config/panel_boss_drop.json"));
            for(String field:new String[]{"mapId","itemId","bossKey"}){var bad=copy(rule);if(field.equals("bossKey"))bad.addProperty(field,"UNKNOWN");else bad.addProperty(field,999);send(422,"POST","boss-drop?action=rule-save",bad.toString(),true);}
            for(double chance:new double[]{-1,101,0.00001}){var bad=copy(rule);bad.addProperty("chance",chance);send(400,"POST","boss-drop?action=rule-save",bad.toString(),true);}
            var bad=copy(rule);bad.addProperty("maxQuantity",0);send(400,"POST","boss-drop?action=rule-save",bad.toString(),true);
            bad=copy(rule);bad.getAsJsonArray("options").add(bad.getAsJsonArray("options").get(0));send(400,"POST","boss-drop?action=rule-save",bad.toString(),true);
            bad=copy(rule);bad.getAsJsonArray("options").get(0).getAsJsonObject().addProperty("id",204);send(422,"POST","boss-drop?action=rule-save",bad.toString(),true);
            check(Arrays.equals(old,Files.readAllBytes(Path.of("data/config/panel_boss_drop.json"))),"Rejected mutations leave file unchanged");
            rule.addProperty("enabled",false);send(200,"POST","boss-drop?action=rule-save",rule.toString(),true);
            check(store.roll(1,List.of("FIDE_DAI_CA_1"),()->0,(a,b)->a).isEmpty(),"Disable stops rolls");
            send(409,"POST","boss-drop?action=rule-delete","{\"id\":1,\"revision\":1}",true);
            send(200,"POST","boss-drop?action=rule-delete","{\"id\":1,\"revision\":2}",true);
            check(store.state().rules().isEmpty()&&new BossDropRules(Path.of("data/config/panel_boss_drop.json")).state().revision()==3,"Delete persists after restart");
            System.out.println("PASS: "+checks+" authenticated boss drop API, validation, revision and persistence checks");
        }finally{server.stop(0);}
    }
    static JsonObject copy(JsonObject o){return new JsonParser().parse(o.toString()).getAsJsonObject();}
    static JsonObject send(int status,String method,String resource,String body,boolean token)throws Exception{
        var req=HttpRequest.newBuilder(URI.create(base+"/api/panel/"+resource)).header("Origin",base).header("Content-Type","application/json");if(token)req.header("X-CSRF-Token",csrf);
        var response=client.send(req.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());
        check(response.statusCode()==status,resource+" expected "+status+", got "+response.statusCode()+": "+response.body());return new JsonParser().parse(response.body()).getAsJsonObject();
    }
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
}
