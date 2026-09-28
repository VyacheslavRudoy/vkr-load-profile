package ru.rudoy.loadprofile.ingest;

/**
 * Вид спана по классификации OpenTelemetry.
 * <p>
 * Для профиля нагрузки важны {@code SERVER} — входящий запрос в сервис —
 * и {@code CLIENT} — исходящий вызов между сервисами. Остальные виды входят
 * для полноты: они встречаются во входных данных, но профилировщик их
 * не различает.
 */
public enum SpanKind {
    UNSPECIFIED,
    INTERNAL,
    SERVER,
    CLIENT,
    PRODUCER,
    CONSUMER
}
