package ru.rudoy.loadprofile.model;

import java.time.Instant;

/** Интенсивность прихода сессий на интервале окна, сессий в минуту. */
public record IntervalRate(Instant from, Instant to, double value) {

    public IntervalRate {
        if (from == null || to == null || !to.isAfter(from)) {
            throw new IllegalArgumentException("interval end must be after its start");
        }
        if (value < 0) {
            throw new IllegalArgumentException("rate value must not be negative: " + value);
        }
    }
}
