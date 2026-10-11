package ru.rudoy.loadprofile.model;

import java.util.List;

/**
 * Параметр запроса в тесте: сегмент пути или поле строки запроса. Поля
 * корреляции заполнены только у CORRELATED, примеры значений — только у FEEDER,
 * у CONSTANT нет ни того ни другого.
 */
public record ParameterSpec(String name, String operationRef, ParameterSource source,
                            Correlation correlation, List<String> sampleValues) {

    public ParameterSpec {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("parameter name must not be blank");
        }
        if (source == null) {
            throw new IllegalArgumentException("parameter source must not be null");
        }
        sampleValues = sampleValues == null ? List.of() : List.copyOf(sampleValues);
        switch (source) {
            case CORRELATED -> {
                if (correlation == null) {
                    throw new IllegalArgumentException("correlation is required for CORRELATED: " + name);
                }
                if (!sampleValues.isEmpty()) {
                    throw new IllegalArgumentException("sampleValues are only allowed for FEEDER: " + name);
                }
            }
            case FEEDER -> {
                if (sampleValues.isEmpty()) {
                    throw new IllegalArgumentException("sampleValues are required for FEEDER: " + name);
                }
                if (correlation != null) {
                    throw new IllegalArgumentException("correlation is only allowed for CORRELATED: " + name);
                }
            }
            case CONSTANT -> {
                if (correlation != null || !sampleValues.isEmpty()) {
                    throw new IllegalArgumentException("CONSTANT takes no correlation or samples: " + name);
                }
            }
        }
    }

    /** Связь значения параметра с ответом более раннего запроса сессии. */
    public record Correlation(String sourceOperationRef, String jsonPath) {

        public Correlation {
            if (sourceOperationRef == null || sourceOperationRef.isBlank()) {
                throw new IllegalArgumentException("sourceOperationRef must not be blank");
            }
            if (jsonPath == null || jsonPath.isBlank()) {
                throw new IllegalArgumentException("jsonPath must not be blank");
            }
        }
    }
}
