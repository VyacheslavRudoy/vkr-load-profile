package ru.rudoy.loadprofile.generator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.Operation;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.model.ParameterSpec;

/**
 * Собирает план теста из профиля нагрузки. Плейсхолдеры пути и параметры
 * строки запроса становятся ссылками {@code ${имя}}; коррелируемые значения
 * извлекаются JSON-экстракторами из ответов операций-источников, фидеры —
 * из примеров значений профиля. Плейсхолдер без спецификации параметра —
 * ошибка: план с пустым местом в пути не имеет смысла.
 */
public final class JmeterPlans {

    private JmeterPlans() {
    }

    public static JmeterPlan from(LoadProfile profile, String host, int port) {
        Map<String, Operation> operations = new HashMap<>();
        for (Operation operation : profile.operations()) {
            operations.put(operation.ref(), operation);
        }
        Map<String, List<ParameterSpec>> specsByOperation = new HashMap<>();
        Map<String, List<JmeterPlan.HttpRequest.Extraction>> extractionsBySource = new HashMap<>();
        // значения фидеров с одинаковым именем объединяются: CSV-файл один на переменную
        Map<String, LinkedHashSet<String>> feederValues = new LinkedHashMap<>();
        for (ParameterSpec spec : profile.parameters()) {
            specsByOperation.computeIfAbsent(spec.operationRef(), ref -> new ArrayList<>()).add(spec);
            if (spec.source() == ParameterSource.CORRELATED) {
                extractionsBySource
                        .computeIfAbsent(spec.correlation().sourceOperationRef(), ref -> new ArrayList<>())
                        .add(new JmeterPlan.HttpRequest.Extraction(spec.name(), spec.correlation().jsonPath()));
            } else {
                feederValues.computeIfAbsent(spec.name(), name -> new LinkedHashSet<>())
                        .addAll(spec.sampleValues());
            }
        }
        List<JmeterPlan.Feeder> feeders = new ArrayList<>();
        for (var entry : feederValues.entrySet()) {
            feeders.add(new JmeterPlan.Feeder(entry.getKey(), List.copyOf(entry.getValue())));
        }
        List<JmeterPlan.Sequence> sequences = new ArrayList<>();
        int number = 1;
        for (ru.rudoy.loadprofile.model.Sequence profileSequence : profile.sequences()) {
            List<JmeterPlan.HttpRequest> steps = new ArrayList<>();
            for (String ref : profileSequence.steps()) {
                steps.add(requestOf(operations.get(ref), ref,
                        specsByOperation.getOrDefault(ref, List.of()), extractionsBySource));
            }
            sequences.add(new JmeterPlan.Sequence("Последовательствие " + number++,
                    profileSequence.weight(), steps));
        }
        LoadShape load = LoadShape.from(profile.arrivalRate(), profile.sourceWindow(), profile.thinkTime());
        return new JmeterPlan(host, port, load, feeders, sequences);
    }

    private static JmeterPlan.HttpRequest requestOf(Operation operation, String ref,
                                                    List<ParameterSpec> specs,
                                                    Map<String, List<JmeterPlan.HttpRequest.Extraction>> extractionsBySource) {
        String path = operation.pathTemplate();
        List<JmeterPlan.HttpRequest.Query> query = new ArrayList<>();
        for (ParameterSpec spec : specs) {
            String placeholder = "{" + spec.name() + "}";
            if (path.contains(placeholder)) {
                path = path.replace(placeholder, "${" + spec.name() + "}");
            } else {
                query.add(new JmeterPlan.HttpRequest.Query(spec.name(), "${" + spec.name() + "}"));
            }
        }
        // незаполненный плейсхолдер: "{" без "$" перед ним ({x}, но не ${x})
        for (int i = path.indexOf('{'); i >= 0; i = path.indexOf('{', i + 1)) {
            if (i == 0 || path.charAt(i - 1) != '$') {
                throw new IllegalArgumentException(
                        "operation " + ref + " has a placeholder without a parameter spec: " + path);
            }
        }
        return new JmeterPlan.HttpRequest(operation.method(), path, query,
                extractionsBySource.getOrDefault(ref, List.of()));
    }
}
