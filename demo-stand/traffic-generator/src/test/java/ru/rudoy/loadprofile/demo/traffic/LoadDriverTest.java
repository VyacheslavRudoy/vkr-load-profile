package ru.rudoy.loadprofile.demo.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

class LoadDriverTest {

    @Test
    void runsSessionsUntilDurationEnds() throws Exception {
        try (StandStub stub = new StandStub()) {
            ReferenceProfile profile = new ReferenceProfile(
                    List.of(new ReferenceProfile.WeightedScenario("quick-browse", 1,
                            (stand, random) -> stand.productIds())),
                    new ThinkTime(0, 0));
            LoadDriver driver = new LoadDriver(profile);

            LoadDriver.RunResult result = driver.run(new LoadDriver.RunOptions(
                    stub.baseUrl(), stub.baseUrl(), 600, Duration.ofMillis(300), 42L));

            assertThat(result.started()).isGreaterThanOrEqualTo(2);
            assertThat(result.completed()).isEqualTo(result.started());
            assertThat(result.failed()).isZero();
        }
    }

    @Test
    void rejectsImpossibleRates() {
        assertThatThrownBy(() -> new LoadDriver.RunOptions(
                "http://localhost:1", "http://localhost:1", 0, Duration.ofSeconds(1), 1L))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
