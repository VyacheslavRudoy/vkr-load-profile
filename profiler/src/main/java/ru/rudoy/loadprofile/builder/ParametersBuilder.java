package ru.rudoy.loadprofile.builder;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.ingest.PathTemplates;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.Trace;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.model.ParameterSpec;
import ru.rudoy.loadprofile.session.Session;

/**
 * Спецификации параметров запросов. Значение параметра ищется среди скаляров
 * тел ответов более ранних запросов той же сессии — строки совпадают точно,
 * числа по значению; подстрока не считается, часть идентификатора давала бы
 * ложные связи. Параметр, у которого совпали все наблюдавшиеся значения,
 * получает спецификацию CORRELATED с самой частой парой «операция + путь
 * в JSON»; остальные — FEEDER со всеми наблюдавшимися значениями без
 * повторов, в порядке появления.
 */
public final class ParametersBuilder {

    private final ObjectMapper json = new ObjectMapper();

    public List<ParameterSpec> build(List<Session> sessions) {
        List<ParameterSpec> specs = new ArrayList<>();
        for (Stats stats : walk(sessions).values()) {
            if (stats.isCorrelated()) {
                specs.add(stats.toCorrelatedSpec());
            }
        }
        return specs;
    }

    public List<ParameterSpec> buildFeeder(List<Session> sessions) {
        List<ParameterSpec> specs = new ArrayList<>();
        for (Stats stats : walk(sessions).values()) {
            if (stats.isFeeder()) {
                specs.add(stats.toFeederSpec());
            }
        }
        return specs;
    }

    private Map<String, Stats> walk(List<Session> sessions) {
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
                    stats.observed(param.value());
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
        return byParameter;
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
        private final LinkedHashSet<String> values = new LinkedHashSet<>();

        private Stats(String operationRef, String name) {
            this.operationRef = operationRef;
            this.name = name;
        }

        private void observed(String value) {
            total++;
            values.add(value);
        }

        private void matched(Source source) {
            sources.merge(source, 1, Integer::sum);
        }

        private boolean isCorrelated() {
            return total > 0 && matchedCount() == total;
        }

        private boolean isFeeder() {
            return total > 0 && matchedCount() < total;
        }

        private int matchedCount() {
            return sources.values().stream().mapToInt(Integer::intValue).sum();
        }

        private ParameterSpec toCorrelatedSpec() {
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

        private ParameterSpec toFeederSpec() {
            return new ParameterSpec(name, operationRef, ParameterSource.FEEDER, null,
                    List.copyOf(values));
        }
    }
}
