package ru.rudoy.loadprofile.session;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SessionStrategiesTest {

    @Test
    void createsTheChosenStrategy() {
        assertThat(SessionStrategies.create(SessionStrategyType.BY_SESSION_ID))
                .isInstanceOf(SessionByIdStrategy.class);
        assertThat(SessionStrategies.create(SessionStrategyType.BY_CLIENT_TIMEOUT))
                .isInstanceOf(SessionByClientStrategy.class);
    }
}
