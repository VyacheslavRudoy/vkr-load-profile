package ru.rudoy.loadprofile.ingest;

import java.util.List;

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
}
