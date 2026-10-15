package ru.rudoy.loadprofile.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.IntervalRate;
import ru.rudoy.loadprofile.session.Session;
import ru.rudoy.loadprofile.session.SessionByIdStrategy;
import ru.rudoy.loadprofile.session.TraceAssembler;

class ArrivalRateBuilderTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final ArrivalRateBuilder builder = new ArrivalRateBuilder();

    @Test
    void splitsWindowIntoBucketsWithRates() {
        List<Session> sessions = List.of(
                session("s-1", "10:00:00"), session("s-2", "10:00:10"), session("s-3", "10:02:10"));

        ArrivalRate rate = builder.build(sessions, window("10:00:00", "10:04:00"), Duration.ofMinutes(1));

        assertThat(rate.sessionsPerMinute()).isEqualTo(0.75);
        assertThat(rate.byInterval()).extracting(IntervalRate::value)
                .containsExactly(2.0, 0.0, 1.0, 0.0);
    }

    @Test
    void clipsLastBucketToWindowEnd() {
        List<Session> sessions = List.of(session("s-1", "10:00:20"));

        ArrivalRate rate = builder.build(sessions, window("10:00:00", "10:01:30"), Duration.ofMinutes(1));

        assertThat(rate.byInterval()).hasSize(2);
        assertThat(rate.byInterval().get(1).from()).isEqualTo(Instant.parse("2026-10-20T10:01:00Z"));
        assertThat(rate.byInterval().get(1).to()).isEqualTo(Instant.parse("2026-10-20T10:01:30Z"));
        assertThat(rate.byInterval().get(1).value()).isEqualTo(0.0);
        assertThat(rate.sessionsPerMinute()).isCloseTo(1.0 / 1.5, within(1e-9));
    }

    @Test
    void referenceSetFitsOneBucket() {
        List<Session> sessions = sessionsOfReferenceSet();
        Instant from = sessions.stream().flatMap(s -> s.traces().stream())
                .map(t -> t.rootSpan().orElseThrow().startTime()).min(Comparator.naturalOrder()).orElseThrow();
        Instant to = sessions.stream().flatMap(s -> s.traces().stream())
                .map(t -> t.rootSpan().orElseThrow().endTime()).max(Comparator.naturalOrder()).orElseThrow();

        ArrivalRate rate = builder.build(sessions, new TimeWindow(from, to), Duration.ofMinutes(1));

        assertThat(rate.byInterval()).hasSize(1);
        assertThat(rate.byInterval().get(0).from()).isEqualTo(from);
        assertThat(rate.byInterval().get(0).to()).isEqualTo(to);
        assertThat(rate.sessionsPerMinute()).isCloseTo(17.2690445, within(1e-6));
    }

    @Test
    void rejectsSessionOutsideWindow() {
        Session late = session("s-late", "10:05:00");

        assertThatThrownBy(() -> builder.build(List.of(late), window("10:00:00", "10:01:00"),
                Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("s-late");
    }

    private static List<Session> sessionsOfReferenceSet() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));
        return new SessionByIdStrategy().group(traces);
    }

    private static TimeWindow window(String from, String to) {
        return new TimeWindow(Instant.parse("2026-10-20T" + from + "Z"),
                Instant.parse("2026-10-20T" + to + "Z"));
    }

    private static Session session(String sessionId, String start) {
        Span root = new Span("t-" + sessionId, sessionId + "-root", null, SpanKind.SERVER,
                "order-service", "GET /", Instant.parse("2026-10-20T" + start + "Z"),
                Instant.parse("2026-10-20T" + start + "Z").plusMillis(100),
                Map.of("session.id", sessionId));
        return new Session(sessionId, List.of(new Trace("t-" + sessionId, List.of(root))));
    }
}
