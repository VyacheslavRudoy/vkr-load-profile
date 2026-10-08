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

class SessionByClientStrategyTest {

    private final SessionByClientStrategy strategy =
            new SessionByClientStrategy(Duration.ofMinutes(30));

    @Test
    void splitsClientSessionsOnInactivity() {
        Trace first = trace("t-1", "42", "10:00:00", "10:01:00");
        Trace second = trace("t-2", "42", "10:05:00", "10:05:30");
        Trace afterLongPause = trace("t-3", "42", "10:40:00", "10:40:10");

        List<Session> sessions = strategy.group(List.of(first, second, afterLongPause));

        assertThat(sessions).extracting(Session::sessionId).containsExactly("42#1", "42#2");
        assertThat(sessions.get(0).traces()).extracting(Trace::traceId).containsExactly("t-1", "t-2");
        assertThat(sessions.get(1).traces()).extracting(Trace::traceId).containsExactly("t-3");
    }

    @Test
    void pauseEqualToTimeoutKeepsSessionTogether() {
        Trace first = trace("t-1", "42", "10:00:00", "10:01:00");
        Trace exactlyThirtyMinutesLater = trace("t-2", "42", "10:31:00", "10:31:05");

        List<Session> sessions = strategy.group(List.of(first, exactlyThirtyMinutesLater));

        assertThat(sessions).extracting(Session::sessionId).containsExactly("42#1");
    }

    @Test
    void sortsTracesBeforeSplitting() {
        Trace late = trace("t-2", "42", "10:05:00", "10:05:30");
        Trace early = trace("t-1", "42", "10:00:00", "10:01:00");

        List<Session> sessions = strategy.group(List.of(late, early));

        assertThat(sessions).hasSize(1);
        assertThat(sessions.get(0).traces()).extracting(Trace::traceId).containsExactly("t-1", "t-2");
    }

    @Test
    void keepsClientsApart() {
        Trace ofA = trace("t-a", "42", "10:00:00", "10:01:00");
        Trace ofB = trace("t-b", "7", "10:02:00", "10:02:30");

        List<Session> sessions = strategy.group(List.of(ofA, ofB));

        assertThat(sessions).extracting(Session::sessionId).containsExactly("42#1", "7#1");
    }

    @Test
    void rejectsTraceWithoutClientId() {
        Trace anonymous = trace("t-x", null, "10:00:00", "10:01:00");

        assertThatThrownBy(() -> strategy.group(List.of(anonymous)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("t-x");
    }

    private static Trace trace(String traceId, String client, String start, String end) {
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER,
                "order-service", "POST /", Instant.parse("2026-10-08T" + start + "Z"),
                Instant.parse("2026-10-08T" + end + "Z"),
                client == null ? null : Map.of("enduser.id", client));
        return new Trace(traceId, List.of(root));
    }
}
