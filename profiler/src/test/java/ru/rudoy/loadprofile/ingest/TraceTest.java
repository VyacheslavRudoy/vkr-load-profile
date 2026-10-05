package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class TraceTest {

    private static final Instant TIME = Instant.parse("2026-09-28T10:00:00Z");

    @Test
    void groupsSpansOfOneRequest() {
        Span server = span("00f067aa", null, SpanKind.SERVER, TIME, TIME.plusMillis(50));
        Span client = span("bbaaddeb", "00f067aa", SpanKind.CLIENT,
                TIME.plusMillis(5), TIME.plusMillis(10));

        Trace trace = new Trace("4bf92f35", List.of(server, client));

        assertThat(trace.traceId()).isEqualTo("4bf92f35");
        assertThat(trace.spans()).containsExactly(server, client);
    }

    @Test
    void rejectsSpanFromAnotherTrace() {
        Span stranger = new Span("deadbeef", "00f067aa", null, SpanKind.SERVER,
                "catalog-service", "GET /api/products", TIME, TIME, null);

        assertThatThrownBy(() -> new Trace("4bf92f35", List.of(stranger)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("deadbeef");
    }

    @Test
    void rejectsEmptyTrace() {
        assertThatThrownBy(() -> new Trace("4bf92f35", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void spansAreUnmodifiable() {
        Trace trace = new Trace("4bf92f35",
                List.of(span("00f067aa", null, SpanKind.SERVER, TIME, TIME)));

        assertThatThrownBy(() -> trace.spans().add(
                span("bbaaddeb", "00f067aa", SpanKind.CLIENT, TIME, TIME)))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void rootIsTheSpanWithoutParent() {
        Span child = span("bbaaddeb", "00f067aa", SpanKind.CLIENT,
                TIME.plusMillis(5), TIME.plusMillis(10));
        Span root = span("00f067aa", null, SpanKind.SERVER, TIME, TIME.plusMillis(50));

        Trace trace = new Trace("4bf92f35", List.of(child, root));

        assertThat(trace.rootSpan()).contains(root);
    }

    @Test
    void rootCanBeASpanWhoseParentIsOutsideTheTrace() {
        Span orphan = span("00f067aa", "deadbeef", SpanKind.SERVER, TIME, TIME);

        Trace trace = new Trace("4bf92f35", List.of(orphan));

        assertThat(trace.rootSpan()).contains(orphan);
    }

    @Test
    void severalOutsideParentsGiveTheEarliestSpan() {
        Span later = span("bbb", "deadbeef", SpanKind.SERVER,
                TIME.plusSeconds(1), TIME.plusSeconds(2));
        Span earlier = span("ccc", "deadbeef", SpanKind.SERVER, TIME, TIME.plusSeconds(1));

        Trace trace = new Trace("4bf92f35", List.of(later, earlier));

        assertThat(trace.rootSpan()).contains(earlier);
    }

    @Test
    void parentCycleLeavesNoRoot() {
        Span first = span("aaa", "bbb", SpanKind.SERVER, TIME, TIME);
        Span second = span("bbb", "aaa", SpanKind.SERVER, TIME, TIME);

        Trace trace = new Trace("4bf92f35", List.of(first, second));

        assertThat(trace.rootSpan()).isEmpty();
    }

    private static Span span(String spanId, String parentSpanId, SpanKind kind,
                             Instant start, Instant end) {
        return new Span("4bf92f35", spanId, parentSpanId, kind, "order-service",
                "POST /api/orders", start, end, null);
    }
}
