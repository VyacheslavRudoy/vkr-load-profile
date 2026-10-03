package ru.rudoy.loadprofile.ingest;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;

/**
 * Мини-сервер Jaeger для тестов: помнит последний пришедший запрос
 * и отвечает заранее заданными кодом и телом.
 */
final class JaegerStub implements AutoCloseable {

    private final HttpServer server;
    private volatile URI lastRequestUri;
    private volatile int status = 200;
    private volatile String body = "{\"result\":{\"resourceSpans\":[]}}";

    JaegerStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", this::handle);
        server.start();
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/";
    }

    URI lastRequestUri() {
        return lastRequestUri;
    }

    void respondWith(int status, String body) {
        this.status = status;
        this.body = body;
    }

    private void handle(HttpExchange exchange) throws IOException {
        lastRequestUri = exchange.getRequestURI();
        byte[] payload = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
