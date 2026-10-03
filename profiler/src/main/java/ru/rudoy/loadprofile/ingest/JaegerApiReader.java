package ru.rudoy.loadprofile.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
 * {@code query.startTimeMax}. Ответ приходит в конверте {@code result} —
 * класс снимает его и отдаёт содержимое готовому {@link OtlpJsonReader},
 * поэтому сам занимается только запросом: собирает адрес, ходит по сети
 * и проверяет код ответа.
 */
public final class JaegerApiReader {

    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper json = new ObjectMapper();
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
        return otlpJson.readSpans(otlpPayload(response.body()));
    }

    /**
     * Снимает конверт {@code {"result": {...}}}, в который Jaeger заворачивает
     * ответ. Ответ без конверта — как от заглушек — принимается как есть.
     */
    private String otlpPayload(String body) {
        try {
            JsonNode root = json.readTree(body);
            return root.has("result") ? root.get("result").toString() : body;
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Jaeger answer is not valid JSON", e);
        }
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
