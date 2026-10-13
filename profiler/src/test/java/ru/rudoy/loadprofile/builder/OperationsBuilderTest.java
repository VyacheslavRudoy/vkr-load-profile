package ru.rudoy.loadprofile.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.Operation;
import ru.rudoy.loadprofile.session.TraceAssembler;

class OperationsBuilderTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final OperationsBuilder builder = new OperationsBuilder();

    @Test
    void countsReferenceSetRootOperations() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));

        Map<String, Long> counts = new java.util.LinkedHashMap<>();
        for (var operation : builder.build(traces)) {
            counts.put(operation.method() + " " + operation.pathTemplate(), operation.count());
        }

        assertThat(counts).containsExactlyInAnyOrderEntriesOf(Map.of(
                "GET /api/products", 6L,
                "GET /api/products/{id}", 4L,
                "GET /api/orders/{uuid}", 2L,
                "POST /api/orders", 2L,
                "POST /api/orders/{uuid}/items", 2L,
                "POST /api/orders/{uuid}/checkout", 2L));
    }

    @Test
    void sharesSumToOne() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));

        double total = builder.build(traces).stream().mapToDouble(o -> o.share()).sum();

        assertThat(total).isCloseTo(1.0, within(1e-9));
    }

    @Test
    void countsOnlyRootSpans() {
        // трасса с дочерним вызовом каталога: операция одна — POST, не GET
        Span root = span("t-1", "t-1", null, "POST", "/api/orders/5e2aaee4-ead4-427e-93f0-ced541654dd4/items");
        Span child = span("t-1", "t-1-child", "t-1", "GET", "/api/products/7");

        List<Operation> operations = builder.build(List.of(new Trace("t-1", List.of(root, child))));

        assertThat(operations).hasSize(1);
        assertThat(operations.get(0).method()).isEqualTo("POST");
        assertThat(operations.get(0).share()).isEqualTo(1.0);
    }

    @Test
    void rejectsRootWithoutMethod() {
        Span root = new Span("t-1", "t-1", null, SpanKind.SERVER, "order-service",
                "GET /", Instant.parse("2026-10-14T10:00:00Z"), Instant.parse("2026-10-14T10:00:00.100Z"),
                Map.of("url.path", "/api/products"));

        assertThatThrownBy(() -> builder.build(List.of(new Trace("t-1", List.of(root)))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("http.request.method");
    }

    private static Span span(String traceId, String spanId, String parentSpanId,
                             String method, String path) {
        return new Span(traceId, spanId, parentSpanId, SpanKind.SERVER, "order-service",
                method + " " + path, Instant.parse("2026-10-14T10:00:00Z"),
                Instant.parse("2026-10-14T10:00:00.100Z"),
                Map.of("http.request.method", method, "url.path", path));
    }
}
