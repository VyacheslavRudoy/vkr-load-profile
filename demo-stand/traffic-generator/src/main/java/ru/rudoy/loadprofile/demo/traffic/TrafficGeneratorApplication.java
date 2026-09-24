package ru.rudoy.loadprofile.demo.traffic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

/**
 * Точка входа генератора активности.
 * <p>
 * Утилита командной строки: поднимается контекст Spring, аргументы
 * передаются команде picocli, а код её завершения становится кодом
 * завершения процесса.
 */
@SpringBootApplication
public class TrafficGeneratorApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(TrafficGeneratorApplication.class, args);
        int exitCode = context.getBean(CommandLine.class).execute(args);
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
