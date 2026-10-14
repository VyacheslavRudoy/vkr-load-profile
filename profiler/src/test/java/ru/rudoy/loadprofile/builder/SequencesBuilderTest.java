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
import ru.rudoy.loadprofile.session.Session;
import ru.rudoy.loadprofile.session.SessionByIdStrategy;
import ru.rudoy.loadprofile.session.TraceAssembler;

class SequencesBuilderTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final SequencesBuilder builder = new SequencesBuilder();

    @Test
    void extractsReferenceSetSequences() {
        List<Session> sessions = sessionsOfReferenceSet();

        List<ru.rudoy.loadprofile.model.Sequence> sequences = builder.build(sessions, 10);

        assertThat(sequences).hasSize(3);
        // при равных частотах порядок первого появления
        assertThat(sequences.get(0).steps()).containsExactly(
                "GET /api/products", "GET /api/products/{id}",
                "POST /api/orders", "POST /api/orders/{uuid}/items",
                "POST /api/orders/{uuid}/checkout", "GET /api/orders/{uuid}");
        assertThat(sequences.get(1).steps()).containsExactly("GET /api/products");
        assertThat(sequences.get(2).steps()).containsExactly(
                "GET /api/products", "GET /api/products/{id}");
        assertThat(sequences).allSatisfy(sequence -> {
            assertThat(sequence.count()).isEqualTo(2);
            assertThat(sequence.weight()).isCloseTo(2.0 / 6, within(1e-9));
        });
    }

    @Test
    void keepsOnlyTopN() {
        List<Session> sessions = sessionsOfReferenceSet();

        List<ru.rudoy.loadprofile.model.Sequence> sequences = builder.build(sessions, 1);

        assertThat(sequences).hasSize(1);
        assertThat(sequences.get(0).steps()).hasSize(6);
    }

    @Test
    void countsIdenticalChainsTogether() {
        Session first = session("s-1", trace("t-1", "GET", "/api/products"));
        Session second = session("s-2", trace("t-2", "GET", "/api/products"));
        Session third = session("s-3", trace("t-3", "POST", "/api/orders"));

        List<ru.rudoy.loadprofile.model.Sequence> sequences =
                builder.build(List.of(first, second, third), 5);

        assertThat(sequences).extracting(s -> s.steps().get(0))
                .containsExactly("GET /api/products", "POST /api/orders");
        assertThat(sequences.get(0).count()).isEqualTo(2);
        assertThat(sequences.get(0).weight()).isCloseTo(2.0 / 3, within(1e-9));
        assertThat(sequences.get(1).weight()).isCloseTo(1.0 / 3, within(1e-9));
    }

    @Test
    void rejectsNonPositiveTopN() {
        assertThatThrownBy(() -> builder.build(List.of(), 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static List<Session> sessionsOfReferenceSet() {
        List<Trace> traces = new TraceAssembler().assemble(new OtlpJsonReader().readFile(REFERENCE));
        return new SessionByIdStrategy().group(traces);
    }

    private static Session session(String sessionId, Trace... traces) {
        return new Session(sessionId, List.of(traces));
    }

    private static Trace trace(String traceId, String method, String path) {
        Span root = new Span(traceId, traceId + "-root", null, SpanKind.SERVER,
                "order-service", method + " " + path,
                Instant.parse("2026-10-16T10:00:00Z"), Instant.parse("2026-10-16T10:00:00.100Z"),
                Map.of("http.request.method", method, "url.path", path,
                        "session.id", "fixed"));
        return new Trace(traceId, List.of(root));
    }
}
