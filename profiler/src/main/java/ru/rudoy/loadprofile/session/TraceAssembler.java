package ru.rudoy.loadprofile.session;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;

/** Собирает спаны в трассы по traceId, сохраняя порядок появления. */
public final class TraceAssembler {

    public List<Trace> assemble(Collection<Span> spans) {
        Map<String, List<Span>> byTrace = new LinkedHashMap<>();
        for (Span span : spans) {
            byTrace.computeIfAbsent(span.traceId(), id -> new ArrayList<>()).add(span);
        }
        List<Trace> traces = new ArrayList<>();
        for (Map.Entry<String, List<Span>> entry : byTrace.entrySet()) {
            traces.add(new Trace(entry.getKey(), entry.getValue()));
        }
        return traces;
    }
}
