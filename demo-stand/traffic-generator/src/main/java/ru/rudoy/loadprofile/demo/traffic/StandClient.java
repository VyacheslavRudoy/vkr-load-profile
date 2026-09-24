package ru.rudoy.loadprofile.demo.traffic;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Обращения к стенду от имени одной пользовательской сессии.
 * <p>
 * Каждый запрос несёт заголовок {@code baggage: session.id=...}, поэтому
 * все трассы сессии получают атрибут {@code session.id}. Методы повторяют
 * API сервисов стенда и возвращают только то, что нужно сценариям:
 * идентификаторы товаров и заказов.
 */
final class StandClient {

    private final HttpClient http;
    private final String sessionId;
    private final URI catalogBase;
    private final URI ordersBase;
    private final ObjectMapper json;
    private final ThinkTime thinkTime;

    StandClient(HttpClient http, String sessionId, URI catalogBase, URI ordersBase,
                ObjectMapper json, ThinkTime thinkTime) {
        this.http = http;
        this.sessionId = sessionId;
        this.catalogBase = withoutTrailingSlash(catalogBase);
        this.ordersBase = withoutTrailingSlash(ordersBase);
        this.json = json;
        this.thinkTime = thinkTime;
    }

    /** Идентификаторы всех товаров каталога. */
    List<Long> productIds() throws IOException, InterruptedException {
        return productIdsOf(get(catalogBase + "/api/products"));
    }

    /** Идентификаторы товаров одной категории. */
    List<Long> productIds(String category) throws IOException, InterruptedException {
        return productIdsOf(get(catalogBase + "/api/products?category=" + category));
    }

    /** Открыть страницу товара. */
    void openProduct(long id) throws IOException, InterruptedException {
        get(catalogBase + "/api/products/" + id);
    }

    /** Создать заказ и вернуть его идентификатор. */
    String createOrder(long customerId) throws IOException, InterruptedException {
        JsonNode order = post(ordersBase + "/api/orders?customerId=" + customerId);
        String id = order.path("id").asText();
        if (id.isEmpty()) {
            throw new IOException("order service returned no id: " + order);
        }
        return id;
    }

    /** Добавить товар в заказ. */
    void addItem(String orderId, long productId, int quantity) throws IOException, InterruptedException {
        post(ordersBase + "/api/orders/" + orderId + "/items?productId=" + productId + "&quantity=" + quantity);
    }

    /** Подтвердить заказ. */
    void checkout(String orderId) throws IOException, InterruptedException {
        post(ordersBase + "/api/orders/" + orderId + "/checkout");
    }

    /** Открыть страницу заказа. */
    void openOrder(String orderId) throws IOException, InterruptedException {
        get(ordersBase + "/api/orders/" + orderId);
    }

    /** Пауза между шагами сценария: время обдумывания пользователя. */
    void think(Random random) throws InterruptedException {
        Thread.sleep(thinkTime.sampleMs(random));
    }

    private List<Long> productIdsOf(JsonNode body) {
        List<Long> ids = new ArrayList<>();
        for (JsonNode product : body) {
            ids.add(product.path("id").asLong());
        }
        if (ids.isEmpty()) {
            throw new IllegalStateException("catalog returned no products");
        }
        return ids;
    }

    private JsonNode get(String url) throws IOException, InterruptedException {
        return send(request(url).GET().build());
    }

    private JsonNode post(String url) throws IOException, InterruptedException {
        return send(request(url).POST(HttpRequest.BodyPublishers.noBody()).build());
    }

    private HttpRequest.Builder request(String url) {
        return HttpRequest.newBuilder(URI.create(url))
                .header("baggage", "session.id=" + sessionId)
                .timeout(Duration.ofSeconds(10));
    }

    private JsonNode send(HttpRequest request) throws IOException, InterruptedException {
        HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IOException(request.method() + " " + request.uri() + " returned "
                    + response.statusCode() + ": " + response.body());
        }
        return json.readTree(response.body());
    }

    /** Базовый адрес без завершающего слэша, чтобы конкатенация с путями давала корректный URI. */
    private static URI withoutTrailingSlash(URI base) {
        String text = base.toString();
        return text.endsWith("/") ? URI.create(text.substring(0, text.length() - 1)) : base;
    }
}
