package ru.rudoy.loadprofile.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;

class SessionTest {

    @Test
    void ordersTracesByRootStart() {
        Trace late = trace("t-1", "2026-10-07T10:00:10Z");
        Trace early = trace("t-2", "2026-10-07T10:00:02Z");

        Session session = new Session("s-1", List.of(late, early));

        assertThat(session.traces()).extracting(Trace::traceId).containsExactly("t-2", "t-1");
    }

    @Test
    void rejectsEmptyAndBlank() {
        assertThatThrownBy(() -> new Session(" ", List.of(trace("t-1", "2026-10-07T10:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Session("s-1", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Trace trace(String traceId, String start) {
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER,
                "order-service", "GET /", Instant.parse(start), Instant.parse(start).plusMillis(50),
                Map.of("session.id", "s-1"));
        return new Trace(traceId, List.of(root));
    }
}
