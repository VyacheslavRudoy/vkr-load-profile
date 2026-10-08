package ru.rudoy.loadprofile.session;

import java.time.Duration;

/** Создаёт стратегию сессионизации по выбранному варианту. */
public final class SessionStrategies {

    /** Таймаут бездействия резервной стратегии по проектному решению. */
    public static final Duration CLIENT_TIMEOUT = Duration.ofMinutes(30);

    private SessionStrategies() {
    }

    public static SessionStrategy create(SessionStrategyType type) {
        return switch (type) {
            case BY_SESSION_ID -> new SessionByIdStrategy();
            case BY_CLIENT_TIMEOUT -> new SessionByClientStrategy(CLIENT_TIMEOUT);
        };
    }
}
