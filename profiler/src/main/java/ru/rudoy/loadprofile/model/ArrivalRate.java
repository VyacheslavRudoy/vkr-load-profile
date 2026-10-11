package ru.rudoy.loadprofile.model;

import java.util.List;

/** Интенсивность нагрузки: среднее число сессий в минуту и по интервалам окна. */
public record ArrivalRate(double sessionsPerMinute, List<IntervalRate> byInterval) {

    public ArrivalRate {
        if (sessionsPerMinute < 0) {
            throw new IllegalArgumentException("sessionsPerMinute must not be negative");
        }
        byInterval = byInterval == null ? List.of() : List.copyOf(byInterval);
    }
}
