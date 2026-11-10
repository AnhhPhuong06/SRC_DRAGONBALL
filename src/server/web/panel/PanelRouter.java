package server.web.panel;

import com.google.gson.JsonObject;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.LongSupplier;

/** Cookie sessions for the panel; legacy bearer endpoints remain separate. */
public final class PanelRouter implements HttpHandler {
    @FunctionalInterface
    public interface Dispatcher {
        Object handle(String resource, HttpExchange exchange) throws Exception;
    }

    private record Session(String username, String csrfToken, long expiresAt) {}
    private record Attempts(int failures, long windowStart, long lockedUntil) {}
    private static final String COOKIE = "nro_panel_sid";
    private static final long HOUR = 60L * 60 * 1000;
    private static final Set<String> RESOURCES = Set.of("status", "server", "account", "player", "revenue",
            "giftcode", "giftcode/sync", "giftcode/batch", "item", "option", "shop", "map", "mob", "boss", "drop", "boss-drop", "combine-rates", "top", "intrinsic",
            "settings", "logo", "website", "cloudflare", "lucky-round", "database-reset", "donate-package");
    private final String username;
    private final String password;
    private final Dispatcher dispatcher;
    private final LongSupplier clock;
    private final boolean secure;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, Attempts> attempts = new ConcurrentHashMap<>();

    public PanelRouter(String username, String password) {
        this(username, password, PanelRouter::dispatch, System::currentTimeMillis,
                Boolean.getBoolean("webpanel.https"));
    }

    public PanelRouter(String username, String password, Dispatcher dispatcher,
            LongSupplier clock, boolean secure) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new IllegalArgumentException("Panel admin credentials are required");
        }
        this.username = username;
        this.password = password;
        this.dispatcher = dispatcher;
        this.clock = clock;
        this.secure = secure;
    }

    private static Object dispatch(String resource, HttpExchange exchange) throws Exception {
        return switch (resource) {
            case "account", "player", "revenue" -> AccountPlayerApi.handle(resource, exchange);
            case "giftcode", "giftcode/sync", "giftcode/batch", "item", "option", "shop" -> CatalogShopApi.handle(resource, exchange);
            case "status", "server", "map", "mob", "boss", "drop", "boss-drop", "combine-rates", "top", "intrinsic", "lucky-round" -> WorldServerApi.handle(resource, exchange);
            case "settings", "logo", "website", "cloudflare" -> SettingsCloudflareApi.handle(resource, exchange);
            case "database-reset" -> DatabaseResetApi.handle(exchange);
            case "donate-package" -> DonatePackageApi.handle(exchange);
            default -> throw PanelSupport.error(404, "API không tồn tại");
        };
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        String resource = "";
        boolean mutation = !Set.of("GET", "HEAD").contains(exchange.getRequestMethod());
        String actor = "anonymous";
        try {
            String path = exchange.getRequestURI().getPath();
            if (!path.startsWith("/api/panel/")) throw PanelSupport.error(404, "API không tồn tại");
            resource = path.substring("/api/panel/".length());
            if ("auth".equals(resource)) {
                auth(exchange);
                return;
            }
            Session session = session(exchange);
            if (session == null) throw PanelSupport.error(401, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.");
            actor = session.username();
            exchange.setAttribute("panel.resetOwner", session.csrfToken());
            if (!RESOURCES.contains(resource)) throw PanelSupport.error(404, "API không tồn tại");
            if (mutation) {
                checkOrigin(exchange);
                if (!equal(session.csrfToken(), exchange.getRequestHeaders().getFirst("X-CSRF-Token"))) {
                    throw PanelSupport.error(403, "CSRF token không hợp lệ. Hãy tải lại trang.");
                }
            }
            if ("logo".equals(resource) && !mutation) {
                SettingsCloudflareApi.serveLogo(exchange);
                return;
            }
            Object result = dispatcher.handle(resource, exchange);
            if (result == null) throw PanelSupport.error(500, "API không trả dữ liệu");
            PanelSupport.sendJson(exchange, 200, result);
            if (mutation) audit(actor, resource, PanelSupport.query(exchange).getOrDefault("action", ""), 200);
        } catch (PanelSupport.PanelException e) {
            PanelSupport.sendJson(exchange, e.status, Map.of("success", false, "error", e.getMessage()));
            if (mutation) audit(actor, resource, "rejected", e.status);
        } catch (Exception e) {
            int status = e instanceof java.sql.SQLException ? 503 : 500;
            System.err.println("[PANEL] " + resource + ": " + e.getClass().getSimpleName());
            PanelSupport.sendJson(exchange, status, Map.of("success", false, "error",
                    status == 503 ? "Không thể truy cập cơ sở dữ liệu. Kiểm tra cấu hình và kết nối." : "Không thể hoàn thành yêu cầu. Kiểm tra log server."));
            if (mutation) audit(actor, resource, "failed", status);
        } finally {
            exchange.close();
        }
    }

    private void auth(HttpExchange exchange) throws IOException {
        PanelSupport.requireMethod(exchange, "GET", "POST");
        String action = PanelSupport.query(exchange).getOrDefault("action", "");
        if ("GET".equals(exchange.getRequestMethod())) {
            if (!action.isEmpty() && !"me".equals(action)) throw PanelSupport.error(400, "Tác vụ không hợp lệ");
            Session session = session(exchange);
            PanelSupport.sendJson(exchange, 200, session == null ? Map.of("authenticated", false)
                    : Map.of("authenticated", true, "username", session.username(), "csrfToken", session.csrfToken()));
            return;
        }
        checkOrigin(exchange);
        if ("logout".equals(action)) {
            Session session = session(exchange);
            if (session == null) throw PanelSupport.error(401, "Phiên đăng nhập không còn hợp lệ");
            if (!equal(session.csrfToken(), exchange.getRequestHeaders().getFirst("X-CSRF-Token"))) {
                throw PanelSupport.error(403, "CSRF token không hợp lệ");
            }
            sessions.remove(cookie(exchange));
            setCookie(exchange, "", 0);
            PanelSupport.sendJson(exchange, 200, Map.of("authenticated", false, "success", true, "message", "Đã đăng xuất"));
            return;
        }
        if (!"login".equals(action)) throw PanelSupport.error(400, "Tác vụ không hợp lệ");
        long now = clock.getAsLong();
        String ip = exchange.getRemoteAddress().getAddress().getHostAddress();
        Attempts attempt = attempts.get(ip);
        if (attempt != null && attempt.lockedUntil() > now) throw PanelSupport.error(429, "Đăng nhập sai quá nhiều lần. Thử lại sau 15 phút.");
        JsonObject body = PanelSupport.body(exchange);
        String user = PanelSupport.required(body, "username");
        String pass = PanelSupport.required(body, "password");
        if (user.length() > 128 || pass.length() > 1024) throw PanelSupport.error(400, "Thông tin đăng nhập quá dài");
        if (!equal(username, user) || !equal(password, pass)) {
            attempts.compute(ip, (key, previous) -> {
                int failures = previous == null || now - previous.windowStart() > 15 * 60_000
                        ? 1 : previous.failures() + 1;
                return new Attempts(failures, previous == null || now - previous.windowStart() > 15 * 60_000
                        ? now : previous.windowStart(), failures >= 5 ? now + 15 * 60_000 : 0);
            });
            attempts.entrySet().removeIf(entry -> now - entry.getValue().windowStart() > HOUR);
            throw PanelSupport.error(401, "Tài khoản hoặc mật khẩu không chính xác");
        }
        attempts.remove(ip);
        sessions.entrySet().removeIf(entry -> entry.getValue().expiresAt() <= now);
        if (sessions.size() >= 1000) throw PanelSupport.error(429, "Quá nhiều phiên đăng nhập");
        String previous = cookie(exchange);
        if (previous != null) sessions.remove(previous);
        boolean remember = PanelSupport.bool(body, "remember", false);
        long lifetime = (remember ? 30 * 24 : 12) * HOUR;
        String id = token();
        Session session = new Session(username, token(), now + lifetime);
        sessions.put(id, session);
        setCookie(exchange, id, remember ? lifetime / 1000 : -1);
        PanelSupport.sendJson(exchange, 200, Map.of("authenticated", true, "username", username,
                "csrfToken", session.csrfToken()));
    }

    private Session session(HttpExchange exchange) {
        String id = cookie(exchange);
        if (id == null) return null;
        Session value = sessions.get(id);
        if (value != null && value.expiresAt() <= clock.getAsLong()) {
            sessions.remove(id);
            return null;
        }
        return value;
    }

    private String cookie(HttpExchange exchange) {
        String header = exchange.getRequestHeaders().getFirst("Cookie");
        if (header == null) return null;
        for (String part : header.split(";")) {
            String value = part.trim();
            if (value.startsWith(COOKIE + "=")) return value.substring(COOKIE.length() + 1);
        }
        return null;
    }

    private void setCookie(HttpExchange exchange, String id, long maxAge) {
        exchange.getResponseHeaders().add("Set-Cookie", COOKIE + "=" + id
                + "; Path=/api/panel; HttpOnly; SameSite=Strict"
                + (secure ? "; Secure" : "") + (maxAge >= 0 ? "; Max-Age=" + maxAge : ""));
    }

    private void checkOrigin(HttpExchange exchange) {
        String origin = exchange.getRequestHeaders().getFirst("Origin");
        String host = exchange.getRequestHeaders().getFirst("Host");
        String site = exchange.getRequestHeaders().getFirst("Sec-Fetch-Site");
        if ("cross-site".equals(site) || (origin != null && !origin.equals((secure ? "https://" : "http://") + host))) {
            throw PanelSupport.error(403, "Chỉ chấp nhận yêu cầu cùng nguồn");
        }
    }

    private String token() {
        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static boolean equal(String expected, String actual) {
        return actual != null && MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }

    private static void audit(String actor, String resource, String action, int status) {
        System.out.println("[PANEL AUDIT] user=" + clean(actor) + " resource=" + clean(resource)
                + " action=" + clean(action) + " status=" + status);
    }

    private static String clean(String value) {
        return value.replaceAll("[^a-zA-Z0-9_./-]", "_");
    }
}
