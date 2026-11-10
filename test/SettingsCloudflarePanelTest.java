package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.*;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.util.*;
import java.util.concurrent.atomic.*;
import javax.imageio.ImageIO;

/** Standalone regression test: only synthetic private temp files and an in-process Cloudflare fake. */
public final class SettingsCloudflarePanelTest {
    static final HttpClient HTTP = HttpClient.newHttpClient();
    static final String TOKEN = "test_token_not_a_credential_1234567890";
    static final String ZONE = "0123456789abcdef0123456789abcdef";
    static final String RECORD = "abcdef0123456789abcdef0123456789";
    static final AtomicInteger CALLS = new AtomicInteger();
    static final List<String> MUTATIONS = new ArrayList<>();
    static String base;
    static int assertions;
    static volatile boolean badToken, upstreamError;

    static void check(boolean ok, String label) {
        assertions++;
        if (!ok) throw new AssertionError(label);
    }
    static CloudflareClient.Reply fake(HttpRequest request) {
        CALLS.incrementAndGet();
        URI uri = request.uri();
        check(uri.getScheme().equals("https") && uri.getHost().equals("api.cloudflare.com"), "fixed Cloudflare host");
        check(request.timeout().isPresent() && request.timeout().get().getSeconds() <= 12, "request timeout");
        check(request.headers().firstValue("Authorization").orElse("").equals("Bearer " + TOKEN), "server-side auth");
        if (upstreamError) return new CloudflareClient.Reply(403, "secret should never be echoed: " + TOKEN);
        String path = uri.getPath().substring("/client/v4".length());
        if (path.equals("/user/tokens/verify")) return reply("{\"status\":\"" + (badToken ? "disabled" : "active") + "\"}");
        if (path.equals("/zones")) return reply("[{\"id\":\"" + ZONE + "\",\"name\":\"example.test\",\"status\":\"active\"}]");
        if (path.equals("/zones/" + ZONE)) return reply("{\"id\":\"" + ZONE + "\",\"name\":\"example.test\"}");
        if (request.method().equals("GET") && path.endsWith("/dns_records")) {
            return reply("[{\"id\":\"" + RECORD + "\",\"type\":\"A\",\"name\":\"example.test\",\"content\":\"192.0.2.1\",\"ttl\":1,\"proxied\":false}]");
        }
        MUTATIONS.add(request.method() + " " + path);
        if (path.endsWith("/purge_cache")) check(request.method().equals("POST"), "purge POST");
        return reply("{\"id\":\"" + RECORD + "\"}");
    }
    static CloudflareClient.Reply reply(String value) {
        return new CloudflareClient.Reply(200, "{\"success\":true,\"result\":" + value + "}");
    }
    static HttpResponse<byte[]> send(String method, String path, String data) throws Exception {
        return send(method, path, data == null ? new byte[0] : data.getBytes(StandardCharsets.UTF_8), "application/json");
    }
    static HttpResponse<byte[]> send(String method, String path, byte[] data, String type) throws Exception {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + path)).timeout(java.time.Duration.ofSeconds(10));
        if (type != null) b.header("Content-Type", type);
        return HTTP.send(b.method(method, HttpRequest.BodyPublishers.ofByteArray(data)).build(), HttpResponse.BodyHandlers.ofByteArray());
    }
    static JsonObject json(HttpResponse<byte[]> response) { return new JsonParser().parse(new String(response.body(), StandardCharsets.UTF_8)).getAsJsonObject(); }
    static JsonObject request(String method, String path, String body) throws Exception {
        HttpResponse<byte[]> response = send(method, path, body);
        check(response.statusCode() == 200, method + " " + path + " returned " + response.statusCode());
        return json(response);
    }
    static void reject(int status, String method, String path, String body) throws Exception {
        check(send(method, path, body).statusCode() == status, "reject " + method + " " + path);
    }
    static byte[] image(String format) throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        image.setRGB(0, 0, 0xff00ee);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, format, out);
        return out.toByteArray();
    }
    static byte[] multipart(byte[] image, String mime) throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        out.write(("--testBoundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"anything.svg\"\r\nContent-Type: " + mime + "\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
        out.write(image);
        out.write("\r\n--testBoundary--\r\n".getBytes(StandardCharsets.US_ASCII));
        return out.toByteArray();
    }
    static HttpResponse<byte[]> upload(byte[] image, String mime) throws Exception {
        return send("POST", "logo", multipart(image, mime), "multipart/form-data; boundary=testBoundary");
    }

    public static void main(String[] args) throws Exception {
        Path data = Files.createTempDirectory("settings-module-data-");
        String previousData = System.getProperty("webpanel.data");
        System.setProperty("webpanel.data", data.toString());
        System.out.println("Injected webpanel.data=" + data + " (removed after test)");
        HttpServer server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try {
                String resource = exchange.getRequestURI().getPath().substring(1);
                if (resource.equals("logo") && !exchange.getRequestMethod().equals("POST")) SettingsCloudflareApi.serveLogo(exchange);
                else PanelSupport.sendJson(exchange, 200, SettingsCloudflareApi.handle(resource, exchange));
            } catch (PanelSupport.PanelException e) {
                PanelSupport.sendJson(exchange, e.status, Map.of("success", false, "message", e.getMessage()));
            } catch (Exception e) {
                e.printStackTrace();
                PanelSupport.sendJson(exchange, 500, Map.of("success", false));
            }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort() + "/";
        try (AutoCloseable fake = SettingsCloudflareApi.useCloudflareTransportForTests(SettingsCloudflarePanelTest::fake)) {
            check(request("GET", "settings", null).get("fpgmail").getAsString().isEmpty(), "empty initial settings");
            JsonObject save = request("POST", "settings", "{\"appName\":\"test\",\"fpgmail\":\"synthetic-secret\",\"domain\":\"https://example.test\"}");
            check(save.getAsJsonObject("settings").get("fpgmail").getAsString().equals("********"), "settings save masked");
            check(!save.toString().contains("synthetic-secret"), "settings no secret leak");
            request("POST", "settings", "{\"fpgmail\":\"********\",\"fname\":\"brand\"}");
            request("POST", "settings", "{\"fpgmail\":\"\"}");
            check(PanelFiles.readJson("settings.json").get("fpgmail").getAsString().equals("synthetic-secret"), "secret roundtrip preserved");
            request("POST", "settings", "{\"fpgmail\":null}");
            check(PanelFiles.readJson("settings.json").get("fpgmail").getAsString().isEmpty(), "explicit null clears secret");
            reject(400, "POST", "settings", "{\"domain\":\"javascript:alert(1)\"}");
            reject(400, "POST", "settings", "{\"logo\":\"x\\\" onerror=\\\"alert(1)\"}");
            reject(400, "POST", "settings", "{\"unknown\":\"value\"}");
            reject(405, "DELETE", "settings", null);
            JsonObject web = request("GET", "website", null);
            check(web.get("storedOnly").getAsBoolean() && !web.get("paymentRuntimeConnected").getAsBoolean(), "stored-only website contract");
            check(web.getAsJsonArray("fields").size() == 21, "complete config metadata");
            request("POST", "website?action=save", "{\"values\":{\"site_name\":\"first\",\"payos_api_key\":\"synthetic-payment-secret\",\"bank_rate\":\"100\"}}");
            web = request("POST", "website?action=save", "{\"values\":{\"site_name\":\"second\",\"payos_api_key\":\"********\"}}");
            check(!web.toString().contains("synthetic-payment-secret"), "website secrets masked");
            check(web.getAsJsonArray("backups").size() == 1, "private backup created");
            String backup = web.getAsJsonArray("backups").get(0).getAsJsonObject().get("name").getAsString();
            reject(400, "POST", "website?action=restore", "{\"backup\":\"../website.json\"}");
            reject(400, "POST", "website?action=restore", "{\"backup\":\"C:/secret.json\"}");
            reject(400, "POST", "website?action=save", "{\"values\":{\"bank_rate\":\"1.5\"}}");
            reject(400, "POST", "website?action=save", "{\"values\":{\"site_url\":\"https://user:pass@example.test\"}}");
            web = request("POST", "website?action=restore", "{\"backup\":\"" + backup + "\"}");
            check(web.getAsJsonObject("config").get("site_name").getAsString().equals("first"), "backup restored");
            check(PanelFiles.readJson("website.json").get("payos_api_key").getAsString().equals("synthetic-payment-secret"), "restored private secret");
            for (int i = 0; i < 27; i++) request("POST", "website?action=save", "{\"values\":{\"site_name\":\"version" + i + "\"}}");
            check(request("GET", "website", null).getAsJsonArray("backups").size() == 25, "bounded backup retention");
            check(upload(image("png"), "image/png").statusCode() == 200, "PNG upload actual decoded");
            HttpResponse<byte[]> logo = send("GET", "logo", (String) null);
            check(logo.headers().firstValue("Content-Type").orElse("").equals("image/png") && ImageIO.read(new ByteArrayInputStream(logo.body())) != null, "PNG served via API");
            check(send("HEAD", "logo", (String) null).body().length == 0, "logo HEAD has no body");
            check(upload(image("jpeg"), "image/jpeg").statusCode() == 200, "JPEG upload");
            check(upload("<svg onload=alert(1)>".getBytes(), "image/svg+xml").statusCode() == 415, "SVG rejected");
            check(upload("<html>bad</html>".getBytes(), "image/png").statusCode() == 415, "HTML rejected despite image MIME");
            check(upload(image("png"), "image/jpeg").statusCode() == 415, "MIME spoof rejected");
            check(upload(new byte[] {(byte)137,80,78,71,13,10,26,10,1,2}, "image/png").statusCode() == 400, "corrupt PNG rejected");
            check(upload(new byte[2*1024*1024+1], "image/png").statusCode() == 413, "image size limit");
            check(upload(new byte[] {82,73,70,70,4,0,0,0,87,69,66,80}, "image/webp").statusCode() == 415, "WebP fails closed without decoder");
            check(!request("GET", "cloudflare", null).get("tokenSet").getAsBoolean() && CALLS.get() == 0, "unconfigured status makes no network calls");
            reject(400, "GET", "cloudflare?action=purge", null);
            reject(400, "POST", "cloudflare?action=token", "{\"token\":\"short\"}");
            check(CALLS.get() == 0, "invalid token blocked before network");
            String tokenBody = "{\"token\":\"" + TOKEN + "\"}";
            badToken = true;
            reject(400, "POST", "cloudflare?action=token", tokenBody);
            badToken = false;
            JsonObject cf = request("POST", "cloudflare?action=token", tokenBody);
            check(cf.get("tokenSet").getAsBoolean() && cf.getAsJsonArray("zones").size() == 1 && !cf.toString().contains(TOKEN), "validated token server side only");
            reject(409, "POST", "cloudflare?action=purge", "{}");
            int before = CALLS.get();
            reject(400, "POST", "cloudflare?action=zone", "{\"zoneId\":\"../../evil\"}");
            check(before == CALLS.get(), "zone path injection blocked");
            cf = request("POST", "cloudflare?action=zone", "{\"zoneId\":\"" + ZONE + "\",\"zoneName\":\"evil.test\"}");
            check(cf.get("zoneName").getAsString().equals("example.test"), "zone name trusted from Cloudflare only");
            String record = "{\"type\":\"A\",\"name\":\"@\",\"content\":\"192.0.2.42\",\"ttl\":300,\"proxied\":true}";
            request("POST", "cloudflare?action=record-create", record);
            request("POST", "cloudflare?action=record-update", record.substring(0,record.length()-1)+",\"id\":\""+RECORD+"\"}");
            request("POST", "cloudflare?action=record-delete", "{\"id\":\""+RECORD+"\"}");
            request("POST", "cloudflare?action=purge", "{}");
            check(MUTATIONS.size() == 4 && MUTATIONS.get(0).startsWith("POST ") && MUTATIONS.get(1).startsWith("PUT ") && MUTATIONS.get(2).startsWith("DELETE "), "CRUD and purge use expected methods");
            before = CALLS.get();
            reject(400, "POST", "cloudflare?action=record-create", "{\"type\":\"A\",\"name\":\"foreign.test\",\"content\":\"192.0.2.1\"}");
            reject(400, "POST", "cloudflare?action=record-create", "{\"type\":\"A\",\"name\":\"@\",\"content\":\"999.1.1.1\"}");
            reject(400, "POST", "cloudflare?action=record-create", "{\"type\":\"TXT\",\"name\":\"_test\",\"content\":\"value\",\"proxied\":true}");
            reject(400, "POST", "cloudflare?action=record-delete", "{\"id\":\"https://other.test\"}");
            check(CALLS.get() == before, "invalid mutations make no upstream calls");
            upstreamError = true;
            cf = request("GET", "cloudflare", null);
            check(cf.has("error") && !cf.toString().contains(TOKEN) && !cf.toString().contains("secret should"), "upstream failure sanitized");
            upstreamError = false;
            before = CALLS.get();
            cf = request("POST", "cloudflare?action=forget", "{}");
            check(!cf.get("tokenSet").getAsBoolean() && CALLS.get() == before, "forget no upstream mutation");
            check(!Files.readString(data.resolve("cloudflare.json")).contains(TOKEN), "forget removes persisted token");
            try (var paths = Files.walk(data)) { check(paths.noneMatch(p -> p.getFileName().toString().endsWith(".tmp")), "no temporary files left"); }
            AclFileAttributeView acl = Files.getFileAttributeView(data.resolve("website.json"), AclFileAttributeView.class);
            if (acl != null) check(acl.getAcl().size() == 1 && acl.getAcl().get(0).principal().equals(acl.getOwner()), "private Windows ACL");
            System.out.println("PASS " + assertions + " assertions; all Cloudflare traffic used in-process fake; isolated config/logo/backups only.");
        } finally {
            server.stop(0);
            if (previousData == null) System.clearProperty("webpanel.data");
            else System.setProperty("webpanel.data", previousData);
            try (var paths = Files.walk(data)) { paths.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (IOException e) { throw new UncheckedIOException(e); } }); }
        }
    }
}
