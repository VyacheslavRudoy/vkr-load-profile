package ru.rudoy.loadprofile.demo.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import org.junit.jupiter.api.Test;

class SessionIdSpanProcessorTest {

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();

    private final Tracer tracer = SdkTracerProvider.builder()
            .addSpanProcessor(new SessionIdSpanProcessor())
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build()
            .get("test");

    @Test
    void copiesSessionIdFromBaggage() {
        Context context = Context.root().with(Baggage.builder().put("session.id", "7c1d2a3e").build());

        tracer.spanBuilder("GET /api/products").setParent(context).startSpan().end();

        SpanData span = exporter.getFinishedSpanItems().get(0);
        assertThat(span.getAttributes().get(SessionIdSpanProcessor.SESSION_ID)).isEqualTo("7c1d2a3e");
    }

    @Test
    void leavesSpanUntouchedWithoutBaggage() {
        tracer.spanBuilder("GET /api/products").startSpan().end();

        SpanData span = exporter.getFinishedSpanItems().get(0);
        assertThat(span.getAttributes().get(SessionIdSpanProcessor.SESSION_ID)).isNull();
    }
}
