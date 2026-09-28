package ru.rudoy.loadprofile.ingest;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

/**
 * Спан распределённой трассировки в том виде, в каком он нужен остальным
 * частям профилировщика.
 * <p>
 * Имя сервиса вынесено из атрибутов ресурса OTLP в отдельное поле: трасса
 * собирается из спанов разных сервисов, и каждый спан помнит, какой сервис
 * его создал. У корневого спана {@code parentSpanId} равен {@code null}.
 * Идентификаторы во входных данных — шестнадцатеричные строки OTLP, но формат
 * здесь не проверяется: для группировки достаточно непустого значения.
 * <p>
 * Значения атрибутов — строки, логические и числа ({@link Long}, {@link Double});
 * других типов во входных данных нет. Карта атрибутов заменяется
 * на неизменяемую.
 */
public record Span(
        String traceId,
        String spanId,
        String parentSpanId,
        SpanKind kind,
        String serviceName,
        String name,
        Instant startTime,
        Instant endTime,
        Map<String, Object> attributes) {

    public Span {
        requireNonBlank(traceId, "traceId");
        requireNonBlank(spanId, "spanId");
        requireNonBlank(name, "name");
        requireNonBlank(serviceName, "serviceName");
        if (kind == null) {
            throw new IllegalArgumentException("kind must not be null");
        }
        if (startTime == null || endTime == null) {
            throw new IllegalArgumentException("startTime and endTime must not be null");
        }
        if (endTime.isBefore(startTime)) {
            throw new IllegalArgumentException("endTime must not be before startTime");
        }
        attributes = immutableAttributes(attributes);
    }

    /** Длительность спана: от начала к концу. */
    public Duration duration() {
        return Duration.between(startTime, endTime);
    }

    private static void requireNonBlank(String value, String name) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
    }

    private static Map<String, Object> immutableAttributes(Map<String, Object> attributes) {
        if (attributes == null) {
            return Map.of();
        }
        for (Map.Entry<String, Object> entry : attributes.entrySet()) {
            if (!isSupportedValueType(entry.getValue())) {
                throw new IllegalArgumentException("attribute '" + entry.getKey()
                        + "' has an unsupported value type: "
                        + entry.getValue().getClass().getSimpleName());
            }
        }
        return Map.copyOf(attributes);
    }

    private static boolean isSupportedValueType(Object value) {
        return value instanceof String || value instanceof Boolean
                || value instanceof Long || value instanceof Double;
    }
}
