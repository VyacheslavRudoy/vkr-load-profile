package ru.rudoy.loadprofile.builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ThinkTimeStats;
import ru.rudoy.loadprofile.session.Session;
import ru.rudoy.loadprofile.session.SessionByIdStrategy;
import ru.rudoy.loadprofile.session.TraceAssembler;

class ThinkTimeStatsBuilderTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final ThinkTimeStatsBuilder builder = new ThinkTimeStatsBuilder();

    @Test
    void referenceSetPausesGiveExpectedStats() {
        List<Session> sessions = sessionsOfReferenceSet();

        assertThat(builder.build(sessions)).isEqualTo(new ThinkTimeStats(2180, 2858, 1160, 2953));
    }

    @Test
    void averagesMiddleSamplesForEvenMedian() {
        // паузы 100 и 300 в одной сессии, 200 и 400 в другой: 100, 200, 300, 400
        List<Session> sessions = List.of(sessionWithPauses(100, 300), sessionWithPauses(200, 400));

        assertThat(builder.build(sessions)).isEqualTo(new ThinkTimeStats(250, 400, 100, 400));
    }

    @Test
    void oddMedianTakesMiddleSample() {
        List<Session> sessions = List.of(sessionWithPauses(100, 300, 500));

        assertThat(builder.build(sessions)).isEqualTo(new ThinkTimeStats(300, 500, 100, 500));
    }

    @Test
    void rejectsSessionsWithoutPauses() {
        List<Trace> single = List.of(new Trace("t-1", List.of(root("t-1",
                Instant.parse("2026-10-20T10:00:00Z")))));

        assertThatThrownBy(() -> builder.build(List.of(new Session("s-1", single))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Session> sessionsOfReferenceSet() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));
        return new SessionByIdStrategy().group(traces);
    }

    private static Session sessionWithPauses(long... pausesMs) {
        List<Trace> traces = new ArrayList<>();
        Instant start = Instant.parse("2026-10-20T10:00:00Z");
        for (int i = 0; i <= pausesMs.length; i++) {
            traces.add(new Trace("t-" + i, List.of(root("t-" + i, start))));
            if (i < pausesMs.length) {
                start = start.plusMillis(100 + pausesMs[i]);
            }
        }
        return new Session("s-1", traces);
    }

    private static Span root(String id, Instant start) {
        return new Span(id, id + "-root", null, SpanKind.SERVER, "order-service", "GET /",
                start, start.plusMillis(100), Map.of("session.id", "s-1"));
    }
}
