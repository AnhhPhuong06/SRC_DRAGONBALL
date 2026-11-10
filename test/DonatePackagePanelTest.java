package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.daos.DonatePackageDAO.Pack;
import database.daos.DonatePackageDAO.PackItem;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import static server.web.panel.CatalogShopApi.map;

/** Real authenticated router and donate handler with transactional, isolated JDBC. No live DB. */
public final class DonatePackagePanelTest {
    static int checks;
    static final CatalogData CATALOG = new CatalogData();
    static {
        for (int id : new int[]{457,568,674,1455,1962}) CATALOG.items.put(id, map("id", id,
                "name", switch(id){case 457->"Thỏi vàng";case 568->"Trứng Mabư";case 674->"Đá ngũ sắc";case 1455->"Đá pháp sư";default->"Pet Cửu Vĩ";}, "icon", id==674?6467:2164));
        for (int id : new int[]{30,50,77,103,201}) CATALOG.options.put(id, map("id",id,"name",id==50?"Sức đánh +#%":"Option #"));
    }
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    static JsonObject json(String value){return new JsonParser().parse(value).getAsJsonObject();}
    static JsonObject payload(){return json("{\"name\":\"Gói tân thủ\",\"detail\":\"Quà donate\\nNhận tại NPC\",\"costVnd\":10000,\"active\":true,\"sortOrder\":2,\"items\":[{\"itemId\":568,\"quantity\":75,\"options\":[{\"id\":50,\"param\":25}]}]}");}
    static void codec(){
        JsonArray items=payload().getAsJsonArray("items");String raw=DonatePackageApi.encode(items,CATALOG);
        check(raw.equals("568 75 50:25"),"Rewards use the game's TEXT format, not JSON");
        check(DonatePackageApi.decode(raw).equals(items),"Lossless reward quantity/options round trip");
        var legacy=DonatePackageApi.decode("457,2000,30:0;568 50:25\r\n674 200\n");
        check(legacy.size()==3&&legacy.get(1).getAsJsonObject().get("quantity").getAsInt()==1,"Legacy commas, semicolons, CRLF and implicit quantity supported");
        rejects(()->DonatePackageApi.decode("568 75 broken"),422,"Malformed legacy token visible, never silently ignored");
        rejects(()->DonatePackageApi.decode("568 1 50:25:9"),422,"Malformed option cannot be silently truncated");
        rejects(()->DonatePackageApi.encode(new JsonArray(),CATALOG),400,"Empty reward list rejected");
        for(String mutation:List.of("{\"itemId\":999,\"quantity\":1,\"options\":[]}","{\"itemId\":568,\"quantity\":0,\"options\":[]}","{\"itemId\":568,\"quantity\":1.5,\"options\":[]}","{\"itemId\":568,\"quantity\":2147483648,\"options\":[]}","{\"itemId\":568,\"quantity\":1,\"options\":[{\"id\":50,\"param\":-1}]}","{\"itemId\":568,\"quantity\":1,\"options\":[{\"id\":50,\"param\":65536}]}","{\"itemId\":568,\"quantity\":1,\"options\":[{\"id\":999,\"param\":1}]}","{\"itemId\":568,\"quantity\":1,\"options\":[{\"id\":50,\"param\":1},{\"id\":50,\"param\":2}]}","{\"itemId\":568,\"quantity\":1,\"options\":[null]}","{\"itemId\":568,\"quantity\":1,\"options\":[],\"untrusted\":true}")){
            rejects(()->DonatePackageApi.encode(new JsonParser().parse("["+mutation+"]").getAsJsonArray(),CATALOG),0,"Invalid item/quantity/options rejected");
        }
        var many=new JsonArray();for(int i=0;i<101;i++)many.add(items.get(0));rejects(()->DonatePackageApi.encode(many,CATALOG),400,"Reward list bounded");
        Pack a=pack(),b=pack();check(a.sameContents(b),"Unchanged reviewed purchase matches");b.costVnd++;check(!a.sameContents(b),"Stale price requires a fresh NPC confirmation");
        b=pack();b.items.get(0).quantity++;check(!a.sameContents(b),"Stale reward quantity requires fresh confirmation");b=pack();b.items.get(0).options.get(0)[1]++;check(!a.sameContents(b),"Stale options require fresh confirmation");
        check(!a.sameContents(null),"Disabled/deleted package cannot match a reviewed purchase");
    }
    static Pack pack(){var p=new Pack();p.id=1;p.name="Gói";p.detail="Quà";p.costVnd=10000;var i=new PackItem();i.tempId=568;i.quantity=75;i.options.add(new int[]{50,25});p.items.add(i);return p;}
    interface Task{void run()throws Exception;}
    static void rejects(Task task,int status,String message){try{task.run();throw new AssertionError(message);}catch(PanelSupport.PanelException e){check(status==0||e.status==status,message);}catch(Exception e){throw new RuntimeException(e);}}

    static HttpServer server(Memory db,Path webRoot)throws Exception{
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,e)->switch(resource){
            case "donate-package" -> DonatePackageApi.handle(e,db::connection,c->CATALOG);
            case "settings" -> Map.of("appName","NroHan");
            case "status" -> Map.of("port",14445,"playersOnline",0);
            default -> throw PanelSupport.error(404,"Fixture resource not found");
        },System::currentTimeMillis,false));
        if(webRoot!=null)server.createContext("/",e->{
            String path=e.getRequestURI().getPath();Path root=path.startsWith("/game-data/icon/")?webRoot.getParent().resolve("data/icon"):webRoot;
            String relative=path.startsWith("/game-data/icon/")?path.substring("/game-data/icon/".length()):path.equals("/")?"index.html":path.substring(1);
            Path file=root.resolve(relative).normalize();if(!file.startsWith(root)||!Files.isRegularFile(file)){e.sendResponseHeaders(404,-1);e.close();return;}
            String type=file.toString().endsWith(".js")?"text/javascript":file.toString().endsWith(".css")?"text/css":file.toString().endsWith(".png")?"image/png":"text/html";
            byte[] bytes=Files.readAllBytes(file);e.getResponseHeaders().set("Content-Type",type+"; charset=utf-8");e.sendResponseHeaders(200,bytes.length);try(var out=e.getResponseBody()){out.write(bytes);}
        });
        server.start();return server;
    }
    static String base;static HttpClient client;static String csrf;
    static HttpResponse<String> request(String resource,String method,String body,String token)throws Exception{
        var builder=HttpRequest.newBuilder(URI.create(base+resource)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).header("Content-Type","application/json");
        if(token!=null)builder.header("X-CSRF-Token",token);
        return client.send(builder.build(),HttpResponse.BodyHandlers.ofString());
    }
    static HttpResponse<String> post(String action,JsonObject body)throws Exception{return request("donate-package?action="+action,"POST",body.toString(),csrf);}
    static JsonObject info()throws Exception{var result=request("donate-package","GET",null,null);check(result.statusCode()==200,"List succeeds through actual router");return json(result.body());}
    static JsonObject saved(int id)throws Exception{for(var row:info().getAsJsonArray("packages")){var p=row.getAsJsonObject();if(p.get("id").getAsInt()==id)return p;}throw new AssertionError("Missing fixture package");}
    static JsonObject reference(JsonObject row){JsonObject r=new JsonObject();r.add("id",row.get("id"));r.add("revision",row.get("revision"));return r;}
    static void http()throws Exception{
        Memory db=new Memory();var server=server(db,null);base="http://127.0.0.1:"+server.getAddress().getPort()+"/api/panel/";client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
        try{
            check(request("donate-package","GET",null,null).statusCode()==401&&db.opens==0,"Anonymous reads do not reach database");
            check(request("donate-package?action=create","POST",payload().toString(),null).statusCode()==401&&db.opens==0,"Anonymous writes do not reach database");
            var login=request("auth?action=login","POST","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",null);check(login.statusCode()==200,"Fixture admin logs in");csrf=json(login.body()).get("csrfToken").getAsString();
            check(request("donate-package?action=create","POST",payload().toString(),null).statusCode()==403&&db.saved.isEmpty(),"CSRF required for donate mutations");
            check(request("donate-package?action=create","GET",null,null).statusCode()==400,"GET cannot invoke an action");
            check(request("donate-package","DELETE",null,csrf).statusCode()==405,"Unsupported HTTP method blocked");
            check(post("unknown",payload()).statusCode()==400,"Unknown action blocked");
            var created=post("create",payload());check(created.statusCode()==200,"Create succeeds");int id=json(created.body()).get("id").getAsInt();
            check(db.saved.get(id).get("items").equals("568 75 50:25")&&db.saved.get(id).get("cost_vnd").equals(10000L),"Database stores exact game reward format and VNĐ price");
            var row=saved(id);check(row.get("sold").getAsInt()==0&&row.get("revision").getAsString().length()==64&&row.get("warning").getAsString().isEmpty(),"List supplies sold count and version");
            check(info().getAsJsonObject("catalog").getAsJsonArray("items").size()==5,"Search/icon catalog included");
            var updated=payload();updated.add("id",row.get("id"));updated.add("revision",row.get("revision"));updated.addProperty("name","Gói vip");updated.addProperty("costVnd",20000);updated.getAsJsonArray("items").get(0).getAsJsonObject().addProperty("quantity",100);
            check(post("update",updated).statusCode()==200,"Update price, description, name and items");
            var before=new LinkedHashMap<>(db.saved.get(id));check(post("update",updated).statusCode()==409&&db.saved.get(id).equals(before),"Stale edit rolls back without overwriting");
            var toggle=reference(saved(id));toggle.addProperty("active",false);check(post("toggle",toggle).statusCode()==200&&(int)db.saved.get(id).get("active")==0,"Disable persists");
            check(post("toggle",toggle).statusCode()==409,"Stale toggle cannot overwrite newer state");
            toggle=reference(saved(id));toggle.addProperty("active",true);check(post("toggle",toggle).statusCode()==200,"Enable valid package");
            db.buyers.put(id,Set.of(123L,456L));row=saved(id);check(row.get("sold").getAsInt()==2,"Per-account purchase count shown");
            check(post("delete",reference(row)).statusCode()==409&&db.saved.containsKey(id)&&db.buyers.get(id).size()==2,"Sold package cannot be deleted; purchase history retained");
            toggle=reference(row);toggle.addProperty("active",false);check(post("toggle",toggle).statusCode()==200&&db.buyers.get(id).size()==2,"Hiding retains one-purchase history");
            var second=post("create",payload());int other=json(second.body()).get("id").getAsInt();check(other!=id,"New packages have distinct identities");
            check(post("delete",reference(saved(other))).statusCode()==200&&!db.saved.containsKey(other),"Unsold package can be deleted");
            var missing=reference(row);missing.addProperty("id",999);check(post("delete",missing).statusCode()==404,"Missing package rejected");
            for(String key:List.of("name","costVnd","items")){var bad=payload();bad.remove(key);check(post("create",bad).statusCode()==400,"Required field: "+key);}
            for(long cost:new long[]{0,-1,2147483648L}){var bad=payload();bad.addProperty("costVnd",cost);check(post("create",bad).statusCode()==400,"Game rejects invalid price: "+cost);}
            var bad=payload();bad.addProperty("name","x".repeat(101));check(post("create",bad).statusCode()==400,"Name length constrained");bad=payload();bad.addProperty("detail","x".repeat(501));check(post("create",bad).statusCode()==400,"Description length constrained");
            bad=payload();bad.addProperty("active","perhaps");check(post("create",bad).statusCode()==400,"Invalid active flag rejected");bad=payload();bad.addProperty("sortOrder",1.5);check(post("create",bad).statusCode()==400,"Fractional order rejected");
            bad=payload();bad.addProperty("accountId",1);check(post("create",bad).statusCode()==400,"Purchase identity cannot be supplied by panel");
            bad=payload();bad.add("items",JsonNull.INSTANCE);check(post("create",bad).statusCode()==400,"Null reward payload rejected");
            int count=db.saved.size();db.failCommit=true;check(post("create",payload()).statusCode()==503&&db.saved.size()==count,"Commit failure rolls back and reports failure");
            db.saved.get(id).put("items","568 75 50:25\nBROKEN");row=saved(id);check(!row.get("warning").getAsString().isEmpty()&&row.get("rawItems").getAsString().contains("BROKEN"),"Malformed old package does not block whole list and exposes raw warning");
            toggle=reference(row);toggle.addProperty("active",true);check(post("toggle",toggle).statusCode()==422,"Cannot enable malformed package");toggle.addProperty("active",false);check(post("toggle",toggle).statusCode()==200,"Malformed package can still be hidden");
            row=saved(id);updated=payload();updated.add("id",row.get("id"));updated.add("revision",row.get("revision"));check(post("update",updated).statusCode()==200&&saved(id).get("warning").getAsString().isEmpty(),"Malformed old rewards can be repaired explicitly");
            check(db.buyers.get(id).equals(Set.of(123L,456L))&&db.historyWrites==0,"No edits alter historical purchase rows");
            check(db.rowLocks>0&&db.rollbacks>0,"Updates use row locks and rejected transactions roll back");
        }finally{server.stop(0);}
    }
    public static void main(String[] args)throws Exception{
        if(args.length>0&&args[0].equals("serve")){
            Memory db=new Memory();db.saved.put(1,map("id",1,"name","Gói đã mua","detail","Lịch sử được giữ lại","cost_vnd",10000L,"items","674 200 30:0","active",1,"sort_order",0));db.buyers.put(1,Set.of(99L));db.nextId=2;
            var server=server(db,Path.of(args[1]).toAbsolutePath().normalize());System.out.println("FIXTURE_PORT="+server.getAddress().getPort());new CountDownLatch(1).await();return;
        }
        codec();http();System.out.println("PASS: "+checks+" donate CRUD, exact game TEXT codec, validation, stale purchases, auth/CSRF, versions, sold history and transaction rollback checks; no live DB access");
    }

    static final class Memory {
        Map<Integer,Map<String,Object>> saved=new LinkedHashMap<>();Map<Integer,Set<Long>> buyers=new HashMap<>();int nextId=1,opens,commits,rollbacks,rowLocks,historyWrites;boolean failCommit;
        static Map<Integer,Map<String,Object>> copy(Map<Integer,Map<String,Object>> rows){var result=new LinkedHashMap<Integer,Map<String,Object>>();rows.forEach((id,row)->result.put(id,new LinkedHashMap<>(row)));return result;}
        Connection connection(){opens++;class Tx{Map<Integer,Map<String,Object>> rows=copy(saved);int generated;}var tx=new Tx();
            return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
                switch(m.getName()){
                    case "setAutoCommit","close" -> {return null;}
                    case "commit" -> {if(failCommit){failCommit=false;throw new SQLException("Fixture commit failure","08001");}saved=copy(tx.rows);commits++;return null;}
                    case "rollback" -> {rollbacks++;tx.rows=copy(saved);return null;}
                    case "prepareStatement" -> {String sql=(String)a[0];if(sql.contains("FOR UPDATE"))rowLocks++;var params=new HashMap<Integer,Object>();return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(ps,pm,pa)->{
                        switch(pm.getName()){
                            case "setObject" -> {params.put((int)pa[0],pa[1]);return null;}
                            case "close" -> {return null;}
                            case "getGeneratedKeys" -> {return result(List.of(map("id",tx.generated)));}
                            case "executeQuery" -> {
                                if(sql.startsWith("SELECT p.id")){List<Map<String,Object>> rows=new ArrayList<>();tx.rows.values().stream().sorted(Comparator.comparingInt((Map<String,Object> r)->(int)r.get("sort_order")).thenComparingInt(r->(int)r.get("id"))).forEach(r->{var row=new LinkedHashMap<>(r);row.put("sold",(long)buyers.getOrDefault((int)r.get("id"),Set.of()).size());rows.add(row);});return result(rows);}
                                if(sql.startsWith("SELECT id,name")){var row=tx.rows.get((int)params.get(1));return result(row==null?List.of():List.of(row));}
                                if(sql.startsWith("SELECT account_id"))return result(buyers.getOrDefault((int)params.get(1),Set.of()).stream().map(id->map("account_id",id)).toList());
                                throw new AssertionError("Unexpected query: "+sql);
                            }
                            case "executeUpdate" -> {
                                if(sql.startsWith("INSERT INTO donate_package(")){int id=nextId++;tx.generated=id;tx.rows.put(id,map("id",id,"name",params.get(1),"detail",params.get(2),"cost_vnd",params.get(3),"items",params.get(4),"active",params.get(5),"sort_order",params.get(6)));return 1;}
                                if(sql.startsWith("UPDATE donate_package SET name=")){var row=tx.rows.get((int)params.get(7));String[] columns={"name","detail","cost_vnd","items","active","sort_order"};for(int i=0;i<columns.length;i++)row.put(columns[i],params.get(i+1));return 1;}
                                if(sql.startsWith("UPDATE donate_package SET active=")){tx.rows.get((int)params.get(2)).put("active",params.get(1));return 1;}
                                if(sql.startsWith("DELETE FROM donate_package WHERE"))return tx.rows.remove((int)params.get(1))==null?0:1;
                                historyWrites++;throw new AssertionError("Unexpected mutation: "+sql);
                            }
                        }throw new AssertionError("Unexpected statement method "+pm.getName());
                    });}
                }throw new AssertionError("Unexpected connection method "+m.getName());
            });
        }
        static ResultSet result(List<Map<String,Object>> rows){int[] index={-1};return (ResultSet)Proxy.newProxyInstance(Memory.class.getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->{
            if(m.getName().equals("next"))return ++index[0]<rows.size();if(m.getName().equals("close"))return null;
            Map<String,Object> row=rows.get(index[0]);Object v=a[0] instanceof Integer?new ArrayList<>(row.values()).get((int)a[0]-1):row.get(a[0]);
            return switch(m.getName()){case "getInt"->((Number)v).intValue();case "getLong"->((Number)v).longValue();case "getString"->v==null?null:String.valueOf(v);default->throw new AssertionError(m.getName());};
        });}
    }
}
