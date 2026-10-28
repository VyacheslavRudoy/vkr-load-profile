package ru.rudoy.loadprofile.compare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.Operation;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

class ProfileComparatorTest {

    private final ProfileComparator comparator = new ProfileComparator();

    @Test
    void identicalProfilesHaveNoDifferences() {
        LoadProfile profile = profile("эталон");

        var comparison = comparator.compare(profile, profile);

        assertThat(comparison.operations())
                .allSatisfy(row -> assertThat(row.difference()).isZero());
        assertThat(comparison.sessionsPerMinuteDifference()).isZero();
        assertThat(comparison.thinkTime().medianMs()).isZero();
        assertThat(comparison.thinkTime().p90Ms()).isZero();
    }

    @Test
    void countsOperationSharesWithMissingAsZero() {
        LoadProfile first = new LoadProfile(null, "A", Instant.parse("2026-10-28T10:00:00Z"),
                window(),
                List.of(new Operation("GET", "/api/products", 6, 0.6),
                        new Operation("POST", "/api/orders", 4, 0.4)),
                List.of(), new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2000, 2800, 1000, 3000), List.of());
        LoadProfile second = new LoadProfile(null, "B", Instant.parse("2026-10-28T10:00:00Z"),
                window(),
                List.of(new Operation("GET", "/api/products", 8, 0.8),
                        new Operation("GET", "/api/orders/{uuid}", 2, 0.2)),
                List.of(), new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2200, 3000, 1000, 3200), List.of());

        var comparison = comparator.compare(first, second);

        assertThat(comparison.operations()).extracting(ProfileComparison.OperationDifference::operation)
                .first().isEqualTo("POST /api/orders");
        assertThat(comparison.operations()).extracting(ProfileComparison.OperationDifference::operation)
                .containsExactlyInAnyOrder("POST /api/orders", "GET /api/orders/{uuid}", "GET /api/products");
        assertThat(comparison.operations().stream()
                .filter(row -> row.operation().equals("POST /api/orders")).findFirst().orElseThrow())
                .satisfies(row -> {
                    assertThat(row.firstShare()).isEqualTo(0.4);
                    assertThat(row.secondShare()).isZero();
                    assertThat(row.difference()).isCloseTo(0.4, within(1e-9));
                });
        assertThat(comparison.sessionsPerMinuteDifference()).isZero();
        assertThat(comparison.thinkTime().medianMs()).isEqualTo(-200);
        assertThat(comparison.thinkTime().p90Ms()).isEqualTo(-200);
    }

    private static TimeWindow window() {
        return new TimeWindow(Instant.parse("2026-10-28T10:00:00Z"), Instant.parse("2026-10-28T11:00:00Z"));
    }

    private static LoadProfile profile(String name) {
        return new LoadProfile(null, name, Instant.parse("2026-10-28T10:00:00Z"), window(),
                List.of(new Operation("GET", "/api/products", 10, 1.0)),
                List.of(), new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2000, 2800, 1000, 3000), List.of());
    }
}
