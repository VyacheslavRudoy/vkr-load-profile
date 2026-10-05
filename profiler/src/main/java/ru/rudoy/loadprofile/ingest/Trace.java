package ru.rudoy.loadprofile.ingest;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Трасса: все спаны одного сквозного запроса, прошедшего через сервисы.
 * <p>
 * Трасса состоит хотя бы из одного спана, и каждый спан обязан принадлежать
 * ей по идентификатору — это проверяется при создании, поэтому чужой спан
 * внутрь попасть не может.
 */
public record Trace(String traceId, List<Span> spans) {

    public Trace {
        if (traceId == null || traceId.isBlank()) {
            throw new IllegalArgumentException("traceId must not be blank");
        }
        if (spans == null || spans.isEmpty()) {
            throw new IllegalArgumentException("a trace consists of at least one span");
        }
        for (Span span : spans) {
            if (!traceId.equals(span.traceId())) {
                throw new IllegalArgumentException("span " + span.spanId() + " belongs to trace "
                        + span.traceId() + ", not " + traceId);
            }
        }
        spans = List.copyOf(spans);
    }

    /**
     * Корневой спан: без родителя или с родителем вне трассы — при частичной
     * выгрузке родитель мог не попасть в набор. Из нескольких кандидатов
     * берётся начавшийся раньше; если все спаны родительски связаны внутри
     * трассы, корня нет.
     */
    public Optional<Span> rootSpan() {
        Set<String> ids = spans.stream().map(Span::spanId).collect(Collectors.toSet());
        return spans.stream()
                .filter(span -> span.parentSpanId() == null || !ids.contains(span.parentSpanId()))
                .min(Comparator.comparing(Span::startTime));
    }
}
