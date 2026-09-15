package ru.rudoy.loadprofile.cli;

import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Spec;

/**
 * Корневая команда {@code profiler}.
 * <p>
 * Сама по себе ничего не делает: запуск без подкоманды печатает справку.
 */
@Component
@Command(
        name = "profiler",
        mixinStandardHelpOptions = true,
        versionProvider = ProfilerVersionProvider.class,
        resourceBundle = "ru.rudoy.loadprofile.cli.messages",
        description = "Строит профиль нагрузки по данным распределённой трассировки "
                + "и генерирует план теста Apache JMeter.")
public class ProfilerCommand implements Runnable {

    @Spec
    private CommandSpec spec;

    @Override
    public void run() {
        spec.commandLine().usage(spec.commandLine().getOut());
    }
}
