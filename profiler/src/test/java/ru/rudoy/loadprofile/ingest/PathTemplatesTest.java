package ru.rudoy.loadprofile.ingest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class PathTemplatesTest {

    @Test
    void replacesNumericSegments() {
        assertThat(PathTemplates.template("/api/products/42"))
                .isEqualTo("/api/products/{id}");
        assertThat(PathTemplates.template("/api/orders/42/items/7"))
                .isEqualTo("/api/orders/{id}/items/{id}");
    }

    @Test
    void replacesUuidSegments() {
        assertThat(PathTemplates.template("/api/orders/550e8400-e29b-41d4-a716-446655440000/checkout"))
                .isEqualTo("/api/orders/{uuid}/checkout");
        assertThat(PathTemplates.template("/api/orders/550E8400-E29B-41D4-A716-446655440000"))
                .isEqualTo("/api/orders/{uuid}");
    }

    @Test
    void keepsWordsThatContainDigits() {
        assertThat(PathTemplates.template("/api/v2/products"))
                .isEqualTo("/api/v2/products");
        assertThat(PathTemplates.template("/files/report2024.pdf"))
                .isEqualTo("/files/report2024.pdf");
    }

    @Test
    void leavesPlainPathsAlone() {
        assertThat(PathTemplates.template("/api/products"))
                .isEqualTo("/api/products");
        assertThat(PathTemplates.template("/api/orders/"))
                .isEqualTo("/api/orders/");
        assertThat(PathTemplates.template("/"))
                .isEqualTo("/");
    }

    @Test
    void rejectsRelativeOrNullPath() {
        assertThatThrownBy(() -> PathTemplates.template("api/orders"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> PathTemplates.template(null))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
