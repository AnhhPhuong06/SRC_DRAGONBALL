import java.io.*;
import java.net.*;
import java.util.*;
import models.player.Player;
import network.io.Message;
import network.session.MySession;
import server.Controller;
import server.Manager;
import services.ClientVersionPolicy;
import services.Service;

/** Exercises real handshake/login packets; LOCAL short-circuits allowed logins before any DB access. */
public final class ClientVersion003Test {
    static int checks;
    static void check(boolean value,String text){checks++;if(!value)throw new AssertionError(text);}
    static final List<String> BAD=Arrays.asList(null,"","0.0.0","0.0.1","0.0.2","0.0.4","0.0.30","0.0.03","00.00.03","0.03","003","3","2.4.0","2.3.7","0.0.3 "," 0.0.3","v0.0.3","0.0.3-beta","0.0.3\n","0.0.3\u0000","0.0.3|2.4.0");
    public static void main(String[] args)throws Exception {
        boolean old=Manager.LOCAL;Manager.LOCAL=true;
        try {
            check(ClientVersionPolicy.isAllowed("0.0.3"),"Exact release accepted");
            for(String bad:BAD)check(!ClientVersionPolicy.isAllowed(bad),"Only exact release allowed: "+bad);
            for(String bad:BAD)try(var s=session()) {
                s.isAdmin=true;s.version=240;s.login("fixture","unused",bad);denied(s,"Wrong/missing version blocked before admin/DB logic");
                s.clear();s.login("fixture","unused");denied(s,"Direct login cannot bypass verification by setting numeric version");
            }
            for(String bad:BAD)if(bad!=null)try(var s=session()) {
                handshake(s,"Pc platform xxx|"+bad);check(s.version==0,"Wrong handshake resets protocol version");denied(s,"Wrong handshake rejected");
                s.clear();loginPacket(s,"0.0.3",true);denied(s,"Correct login string cannot bypass wrong handshake");
            }
            try(var s=session()) {
                handshake(s,"Pc platform xxx|0.0.3");check(s.version==240&&!s.hasAllowedClientVersion(),"Handshake retains current packet layout but still requires login release");
                check(s.sent.stream().anyMatch(p->p.command()==-29),"Valid handshake still receives server list");
                s.clear();loginPacket(s,"0.0.3",true);accepted(s);
                var player=new Player();player.setSession(s);check(player.isVersionAbove(237)&&player.isVersionAbove(228),"Release 0.0.3 retains current inventory, skill and currency packet branches");
                s.clear();s.login("fixture","unused");accepted(s);
                s.clear();loginPacket(s,"2.4.0",true);denied(s,"Login also checks its own release despite valid handshake");check(!s.hasAllowedClientVersion(),"Rejected login revokes prior version proof");
                s.clear();s.login("fixture","unused");denied(s,"Internal relogin cannot reuse rejected login proof");
            }
            for(String platform:List.of("Pc platform xxx","Pc platform xxx|","Pc|0.0.3|extra","Pc|0.0.03"))try(var s=session()) {
                handshake(s,"Pc platform xxx|0.0.3");loginPacket(s,"0.0.3",true);check(s.hasAllowedClientVersion(),"Precondition valid session");s.clear();
                handshake(s,platform);denied(s,"Malformed/repeated handshake fails closed");check(!s.hasAllowedClientVersion()&&s.version==0,"Malformed handshake cannot retain previous proof");
            }
            byte[] header=handshakeBytes("Pc platform xxx|0.0.3");
            for(int cut=0;cut<header.length;cut++)try(var s=session()) {
                s.reportClientVersion("0.0.3");Service.gI().setClientType(s,new Message((byte)-29,Arrays.copyOf(header,cut)));denied(s,"Every truncated handshake rejected");check(s.version==0,"Truncation clears old parsed version");
            }
            try(var s=session()) {
                handshake(s,"Pc platform xxx|0.0.3");s.clear();loginPacket(s,null,false);check(s.disconnects==1&&!s.hasAllowedClientVersion(),"Legacy login omitting version disconnects and cannot log in");
            }
            for(boolean admin:new boolean[]{false,true})try(var s=session()) {
                s.isAdmin=admin;Controller.gI().createChar(s,new Message((byte)-28,new byte[0]));denied(s,"Character creation cannot bypass version gate");
            }
            for(String platform:List.of("Android","iPhone","Pc platform xxx"))try(var s=session()) {
                handshake(s,platform+"|0.0.3");s.clear();loginPacket(s,"0.0.3",true);accepted(s);
            }
            System.out.println("PASS: "+checks+" exact release, handshake/login packet, malformed/truncated payload, admin/direct-login/create-character rejection and protocol-240 compatibility checks; no DB access or live login");
        }finally{Manager.LOCAL=old;}
    }
    static void accepted(CaptureSession s){check(s.hasAllowedClientVersion()&&s.version==240,"Both release checks pass");check(s.notices().stream().anyMatch(t->t.contains("chỉ để lưu dữ liệu")),"Allowed login reaches existing LOCAL guard without DB");check(s.notices().stream().noneMatch(ClientVersionPolicy.DENIED_MESSAGE::equals),"Allowed version gets no update rejection");}
    static void denied(CaptureSession s,String text){check(!s.hasAllowedClientVersion()&&s.notices().contains(ClientVersionPolicy.DENIED_MESSAGE)&&s.player==null&&!s.joinedGame,text);}
    static void handshake(CaptureSession s,String platform)throws Exception{var out=new Message(-29);out.writer().writeByte(2);out.writer().write(handshakeBytes(platform));Controller.gI().messageNotLogin(s,new Message((byte)-29,out.getData()));}
    static byte[] handshakeBytes(String platform)throws Exception{var out=new Message(-29);var w=out.writer();w.writeByte(4);w.writeByte(2);w.writeBoolean(false);w.writeInt(1024);w.writeInt(600);w.writeBoolean(true);w.writeBoolean(true);w.writeUTF(platform);return out.getData();}
    static void loginPacket(CaptureSession s,String version,boolean hasVersion)throws Exception{var out=new Message(-29);out.writer().writeByte(0);out.writer().writeUTF("fixture");out.writer().writeUTF("unused");if(hasVersion){out.writer().writeUTF(version);out.writer().writeByte(0);}Controller.gI().messageNotLogin(s,new Message((byte)-29,out.getData()));}
    static CaptureSession session()throws Exception{try(var listener=new ServerSocket(0,1,InetAddress.getLoopbackAddress())){var peer=new Socket(InetAddress.getLoopbackAddress(),listener.getLocalPort());return new CaptureSession(listener.accept(),peer);}}
    record Packet(byte command,byte[] data){}
    static final class CaptureSession extends MySession implements AutoCloseable {
        final List<Packet> sent=new ArrayList<>();final Socket peer;int disconnects;
        CaptureSession(Socket socket,Socket peer){super(socket);this.peer=peer;}
        @Override public void sendMessage(Message message){sent.add(new Packet(message.command,message.getData().clone()));}
        @Override public void disconnect(){disconnects++;}
        List<String> notices(){var list=new ArrayList<String>();for(var p:sent)if(p.command()==-26)try{list.add(new DataInputStream(new ByteArrayInputStream(p.data())).readUTF());}catch(IOException e){throw new AssertionError(e);}return list;}
        void clear(){sent.clear();}
        @Override public void close()throws Exception{super.disconnect();peer.close();}
    }
}
