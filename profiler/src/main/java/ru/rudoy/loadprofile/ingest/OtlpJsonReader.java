package ru.rudoy.loadprofile.ingest;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Читает спаны из OTLP JSON — формата, которым обмениваются по HTTP экспортёры
 * трассировок, коллектор и Jaeger.
 * <p>
 * Имя сервиса берётся из атрибутов ресурса и кладётся в каждый спан этой группы;
 * группа без имени сервиса считается повреждённой. Целые значения атрибутов
 * protobuf JSON кодирует строками — здесь они снова становятся числами. Спаны
 * возвращаются плоским списком в порядке следования в документе; сборка их
 * в трассы — задача этапа сессионизации.
 */
public final class OtlpJsonReader {

    private final ObjectMapper json = new ObjectMapper();

    /** Читает все спаны из файла с трассировками. */
    public List<Span> readFile(Path file) {
        try {
            return readSpans(Files.readString(file));
        } catch (IOException e) {
            throw new UncheckedIOException("cannot read trace file " + file, e);
        }
    }

    /** Разбирает OTLP JSON в спаны в порядке следования в документе. */
    public List<Span> readSpans(String otlpJson) {
        JsonNode root;
        try {
            root = json.readTree(otlpJson);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("trace file is not valid JSON", e);
        }
        List<Span> spans = new ArrayList<>();
        JsonNode resourceSpans = root.path("resourceSpans");
        for (int i = 0; i < resourceSpans.size(); i++) {
            JsonNode resourceSpan = resourceSpans.get(i);
            String serviceName = serviceNameOf(resourceSpan.path("resource"), i);
            for (JsonNode scopeSpans : resourceSpan.path("scopeSpans")) {
                for (JsonNode spanNode : scopeSpans.path("spans")) {
                    spans.add(toSpan(serviceName, spanNode));
                }
            }
        }
        return List.copyOf(spans);
    }

    private static String serviceNameOf(JsonNode resource, int index) {
        for (JsonNode attribute : resource.path("attributes")) {
            if ("service.name".equals(attribute.path("key").asText())) {
                String name = attribute.path("value").path("stringValue").asText();
                if (!name.isBlank()) {
                    return name;
                }
            }
        }
        throw new IllegalArgumentException(
                "resourceSpans[" + index + "] has no service.name attribute");
    }

    private static Span toSpan(String serviceName, JsonNode node) {
        return new Span(
                node.path("traceId").asText(),
                node.path("spanId").asText(),
                emptyToNull(node.path("parentSpanId").asText()),
                spanKind(node.path("kind").asInt(0)),
                serviceName,
                node.path("name").asText(),
                instant(node.path("startTimeUnixNano")),
                instant(node.path("endTimeUnixNano")),
                attributes(node.path("attributes")));
    }

    private static SpanKind spanKind(int code) {
        return switch (code) {
            case 0 -> SpanKind.UNSPECIFIED;
            case 1 -> SpanKind.INTERNAL;
            case 2 -> SpanKind.SERVER;
            case 3 -> SpanKind.CLIENT;
            case 4 -> SpanKind.PRODUCER;
            case 5 -> SpanKind.CONSUMER;
            default -> throw new IllegalArgumentException("unknown span kind code: " + code);
        };
    }

    private static Instant instant(JsonNode nanos) {
        long value;
        try {
            value = Long.parseLong(nanos.asText());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("missing or invalid timestamp: " + nanos.asText(), e);
        }
        return Instant.ofEpochSecond(value / 1_000_000_000L, value % 1_000_000_000L);
    }

    private static Map<String, Object> attributes(JsonNode attributes) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (JsonNode attribute : attributes) {
            String key = attribute.path("key").asText();
            result.put(key, value(key, attribute.path("value")));
        }
        return result;
    }

    private static Object value(String key, JsonNode value) {
        if (value.has("stringValue")) {
            return value.path("stringValue").asText();
        }
        if (value.has("boolValue")) {
            return value.path("boolValue").asBoolean();
        }
        if (value.has("intValue")) {
            return Long.parseLong(value.path("intValue").asText());
        }
        if (value.has("doubleValue")) {
            return value.path("doubleValue").asDouble();
        }
        throw new IllegalArgumentException(
                "attribute '" + key + "' has an unsupported value: " + value);
    }

    private static String emptyToNull(String text) {
        return text.isEmpty() ? null : text;
    }
}
