package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class TimeWindowTest {

    @Test
    void rejectsReversedWindow() {
        assertThatThrownBy(() -> new TimeWindow(
                Instant.parse("2026-09-28T11:00:00Z"), Instant.parse("2026-09-28T10:00:00Z")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
    }

    @Test
    void rejectsEmptyWindow() {
        Instant moment = Instant.parse("2026-09-28T10:00:00Z");

        assertThatThrownBy(() -> new TimeWindow(moment, moment))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("after");
    }
}
