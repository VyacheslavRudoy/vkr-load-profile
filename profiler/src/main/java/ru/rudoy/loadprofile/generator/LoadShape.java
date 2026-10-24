package ru.rudoy.loadprofile.generator;

import java.time.Duration;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

/**
 * Параметры нагрузки плана теста, выведенные из профиля.
 * <p>
 * Потоки: один поток проживает одну сессию, число потоков — наблюдавшееся
 * число сессий в окне (интенсивность × длина окна), а период разгона равен
 * длине окна. Разгон размазывает старты потоков равномерно, и темп прихода
 * сессий совпадает с наблюдавшимся.
 * <p>
 * Паузы: постоянная задержка таймера равна медиане пауз, отклонение —
 * {@code (p90 − медиана) / 1.2816}: у нормального распределения
 * p90 = медиана + 1.2816σ, так что сигма восстанавливается из двух
 * статистик. Правило принято для гауссова таймера JMeter, где задержка —
 * нормальная случайная величина.
 */
public record LoadShape(int threads, long rampSeconds, Timer timer) {

    public LoadShape {
        if (threads < 1) {
            throw new IllegalArgumentException("threads must be positive: " + threads);
        }
        if (rampSeconds < 1) {
            throw new IllegalArgumentException("rampSeconds must be positive: " + rampSeconds);
        }
        if (timer == null) {
            throw new IllegalArgumentException("timer must not be null");
        }
    }

    /** Гауссов таймер: постоянная задержка и отклонение, миллисекунды. */
    public record Timer(long constantDelayMs, double deviationMs) {

        public Timer {
            if (constantDelayMs < 0) {
                throw new IllegalArgumentException("delay must not be negative: " + constantDelayMs);
            }
            if (deviationMs < 0) {
                throw new IllegalArgumentException("deviation must not be negative: " + deviationMs);
            }
        }
    }

    public static LoadShape from(ArrivalRate rate, TimeWindow window, ThinkTimeStats thinkTime) {
        // длина окна в секундах с долями: обрезание до целых искажает число
        // сессий коротких окон (20.8 с против 21 с дают 6 потоков вместо 7)
        double windowSeconds = Duration.between(window.from(), window.to()).toNanos() / 1e9;
        int threads = (int) Math.ceil(rate.sessionsPerMinute() * windowSeconds / 60.0);
        double deviation = (thinkTime.p90Ms() - thinkTime.medianMs()) / 1.2816;
        return new LoadShape(Math.max(threads, 1), Math.max(Math.round(windowSeconds), 1),
                new Timer(thinkTime.medianMs(), Math.max(deviation, 0)));
    }
}
