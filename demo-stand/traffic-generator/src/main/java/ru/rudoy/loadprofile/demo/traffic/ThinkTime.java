package ru.rudoy.loadprofile.demo.traffic;

import java.util.Random;

/**
 * Время обдумывания: пауза пользователя между запросами.
 * <p>
 * Распределение равномерное на отрезке [minMs, maxMs]. У равномерного
 * распределения медиана и 90-й перцентиль вычисляются точно, поэтому
 * эталонные статистики известны без измерений.
 */
public record ThinkTime(long minMs, long maxMs) {

    public ThinkTime {
        if (minMs < 0 || maxMs < minMs) {
            throw new IllegalArgumentException(
                    "think time bounds must satisfy 0 <= min <= max, got [" + minMs + ", " + maxMs + "]");
        }
    }

    /** Один отсчёт времени обдумывания. */
    public long sampleMs(Random random) {
        return minMs + random.nextLong(maxMs - minMs + 1);
    }

    public long medianMs() {
        return (minMs + maxMs) / 2;
    }

    public long p90Ms() {
        return minMs + Math.round((maxMs - minMs) * 0.9);
    }
}
