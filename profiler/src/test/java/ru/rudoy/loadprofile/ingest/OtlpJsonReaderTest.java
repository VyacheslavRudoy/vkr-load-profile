package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Проверяет разбор OTLP JSON на фиксированном наборе: покупка из двух сервисов
 * и отдельный просмотр каталога.
 */
class OtlpJsonReaderTest {

    private static final Path SAMPLE = Path.of("src/test/resources/otlp-sample.json");

    private final OtlpJsonReader reader = new OtlpJsonReader();

    private List<Span> spans;

    @BeforeEach
    void parseSample() throws IOException {
        spans = reader.readSpans(Files.readString(SAMPLE));
    }

    @Test
    void readsAllSpansInFileOrder() {
        assertThat(spans).extracting(Span::name).containsExactly(
                "POST /api/orders/{id}/items",
                "GET /api/products/{id}",
                "GET /api/products/{id}",
                "GET /api/products");
    }

    @Test
    void liftsServiceNameFromResourceAttributes() {
        assertThat(spans).extracting(Span::serviceName).containsExactly(
                "order-service", "order-service", "catalog-service", "catalog-service");
    }

    @Test
    void mapsSpanFields() {
        Span root = spans.get(0);

        assertThat(root.traceId()).isEqualTo("5b8efff798038103d269b633813fc60c");
        assertThat(root.spanId()).isEqualTo("eee19b7ec3c1b174");
        assertThat(root.parentSpanId()).isNull();
        assertThat(root.kind()).isEqualTo(SpanKind.SERVER);
        assertThat(root.startTime()).isEqualTo(Instant.parse("2026-09-28T10:00:00.123456789Z"));
        assertThat(root.duration()).isEqualTo(Duration.ofMillis(65));
        assertThat(spans.get(1).kind()).isEqualTo(SpanKind.CLIENT);
        assertThat(spans.get(2).parentSpanId()).isEqualTo("c8510e9d3e47a107");
    }

    @Test
    void intValueBecomesLong() {
        assertThat(spans.get(0).attributes().get("http.response.status_code"))
                .isEqualTo(200L)
                .isInstanceOf(Long.class);
    }

    @Test
    void readFileReadsTheSameSpans() {
        assertThat(reader.readFile(SAMPLE)).isEqualTo(spans);
    }

    @Test
    void parsesBoolAndDoubleValues() {
        List<Span> parsed = reader.readSpans("""
                {"resourceSpans": [{"resource": {"attributes": [
                    {"key": "service.name", "value": {"stringValue": "catalog-service"}}]},
                  "scopeSpans": [{"spans": [{
                    "traceId": "aa", "spanId": "bb", "name": "GET",
                    "startTimeUnixNano": "1000000000", "endTimeUnixNano": "2000000000",
                    "attributes": [
                      {"key": "ok", "value": {"boolValue": true}},
                      {"key": "ratio", "value": {"doubleValue": 0.5}}]}]}]}]}
                """);

        assertThat(parsed.get(0).attributes())
                .containsEntry("ok", true)
                .containsEntry("ratio", 0.5);
    }

    @Test
    void emptyParentSpanIdMeansRoot() {
        List<Span> parsed = reader.readSpans("""
                {"resourceSpans": [{"resource": {"attributes": [
                    {"key": "service.name", "value": {"stringValue": "catalog-service"}}]},
                  "scopeSpans": [{"spans": [{
                    "traceId": "aa", "spanId": "bb", "name": "GET",
                    "parentSpanId": "",
                    "startTimeUnixNano": "1000000000", "endTimeUnixNano": "2000000000",
                    "attributes": []}]}]}]}
                """);

        assertThat(parsed.get(0).parentSpanId()).isNull();
        assertThat(parsed.get(0).kind()).isEqualTo(SpanKind.UNSPECIFIED);
    }

    @Test
    void rejectsResourceWithoutServiceName() {
        assertThatThrownBy(() -> reader.readSpans("""
                {"resourceSpans": [{"resource": {"attributes": [
                    {"key": "telemetry.sdk.language", "value": {"stringValue": "java"}}]},
                  "scopeSpans": [{"spans": [{
                    "traceId": "aa", "spanId": "bb", "name": "GET",
                    "startTimeUnixNano": "1000000000", "endTimeUnixNano": "2000000000",
                    "attributes": []}]}]}]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("service.name");
    }

    @Test
    void rejectsUnsupportedAttributeValue() {
        assertThatThrownBy(() -> reader.readSpans("""
                {"resourceSpans": [{"resource": {"attributes": [
                    {"key": "service.name", "value": {"stringValue": "catalog-service"}}]},
                  "scopeSpans": [{"spans": [{
                    "traceId": "aa", "spanId": "bb", "name": "GET",
                    "startTimeUnixNano": "1000000000", "endTimeUnixNano": "2000000000",
                    "attributes": [
                      {"key": "tags", "value": {"arrayValue": [{"stringValue": "a"}]}}]}]}]}]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tags");
    }

    @Test
    void rejectsUnknownKindCode() {
        assertThatThrownBy(() -> reader.readSpans("""
                {"resourceSpans": [{"resource": {"attributes": [
                    {"key": "service.name", "value": {"stringValue": "catalog-service"}}]},
                  "scopeSpans": [{"spans": [{
                    "traceId": "aa", "spanId": "bb", "name": "GET", "kind": 9,
                    "startTimeUnixNano": "1000000000", "endTimeUnixNano": "2000000000",
                    "attributes": []}]}]}]}
                """))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("kind");
    }

    @Test
    void rejectsMalformedJson() {
        assertThatThrownBy(() -> reader.readSpans("это не JSON"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
