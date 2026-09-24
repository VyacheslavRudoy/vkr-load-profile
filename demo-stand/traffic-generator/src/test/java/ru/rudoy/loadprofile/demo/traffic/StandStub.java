package ru.rudoy.loadprofile.demo.traffic;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Мини-сервер для тестов: записывает пришедшие запросы и отвечает так же,
 * как сервисы стенда, — отличие только в том, что каталог всегда отдаёт
 * одни и те же два товара.
 */
final class StandStub implements AutoCloseable {

    /** Идентификатор заказа, который заглушка возвращает на создание заказа. */
    static final String ORDER_ID = "1b0e6b2a-9c3d-4e5f-8a7b-2c3d4e5f6a7b";

    private final HttpServer server;
    private final List<RecordedRequest> requests = new CopyOnWriteArrayList<>();
    private volatile int errorStatus;

    record RecordedRequest(String method, String path, String baggage) {
    }

    StandStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort();
    }

    List<RecordedRequest> requests() {
        return List.copyOf(requests);
    }

    /** Дальше заглушка отвечает ошибкой с указанным кодом. */
    void respondWithStatus(int status) {
        this.errorStatus = status;
    }

    private void handle(HttpExchange exchange) throws IOException {
        requests.add(new RecordedRequest(
                exchange.getRequestMethod(),
                exchange.getRequestURI().toString(),
                exchange.getRequestHeaders().getFirst("baggage")));
        if (errorStatus != 0) {
            exchange.sendResponseHeaders(errorStatus, -1);
            exchange.close();
            return;
        }
        byte[] body = responseFor(exchange.getRequestURI());
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(200, body.length);
        exchange.getResponseBody().write(body);
        exchange.close();
    }

    private static byte[] responseFor(URI uri) {
        String path = uri.getPath();
        if (path.equals("/api/products")) {
            return "[{\"id\":7},{\"id\":8}]".getBytes(StandardCharsets.UTF_8);
        }
        if (path.startsWith("/api/products/")) {
            return ("{\"id\":" + path.substring("/api/products/".length()) + ",\"name\":\"Stub\"}")
                    .getBytes(StandardCharsets.UTF_8);
        }
        if (path.equals("/api/orders")) {
            return ("{\"id\":\"" + ORDER_ID + "\",\"customerId\":1}").getBytes(StandardCharsets.UTF_8);
        }
        return "{}".getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
