package server.web.panel;

import com.sun.net.httpserver.HttpExchange;

/** Account/player operations for the authenticated embedded panel. */
public final class AccountPlayerApi {
    private AccountPlayerApi() {}

    public static Object handle(String resource, HttpExchange exchange) throws Exception {
        return switch (resource) {
            case "account" -> AccountDataApi.handle(exchange);
            case "player" -> PlayerDataApi.handle(exchange);
            case "revenue" -> RevenueDataApi.handle(exchange);
            default -> throw PanelSupport.error(404, "Tài nguyên không tồn tại");
        };
    }
}
