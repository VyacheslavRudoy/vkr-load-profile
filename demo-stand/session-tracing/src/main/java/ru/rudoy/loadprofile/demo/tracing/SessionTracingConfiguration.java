package ru.rudoy.loadprofile.demo.tracing;

import io.opentelemetry.sdk.autoconfigure.spi.AutoConfigurationCustomizerProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Подключает {@link SessionIdSpanProcessor} к SDK OpenTelemetry.
 * Сервис импортирует эту конфигурацию через {@code @Import}.
 */
@Configuration
public class SessionTracingConfiguration {

    @Bean
    AutoConfigurationCustomizerProvider sessionIdOnSpans() {
        return customizer -> customizer.addTracerProviderCustomizer(
                (builder, config) -> builder.addSpanProcessor(new SessionIdSpanProcessor()));
    }
}
