package ru.rudoy.loadprofile.generator;

import java.util.List;

/** План теста: цель, параметры нагрузки, фидеры и последовательности операций с весами. */
public record JmeterPlan(String host, int port, LoadShape load,
                         List<Feeder> feeders, List<Sequence> sequences) {

    public JmeterPlan {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535: " + port);
        }
        if (load == null) {
            throw new IllegalArgumentException("load must not be null");
        }
        feeders = List.copyOf(feeders);
        sequences = List.copyOf(sequences);
        if (sequences.isEmpty()) {
            throw new IllegalArgumentException("plan needs at least one sequence");
        }
    }

    /** Фидер: значения одного параметра, построчно в CSV-файле с именем переменной. */
    public record Feeder(String name, List<String> values) {

        public Feeder {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("feeder name must not be blank");
            }
            values = List.copyOf(values);
            if (values.isEmpty()) {
                throw new IllegalArgumentException("feeder needs at least one value: " + name);
            }
            for (String value : values) {
                if (value.isBlank() || value.contains(",") || value.contains("\n")) {
                    throw new IllegalArgumentException(
                            "feeder value must not be blank or contain comma/newline: " + value);
                }
            }
        }
    }

    /** Последовательность операций: вес — доля сессий такого типа. */
    public record Sequence(String name, double weight, List<HttpRequest> steps) {

        public Sequence {
            if (name == null || name.isBlank()) {
                throw new IllegalArgumentException("sequence name must not be blank");
            }
            if (weight <= 0 || weight > 1) {
                throw new IllegalArgumentException("weight must be in (0, 1]: " + weight);
            }
            steps = List.copyOf(steps);
            if (steps.isEmpty()) {
                throw new IllegalArgumentException("sequence needs at least one step");
            }
        }
    }

    /**
     * Запрос плана. Путь и значения параметров могут содержать ссылки
     * {@code ${имя}} на переменные экстракций и фидеров.
     */
    public record HttpRequest(String method, String path, List<Query> query,
                              List<Extraction> extractions) {

        public HttpRequest {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("method must not be blank");
            }
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("path must start with '/': " + path);
            }
            query = List.copyOf(query);
            extractions = List.copyOf(extractions);
        }

        /** Параметр строки запроса. */
        public record Query(String name, String value) {

            public Query {
                if (name == null || name.isBlank()) {
                    throw new IllegalArgumentException("query name must not be blank");
                }
                if (value == null || value.isBlank()) {
                    throw new IllegalArgumentException("query value must not be blank: " + name);
                }
            }
        }

        /** Извлечение значения из JSON-ответа этого запроса в переменную. */
        public record Extraction(String variable, String jsonPath) {

            public Extraction {
                if (variable == null || variable.isBlank()) {
                    throw new IllegalArgumentException("variable must not be blank");
                }
                if (jsonPath == null || !jsonPath.startsWith("$")) {
                    throw new IllegalArgumentException("jsonPath must start with '$': " + jsonPath);
                }
            }
        }
    }
}
