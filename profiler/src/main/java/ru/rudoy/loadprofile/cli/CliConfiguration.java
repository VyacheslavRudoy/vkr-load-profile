package ru.rudoy.loadprofile.cli;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Сборка объекта командной строки picocli.
 * <p>
 * Фабрика {@link IFactory} из picocli-spring-boot-starter создаёт команды
 * и вспомогательные объекты (например, поставщика версии) как бины Spring,
 * поэтому в команды можно внедрять зависимости обычным способом.
 */
@Configuration
class CliConfiguration {

    @Bean
    CommandLine commandLine(ProfilerCommand rootCommand, IFactory factory) {
        return new CommandLine(rootCommand, factory);
    }
}
