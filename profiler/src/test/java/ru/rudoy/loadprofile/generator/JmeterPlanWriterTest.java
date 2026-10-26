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
    void parameterizedPlanMatchesGoldenFile() throws Exception {
        assertMatchesGolden(parametersPlan(), "parameters-plan.jmx");
    }

    @Test
    void feederCsvFilesCarryOneValuePerLine() {
        var files = writer.csvFiles(parametersPlan());

        assertThat(files).containsKeys("category.csv", "customerId.csv", "quantity.csv");
        assertThat(files.get("category.csv")).isEqualTo("books\nelectronics\nsport\n");
        assertThat(files.get("customerId.csv")).isEqualTo("5\n8\n");
    }

    @Test
    void rejectsBadFeedersAndExtractions() {
        assertThatThrownBy(() -> new JmeterPlan.Feeder("пустой", List.of()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JmeterPlan.Feeder("с запятой", List.of("a,b")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new JmeterPlan.HttpRequest.Extraction("uuid", "id"))
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
        return new JmeterPlan("localhost", 8081, load(), List.of(), List.of(
                new JmeterPlan.Sequence("Быстрый просмотр", 1.0,
                        List.of(request("GET", "/api/products")))));
    }

    private static JmeterPlan sequencesPlan() {
        return new JmeterPlan("localhost", 8081, load(), List.of(), List.of(
                new JmeterPlan.Sequence("Полная покупка", 1.0 / 3, List.of(
                        request("GET", "/api/products"),
                        request("POST", "/api/orders"),
                        request("POST", "/api/orders/{uuid}/checkout"))),
                new JmeterPlan.Sequence("Просмотр с товаром", 1.0 / 3, List.of(
                        request("GET", "/api/products"),
                        request("GET", "/api/products/{id}"))),
                new JmeterPlan.Sequence("Быстрый просмотр", 1.0 / 3, List.of(
                        request("GET", "/api/products")))));
    }

    private static JmeterPlan parametersPlan() {
        return new JmeterPlan("localhost", 8081, load(),
                List.of(
                        new JmeterPlan.Feeder("category", List.of("books", "electronics", "sport")),
                        new JmeterPlan.Feeder("customerId", List.of("5", "8")),
                        new JmeterPlan.Feeder("quantity", List.of("1", "2", "3"))),
                List.of(new JmeterPlan.Sequence("Полная покупка", 1.0, List.of(
                        new JmeterPlan.HttpRequest("GET", "/api/products",
                                List.of(new JmeterPlan.HttpRequest.Query("category", "${category}")),
                                List.of(new JmeterPlan.HttpRequest.Extraction("productId", "$[0].id"))),
                        new JmeterPlan.HttpRequest("POST", "/api/orders",
                                List.of(new JmeterPlan.HttpRequest.Query("customerId", "${customerId}")),
                                List.of(new JmeterPlan.HttpRequest.Extraction("uuid", "$.id"))),
                        new JmeterPlan.HttpRequest("POST", "/api/orders/${uuid}/items",
                                List.of(
                                        new JmeterPlan.HttpRequest.Query("productId", "${productId}"),
                                        new JmeterPlan.HttpRequest.Query("quantity", "${quantity}")),
                                List.of()),
                        request("POST", "/api/orders/${uuid}/checkout"),
                        request("GET", "/api/orders/${uuid}")))));
    }

    private static JmeterPlan.HttpRequest request(String method, String path) {
        return new JmeterPlan.HttpRequest(method, path, List.of(), List.of());
    }
}
