import java.net.*;
import java.lang.reflect.*;
import network.Network;
import network.session.*;
import interfaces.*;
import server.Manager;
import server.ServerManager;
import sun.misc.Unsafe;

/** Real local admission/rejection and IP bookkeeping without ServerManager initialization. */
public final class AuditSessionLifecycleTest {
    static int checks;
    static void check(boolean ok,String label){checks++;if(!ok)throw new AssertionError(label);}
    public static void main(String[] args)throws Exception{
        Field unsafeField=Unsafe.class.getDeclaredField("theUnsafe");unsafeField.setAccessible(true);
        Unsafe unsafe=(Unsafe)unsafeField.get(null);
        ServerManager manager=(ServerManager)unsafe.allocateInstance(ServerManager.class);
        Method admit=ServerManager.class.getDeclaredMethod("canConnectWithIp",String.class);admit.setAccessible(true);
        boolean old=Manager.ENABLE_IP_LIMIT;int max=Manager.MAX_PER_IP;
        Manager.ENABLE_IP_LIMIT=true;Manager.MAX_PER_IP=2;
        String ip="audit-fixture-ip";
        try(var local=new ServerSocket(0,1,InetAddress.getLoopbackAddress());var peer=new Socket(InetAddress.getLoopbackAddress(),local.getLocalPort())){
            MySession session=new MySession(local.accept());session.ipAddress=ip;
            check((boolean)admit.invoke(manager,ip)&&(boolean)admit.invoke(manager,ip),"Two admitted connections");
            check(!(boolean)admit.invoke(manager,ip),"Third connection denied");
            try{Field counted=MySession.class.getField("countedConnection");((java.util.concurrent.atomic.AtomicBoolean)counted.get(session)).set(true);}
            catch(NoSuchFieldException baseline){}
            manager.disconnect(session);manager.disconnect(session);
            check(ServerManager.CLIENTS.containsKey(ip)&&ServerManager.CLIENTS.get(ip).get()==1,"Repeated disconnect only releases one admitted connection");
            session.disconnect();session.disconnect();
            check(!session.isConnected(),"Idempotent session disconnect");
        }finally{ServerManager.CLIENTS.remove(ip);Manager.ENABLE_IP_LIMIT=old;Manager.MAX_PER_IP=max;}
        Network network=Network.gI();
        try{
            network.init().setAcceptHandler(new ISessionAcceptHandler(){
                public void sessionInit(ISession session){session.disconnect();}
                public void sessionDisconnect(ISession session){session.disconnect();}
            }).start(0);
            Field listener=Network.class.getDeclaredField("serverSocketChannel");listener.setAccessible(true);
            var channel=(java.nio.channels.ServerSocketChannel)listener.get(network);
            int port=((InetSocketAddress)channel.getLocalAddress()).getPort();
            int before=SessionManager.gI().getNumSession();
            try(Socket socket=new Socket(InetAddress.getLoopbackAddress(),port)){
                socket.setSoTimeout(2000);check(socket.getInputStream().read()==-1,"Rejected socket closes");
            }
            Thread.sleep(50);
            check(SessionManager.gI().getNumSession()==before,"Rejected session never remains registered");
        }finally{network.close();}
        System.out.println("PASS: "+checks+" connection admission, double-release and session-registry regressions");
    }
}
