package server.web.panel;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Flow;
import java.util.concurrent.TimeUnit;
import static server.web.panel.PanelSupport.*;

/** Fixed-host Cloudflare transport. Injection is in-process only, never an HTTP option. */
public final class CloudflareClient {
    private static final String BASE = "https://api.cloudflare.com/client/v4";
    private static final int MAX_RESPONSE = 2 * 1024 * 1024;
    private final Transport transport;

    @FunctionalInterface
    public interface Transport { Reply send(HttpRequest request) throws Exception; }

    public static final class Reply {
        public final int status;
        public final String body;
        public Reply(int status, String body) { this.status = status; this.body = body; }
    }

    CloudflareClient() { this(new JavaTransport()); }
    CloudflareClient(Transport transport) { this.transport = java.util.Objects.requireNonNull(transport); }

    JsonObject call(String method, String path, String token, JsonObject payload) {
        if (!path.startsWith("/") || path.contains("..") || !path.matches("/[A-Za-z0-9_/?=&-]+")) {
            throw error(400, "Đường dẫn Cloudflare không hợp lệ");
        }
        if (!token.matches("[A-Za-z0-9_-]{20,256}")) throw error(400, "API Token Cloudflare không hợp lệ");
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(BASE + path))
                .timeout(Duration.ofSeconds(12)).header("Authorization", "Bearer " + token)
                .header("Accept", "application/json").header("User-Agent", "Java-WebPanel/1.0");
        if (payload == null) builder.method(method, HttpRequest.BodyPublishers.noBody());
        else builder.header("Content-Type", "application/json").method(method,
                HttpRequest.BodyPublishers.ofString(GSON.toJson(payload), StandardCharsets.UTF_8));
        Reply reply;
        try { reply = transport.send(builder.build()); }
        catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw error(502, "Yêu cầu Cloudflare bị gián đoạn");
        } catch (Exception e) {
            // Do not expose exception text, request headers, tokens or upstream response content.
            throw error(502, "Không kết nối được Cloudflare trong thời gian cho phép");
        }
        if (reply == null || reply.body == null || reply.body.length() > MAX_RESPONSE) {
            throw error(502, "Phản hồi Cloudflare quá lớn hoặc không hợp lệ");
        }
        if (reply.status < 200 || reply.status >= 300) {
            throw error(reply.status == 429 ? 429 : 502, "Cloudflare từ chối yêu cầu (HTTP " + reply.status + ")");
        }
        try {
            JsonObject object = new JsonParser().parse(reply.body).getAsJsonObject();
            if (!object.has("success") || !object.get("success").getAsBoolean()) {
                throw error(502, "Cloudflare không chấp nhận yêu cầu; kiểm tra quyền API Token và dữ liệu");
            }
            if (!object.has("result") || object.get("result").isJsonNull()) throw new IllegalArgumentException();
            return object;
        } catch (PanelException e) { throw e; }
        catch (RuntimeException e) { throw error(502, "JSON trả về từ Cloudflare không hợp lệ"); }
    }

    JsonObject object(String method, String path, String token, JsonObject payload) {
        try { return call(method, path, token, payload).getAsJsonObject("result"); }
        catch (PanelException e) { throw e; }
        catch (RuntimeException e) { throw error(502, "Dữ liệu Cloudflare không hợp lệ"); }
    }

    JsonArray list(String path, String token) {
        JsonArray all = new JsonArray();
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(30);
        for (int page = 1; page <= 20; page++) {
            if (System.nanoTime() > deadline) throw error(504, "Đọc danh sách Cloudflare quá thời gian cho phép");
            JsonObject response = call("GET", path + "?per_page=100&page=" + page, token, null);
            try {
                JsonArray rows = response.getAsJsonArray("result");
                if (rows == null || rows.size() > 100) throw new IllegalArgumentException();
                for (JsonElement row : rows) {
                    if (!row.isJsonObject()) throw new IllegalArgumentException();
                    all.add(row);
                }
                int pages = response.has("result_info") && response.get("result_info").isJsonObject()
                        && response.getAsJsonObject("result_info").has("total_pages")
                        ? response.getAsJsonObject("result_info").get("total_pages").getAsInt()
                        : rows.size() < 100 ? page : page + 1;
                if (pages > 20) throw error(502, "Danh sách Cloudflare vượt giới hạn an toàn 2000 mục");
                if (page >= pages) return all;
            } catch (PanelException e) { throw e; }
            catch (RuntimeException e) { throw error(502, "Danh sách Cloudflare không hợp lệ"); }
        }
        throw error(502, "Không đọc được toàn bộ danh sách Cloudflare");
    }

    private static final class JavaTransport implements Transport {
        private final HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER).build();

        @Override public Reply send(HttpRequest request) throws Exception {
            CompletableFuture<HttpResponse<byte[]>> pending = client.sendAsync(request, info -> new LimitedBody());
            try {
                HttpResponse<byte[]> response = pending.get(15, TimeUnit.SECONDS);
                return new Reply(response.statusCode(), new String(response.body(), StandardCharsets.UTF_8));
            } finally {
                if (!pending.isDone()) pending.cancel(true);
            }
        }
    }

    /** Cancel at the byte limit, rather than buffering an unbounded upstream body. */
    private static final class LimitedBody implements HttpResponse.BodySubscriber<byte[]> {
        private final HttpResponse.BodySubscriber<byte[]> delegate = HttpResponse.BodySubscribers.ofByteArray();
        private Flow.Subscription subscription;
        private long size;
        @Override public CompletionStage<byte[]> getBody() { return delegate.getBody(); }
        @Override public void onSubscribe(Flow.Subscription value) {
            subscription = value;
            delegate.onSubscribe(value);
        }
        @Override public void onNext(List<ByteBuffer> buffers) {
            for (ByteBuffer buffer : buffers) size += buffer.remaining();
            if (size > MAX_RESPONSE) {
                subscription.cancel();
                delegate.onError(new java.io.IOException("Cloudflare response limit"));
            } else delegate.onNext(buffers);
        }
        @Override public void onError(Throwable error) { delegate.onError(error); }
        @Override public void onComplete() { delegate.onComplete(); }
    }
}
