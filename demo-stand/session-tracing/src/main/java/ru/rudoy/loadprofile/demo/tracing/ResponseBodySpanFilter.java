package ru.rudoy.loadprofile.demo.tracing;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingResponseWrapper;

/**
 * Записывает тело JSON-ответа в атрибут {@code http.response.body} текущего
 * спана: по нему профилировщик связывает параметры следующих запросов
 * со значениями из ответов. Тела длиннее лимита не записываются — обрезанный
 * JSON для поиска значений непригоден.
 */
public final class ResponseBodySpanFilter extends OncePerRequestFilter {

    public static final String ATTRIBUTE = "http.response.body";
    private static final int LIMIT = 16 * 1024;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        ContentCachingResponseWrapper wrapper = new ContentCachingResponseWrapper(response);
        try {
            filterChain.doFilter(request, wrapper);
        } finally {
            byte[] body = wrapper.getContentAsByteArray();
            String contentType = wrapper.getContentType();
            if (contentType != null && contentType.contains("json") && body.length <= LIMIT) {
                Span.current().setAttribute(ATTRIBUTE, new String(body, StandardCharsets.UTF_8));
            }
            wrapper.copyBodyToResponse();
        }
    }
}
