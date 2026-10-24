package ru.rudoy.loadprofile.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

/** Генератор проверяется сравнением с эталонными файлами. */
class JmeterPlanWriterTest {

    private final JmeterPlanWriter writer = new JmeterPlanWriter();

    @Test
    void minimalPlanMatchesGoldenFile() throws Exception {
        assertMatchesGolden(minimalPlan(), "minimal-plan.jmx");
    }

    @Test
    void weightedSequencesMatchGoldenFile() throws Exception {
        assertMatchesGolden(sequencesPlan(), "sequences-plan.jmx");
    }

    @Test
    void rejectsSequenceWithoutSteps() {
        assertThatThrownBy(() -> new JmeterPlan.Sequence("Пустая", 1.0, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JmeterPlan.Sequence("Слишком тяжёлая", 1.5,
                List.of(new JmeterPlan.HttpRequest("GET", "/api/products"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private void assertMatchesGolden(JmeterPlan plan, String goldenName) throws Exception {
        String generated = writer.write(plan);
        String golden = Files.readString(
                Path.of("src/test/resources/generator/" + goldenName), StandardCharsets.UTF_8);
        // сравнение без учёта концов строк: рабочая копия на Windows может иметь CRLF
        assertThat(generated.replace("\r\n", "\n"))
                .isEqualTo(golden.replace("\r\n", "\n"));
        DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(generated.getBytes(StandardCharsets.UTF_8)));
    }

    private static LoadShape load() {
        return LoadShape.from(
                new ArrivalRate(6.0, List.of()),
                new TimeWindow(Instant.parse("2026-10-03T23:40:54Z"), Instant.parse("2026-10-03T23:41:15Z")),
                new ThinkTimeStats(2180, 2858, 1160, 2953));
    }

    private static JmeterPlan minimalPlan() {
        return new JmeterPlan("localhost", 8081, load(), List.of(
                new JmeterPlan.Sequence("Быстрый просмотр", 1.0,
                        List.of(new JmeterPlan.HttpRequest("GET", "/api/products")))));
    }

    private static JmeterPlan sequencesPlan() {
        return new JmeterPlan("localhost", 8081, load(), List.of(
                new JmeterPlan.Sequence("Полная покупка", 1.0 / 3, List.of(
                        new JmeterPlan.HttpRequest("GET", "/api/products"),
                        new JmeterPlan.HttpRequest("POST", "/api/orders"),
                        new JmeterPlan.HttpRequest("POST", "/api/orders/{uuid}/checkout"))),
                new JmeterPlan.Sequence("Просмотр с товаром", 1.0 / 3, List.of(
                        new JmeterPlan.HttpRequest("GET", "/api/products"),
                        new JmeterPlan.HttpRequest("GET", "/api/products/{id}"))),
                new JmeterPlan.Sequence("Быстрый просмотр", 1.0 / 3, List.of(
                        new JmeterPlan.HttpRequest("GET", "/api/products")))));
    }
}
