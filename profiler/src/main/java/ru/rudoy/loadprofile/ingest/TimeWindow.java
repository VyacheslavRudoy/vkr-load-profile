package ru.rudoy.loadprofile.ingest;

import java.time.Instant;

/**
 * Временное окно, за которое выгружаются трассировки.
 * <p>
 * Окно имеет положительную длину: конец строго позже начала. Нулевое
 * окно запрещено — оно не может содержать ни одной трассы.
 */
public record TimeWindow(Instant from, Instant to) {

    public TimeWindow {
        if (from == null || to == null) {
            throw new IllegalArgumentException("window bounds must not be null");
        }
        if (!to.isAfter(from)) {
            throw new IllegalArgumentException("window end must be after its start: "
                    + from + " .. " + to);
        }
    }
}
