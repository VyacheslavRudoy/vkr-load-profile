package ru.rudoy.loadprofile.demo.traffic;

import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Эталонный профиль нагрузки.
 * <p>
 * Вес сценария — это доля сессий такого типа в процентах. Время
 * обдумывания равномерно на отрезке 1–3 секунды: медиана 2 секунды,
 * 90-й перцентиль 2,8 секунды.
 */
@Configuration
class TrafficConfiguration {

    @Bean
    ReferenceProfile referenceProfile(ShoppingScenarios scenarios) {
        return new ReferenceProfile(
                List.of(
                        new ReferenceProfile.WeightedScenario("quick-browse", 30, scenarios::quickBrowse),
                        new ReferenceProfile.WeightedScenario("browse-and-view", 30, scenarios::browseAndView),
                        new ReferenceProfile.WeightedScenario("full-purchase", 40, scenarios::fullPurchase)),
                new ThinkTime(1_000, 3_000));
    }
}
