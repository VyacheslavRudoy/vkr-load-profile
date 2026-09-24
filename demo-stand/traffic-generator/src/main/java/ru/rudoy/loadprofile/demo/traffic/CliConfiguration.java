package ru.rudoy.loadprofile.demo.traffic;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Сборка объекта командной строки picocli.
 * <p>
 * Фабрика {@link IFactory} из picocli-spring-boot-starter создаёт команды
 * как бины Spring, поэтому в команду внедряется исполнитель нагрузки
 * обычным способом.
 */
@Configuration
class CliConfiguration {

    @Bean
    CommandLine commandLine(TrafficGeneratorCommand rootCommand, IFactory factory) {
        return new CommandLine(rootCommand, factory);
    }
}
