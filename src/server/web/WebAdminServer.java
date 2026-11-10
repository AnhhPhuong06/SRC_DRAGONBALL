package server.web;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import server.web.panel.PanelSupport;
import database.daos.GiftDataDAO;
import database.daos.PlayerDAO;
import jdbc.DBConnecter;
import models.item.Item;
import models.player.Player;
import server.AutoSaveManager;
import server.Client;
import server.Maintenance;
import server.Manager;
import server.ServerManager;
import server.ui.ServerManagerUI;
import services.CombineRateConfig;
import services.ItemService;import services.Service;
import services.VongQuayThoiVangConfig;
import services.func.GiftMailService;
import utils.Logger;
import utils.TimeUtil;

import Bot.Bot;
import Bot.BotConfig;
import Bot.BotManager;
import consts.BossID;
import consts.BossStatus;
import interfaces.ISession;
import managers.GiftCodeManager;
import managers.boss.BossManager;
import models.boss.Boss;
import models.boss.BossData;
import models.boss.BossesData;
import models.Template.ItemOptionTemplate;
import models.Template.ItemTemplate;
import network.session.MySession;
import network.session.SessionManager;
import services.MobMeCleanerService;
import java.lang.reflect.Field;
import java.security.MessageDigest;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.Executors;

/**
 * Embedded Web Admin Server - Quản trị máy chủ Ngọc Rồng qua Web Browser.
 * Cung cấp toàn bộ tính năng của ServerManagerUI (Java Swing) qua giao diện Web hiện đại.
 */
public class WebAdminServer {

    private static final int DEFAULT_PORT = 8888;
    private static final String WEB_ROOT = "web";
    private static final Gson gson = new Gson();

    private static WebAdminServer instance;
    private HttpServer server;
    private java.util.concurrent.ExecutorService executor;
    private int port = DEFAULT_PORT;
    private String host = "127.0.0.1";
    private boolean isRunning = false;

    /** Token xác thực — sinh ngẫu nhiên mỗi lần khởi server, static để các handler static truy cập. */
    private static volatile String adminToken = "admintoken_" + UUID.randomUUID().toString().replace("-", "");
    /** Tên tài khoản admin — load từ config. */
    private static volatile String adminUsername = "admin";
    /** Mật khẩu admin — load từ config, bắt buộc phải có. */
    private static volatile String adminPassword = null;

    public static synchronized WebAdminServer gI() {
        if (instance == null) {
            instance = new WebAdminServer();
        }
        return instance;
    }

    private WebAdminServer() {
        loadConfig();
    }

    private void loadConfig() {
        File configFile = new File("data/config/web_admin.properties");
        if (configFile.exists()) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                Properties props = new Properties();
                props.load(fis);
                port = Integer.parseInt(props.getProperty("port", String.valueOf(DEFAULT_PORT)));
                if (port < 1 || port > 65535) throw new IllegalArgumentException("Invalid panel port");
                host = props.getProperty("host", "127.0.0.1").trim();
                if (host.isEmpty()) host = "127.0.0.1";
                String u = props.getProperty("username", "").trim();
                if (!u.isEmpty()) {
                    adminUsername = u;
                }
                String pw = props.getProperty("password", "").trim();
                if (!pw.isEmpty()) {
                    adminPassword = pw;
                }
            } catch (Exception e) {
                port = DEFAULT_PORT;
            }
        }
        boolean saveNeeded = false;
        if (adminUsername == null || adminUsername.isEmpty()) {
            adminUsername = "admin";
            saveNeeded = true;
        }
        // Nếu chưa có password trong config → tạo password ngẫu nhiên và ghi vào config
        if (adminPassword == null || adminPassword.isEmpty()) {
            adminPassword = "admin_" + UUID.randomUUID().toString().substring(0, 12);
            saveNeeded = true;
        }
        if (saveNeeded || !configFile.exists()) {
            try {
                configFile.getParentFile().mkdirs();
                Properties props = new Properties();
                props.setProperty("port", String.valueOf(port));
                props.setProperty("host", host);
                props.setProperty("username", adminUsername);
                props.setProperty("password", adminPassword);
                try (FileOutputStream fos = new FileOutputStream(configFile)) {
                    props.store(fos, "Web Admin Panel Config - DO NOT SHARE CREDENTIALS");
                }
            } catch (Exception ignored) {}
            System.out.println(">>> [WEB ADMIN] Tài khoản đã được lưu trong data/config/web_admin.properties");
            System.out.println(">>> [WEB ADMIN] Config saved to: data/config/web_admin.properties");
        }
    }

    // =========================================================================
    //  Auth: Kiểm tra token từ header Authorization: Bearer <token>
    // =========================================================================

    // Quản lý số lần đăng nhập sai theo IP để chống Brute Force (khóa 15 phút nếu sai 5 lần)
    private static final Map<String, long[]> loginAttempts = new java.util.concurrent.ConcurrentHashMap<>();

private static String getClientIp(HttpExchange exchange) {
        // Không tin X-Forwarded-For: client tự đặt header này được, dùng để bypass lockout
        java.net.InetSocketAddress addr = exchange.getRemoteAddress();
        return addr == null || addr.getAddress() == null ? "" : addr.getAddress().getHostAddress();
    }

    private static boolean isIpLocked(String ip) {
        long[] record = loginAttempts.get(ip);
        if (record == null) return false;
        long lockUntil = record[1];
        if (lockUntil > System.currentTimeMillis()) {
            return true;
        }
        if (lockUntil > 0 && lockUntil <= System.currentTimeMillis()) {
            loginAttempts.remove(ip);
            return false;
        }
        return false;
    }

    private static void recordFailedLogin(String ip) {
        long now = System.currentTimeMillis();
        loginAttempts.compute(ip, (k, v) -> {
            if (v == null) {
                return new long[]{1, 0};
            }
            v[0]++;
            if (v[0] >= 5) {
                v[1] = now + 15 * 60 * 1000L; // Khóa 15 phút
            }
            return v;
        });
    }

    private static void recordSuccessLogin(String ip) {
        loginAttempts.remove(ip);
    }

    /**
     * Kiểm tra xác thực cho mọi API request (trừ login).
     * @return true nếu xác thực thành công, false nếu đã trả lỗi 401
     */
    private static boolean checkAuth(HttpExchange exchange) throws IOException {
        String authHeader = exchange.getRequestHeaders().getFirst("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7).trim();
            if (MessageDigest.isEqual(token.getBytes(java.nio.charset.StandardCharsets.UTF_8), adminToken.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                return true;
            }
        }
        sendError(exchange, 401, "Phiên đăng nhập không hợp lệ hoặc đã hết hạn. Vui lòng đăng nhập lại.");
        return false;
    }


    /**
     * Wrap handler với kiểm tra xác thực bắt buộc.
     * Mọi request không có token hợp lệ sẽ bị trả 401 ngay lập tức.
     */
    private static HttpHandler authWrap(HttpHandler inner) {
        return exchange -> {
            if (handleOptions(exchange)) return;
            if (!checkAuth(exchange)) return;  // 401 đã được gửi
            inner.handle(exchange);
        };
    }

    public synchronized void start() {
        if (isRunning) return;
        try {
            server = HttpServer.create(new InetSocketAddress(host, port), 0);
            executor = new java.util.concurrent.ThreadPoolExecutor(4, 10, 60L, java.util.concurrent.TimeUnit.SECONDS,
                    new java.util.concurrent.ArrayBlockingQueue<>(200));
            server.setExecutor(executor);

            // Static assets and the new cookie-authenticated panel share this server.
            server.createContext("/", new server.web.panel.PanelStaticHandler());
            server.createContext("/game-data/", new server.web.panel.PanelStaticHandler());
            server.createContext("/api/panel", new server.web.panel.PanelRouter(adminUsername, adminPassword));
            server.createContext("/api/auth/login", new LoginHandler());

            // ── Protected (bắt buộc đăng nhập) ──
            server.createContext("/api/auth/verify", authWrap(new VerifyAuthHandler()));

            // ── Protected (bắt buộc đăng nhập) ──
            server.createContext("/api/dashboard", authWrap(new DashboardHandler()));
            server.createContext("/api/dashboard/action", authWrap(new DashboardActionHandler()));
            server.createContext("/api/accounts", authWrap(new AccountHandler()));
            server.createContext("/api/players", authWrap(new PlayerHandler()));
            server.createContext("/api/items", authWrap(new ItemSearchHandler()));
            server.createContext("/api/shops", authWrap(new ShopHandler()));
            server.createContext("/api/bosses", authWrap(new BossHandler()));
            server.createContext("/api/combine-rates", authWrap(new CombineRateHandler()));            server.createContext("/api/vong-quay", authWrap(new VongQuayHandler()));
            server.createContext("/api/giftcodes", authWrap(new GiftcodeHandler()));
            server.createContext("/api/mail", authWrap(new MailHandler()));
            server.createContext("/api/buff-vnd", authWrap(new BuffVndHandler()));
            server.createContext("/api/history", authWrap(new HistoryHandler()));
            server.createContext("/api/bot-ai", authWrap(new BotAiHandler()));
            server.createContext("/api/events", authWrap(new EventHandler()));
            server.createContext("/api/mob-me-cleaner", authWrap(new MobMeCleanerHandler()));
            server.createContext("/api/donate-packages", authWrap(new DonatePackageHandler()));
            server.createContext("/api/nap-tien", authWrap(new NapTienHandler()));
            server.createContext("/api/boss-editor", authWrap(new BossEditorHandler()));
            server.createContext("/api/map-editor", authWrap(new MapEditorHandler()));
            server.createContext("/api/boss-summon", authWrap(new BossSummonHandler()));
            server.createContext("/api/boss-custom-drops", authWrap(new BossCustomDropHandler()));
            server.createContext("/api/server-config", authWrap(new ServerConfigHandler()));
            server.createContext("/api/system-optimize", authWrap(new SystemOptimizeHandler()));
            server.createContext("/api/player-inventory", authWrap(new PlayerInventoryHandler()));

            server.start();
            isRunning = true;
            System.out.println("=================================================");
            System.out.println(">>> [WEB ADMIN] Started at http://" + host + ":" + port);
            System.out.println(">>> [WEB ADMIN] Password: xem file data/config/web_admin.properties");
            System.out.println("=================================================");
        } catch (Exception e) {
            if (server != null) server.stop(0);
            if (executor != null) executor.shutdownNow();
            System.err.println(">>> [WEB ADMIN] Start error: " + e.getClass().getSimpleName());
        }
    }


    public synchronized void stop() {
        if (!isRunning || server == null) return;
        server.stop(1);
        if (executor != null) executor.shutdownNow();
        isRunning = false;
        System.out.println(">>> [WEB ADMIN] Stopped.");
    }

    public int getPort() {
        return port;
    }

    // =========================================================================
    //  HTTP Helpers
    // =========================================================================

    private static void sendJsonResponse(HttpExchange exchange, int statusCode, Object data) throws IOException {
        String json = (data instanceof String) ? (String) data : gson.toJson(data);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=UTF-8");
        
        
        
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static void sendError(HttpExchange exchange, int statusCode, String message) throws IOException {
        Map<String, Object> err = new HashMap<>();
        err.put("success", false);
        err.put("error", message);
        sendJsonResponse(exchange, statusCode, err);
    }

    private static void sendSuccess(HttpExchange exchange, String message, Object extra) throws IOException {
        Map<String, Object> res = new HashMap<>();
        res.put("success", true);
        res.put("message", message);
        if (extra != null) {
            res.put("data", extra);
        }
        sendJsonResponse(exchange, 200, res);
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        try (InputStream is = exchange.getRequestBody()) {
            byte[] body = is.readNBytes(1024 * 1024 + 1);
            if (body.length > 1024 * 1024) {
                throw PanelSupport.error(413, "Dữ liệu gửi lên quá lớn");
            }
            String json = new String(body, StandardCharsets.UTF_8);
            if (utils.JsonInput.hasExcessiveDepth(json)) throw PanelSupport.error(400, "JSON lồng nhau quá sâu");
            return json;
        }
    }

    private static Map<String, String> parseQueryParams(String query) {
        Map<String, String> map = new HashMap<>();
        if (query == null || query.isEmpty()) return map;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            String key = URLDecoder.decode(pair[0], StandardCharsets.UTF_8);
            String val = pair.length > 1 ? URLDecoder.decode(pair[1], StandardCharsets.UTF_8) : "";
            map.put(key, val);
        }
        return map;
    }

    private static boolean handleOptions(HttpExchange exchange) throws IOException {
        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type, Authorization");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            exchange.sendResponseHeaders(204, -1);
            return true;
        }
        return false;
    }

    // =========================================================================
    //  Static Files Handler
    // =========================================================================

    private static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String path = exchange.getRequestURI().getPath();
            if (path == null || path.isEmpty() || "/".equals(path)) {
                path = "/index.html";
            }

            File webRootDir;
            try {
                webRootDir = new File(WEB_ROOT).getCanonicalFile();
            } catch (Exception e) {
                webRootDir = new File(WEB_ROOT).getAbsoluteFile();
            }

            // Chuẩn hóa path và loại bỏ leading slashes
            while (path.startsWith("/")) {
                path = path.substring(1);
            }
            if (path.isEmpty()) {
                path = "index.html";
            }

            File file;
            try {
                file = new File(webRootDir, path).getCanonicalFile();
            } catch (Exception e) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            // Chống Path Traversal: Tuyệt đối không cho phép truy cập ra ngoài thư mục web
            if (!file.getPath().startsWith(webRootDir.getPath())) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            // Chặn đọc các file nhạy cảm kể cả khi nằm trong web root
            String lowerName = file.getName().toLowerCase();
            if (lowerName.endsWith(".properties") || lowerName.endsWith(".class") || lowerName.endsWith(".java")
                    || lowerName.endsWith(".jar") || lowerName.endsWith(".xml") || lowerName.endsWith(".bat")
                    || lowerName.endsWith(".sh") || lowerName.endsWith(".sql")) {
                sendError(exchange, 403, "Forbidden");
                return;
            }

            if (!file.exists() || file.isDirectory()) {
                file = new File(webRootDir, "index.html");
            }

            if (!file.exists()) {
                String notFound = "<h1>404 Not Found - Web Admin Panel</h1>";
                byte[] bytes = notFound.getBytes(StandardCharsets.UTF_8);
                exchange.sendResponseHeaders(404, bytes.length);
                try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                return;
            }

            String mime = "text/plain";
            String name = file.getName().toLowerCase();
            if (name.endsWith(".html")) mime = "text/html; charset=UTF-8";
            else if (name.endsWith(".css")) mime = "text/css; charset=UTF-8";
            else if (name.endsWith(".js")) mime = "application/javascript; charset=UTF-8";
            else if (name.endsWith(".json")) mime = "application/json; charset=UTF-8";
            else if (name.endsWith(".png")) mime = "image/png";
            else if (name.endsWith(".jpg") || name.endsWith(".jpeg")) mime = "image/jpeg";
            else if (name.endsWith(".svg")) mime = "image/svg+xml";
            else if (name.endsWith(".ico")) mime = "image/x-icon";

            exchange.getResponseHeaders().set("Content-Type", mime);
            exchange.sendResponseHeaders(200, file.length());
            try (OutputStream os = exchange.getResponseBody();
                 FileInputStream fis = new FileInputStream(file)) {
                byte[] buf = new byte[4096];
                int n;
                while ((n = fis.read(buf)) != -1) {
                    os.write(buf, 0, n);
                }
            }
        }
    }

    // =========================================================================
    //  API: Verify Auth
    // =========================================================================

    private static class VerifyAuthHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            Map<String, Object> data = new HashMap<>();
            data.put("authenticated", true);
            data.put("username", adminUsername != null ? adminUsername : "admin");
            sendSuccess(exchange, "Token hợp lệ", data);
        }
    }

    // =========================================================================
    //  API: Login
    // =========================================================================

    private static class LoginHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }
            String ip = getClientIp(exchange);
            if (isIpLocked(ip)) {
                sendError(exchange, 429, "Quá nhiều lần thử đăng nhập sai. IP tạm thời bị khóa trong 15 phút!");
                return;
            }
            try {
                JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                String user = body.has("username") ? body.get("username").getAsString().trim() : "";
                String pass = body.has("password") ? body.get("password").getAsString().trim() : "";
                // Bắt buộc nhập cả tài khoản và mật khẩu
                if (user.isEmpty()) {
                    sendError(exchange, 401, "Vui lòng nhập tài khoản!");
                    return;
                }
                if (pass.isEmpty()) {
                    sendError(exchange, 401, "Vui lòng nhập mật khẩu!");
                    return;
                }
                if (adminUsername != null && user.equals(adminUsername) && adminPassword != null && MessageDigest.isEqual(pass.getBytes(java.nio.charset.StandardCharsets.UTF_8), adminPassword.getBytes(java.nio.charset.StandardCharsets.UTF_8))) {
                    recordSuccessLogin(ip);
                    // Tạo token mới mỗi lần đăng nhập thành công → vô hiệu hóa token cũ
                    adminToken = "tok_" + UUID.randomUUID().toString().replace("-", "");
                    Map<String, Object> data = new HashMap<>();
                    data.put("token", adminToken);
                    data.put("username", adminUsername);
                    sendSuccess(exchange, "Đăng nhập thành công!", data);
                } else {
                    recordFailedLogin(ip);
                    try {
                        Thread.sleep(500); // Chống brute-force attack tốc độ cao
                    } catch (InterruptedException ignored) {}
                    sendError(exchange, 401, "Tài khoản hoặc mật khẩu không chính xác!");
                }
            } catch (Exception e) {
                sendError(exchange, 400, "Dữ liệu yêu cầu không hợp lệ");
            }
        }
    }


    // =========================================================================
    //  API: Dashboard
    // =========================================================================

    private static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            try {
                MemoryMXBean mem = ManagementFactory.getMemoryMXBean();
                long usedMem = mem.getHeapMemoryUsage().getUsed() / (1024 * 1024);
                long maxMem = mem.getHeapMemoryUsage().getMax() / (1024 * 1024);
                int threads = Thread.activeCount();

                int onlineCount = 0;
                int sessionCount = 0;
                try {
                    onlineCount = Client.gI().getPlayers().size();
                } catch (Exception ignored) {}
                try {
                    sessionCount = network.session.SessionManager.gI().getNumSession();
                } catch (Exception ignored) {}
                if (sessionCount < onlineCount) {
                    sessionCount = onlineCount;
                }

                long uptime = (System.currentTimeMillis() - ServerManager.timeStartMillis) / 1000;
                long hours = uptime / 3600;
                long mins = (uptime % 3600) / 60;
                long secs = uptime % 60;
                String uptimeStr = String.format("%02dh %02dm %02ds", hours, mins, secs);

                Map<String, Object> res = new HashMap<>();
                res.put("serverName", ServerManager.NAME);
                res.put("serverIp", ServerManager.IP);
                res.put("serverPort", ServerManager.PORT);
                res.put("onlineCount", onlineCount);
                res.put("onlinePlayers", onlineCount);
                res.put("activeSessions", sessionCount);
                res.put("sessionCount", sessionCount);
                res.put("usedMemMb", usedMem);
                res.put("ramUsed", usedMem);
                res.put("maxMemMb", maxMem);
                res.put("ramMax", maxMem);
                res.put("threadCount", threads);
                res.put("threads", threads);
                res.put("uptime", uptimeStr);
                res.put("timeStart", ServerManager.timeStart);
                res.put("startTime", ServerManager.timeStart);
                res.put("serverTime", TimeUtil.getTimeNow("HH:mm:ss dd/MM/yyyy"));

                // Top sức mạnh
                List<Map<String, Object>> topPower = new ArrayList<>();
                try (Connection conn = DBConnecter.getConnectionServer();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT p.name, p.gender, p.point, " +
                             "(SELECT username FROM account WHERE id = p.account_id LIMIT 1) AS account_name " +
                             "FROM player p ORDER BY p.point DESC LIMIT 5")) {
                    int rank = 1;
                    while (rs.next()) {
                        Map<String, Object> p = new HashMap<>();
                        p.put("rank", rank++);
                        p.put("name", rs.getString("name"));
                        p.put("account", rs.getString("account_name"));
                        p.put("power", rs.getLong("point"));
                        p.put("gender", rs.getInt("gender"));
                        topPower.add(p);
                    }
                } catch (Exception ignored) {}
                res.put("topPower", topPower);

                // Top nạp
                List<Map<String, Object>> topNap = new ArrayList<>();
                try (Connection conn = DBConnecter.getConnectionServer();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT username, sotien, danap FROM account ORDER BY danap DESC LIMIT 5")) {
                    int rank = 1;
                    while (rs.next()) {
                        Map<String, Object> a = new HashMap<>();
                        a.put("rank", rank++);
                        a.put("username", rs.getString("username"));
                        a.put("sotien", rs.getInt("sotien"));
                        a.put("danap", rs.getInt("danap"));
                        topNap.add(a);
                    }
                } catch (Exception ignored) {}
                res.put("topNap", topNap);

                sendSuccess(exchange, "OK", res);
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi server: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Dashboard Actions (Save, Maintenance, Broadcast, KickAll, Restart)
    // =========================================================================

    private static class DashboardActionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                String action = body.has("action") ? body.get("action").getAsString() : "";

                switch (action) {
                    case "saveAll" -> {
                        new Thread(() -> {
                            try {
                                AutoSaveManager.getInstance().handleAutoSave();
                                for (Player p : Client.gI().getPlayers()) {
                                    if (p != null) PlayerDAO.updatePlayer(p);
                                }
                            } catch (Exception e) { e.printStackTrace(); }
                        }).start();
                        sendSuccess(exchange, "Đã bắt đầu lưu toàn bộ dữ liệu server & người chơi!", null);
                    }
                    case "maintenance" -> {
                        int seconds = body.has("seconds") ? body.get("seconds").getAsInt() : 60;
                        Maintenance.gI().start(seconds);
                        sendSuccess(exchange, "Đã kích hoạt bảo trì server trong " + seconds + " giây!", null);
                    }
                    case "broadcast" -> {
                        String msg = body.has("message") ? body.get("message").getAsString().trim() : "";
                        if (msg.isEmpty()) {
                            sendError(exchange, 400, "Nội dung thông báo không được để trống!");
                            return;
                        }
                        Service.gI().sendThongBaoAllPlayer(msg);
                        sendSuccess(exchange, "Đã phát thông báo toàn server: " + msg, null);
                    }
                    case "kickAll" -> {
                        int count = 0;
                        try {
                            List<Player> list = new ArrayList<>(Client.gI().getPlayers());
                            count = list.size();
                            for (Player p : list) {
                                if (p != null && p.getSession() != null) {
                                    Client.gI().kickSession(p.getSession());
                                }
                            }
                        } catch (Exception ignored) {}
                        sendSuccess(exchange, "Đã ngắt kết nối " + count + " người chơi online!", null);
                    }
                    case "restart" -> {
                        ServerManagerUI.REQUEST_AUTO_RESTART = true;
                        new Thread(() -> {
                            try {
                                Thread.sleep(2000);
                                System.exit(0);
                            } catch (Exception ignored) {}
                        }).start();
                        sendSuccess(exchange, "Server sẽ khởi động lại trong giây lát...", null);
                    }
                    default -> sendError(exchange, 400, "Hành động không hợp lệ: " + action);
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi thực thi: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Accounts (CRUD)
    // =========================================================================

    private static class AccountHandler implements HttpHandler {
        private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String q = query.getOrDefault("q", "").trim();
                int page = Math.max(1, Integer.parseInt(query.getOrDefault("page", "1")));
                int limit = Math.max(10, Math.min(100, Integer.parseInt(query.getOrDefault("limit", "20"))));
                int offset = (page - 1) * limit;

                List<Map<String, Object>> list = new ArrayList<>();
                int total = 0;

                String countSql = "SELECT COUNT(*) FROM account a " +
                        (q.isEmpty() ? "" : "WHERE a.username LIKE ? OR a.id = ? OR (SELECT name FROM player WHERE account_id = a.id LIMIT 1) LIKE ?");
                String selectSql = "SELECT a.id, a.username, a.password, a.active, a.ban, a.vip, a.sotien, a.danap, a.create_time, " +
                        "(SELECT name FROM player WHERE account_id = a.id LIMIT 1) AS player_name " +
                        "FROM account a " +
                        (q.isEmpty() ? "" : "WHERE a.username LIKE ? OR a.id = ? OR (SELECT name FROM player WHERE account_id = a.id LIMIT 1) LIKE ? ") +
                        "ORDER BY a.id ASC LIMIT ? OFFSET ?";

                try (Connection conn = DBConnecter.getConnectionServer()) {
                    // Count
                    try (PreparedStatement ps = conn.prepareStatement(countSql)) {
                        if (!q.isEmpty()) {
                            ps.setString(1, "%" + q + "%");
                            ps.setString(2, q);
                            ps.setString(3, "%" + q + "%");
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) total = rs.getInt(1);
                        }
                    }

                    // Select
                    try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                        int idx = 1;
                        if (!q.isEmpty()) {
                            ps.setString(idx++, "%" + q + "%");
                            ps.setString(idx++, q);
                            ps.setString(idx++, "%" + q + "%");
                        }
                        ps.setInt(idx++, limit);
                        ps.setInt(idx, offset);

                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> acc = new HashMap<>();
                                acc.put("id", rs.getInt("id"));
                                acc.put("username", rs.getString("username"));
                                acc.put("playerName", rs.getString("player_name") == null ? "-(Chưa tạo)-" : rs.getString("player_name"));
                                acc.put("active", rs.getInt("active"));
                                acc.put("ban", rs.getInt("ban"));
                                acc.put("vip", rs.getInt("vip"));
                                acc.put("sotien", rs.getInt("sotien"));
                                acc.put("danap", rs.getInt("danap"));
                                Timestamp ts = rs.getTimestamp("create_time");
                                acc.put("createdAt", ts != null ? DATE_FMT.format(ts) : "N/A");
                                list.add(acc);
                            }
                        }
                    }

                    Map<String, Object> data = new HashMap<>();
                    data.put("accounts", list);
                    data.put("total", total);
                    data.put("page", page);
                    data.put("limit", limit);
                    data.put("totalPages", (int) Math.ceil((double) total / limit));
                    sendSuccess(exchange, "OK", data);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi đọc tài khoản: " + e.getMessage());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                // Update / Create / Delete
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "update";

                    if ("create".equalsIgnoreCase(action)) {
                        String user = body.get("username").getAsString().trim();
                        String pass = body.get("password").getAsString().trim();
                        int sotien = body.has("sotien") ? body.get("sotien").getAsInt() : 0;
                        if (user.isEmpty() || pass.isEmpty()) {
                            sendError(exchange, 400, "Tài khoản và mật khẩu không được rỗng!");
                            return;
                        }
                        try (Connection conn = DBConnecter.getConnectionServer();
                             PreparedStatement ps = conn.prepareStatement("INSERT INTO account (username, password, active, sotien, create_time) VALUES (?, ?, 1, ?, NOW())")) {
                            ps.setString(1, user);
                            ps.setString(2, pass);
                            ps.setInt(3, sotien);
                            ps.executeUpdate();
                            sendSuccess(exchange, "Đã tạo tài khoản " + user + " thành công!", null);
                        }
                    } else if ("delete".equalsIgnoreCase(action)) {
                        int id = body.get("id").getAsInt();
                        try (Connection conn = DBConnecter.getConnectionServer()) {
                            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM player WHERE account_id = ?")) {
                                ps.setInt(1, id);
                                ps.executeUpdate();
                            }
                            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM account WHERE id = ?")) {
                                ps.setInt(1, id);
                                ps.executeUpdate();
                            }
                            sendSuccess(exchange, "Đã xóa tài khoản ID: " + id, null);
                        }
                    } else { // update
                        int id = body.get("id").getAsInt();
                        String newPass = body.has("password") ? body.get("password").getAsString().trim() : "";
                        int active = body.has("active") ? body.get("active").getAsInt() : 1;
                        int ban = body.has("ban") ? body.get("ban").getAsInt() : 0;
                        int vip = body.has("vip") ? body.get("vip").getAsInt() : 0;
                        int sotien = body.has("sotien") ? body.get("sotien").getAsInt() : 0;
                        int danap = body.has("danap") ? body.get("danap").getAsInt() : 0;

                        String sql = newPass.isEmpty() ?
                                "UPDATE account SET active = ?, ban = ?, vip = ?, sotien = ?, danap = ? WHERE id = ?" :
                                "UPDATE account SET password = ?, active = ?, ban = ?, vip = ?, sotien = ?, danap = ? WHERE id = ?";

                        try (Connection conn = DBConnecter.getConnectionServer();
                             PreparedStatement ps = conn.prepareStatement(sql)) {
                            int idx = 1;
                            if (!newPass.isEmpty()) ps.setString(idx++, newPass);
                            ps.setInt(idx++, active);
                            ps.setInt(idx++, ban);
                            ps.setInt(idx++, vip);
                            ps.setInt(idx++, sotien);
                            ps.setInt(idx++, danap);
                            ps.setInt(idx, id);
                            ps.executeUpdate();

                            // Nếu ban thì kick ngay
                            if (ban == 1) {
                                Player p = Client.gI().getPlayerByUser(id);
                                if (p != null && p.getSession() != null) {
                                    Client.gI().kickSession(p.getSession());
                                }
                            }
                            sendSuccess(exchange, "Cập nhật tài khoản ID " + id + " thành công!", null);
                        }
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi cập nhật: " + e.getMessage());
                }
            } else {
                sendError(exchange, 405, "Method Not Allowed");
            }
        }
    }

    // =========================================================================
    //  API: Players (Online / Offline, Kick, Teleport, Buff Stats, Buff Item)
    // =========================================================================

    private static class PlayerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                String q = query.getOrDefault("q", "").trim().toLowerCase();
                String filter = query.getOrDefault("filter", "all"); // all, online

                List<Map<String, Object>> result = new ArrayList<>();

                // Online Players từ RAM
                Set<Integer> onlineIds = new HashSet<>();
                try {
                    for (Player p : Client.gI().getPlayers()) {
                        if (p == null) continue;
                        onlineIds.add((int) p.id);
                        if (!q.isEmpty() && !p.name.toLowerCase().contains(q) && !String.valueOf(p.id).equals(q)) {
                            continue;
                        }
                        Map<String, Object> row = new HashMap<>();
                        row.put("id", p.id);
                        row.put("accountId", p.getSession() != null ? p.getSession().userId : -1);
                        row.put("name", p.name);
                        row.put("gender", (int) p.gender);
                        row.put("isOnline", true);
                        row.put("power", p.nPoint != null ? p.nPoint.power : 0);
                        row.put("tiemNang", p.nPoint != null ? p.nPoint.tiemNang : 0);
                        row.put("hp", p.nPoint != null ? p.nPoint.hpMax : 0);
                        row.put("ki", p.nPoint != null ? p.nPoint.mpMax : 0);
                        row.put("dame", p.nPoint != null ? p.nPoint.dame : 0);
                        row.put("mapId", p.zone != null && p.zone.map != null ? p.zone.map.mapId : -1);
                        row.put("mapName", p.zone != null && p.zone.map != null ? p.zone.map.mapName : "N/A");
                        row.put("x", p.location != null ? p.location.x : 0);
                        row.put("y", p.location != null ? p.location.y : 0);
                        result.add(row);
                    }
                } catch (Exception ignored) {}

                // Offline Players từ DB (nếu filter = all)
                if (!"online".equalsIgnoreCase(filter)) {
                    String sql = "SELECT id, account_id, name, gender, data_point, data_location FROM player " +
                            (q.isEmpty() ? "" : "WHERE name LIKE ? OR id = ? ") +
                            "ORDER BY id DESC LIMIT 50";
                    try (Connection conn = DBConnecter.getConnectionServer();
                         PreparedStatement ps = conn.prepareStatement(sql)) {
                        if (!q.isEmpty()) {
                            ps.setString(1, "%" + q + "%");
                            ps.setString(2, q);
                        }
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                int id = rs.getInt("id");
                                if (onlineIds.contains(id)) continue; // Đã thêm ở phần online

                                Map<String, Object> row = new HashMap<>();
                                row.put("id", id);
                                row.put("accountId", rs.getInt("account_id"));
                                row.put("name", rs.getString("name"));
                                row.put("gender", rs.getInt("gender"));
                                row.put("isOnline", false);

                                long power = 0, tiemNang = 0, hp = 0, ki = 0, dame = 0;
                                String dataPoint = rs.getString("data_point");
                                if (dataPoint != null && !dataPoint.isEmpty()) {
                                    try {
                                        JsonArray ptArr = new JsonParser().parse(dataPoint).getAsJsonArray();
                                        if (ptArr.size() > 1) power = ptArr.get(1).getAsLong();
                                        if (ptArr.size() > 2) tiemNang = ptArr.get(2).getAsLong();
                                        if (ptArr.size() > 7) dame = ptArr.get(7).getAsLong();
                                        if (ptArr.size() > 11) hp = ptArr.get(11).getAsLong();
                                        if (ptArr.size() > 12) ki = ptArr.get(12).getAsLong();
                                    } catch (Exception ignored) {}
                                }
                                row.put("power", power);
                                row.put("tiemNang", tiemNang);
                                row.put("hp", hp);
                                row.put("ki", ki);
                                row.put("dame", dame);

                                int mapId = 0, x = 0, y = 0;
                                String dataLoc = rs.getString("data_location");
                                if (dataLoc != null && !dataLoc.isEmpty()) {
                                    try {
                                        JsonArray locArr = new JsonParser().parse(dataLoc).getAsJsonArray();
                                        if (locArr.size() > 0) mapId = locArr.get(0).getAsInt();
                                        if (locArr.size() > 1) x = locArr.get(1).getAsInt();
                                        if (locArr.size() > 2) y = locArr.get(2).getAsInt();
                                    } catch (Exception ignored) {}
                                }
                                row.put("mapId", mapId);
                                String mapName = "Map " + mapId;
                                try {
                                    models.map.Map m = services.map.MapService.gI().getMapById(mapId);
                                    if (m != null && m.mapName != null) mapName = m.mapName;
                                } catch (Exception ignored) {}
                                row.put("mapName", mapName);
                                row.put("x", x);
                                row.put("y", y);
                                result.add(row);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                sendSuccess(exchange, "OK", result);
            } else if ("POST".equalsIgnoreCase(method)) {
                // Actions: kick, teleport, buff-stats, buff-item
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.get("action").getAsString();
                    long playerId = body.has("playerId") ? body.get("playerId").getAsLong() : (body.has("id") ? body.get("id").getAsLong() : -1);
                    String playerName = body.has("playerName") ? body.get("playerName").getAsString().trim() : "";

                    Player pl = null;
                    if (playerId > 0) {
                        pl = Client.gI().getPlayer(playerId);
                    }
                    if (pl == null && !playerName.isEmpty()) {
                        pl = Client.gI().getPlayer(playerName);
                    }

                    switch (action) {
                        case "kick" -> {
                            if (pl == null || pl.getSession() == null) {
                                sendError(exchange, 400, "Người chơi không online!");
                                return;
                            }
                            Client.gI().kickSession(pl.getSession());
                            sendSuccess(exchange, "Đã ngắt kết nối người chơi: " + pl.name, null);
                        }
                        case "teleport" -> {
                            if (pl == null) {
                                sendError(exchange, 400, "Người chơi không online!");
                                return;
                            }
                            int mapId = body.get("mapId").getAsInt();
                            int x = body.has("x") ? body.get("x").getAsInt() : 300;
                            int y = body.has("y") ? body.get("y").getAsInt() : 300;
                            services.map.ChangeMapService.gI().changeMapInYard(pl, mapId, -1, x);
                            sendSuccess(exchange, "Đã dịch chuyển " + pl.name + " tới Map " + mapId, null);
                        }
                        case "buffStats" -> {
                            if (pl == null) {
                                sendError(exchange, 400, "Người chơi phải online để nhận buff chỉ số!");
                                return;
                            }
                            long addHp = body.has("addHp") ? body.get("addHp").getAsLong() : 0;
                            long addMp = body.has("addMp") ? body.get("addMp").getAsLong() : 0;
                            long addDame = body.has("addDame") ? body.get("addDame").getAsLong() : 0;
                            long addPower = body.has("addPower") ? body.get("addPower").getAsLong() : 0;

                            if (pl.nPoint != null) {
                                if (addHp > 0) pl.nPoint.hpg += addHp;
                                if (addMp > 0) pl.nPoint.mpg += addMp;
                                if (addDame > 0) pl.nPoint.dameg += addDame;
                                if (addPower > 0) {
                                    pl.nPoint.power += addPower;
                                    pl.nPoint.tiemNang += addPower;
                                }
                                pl.nPoint.calPoint();
                                Service.gI().point(pl);
                                Service.gI().sendThongBao(pl, "Chỉ số của bạn đã được Admin nâng cấp!");
                            }
                            sendSuccess(exchange, "Đã buff chỉ số cho " + pl.name + " thành công!", null);
                        }
                        case "buffMoney" -> {
                            long gold = body.has("gold") ? body.get("gold").getAsLong() : 0;
                            int gem = body.has("gem") ? body.get("gem").getAsInt() : 0;
                            int ruby = body.has("ruby") ? body.get("ruby").getAsInt() : 0;
                            int cash = body.has("cash") ? body.get("cash").getAsInt() : 0;

                            if (gold <= 0 && gem <= 0 && ruby <= 0 && cash <= 0) {
                                sendError(exchange, 400, "Vui lòng nhập ít nhất một khoản tiền muốn buff!");
                                return;
                            }

                            // 1. Buff Cash if > 0
                            if (cash > 0) {
                                String userTarget = pl != null && pl.getSession() != null && pl.getSession().uu != null
                                        ? pl.getSession().uu
                                        : (playerName.isEmpty() ? String.valueOf(playerId) : playerName);
                                TargetAccountInfo info = resolveTargetAccount(userTarget);
                                if (info.accountId != -1) {
                                    PlayerDAO.addCashDanapAndDanapGoc(info.username, cash, 0);
                                    if (pl != null && pl.getSession() != null) {
                                        pl.getSession().cash += cash;
                                    }
                                }
                            }

                            // 2. Buff In-game Money (Gold, Gem, Ruby)
                            if (pl != null) {
                                if (gold > 0) pl.inventory.gold = Math.min(models.player.Inventory.LIMIT_GOLD, pl.inventory.gold + gold);
                                if (gem > 0) pl.inventory.gem = (int) Math.min(Integer.MAX_VALUE, (long) pl.inventory.gem + gem);
                                if (ruby > 0) pl.inventory.ruby = (int) Math.min(Integer.MAX_VALUE, (long) pl.inventory.ruby + ruby);
                                Service.gI().sendMoney(pl);
                                Service.gI().sendThongBao(pl, "Bạn vừa được Admin cộng tiền thành công!");
                                sendSuccess(exchange, "Đã buff tiền trực tiếp cho " + pl.name + " thành công!", null);
                            } else {
                                // Player offline -> deliver via Mailbox
                                long targetId = playerId;
                                if (targetId <= 0 && !playerName.isEmpty()) {
                                    targetId = PlayerDAO.findPlayerIdByName(playerName);
                                }
                                if (targetId <= 0) {
                                    sendError(exchange, 400, "Không tìm thấy người chơi để nhận tiền!");
                                    return;
                                }
                                List<GiftDataDAO.Reward> rewards = new ArrayList<>();
                                if (gold > 0) rewards.add(new GiftDataDAO.Reward(76, (int) Math.min(gold, 2_000_000_000L)));
                                if (gem > 0) rewards.add(new GiftDataDAO.Reward(77, gem));
                                if (ruby > 0) rewards.add(new GiftDataDAO.Reward(861, ruby));
                                int mailId = GiftMailService.gI().deliver(targetId, "Hệ Thống", "Quà Tặng Từ Ban Quản Trị",
                                        "Bạn nhận được phần thưởng tiền tệ từ Admin!", rewards, 30);
                                sendSuccess(exchange, "Người chơi đang offline, đã gửi tiền vào Hòm Thư thành công (Mail ID: " + mailId + ")!", null);
                            }
                        }
                        case "buffItem" -> {
                            int templateId = body.get("templateId").getAsInt();
                            int quantity = body.has("quantity") ? body.get("quantity").getAsInt() : 1;
                            if (quantity <= 0) quantity = 1;

                            Item item = ItemService.gI().createNewItem((short) templateId, quantity);
                            if (item == null || item.template == null) {
                                sendError(exchange, 400, "Vật phẩm có ID " + templateId + " không tồn tại trong hệ thống!");
                                return;
                            }

                            List<int[]> optionPairs = new ArrayList<>();
                            if (body.has("options") && body.get("options").isJsonArray()) {
                                JsonArray opts = body.getAsJsonArray("options");
                                for (JsonElement elem : opts) {
                                    JsonObject o = elem.getAsJsonObject();
                                    int optId = o.get("id").getAsInt();
                                    int param = o.get("param").getAsInt();
                                    item.itemOptions.add(new Item.ItemOption(optId, param));
                                    optionPairs.add(new int[]{optId, param});
                                }
                            }

                            if (pl != null) {
                                boolean added = services.player.InventoryService.gI().addItemBag(pl, item);
                                if (added) {
                                    services.player.InventoryService.gI().sendItemBags(pl);
                                    Service.gI().sendThongBao(pl, "Bạn vừa nhận được " + quantity + "x " + item.template.name + " từ Admin!");
                                    sendSuccess(exchange, "Đã trao " + quantity + "x " + item.template.name + " vào hành trang " + pl.name, null);
                                    return;
                                }
                            }

                            // If player is offline OR inventory was full -> deliver safely via Mail
                            long targetId = pl != null ? pl.id : playerId;
                            if (targetId <= 0 && !playerName.isEmpty()) {
                                targetId = PlayerDAO.findPlayerIdByName(playerName);
                            }
                            if (targetId <= 0) {
                                sendError(exchange, 400, "Không tìm thấy người chơi để nhận vật phẩm!");
                                return;
                            }

                            GiftDataDAO.Reward rew = new GiftDataDAO.Reward(templateId, quantity);
                            rew.options = optionPairs;
                            List<GiftDataDAO.Reward> rewards = new ArrayList<>();
                            rewards.add(rew);
                            int mailId = GiftMailService.gI().deliver(targetId, "Hệ Thống", "Vật Phẩm Từ Admin",
                                    "Ban Quản Trị đã gửi cho bạn " + quantity + "x " + item.template.name, rewards, 30);
                            if (mailId > 0) {
                                String msg = (pl != null)
                                        ? "Hành trang của " + pl.name + " đã đầy! Vật phẩm (" + quantity + "x " + item.template.name + ") đã được chuyển vào Hòm Thư."
                                        : "Người chơi đang offline. Đã gửi " + quantity + "x " + item.template.name + " vào Hòm Thư thành công!";
                                sendSuccess(exchange, msg, null);
                            } else {
                                sendError(exchange, 500, "Không thể gửi vật phẩm vào Hòm Thư.");
                            }
                        }
                        default -> sendError(exchange, 400, "Hành động không hợp lệ: " + action);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi xử lý người chơi: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Item Search (Tra cứu template vật phẩm)
    // =========================================================================

    private static class ItemSearchHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String q = query.getOrDefault("q", "").trim().toLowerCase();
            int limit = Math.max(10, Math.min(100, Integer.parseInt(query.getOrDefault("limit", "50"))));

            List<Map<String, Object>> list = new ArrayList<>();
            String sql = "SELECT id, name, type, gender, description, icon_id FROM item_template " +
                    (q.isEmpty() ? "" : "WHERE name LIKE ? OR id = ? ") +
                    "ORDER BY id ASC LIMIT ?";

            try (Connection conn = DBConnecter.getConnectionData();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                int idx = 1;
                if (!q.isEmpty()) {
                    ps.setString(idx++, "%" + q + "%");
                    ps.setString(idx++, q);
                }
                ps.setInt(idx, limit);

                try (ResultSet rs = ps.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> it = new HashMap<>();
                        it.put("id", rs.getInt("id"));
                        it.put("name", rs.getString("name"));
                        it.put("type", rs.getInt("type"));
                        it.put("gender", rs.getInt("gender"));
                        it.put("description", rs.getString("description"));
                        it.put("iconId", rs.getInt("icon_id"));
                        list.add(it);
                    }
                }
                sendSuccess(exchange, "OK", list);
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi tra cứu item: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Shops & Shop Editor
    // =========================================================================

    private static class ShopHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                try (Connection conn = DBConnecter.getConnectionData();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery(
                         "SELECT ts.id, ts.shop_id, ts.tab_name, ts.tab_index, ts.items, n.name AS npc_name " +
                         "FROM tab_shop ts " +
                         "LEFT JOIN shop s ON ts.shop_id = s.id " +
                         "LEFT JOIN npc_template n ON s.npc_id = n.id " +
                         "ORDER BY ts.shop_id ASC, ts.tab_index ASC")) {
                    List<Map<String, Object>> tabs = new ArrayList<>();
                    while (rs.next()) {
                        Map<String, Object> t = new HashMap<>();
                        t.put("id", rs.getInt("id"));
                        t.put("shopId", rs.getInt("shop_id"));
                        t.put("tabName", rs.getString("tab_name") != null ? rs.getString("tab_name") : "");
                        t.put("tabIndex", rs.getInt("tab_index"));
                        t.put("npcName", rs.getString("npc_name") == null ? ("Shop #" + rs.getInt("shop_id")) : rs.getString("npc_name"));
                        String rawItems = rs.getString("items");
                        try {
                            t.put("items", new JsonParser().parse(rawItems == null ? "[]" : rawItems));
                        } catch (Exception ex) {
                            t.put("items", new JsonArray());
                        }
                        tabs.add(t);
                    }
                    sendSuccess(exchange, "OK", tabs);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi đọc shop: " + e.getMessage());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "save";
                    if ("save".equalsIgnoreCase(action)) {
                        int tabId = body.get("tabId").getAsInt();
                        String tabName = body.has("tabName") ? body.get("tabName").getAsString() : "";
                        String itemsJson = body.has("items") ? body.get("items").toString() : "[]";

                        try (Connection conn = DBConnecter.getConnectionData();
                             PreparedStatement ps = conn.prepareStatement(
                                 tabName.isEmpty() ?
                                     "UPDATE tab_shop SET items = ? WHERE id = ?" :
                                     "UPDATE tab_shop SET tab_name = ?, items = ? WHERE id = ?")) {
                            int idx = 1;
                            if (!tabName.isEmpty()) ps.setString(idx++, tabName);
                            ps.setString(idx++, itemsJson);
                            ps.setInt(idx, tabId);
                            ps.executeUpdate();
                        }
                        try {
                            Manager.reloadShops();
                        } catch (Exception ex) {
                            Logger.error("reloadShops: " + ex.getMessage() + "\n");
                        }
                        sendSuccess(exchange, "Đã lưu và đồng bộ Shop live thành công!", null);
                    } else {
                        sendError(exchange, 400, "Action không hợp lệ: " + action);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi lưu Shop: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Bosses & Boss Drops
    // =========================================================================

    private static class BossHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            sendSuccess(exchange, "OK", new HashMap<>());
        }
    }

    // =========================================================================
    //  API: Combine Rates (Tỉ lệ đập đồ)
    // =========================================================================

    private static class CombineRateHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> res = new HashMap<>();
                res.put("rateNangCapDo", CombineRateConfig.rateNangCapDo);
                res.put("ratePhaLeHoa", CombineRateConfig.ratePhaLeHoa);
                res.put("skhVip1", CombineRateConfig.skhVip1);
                res.put("skhVip2", CombineRateConfig.skhVip2);
                res.put("skhThuong", CombineRateConfig.skhThuong);
                res.put("rateBongTai2", CombineRateConfig.rateBongTai2);
                res.put("rateBongTai3", CombineRateConfig.rateBongTai3);
                res.put("rateTrungDeTu1", CombineRateConfig.rateTrungDeTu1);
                res.put("rateTrungDeTu2", CombineRateConfig.rateTrungDeTu2);
                res.put("rateTrungDeTu3", CombineRateConfig.rateTrungDeTu3);
                res.put("rateZeno", CombineRateConfig.rateZeno);
                sendSuccess(exchange, "OK", res);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    if (body.has("skhVip1")) CombineRateConfig.skhVip1 = body.get("skhVip1").getAsInt();
                    if (body.has("skhVip2")) CombineRateConfig.skhVip2 = body.get("skhVip2").getAsInt();
                    if (body.has("skhThuong")) CombineRateConfig.skhThuong = body.get("skhThuong").getAsInt();
                    if (body.has("rateBongTai2")) CombineRateConfig.rateBongTai2 = body.get("rateBongTai2").getAsInt();
                    if (body.has("rateBongTai3")) CombineRateConfig.rateBongTai3 = body.get("rateBongTai3").getAsInt();
                    if (body.has("rateZeno")) CombineRateConfig.rateZeno = body.get("rateZeno").getAsInt();

                    if (body.has("rateNangCapDo") && body.get("rateNangCapDo").isJsonArray()) {
                        JsonArray arr = body.getAsJsonArray("rateNangCapDo");
                        for (int i = 0; i < Math.min(arr.size(), CombineRateConfig.rateNangCapDo.length); i++) {
                            CombineRateConfig.rateNangCapDo[i] = arr.get(i).getAsDouble();
                        }
                    }

                    CombineRateConfig.save();
                    sendSuccess(exchange, "Đã cập nhật tỉ lệ đập đồ thành công!", null);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi cập nhật: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Mob Drops

    // =========================================================================
    //  API: Vòng Quay Thỏi Vàng
    // =========================================================================

    private static class VongQuayHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> res = new HashMap<>();
                res.put("idVatPhamQuay", VongQuayThoiVangConfig.idVatPhamQuay);
                res.put("soLuongVatPhamQuay", VongQuayThoiVangConfig.soLuongVatPhamQuay);
                res.put("mocMoQuaHiem", VongQuayThoiVangConfig.mocMoQuaHiem);
                res.put("tiLeMoiMonSieuHiem", VongQuayThoiVangConfig.tiLeMoiMonSieuHiem);
                res.put("tiLeNhom5", VongQuayThoiVangConfig.tiLeNhom5);
                res.put("tiLeNhom15", VongQuayThoiVangConfig.tiLeNhom15);
                res.put("soLuongHongNgoc", VongQuayThoiVangConfig.soLuongHongNgoc);
                res.put("vangMin", VongQuayThoiVangConfig.vangMin);
                res.put("vangMax", VongQuayThoiVangConfig.vangMax);
                sendSuccess(exchange, "OK", res);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    if (body.has("idVatPhamQuay")) VongQuayThoiVangConfig.idVatPhamQuay = body.get("idVatPhamQuay").getAsInt();
                    if (body.has("soLuongVatPhamQuay")) VongQuayThoiVangConfig.soLuongVatPhamQuay = body.get("soLuongVatPhamQuay").getAsInt();
                    if (body.has("mocMoQuaHiem")) VongQuayThoiVangConfig.mocMoQuaHiem = body.get("mocMoQuaHiem").getAsInt();
                    if (body.has("tiLeMoiMonSieuHiem")) VongQuayThoiVangConfig.tiLeMoiMonSieuHiem = body.get("tiLeMoiMonSieuHiem").getAsInt();
                    if (body.has("tiLeNhom5")) VongQuayThoiVangConfig.tiLeNhom5 = body.get("tiLeNhom5").getAsInt();
                    if (body.has("tiLeNhom15")) VongQuayThoiVangConfig.tiLeNhom15 = body.get("tiLeNhom15").getAsInt();
                    if (body.has("soLuongHongNgoc")) VongQuayThoiVangConfig.soLuongHongNgoc = body.get("soLuongHongNgoc").getAsInt();
                    if (body.has("vangMin")) VongQuayThoiVangConfig.vangMin = body.get("vangMin").getAsInt();
                    if (body.has("vangMax")) VongQuayThoiVangConfig.vangMax = body.get("vangMax").getAsInt();
                    VongQuayThoiVangConfig.save();
                    sendSuccess(exchange, "Đã lưu cấu hình vòng quay thỏi vàng!", null);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Giftcode (CRUD)
    // =========================================================================

    private static class GiftcodeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> list = new ArrayList<>();
                try (Connection conn = DBConnecter.getConnectionServer();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT id, code, count_left, detail, datecreate, expired FROM giftcode ORDER BY id DESC")) {
                    while (rs.next()) {
                        Map<String, Object> gc = new HashMap<>();
                        gc.put("id", rs.getInt("id"));
                        gc.put("code", rs.getString("code"));
                        gc.put("countLeft", rs.getInt("count_left"));
                        gc.put("detail", rs.getString("detail"));
                        gc.put("date", rs.getString("datecreate"));
                        gc.put("dateCreate", rs.getString("datecreate"));
                        gc.put("expired", rs.getString("expired"));
                        list.add(gc);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi đọc giftcode: " + e.getMessage());
                    return;
                }
                sendSuccess(exchange, "OK", list);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "create";

                    if ("delete".equalsIgnoreCase(action)) {
                        int id = body.get("id").getAsInt();
                        try (Connection conn = DBConnecter.getConnectionServer();
                             PreparedStatement ps = conn.prepareStatement("DELETE FROM giftcode WHERE id = ?")) {
                            ps.setInt(1, id);
                            ps.executeUpdate();
                        }
                        try { managers.GiftCodeManager.gI().init(); } catch (Exception ignored) {}
                        sendSuccess(exchange, "Đã xóa Giftcode ID " + id, null);
                    } else { // Create
                        String code = body.get("code").getAsString().trim().toUpperCase();
                        int countLeft = body.has("countLeft") ? body.get("countLeft").getAsInt() : (body.has("limit") ? body.get("limit").getAsInt() : 100);
                        int expireDays = body.has("expireDays") ? body.get("expireDays").getAsInt() : 30;
                        String detail = body.has("detail") ? body.get("detail").getAsString() : (body.has("items") ? body.get("items").toString() : "[]");
                        int allGender = body.has("allGender") ? body.get("allGender").getAsInt() : 1;
                        String playerName = body.has("playerName") ? body.get("playerName").getAsString().trim() : "";

                        try (Connection conn = DBConnecter.getConnectionServer();
                             PreparedStatement ps = conn.prepareStatement(
                                 "INSERT INTO giftcode (code, count_left, detail, datecreate, expired, allGender, player_name) VALUES (?, ?, ?, NOW(), DATE_ADD(NOW(), INTERVAL ? DAY), ?, ?)")) {
                            ps.setString(1, code);
                            ps.setInt(2, countLeft);
                            ps.setString(3, detail);
                            ps.setInt(4, expireDays);
                            ps.setInt(5, allGender);
                            ps.setString(6, playerName);
                            ps.executeUpdate();
                        }
                        try { managers.GiftCodeManager.gI().init(); } catch (Exception ignored) {}
                        sendSuccess(exchange, "Đã tạo mã Giftcode " + code + " thành công!", null);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi xử lý Giftcode: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Mail (Gửi thư In-Game)
    // =========================================================================

    private static class TargetAccountInfo {
        int accountId = -1;
        String username = "";
        long playerId = -1;
        String playerName = "";
    }

    private static TargetAccountInfo resolveTargetAccount(String target) {
        TargetAccountInfo info = new TargetAccountInfo();
        if (target == null || target.trim().isEmpty()) return info;
        target = target.trim();

        try (Connection con = DBConnecter.getConnectionServer()) {
            // 1. Check account table by username
            try (PreparedStatement ps = con.prepareStatement("SELECT id, username FROM account WHERE username = ?")) {
                ps.setString(1, target);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        info.accountId = rs.getInt("id");
                        info.username = rs.getString("username");
                    }
                }
            }
            if (info.accountId != -1) {
                try (PreparedStatement ps = con.prepareStatement("SELECT id, name FROM player WHERE account_id = ? LIMIT 1")) {
                    ps.setInt(1, info.accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            info.playerId = rs.getLong("id");
                            info.playerName = rs.getString("name");
                        }
                    }
                }
                return info;
            }

            // 2. Check player table by character name
            try (PreparedStatement ps = con.prepareStatement("SELECT id, name, account_id FROM player WHERE name = ?")) {
                ps.setString(1, target);
                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        info.playerId = rs.getLong("id");
                        info.playerName = rs.getString("name");
                        info.accountId = rs.getInt("account_id");
                    }
                }
            }

            // 3. Check player table by numeric ID
            if (info.accountId == -1) {
                try {
                    long pid = Long.parseLong(target);
                    try (PreparedStatement ps = con.prepareStatement("SELECT id, name, account_id FROM player WHERE id = ?")) {
                        ps.setLong(1, pid);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                info.playerId = rs.getLong("id");
                                info.playerName = rs.getString("name");
                                info.accountId = rs.getInt("account_id");
                            }
                        }
                    }
                } catch (NumberFormatException ignored) {}
            }

            // If found from player, retrieve username
            if (info.accountId != -1 && info.username.isEmpty()) {
                try (PreparedStatement ps = con.prepareStatement("SELECT username FROM account WHERE id = ?")) {
                    ps.setInt(1, info.accountId);
                    try (ResultSet rs = ps.executeQuery()) {
                        if (rs.next()) {
                            info.username = rs.getString("username");
                        }
                    }
                }
            }
        } catch (Exception e) {
            Logger.logException(WebAdminServer.class, e, "resolveTargetAccount: " + target);
        }
        return info;
    }

    private static class MailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                String targetType = body.has("targetType") ? body.get("targetType").getAsString() : "single";
                String playerName = body.has("playerName") ? body.get("playerName").getAsString().trim() : "";
                if (playerName.isEmpty() && body.has("target")) {
                    playerName = body.get("target").getAsString().trim();
                }
                long playerId = body.has("playerId") ? body.get("playerId").getAsLong() : 0;
                String sender = body.has("sender") ? body.get("sender").getAsString().trim() : "Hệ Thống";
                String title = body.has("title") ? body.get("title").getAsString().trim() : "Quà Tặng Từ Ban Quản Trị";
                String content = body.has("content") ? body.get("content").getAsString().trim() : "";
                int expireDays = body.has("expireDays") ? body.get("expireDays").getAsInt() : 7;

                List<GiftDataDAO.Reward> rewards = new ArrayList<>();
                if (body.has("rewards") && body.get("rewards").isJsonArray()) {
                    JsonArray rArr = body.getAsJsonArray("rewards");
                    for (JsonElement elem : rArr) {
                        if (elem.isJsonObject()) {
                            JsonObject r = elem.getAsJsonObject();
                            int id = r.has("itemId") ? r.get("itemId").getAsInt() : (r.has("id") ? r.get("id").getAsInt() : 0);
                            int amount = r.has("amount") ? r.get("amount").getAsInt() : (r.has("quantity") ? r.get("quantity").getAsInt() : 1);
                            if (id > 0) rewards.add(new GiftDataDAO.Reward(id, amount));
                        }
                    }
                }
                if (body.has("items") && body.get("items").isJsonArray()) {
                    JsonArray rArr = body.getAsJsonArray("items");
                    for (JsonElement elem : rArr) {
                        if (elem.isJsonObject()) {
                            JsonObject r = elem.getAsJsonObject();
                            int id = r.has("itemId") ? r.get("itemId").getAsInt() : (r.has("id") ? r.get("id").getAsInt() : 0);
                            int amount = r.has("amount") ? r.get("amount").getAsInt() : (r.has("quantity") ? r.get("quantity").getAsInt() : 1);
                            if (id > 0) rewards.add(new GiftDataDAO.Reward(id, amount));
                        }
                    }
                }
                if (body.has("itemId")) {
                    int itemId = body.get("itemId").getAsInt();
                    int qty = body.has("quantity") ? body.get("quantity").getAsInt() : 1;
                    if (itemId > 0) rewards.add(new GiftDataDAO.Reward(itemId, qty));
                }
                if (body.has("gold")) {
                    long gold = body.get("gold").getAsLong();
                    if (gold > 0) {
                        rewards.add(new GiftDataDAO.Reward(76, (int) Math.min(gold, 2_000_000_000L)));
                    }
                }
                if (body.has("gem")) {
                    int gem = body.get("gem").getAsInt();
                    if (gem > 0) rewards.add(new GiftDataDAO.Reward(77, gem));
                }
                if (body.has("ruby")) {
                    int ruby = body.get("ruby").getAsInt();
                    if (ruby > 0) rewards.add(new GiftDataDAO.Reward(861, ruby));
                }

                if ("all".equalsIgnoreCase(targetType) || "all_online".equalsIgnoreCase(targetType) || playerId == -1) {
                    // Gửi toàn server
                    int count = 0;
                    List<Player> players = Client.gI().getPlayers();
                    for (Player p : players) {
                        if (p != null) {
                            GiftMailService.gI().deliver(p.id, sender, title, content, rewards, expireDays);
                            count++;
                        }
                    }
                    sendSuccess(exchange, "Đã gửi thư thành công cho " + count + " người chơi online!", null);
                } else {
                    // Gửi 1 người chơi
                    if (playerId <= 0 && !playerName.isEmpty()) {
                        Player onlinePl = Client.gI().getPlayer(playerName);
                        if (onlinePl != null) {
                            playerId = onlinePl.id;
                        } else {
                            try {
                                playerId = Long.parseLong(playerName);
                            } catch (Exception ignored) {
                                playerId = PlayerDAO.findPlayerIdByName(playerName);
                            }
                        }
                    }
                    if (playerId <= 0) {
                        sendError(exchange, 400, "Vui lòng nhập ID hoặc Tên người chơi nhận thư hợp lệ!");
                        return;
                    }
                    int mailId = GiftMailService.gI().deliver(playerId, sender, title, content, rewards, expireDays);
                    if (mailId > 0) {
                        sendSuccess(exchange, "Đã gửi thư thành công cho người chơi #" + playerId + " (Mail ID: " + mailId + ")", null);
                    } else {
                        sendError(exchange, 500, "Không thể gửi thư tới người chơi #" + playerId + " (Lỗi hệ thống mail)");
                    }
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi gửi thư: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Buff Tiền & VND
    // =========================================================================

    private static class BuffVndHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }
            try {
                JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                String target = body.has("username") ? body.get("username").getAsString().trim() : "";
                if (target.isEmpty() && body.has("target")) {
                    target = body.get("target").getAsString().trim();
                }
                int cashDanap = body.has("cashDanap") ? body.get("cashDanap").getAsInt() : (body.has("cash") ? body.get("cash").getAsInt() : 0);
                int danapGoc = body.has("danapGoc") ? body.get("danapGoc").getAsInt() : (body.has("tongNap") ? body.get("tongNap").getAsInt() : 0);
                int soTien = body.has("soTien") ? body.get("soTien").getAsInt() : (body.has("sotien") ? body.get("sotien").getAsInt() : 0);
                long gold = body.has("gold") ? body.get("gold").getAsLong() : 0;
                int gem = body.has("gem") ? body.get("gem").getAsInt() : 0;
                int ruby = body.has("ruby") ? body.get("ruby").getAsInt() : 0;

                if (target.isEmpty()) {
                    sendError(exchange, 400, "Vui lòng nhập Tên tài khoản, Tên nhân vật hoặc ID người chơi!");
                    return;
                }
                if (cashDanap <= 0 && danapGoc <= 0 && soTien <= 0 && gold <= 0 && gem <= 0 && ruby <= 0) {
                    sendError(exchange, 400, "Vui lòng nhập ít nhất một khoản tiền muốn buff!");
                    return;
                }

                TargetAccountInfo accInfo = resolveTargetAccount(target);
                if (accInfo.accountId == -1) {
                    sendError(exchange, 404, "Không tìm thấy tài khoản hoặc nhân vật: " + target);
                    return;
                }

                StringBuilder sbMsg = new StringBuilder();
                sbMsg.append("Đã buff thành công cho ").append(accInfo.username);
                if (!accInfo.playerName.isEmpty()) {
                    sbMsg.append(" (NV: ").append(accInfo.playerName).append(")");
                }
                sbMsg.append(": ");

                List<String> changes = new ArrayList<>();

                // 1. Buff Cash / Danap / VND / Sotien
                if (cashDanap > 0 || danapGoc > 0 || soTien > 0) {
                    boolean ok = PlayerDAO.buffVnd(accInfo.username, Math.max(0, cashDanap), Math.max(0, danapGoc), Math.max(0, soTien));
                    if (ok) {
                        if (cashDanap > 0) changes.add("+" + cashDanap + " VNĐ/Cash");
                        if (danapGoc > 0) changes.add("+" + danapGoc + " Tổng Nạp");
                        if (soTien > 0) changes.add("+" + soTien + " Số Tiền");
                    }
                }

                // 2. Buff In-Game Currencies (Gold, Gem, Ruby)
                if (gold > 0 || gem > 0 || ruby > 0) {
                    Player onlinePl = null;
                    if (accInfo.playerId > 0) onlinePl = Client.gI().getPlayer(accInfo.playerId);
                    if (onlinePl == null && !accInfo.playerName.isEmpty()) onlinePl = Client.gI().getPlayer(accInfo.playerName);

                    if (onlinePl != null) {
                        if (gold > 0) {
                            onlinePl.inventory.gold = Math.min(models.player.Inventory.LIMIT_GOLD, onlinePl.inventory.gold + gold);
                            changes.add("+" + gold + " Vàng");
                        }
                        if (gem > 0) {
                            onlinePl.inventory.gem = (int) Math.min(Integer.MAX_VALUE, (long) onlinePl.inventory.gem + gem);
                            changes.add("+" + gem + " Ngọc");
                        }
                        if (ruby > 0) {
                            onlinePl.inventory.ruby = (int) Math.min(Integer.MAX_VALUE, (long) onlinePl.inventory.ruby + ruby);
                            changes.add("+" + ruby + " Hồng Ngọc");
                        }
                        Service.gI().sendMoney(onlinePl);
                        Service.gI().sendThongBao(onlinePl, "Tài khoản của bạn vừa được Admin buff tiền thành công!");
                    } else if (accInfo.playerId > 0) {
                        // Offline player: deliver directly to mailbox
                        List<GiftDataDAO.Reward> rewards = new ArrayList<>();
                        if (gold > 0) {
                            rewards.add(new GiftDataDAO.Reward(76, (int) Math.min(gold, 2_000_000_000L)));
                            changes.add("+" + gold + " Vàng (Hòm Thư)");
                        }
                        if (gem > 0) {
                            rewards.add(new GiftDataDAO.Reward(77, gem));
                            changes.add("+" + gem + " Ngọc (Hòm Thư)");
                        }
                        if (ruby > 0) {
                            rewards.add(new GiftDataDAO.Reward(861, ruby));
                            changes.add("+" + ruby + " Hồng Ngọc (Hòm Thư)");
                        }
                        GiftMailService.gI().deliver(accInfo.playerId, "Hệ Thống", "Quà Nạp Tiền / Buff Tiền",
                                "Bạn nhận được tiền từ Ban Quản Trị!", rewards, 30);
                    }
                }

                sbMsg.append(String.join(", ", changes));
                sendSuccess(exchange, sbMsg.toString(), null);
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi buff tiền: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: History / Logs
    // =========================================================================

    private static class HistoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            List<Map<String, Object>> logs = new ArrayList<>();
            try (Connection conn = DBConnecter.getConnectionServer();
                 Statement st = conn.createStatement();
                 ResultSet rs = st.executeQuery("SELECT * FROM history_transaction ORDER BY id DESC LIMIT 50")) {
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("id", rs.getInt("id"));
                    log.put("playerId", rs.getInt("player_id"));
                    log.put("before", rs.getString("before"));
                    log.put("after", rs.getString("after"));
                    log.put("time", rs.getString("time"));
                    logs.add(log);
                }
            } catch (Exception ignored) {}
            sendSuccess(exchange, "OK", logs);
        }
    }

    // =========================================================================
    //  API: Bot AI
    // =========================================================================

    private static class BotAiHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> data = new HashMap<>();
                data.put("maxBot", BotManager.MAX_BOT);
                data.put("maxBotToiDa", BotManager.MAX_BOT_TOI_DA);
                data.put("botCount", BotManager.gI().getBotCount());
                data.put("isSpawning", BotManager.gI().isSpawning());

                List<Map<String, Object>> botList = new ArrayList<>();
                try {
                    for (Bot b : BotManager.gI().bot) {
                        if (b == null) continue;
                        Map<String, Object> item = new HashMap<>();
                        item.put("id", b.id);
                        item.put("name", b.name);
                        item.put("gender", b.gender);
                        item.put("type", b.type);
                        item.put("mapId", b.zone != null && b.zone.map != null ? b.zone.map.mapId : -1);
                        item.put("mapName", b.zone != null && b.zone.map != null ? b.zone.map.mapName : "N/A");
                        item.put("zoneId", b.zone != null ? b.zone.zoneId : -1);
                        item.put("hp", b.nPoint != null ? b.nPoint.hp : 0);
                        item.put("maxHp", b.nPoint != null ? b.nPoint.hpMax : 0);
                        item.put("dame", b.nPoint != null ? b.nPoint.dame : 0);
                        item.put("power", b.nPoint != null ? b.nPoint.power : 0);
                        botList.add(item);
                    }
                } catch (Exception ignored) {}
                data.put("bots", botList);
                sendSuccess(exchange, "OK", data);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "";

                    switch (action) {
                        case "setMax" -> {
                            int max = body.get("max").getAsInt();
                            int applied = BotManager.datTranBot(max);
                            sendSuccess(exchange, "Đã đặt trần Bot = " + applied, applied);
                        }
                        case "clearAll" -> {
                            int count = BotManager.gI().clearAllBots();
                            sendSuccess(exchange, "Đã xóa toàn bộ " + count + " Bot AI khỏi máy chủ!", count);
                        }
                        case "kick" -> {
                            long botId = body.get("id").getAsLong();
                            Bot found = null;
                            for (Bot b : BotManager.gI().bot) {
                                if (b.id == botId) {
                                    found = b;
                                    break;
                                }
                            }
                            if (found != null) {
                                BotManager.gI().xoaBot(found);
                                sendSuccess(exchange, "Đã kick bot: " + found.name, null);
                            } else {
                                sendError(exchange, 404, "Không tìm thấy bot ID: " + botId);
                            }
                        }
                        case "spawn" -> {
                            int type = body.has("type") ? body.get("type").getAsInt() : 0;
                            int count = body.has("count") ? body.get("count").getAsInt() : 1;

                            BotConfig cfg = new BotConfig();
                            if (body.has("nhuAccMoi")) cfg.nhuAccMoi = body.get("nhuAccMoi").getAsBoolean();
                            if (body.has("nuoiDeTu")) cfg.nuoiDeTu = body.get("nuoiDeTu").getAsBoolean();
                            if (body.has("phaPlayer")) cfg.phaPlayer = body.get("phaPlayer").getAsBoolean();
                            if (body.has("chatTheGioi")) cfg.chatTheGioi = body.get("chatTheGioi").getAsBoolean();

                            if (body.has("custom") && body.get("custom").getAsBoolean()) {
                                cfg.custom = true;
                                if (body.has("gender")) cfg.gender = body.get("gender").getAsInt();
                                if (body.has("hp")) cfg.hp = body.get("hp").getAsLong();
                                if (body.has("mp")) cfg.mp = body.get("mp").getAsLong();
                                if (body.has("dame")) cfg.dame = body.get("dame").getAsLong();
                                if (body.has("power")) cfg.power = body.get("power").getAsLong();
                                if (body.has("tiemNang")) cfg.tiemNang = body.get("tiemNang").getAsLong();
                                if (body.has("crit")) cfg.crit = body.get("crit").getAsInt();
                                if (body.has("def")) cfg.def = body.get("def").getAsInt();
                                if (body.has("head")) cfg.head = body.get("head").getAsInt();
                                if (body.has("body")) cfg.body = body.get("body").getAsInt();
                                if (body.has("leg")) cfg.leg = body.get("leg").getAsInt();
                                if (body.has("intrinsicId")) cfg.intrinsicId = body.get("intrinsicId").getAsInt();
                            }

                            int actual = BotManager.gI().spawnBots(type, count, cfg);
                            sendSuccess(exchange, "Đang khởi tạo " + actual + " Bot AI...", actual);
                        }
                        default -> sendError(exchange, 400, "Action không hợp lệ: " + action);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi điều khiển Bot AI: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Events
    // =========================================================================

    private static class EventHandler implements HttpHandler {
        private static final String[] EVENT_NAMES = {
            "1. Halloween", "2. 8/3 Quốc Tế PN", "3. Giáng Sinh", "4. Tết Nguyên Đán",
            "5. Trung Thu", "6. Giỗ Tổ", "7. Top Up (Mặc định)", "8. Pokemon",
            "9. 20/11", "10. Phở Anh Hai"
        };

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> allEvents = new ArrayList<>();
                List<Integer> activeIds = new ArrayList<>();

                File f = new File("active_event.txt");
                if (f.exists()) {
                    try (BufferedReader br = new BufferedReader(new FileReader(f))) {
                        String line = br.readLine();
                        if (line != null && !line.trim().isEmpty()) {
                            for (String part : line.split("-")) {
                                try { activeIds.add(Integer.parseInt(part.trim())); } catch (Exception ignored) {}
                            }
                        }
                    } catch (Exception ignored) {}
                }

                for (int i = 0; i < EVENT_NAMES.length; i++) {
                    Map<String, Object> ev = new HashMap<>();
                    ev.put("id", i + 1);
                    ev.put("name", EVENT_NAMES[i]);
                    ev.put("active", activeIds.contains(i + 1));
                    allEvents.add(ev);
                }

                Map<String, Object> res = new HashMap<>();
                res.put("events", allEvents);
                res.put("activeIds", activeIds);
                sendSuccess(exchange, "OK", res);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    JsonArray arr = body.getAsJsonArray("activeIds");
                    List<String> ids = new ArrayList<>();
                    for (JsonElement el : arr) {
                        ids.add(String.valueOf(el.getAsInt()));
                    }
                    java.nio.file.Files.writeString(java.nio.file.Path.of("active_event.txt"),
                            String.join("-", ids) + System.lineSeparator());
                    event.EventManager.setHalloweenEnabled(ids.contains("1"));

                    boolean restart = body.has("restart") && body.get("restart").getAsBoolean();
                    if (restart) {
                        ServerManagerUI.REQUEST_AUTO_RESTART = true;
                        new Thread(() -> {
                            try { Thread.sleep(3000); } catch (Exception ignored) {}
                            System.exit(0);
                        }).start();
                        sendSuccess(exchange, "Đã lưu sự kiện! Server sẽ khởi động lại sau 3 giây...", null);
                    } else {
                        sendSuccess(exchange, "Đã lưu cấu hình sự kiện thành công!", null);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi lưu sự kiện: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: MobMe Cleaner (Dọn rác đẻ trứng)
    // =========================================================================

    private static class MobMeCleanerHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> data = new HashMap<>();
                data.put("enabled", MobMeCleanerService.gI().isEnabled());
                data.put("lastRun", MobMeCleanerService.gI().getLastRunTime());
                data.put("lastCleared", MobMeCleanerService.gI().getLastClearedCount());
                data.put("totalCleared", MobMeCleanerService.gI().getTotalCleared());
                data.put("nextRunMs", MobMeCleanerService.gI().getNextRunMs());
                sendSuccess(exchange, "OK", data);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "";
                    if ("toggle".equalsIgnoreCase(action)) {
                        boolean enable = body.get("enabled").getAsBoolean();
                        MobMeCleanerService.gI().setEnabled(enable);
                        sendSuccess(exchange, "Đã " + (enable ? "BẬT" : "TẮT") + " chế độ tự động dọn rác 30 phút/lần!", null);
                    } else if ("cleanNow".equalsIgnoreCase(action)) {
                        int cleaned = MobMeCleanerService.gI().cleanAll();
                        sendSuccess(exchange, "Đã dọn dẹp sạch sẽ " + cleaned + " xác trứng (mobMe) trên toàn server!", cleaned);
                    } else {
                        sendError(exchange, 400, "Action không hợp lệ: " + action);
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi xử lý MobMe Cleaner: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Donate Packages (Gói quà NPC Chú Bé Đần)
    // =========================================================================

    private static class DonatePackageHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> list = new ArrayList<>();
                try (Connection conn = DBConnecter.getConnectionData();
                     Statement st = conn.createStatement();
                     ResultSet rs = st.executeQuery("SELECT * FROM donate_package ORDER BY sort_order ASC, id ASC")) {
                    while (rs.next()) {
                        Map<String, Object> p = new HashMap<>();
                        p.put("id", rs.getInt("id"));
                        p.put("name", rs.getString("name"));
                        p.put("detail", rs.getString("detail"));
                        p.put("costVnd", rs.getLong("cost_vnd"));
                        p.put("items", rs.getString("items"));
                        p.put("active", rs.getInt("active"));
                        p.put("sortOrder", rs.getInt("sort_order"));
                        list.add(p);
                    }
                    sendSuccess(exchange, "OK", list);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi đọc gói donate: " + e.getMessage());
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String action = body.has("action") ? body.get("action").getAsString() : "create";

                    try (Connection conn = DBConnecter.getConnectionData()) {
                        switch (action) {
                            case "create" -> {
                                String name = body.get("name").getAsString();
                                String detail = body.has("detail") ? body.get("detail").getAsString() : "";
                                long costVnd = body.get("costVnd").getAsLong();
                                String items = body.has("items") ? body.get("items").getAsString() : "";
                                int active = body.has("active") ? body.get("active").getAsInt() : 1;
                                int sortOrder = body.has("sortOrder") ? body.get("sortOrder").getAsInt() : 0;

                                try (PreparedStatement ps = conn.prepareStatement(
                                    "INSERT INTO donate_package (name, detail, cost_vnd, items, active, sort_order) VALUES (?, ?, ?, ?, ?, ?)")) {
                                    ps.setString(1, name);
                                    ps.setString(2, detail);
                                    ps.setLong(3, costVnd);
                                    ps.setString(4, items);
                                    ps.setInt(5, active);
                                    ps.setInt(6, sortOrder);
                                    ps.executeUpdate();
                                }
                                sendSuccess(exchange, "Đã thêm gói donate mới!", null);
                            }
                            case "update" -> {
                                int id = body.get("id").getAsInt();
                                String name = body.get("name").getAsString();
                                String detail = body.has("detail") ? body.get("detail").getAsString() : "";
                                long costVnd = body.get("costVnd").getAsLong();
                                String items = body.has("items") ? body.get("items").getAsString() : "";
                                int active = body.has("active") ? body.get("active").getAsInt() : 1;
                                int sortOrder = body.has("sortOrder") ? body.get("sortOrder").getAsInt() : 0;

                                try (PreparedStatement ps = conn.prepareStatement(
                                    "UPDATE donate_package SET name = ?, detail = ?, cost_vnd = ?, items = ?, active = ?, sort_order = ? WHERE id = ?")) {
                                    ps.setString(1, name);
                                    ps.setString(2, detail);
                                    ps.setLong(3, costVnd);
                                    ps.setString(4, items);
                                    ps.setInt(5, active);
                                    ps.setInt(6, sortOrder);
                                    ps.setInt(7, id);
                                    ps.executeUpdate();
                                }
                                sendSuccess(exchange, "Đã cập nhật gói donate ID: " + id, null);
                            }
                            case "delete" -> {
                                int id = body.get("id").getAsInt();
                                try (PreparedStatement ps = conn.prepareStatement("DELETE FROM donate_package WHERE id = ?")) {
                                    ps.setInt(1, id);
                                    ps.executeUpdate();
                                }
                                sendSuccess(exchange, "Đã xóa gói donate ID: " + id, null);
                            }
                            default -> sendError(exchange, 400, "Action không hợp lệ: " + action);
                        }
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi xử lý gói donate: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Nạp Tiền & Tra Cứu Thẻ Cào / ATM
    // =========================================================================

    private static class NapTienHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String userFilter = query.getOrDefault("user", "").trim();
            String typeFilter = query.getOrDefault("type", "Tất cả").trim();

            List<Map<String, Object>> results = new ArrayList<>();

            try (Connection con = DBConnecter.getConnectionServer()) {
                // 1. Transactions
                if (!"Card".equalsIgnoreCase(typeFilter)) {
                    String sqlTx = userFilter.isEmpty() ?
                        "SELECT created_at, username, amount, type FROM transactions ORDER BY created_at DESC LIMIT 50" :
                        "SELECT created_at, username, amount, type FROM transactions WHERE username = ? ORDER BY created_at DESC LIMIT 50";
                    try (PreparedStatement ps = con.prepareStatement(sqlTx)) {
                        if (!userFilter.isEmpty()) ps.setString(1, userFilter);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> r = new HashMap<>();
                                r.put("time", new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(rs.getTimestamp("created_at")));
                                r.put("username", rs.getString("username"));
                                r.put("amount", rs.getLong("amount"));
                                r.put("type", "ATM (" + rs.getString("type") + ")");
                                r.put("detail", "Nạp ATM / Ví điện tử");
                                results.add(r);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                // 2. Napthe
                if (!"ATM".equalsIgnoreCase(typeFilter)) {
                    String sqlCard = userFilter.isEmpty() ?
                        "SELECT time, user_nap, menhgia, loaithe, seri, mathe, status FROM napthe ORDER BY id DESC LIMIT 50" :
                        "SELECT time, user_nap, menhgia, loaithe, seri, mathe, status FROM napthe WHERE user_nap = ? ORDER BY id DESC LIMIT 50";
                    try (PreparedStatement ps = con.prepareStatement(sqlCard)) {
                        if (!userFilter.isEmpty()) ps.setString(1, userFilter);
                        try (ResultSet rs = ps.executeQuery()) {
                            while (rs.next()) {
                                Map<String, Object> r = new HashMap<>();
                                Timestamp ts = rs.getTimestamp("time");
                                r.put("time", ts != null ? new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(ts) : "N/A");
                                r.put("username", rs.getString("user_nap"));
                                r.put("amount", rs.getLong("menhgia"));
                                r.put("type", "Thẻ " + rs.getString("loaithe"));
                                int status = rs.getInt("status");
                                String stText = status == 1 ? "Thành công" : (status == 2 ? "Thẻ sai" : "Chờ xử lý");
                                r.put("detail", "Seri: " + rs.getString("seri") + " | Pin: " + rs.getString("mathe") + " [" + stText + "]");
                                results.add(r);
                            }
                        }
                    } catch (Exception ignored) {}
                }

                sendSuccess(exchange, "OK", results);
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi tra cứu nạp tiền: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Boss Editor (Đọc & Sửa BossesData)
    // =========================================================================

    private static class BossEditorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                List<Map<String, Object>> bosses = new ArrayList<>();
                try {
                    Field[] fields = BossesData.class.getDeclaredFields();
                    for (Field f : fields) {
                        if (f.getType().equals(BossData.class)) {
                            try {
                                BossData b = (BossData) f.get(null);
                                if (b != null) {
                                    Map<String, Object> item = new HashMap<>();
                                    item.put("key", f.getName());
                                    item.put("name", b.getName());
                                    item.put("gender", b.getGender());
                                    item.put("dame", b.getDame());
                                    item.put("hp", b.getHp());
                                    item.put("mapJoin", b.getMapJoin());
                                    item.put("outfit", b.getOutfit());
                                    item.put("skills", b.getSkillTemp());
                                    item.put("textS", b.getTextS());
                                    item.put("secondsRest", b.getSecondsRest());
                                    bosses.add(item);
                                }
                            } catch (Exception ignored) {}
                        }
                    }
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi đọc BossesData: " + e.getMessage());
                    return;
                }
                sendSuccess(exchange, "OK", bosses);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    String key = body.get("key").getAsString();
                    Field f = BossesData.class.getField(key);
                    BossData b = (BossData) f.get(null);
                    if (b == null) {
                        sendError(exchange, 404, "Không tìm thấy Boss: " + key);
                        return;
                    }

                    if (body.has("name")) b.setName(body.get("name").getAsString());
                    if (body.has("dame")) b.setDame(body.get("dame").getAsLong());
                    if (body.has("secondsRest")) b.setSecondsRest(body.get("secondsRest").getAsInt());
                    if (body.has("gender")) b.setGender(body.get("gender").getAsByte());

                    // Máu boss ăn theo code, không cho phép Web Admin Panel ghi đè
//                    if (body.has("hp") && body.get("hp").isJsonArray()) {
//                        JsonArray arr = body.getAsJsonArray("hp");
//                        long[] hp = new long[arr.size()];
//                        for (int i = 0; i < arr.size(); i++) hp[i] = arr.get(i).getAsLong();
//                        b.setHp(hp);
//                    }
                    if (body.has("outfit") && body.get("outfit").isJsonArray()) {
                        JsonArray arr = body.getAsJsonArray("outfit");
                        short[] outfit = new short[arr.size()];
                        for (int i = 0; i < arr.size(); i++) outfit[i] = arr.get(i).getAsShort();
                        b.setOutfit(outfit);
                    }
                    if (body.has("mapJoin") && body.get("mapJoin").isJsonArray()) {
                        JsonArray arr = body.getAsJsonArray("mapJoin");
                        int[] mapJoin = new int[arr.size()];
                        for (int i = 0; i < arr.size(); i++) mapJoin[i] = arr.get(i).getAsInt();
                        b.setMapJoin(mapJoin);
                    }

                    sendSuccess(exchange, "Đã cập nhật chỉ số Boss " + b.getName() + " trực tiếp vào RAM server!", null);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi cập nhật Boss: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: Map Editor (Danh sách map, quái, NPC, Waypoint)
    // =========================================================================

    private static class MapEditorHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
                if (query.containsKey("id")) {
                    int mapId = Integer.parseInt(query.get("id"));
                    try (Connection conn = DBConnecter.getConnectionData();
                         PreparedStatement ps = conn.prepareStatement("SELECT * FROM map_template WHERE id = ?")) {
                        ps.setInt(1, mapId);
                        try (ResultSet rs = ps.executeQuery()) {
                            if (rs.next()) {
                                Map<String, Object> map = new HashMap<>();
                                map.put("id", rs.getInt("id"));
                                map.put("name", rs.getString("name"));
                                map.put("planetId", rs.getInt("planet_id"));
                                map.put("type", rs.getInt("type"));
                                map.put("zones", rs.getInt("zones"));
                                map.put("maxPlayer", rs.getInt("max_player"));
                                map.put("tileId", rs.getInt("tile_id"));
                                map.put("bgId", rs.getInt("bg_id"));
                                map.put("bgType", rs.getInt("bg_type"));

                                String mobsStr = rs.getString("mobs");
                                String npcsStr = rs.getString("npcs");
                                String wpStr = rs.getString("waypoints");

                                map.put("mobs", parseMobsList(mobsStr));
                                map.put("npcs", parseNpcsList(npcsStr));
                                map.put("waypoints", parseWaypointsList(wpStr));

                                sendSuccess(exchange, "OK", map);
                                return;
                            }
                        }
                    } catch (Exception e) {
                        sendError(exchange, 500, "Lỗi đọc chi tiết map: " + e.getMessage());
                        return;
                    }
                    sendError(exchange, 404, "Không tìm thấy map ID: " + mapId);
                } else {
                    String planet = query.getOrDefault("planet", "");
                    String q = query.getOrDefault("q", "").trim().toLowerCase();

                    List<Map<String, Object>> maps = new ArrayList<>();
                    StringBuilder sql = new StringBuilder("SELECT id, name, planet_id, type, zones, max_player FROM map_template WHERE 1=1 ");
                    int pId = -1;
                    if (!planet.isEmpty() && !planet.equalsIgnoreCase("all") && !planet.equals("-1")) {
                        try {
                            pId = Integer.parseInt(planet);
                        } catch (Exception ignored) {}
                    }
                    if (pId >= 0) {
                        sql.append("AND planet_id = ").append(pId).append(" ");
                    }
                    if (!q.isEmpty()) {
                        sql.append("AND (LOWER(name) LIKE '%").append(q.replace("'", "''")).append("%' OR id = '").append(q.replace("'", "''")).append("') ");
                    }
                    sql.append("ORDER BY id ASC");

                    try (Connection conn = DBConnecter.getConnectionData();
                         Statement st = conn.createStatement();
                         ResultSet rs = st.executeQuery(sql.toString())) {
                        while (rs.next()) {
                            Map<String, Object> m = new HashMap<>();
                            m.put("id", rs.getInt("id"));
                            m.put("name", rs.getString("name"));
                            m.put("planetId", rs.getInt("planet_id"));
                            m.put("type", rs.getInt("type"));
                            m.put("zones", rs.getInt("zones"));
                            m.put("maxPlayer", rs.getInt("max_player"));
                            maps.add(m);
                        }
                        sendSuccess(exchange, "OK", maps);
                    } catch (Exception e) {
                        sendError(exchange, 500, "Lỗi đọc danh sách map: " + e.getMessage());
                    }
                }
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    int mapId = body.get("id").getAsInt();
                    String name = body.get("name").getAsString();
                    int planetId = body.get("planetId").getAsInt();
                    int type = body.get("type").getAsInt();
                    int zones = body.get("zones").getAsInt();
                    int maxPlayer = body.get("maxPlayer").getAsInt();
                    int tileId = body.has("tileId") ? body.get("tileId").getAsInt() : 1;
                    int bgId = body.has("bgId") ? body.get("bgId").getAsInt() : 0;
                    int bgType = body.has("bgType") ? body.get("bgType").getAsInt() : 0;

                    // Serialize Mobs thành mảng chuẩn [[temp, level, hp, x, y], ...]
                    JsonArray mobsArr = new JsonArray();
                    if (body.has("mobs") && body.get("mobs").isJsonArray()) {
                        for (JsonElement el : body.get("mobs").getAsJsonArray()) {
                            if (el.isJsonObject()) {
                                JsonObject m = el.getAsJsonObject();
                                JsonArray sub = new JsonArray();
                                sub.add(m.has("temp_id") ? m.get("temp_id").getAsInt() : 1);
                                sub.add(m.has("level") ? m.get("level").getAsInt() : 1);
                                sub.add(m.has("hp") ? m.get("hp").getAsLong() : 1000);
                                sub.add(m.has("x") ? m.get("x").getAsInt() : 0);
                                sub.add(m.has("y") ? m.get("y").getAsInt() : 0);
                                mobsArr.add(sub);
                            } else if (el.isJsonArray()) {
                                mobsArr.add(el);
                            }
                        }
                    }
                    String mobs = gson.toJson(mobsArr);

                    String npcs = body.has("npcs") ? body.get("npcs").toString() : "[]";
                    String waypoints = body.has("waypoints") ? body.get("waypoints").toString() : "[]";

                    try (Connection conn = DBConnecter.getConnectionData();
                         PreparedStatement ps = conn.prepareStatement(
                             "UPDATE map_template SET name = ?, planet_id = ?, type = ?, zones = ?, max_player = ?, tile_id = ?, bg_id = ?, bg_type = ?, mobs = ? WHERE id = ?")) {
                        ps.setString(1, name);
                        ps.setInt(2, planetId);
                        ps.setInt(3, type);
                        ps.setInt(4, zones);
                        ps.setInt(5, maxPlayer);
                        ps.setInt(6, tileId);
                        ps.setInt(7, bgId);
                        ps.setInt(8, bgType);
                        ps.setString(9, mobs);
                        ps.setInt(10, mapId);
                        ps.executeUpdate();
                    }

                    // Live sync vào bộ nhớ Manager.MAP_TEMPLATES
                    try {
                        if (Manager.MAP_TEMPLATES != null) {
                            for (models.Template.MapTemplate temp : Manager.MAP_TEMPLATES) {
                                if (temp != null && temp.id == mapId) {
                                    temp.name = name;
                                    temp.planetId = (byte) planetId;
                                    temp.type = (byte) type;
                                    temp.zones = (byte) zones;
                                    temp.maxPlayerPerZone = (byte) maxPlayer;
                                    temp.tileId = (byte) tileId;
                                    temp.bgId = (byte) bgId;
                                    temp.bgType = (byte) bgType;
                                    break;
                                }
                            }
                        }
                    } catch (Exception ignored) {}

                    sendSuccess(exchange, "Đã lưu cấu hình Map [" + mapId + "] thành công!", null);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi lưu cấu hình map: " + e.getMessage());
                }
            }
        }

        private static List<Map<String, Object>> parseMobsList(String raw) {
            List<Map<String, Object>> list = new ArrayList<>();
            if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return list;
            try {
                String cleaned = raw.replace("\\\"", "\"").trim();
                JsonElement root = new JsonParser().parse(cleaned);
                if (root.isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray()) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            try {
                                JsonElement subEl = new JsonParser().parse(el.getAsString());
                                if (subEl.isJsonArray()) {
                                    addMobFromSubArray(list, subEl.getAsJsonArray());
                                }
                            } catch (Exception ignored) {}
                        } else if (el.isJsonArray()) {
                            addMobFromSubArray(list, el.getAsJsonArray());
                        } else if (el.isJsonObject()) {
                            JsonObject obj = el.getAsJsonObject();
                            Map<String, Object> m = new HashMap<>();
                            m.put("temp_id", obj.has("temp_id") ? obj.get("temp_id").getAsInt() : (obj.has("tempId") ? obj.get("tempId").getAsInt() : 0));
                            m.put("level", obj.has("level") ? obj.get("level").getAsInt() : 1);
                            m.put("hp", obj.has("hp") ? obj.get("hp").getAsLong() : 1000);
                            m.put("x", obj.has("x") ? obj.get("x").getAsInt() : 0);
                            m.put("y", obj.has("y") ? obj.get("y").getAsInt() : 0);
                            list.add(m);
                        }
                    }
                }
            } catch (Exception e) {
                java.util.regex.Matcher mat = java.util.regex.Pattern.compile("\\[(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)\\]").matcher(raw);
                while (mat.find()) {
                    Map<String, Object> m = new HashMap<>();
                    m.put("temp_id", Integer.parseInt(mat.group(1)));
                    m.put("level", Integer.parseInt(mat.group(2)));
                    m.put("hp", Long.parseLong(mat.group(3)));
                    m.put("x", Integer.parseInt(mat.group(4)));
                    m.put("y", Integer.parseInt(mat.group(5)));
                    list.add(m);
                }
            }
            return list;
        }

        private static void addMobFromSubArray(List<Map<String, Object>> list, JsonArray arr) {
            if (arr.size() >= 5) {
                Map<String, Object> m = new HashMap<>();
                m.put("temp_id", arr.get(0).getAsInt());
                m.put("level", arr.get(1).getAsInt());
                m.put("hp", arr.get(2).getAsLong());
                m.put("x", arr.get(3).getAsInt());
                m.put("y", arr.get(4).getAsInt());
                list.add(m);
            }
        }

        private static List<Map<String, Object>> parseNpcsList(String raw) {
            List<Map<String, Object>> list = new ArrayList<>();
            if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return list;
            try {
                String cleaned = raw.replace("\\\"", "\"").trim();
                JsonElement root = new JsonParser().parse(cleaned);
                if (root.isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray()) {
                        if (el.isJsonPrimitive() && el.getAsJsonPrimitive().isString()) {
                            try {
                                JsonElement subEl = new JsonParser().parse(el.getAsString());
                                if (subEl.isJsonArray()) {
                                    addNpcFromSubArray(list, subEl.getAsJsonArray());
                                }
                            } catch (Exception ignored) {}
                        } else if (el.isJsonArray()) {
                            addNpcFromSubArray(list, el.getAsJsonArray());
                        } else if (el.isJsonObject()) {
                            JsonObject obj = el.getAsJsonObject();
                            Map<String, Object> n = new HashMap<>();
                            n.put("temp_id", obj.has("temp_id") ? obj.get("temp_id").getAsInt() : (obj.has("tempId") ? obj.get("tempId").getAsInt() : 0));
                            n.put("x", obj.has("x") ? obj.get("x").getAsInt() : 0);
                            n.put("y", obj.has("y") ? obj.get("y").getAsInt() : 0);
                            n.put("avatar", obj.has("avatar") ? obj.get("avatar").getAsInt() : 0);
                            list.add(n);
                        }
                    }
                }
            } catch (Exception e) {
                java.util.regex.Matcher mat = java.util.regex.Pattern.compile("\\[(\\d+)\\s*,\\s*(\\d+)\\s*,\\s*(\\d+)(?:\\s*,\\s*(\\d+))?\\]").matcher(raw);
                while (mat.find()) {
                    Map<String, Object> n = new HashMap<>();
                    n.put("temp_id", Integer.parseInt(mat.group(1)));
                    n.put("x", Integer.parseInt(mat.group(2)));
                    n.put("y", Integer.parseInt(mat.group(3)));
                    n.put("avatar", mat.group(4) != null ? Integer.parseInt(mat.group(4)) : 0);
                    list.add(n);
                }
            }
            return list;
        }

        private static void addNpcFromSubArray(List<Map<String, Object>> list, JsonArray arr) {
            if (arr.size() >= 3) {
                Map<String, Object> n = new HashMap<>();
                n.put("temp_id", arr.get(0).getAsInt());
                n.put("x", arr.get(1).getAsInt());
                n.put("y", arr.get(2).getAsInt());
                if (arr.size() >= 4) {
                    n.put("avatar", arr.get(3).getAsInt());
                }
                list.add(n);
            }
        }

        private static List<Map<String, Object>> parseWaypointsList(String raw) {
            List<Map<String, Object>> list = new ArrayList<>();
            if (raw == null || raw.trim().isEmpty() || raw.equals("[]")) return list;
            try {
                String cleaned = raw.replaceAll("\\[\"\\[", "[[")
                                    .replaceAll("\\]\"\\]", "]]")
                                    .replaceAll("\",\"", ",")
                                    .replace("\\\"", "\"");
                JsonElement root = new JsonParser().parse(cleaned);
                if (root.isJsonArray()) {
                    for (JsonElement el : root.getAsJsonArray()) {
                        if (el.isJsonArray()) {
                            JsonArray dtwp = el.getAsJsonArray();
                            if (dtwp.size() >= 10) {
                                Map<String, Object> wp = new HashMap<>();
                                wp.put("name", dtwp.get(0).getAsString());
                                wp.put("minX", dtwp.get(1).getAsInt());
                                wp.put("minY", dtwp.get(2).getAsInt());
                                wp.put("maxX", dtwp.get(3).getAsInt());
                                wp.put("maxY", dtwp.get(4).getAsInt());
                                wp.put("isEnter", dtwp.get(5).getAsInt() == 1);
                                wp.put("isOffline", dtwp.get(6).getAsInt() == 1);
                                wp.put("goMap", dtwp.get(7).getAsInt());
                                wp.put("goX", dtwp.get(8).getAsInt());
                                wp.put("goY", dtwp.get(9).getAsInt());
                                list.add(wp);
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            return list;
        }
    }

    // =========================================================================
    //  Boss & Drop Constants Helper
    // =========================================================================

    private static Map<String, Object> bossMap(int id, String name, String group) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("name", name);
        m.put("group", group);
        return m;
    }

    private static List<Map<String, Object>> getKnownBossList() {
        List<Map<String, Object>> list = new ArrayList<>();
        // Nhóm Boss Nappa & Fide
        list.add(bossMap(BossID.KUKU, "Kuku", "Boss Nappa"));
        list.add(bossMap(BossID.MAP_DAU_DINH, "Mập Đầu Đinh", "Boss Nappa"));
        list.add(bossMap(BossID.RAMBO, "Rambo", "Boss Nappa"));
        list.add(bossMap(BossID.FIDE, "Fide Đại Ca", "Frieza Cấp 1, 2, 3"));
        // Tiểu Đội Sát Thủ Trái Đất
        list.add(bossMap(BossID.SO_4, "Số 4 (Trái Đất)", "Tiểu Đội Sát Thủ (Trái Đất)"));
        list.add(bossMap(BossID.SO_3, "Số 3 (Trái Đất)", "Tiểu Đội Sát Thủ (Trái Đất)"));
        list.add(bossMap(BossID.SO_2, "Số 2 (Trái Đất)", "Tiểu Đội Sát Thủ (Trái Đất)"));
        list.add(bossMap(BossID.SO_1, "Số 1 (Trái Đất)", "Tiểu Đội Sát Thủ (Trái Đất)"));
        list.add(bossMap(BossID.TIEU_DOI_TRUONG, "Tiểu Đội Trưởng (Trái Đất)", "Tiểu Đội Sát Thủ (Trái Đất)"));
        // Tiểu Đội Sát Thủ Namek
        list.add(bossMap(BossID.SO_4_NM, "Số 4 (Namek)", "Tiểu Đội Sát Thủ (Namek)"));
        list.add(bossMap(BossID.SO_3_NM, "Số 3 (Namek)", "Tiểu Đội Sát Thủ (Namek)"));
        list.add(bossMap(BossID.SO_2_NM, "Số 2 (Namek)", "Tiểu Đội Sát Thủ (Namek)"));
        list.add(bossMap(BossID.SO_1_NM, "Số 1 (Namek)", "Tiểu Đội Sát Thủ (Namek)"));
        list.add(bossMap(BossID.TIEU_DOI_TRUONG_NM, "Tiểu Đội Trưởng (Namek)", "Tiểu Đội Sát Thủ (Namek)"));
        // Android & Robot
        list.add(bossMap(BossID.DR_KORE, "Dr.Kôrê", "Android & Robot"));
        list.add(bossMap(BossID.ANDROID_19, "Android 19", "Android & Robot"));
        list.add(bossMap(BossID.ANDROID_13, "Android 13", "Android & Robot"));
        list.add(bossMap(BossID.ANDROID_14, "Android 14", "Android & Robot"));
        list.add(bossMap(BossID.ANDROID_15, "Android 15", "Android & Robot"));
        list.add(bossMap(BossID.PIC, "Pic", "Android & Robot"));
        list.add(bossMap(BossID.POC, "Poc", "Android & Robot"));
        list.add(bossMap(BossID.KING_KONG, "King Kong", "Android & Robot"));
        // Cell / Xen bọ hung
        list.add(bossMap(BossID.XEN_BO_HUNG, "Xên Bọ Hung", "Xên Bọ Hung (Cell)"));
        list.add(bossMap(BossID.SIEU_BO_HUNG, "Siêu Bọ Hung", "Xên Bọ Hung (Cell)"));
        list.add(bossMap(BossID.XEN_CON_1, "Xên Con", "Xên Bọ Hung (Cell)"));
        // Black Goku & Broly
        list.add(bossMap(BossID.BLACK_GOKU, "Black Goku", "Saiyan & Tương Lai"));
        list.add(bossMap(BossID.BROLY, "Broly", "Huyền Thoại Saiyan"));
        list.add(bossMap(BossID.SUPER_BROLY, "Super Broly", "Huyền Thoại Saiyan"));
        list.add(bossMap(BossID.BROLYSSJ, "Broly SSJ", "Huyền Thoại Saiyan"));
        // Cooler & Frieza & Frost
        list.add(bossMap(BossID.COOLER, "Cooler", "Dòng Tộc Frieza"));
        list.add(bossMap(BossID.COLLER_GOLD, "Cooler Vàng", "Dòng Tộc Frieza"));
        list.add(bossMap(BossID.GOLDEN_FRIEZA, "Golden Frieza", "Dòng Tộc Frieza"));
        list.add(bossMap(BossID.CHILL_1, "Chilled Cấp 1", "Dòng Tộc Frieza"));
        list.add(bossMap(BossID.CHILL_2, "Chilled Cấp 2", "Dòng Tộc Frieza"));
        list.add(bossMap(BossID.FROST, "Frost", "Vũ Trụ 6"));
        // Mabư & Phù thủy
        list.add(bossMap(BossID.MABU_12H, "Mabư 12h", "Nhóm Mabư"));
        list.add(bossMap(BossID.MABU, "Mabư", "Nhóm Mabư"));
        list.add(bossMap(BossID.SUPERBU, "Super Bư", "Nhóm Mabư"));
        list.add(bossMap(BossID.DRABURA, "Drabura", "Vua Quỷ Dabura"));
        list.add(bossMap(BossID.BUI_BUI, "Bui Bui", "Phù Thủy Babidi"));
        list.add(bossMap(BossID.YA_CON, "Yacon", "Quái Vật Yacon"));
        // Bojack
        list.add(bossMap(BossID.BOJACK, "Bojack", "Băng Bojack"));
        list.add(bossMap(BossID.SUPER_BOJACK, "Super Bojack", "Băng Bojack"));
        list.add(bossMap(BossID.BUJIN, "Bujin", "Băng Bojack"));
        list.add(bossMap(BossID.KOGU, "Kogu", "Băng Bojack"));
        list.add(bossMap(BossID.ZANGYA, "Zangya", "Băng Bojack"));
        list.add(bossMap(BossID.BIDO, "Bido", "Băng Bojack"));
        // Thần Hủy Diệt & Thiên Sứ
        list.add(bossMap(BossID.BERUS, "Beerus", "Thần Hủy Diệt Vũ Trụ 7"));
        list.add(bossMap(BossID.WHIS_1, "Whis", "Thiên Sứ Vũ Trụ 7"));
        list.add(bossMap(BossID.CHAMPA, "Champa", "Thần Hủy Diệt Vũ Trụ 6"));
        list.add(bossMap(BossID.VADOS, "Vados", "Thiên Sứ Vũ Trụ 6"));
        // Võ đài sinh tử
        list.add(bossMap(BossID.VD_BILL, "Võ Đài ST (Bill)", "Võ Đài Sinh Tử"));
        list.add(bossMap(BossID.VD_WHIS, "Võ Đài ST (Whis)", "Võ Đài Sinh Tử"));
        list.add(bossMap(BossID.VD_CHAMPA, "Võ Đài ST (Champa)", "Võ Đài Sinh Tử"));
        list.add(bossMap(BossID.VD_VADOS, "Võ Đài ST (Vados)", "Võ Đài Sinh Tử"));
        list.add(bossMap(BossID.VD_DAI_THIEN_SU, "Võ Đài ST (Đại Thiên Sứ)", "Võ Đài Sinh Tử"));
        // Zeno & Hit & Cumber
        list.add(bossMap(BossID.ZENO, "Zeno", "Đấng Toàn Năng Zeno"));
        list.add(bossMap(BossID.HAU_VE_1, "Hậu Vệ 1", "Hậu Vệ Zeno"));
        list.add(bossMap(BossID.HAU_VE_2, "Hậu Vệ 2", "Hậu Vệ Zeno"));
        list.add(bossMap(BossID.HITNE, "Sát thủ Hit", "Sát Thủ Vũ Trụ 6"));
        list.add(bossMap(BossID.CUMBER, "Cumber", "Tà Ma Saiyan"));
        list.add(bossMap(BossID.POCOLONE, "Pôcôlô Daima", "Poc Daima"));
        // Hành Tinh Yardart
        list.add(bossMap(BossID.TAP_SU_0, "Tập Sự 0", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAP_SU_1, "Tập Sự 1", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAP_SU_2, "Tập Sự 2", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAP_SU_3, "Tập Sự 3", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAP_SU_4, "Tập Sự 4", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_0, "Tân Binh 0", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_1, "Tân Binh 1", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_2, "Tân Binh 2", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_3, "Tân Binh 3", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_4, "Tân Binh 4", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.TAN_BINH_5, "Tân Binh 5", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_0, "Chiến Binh 0", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_1, "Chiến Binh 1", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_2, "Chiến Binh 2", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_3, "Chiến Binh 3", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_4, "Chiến Binh 4", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.CHIEN_BINH_5, "Chiến Binh 5", "Hành Tinh Yardart"));
        list.add(bossMap(BossID.DOI_TRUONG_5, "Đội Trưởng 5", "Hành Tinh Yardart"));
        // Doanh Trại Độc Nhãn
        list.add(bossMap(BossID.TRUNG_UY_TRANG, "Trung Úy Trắng", "Doanh Trại Độc Nhãn"));
        list.add(bossMap(BossID.TRUNG_UY_THEP, "Trung Úy Thép", "Doanh Trại Độc Nhãn"));
        list.add(bossMap(BossID.TRUNG_UY_XANH_LO, "Trung Úy Xanh Lơ", "Doanh Trại Độc Nhãn"));
        list.add(bossMap(BossID.NINJA_AO_TIM, "Ninja Áo Tím", "Doanh Trại Độc Nhãn"));
        list.add(bossMap(BossID.ROBOT_VE_SI, "Robot Vệ Sĩ", "Doanh Trại Độc Nhãn"));
        // Destron Gas
        list.add(bossMap(BossID.HATCHIYACK, "Hatchiyack", "Destron Gas"));
        list.add(bossMap(BossID.DR_LYCHEE, "Dr Lychee", "Destron Gas"));
        // Team Rocket
        list.add(bossMap(BossID.MUSASHI, "Musashi", "Team Rocket"));
        list.add(bossMap(BossID.KOCHIRO, "Kojiro", "Team Rocket"));
        list.add(bossMap(BossID.MEOWTH, "Meowth", "Team Rocket"));
        // Đại Hội Võ Thuật
        list.add(bossMap(BossID.SOI_HEC_QUYN, "Chó Rách (Sói Hẹc Quyn)", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.O_DO, "Ba Đô (Ở Dơ)", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.XINBATO, "Xinbato", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.CHA_PA, "Cha Pa", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.PON_PUT, "Pon Put", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.CHAN_XU, "Chan Xu", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.TAU_PAY_PAY, "Tàu Pảy Pảy", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.YAMCHA, "Yamcha", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.JACKY_CHUN, "Jacky Chun", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.THIEN_XIN_HANG, "Thiên Xin Hăng", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.DRACULA, "Dracula", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.NGUOI_VO_HINH, "Người Vô Hình", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.BONG_BANG, "Bông Băng", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.VUA_QUY_SA_TANG, "Vua Quỷ Sa Tăng", "Đại Hội Võ Thuật"));
        list.add(bossMap(BossID.THO_DAU_BAC, "Thỏ Đầu Bạc", "Đại Hội Võ Thuật"));
        // Sự kiện & Boss Khác
        list.add(bossMap(BossID.SON_TINH, "Sơn Tinh", "Sự Kiện"));
        list.add(bossMap(BossID.THUY_TINH, "Thủy Tinh", "Sự Kiện"));
        list.add(bossMap(BossID.ONG_GIA_NOEL, "Ông Già Noel", "Sự Kiện Noel"));
        list.add(bossMap(BossID.AN_TROM, "Ăn Trộm", "Sự Kiện Tết"));
        list.add(bossMap(BossID.THODAICA, "Thỏ Đại Ca", "Sự Kiện Trung Thu"));
        list.add(bossMap(BossID.NHATTHAN, "Nhất Thần", "Sự Kiện Trung Thu"));
        list.add(bossMap(BossID.NGUYETTHAN, "Nguyệt Thần", "Sự Kiện Trung Thu"));
        list.add(bossMap(BossID.BE_NA, "Bé Na", "Sự Kiện"));
        list.add(bossMap(BossID.LAN_CON, "Lân Con", "Sự Kiện Tết"));
        list.add(bossMap(BossID.TUAN_LOC, "Tuần Lộc", "Sự Kiện Noel"));
        list.add(bossMap(BossID.UBU, "Cậu Bé Uub", "Boss Sự Kiện"));
        list.add(bossMap(BossID.GOGETA, "Gogeta", "Siêu Chiến Binh"));
        list.add(bossMap(BossID.MATROI, "Ma Trơi", "Sự Kiện"));
        list.add(bossMap(BossID.DOI, "Dơi Ma Cà Rồng", "Sự Kiện"));
        list.add(bossMap(BossID.BIMA, "Bí Ma", "Sự Kiện"));
        list.add(bossMap(BossID.JANEMBA, "Janemba Địa Ngục", "Địa Ngục"));
        // Dragon Ball Super / Movie
        list.add(bossMap(BossID.GOHAN_BEAST, "Gohan Beast", "Dragon Ball Super"));
        list.add(bossMap(BossID.CELL_MAX, "Cell Max", "Dragon Ball Super"));
        list.add(bossMap(BossID.BROLY_SUPER, "Broly Cương Nộ", "Dragon Ball Super"));
        list.add(bossMap(BossID.BLACK_FRIEZA, "Black Frieza", "Dragon Ball Super"));
        list.add(bossMap(BossID.MORO, "Phù Thủy Moro", "Dragon Ball Super"));
        return list;
    }

    // =========================================================================
    //  API: Boss Summon & Quick Control
    // =========================================================================

    private static class BossSummonHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> res = new HashMap<>();
                int[] counts = new int[]{0, 0, 0};
                try {
                    counts = BossManager.gI().getBossStatusCounts();
                } catch (Exception ignored) {}
                res.put("alive", counts[0]);
                res.put("respawn", counts[1]);
                res.put("waiting", counts[2]);

                res.put("summonableBosses", getKnownBossList());

                List<Map<String, Object>> liveList = new ArrayList<>();
                try {
                    for (Boss b : BossManager.gI().getBosses()) {
                        if (b != null) {
                            Map<String, Object> bm = new HashMap<>();
                            bm.put("id", b.id);
                            bm.put("name", b.name != null ? b.name : (b.data != null && b.data.length > 0 ? b.data[0].getName() : "Boss"));
                            bm.put("status", b.bossStatus != null ? b.bossStatus.name() : "N/A");
                            bm.put("mapId", b.zone != null && b.zone.map != null ? b.zone.map.mapId : -1);
                            bm.put("mapName", b.zone != null && b.zone.map != null ? b.zone.map.mapName : "N/A");
                            bm.put("hp", b.nPoint != null ? b.nPoint.hp : 0);
                            bm.put("maxHp", b.nPoint != null ? b.nPoint.hpMax : 0);
                            liveList.add(bm);
                        }
                    }
                } catch (Exception ignored) {}
                res.put("liveBosses", liveList);

                sendSuccess(exchange, "OK", res);
            } else if ("POST".equalsIgnoreCase(method)) {
                sendError(exchange, 403, "Tính năng điều khiển boss đã bị khóa.");
            }
        }
    }

    private static class BossCustomDropHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            sendSuccess(exchange, "OK", new HashMap<>());
        }
    }

    // =========================================================================
    //  API: Server Config Tuning
    // =========================================================================

    private static class ServerConfigHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            String method = exchange.getRequestMethod();

            if ("GET".equalsIgnoreCase(method)) {
                Map<String, Object> res = new HashMap<>();
                res.put("rateExpServer", Manager.RATE_EXP_SERVER);
                res.put("suKien", (int) Manager.SU_KIEN);
                res.put("enableIpLimit", Manager.ENABLE_IP_LIMIT);
                res.put("maxPerIp", Manager.MAX_PER_IP);
                res.put("maxPlayer", Manager.MAX_PLAYER);
                res.put("secondWaitLogin", (int) Manager.SECOND_WAIT_LOGIN);
                res.put("serverName", Manager.SERVER_NAME);
                res.put("serverPort", ServerManager.PORT);
                sendSuccess(exchange, "OK", res);
            } else if ("POST".equalsIgnoreCase(method)) {
                try {
                    JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                    if (body.has("rateExpServer")) {
                        int oldRate = Manager.RATE_EXP_SERVER;
                        int newRate = body.get("rateExpServer").getAsInt();
                        if (newRate > 0) {
                            Manager.RATE_EXP_SERVER = newRate;
                            if (oldRate != newRate) {
                                try {
                                    Service.gI().sendThongBaoAllPlayer("Hệ số nhân EXP toàn server cập nhật: x" + Manager.RATE_EXP_SERVER);
                                } catch (Exception ignored) {}
                            }
                        }
                    }
                    if (body.has("suKien")) {
                        Manager.SU_KIEN = (byte) body.get("suKien").getAsInt();
                    }
                    if (body.has("enableIpLimit")) {
                        Manager.ENABLE_IP_LIMIT = body.get("enableIpLimit").getAsBoolean();
                    }
                    if (body.has("maxPerIp")) {
                        int m = body.get("maxPerIp").getAsInt();
                        if (m > 0) Manager.MAX_PER_IP = m;
                    }
                    if (body.has("maxPlayer")) {
                        int mp = body.get("maxPlayer").getAsInt();
                        if (mp > 0) Manager.MAX_PLAYER = mp;
                    }
                    sendSuccess(exchange, "Đã cập nhật cấu hình máy chủ thành công!", null);
                } catch (Exception e) {
                    sendError(exchange, 500, "Lỗi cập nhật cấu hình: " + e.getMessage());
                }
            }
        }
    }

    // =========================================================================
    //  API: System Optimization & Live Reload
    // =========================================================================

    private static class SystemOptimizeHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            try {
                JsonObject body = new JsonParser().parse(readRequestBody(exchange)).getAsJsonObject();
                String action = body.has("action") ? body.get("action").getAsString() : "";

                switch (action) {
                    case "cleanRam" -> {
                        long before = Runtime.getRuntime().freeMemory();
                        System.gc();
                        long after = Runtime.getRuntime().freeMemory();
                        long freedMB = (after - before) / (1024 * 1024);
                        String msg = freedMB > 0 ? ("Đã dọn dẹp JVM Heap! Giải phóng: " + freedMB + " MB RAM.")
                                                 : "Bộ nhớ JVM đã ở trạng thái tối ưu!";
                        Map<String, Object> data = new HashMap<>();
                        data.put("freedMB", freedMB);
                        data.put("freeMemoryMB", after / (1024 * 1024));
                        data.put("totalMemoryMB", Runtime.getRuntime().totalMemory() / (1024 * 1024));
                        sendSuccess(exchange, msg, data);
                    }
                    case "cleanSessions" -> {
                        int removedNull = 0;
                        int kickedDead = 0;
                        int scanned = 0;
                        try {
                            List<ISession> all = SessionManager.gI().getSessions();
                            if (all != null) {
                                List<ISession> snapshot;
                                synchronized (all) {
                                    snapshot = new ArrayList<>(all);
                                }
                                scanned = snapshot.size();
                                for (ISession s : snapshot) {
                                    if (s == null) {
                                        synchronized (all) { all.remove(null); }
                                        removedNull++;
                                        continue;
                                    }
                                    try {
                                        if (!s.isConnected()) {
                                            if (s instanceof MySession ms) Client.gI().kickSession(ms);
                                            else SessionManager.gI().removeSession(s);
                                            kickedDead++;
                                        }
                                    } catch (Exception ignored) {
                                        kickedDead++;
                                    }
                                }
                            }
                        } catch (Exception ex) {
                            sendError(exchange, 500, "Lỗi khi quét session: " + ex.getMessage());
                            return;
                        }
                        String msg = String.format("Quét %d kết nối: đã gỡ %d kết nối rỗng + đóng %d session treo!", scanned, removedNull, kickedDead);
                        Map<String, Object> data = new HashMap<>();
                        data.put("scanned", scanned);
                        data.put("removedNull", removedNull);
                        data.put("kickedDead", kickedDead);
                        sendSuccess(exchange, msg, data);
                    }
                    case "reloadShop" -> {
                        try {
                            Manager.reloadShops();
                            sendSuccess(exchange, "Đã tải lại toàn bộ Cửa Hàng (Shops) live từ database!", null);
                        } catch (Exception ex) {
                            sendError(exchange, 500, "Lỗi tải lại shop: " + ex.getMessage());
                        }
                    }
                    case "reloadGiftcodes" -> {
                        try {
                            GiftCodeManager.gI().init();
                            sendSuccess(exchange, "Đã tải lại toàn bộ Giftcode từ database!", null);
                        } catch (Exception ex) {
                            sendError(exchange, 500, "Lỗi tải lại giftcode: " + ex.getMessage());
                        }
                    }
                    default -> sendError(exchange, 400, "Hành động tối ưu không hợp lệ: " + action);
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi thực thi tối ưu: " + e.getMessage());
            }
        }
    }

    // =========================================================================
    //  API: Player Inventory & Storage Inspector
    // =========================================================================

    private static class PlayerInventoryHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if (handleOptions(exchange)) return;
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendError(exchange, 405, "Method Not Allowed");
                return;
            }

            Map<String, String> query = parseQueryParams(exchange.getRequestURI().getQuery());
            String idStr = query.get("id");
            String nameStr = query.get("name");

            long playerId = -1;
            if (idStr != null && !idStr.isEmpty()) {
                try { playerId = Long.parseLong(idStr); } catch (Exception ignored) {}
            }

            Player onlinePl = null;
            if (playerId > 0) {
                onlinePl = Client.gI().getPlayer((int) playerId);
            }
            if (onlinePl == null && nameStr != null && !nameStr.trim().isEmpty()) {
                onlinePl = Client.gI().getPlayer(nameStr.trim());
            }

            Map<String, Object> res = new HashMap<>();

            if (onlinePl != null) {
                res.put("id", onlinePl.id);
                res.put("name", onlinePl.name);
                res.put("isOnline", true);
                res.put("gender", (int) onlinePl.gender);
                res.put("power", onlinePl.nPoint != null ? onlinePl.nPoint.power : 0);
                res.put("gold", onlinePl.inventory != null ? onlinePl.inventory.gold : 0);
                res.put("gem", onlinePl.inventory != null ? onlinePl.inventory.gem : 0);
                res.put("ruby", onlinePl.inventory != null ? onlinePl.inventory.ruby : 0);

                res.put("itemsBody", formatOnlineItems(onlinePl.inventory != null ? onlinePl.inventory.itemsBody : null));
                res.put("itemsBag", formatOnlineItems(onlinePl.inventory != null ? onlinePl.inventory.itemsBag : null));
                res.put("itemsBox", formatOnlineItems(onlinePl.inventory != null ? onlinePl.inventory.itemsBox : null));

                sendSuccess(exchange, "OK", res);
                return;
            }

            // Offline: Query DB
            String sql = "SELECT id, name, gender, data_point, data_inventory, items_body, items_bag, items_box FROM player WHERE " +
                         (playerId > 0 ? "id = ?" : "name = ?");
            try (Connection conn = DBConnecter.getConnectionServer();
                 PreparedStatement ps = conn.prepareStatement(sql)) {
                if (playerId > 0) {
                    ps.setLong(1, playerId);
                } else {
                    ps.setString(1, nameStr != null ? nameStr.trim() : "");
                }

                try (ResultSet rs = ps.executeQuery()) {
                    if (rs.next()) {
                        res.put("id", rs.getInt("id"));
                        res.put("name", rs.getString("name"));
                        res.put("isOnline", false);
                        res.put("gender", rs.getInt("gender"));

                        long power = 0;
                        String dataPoint = rs.getString("data_point");
                        if (dataPoint != null && !dataPoint.isEmpty()) {
                            try {
                                JsonArray ptArr = new JsonParser().parse(dataPoint).getAsJsonArray();
                                if (ptArr.size() > 1) power = ptArr.get(1).getAsLong();
                            } catch (Exception ignored) {}
                        }
                        res.put("power", power);

                        long gold = 0, gem = 0, ruby = 0;
                        String dataInv = rs.getString("data_inventory");
                        if (dataInv != null && !dataInv.isEmpty()) {
                            try {
                                JsonArray invArr = new JsonParser().parse(dataInv).getAsJsonArray();
                                if (invArr.size() > 0) gold = invArr.get(0).getAsLong();
                                if (invArr.size() > 1) gem = invArr.get(1).getAsLong();
                                if (invArr.size() > 2) ruby = invArr.get(2).getAsLong();
                            } catch (Exception ignored) {}
                        }
                        res.put("gold", gold);
                        res.put("gem", gem);
                        res.put("ruby", ruby);

                        res.put("itemsBody", formatOfflineItems(rs.getString("items_body")));
                        res.put("itemsBag", formatOfflineItems(rs.getString("items_bag")));
                        res.put("itemsBox", formatOfflineItems(rs.getString("items_box")));

                        sendSuccess(exchange, "OK", res);
                    } else {
                        sendError(exchange, 404, "Không tìm thấy người chơi!");
                    }
                }
            } catch (Exception e) {
                sendError(exchange, 500, "Lỗi đọc dữ liệu người chơi: " + e.getMessage());
            }
        }
    }

    private static List<Map<String, Object>> formatOnlineItems(List<Item> items) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (items == null) return list;
        int slot = 0;
        for (Item it : items) {
            if (it != null && it.isNotNullItem() && it.template != null) {
                Map<String, Object> map = new HashMap<>();
                map.put("slot", slot);
                map.put("id", (int) it.template.id);
                map.put("name", it.template.name);
                map.put("iconId", (int) it.template.iconID);
                map.put("quantity", it.quantity);
                map.put("type", (int) it.template.type);
                map.put("gender", (int) it.template.gender);
                map.put("description", it.template.description);

                List<String> optDescs = new ArrayList<>();
                if (it.itemOptions != null) {
                    for (Item.ItemOption opt : it.itemOptions) {
                        if (opt != null && opt.optionTemplate != null) {
                            String optStr = opt.optionTemplate.name;
                            optStr = optStr.replaceAll("%d", String.valueOf(opt.param)).replaceAll("#", String.valueOf(opt.param));
                            optDescs.add(optStr);
                        }
                    }
                }
                map.put("options", optDescs);
                list.add(map);
            }
            slot++;
        }
        return list;
    }

    private static List<Map<String, Object>> formatOfflineItems(String jsonStr) {
        List<Map<String, Object>> list = new ArrayList<>();
        if (jsonStr == null || jsonStr.trim().isEmpty() || jsonStr.equals("[]")) return list;
        try {
            JsonArray arr = new JsonParser().parse(jsonStr).getAsJsonArray();
            int slot = 0;
            for (JsonElement el : arr) {
                if (el == null || el.isJsonNull()) {
                    slot++;
                    continue;
                }
                try {
                    String innerStr = el.isJsonPrimitive() ? el.getAsString() : el.toString();
                    JsonArray itemData = new JsonParser().parse(innerStr).getAsJsonArray();
                    int id = itemData.get(0).getAsInt();
                    if (id != -1) {
                        int qty = itemData.get(1).getAsInt();
                        String rawOpt = (itemData.size() > 2) ? itemData.get(2).getAsString() : "[]";

                        Map<String, Object> map = new HashMap<>();
                        map.put("slot", slot);
                        map.put("id", id);

                        ItemTemplate tmpl = null;
                        try {
                            tmpl = ItemService.gI().getTemplate((short) id);
                        } catch (Exception ignored) {}

                        map.put("name", tmpl != null ? tmpl.name : ("Vật phẩm #" + id));
                        map.put("iconId", tmpl != null ? tmpl.iconID : 0);
                        map.put("quantity", qty);
                        map.put("type", tmpl != null ? (int) tmpl.type : 0);
                        map.put("gender", tmpl != null ? (int) tmpl.gender : 0);
                        map.put("description", tmpl != null ? tmpl.description : "");

                        List<String> optDescs = new ArrayList<>();
                        try {
                            JsonElement optEl = new JsonParser().parse(rawOpt);
                            if (optEl.isJsonPrimitive() && optEl.getAsJsonPrimitive().isString()) {
                                optEl = new JsonParser().parse(optEl.getAsString());
                            }
                            if (optEl.isJsonArray()) {
                                for (JsonElement o : optEl.getAsJsonArray()) {
                                    JsonArray pair = o.isJsonArray() ? o.getAsJsonArray() : (o.isJsonPrimitive() ? new JsonParser().parse(o.getAsString()).getAsJsonArray() : null);
                                    if (pair != null && pair.size() >= 2) {
                                        int optId = pair.get(0).getAsInt();
                                        int optParam = pair.get(1).getAsInt();
                                        optDescs.add(formatOptionName(optId, optParam));
                                    }
                                }
                            }
                        } catch (Exception ignored) {}

                        map.put("options", optDescs);
                        list.add(map);
                    }
                } catch (Exception ignored) {}
                slot++;
            }
        } catch (Exception ignored) {}
        return list;
    }

    private static String formatOptionName(int optId, int param) {
        try {
            for (ItemOptionTemplate ot : Manager.ITEM_OPTION_TEMPLATES) {
                if (ot.id == optId) {
                    return ot.name.replaceAll("%d", String.valueOf(param)).replaceAll("#", String.valueOf(param));
                }
            }
        } catch (Exception ignored) {}
        return "Chỉ số #" + optId + ": " + param;
    }
}

