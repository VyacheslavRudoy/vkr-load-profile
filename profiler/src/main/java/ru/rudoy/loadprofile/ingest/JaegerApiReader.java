package ru.rudoy.loadprofile.ingest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;

/**
 * Выгружает спаны из Jaeger за указанное временное окно.
 * <p>
 * Jaeger 2.x отдаёт трассы по HTTP в OTLP JSON на {@code /api/v3/traces};
 * границы окна передаются параметрами {@code query.startTimeMin} и
 * {@code query.startTimeMax}. Ответ разбирается уже готовым
 * {@link OtlpJsonReader}, поэтому этот класс занимается только запросом:
 * собирает адрес, ходит по сети и проверяет код ответа.
 */
public final class JaegerApiReader {

    private final HttpClient http = HttpClient.newHttpClient();
    private final URI baseUri;
    private final OtlpJsonReader otlpJson = new OtlpJsonReader();

    public JaegerApiReader(String baseUrl) {
        String trimmed = baseUrl.endsWith("/")
                ? baseUrl.substring(0, baseUrl.length() - 1)
                : baseUrl;
        this.baseUri = URI.create(trimmed);
    }

    /** Выгружает все спаны трасс, попадающих в окно. */
    public List<Span> fetch(TimeWindow window) {
        // Двоеточия в метках времени разрешены в строке запроса, кодировать их не нужно
        URI uri = URI.create(baseUri + "/api/v3/traces"
                + "?query.startTimeMin=" + window.from()
                + "&query.startTimeMax=" + window.to());
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(30))
                .GET()
                .build();
        HttpResponse<String> response = send(request);
        if (response.statusCode() != 200) {
            throw new IllegalStateException("Jaeger returned HTTP " + response.statusCode()
                    + ": " + excerpt(response.body()));
        }
        return otlpJson.readSpans(response.body());
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new UncheckedIOException("cannot reach Jaeger at " + baseUri, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while fetching traces from Jaeger", e);
        }
    }

    private static String excerpt(String body) {
        return body.length() <= 200 ? body : body.substring(0, 200) + "...";
    }
}
