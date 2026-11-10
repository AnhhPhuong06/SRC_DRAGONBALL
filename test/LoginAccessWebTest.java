package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import server.Manager;
import services.LoginAccessConfig;

/** Real authenticated routing and config persistence in a temporary working directory, no DB. */
public final class LoginAccessWebTest {
    static int checks;static Path file=Path.of("data/config/alyra.properties");
    static void check(boolean ok,String message){checks++;if(!ok)throw new AssertionError(message);}
    public static void main(String[] args)throws Exception {
        if(Files.exists(Path.of("src")))throw new AssertionError("Use an isolated working directory");
        Files.createDirectories(file.getParent());String original="# fixture only\r\nserver.onlyadmin=false\r\nserver.debug=0\r\ndatabase.pass=dummy:a=b\\\\keep\r\nother.flag=unchanged\r\n";Files.writeString(file,original,StandardCharsets.ISO_8859_1);LoginAccessConfig.load();
        var server=HttpServer.create(new InetSocketAddress("127.0.0.1",0),0);server.createContext("/api/panel/",new PanelRouter("fixture-admin","fixture-password"));server.start();
        try {
            String base="http://127.0.0.1:"+server.getAddress().getPort()+"/api/panel/";var client=HttpClient.newBuilder().cookieHandler(new CookieManager(null,CookiePolicy.ACCEPT_ALL)).build();
            check(request(client,base,"server?action=login-access","GET",null,null).statusCode()==401,"Login-access read requires panel session");
            var login=request(client,base,"auth?action=login","POST","{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}",null);String csrf=json(login).get("csrfToken").getAsString();
            var state=json(request(client,base,"server?action=login-access","GET",null,null));check(!state.get("adminOnly").getAsBoolean()&&state.get("persistent").getAsBoolean(),"Lightweight GET returns current policy without initializing game/DB managers");
            check(request(client,base,"server?action=login-access","POST","{\"on\":true}",null).statusCode()==403,"CSRF required for policy change");
            for(String invalid:List.of("{}","{\"on\":null}","{\"on\":\"true\"}","{\"on\":1}","{\"on\":[]}","{\"on\":true,\"other\":1}")) {
                check(request(client,base,"server?action=login-access","POST",invalid,csrf).statusCode()==400,"Invalid policy payload rejected");check(Files.readString(file,StandardCharsets.ISO_8859_1).equals(original)&&!LoginAccessConfig.isAdminOnly(),"Rejected save preserves configuration and runtime");
            }
            check(request(client,base,"server?action=login-access&extra=1","GET",null,null).statusCode()==400,"Unknown query rejected");
            check(request(client,base,"server?action=login-access","DELETE",null,csrf).statusCode()==405,"Wrong method rejected");
            var enabled=json(request(client,base,"server?action=login-access","POST","{\"on\":true}",csrf));
            check(enabled.get("adminOnly").getAsBoolean()&&enabled.get("liveApplied").getAsBoolean()&&enabled.get("persistent").getAsBoolean(),"Enable response confirms persisted live setting");
            check(!LoginAccessConfig.canLogin(false)&&LoginAccessConfig.canLogin(true),"Enable blocks ordinary accounts, accepts admins");
            check(Files.readString(file,StandardCharsets.ISO_8859_1).equals(original.replace("server.onlyadmin=false","server.onlyadmin=true")),"Only intended property changes; credentials and other config preserved");
            Manager.ONLY_ADMIN=false;Manager.DEBUG=false;LoginAccessConfig.load();check(LoginAccessConfig.isAdminOnly(),"Saved restriction reloads after restart");
            Manager.ONLY_ADMIN=false;Manager.DEBUG=true;check(json(request(client,base,"server?action=login-access","GET",null,null)).get("adminOnly").getAsBoolean(),"Legacy debug-only restriction is reflected on web");
            var off=json(request(client,base,"server?action=login-access","POST","{\"on\":false}",csrf));check(!off.get("adminOnly").getAsBoolean()&&!Manager.DEBUG&&LoginAccessConfig.canLogin(false)&&LoginAccessConfig.canLogin(true),"Disable clears both legacy restrictions and allows both roles");
            Manager.ONLY_ADMIN=true;Manager.DEBUG=true;LoginAccessConfig.load();check(!LoginAccessConfig.isAdminOnly(),"Disabled policy also persists");
            LoginAccessConfig.setAdminOnly(true);Path moved=file.resolveSibling("fixture-backup.properties");Files.move(file,moved);
            try{check(request(client,base,"server?action=login-access","POST","{\"on\":false}",csrf).statusCode()==503&&LoginAccessConfig.isAdminOnly(),"Disk failure returns error and leaves live restriction enabled");}finally{Files.move(moved,file);}
            check(json(request(client,base,"server?action=login-access","GET",null,null)).get("adminOnly").getAsBoolean(),"Reload after failed write gives actual restriction");
            System.out.println("PASS: "+checks+" authenticated web routing, CSRF, strict payload, live role policy, persistence/reload, legacy debug, config preservation and disk-failure checks; no DB access or real setting changes");
        }finally{server.stop(0);}
    }
    static JsonObject json(HttpResponse<String> r){check(r.statusCode()==200,"Expected success "+r.statusCode()+" "+r.body());return new JsonParser().parse(r.body()).getAsJsonObject();}
    static HttpResponse<String> request(HttpClient c,String base,String path,String method,String body,String csrf)throws Exception{var b=HttpRequest.newBuilder(URI.create(base+path)).method(method,body==null?HttpRequest.BodyPublishers.noBody():HttpRequest.BodyPublishers.ofString(body)).header("Content-Type","application/json");if(csrf!=null)b.header("X-CSRF-Token",csrf);return c.send(b.build(),HttpResponse.BodyHandlers.ofString());}
}
