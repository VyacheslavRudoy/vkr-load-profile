package ru.rudoy.loadprofile.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
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
        Trace late = trace("t-1", "10:00:10", "10:00:11");
        Trace early = trace("t-2", "10:00:02", "10:00:03");

        Session session = new Session("s-1", List.of(late, early));

        assertThat(session.traces()).extracting(Trace::traceId).containsExactly("t-2", "t-1");
    }

    @Test
    void rejectsEmptyAndBlank() {
        assertThatThrownBy(() -> new Session(" ", List.of(trace("t-1", "10:00:00", "10:00:01"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Session("s-1", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void thinkTimesSpanFromEndOfOneTraceToStartOfNext() {
        Session session = new Session("s-1", List.of(
                trace("t-1", "10:00:00", "10:00:02"),
                trace("t-2", "10:00:05", "10:00:06"),
                trace("t-3", "10:00:09", "10:00:10")));

        assertThat(session.thinkTimes()).containsExactly(Duration.ofSeconds(3), Duration.ofSeconds(3));
    }

    @Test
    void overlappingTracesGiveZeroPause() {
        Session session = new Session("s-1", List.of(
                trace("t-1", "10:00:00", "10:00:05"),
                trace("t-2", "10:00:03", "10:00:04")));

        assertThat(session.thinkTimes()).containsExactly(Duration.ZERO);
    }

    @Test
    void singleTraceHasNoPauses() {
        Session session = new Session("s-1", List.of(trace("t-1", "10:00:00", "10:00:01")));

        assertThat(session.thinkTimes()).isEmpty();
    }

    private static Trace trace(String traceId, String start, String end) {
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER,
                "order-service", "GET /", Instant.parse("2026-10-07T" + start + "Z"),
                Instant.parse("2026-10-07T" + end + "Z"), Map.of("session.id", "s-1"));
        return new Trace(traceId, List.of(root));
    }
}
