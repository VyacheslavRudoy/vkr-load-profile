package ru.rudoy.loadprofile.demo.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.junit.jupiter.api.Test;

class ReferenceProfileTest {

    @Test
    void weightsGiveSharesOfSessions() {
        ReferenceProfile profile = new ReferenceProfile(
                List.of(scenario("a", 30), scenario("b", 70)), new ThinkTime(0, 0));
        Random random = new Random(42);

        Map<String, Long> counts = new HashMap<>();
        for (int i = 0; i < 10_000; i++) {
            counts.merge(profile.chooseScenario(random).name(), 1L, Long::sum);
        }

        assertThat(counts.get("a")).isCloseTo(3_000L, within(300L));
        assertThat(counts.get("b")).isCloseTo(7_000L, within(300L));
    }

    @Test
    void sameSeedChoosesSameScenarios() {
        ReferenceProfile profile = new ReferenceProfile(
                List.of(scenario("a", 1), scenario("b", 1)), new ThinkTime(0, 0));
        Random first = new Random(7);
        Random second = new Random(7);

        for (int i = 0; i < 100; i++) {
            assertThat(profile.chooseScenario(first).name())
                    .isEqualTo(profile.chooseScenario(second).name());
        }
    }

    @Test
    void rejectsZeroWeight() {
        assertThatThrownBy(() -> scenario("a", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsProfileWithoutScenarios() {
        assertThatThrownBy(() -> new ReferenceProfile(List.of(), new ThinkTime(0, 0)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ReferenceProfile.WeightedScenario scenario(String name, int weight) {
        return new ReferenceProfile.WeightedScenario(name, weight, (stand, random) -> { });
    }
}
