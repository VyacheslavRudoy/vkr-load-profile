package ru.rudoy.loadprofile.session;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;

/**
 * Резервная стратегия: группирует трассы в сессии по идентификатору клиента
 * из атрибута {@code enduser.id} корневого спана. Пауза от конца предыдущей
 * трассы до начала следующей дольше таймаута бездействия разрывает сессию —
 * та же пауза, что у пользователя в профиле нагрузки. Идентификаторы сессий
 * синтетические: значение атрибута и порядковый номер, например {@code 42#2}.
 */
public final class SessionByClientStrategy implements SessionStrategy {

    private final Duration inactivityTimeout;

    public SessionByClientStrategy(Duration inactivityTimeout) {
        if (inactivityTimeout == null || inactivityTimeout.isZero() || inactivityTimeout.isNegative()) {
            throw new IllegalArgumentException("inactivity timeout must be positive");
        }
        this.inactivityTimeout = inactivityTimeout;
    }

    @Override
    public List<Session> group(List<Trace> traces) {
        Map<String, List<Trace>> byClient = new LinkedHashMap<>();
        for (Trace trace : traces) {
            Span root = trace.rootSpan().orElseThrow(() ->
                    new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
            Object client = root.attributes().get("enduser.id");
            if (!(client instanceof String value) || value.isBlank()) {
                throw new IllegalArgumentException(
                        "root span of trace " + trace.traceId() + " has no enduser.id attribute");
            }
            byClient.computeIfAbsent(value, id -> new ArrayList<>()).add(trace);
        }
        List<Session> sessions = new ArrayList<>();
        for (Map.Entry<String, List<Trace>> entry : byClient.entrySet()) {
            splitByTimeout(entry.getKey(), entry.getValue(), sessions);
        }
        return sessions;
    }

    private void splitByTimeout(String clientId, List<Trace> traces, List<Session> sessions) {
        List<Trace> ordered = traces.stream()
                .sorted(Comparator.comparing(trace -> trace.rootSpan().orElseThrow().startTime()))
                .toList();
        int sequence = 1;
        List<Trace> current = new ArrayList<>();
        for (Trace trace : ordered) {
            if (!current.isEmpty() && isIdleAfter(current.get(current.size() - 1), trace)) {
                sessions.add(new Session(clientId + "#" + sequence++, current));
                current = new ArrayList<>();
            }
            current.add(trace);
        }
        sessions.add(new Session(clientId + "#" + sequence, current));
    }

    private boolean isIdleAfter(Trace previous, Trace next) {
        Span previousRoot = previous.rootSpan().orElseThrow();
        Span nextRoot = next.rootSpan().orElseThrow();
        return Duration.between(previousRoot.endTime(), nextRoot.startTime()).compareTo(inactivityTimeout) > 0;
    }
}
