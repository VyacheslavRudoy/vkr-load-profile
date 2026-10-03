package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class JaegerApiReaderTest {

    private static final Path SAMPLE = Path.of("src/test/resources/otlp-sample.json");

    private JaegerStub jaeger;
    private JaegerApiReader reader;

    @BeforeEach
    void startJaeger() throws IOException {
        jaeger = new JaegerStub();
        // Базовый адрес с завершающим слэшем заодно проверяет его обрезку
        reader = new JaegerApiReader(jaeger.baseUrl());
    }

    @AfterEach
    void stopJaeger() {
        jaeger.close();
    }

    @Test
    void asksTheV3ApiForTheTimeWindow() {
        reader.fetch(window());

        assertThat(jaeger.lastRequestUri().getPath()).isEqualTo("/api/v3/traces");
        assertThat(jaeger.lastRequestUri().getQuery())
                .contains("query.startTimeMin=2026-09-28T10:00:00Z")
                .contains("query.startTimeMax=2026-09-28T11:00:00Z");
    }

    @Test
    void parsesSpansFromTheResponse() throws IOException {
        jaeger.respondWith(200, Files.readString(SAMPLE));

        List<Span> spans = reader.fetch(window());

        assertThat(spans).hasSize(4);
        assertThat(spans.get(0).serviceName()).isEqualTo("order-service");
        assertThat(spans.get(2).parentSpanId()).isEqualTo("c8510e9d3e47a107");
    }

    @Test
    void unwrapsTheResultEnvelope() throws IOException {
        jaeger.respondWith(200, "{\"result\":" + Files.readString(SAMPLE) + "}");

        List<Span> spans = reader.fetch(window());

        assertThat(spans).hasSize(4);
    }

    @Test
    void rejectsErrorResponse() {
        jaeger.respondWith(503, "service unavailable");

        assertThatThrownBy(() -> reader.fetch(window()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("503");
    }

    private static TimeWindow window() {
        return new TimeWindow(
                Instant.parse("2026-09-28T10:00:00Z"), Instant.parse("2026-09-28T11:00:00Z"));
    }
}
