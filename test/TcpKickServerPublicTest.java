import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import network.TcpKickServer;

/** Starts only the socket listener on an ephemeral port; never sends a player action. */
public final class TcpKickServerPublicTest {
    public static void main(String[] args) throws Exception {
        InetAddress lan = null;
        var interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements() && lan == null) {
            var network = interfaces.nextElement();
            if (!network.isUp() || network.isLoopback()) continue;
            var addresses = network.getInetAddresses();
            while (addresses.hasMoreElements()) {
                var address = addresses.nextElement();
                if (address instanceof Inet4Address && !address.isLoopbackAddress() && !address.isLinkLocalAddress()) {
                    lan = address;
                    break;
                }
            }
        }
        if (lan == null) throw new AssertionError("A non-loopback IPv4 interface is required for this test");
        int port;
        try (var free = new ServerSocket(0)) { port = free.getLocalPort(); }
        var listener = new TcpKickServer(port);
        listener.setDaemon(true);
        listener.start();
        Exception last = null;
        for (int attempt = 0; attempt < 20; attempt++) {
            try (var socket = new Socket()) {
                socket.connect(new InetSocketAddress(lan, port), 500);
                socket.setSoTimeout(2000);
                var out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                String token = System.getenv("NRO_CONTROL_TOKEN");
                if (token == null || token.length() < 32) throw new AssertionError("Set a fixture NRO_CONTROL_TOKEN before this test");
                out.println("{\"action\":\"probe\",\"token\":\"" + token + "\"}");
                String response = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8)).readLine();
                if (response == null || !response.contains("Hành động không hợp lệ")) {
                    throw new AssertionError("Expected a harmless protocol rejection, got: " + response);
                }
                System.out.println("PASS: production SocketServer accepts a connection on " + lan.getHostAddress()
                        + ":" + port + "; no game/DB/player action was started");
                return;
            } catch (java.net.ConnectException error) {
                last = error;
                Thread.sleep(50);
            }
        }
        throw new AssertionError("Listener is unreachable through the non-loopback interface", last);
    }
}
