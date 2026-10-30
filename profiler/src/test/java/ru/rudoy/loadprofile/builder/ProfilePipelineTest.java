package ru.rudoy.loadprofile.builder;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.OtlpJsonReader;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.session.SessionStrategyType;

class ProfilePipelineTest {

    private static final Path REFERENCE = Path.of("src/test/resources/reference-traces/stand-traces.json");

    private final ProfilePipeline pipeline = new ProfilePipeline();

    @Test
    void rejectsEmptyInput() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() ->
                        pipeline.build(List.of(), "пустой",
                                ru.rudoy.loadprofile.session.SessionStrategyType.BY_SESSION_ID,
                                5, new ru.rudoy.loadprofile.ingest.TimeWindow(
                                        java.time.Instant.parse("2026-10-28T10:00:00Z"),
                                        java.time.Instant.parse("2026-10-28T11:00:00Z"))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no spans");
    }

    @Test
    void buildsProfileFromTheReferenceSet() {
        List<ru.rudoy.loadprofile.ingest.Span> spans = new OtlpJsonReader().readFile(REFERENCE);
        Instant from = spans.stream().map(s -> s.startTime()).min(Comparator.naturalOrder()).orElseThrow();
        Instant to = spans.stream().map(s -> s.endTime()).max(Comparator.naturalOrder()).orElseThrow();

        LoadProfile profile = pipeline.build(spans, "эталонный прогон",
                SessionStrategyType.BY_SESSION_ID, 10, new TimeWindow(from, to));

        assertThat(profile.name()).isEqualTo("эталонный прогон");
        assertThat(profile.operations()).hasSize(6);
        assertThat(profile.sequences()).hasSize(3);
        assertThat(profile.thinkTime()).isEqualTo(
                new ru.rudoy.loadprofile.model.ThinkTimeStats(2180, 2858, 1160, 2953));
        assertThat(profile.arrivalRate().sessionsPerMinute()).isCloseTo(17.2690445,
                org.assertj.core.data.Offset.offset(1e-6));
    }

    @Test
    void referenceSetWithoutBodiesGivesOnlyFeederParameters() {
        List<ru.rudoy.loadprofile.ingest.Span> spans = new OtlpJsonReader().readFile(REFERENCE);
        Instant from = spans.stream().map(s -> s.startTime()).min(Comparator.naturalOrder()).orElseThrow();
        Instant to = spans.stream().map(s -> s.endTime()).max(Comparator.naturalOrder()).orElseThrow();

        LoadProfile profile = pipeline.build(spans, "эталонный прогон",
                SessionStrategyType.BY_SESSION_ID, 10, new TimeWindow(from, to));

        assertThat(profile.parameters())
                .filteredOn(spec -> spec.source() == ParameterSource.CORRELATED)
                .isEmpty();
        Set<String> feederNames = profile.parameters().stream()
                .map(spec -> spec.name())
                .collect(Collectors.toSet());
        assertThat(feederNames).containsExactlyInAnyOrder(
                "uuid", "id", "productId", "customerId", "quantity", "category");
    }
}
