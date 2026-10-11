package ru.rudoy.loadprofile.model;

/** Статистики пауз пользователя между трассами, миллисекунды. */
public record ThinkTimeStats(long medianMs, long p90Ms, long minMs, long maxMs) {

    public ThinkTimeStats {
        if (minMs < 0 || minMs > medianMs || medianMs > p90Ms || p90Ms > maxMs) {
            throw new IllegalArgumentException(
                    "think time stats must satisfy 0 <= min <= median <= p90 <= max: "
                            + minMs + ", " + medianMs + ", " + p90Ms + ", " + maxMs);
        }
    }
}
