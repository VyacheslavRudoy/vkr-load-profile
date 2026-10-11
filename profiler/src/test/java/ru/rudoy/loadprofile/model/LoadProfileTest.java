package ru.rudoy.loadprofile.model;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import ru.rudoy.loadprofile.ingest.TimeWindow;

class LoadProfileTest {

    @Test
    void rejectsSequenceStepOutsideOperations() {
        assertThatThrownBy(() -> profileWithSequence(
                new Sequence(List.of("GET /api/products", "DELETE /api/orders"), 1, 1.0)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("DELETE /api/orders");
    }

    @Test
    void rejectsParameterOutsideOperations() {
        assertThatThrownBy(() -> profileWithParameters(new ParameterSpec(
                "id", "GET /api/unknown/{id}", ParameterSource.CONSTANT, null, List.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("GET /api/unknown/{id}");
    }

    @Test
    void rejectsCorrelatedWithoutCorrelation() {
        assertThatThrownBy(() -> new ParameterSpec(
                "orderId", "POST /api/orders/{uuid}/items", ParameterSource.CORRELATED,
                null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("orderId");
    }

    @Test
    void rejectsFeederWithoutSamples() {
        assertThatThrownBy(() -> new ParameterSpec(
                "customerId", "POST /api/orders", ParameterSource.FEEDER, null, List.of()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("customerId");
    }

    @Test
    void rejectsDisorderedThinkTimeStats() {
        assertThatThrownBy(() -> new ThinkTimeStats(2000, 1800, 1000, 3000))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("p90");
    }

    private static LoadProfile profileWithSequence(Sequence sequence) {
        LoadProfile base = TestProfiles.sampleProfile();
        return new LoadProfile(base.id(), base.name(), base.createdAt(), base.sourceWindow(),
                base.operations(), List.of(sequence), base.arrivalRate(), base.thinkTime(),
                base.parameters());
    }

    private static LoadProfile profileWithParameters(ParameterSpec parameter) {
        LoadProfile base = TestProfiles.sampleProfile();
        return new LoadProfile(base.id(), base.name(), base.createdAt(), base.sourceWindow(),
                base.operations(), base.sequences(), base.arrivalRate(), base.thinkTime(),
                List.of(parameter));
    }
}
