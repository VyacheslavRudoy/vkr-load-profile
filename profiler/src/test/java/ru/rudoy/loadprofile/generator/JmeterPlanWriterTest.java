package ru.rudoy.loadprofile.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.Test;

/** Генератор проверяется сравнением с эталонным файлом. */
class JmeterPlanWriterTest {

    private static final Path GOLDEN = Path.of("src/test/resources/generator/minimal-plan.jmx");

    private final JmeterPlanWriter writer = new JmeterPlanWriter();

    @Test
    void minimalPlanMatchesGoldenFile() throws Exception {
        String generated = writer.write(new JmeterPlan("localhost", 8081,
                List.of(new JmeterPlan.HttpRequest("GET", "/api/products"))));

        String golden = Files.readString(GOLDEN, StandardCharsets.UTF_8);
        // сравнение без учёта концов строк: рабочая копия на Windows может иметь CRLF
        assertThat(generated.replace("\r\n", "\n"))
                .isEqualTo(golden.replace("\r\n", "\n"));

        DocumentBuilderFactory.newDefaultInstance()
                .newDocumentBuilder()
                .parse(new ByteArrayInputStream(generated.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    void rejectsPlanWithoutRequests() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        new JmeterPlan("localhost", 8081, List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
