package ru.rudoy.loadprofile.demo.traffic;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Spec;

/**
 * Команда {@code traffic-generator}.
 * <p>
 * Разгоняет стенд пользовательской активностью по эталонному профилю.
 * Код завершения: 0 — все сессии прошли, 1 — часть сессий упала или
 * прогон прерван, 2 — неверные параметры.
 */
@Component
@Command(
        name = "traffic-generator",
        description = "Генерирует пользовательскую активность на демонстрационном стенде "
                + "по эталонному профилю нагрузки.")
public class TrafficGeneratorCommand implements Callable<Integer> {

    @Spec
    private CommandSpec spec;

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "Показать справку и выйти")
    private boolean help;

    @Option(names = "--catalog-url", defaultValue = "http://localhost:8081",
            description = "Адрес сервиса каталога. По умолчанию: ${DEFAULT-VALUE}.")
    private String catalogUrl;

    @Option(names = "--orders-url", defaultValue = "http://localhost:8082",
            description = "Адрес сервиса заказов. По умолчанию: ${DEFAULT-VALUE}.")
    private String ordersUrl;

    @Option(names = "--sessions-per-minute", defaultValue = "6",
            description = "Сколько новых пользовательских сессий начинать в минуту. "
                    + "По умолчанию: ${DEFAULT-VALUE}.")
    private int sessionsPerMinute;

    @Option(names = "--duration-seconds", defaultValue = "300",
            description = "Длительность прогона в секундах. По умолчанию: ${DEFAULT-VALUE}.")
    private int durationSeconds;

    @Option(names = "--seed",
            description = "Зерно генератора случайных чисел. Одно и то же зерно даёт одинаковый "
                    + "состав сессий; по умолчанию выбирается случайно.")
    private Long seed;

    private final LoadDriver driver;

    TrafficGeneratorCommand(LoadDriver driver) {
        this.driver = driver;
    }

    @Override
    public Integer call() throws InterruptedException {
        if (help) {
            spec.commandLine().usage(spec.commandLine().getOut());
            return 0;
        }
        long runSeed = seed != null ? seed : ThreadLocalRandom.current().nextLong();
        try {
            LoadDriver.RunOptions options = new LoadDriver.RunOptions(
                    catalogUrl, ordersUrl, sessionsPerMinute, Duration.ofSeconds(durationSeconds), runSeed);
            LoadDriver.RunResult result = driver.run(options);
            return result.successful() ? 0 : 1;
        } catch (IllegalArgumentException e) {
            spec.commandLine().getErr().println("Ошибка: " + e.getMessage());
            return 2;
        }
    }
}
