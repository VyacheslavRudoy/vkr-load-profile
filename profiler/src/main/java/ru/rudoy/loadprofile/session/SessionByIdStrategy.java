package ru.rudoy.loadprofile.session;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;

/**
 * Группирует трассы в сессии по атрибуту {@code session.id} корневого спана.
 * Трасса без атрибута — ошибка с именем трассы: раз атрибут не проставлен,
 * эта стратегия к таким данным неприменима и нужен другой способ группировки.
 */
public final class SessionByIdStrategy implements SessionStrategy {

    @Override
    public List<Session> group(List<Trace> traces) {
        Map<String, List<Trace>> bySession = new LinkedHashMap<>();
        for (Trace trace : traces) {
            Span root = trace.rootSpan().orElseThrow(() ->
                    new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
            Object sessionId = root.attributes().get("session.id");
            if (!(sessionId instanceof String value) || value.isBlank()) {
                throw new IllegalArgumentException(
                        "root span of trace " + trace.traceId() + " has no session.id attribute");
            }
            bySession.computeIfAbsent(value, id -> new ArrayList<>()).add(trace);
        }
        List<Session> sessions = new ArrayList<>();
        for (Map.Entry<String, List<Trace>> entry : bySession.entrySet()) {
            sessions.add(new Session(entry.getKey(), entry.getValue()));
        }
        return sessions;
    }
}
