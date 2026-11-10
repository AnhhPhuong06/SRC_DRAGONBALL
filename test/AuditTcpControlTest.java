package network;

import java.net.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

/** Ephemeral local listener; player actions replaced with an in-memory counter. */
public final class AuditTcpControlTest {
    static final String TOKEN="test-only-random-token-01234567890123456789";
    static int checks;
    static void check(boolean ok,String text){checks++;if(!ok)throw new AssertionError(text);}
    static final class Fixture extends TcpKickServer {
        final AtomicInteger actions=new AtomicInteger();
        Fixture(int port,String token){super(port,token);setDaemon(true);}
        @Override protected boolean unstuckPlayer(String name){actions.incrementAndGet();return true;}
    }
    static Socket connect(int port)throws Exception{
        for(int i=0;i<50;i++)try {Socket s=new Socket(InetAddress.getLoopbackAddress(),port);s.setSoTimeout(4000);return s;}
        catch(ConnectException e){Thread.sleep(20);}
        throw new AssertionError("Listener did not bind");
    }
    static String request(int port,String json)throws Exception{
        try(Socket s=connect(port)){
            s.getOutputStream().write((json+"\n").getBytes(StandardCharsets.UTF_8));
            return new BufferedReader(new InputStreamReader(s.getInputStream(),StandardCharsets.UTF_8)).readLine();
        }
    }
    public static void main(String[] args)throws Exception{
        int port;try(ServerSocket s=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){port=s.getLocalPort();}
        try(Fixture listener=new Fixture(port,TOKEN)){
            listener.start();
            for(String json:new String[]{"{}","{\"action\":\"unstuck\",\"name\":\"fixture\"}","{\"token\":42}","{\"token\":\"wrong\"}"})
                check(request(port,json).contains("Không được phép"),"Unauthorized request rejected");
            check(listener.actions.get()==0,"Unauthorized commands have no player effects");
            check(request(port,"{\"token\":\""+TOKEN+"\",\"action\":\"probe\"}").contains("Hành động không hợp lệ"),"Authorized unknown action rejected");
            check(request(port,"{\"token\":\""+TOKEN+"\",\"action\":\"unstuck\",\"name\":\"fixture\"}").contains("Đã dịch chuyển"),"Authorized action accepted");
            check(listener.actions.get()==1,"One action exactly");
            try(Socket slow=connect(port)){
                long start=System.nanoTime();
                check(request(port,"{}").contains("Không được phép"),"Idle peer cannot block other clients");
                check((System.nanoTime()-start)/1000000<1500,"Other request finishes before idle timeout");
                check(slow.getInputStream().read()==-1,"Idle peer closed by timeout");
            }
            check(request(port,"x".repeat(1025))==null,"Oversized line closed");
            check(request(port,"bad json").contains("JSON không hợp lệ"),"Malformed JSON bounded");
        }
        Fixture disabled=new Fixture(port,null);disabled.start();disabled.join(1000);
        check(!disabled.isAlive(),"Missing token disables listener");
        System.out.println("PASS: "+checks+" TCP auth, limits, timeout and local action-counter regressions");
    }
}
