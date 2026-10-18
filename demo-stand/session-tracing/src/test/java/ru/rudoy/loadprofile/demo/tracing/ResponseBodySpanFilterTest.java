package ru.rudoy.loadprofile.demo.tracing;

import static org.assertj.core.api.Assertions.assertThat;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.sdk.testing.exporter.InMemorySpanExporter;
import io.opentelemetry.sdk.trace.SdkTracerProvider;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.export.SimpleSpanProcessor;
import jakarta.servlet.FilterChain;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class ResponseBodySpanFilterTest {

    private static final String BODY = "{\"id\":\"5e2aaee4-ead4-427e-93f0-ced541654dd4\"}";

    private final InMemorySpanExporter exporter = InMemorySpanExporter.create();
    private final SdkTracerProvider provider = SdkTracerProvider.builder()
            .addSpanProcessor(SimpleSpanProcessor.create(exporter))
            .build();
    private final Tracer tracer = provider.get("test");
    private final ResponseBodySpanFilter filter = new ResponseBodySpanFilter();

    @AfterEach
    void shutdown() {
        provider.close();
    }

    @Test
    void copiesJsonBodyToCurrentSpanAndPassesItThrough() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Span span = tracer.spanBuilder("POST /api/orders").startSpan();
        try (var ignored = span.makeCurrent()) {
            filter.doFilter(new MockHttpServletRequest(), response, jsonChain(BODY));
        } finally {
            span.end();
        }

        SpanData recorded = exporter.getFinishedSpanItems().get(0);
        assertThat(recorded.getAttributes().get(AttributeKey.stringKey(ResponseBodySpanFilter.ATTRIBUTE)))
                .isEqualTo(BODY);
        assertThat(response.getContentAsString()).isEqualTo(BODY);
    }

    @Test
    void skipsNonJsonResponses() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();
        Span span = tracer.spanBuilder("GET /api/products").startSpan();
        try (var ignored = span.makeCurrent()) {
            filter.doFilter(new MockHttpServletRequest(), response,
                    (request, resp) -> resp.getWriter().write("plain text"));
        } finally {
            span.end();
        }

        SpanData recorded = exporter.getFinishedSpanItems().get(0);
        assertThat(recorded.getAttributes().get(AttributeKey.stringKey(ResponseBodySpanFilter.ATTRIBUTE)))
                .isNull();
        assertThat(response.getContentAsString()).isEqualTo("plain text");
    }

    @Test
    void skipsBodiesOverTheLimit() throws Exception {
        String large = "x".repeat(17 * 1024);
        MockHttpServletResponse response = new MockHttpServletResponse();
        Span span = tracer.spanBuilder("GET /api/products").startSpan();
        try (var ignored = span.makeCurrent()) {
            filter.doFilter(new MockHttpServletRequest(), response, jsonChain(large));
        } finally {
            span.end();
        }

        SpanData recorded = exporter.getFinishedSpanItems().get(0);
        assertThat(recorded.getAttributes().get(AttributeKey.stringKey(ResponseBodySpanFilter.ATTRIBUTE)))
                .isNull();
        assertThat(response.getContentAsString()).isEqualTo(large);
    }

    private static FilterChain jsonChain(String body) {
        return (request, response) -> {
            response.setContentType("application/json");
            try {
                response.getWriter().write(body);
            } catch (IOException e) {
                throw new IllegalStateException(e);
            }
        };
    }
}
