package ru.rudoy.loadprofile.model;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import ru.rudoy.loadprofile.ingest.TimeWindow;

/**
 * Профиль нагрузки: читаемый артефакт, из которого собирается план теста.
 * Операции в последовательностях и параметрах ссылаются строкой
 * {@code "МЕТОД шаблон"} на элементы {@code operations}. Идентификатор
 * назначается хранилищем и до сохранения равен {@code null}.
 */
public record LoadProfile(Long id, String name, Instant createdAt, TimeWindow sourceWindow,
                          List<Operation> operations, List<Sequence> sequences,
                          ArrivalRate arrivalRate, ThinkTimeStats thinkTime,
                          List<ParameterSpec> parameters) {

    public LoadProfile {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("profile name must not be blank");
        }
        if (createdAt == null) {
            throw new IllegalArgumentException("createdAt must not be null");
        }
        if (operations == null || operations.isEmpty()) {
            throw new IllegalArgumentException("profile needs at least one operation");
        }
        if (arrivalRate == null || thinkTime == null) {
            throw new IllegalArgumentException("arrivalRate and thinkTime must not be null");
        }
        operations = List.copyOf(operations);
        sequences = sequences == null ? List.of() : List.copyOf(sequences);
        parameters = parameters == null ? List.of() : List.copyOf(parameters);
        Set<String> refs = operations.stream().map(Operation::ref).collect(Collectors.toSet());
        for (Sequence sequence : sequences) {
            for (String step : sequence.steps()) {
                if (!refs.contains(step)) {
                    throw new IllegalArgumentException(
                            "sequence step references unknown operation: " + step);
                }
            }
        }
        for (ParameterSpec parameter : parameters) {
            if (!refs.contains(parameter.operationRef())) {
                throw new IllegalArgumentException(
                        "parameter references unknown operation: " + parameter.operationRef());
            }
        }
    }
}
