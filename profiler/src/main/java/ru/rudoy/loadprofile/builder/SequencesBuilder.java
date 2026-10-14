package ru.rudoy.loadprofile.builder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.PathTemplates;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.Sequence;
import ru.rudoy.loadprofile.session.Session;

/**
 * Топ-N наблюдавшихся последовательностей операций. Последовательность —
 * цепочка операций одной сессии в порядке выполнения, одинаковые цепочки
 * считаются вместе; вес — доля сессий с такой цепочкой. При равных частотах
 * раньше идёт появившаяся раньше.
 */
public final class SequencesBuilder {

    public List<Sequence> build(List<Session> sessions, int topN) {
        if (topN < 1) {
            throw new IllegalArgumentException("topN must be positive: " + topN);
        }
        Map<List<String>, Long> counts = new LinkedHashMap<>();
        for (Session session : sessions) {
            counts.merge(stepsOf(session), 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<List<String>, Long>comparingByValue().reversed())
                .limit(topN)
                .map(entry -> new Sequence(entry.getKey(), entry.getValue(),
                        (double) entry.getValue() / sessions.size()))
                .toList();
    }

    private static List<String> stepsOf(Session session) {
        return session.traces().stream().map(SequencesBuilder::refOf).toList();
    }

    private static String refOf(Trace trace) {
        Span root = trace.rootSpan().orElseThrow(() ->
                new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
        Object method = root.attributes().get("http.request.method");
        Object path = root.attributes().get("url.path");
        if (!(method instanceof String m) || m.isBlank()
                || !(path instanceof String p) || p.isBlank()) {
            throw new IllegalArgumentException("root span of trace " + trace.traceId()
                    + " lacks http.request.method or url.path");
        }
        return m + " " + PathTemplates.template(p);
    }
}
