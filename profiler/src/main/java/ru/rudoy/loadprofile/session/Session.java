package ru.rudoy.loadprofile.session;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
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

    private static Instant startOf(Trace trace) {
        return trace.rootSpan().orElseThrow(() ->
                new IllegalArgumentException("trace " + trace.traceId() + " has no root span")).startTime();
    }
}
