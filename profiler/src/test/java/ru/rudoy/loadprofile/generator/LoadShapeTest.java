package ru.rudoy.loadprofile.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.IntervalRate;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

class LoadShapeTest {

    @Test
    void derivesThreadsAndRampFromTheWindow() {
        Instant from = Instant.parse("2026-10-03T10:00:00Z");
        Instant to = Instant.parse("2026-10-03T10:01:30Z");

        LoadShape shape = LoadShape.from(
                new ArrivalRate(12.0, List.of(new IntervalRate(from, to, 12.0))),
                new TimeWindow(from, to),
                new ThinkTimeStats(2180, 2858, 1160, 2953));

        assertThat(shape.threads()).isEqualTo(18);
        assertThat(shape.rampSeconds()).isEqualTo(90);
    }

    @Test
    void fractionalWindowsRoundThreadsUp() {
        Instant from = Instant.parse("2026-10-03T10:00:00Z");
        Instant to = Instant.parse("2026-10-03T10:00:20.500Z");

        LoadShape shape = LoadShape.from(
                new ArrivalRate(12.0, List.of()),
                new TimeWindow(from, to),
                new ThinkTimeStats(2180, 2858, 1160, 2953));

        assertThat(shape.threads()).isEqualTo(5);
        assertThat(shape.rampSeconds()).isEqualTo(21);
    }

    @Test
    void timerDelayIsMedianAndDeviationRecoversSigma() {
        Instant from = Instant.parse("2026-10-03T23:40:54Z");
        Instant to = Instant.parse("2026-10-03T23:41:15Z");

        LoadShape.Timer timer = LoadShape.from(
                new ArrivalRate(6.0, List.of()),
                new TimeWindow(from, to),
                new ThinkTimeStats(2180, 2858, 1160, 2953)).timer();

        assertThat(timer.constantDelayMs()).isEqualTo(2180);
        assertThat(timer.deviationMs()).isCloseTo((2858 - 2180) / 1.2816, within(1e-9));
    }

    @Test
    void invertedStatsClampDeviationToZero() {
        Instant from = Instant.parse("2026-10-03T10:00:00Z");
        Instant to = Instant.parse("2026-10-03T10:01:00Z");

        LoadShape.Timer timer = LoadShape.from(
                new ArrivalRate(6.0, List.of()),
                new TimeWindow(from, to),
                new ThinkTimeStats(2000, 2000, 1000, 3000)).timer();

        assertThat(timer.deviationMs()).isZero();
    }
}
