package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.time.*;
import java.util.*;

public final class TopPanelDataTest {
    static int checks;static String base,csrf;static HttpClient client;
    static final long NOW=Instant.parse("2026-10-21T00:00:00Z").toEpochMilli();
    static final String GIFTS="[{\"top\":1,\"items\":[{\"id\":568,\"quantity\":10,\"options\":[{\"id\":50,\"param\":20}]}]},{\"top\":2,\"items\":[{\"id\":674,\"quantity\":5,\"options\":[]}]}]";
    static final TopPanelData.Catalog CATALOG=new TopPanelData.Catalog(List.of(Map.of("id",568,"name","Quả Trứng"),Map.of("id",674,"name","Đá ngũ sắc")),List.of(Map.of("id",50,"name","SĐ #%"),Map.of("id",200,"name","Trang Bị Pháp Sư # lần")));
    static TopPanelData.Row row(int id,boolean done,boolean auto,String users){return new TopPanelData.Row(id,new String[]{"Nạp","Sức Mạnh","Nhiệm Vụ"}[id],"SELECT id FROM player","2029-12-28T23:59:59",GIFTS,users,done,auto,2);}
    static final class Memory implements TopPanelData.Store {
        final Map<Integer,TopPanelData.Row> data=new LinkedHashMap<>();boolean failSave;
        Memory(){data.put(0,row(0,false,true,"[]"));data.put(1,row(1,false,false,"[]"));data.put(2,row(2,true,false,"[]"));}
        public List<TopPanelData.Row> list(){return new ArrayList<>(data.values());}
        public TopPanelData.Catalog catalog(){return CATALOG;}
        public List<Map<String,Object>> board(TopPanelData.Row row){return List.of(WorldData.dto("rank",1,"id",123,"name","Fixture player","gender",0,"score",row.id()==2?"NV 12 · bước 1":"123456","source","Fixture DB"));}
        public boolean edit(int id,String expected,TopPanelData.Edit edit)throws Exception {
            var old=data.get(id);if(old==null)throw PanelSupport.error(404,"Missing");
            if(!TopPanelData.version(old).equals(expected))throw PanelSupport.error(409,"Stale");
            var next=edit.apply(old);if(failSave)throw new java.sql.SQLException("Fixture failed write");data.put(id,next);return true;
        }
    }
    public static void main(String[] args)throws Exception {
        Memory store=new Memory();HttpServer server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,e)->{
            if(!resource.equals("top"))throw PanelSupport.error(404,"Fixture route");return TopPanelData.handle(e,store,()->NOW);
        },System::currentTimeMillis,false));server.start();
        base="http://127.0.0.1:"+server.getAddress().getPort();client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        try {
            expect(401,"GET","top",null,false);
            csrf=request("POST","auth?action=login","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",false).get("csrfToken").getAsString();
            expect(403,"POST","top?action=save","{}",false);expect(405,"DELETE","top","{}",true);
            var info=request("GET","top",null,true);
            check(info.getAsJsonArray("tops").size()==3,"Existing three server tops returned");
            check(info.getAsJsonArray("tops").get(0).getAsJsonObject().getAsJsonArray("ranking").size()==1,"Leaderboard returned");
            Files.writeString(Path.of("top-api-fixture.json"),info.toString());
            JsonObject input=payload(store.data.get(0));input.addProperty("name","Nạp <VIP>");
            request("POST","top?action=save",input.toString(),true);
            check(store.data.get(0).name().equals("Nạp <VIP>")&&store.data.get(0).query().equals("SELECT id FROM player")&&store.data.get(0).users().equals("[]"),"Save changes settings while preserving query and progress");
            expect(409,"POST","top?action=save",input.toString(),true);
            for(int invalid=0;invalid<8;invalid++) {
                var bad=payload(store.data.get(0));
                switch(invalid){
                    case 0->bad.addProperty("limit",0);
                    case 1->bad.addProperty("date","2020-01-01T00:00:00");
                    case 2->bad.getAsJsonArray("rewards").get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().addProperty("id",999);
                    case 3->bad.getAsJsonArray("rewards").get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().addProperty("quantity",0);
                    case 4->bad.getAsJsonArray("rewards").get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonArray("options").get(0).getAsJsonObject().addProperty("param",65536);
                    case 5->bad.getAsJsonArray("rewards").get(1).getAsJsonObject().addProperty("top",1);
                    case 6->bad.add("rewards",new JsonArray());
                    case 7->bad.getAsJsonArray("rewards").get(0).getAsJsonObject().getAsJsonArray("items").get(0).getAsJsonObject().getAsJsonArray("options").add(new JsonParser().parse("{\"id\":50,\"param\":1}"));
                }
                expect((invalid==1||invalid==2||invalid==6)?422:400,"POST","top?action=save",bad.toString(),true);
            }
            String unchanged=TopPanelData.version(store.data.get(0));store.failSave=true;
            expect(503,"POST","top?action=save",payload(store.data.get(0)).toString(),true);store.failSave=false;
            check(unchanged.equals(TopPanelData.version(store.data.get(0))),"Failed storage publishes no changes");
            request("POST","top?action=toggle",toggle(store.data.get(0),false).toString(),true);
            check(!store.data.get(0).auto(),"Pause persisted");
            var draft=payload(store.data.get(0));draft.add("rewards",new JsonArray());request("POST","top?action=save",draft.toString(),true);
            expect(422,"POST","top?action=toggle",toggle(store.data.get(0),true).toString(),true);
            draft=payload(store.data.get(0));draft.add("rewards",new JsonParser().parse(GIFTS));request("POST","top?action=save",draft.toString(),true);
            store.data.put(0,new TopPanelData.Row(0,"Nạp","SELECT id FROM player","2029-12-28T23:59:59",GIFTS,"[{\"id\":123,\"top\":1,\"log\":\"fixture\"}]",false,false,2));
            expect(409,"POST","top?action=save",payload(store.data.get(0)).toString(),true);
            request("POST","top?action=toggle",toggle(store.data.get(0),true).toString(),true);
            check(TopPanelData.recipientMap(TopPanelData.recipients(store.data.get(0).users())).equals(Map.of(123,1)),"Resume preserves recipient progress and accepts legacy log field");
            expect(409,"POST","top?action=new-cycle",payload(store.data.get(0)).toString(),true);
            expect(409,"POST","top?action=save",payload(store.data.get(2)).toString(),true);
            request("POST","top?action=new-cycle",payload(store.data.get(2)).toString(),true);
            check(!store.data.get(2).done()&&!store.data.get(2).auto()&&store.data.get(2).users().equals("[]"),"New campaign is explicitly paused and preserves history field");
            expect(409,"POST","top?action=award","{\"id\":0,\"version\":\"stale\"}",true);
            expect(409,"POST","top?action=award",toggle(store.data.get(0),true).toString(),true);
            var unknown=payload(store.data.get(0));unknown.addProperty("id",3);expect(400,"POST","top?action=save",unknown.toString(),true);
            check(TopPanelData.rewards(new JsonParser().parse(GIFTS).getAsJsonArray(),CATALOG).get(0).getAsJsonObject().has("top"),"Reward JSON uses TOPAUTO schema");
            System.out.println("PASS: "+checks+" authenticated top API, leaderboard, reward schema, limits, conflicts, failure, pause/resume and campaign checks");
        } finally {server.stop(0);}
    }
    static JsonObject payload(TopPanelData.Row row){var o=new JsonObject();o.addProperty("id",row.id());o.addProperty("version",TopPanelData.version(row));o.addProperty("name",row.name());o.addProperty("date",row.date());o.addProperty("limit",row.limit());o.add("rewards",new JsonParser().parse(row.items()));return o;}
    static JsonObject toggle(TopPanelData.Row row,boolean enabled){var o=new JsonObject();o.addProperty("id",row.id());o.addProperty("version",TopPanelData.version(row));o.addProperty("enabled",enabled);return o;}
    static JsonObject request(String method,String path,String body,boolean token)throws Exception{var response=send(method,path,body,token);check(response.statusCode()==200,path+" HTTP "+response.statusCode()+": "+response.body());return new JsonParser().parse(response.body()).getAsJsonObject();}
    static void expect(int status,String method,String path,String body,boolean token)throws Exception{var response=send(method,path,body,token);check(response.statusCode()==status,path+" expected "+status+", got "+response.statusCode()+": "+response.body());}
    static HttpResponse<String> send(String method,String path,String body,boolean token)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+"/api/panel/"+path)).header("Origin",base);if(body!=null)b.header("Content-Type","application/json");if(token)b.header("X-CSRF-Token",csrf);return client.send(b.method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).build(),HttpResponse.BodyHandlers.ofString());}
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
}
