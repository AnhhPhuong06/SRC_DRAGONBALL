package network;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.*;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import server.Client;
import models.player.Player;
import services.Service;
import services.map.ChangeMapService;

/** Restricted control channel. Set NRO_CONTROL_TOKEN (32+ characters) before use. */
public class TcpKickServer extends Thread implements AutoCloseable {
    private static final int MAX_REQUEST_BYTES = 1024;
    private static final int READ_TIMEOUT_MS = 2000;
    private final int port;
    private final String token;
    private volatile boolean running = true;
    private volatile ServerSocket listener;
    private final java.util.Set<Socket> clients = ConcurrentHashMap.newKeySet();
    private final ThreadPoolExecutor workers = new ThreadPoolExecutor(2, 4, 30, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(16), task -> {
                Thread thread = new Thread(task, "TCP control request");
                thread.setDaemon(true);
                return thread;
            });

    public TcpKickServer(int port) {
        this(port, System.getenv("NRO_CONTROL_TOKEN"));
    }

    protected TcpKickServer(int port, String token) {
        this.port = port;
        this.token = token;
    }

    @Override
    public void run() {
        if (token == null || token.length() < 32) {
            System.err.println("TCP control disabled: set NRO_CONTROL_TOKEN to 32+ random characters.");
            workers.shutdown();
            return;
        }
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName("0.0.0.0"))) {
            listener = server;
            while (running) {
                Socket socket = server.accept();
                clients.add(socket);
                try {
                    socket.setSoTimeout(READ_TIMEOUT_MS);
                    workers.execute(() -> handleClient(socket));
                } catch (IOException | RejectedExecutionException error) {
                    clients.remove(socket);
                    socket.close();
                }
            }
        } catch (IOException error) {
            if (running) System.err.println("TCP control listener failed: " + error.getClass().getSimpleName());
        } finally {
            workers.shutdownNow();
            for (Socket socket : clients) {
                try { socket.close(); } catch (IOException ignored) {}
            }
            clients.clear();
            listener = null;
        }
    }

    private void handleClient(Socket socket) {
        try (socket;
             var output = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {
            String message = readRequest(socket);
            Object parsed = JSONValue.parse(message);
            if (!(parsed instanceof JSONObject json)) {
                output.println("JSON không hợp lệ.");
                return;
            }
            Object supplied = json.get("token");
            if (!(supplied instanceof String credential) || !MessageDigest.isEqual(
                    token.getBytes(StandardCharsets.UTF_8), credential.getBytes(StandardCharsets.UTF_8))) {
                output.println("Không được phép.");
                return;
            }
            Object action = json.get("action");
            Object name = json.get("name");
            if (!(action instanceof String command) || !"unstuck".equalsIgnoreCase(command)
                    || !(name instanceof String playerName) || playerName.isBlank() || playerName.length() > 64
                    || playerName.chars().anyMatch(Character::isISOControl)) {
                output.println("Hành động không hợp lệ hoặc thiếu tên người chơi.");
                return;
            }
            output.println(unstuckPlayer(playerName) ? "Đã dịch chuyển người chơi." : "Không tìm thấy người chơi.");
        } catch (IOException | RuntimeException error) {
            // The socket is always closed, including timeouts and oversized lines.
        } finally {
            clients.remove(socket);
        }
    }

    private static String readRequest(Socket socket) throws IOException {
        InputStream input = new BufferedInputStream(socket.getInputStream(), MAX_REQUEST_BYTES + 1);
        long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(READ_TIMEOUT_MS);
        var bytes = new ByteArrayOutputStream();
        while (true) {
            long remaining = deadline - System.nanoTime();
            if (remaining <= 0) throw new SocketTimeoutException("Request deadline exceeded");
            socket.setSoTimeout((int)Math.max(1, TimeUnit.NANOSECONDS.toMillis(remaining)));
            int value = input.read();
            if (value < 0 || value == '\n') break;
            if (bytes.size() >= MAX_REQUEST_BYTES) throw new IOException("Request too large");
            bytes.write(value);
        }
        return bytes.toString(StandardCharsets.UTF_8).trim();
    }

    protected boolean unstuckPlayer(String name) {
        Player player = Client.gI().getPlayer(name);
        if (player == null || player.gender < 0 || player.gender > 2) return false;
        ChangeMapService.gI().changeMapInYard(player, 21 + player.gender, -1, -1);
        Service.gI().sendThongBao(player, "Đã dịch chuyển về bản đồ an toàn.");
        return true;
    }

    @Override
    public void close() throws IOException {
        running = false;
        ServerSocket server = listener;
        if (server != null) server.close();
        workers.shutdownNow();
        for (Socket socket : clients) socket.close();
    }
}
