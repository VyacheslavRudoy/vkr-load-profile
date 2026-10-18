package ru.rudoy.loadprofile.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.model.ParameterSpec;
import ru.rudoy.loadprofile.session.Session;

class CorrelatedParametersBuilderTest {

    private final CorrelatedParametersBuilder builder = new CorrelatedParametersBuilder();

    @Test
    void linksOrderAndProductIdsToEarlierResponses() {
        String orderId = "5e2aaee4-ead4-427e-93f0-ced541654dd4";
        Session purchase = session(
                trace("t-1", "GET", "/api/products", "category=books",
                        "[{\"id\":7,\"name\":\"Headphones\"},{\"id\":12,\"name\":\"Webcam\"}]"),
                trace("t-2", "POST", "/api/orders", "customerId=5",
                        "{\"id\":\"" + orderId + "\",\"total\":99}"),
                trace("t-3", "POST", "/api/orders/" + orderId + "/items", "productId=7&quantity=2",
                        "{\"ok\":true}"),
                trace("t-4", "POST", "/api/orders/" + orderId + "/checkout", null, "{\"ok\":true}"),
                trace("t-5", "GET", "/api/orders/" + orderId, null, "{\"id\":\"" + orderId + "\"}"));

        List<ParameterSpec> specs = builder.build(List.of(purchase));

        assertThat(specs).containsExactlyInAnyOrder(
                new ParameterSpec("productId", "POST /api/orders/{uuid}/items",
                        ParameterSource.CORRELATED,
                        new ParameterSpec.Correlation("GET /api/products", "$[0].id"), List.of()),
                new ParameterSpec("uuid", "POST /api/orders/{uuid}/items",
                        ParameterSource.CORRELATED,
                        new ParameterSpec.Correlation("POST /api/orders", "$.id"), List.of()),
                new ParameterSpec("uuid", "POST /api/orders/{uuid}/checkout",
                        ParameterSource.CORRELATED,
                        new ParameterSpec.Correlation("POST /api/orders", "$.id"), List.of()),
                new ParameterSpec("uuid", "GET /api/orders/{uuid}",
                        ParameterSource.CORRELATED,
                        new ParameterSpec.Correlation("POST /api/orders", "$.id"), List.of()));
    }

    @Test
    void substringOfAValueDoesNotCount() {
        Session session = session(
                trace("t-1", "GET", "/api/products", null, "{\"ref\":\"abc-1234\"}"),
                trace("t-2", "GET", "/api/products/1234", null, null));

        assertThat(builder.build(List.of(session))).isEmpty();
    }

    @Test
    void partialMatchAcrossSessionsExcludesParameter() {
        Session matched = session(
                trace("t-1", "GET", "/api/products", null, "{\"id\":7}"),
                trace("t-2", "POST", "/api/orders/7/items", "productId=7", null));
        Session unmatched = session(
                trace("t-3", "POST", "/api/orders/9/items", "productId=9", null));

        assertThat(builder.build(List.of(matched, unmatched))).isEmpty();
    }

    @Test
    void parameterOfTheFirstRequestIsNeverCorrelated() {
        Session session = session(
                trace("t-1", "GET", "/api/products", "category=books",
                        "[{\"id\":7}]"));

        assertThat(builder.build(List.of(session))).isEmpty();
    }

    private static Session session(Trace... traces) {
        return new Session("s-1", List.of(traces));
    }

    private static final int[] counter = {0};

    private static Trace trace(String traceId, String method, String path, String query, String body) {
        Map<String, Object> attrs = new HashMap<>();
        attrs.put("http.request.method", method);
        attrs.put("url.path", path);
        attrs.put("session.id", "s-1");
        if (query != null) {
            attrs.put("url.query", query);
        }
        if (body != null) {
            attrs.put("http.response.body", body);
        }
        Instant start = Instant.parse("2026-10-18T10:00:00Z").plusSeconds(counter[0]++ * 10);
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER, "order-service",
                method + " " + path, start, start.plusMillis(100), attrs);
        return new Trace(traceId, List.of(root));
    }
}
