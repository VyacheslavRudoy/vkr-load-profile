package ru.rudoy.loadprofile.cli;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import picocli.CommandLine.IVersionProvider;

/**
 * Версия приложения для параметра {@code --version}.
 * <p>
 * Значение берётся из {@code application.properties}, куда его подставляет
 * Maven из {@code pom.xml} при сборке.
 */
@Component
class ProfilerVersionProvider implements IVersionProvider {

    private final String version;

    ProfilerVersionProvider(@Value("${profiler.version}") String version) {
        this.version = version;
    }

    @Override
    public String[] getVersion() {
        return new String[] {"profiler " + version};
    }
}
