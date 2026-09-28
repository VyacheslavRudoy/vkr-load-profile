package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpanTest {

    private static final Instant START = Instant.parse("2026-09-28T10:00:00Z");

    @Test
    void keepsAllFields() {
        Span span = new Span("4bf92f35", "00f067aa", "bbaaddeb", SpanKind.SERVER,
                "catalog-service", "GET /api/products", START, START.plusMillis(150),
                Map.of("session.id", "7c1d2a3e"));

        assertThat(span.traceId()).isEqualTo("4bf92f35");
        assertThat(span.spanId()).isEqualTo("00f067aa");
        assertThat(span.parentSpanId()).isEqualTo("bbaaddeb");
        assertThat(span.kind()).isEqualTo(SpanKind.SERVER);
        assertThat(span.serviceName()).isEqualTo("catalog-service");
        assertThat(span.name()).isEqualTo("GET /api/products");
        assertThat(span.startTime()).isEqualTo(START);
        assertThat(span.endTime()).isEqualTo(START.plusMillis(150));
        assertThat(span.attributes()).containsEntry("session.id", "7c1d2a3e");
    }

    @Test
    void durationSpansFromStartToEnd() {
        Span span = span("4bf92f35", "00f067aa", START.plusMillis(200));

        assertThat(span.duration()).isEqualTo(Duration.ofMillis(200));
    }

    @Test
    void nullAttributesBecomeEmpty() {
        Span span = span("4bf92f35", "00f067aa", START);

        assertThat(span.attributes()).isEmpty();
    }

    @Test
    void attributesAreUnmodifiable() {
        Span span = new Span("4bf92f35", "00f067aa", null, SpanKind.SERVER,
                "catalog-service", "GET /api/products", START, START,
                Map.of("session.id", "7c1d2a3e"));

        assertThatThrownBy(() -> span.attributes().put("key", "value"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rejectsUnsupportedAttributeValue() {
        assertThatThrownBy(() -> new Span("4bf92f35", "00f067aa", null, SpanKind.SERVER,
                "catalog-service", "GET /api/products", START, START, Map.of("ids", List.of("1"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("ids");
    }

    @Test
    void rejectsEndBeforeStart() {
        assertThatThrownBy(() -> span("4bf92f35", "00f067aa", START.minusMillis(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("endTime");
    }

    @Test
    void rejectsBlankIdentifier() {
        assertThatThrownBy(() -> span(" ", "00f067aa", START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("traceId");
        assertThatThrownBy(() -> span("4bf92f35", "", START))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("spanId");
    }

    private static Span span(String traceId, String spanId, Instant endTime) {
        return new Span(traceId, spanId, "bbaaddeb", SpanKind.SERVER, "catalog-service",
                "GET /api/products", START, endTime, null);
    }
}
