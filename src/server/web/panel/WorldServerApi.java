package server.web.panel;

import com.sun.net.httpserver.HttpExchange;

/** Authenticated world/runtime routes. Authentication and JSON transport belong to the router. */
public final class WorldServerApi {
    private WorldServerApi() {}

    public static Object handle(String resource, HttpExchange exchange) throws Exception {
        PanelSupport.requireMethod(exchange, "GET", "POST");
        if ("GET".equals(exchange.getRequestMethod())) return dispatch(resource, exchange);
        // Serialize panel edits across resources (map references mob/intrinsic catalogues).
        synchronized (WorldData.EDIT_LOCK) {
            return dispatch(resource, exchange);
        }
    }

    private static Object dispatch(String resource, HttpExchange exchange) throws Exception {
        return switch (resource) {
            case "status" -> {
                PanelSupport.requireMethod(exchange, "GET");
                yield ServerRuntimeApi.status();
            }
            case "server" -> ServerRuntimeApi.handle(exchange);
            case "map" -> MapPanelData.handle(exchange);
            case "mob" -> MobPanelData.handle(exchange);
            case "boss" -> BossPanelData.handle(exchange);
            case "top" -> TopPanelData.handle(exchange);
            case "drop" -> DropPanelData.handle(exchange);
            case "boss-drop" -> BossDropPanelData.handle(exchange);
            case "combine-rates" -> CombineRatePanelData.handle(exchange);
            case "intrinsic" -> IntrinsicPanelData.handle(exchange);
            case "lucky-round" -> LuckyRoundPanelData.handle(exchange);
            default -> throw PanelSupport.error(404, "Tài nguyên world không tồn tại");
        };
    }
}
