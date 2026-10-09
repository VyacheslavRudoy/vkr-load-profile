package ru.rudoy.loadprofile.session;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;

/**
 * Сессия: трассы одного пользователя. Трассы упорядочены по моменту начала
 * корневого спана — в этом порядке между ними измеряются паузы пользователя.
 */
public record Session(String sessionId, List<Trace> traces) {

    public Session {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("sessionId must not be blank");
        }
        if (traces == null || traces.isEmpty()) {
            throw new IllegalArgumentException("a session consists of at least one trace");
        }
        traces = traces.stream().sorted(Comparator.comparing(Session::startOf)).toList();
    }

    /**
     * Паузы пользователя: интервалы от конца корневого спана одной трассы
     * до начала следующей. Перекрывающиеся запросы паузой не считаются
     * и дают ноль.
     */
    public List<Duration> thinkTimes() {
        List<Duration> pauses = new ArrayList<>();
        for (int i = 1; i < traces.size(); i++) {
            Span previous = root(traces.get(i - 1));
            Span next = root(traces.get(i));
            Duration pause = Duration.between(previous.endTime(), next.startTime());
            pauses.add(pause.isNegative() ? Duration.ZERO : pause);
        }
        return List.copyOf(pauses);
    }

    private static Instant startOf(Trace trace) {
        return root(trace).startTime();
    }

    private static Span root(Trace trace) {
        return trace.rootSpan().orElseThrow(() ->
                new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
    }
}
