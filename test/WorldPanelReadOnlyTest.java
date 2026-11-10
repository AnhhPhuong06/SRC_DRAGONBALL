package server.web.panel;

import com.google.gson.*;
import com.sun.net.httpserver.HttpServer;
import database.AlyraManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;

/** Explicit read-only integration check against the configured game database. Never starts the game. */
public final class WorldPanelReadOnlyTest {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        for (String resource : new String[]{"mob", "map"}) {
            server.createContext("/" + resource, exchange -> {
                int status = 200;
                Object result;
                try { result = WorldServerApi.handle(resource, exchange); }
                catch (PanelSupport.PanelException error) { status = error.status; result = java.util.Map.of("error", error.getMessage()); }
                catch (Exception error) { status = 500; result = java.util.Map.of("error", error.getClass().getSimpleName()); }
                byte[] data = new Gson().toJson(result).getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
                exchange.sendResponseHeaders(status, data.length);
                try (var stream = exchange.getResponseBody()) { stream.write(data); }
            });
        }
        server.start();
        try {
            HttpClient client = HttpClient.newHttpClient();
            for (String resource : new String[]{"mob", "map"}) {
                URI uri = URI.create("http://127.0.0.1:" + server.getAddress().getPort() + "/" + resource + "?limit=1000");
                HttpResponse<String> response = client.send(HttpRequest.newBuilder(uri).GET().build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200) throw new AssertionError(resource + ": " + response.statusCode() + " " + response.body());
                JsonObject body = new JsonParser().parse(response.body()).getAsJsonObject();
                int total = body.get("total").getAsInt();
                if (total <= 0 || body.getAsJsonArray(resource + "s").size() != total) throw new AssertionError("Incomplete " + resource + " list");
                if (resource.equals("map")) {
                    for (JsonElement entry : body.getAsJsonArray("maps")) {
                        JsonObject map = entry.getAsJsonObject();
                        int id = map.get("id").getAsInt();
                        if ((id == 136 && map.get("mobCount").getAsInt() != 8)
                                || (id == 169 && map.get("mobCount").getAsInt() != 6)) {
                            throw new AssertionError("Missing mob spawns on map " + id);
                        }
                    }
                }
                System.out.println("PASS: GET " + resource + " HTTP 200; total=" + total);
            }
        } finally {
            server.stop(0);
            AlyraManager.close();
            AlyraManager.shutdownDataPool();
        }
    }
}
