package ru.rudoy.loadprofile;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import picocli.CommandLine;

/**
 * Точка входа приложения.
 * <p>
 * Приложение работает как утилита командной строки: поднимается контекст Spring,
 * аргументы передаются корневой команде picocli, а код её завершения становится
 * кодом завершения процесса.
 */
@SpringBootApplication
public class ProfilerApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(ProfilerApplication.class, args);
        int exitCode = context.getBean(CommandLine.class).execute(args);
        System.exit(SpringApplication.exit(context, () -> exitCode));
    }
}
