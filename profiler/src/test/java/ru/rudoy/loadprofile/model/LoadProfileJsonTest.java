package ru.rudoy.loadprofile.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LoadProfileJsonTest {

    private final LoadProfileJson json = new LoadProfileJson();

    @Test
    void roundTripsThroughJson() {
        LoadProfile profile = TestProfiles.sampleProfile();

        assertThat(json.read(json.write(profile))).isEqualTo(profile);
    }

    @Test
    void writesReadableJson() {
        String text = json.write(TestProfiles.sampleProfile());

        assertThat(text).contains("\"pathTemplate\"").contains("/api/orders/{uuid}/items");
        assertThat(text).contains("\"sourceWindow\"").contains("2026-09-28T10:00:00Z");
        assertThat(text).contains("\"medianMs\"").contains("\"sessionsPerMinute\"");
        assertThat(text).contains(System.lineSeparator());
    }
}
