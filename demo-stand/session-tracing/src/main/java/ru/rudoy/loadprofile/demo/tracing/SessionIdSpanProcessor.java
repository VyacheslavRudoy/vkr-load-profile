package ru.rudoy.loadprofile.demo.tracing;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadWriteSpan;
import io.opentelemetry.sdk.trace.ReadableSpan;
import io.opentelemetry.sdk.trace.SpanProcessor;

/**
 * Переносит идентификатор сессии из baggage в атрибут спана.
 * <p>
 * Клиент передаёт идентификатор в заголовке {@code baggage: session.id=...}.
 * Baggage распространяется вместе с контекстом трассы, поэтому атрибут
 * получают все спаны трассы, включая вызовы между сервисами.
 */
public final class SessionIdSpanProcessor implements SpanProcessor {

    /** Имя атрибута из семантических соглашений OpenTelemetry. */
    public static final AttributeKey<String> SESSION_ID = AttributeKey.stringKey("session.id");

    @Override
    public void onStart(Context parentContext, ReadWriteSpan span) {
        String sessionId = Baggage.fromContext(parentContext).getEntryValue(SESSION_ID.getKey());
        if (sessionId != null) {
            span.setAttribute(SESSION_ID, sessionId);
        }
    }

    @Override
    public boolean isStartRequired() {
        return true;
    }

    @Override
    public void onEnd(ReadableSpan span) {
    }

    @Override
    public boolean isEndRequired() {
        return false;
    }
}
