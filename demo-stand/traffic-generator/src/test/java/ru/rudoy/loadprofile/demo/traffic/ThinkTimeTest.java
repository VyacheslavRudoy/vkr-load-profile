package ru.rudoy.loadprofile.demo.traffic;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Random;
import org.junit.jupiter.api.Test;

class ThinkTimeTest {

    @Test
    void sampleStaysWithinBounds() {
        ThinkTime thinkTime = new ThinkTime(1_000, 3_000);
        Random random = new Random(42);

        for (int i = 0; i < 10_000; i++) {
            assertThat(thinkTime.sampleMs(random)).isBetween(1_000L, 3_000L);
        }
    }

    @Test
    void zeroLengthPauseIsAllowed() throws InterruptedException {
        ThinkTime thinkTime = new ThinkTime(0, 0);
        assertThat(thinkTime.sampleMs(new Random(1))).isZero();
    }

    @Test
    void statisticsOfUniformDistributionAreExact() {
        ThinkTime thinkTime = new ThinkTime(1_000, 3_000);

        assertThat(thinkTime.medianMs()).isEqualTo(2_000);
        assertThat(thinkTime.p90Ms()).isEqualTo(2_800);
    }

    @Test
    void rejectsInvertedBounds() {
        assertThatThrownBy(() -> new ThinkTime(3_000, 1_000))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
