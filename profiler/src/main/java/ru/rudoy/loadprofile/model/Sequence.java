package ru.rudoy.loadprofile.model;

import java.util.List;

/** Цепочка операций одной сессии; шаги ссылаются на операции строкой {@code "МЕТОД шаблон"}. */
public record Sequence(List<String> steps, long count, double weight) {

    public Sequence {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("a sequence consists of at least one step");
        }
        steps = List.copyOf(steps);
        if (count < 1) {
            throw new IllegalArgumentException("count must be positive: " + count);
        }
        if (weight <= 0 || weight > 1) {
            throw new IllegalArgumentException("weight must be in (0, 1]: " + weight);
        }
    }
}
