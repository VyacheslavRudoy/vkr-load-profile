package ru.rudoy.loadprofile.builder;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.PathTemplates;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.Operation;

/**
 * Состав операций с долями. Операция — пара «HTTP-метод + шаблон пути»;
 * считается по корневым спанам, по одной операции на трассу: внутренние
 * вызовы между сервисами — часть того же запроса пользователя.
 */
public final class OperationsBuilder {

    public List<Operation> build(List<Trace> traces) {
        Map<OperationKey, Long> counts = new LinkedHashMap<>();
        for (Trace trace : traces) {
            Span root = trace.rootSpan().orElseThrow(() ->
                    new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
            String method = attribute(root, trace, "http.request.method");
            String path = attribute(root, trace, "url.path");
            counts.merge(new OperationKey(method, PathTemplates.template(path)), 1L, Long::sum);
        }
        List<Operation> operations = new ArrayList<>();
        for (Map.Entry<OperationKey, Long> entry : counts.entrySet()) {
            long count = entry.getValue();
            operations.add(new Operation(entry.getKey().method(), entry.getKey().pathTemplate(),
                    count, (double) count / traces.size()));
        }
        return operations;
    }

    private static String attribute(Span root, Trace trace, String name) {
        Object value = root.attributes().get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "root span of trace " + trace.traceId() + " has no " + name + " attribute");
        }
        return text;
    }

    private record OperationKey(String method, String pathTemplate) {
    }
}
