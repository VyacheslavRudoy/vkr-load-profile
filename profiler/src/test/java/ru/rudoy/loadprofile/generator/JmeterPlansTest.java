package ru.rudoy.loadprofile.generator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.Operation;
import ru.rudoy.loadprofile.model.ParameterSource;
import ru.rudoy.loadprofile.model.ParameterSpec;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

class JmeterPlansTest {

    @Test
    void mapsProfileParametersOntoPlan() {
        JmeterPlan plan = JmeterPlans.from(profile(), "localhost", 8080);

        assertThat(plan.feeders()).extracting(JmeterPlan.Feeder::name)
                .containsExactly("category", "quantity");

        JmeterPlan.Sequence sequence = plan.sequences().get(0);
        assertThat(sequence.steps()).hasSize(3);

        JmeterPlan.HttpRequest catalog = sequence.steps().get(0);
        assertThat(catalog.path()).isEqualTo("/api/products");
        assertThat(catalog.query()).containsExactly(
                new JmeterPlan.HttpRequest.Query("category", "${category}"));
        assertThat(catalog.extractions()).containsExactly(
                new JmeterPlan.HttpRequest.Extraction("productId", "$[0].id"));

        JmeterPlan.HttpRequest create = sequence.steps().get(1);
        assertThat(create.extractions()).containsExactly(
                new JmeterPlan.HttpRequest.Extraction("uuid", "$.id"));

        JmeterPlan.HttpRequest items = sequence.steps().get(2);
        assertThat(items.path()).isEqualTo("/api/orders/${uuid}/items");
        assertThat(items.query()).containsExactly(
                new JmeterPlan.HttpRequest.Query("productId", "${productId}"),
                new JmeterPlan.HttpRequest.Query("quantity", "${quantity}"));
        assertThat(items.extractions()).isEmpty();

        assertThat(plan.load().threads()).isEqualTo(6);
        assertThat(plan.load().timer().constantDelayMs()).isEqualTo(2000);
    }

    @Test
    void rejectsPlaceholderWithoutSpec() {
        LoadProfile profile = new LoadProfile(null, "кривой", Instant.parse("2026-10-27T10:00:00Z"),
                new TimeWindow(Instant.parse("2026-10-27T10:00:00Z"), Instant.parse("2026-10-27T10:01:00Z")),
                List.of(new Operation("GET", "/api/products/{id}", 1, 1.0)),
                List.of(new ru.rudoy.loadprofile.model.Sequence(List.of("GET /api/products/{id}"), 1, 1.0)),
                new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2000, 2800, 1000, 3000),
                List.of());

        assertThatThrownBy(() -> JmeterPlans.from(profile, "localhost", 8080))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("{id}");
    }

    @Test
    void mergesFeederValuesWhenTheSameNameAppearsOnTwoOperations() {
        Instant from = Instant.parse("2026-10-27T10:00:00Z");
        Instant to = Instant.parse("2026-10-27T10:01:00Z");
        LoadProfile profile = new LoadProfile(null, "два productId", Instant.parse("2026-10-27T11:00:00Z"),
                new TimeWindow(from, to),
                List.of(new Operation("GET", "/api/products", 6, 0.5),
                        new Operation("POST", "/api/orders/items", 6, 0.5)),
                List.of(new ru.rudoy.loadprofile.model.Sequence(
                        List.of("GET /api/products", "POST /api/orders/items"), 6, 1.0)),
                new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2000, 2800, 1000, 3000),
                List.of(
                        new ParameterSpec("productId", "POST /api/orders/items",
                                ParameterSource.FEEDER, null, List.of("7", "12")),
                        new ParameterSpec("productId", "GET /api/products",
                                ParameterSource.FEEDER, null, List.of("12", "23"))));

        JmeterPlan plan = JmeterPlans.from(profile, "localhost", 8080);

        assertThat(plan.feeders()).hasSize(1);
        assertThat(plan.feeders().get(0).name()).isEqualTo("productId");
        assertThat(plan.feeders().get(0).values()).containsExactly("7", "12", "23");
    }

    private static LoadProfile profile() {
        Instant from = Instant.parse("2026-10-27T10:00:00Z");
        Instant to = Instant.parse("2026-10-27T10:01:00Z");
        return new LoadProfile(null, "прогон A", Instant.parse("2026-10-27T11:00:00Z"),
                new TimeWindow(from, to),
                List.of(
                        new Operation("GET", "/api/products", 6, 0.5),
                        new Operation("POST", "/api/orders", 3, 0.25),
                        new Operation("POST", "/api/orders/{uuid}/items", 3, 0.25)),
                List.of(new ru.rudoy.loadprofile.model.Sequence(List.of(
                        "GET /api/products", "POST /api/orders", "POST /api/orders/{uuid}/items"), 3, 1.0)),
                new ArrivalRate(6.0, List.of()),
                new ThinkTimeStats(2000, 2800, 1000, 3000),
                List.of(
                        new ParameterSpec("productId", "POST /api/orders/{uuid}/items",
                                ParameterSource.CORRELATED,
                                new ParameterSpec.Correlation("GET /api/products", "$[0].id"), List.of()),
                        new ParameterSpec("uuid", "POST /api/orders/{uuid}/items",
                                ParameterSource.CORRELATED,
                                new ParameterSpec.Correlation("POST /api/orders", "$.id"), List.of()),
                        new ParameterSpec("category", "GET /api/products",
                                ParameterSource.FEEDER, null, List.of("books", "sport")),
                        new ParameterSpec("quantity", "POST /api/orders/{uuid}/items",
                                ParameterSource.FEEDER, null, List.of("1", "2"))));
    }
}
