package ru.rudoy.loadprofile.generator;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

class PlanFilesTest {

    @TempDir
    private Path directory;

    @Test
    void writesPlanAndFeederFiles() throws Exception {
        LoadShape load = LoadShape.from(
                new ArrivalRate(6.0, List.of()),
                new TimeWindow(Instant.parse("2026-10-27T10:00:00Z"), Instant.parse("2026-10-27T10:01:00Z")),
                new ThinkTimeStats(2000, 2800, 1000, 3000));
        JmeterPlan plan = new JmeterPlan("localhost", 8080, load,
                List.of(new JmeterPlan.Feeder("category", List.of("books", "sport"))),
                List.of(new JmeterPlan.Sequence("Быстрый просмотр", 1.0,
                        List.of(new JmeterPlan.HttpRequest("GET", "/api/products",
                                List.of(new JmeterPlan.HttpRequest.Query("category", "${category}")),
                                List.of())))));

        List<Path> written = new PlanFiles().write(plan, directory);

        assertThat(written).hasSize(2);
        assertThat(Files.readString(directory.resolve("test-plan.jmx")))
                .startsWith("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
                .contains("<stringProp name=\"HTTPSampler.domain\">localhost</stringProp>");
        assertThat(Files.readString(directory.resolve("category.csv"))).isEqualTo("books\nsport\n");
    }
}
