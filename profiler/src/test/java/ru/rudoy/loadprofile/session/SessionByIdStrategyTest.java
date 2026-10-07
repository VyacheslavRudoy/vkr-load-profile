package ru.rudoy.loadprofile.session;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;

class SessionByIdStrategyTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final SessionByIdStrategy strategy = new SessionByIdStrategy();

    @Test
    void groupsReferenceSetIntoSixSessions() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));

        List<Session> sessions = strategy.group(traces);

        assertThat(sessions).hasSize(6);
        assertThat(sessions.stream().mapToInt(session -> session.traces().size()).sum()).isEqualTo(18);
        assertThat(sessions).allSatisfy(session -> {
            assertThat(session.traces()).allSatisfy(trace ->
                    assertThat(trace.rootSpan().orElseThrow().attributes().get("session.id"))
                            .isEqualTo(session.sessionId()));
            assertThat(session.traces()).isSortedAccordingTo(
                    Comparator.comparing(trace -> trace.rootSpan().orElseThrow().startTime()));
        });
    }

    @Test
    void keepsOrderOfFirstAppearanceAndSortsInside() {
        Trace lateA = trace("t-a1", "s-1", "2026-10-07T10:00:10Z");
        Trace firstB = trace("t-b1", "s-2", "2026-10-07T10:00:01Z");
        Trace earlyA = trace("t-a2", "s-1", "2026-10-07T10:00:02Z");

        List<Session> sessions = strategy.group(List.of(lateA, firstB, earlyA));

        assertThat(sessions).extracting(Session::sessionId).containsExactly("s-1", "s-2");
        assertThat(sessions.get(0).traces()).extracting(Trace::traceId).containsExactly("t-a2", "t-a1");
    }

    @Test
    void rejectsTraceWithoutSessionId() {
        Trace anonymous = trace("t-x", null, "2026-10-07T10:00:00Z");

        assertThatThrownBy(() -> strategy.group(List.of(anonymous)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("t-x");
    }

    private static Trace trace(String traceId, String sessionId, String start) {
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER,
                "order-service", "GET /", Instant.parse(start), Instant.parse(start).plusMillis(50),
                sessionId == null ? null : Map.of("session.id", sessionId));
        return new Trace(traceId, List.of(root));
    }
}
