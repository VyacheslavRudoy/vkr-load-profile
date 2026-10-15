package ru.rudoy.loadprofile.builder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.IntervalRate;
import ru.rudoy.loadprofile.session.Session;

/**
 * Интенсивность прихода сессий: среднее число в минуту по окну и по интервалам.
 * Интервалы нарезаются от начала окна, последний короче остальных, если окно
 * не делится нацело. Приход сессии — момент начала её первой трассы; сессия
 * вне окна — ошибка с её именем.
 */
public final class ArrivalRateBuilder {

    public ArrivalRate build(List<Session> sessions, TimeWindow window, Duration bucket) {
        if (bucket == null || bucket.isZero() || bucket.isNegative()) {
            throw new IllegalArgumentException("bucket must be positive");
        }
        long windowNanos = Duration.between(window.from(), window.to()).toNanos();
        long bucketNanos = bucket.toNanos();
        int bucketCount = (int) ((windowNanos + bucketNanos - 1) / bucketNanos);
        int[] counts = new int[bucketCount];
        for (Session session : sessions) {
            Trace first = session.traces().get(0);
            Instant arrival = first.rootSpan().orElseThrow(() ->
                    new IllegalArgumentException("trace " + first.traceId() + " has no root span")).startTime();
            long offset = Duration.between(window.from(), arrival).toNanos();
            if (offset < 0 || offset >= windowNanos) {
                throw new IllegalArgumentException(
                        "session " + session.sessionId() + " arrives outside the window");
            }
            counts[(int) (offset / bucketNanos)]++;
        }
        List<IntervalRate> intervals = new ArrayList<>();
        Instant start = window.from();
        for (int i = 0; i < bucketCount; i++) {
            Instant end = i == bucketCount - 1 ? window.to() : start.plus(bucket);
            double minutes = Duration.between(start, end).toNanos() / 60_000_000_000.0;
            intervals.add(new IntervalRate(start, end, counts[i] / minutes));
            start = end;
        }
        return new ArrivalRate(sessions.size() / (windowNanos / 60_000_000_000.0), intervals);
    }
}
