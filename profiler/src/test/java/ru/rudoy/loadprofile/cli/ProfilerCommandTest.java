package ru.rudoy.loadprofile.cli;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import picocli.CommandLine;
import picocli.CommandLine.Help;

/**
 * Проверяет, что корневая команда собирается вместе с контекстом Spring
 * и отвечает на стандартные параметры справки и версии.
 */
@SpringBootTest
class ProfilerCommandTest {

    @Autowired
    private CommandLine commandLine;

    @Value("${profiler.version}")
    private String version;

    private StringWriter out;

    @BeforeEach
    void captureOutput() {
        out = new StringWriter();
        commandLine.setOut(new PrintWriter(out));
        // Без управляющих последовательностей ANSI вывод одинаков в любом окружении
        commandLine.setColorScheme(Help.defaultColorScheme(Help.Ansi.OFF));
    }

    @Test
    void withoutArgumentsPrintsUsage() {
        int exitCode = commandLine.execute();

        assertThat(exitCode).isZero();
        assertThat(out.toString()).startsWith("Использование: profiler");
    }

    @Test
    void helpOptionPrintsUsageWithLocalizedOptions() {
        int exitCode = commandLine.execute("--help");

        assertThat(exitCode).isZero();
        assertThat(out.toString())
                .startsWith("Использование: profiler")
                .contains("-h, --help")
                .contains("Показать эту справку и выйти.");
    }

    @Test
    void versionOptionPrintsProjectVersion() {
        int exitCode = commandLine.execute("--version");

        assertThat(exitCode).isZero();
        assertThat(out.toString().trim()).isEqualTo("profiler " + version);
    }
}
