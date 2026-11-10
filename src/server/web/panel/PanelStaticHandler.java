package server.web.panel;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Serves public assets only. Private panel configuration is never a static resource. */
public final class PanelStaticHandler implements HttpHandler {
    private static final Pattern ASSET = Pattern.compile("/(?:css|js|assets)/(?:[a-zA-Z0-9_-]+/)*[a-zA-Z0-9_.-]+\\.(?:css|js|png|jpg|jpeg|gif|webp|ico|woff|woff2|ttf)");
    private static final Pattern ICON = Pattern.compile("/game-data/icon/x[1-4]/[0-9]{1,6}\\.png");
    private final Path webRoot;
    private final Path gameRoot;
    private final boolean classpath;

    public PanelStaticHandler() {
        this(Path.of(System.getProperty("webpanel.dir", "web")), Path.of("data"), true);
    }

    public PanelStaticHandler(Path webRoot, Path gameRoot, boolean classpath) {
        this.webRoot = webRoot.toAbsolutePath().normalize();
        this.gameRoot = gameRoot.toAbsolutePath().normalize();
        this.classpath = classpath;
    }

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        try {
            PanelSupport.requireMethod(exchange, "GET", "HEAD");
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/api") || path.startsWith("/api/")) throw PanelSupport.error(404, "API không tồn tại");
            if ("/".equals(path) || "/panel".equals(path) || "/panel/".equals(path)) path = "/index.html";
            if (path.indexOf('\\') >= 0 || path.indexOf('\0') >= 0 || path.contains("..")) {
                throw PanelSupport.error(403, "Forbidden");
            }
            byte[] bytes;
            if (ICON.matcher(path).matches()) {
                bytes = readFile(gameRoot, path.substring("/game-data/".length()));
            } else {
                if (!"/index.html".equals(path) && !"/favicon.ico".equals(path) && !ASSET.matcher(path).matches()) {
                    throw PanelSupport.error(404, "Không tìm thấy tài nguyên");
                }
                bytes = readFile(webRoot, path.substring(1));
                if (bytes == null && classpath) {
                    try (InputStream input = getClass().getResourceAsStream("/web" + path)) {
                        if (input != null) bytes = input.readNBytes(16 * 1024 * 1024 + 1);
                    }
                }
            }
            if (bytes == null) throw PanelSupport.error(404, "Không tìm thấy tài nguyên");
            if (bytes.length > 16 * 1024 * 1024) throw PanelSupport.error(413, "Tài nguyên quá lớn");
            exchange.getResponseHeaders().set("Content-Type", contentType(path));
            exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
            exchange.getResponseHeaders().set("Referrer-Policy", "same-origin");
            exchange.getResponseHeaders().set("X-Frame-Options", "DENY");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            if ("HEAD".equals(exchange.getRequestMethod())) {
                exchange.getResponseHeaders().set("Content-Length", String.valueOf(bytes.length));
                exchange.sendResponseHeaders(200, -1);
            } else {
                exchange.sendResponseHeaders(200, bytes.length);
                try (var output = exchange.getResponseBody()) { output.write(bytes); }
            }
        } catch (PanelSupport.PanelException e) {
            PanelSupport.sendJson(exchange, e.status, Map.of("success", false, "error", e.getMessage()));
        } finally {
            exchange.close();
        }
    }

    private static byte[] readFile(Path root, String relative) throws IOException {
        if (!Files.isDirectory(root)) return null;
        Path file = root.resolve(relative).normalize();
        if (!file.startsWith(root)) throw PanelSupport.error(403, "Forbidden");
        if (!Files.isRegularFile(file)) return null;
        Path realRoot = root.toRealPath();
        if (!file.toRealPath().startsWith(realRoot)) throw PanelSupport.error(403, "Forbidden");
        if (Files.size(file) > 16 * 1024 * 1024) throw PanelSupport.error(413, "Tài nguyên quá lớn");
        return Files.readAllBytes(file);
    }

    private static String contentType(String path) {
        String extension = path.substring(path.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
        return switch (extension) {
            case "html" -> "text/html; charset=UTF-8";
            case "css" -> "text/css; charset=UTF-8";
            case "js" -> "application/javascript; charset=UTF-8";
            case "png" -> "image/png";
            case "jpg", "jpeg" -> "image/jpeg";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "ico" -> "image/x-icon";
            case "woff" -> "font/woff";
            case "woff2" -> "font/woff2";
            case "ttf" -> "font/ttf";
            default -> "application/octet-stream";
        };
    }
}
