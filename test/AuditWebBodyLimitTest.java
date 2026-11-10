import com.sun.net.httpserver.HttpServer;
import java.net.*;
import java.net.http.*;
import java.lang.reflect.*;
import server.web.WebAdminServer;
import server.web.panel.PanelSupport;

/** Actual legacy body reader behind an ephemeral loopback HTTP handler. */
public final class AuditWebBodyLimitTest {
    public static void main(String[] args)throws Exception{
        Method reader=WebAdminServer.class.getDeclaredMethod("readRequestBody",com.sun.net.httpserver.HttpExchange.class);
        reader.setAccessible(true);
        var server=HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(),0),0);
        server.createContext("/",exchange->{
            try{
                String body=(String)reader.invoke(null,exchange);
                exchange.sendResponseHeaders(200,-1);
            }catch(InvocationTargetException e){
                int status=e.getCause() instanceof PanelSupport.PanelException p?p.status:500;
                exchange.sendResponseHeaders(status,-1);
            }catch(Exception e){throw new java.io.IOException(e);}
            finally{exchange.close();}
        });
        server.start();
        try{
            var client=HttpClient.newHttpClient();
            URI uri=URI.create("http://localhost:"+server.getAddress().getPort()+"/");
            for(int length:new int[]{0,1024,1024*1024,1024*1024+1}){
                var response=client.send(HttpRequest.newBuilder(uri).timeout(java.time.Duration.ofSeconds(8))
                        .POST(HttpRequest.BodyPublishers.ofString("x".repeat(length))).build(),HttpResponse.BodyHandlers.discarding());
                if(response.statusCode()!=(length>1024*1024?413:200))throw new AssertionError("Legacy body limit not enforced: "+length+" returned "+response.statusCode());
            }
            var deep=client.send(HttpRequest.newBuilder(uri).POST(HttpRequest.BodyPublishers.ofString("[".repeat(65)+"0"+"]".repeat(65))).build(),HttpResponse.BodyHandlers.discarding());
            if(deep.statusCode()!=400)throw new AssertionError("Deep JSON must be rejected before recursive Gson parser");
            System.out.println("PASS: 5 legacy HTTP body/depth boundaries; ephemeral loopback only");
        }finally{server.stop(0);}
    }
}
