package ru.rudoy.loadprofile.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.PathTemplates;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.model.ParameterSpec;
import ru.rudoy.loadprofile.session.Session;

/**
 * Выявление коррелируемых параметров: значение параметра запроса ищется
 * среди скаляров тел ответов более ранних запросов той же сессии — строки
 * совпадают точно, числа по значению. Параметр считается коррелируемым,
 * когда совпали все его наблюдавшиеся значения; источником берётся самая
 * частая пара «операция + путь в JSON». Совпадение по подстроке не считается:
 * часть идентификатора давала бы ложные связи.
 */
public final class CorrelatedParametersBuilder {

    private final ObjectMapper json = new ObjectMapper();

    public List<ParameterSpec> build(List<Session> sessions) {
        Map<String, Stats> byParameter = new LinkedHashMap<>();
        for (Session session : sessions) {
            List<EarlierResponse> earlierResponses = new ArrayList<>();
            for (Trace trace : session.traces()) {
                Span root = trace.rootSpan().orElseThrow(() ->
                        new IllegalArgumentException("trace " + trace.traceId() + " has no root span"));
                String ref = attribute(root, trace, "http.request.method") + " "
                        + PathTemplates.template(attribute(root, trace, "url.path"));
                for (Param param : paramsOf(root, trace)) {
                    Stats stats = byParameter.computeIfAbsent(ref + "#" + param.name(),
                            key -> new Stats(ref, param.name()));
                    stats.total++;
                    Source source = findInEarlier(earlierResponses, param.value());
                    if (source != null) {
                        stats.matched(source);
                    }
                }
                Object body = root.attributes().get("http.response.body");
                if (body instanceof String text && !text.isBlank()) {
                    earlierResponses.add(new EarlierResponse(ref, text));
                }
            }
        }
        List<ParameterSpec> specs = new ArrayList<>();
        for (Stats stats : byParameter.values()) {
            if (stats.isCorrelated()) {
                specs.add(stats.toSpec());
            }
        }
        return specs;
    }

    private static List<Param> paramsOf(Span root, Trace trace) {
        String path = attribute(root, trace, "url.path");
        String template = PathTemplates.template(path);
        String[] segments = path.split("/", -1);
        String[] templateSegments = template.split("/", -1);
        List<Param> params = new ArrayList<>();
        for (int i = 0; i < segments.length; i++) {
            String placeholder = templateSegments[i];
            if (placeholder.startsWith("{") && placeholder.endsWith("}")) {
                params.add(new Param(placeholder.substring(1, placeholder.length() - 1), segments[i]));
            }
        }
        Object query = root.attributes().get("url.query");
        if (query instanceof String text && !text.isBlank()) {
            for (String pair : text.split("&")) {
                int equals = pair.indexOf('=');
                if (equals > 0) {
                    params.add(new Param(pair.substring(0, equals), pair.substring(equals + 1)));
                }
            }
        }
        return params;
    }

    private Source findInEarlier(List<EarlierResponse> earlierResponses, String value) {
        for (EarlierResponse earlier : earlierResponses) {
            String jsonPath = findValue(earlier.body, value);
            if (jsonPath != null) {
                return new Source(earlier.operationRef, jsonPath);
            }
        }
        return null;
    }

    private String findValue(String body, String value) {
        JsonNode root;
        try {
            root = json.readTree(body);
        } catch (JsonProcessingException e) {
            return null;
        }
        return findIn(root, "$", value);
    }

    private String findIn(JsonNode node, String path, String value) {
        if (node.isValueNode()) {
            boolean matches = node.isTextual() && value.equals(node.asText())
                    || node.isNumber() && parsesToSameNumber(value, node);
            return matches ? path : null;
        }
        if (node.isArray()) {
            for (int i = 0; i < node.size(); i++) {
                String found = findIn(node.get(i), path + "[" + i + "]", value);
                if (found != null) {
                    return found;
                }
            }
        } else {
            for (var field : node.properties()) {
                String found = findIn(field.getValue(), path + "." + field.getKey(), value);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static boolean parsesToSameNumber(String value, JsonNode number) {
        try {
            return new BigDecimal(value).compareTo(number.decimalValue()) == 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private static String attribute(Span root, Trace trace, String name) {
        Object value = root.attributes().get(name);
        if (!(value instanceof String text) || text.isBlank()) {
            throw new IllegalArgumentException(
                    "root span of trace " + trace.traceId() + " has no " + name + " attribute");
        }
        return text;
    }

    private record Param(String name, String value) {
    }

    private record EarlierResponse(String operationRef, String body) {
    }

    private record Source(String operationRef, String jsonPath) {
    }

    private static final class Stats {
        private final String operationRef;
        private final String name;
        private int total;
        private final Map<Source, Integer> sources = new LinkedHashMap<>();

        private Stats(String operationRef, String name) {
            this.operationRef = operationRef;
            this.name = name;
        }

        private void matched(Source source) {
            sources.merge(source, 1, Integer::sum);
        }

        private boolean isCorrelated() {
            int matched = sources.values().stream().mapToInt(Integer::intValue).sum();
            return total > 0 && matched == total;
        }

        private ParameterSpec toSpec() {
            Source best = null;
            int bestCount = 0;
            for (var entry : sources.entrySet()) {
                if (entry.getValue() > bestCount) {
                    best = entry.getKey();
                    bestCount = entry.getValue();
                }
            }
            return new ParameterSpec(name, operationRef, ParameterSource.CORRELATED,
                    new ParameterSpec.Correlation(best.operationRef(), best.jsonPath()), List.of());
        }
    }
}
