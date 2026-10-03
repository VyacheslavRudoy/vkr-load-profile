package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

/**
 * Эталонный набор трассировок, снятых с демостенда: генератор активности
 * прогнал шесть сессий с фиксированным зерном по всем сценариям, трассы
 * выгружены из Jaeger за окно прогона, спаны самого Jaeger из файла убраны.
 * Набор — зафиксированный файл, поэтому тесты детерминированы и стенд не нужен;
 * на этом же наборе позже проверяется сессионизация.
 */
class ReferenceTracesTest {

    private static final Path TRACES = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final List<Span> spans = new OtlpJsonReader().readFile(TRACES);

    @Test
    void holdsTheCapturedShape() {
        assertThat(spans).hasSize(22);
        assertThat(spans.stream().map(Span::traceId).distinct()).hasSize(18);
    }

    @Test
    void coversBothServices() {
        assertThat(spans.stream().map(Span::serviceName).distinct())
                .containsExactlyInAnyOrder("order-service", "catalog-service");
    }

    @Test
    void everySpanCarriesSessionId() {
        assertThat(spans).allSatisfy(span ->
                assertThat(span.attributes()).containsKey("session.id"));
    }

    @Test
    void spansBelongToSixSessions() {
        assertThat(spans.stream()
                .map(span -> (String) span.attributes().get("session.id"))
                .distinct())
                .hasSize(6);
    }

    @Test
    void rawPathsReduceToOperationTemplates() {
        Set<String> templates = spans.stream()
                .map(span -> (String) span.attributes().get("url.path"))
                .filter(Objects::nonNull)
                .map(PathTemplates::template)
                .collect(Collectors.toCollection(TreeSet::new));

        assertThat(templates).containsExactly(
                "/api/orders",
                "/api/orders/{uuid}",
                "/api/orders/{uuid}/checkout",
                "/api/orders/{uuid}/items",
                "/api/products",
                "/api/products/{id}");
    }

    @Test
    void hasServerAndClientSpans() {
        assertThat(spans).extracting(Span::kind).contains(SpanKind.SERVER, SpanKind.CLIENT);
    }
}
