package ru.rudoy.loadprofile.generator;

import java.util.List;

/** План теста: цель и последовательности операций с весами. */
public record JmeterPlan(String host, int port, List<Sequence> sequences) {

    public JmeterPlan {
        if (host == null || host.isBlank()) {
            throw new IllegalArgumentException("host must not be blank");
        }
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("port must be between 1 and 65535: " + port);
        }
        sequences = List.copyOf(sequences);
        if (sequences.isEmpty()) {
            throw new IllegalArgumentException("plan needs at least one sequence");
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

    public record HttpRequest(String method, String path) {

        public HttpRequest {
            if (method == null || method.isBlank()) {
                throw new IllegalArgumentException("method must not be blank");
            }
            if (path == null || !path.startsWith("/")) {
                throw new IllegalArgumentException("path must start with '/': " + path);
            }
        }
    }
}
