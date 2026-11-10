package server.web.panel;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** Real default router, cookie auth and handler; run in an isolated fixture directory. */
public final class CombineRatesRouteTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        check(!Files.exists(Path.of("src")), "Run outside the game workspace");
        Path config = Path.of("data/config/combine_rates.properties");
        Files.createDirectories(config.getParent());
        Files.writeString(config, "nang_cap_do_c1=80\nskh_vip1=45\nskh_vip2=10\nskh_thuong=45\n");

        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // No injected dispatcher: this must exercise PanelRouter.dispatch itself.
        server.createContext("/api/panel/", new PanelRouter("fixture-admin", "fixture-password"));
        server.start();
        try (var client = HttpClient.newHttpClient()) {
            String base = "http://127.0.0.1:" + server.getAddress().getPort();
            URI rates = URI.create(base + "/api/panel/combine-rates");
            var response = client.send(HttpRequest.newBuilder(rates).GET().build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 401, "Rates still require authentication");

            response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/panel/auth?action=login"))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{\"username\":\"fixture-admin\",\"password\":\"fixture-password\"}"))
                    .build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200, "Fixture login succeeds");
            String cookie = response.headers().firstValue("Set-Cookie").orElseThrow().split(";", 2)[0];
            String csrf = json(response).get("csrfToken").getAsString();

            response = client.send(HttpRequest.newBuilder(rates).header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200, "Authenticated route reaches the real handler, not 404: " + response.body());
            check(json(response).getAsJsonArray("rates").size() == 27, "All 27 configurable rates are returned");
            check(json(response).has("notes"), "Panel DTO contains the help text");

            String body = "{\"values\":{\"nangCapDo0\":2.5}}";
            URI save = URI.create(rates + "?action=rates-save");
            response = client.send(HttpRequest.newBuilder(save).header("Cookie", cookie)
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body)).build(),
                    HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 403, "Writes still require CSRF");

            response = client.send(HttpRequest.newBuilder(save).header("Cookie", cookie)
                    .header("Content-Type", "application/json").header("Origin", base).header("X-CSRF-Token", csrf)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200 && json(response).get("success").getAsBoolean(), "Save passes through the default router");
            var saved = new Properties();
            try (var in = Files.newInputStream(config)) { saved.load(in); }
            check(Double.parseDouble(saved.getProperty("nang_cap_do_c1")) == 2.5, "Save persists to the isolated fixture file");

            response = client.send(HttpRequest.newBuilder(rates).header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 200, "Reload succeeds after saving");
            var first = json(response).getAsJsonArray("rates").get(0).getAsJsonObject();
            check(first.get("key").getAsString().equals("nangCapDo0") && first.get("value").getAsDouble() == 2.5,
                    "Reload returns the saved decimal rate");

            response = client.send(HttpRequest.newBuilder(URI.create(base + "/api/panel/nonexistent"))
                    .header("Cookie", cookie).GET().build(), HttpResponse.BodyHandlers.ofString());
            check(response.statusCode() == 404, "Unknown APIs remain 404");
            System.out.println("PASS: " + checks + " default-router authentication, rates GET/save/reload and persistence checks; no live DB");
        } finally {
            server.stop(0);
        }
    }

    private static JsonObject json(HttpResponse<String> response) {
        return new JsonParser().parse(response.body()).getAsJsonObject();
    }
}
