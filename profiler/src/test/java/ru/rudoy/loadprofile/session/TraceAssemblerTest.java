package ru.rudoy.loadprofile.session;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.SpanKind;
import ru.rudoy.loadprofile.ingest.Trace;

class TraceAssemblerTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final TraceAssembler assembler = new TraceAssembler();

    @Test
    void groupsReferenceSetIntoEighteenTraces() {
        List<Span> spans = new OtlpJsonReader().readFile(REFERENCE);

        List<Trace> traces = assembler.assemble(spans);

        assertThat(traces).hasSize(18);
        assertThat(traces.stream().mapToInt(trace -> trace.spans().size()).sum()).isEqualTo(22);
        assertThat(traces).allSatisfy(trace -> {
            Optional<Span> root = trace.rootSpan();
            assertThat(root).isPresent();
            root.ifPresent(span -> {
                assertThat(span.parentSpanId()).isNull();
                assertThat(span.kind()).isEqualTo(SpanKind.SERVER);
            });
        });
    }

    @Test
    void keepsOrderOfFirstAppearance() {
        Span firstOfA = span("t-a", "a1", null);
        Span firstOfB = span("t-b", "b1", null);
        Span secondOfA = span("t-a", "a2", "a1");

        List<Trace> traces = assembler.assemble(List.of(firstOfA, firstOfB, secondOfA));

        assertThat(traces).extracting(Trace::traceId).containsExactly("t-a", "t-b");
        assertThat(traces.get(0).spans()).containsExactly(firstOfA, secondOfA);
    }

    @Test
    void emptyInputGivesNoTraces() {
        assertThat(assembler.assemble(List.of())).isEmpty();
    }

    private static Span span(String traceId, String spanId, String parentSpanId) {
        return new Span(traceId, spanId, parentSpanId, SpanKind.SERVER, "order-service",
                "GET /", Instant.parse("2026-10-04T10:00:00Z"),
                Instant.parse("2026-10-04T10:00:00.100Z"), null);
    }
}
