package ru.rudoy.loadprofile.builder;

import java.time.Duration;
import java.util.List;
import ru.rudoy.loadprofile.model.ThinkTimeStats;
import ru.rudoy.loadprofile.session.Session;

/**
 * Статистики пауз пользователя по всем сессиям. Перцентили считаются
 * ближайшим рангом: берётся отсчёт с номером ceil(доля × n); медиана при
 * чётном числе отсчётов — среднее двух средних. Отсчёты хранятся
 * в наносекундах и округляются до миллисекунд один раз, при выдаче.
 */
public final class ThinkTimeStatsBuilder {

    public ThinkTimeStats build(List<Session> sessions) {
        List<Long> pauses = sessions.stream()
                .flatMap(session -> session.thinkTimes().stream())
                .map(Duration::toNanos)
                .sorted()
                .toList();
        if (pauses.isEmpty()) {
            throw new IllegalArgumentException("no think times in the given sessions");
        }
        return new ThinkTimeStats(millis(median(pauses)), millis(percentile(pauses, 0.9)),
                millis(pauses.get(0)), millis(pauses.get(pauses.size() - 1)));
    }

    private static long median(List<Long> sorted) {
        int n = sorted.size();
        if (n % 2 == 1) {
            return sorted.get(n / 2);
        }
        return Math.round((sorted.get(n / 2 - 1) + sorted.get(n / 2)) / 2.0);
    }

    private static long percentile(List<Long> sorted, double share) {
        return sorted.get((int) Math.ceil(share * sorted.size()) - 1);
    }

    private static long millis(long nanos) {
        return Math.round(nanos / 1_000_000.0);
    }
}
