package ru.rudoy.loadprofile.model;

import java.time.Instant;
import java.util.List;
import ru.rudoy.loadprofile.ingest.TimeWindow;

/** Небольшой корректный профиль для тестов модели и сериализации. */
final class TestProfiles {

    private TestProfiles() {
    }

    static LoadProfile sampleProfile() {
        Instant from = Instant.parse("2026-09-28T10:00:00Z");
        Instant to = Instant.parse("2026-09-28T11:00:00Z");
        return new LoadProfile(null, "stand-run-a", Instant.parse("2026-09-28T12:00:00Z"),
                new TimeWindow(from, to),
                List.of(
                        new Operation("GET", "/api/products", 12, 0.55),
                        new Operation("GET", "/api/products/{id}", 4, 0.18),
                        new Operation("POST", "/api/orders", 3, 0.14),
                        new Operation("POST", "/api/orders/{uuid}/items", 2, 0.09),
                        new Operation("POST", "/api/orders/{uuid}/checkout", 1, 0.04)),
                List.of(new Sequence(List.of("GET /api/products", "POST /api/orders",
                        "POST /api/orders/{uuid}/items", "POST /api/orders/{uuid}/checkout"), 2, 0.4)),
                new ArrivalRate(6.0, List.of(new IntervalRate(from, to, 6.0))),
                new ThinkTimeStats(2000, 2800, 1000, 3000),
                List.of(new ParameterSpec("id", "GET /api/products/{id}", ParameterSource.FEEDER,
                        null, List.of("7", "12", "23"))));
    }
}
