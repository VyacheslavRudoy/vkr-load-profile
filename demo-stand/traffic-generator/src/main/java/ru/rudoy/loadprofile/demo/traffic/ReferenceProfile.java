package ru.rudoy.loadprofile.demo.traffic;

import java.util.List;
import java.util.Random;

/**
 * Эталонный профиль нагрузки, по которому работает генератор.
 * <p>
 * Профиль задаётся сценариями с весами и распределением времени
 * обдумывания. Именно с этим профилем позже сравнивается профиль,
 * восстановленный из трассировок.
 */
public record ReferenceProfile(List<WeightedScenario> scenarios, ThinkTime thinkTime) {

    public ReferenceProfile {
        scenarios = List.copyOf(scenarios);
        if (scenarios.isEmpty()) {
            throw new IllegalArgumentException("profile must have at least one scenario");
        }
    }

    /** Сумма весов всех сценариев. */
    public int totalWeight() {
        return scenarios.stream().mapToInt(WeightedScenario::weight).sum();
    }

    /**
     * Выбирает сценарий по весам: датчик выдаёт число от 0 до суммы весов,
     * и оно попадает на отрезок конкретного сценария.
     */
    public WeightedScenario chooseScenario(Random random) {
        int ticket = random.nextInt(totalWeight());
        for (WeightedScenario scenario : scenarios) {
            if (ticket < scenario.weight()) {
                return scenario;
            }
            ticket -= scenario.weight();
        }
        throw new IllegalStateException("scenario weights do not cover the whole range");
    }

    /** Сценарий использования и его вес: доля сессий такого типа. */
    public record WeightedScenario(String name, int weight, Scenario script) {

        public WeightedScenario {
            if (weight <= 0) {
                throw new IllegalArgumentException("scenario weight must be positive: " + name);
            }
        }
    }

    /** Один сценарий: цепочка обращений к стенду от имени пользователя. */
    @FunctionalInterface
    public interface Scenario {

        void play(StandClient stand, Random random) throws Exception;
    }
}
