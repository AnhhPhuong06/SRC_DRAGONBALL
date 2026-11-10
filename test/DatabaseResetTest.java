package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.DatabaseResetGuard;
import java.lang.reflect.*;
import java.lang.reflect.Proxy;
import java.net.*;
import java.net.http.*;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/** In-memory JDBC and isolated authenticated HTTP; never connects to any real database. */
public final class DatabaseResetTest {
    static int checks;static final Gson JSON=new Gson();
    static void check(boolean value,String message){checks++;if(!value)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception{sql();guard();http(false,false);http(true,false);http(false,true);System.out.println("PASS: "+checks+" reset scope, transaction/rollback, schema/FK/engine/trigger guards, connection/login drain, restart latch, authenticated HTTP, CSRF, exact confirmation, expiring one-use session-bound preview and concurrent reset checks; no live DB access");}
    static void sql()throws Exception {
        var db=new Memory();var original=new LinkedHashMap<>(db.saved);var snapshot=DatabaseResetStore.inspect(db.connection());
        check(snapshot.blockers().isEmpty()&&snapshot.tables().size()==4,"Existing InnoDB schema has four reset tables");
        var deleted=DatabaseResetStore.delete(db.connection(),snapshot);check(deleted.equals(Map.of("account",12L,"player",8L,"clan",3L,"player_gift_data",8L)),"Report exact deleted counts");
        check(db.commits==1&&db.rollbacks==0&&db.saved.entrySet().stream().allMatch(e->DatabaseResetStore.SCOPE.contains(e.getKey())?e.getValue()==0:e.getValue().equals(original.get(e.getKey()))),"One commit deletes all core data and only its explicit scope");
        check(db.deletes.indexOf("player")<db.deletes.indexOf("account"),"Child player deleted before account");check(db.isolation==Connection.TRANSACTION_SERIALIZABLE&&db.timeouts==4,"Serializable and bounded statements used");
        for(int bad=0;bad<6;bad++) {
            db=new Memory();switch(bad){case 0->db.engines.put("clan","MyISAM");case 1->db.engines.remove("account");case 2->db.edges.add(new Object[]{"fixture","billing","account"});case 3->db.triggers.add(new Object[]{"delete_extra","player"});case 4->db.edges.add(new Object[]{"fixture","account","player"});case 5->db.edges.add(new Object[]{"other_database","player","account"});}
            var blocked=DatabaseResetStore.inspect(db.connection());check(!blocked.blockers().isEmpty(),"Unsupported schema visibly blocks: "+bad);
            try{DatabaseResetStore.delete(db.connection(),blocked);throw new AssertionError("Blocked reset succeeded");}catch(PanelSupport.PanelException expected){check(expected.status==409&&db.deletes.isEmpty()&&db.commits==0,"Schema checks happen before DELETE: "+bad);}
        }
        db=new Memory();db.engines.remove("player_gift_data");db.saved.remove("player_gift_data");DatabaseResetStore.delete(db.connection(),DatabaseResetStore.inspect(db.connection()));check(db.deletes.size()==3,"Optional player benefit table need not exist");
        for(int fail=1;fail<=4;fail++) {
            db=new Memory();snapshot=DatabaseResetStore.inspect(db.connection());var before=new LinkedHashMap<>(db.saved);db.failDelete=fail;
            try{DatabaseResetStore.delete(db.connection(),snapshot);throw new AssertionError("Fault did not fail");}catch(SQLException expected){check(db.saved.equals(before)&&db.rollbacks==1&&db.commits==0,"Partial deletes roll back atomically: "+fail);}
        }
        db=new Memory();snapshot=DatabaseResetStore.inspect(db.connection());db.saved.put("account",13L);try{DatabaseResetStore.delete(db.connection(),snapshot);throw new AssertionError("Stale counts accepted");}catch(PanelSupport.PanelException expected){check(expected.status==409&&db.deletes.isEmpty(),"Changed counts invalidate reviewed preview");}
        db=new Memory();snapshot=DatabaseResetStore.inspect(db.connection());db.failCommit=true;try{DatabaseResetStore.delete(db.connection(),snapshot);throw new AssertionError("Commit failure ignored");}catch(DatabaseResetStore.UncertainCommit expected){check(db.rollbacks==1&&db.saved.get("account")==12,"Commit uncertainty reported and rollback attempted");}
        db=new Memory();snapshot=DatabaseResetStore.inspect(db.connection());db.failDelete=1;db.failRollback=true;try{DatabaseResetStore.delete(db.connection(),snapshot);throw new AssertionError("Rollback failure ignored");}catch(DatabaseResetStore.UncertainCommit expected){check(db.commits==0,"Rollback uncertainty reported");}
    }
    static void guard()throws Exception {
        var guard=new DatabaseResetGuard();try(var read=guard.read()){try{guard.exclusive(5);throw new AssertionError("Read to write upgrade permitted");}catch(SQLException expected){checks++;}}
        var opened=new AtomicInteger();try{guard.connection(()->{throw new SQLException("fixture pool unavailable");});throw new AssertionError("Failed open ignored");}catch(SQLException expected){}
        try(var write=guard.exclusive(20)){checks++;}
        Connection connection=guard.connection(()->{opened.incrementAndGet();return new Memory().connection();});var pool=Executors.newSingleThreadExecutor();
        try {
            Future<Boolean> blocked=pool.submit(()->{try(var write=guard.exclusive(25)){return false;}catch(SQLException expected){return true;}});check(blocked.get(2,TimeUnit.SECONDS),"Reset waits for an open account connection and times out without writing");
            connection.close();connection.close();
            var entered=new CountDownLatch(1);var release=new CountDownLatch(1);
            Future<?> writer=pool.submit(()->{try(var write=guard.exclusive(100)){entered.countDown();release.await();write.complete();}catch(Exception e){throw new RuntimeException(e);}});
            check(entered.await(2,TimeUnit.SECONDS),"Writer acquires after connection drains");
            var readPool=Executors.newSingleThreadExecutor();try{var next=readPool.submit(()->{try(var ignored=guard.read()){return false;}catch(SQLException expected){return "55000".equals(expected.getSQLState());}});check(!next.isDone(),"New login/connection waits during reset");release.countDown();writer.get(2,TimeUnit.SECONDS);check(next.get(2,TimeUnit.SECONDS),"Queued login/connection rejected after reset until restart");}finally{release.countDown();readPool.shutdownNow();}
            check(guard.completed(),"Completed latch survives lease release");try{guard.connection(()->{opened.incrementAndGet();return new Memory().connection();});throw new AssertionError("Pool opened after reset");}catch(SQLException expected){check(opened.get()==1,"Completed reset prevents pool acquisition entirely");}
        }finally{pool.shutdownNow();}
    }
    static void http(boolean uncertain,boolean cleanupFail)throws Exception {
        var db=new Memory();var guard=new DatabaseResetGuard();var clock=new AtomicLong(1_800_000_000_000L);var runtime=new TestRuntime();runtime.cleanupFail=cleanupFail;
        DatabaseResetStore.Store store=new DatabaseResetStore.Store(){
            public DatabaseResetStore.Snapshot preview()throws Exception{try(var c=guard.connection(db::connection)){return DatabaseResetStore.inspect(c);}}
            public Map<String,Long> erase(DatabaseResetStore.Snapshot expected)throws Exception{guard.requireExclusive();try(var c=db.connection()){return DatabaseResetStore.delete(c,expected);}}
        };
        var api=new DatabaseResetApi(guard,store,runtime,clock::get);var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);var workers=Executors.newCachedThreadPool();server.setExecutor(workers);
        server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password",(resource,e)->{check(resource.equals("database-reset"),"Reset resource dispatched");return api.request(e);},clock::get,false));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort()+"/api/panel/";var client=client();check(request(client,base,"database-reset","GET",null,null).statusCode()==401,"Preview requires authentication");
            String csrf=login(client,base),otherCsrf;var other=client();otherCsrf=login(other,base);
            var preview=object(request(client,base,"database-reset","GET",null,null));String token=preview.get("token").getAsString();
            check(preview.get("canReset").getAsBoolean()&&preview.getAsJsonArray("tables").size()==4&&db.deletes.isEmpty(),"Preview reviews counts and never deletes");
            check(request(client,base,"database-reset","POST",payload(token),null).statusCode()==403,"CSRF required");
            check(request(client,base,"database-reset","POST","{\"confirmation\":\"wrong\",\"token\":\""+token+"\"}",csrf).statusCode()==400,"Exact typed confirmation required");
            check(request(other,base,"database-reset","POST",payload(token),otherCsrf).statusCode()==409,"Preview token cannot be used by another panel session");
            check(request(client,base,"database-reset","DELETE",null,csrf).statusCode()==405,"DELETE cannot bypass reset flow");
            check(request(client,base,"database-reset?action=reset","GET",null,null).statusCode()==400,"GET parameters cannot request deletion");
            var arbitrary=new JsonParser().parse(payload(token)).getAsJsonObject();arbitrary.addProperty("table","giftcode");
            check(request(client,base,"database-reset","POST",arbitrary.toString(),csrf).statusCode()==400&&db.deletes.isEmpty(),"Cannot extend reset scope through a request field");
            clock.addAndGet(120001);check(request(client,base,"database-reset","POST",payload(token),csrf).statusCode()==409,"Expired review token rejected");
            for(var state:List.of(new DatabaseResetApi.RuntimeState(1,0,true,false),new DatabaseResetApi.RuntimeState(0,1,true,false),new DatabaseResetApi.RuntimeState(0,0,false,false),new DatabaseResetApi.RuntimeState(0,0,true,true))) {
                runtime.value=state;preview=object(request(client,base,"database-reset","GET",null,null));check(!preview.get("canReset").getAsBoolean(),"Runtime blocker shown");token=preview.get("token").getAsString();check(request(client,base,"database-reset","POST",payload(token),csrf).statusCode()==409&&db.deletes.isEmpty(),"Online players, authenticated sessions, startup and maintenance block writes");
            }
            runtime.value=new DatabaseResetApi.RuntimeState(0,0,true,false);preview=object(request(client,base,"database-reset","GET",null,null));token=preview.get("token").getAsString();db.saved.put("account",13L);
            check(request(client,base,"database-reset","POST",payload(token),csrf).statusCode()==409&&db.deletes.isEmpty()&&!guard.completed(),"Changed database counts abort before deleting");
            preview=object(request(client,base,"database-reset","GET",null,null));token=preview.get("token").getAsString();db.failDelete=2;
            check(request(client,base,"database-reset","POST",payload(token),csrf).statusCode()==503&&!guard.completed()&&db.saved.get("account")==13,"SQL delete failure rolls back and does not latch completed");db.failDelete=0;db.deletes.clear();
            preview=object(request(client,base,"database-reset","GET",null,null));token=preview.get("token").getAsString();db.failCommit=uncertain;
            final String body=payload(token);var futures=new ArrayList<CompletableFuture<HttpResponse<String>>>();
            for(int i=0;i<2;i++)futures.add(CompletableFuture.supplyAsync(()->{try{return request(client,base,"database-reset","POST",body,csrf);}catch(Exception ex){throw new CompletionException(ex);}}));
            var first=futures.get(0).get(5,TimeUnit.SECONDS);var second=futures.get(1).get(5,TimeUnit.SECONDS);
            int expected=uncertain?503:200;check((first.statusCode()==expected?1:0)+(second.statusCode()==expected?1:0)==1,"Concurrent double submit executes once");
            check(guard.completed()&&(uncertain?db.commits==0:db.commits==1),"Successful/uncertain commit holds restart latch");
            var finalState=object(request(client,base,"database-reset","GET",null,null));check(finalState.get("completed").getAsBoolean()&&finalState.get("restartRequired").getAsBoolean(),"Reset outcome remains available without reopening account DB");
            if(!uncertain){check(db.saved.get("account")==0&&db.saved.get("player")==0&&db.saved.get("clan")==0&&runtime.cleanups==1,"Core tables empty and cleanup called exactly once");if(cleanupFail)check(finalState.getAsJsonArray("warnings").size()==1,"Cache cleanup failure cannot hide committed delete");}
            check(request(client,base,"database-reset","POST",body,csrf).statusCode()==409,"Replay never executes another reset");
        }finally{server.stop(0);workers.shutdownNow();}
    }
    static HttpClient client(){return HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();}
    static String login(HttpClient client,String base)throws Exception{return object(request(client,base,"auth?action=login","POST","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",null)).get("csrfToken").getAsString();}
    static JsonObject object(HttpResponse<String> r){check(r.statusCode()==200,"Expected JSON success: "+r.statusCode()+" "+r.body());return new JsonParser().parse(r.body()).getAsJsonObject();}
    static String payload(String token){return JSON.toJson(Map.of("confirmation",DatabaseResetApi.CONFIRMATION,"token",token));}
    static HttpResponse<String> request(HttpClient c,String base,String path,String method,String body,String csrf)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+path)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).header("Content-Type","application/json");if(csrf!=null)b.header("X-CSRF-Token",csrf);return c.send(b.build(),HttpResponse.BodyHandlers.ofString());}
    static final class TestRuntime implements DatabaseResetApi.Runtime {DatabaseResetApi.RuntimeState value=new DatabaseResetApi.RuntimeState(0,0,true,false);int cleanups;boolean cleanupFail;public DatabaseResetApi.RuntimeState state(){return value;}public void afterCommit(){cleanups++;if(cleanupFail)throw new IllegalStateException("fixture cleanup failure");}}
    static final class Memory {
        Map<String,Long> saved=new LinkedHashMap<>(Map.of("account",12L,"player",8L,"clan",3L,"player_gift_data",8L,"giftcode",70L,"map_template",50L));
        Map<String,String> engines=new LinkedHashMap<>(Map.of("account","InnoDB","player","InnoDB","clan","InnoDB","player_gift_data","InnoDB"));
        List<Object[]> edges=new ArrayList<>(List.<Object[]>of(new Object[]{"fixture","player","account"})),triggers=new ArrayList<>();List<String> deletes=new ArrayList<>();int commits,rollbacks,isolation,timeouts,failDelete;boolean failCommit,failRollback;
        Connection connection(){class Tx{Map<String,Long> working=new LinkedHashMap<>(saved);}var tx=new Tx();return (Connection)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{Connection.class},(p,m,a)->{
            switch(m.getName()) {
                case "getCatalog"->{return "fixture";}case "close","setReadOnly"->{return null;}case "setAutoCommit"->{tx.working=new LinkedHashMap<>(saved);return null;}case "setTransactionIsolation"->{isolation=(Integer)a[0];return null;}
                case "commit"->{if(failCommit)throw new SQLException("fixture commit lost");saved=new LinkedHashMap<>(tx.working);commits++;return null;}
                case "rollback"->{rollbacks++;if(failRollback)throw new SQLException("fixture rollback lost");tx.working=new LinkedHashMap<>(saved);return null;}
                case "prepareStatement"->{String sql=(String)a[0];return Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{PreparedStatement.class},(sp,sm,sa)->{
                    switch(sm.getName()) {
                        case "close"->{return null;}case "setQueryTimeout"->{check(sa[0].equals(60),"Delete timeout configured");timeouts++;return null;}
                        case "executeQuery"->{
                            if(sql.startsWith("SELECT TABLE_NAME,ENGINE"))return rows(engines.entrySet().stream().map(e->new Object[]{e.getKey(),e.getValue()}).toList());
                            if(sql.startsWith("SELECT TABLE_SCHEMA,TABLE_NAME"))return rows(edges);
                            if(sql.startsWith("SELECT TRIGGER_NAME"))return rows(triggers);
                            if(sql.startsWith("SELECT COUNT(*) FROM `")){String name=sql.split("`")[1];return rows(List.<Object[]>of(new Object[]{tx.working.get(name)}));}
                            throw new AssertionError("Unexpected query "+sql);
                        }
                        case "executeUpdate"->{check(sql.startsWith("DELETE FROM `"),"Reset never uses TRUNCATE, DDL, foreign-key disabling or arbitrary SQL");String name=sql.split("`")[1];check(DatabaseResetStore.SCOPE.contains(name),"Delete stays in explicit scope");deletes.add(name);if(failDelete>0&&deletes.size()==failDelete)throw new SQLException("fixture delete failed");long count=tx.working.get(name);tx.working.put(name,0L);return (int)count;}
                        default->throw new AssertionError("Unexpected statement method "+sm.getName());
                    }
                });}
                default->throw new AssertionError("Unexpected connection method "+m.getName());
            }
        });}
        ResultSet rows(List<Object[]> rows){int[] index={-1};return (ResultSet)Proxy.newProxyInstance(getClass().getClassLoader(),new Class[]{ResultSet.class},(p,m,a)->switch(m.getName()){case "next"->++index[0]<rows.size();case "close"->null;case "getString"->String.valueOf(rows.get(index[0])[(Integer)a[0]-1]);case "getLong"->((Number)rows.get(index[0])[(Integer)a[0]-1]).longValue();default->throw new AssertionError("Unexpected result method "+m.getName());});}
    }
}
