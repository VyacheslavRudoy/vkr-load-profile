package ru.rudoy.loadprofile.cli;

import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.rudoy.loadprofile.generator.JmeterPlan;
import ru.rudoy.loadprofile.generator.JmeterPlans;
import ru.rudoy.loadprofile.generator.PlanFiles;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.storage.ProfileDatabase;

@Component
@Command(name = "generate", description = "Генерирует план теста JMeter из сохранённого профиля.")
class GenerateCommand implements Callable<Integer> {

    @Option(names = "--db-url", required = true, description = "URL базы PostgreSQL (jdbc:postgresql://...).")
    private String dbUrl;

    @Option(names = "--db-user", defaultValue = "postgres", description = "Пользователь базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbUser;

    @Option(names = "--db-password", defaultValue = "postgres", description = "Пароль базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbPassword;

    @Option(names = "--profile-id", required = true, description = "Идентификатор профиля.")
    private long profileId;

    @Option(names = "--host", defaultValue = "localhost", description = "Адрес тестируемой системы. По умолчанию: ${DEFAULT-VALUE}.")
    private String host;

    @Option(names = "--port", defaultValue = "8080", description = "Порт тестируемой системы. По умолчанию: ${DEFAULT-VALUE}.")
    private int port;

    @Option(names = "--out", defaultValue = ".", description = "Каталог для плана и CSV-файлов. По умолчанию: текущий.")
    private Path out;

    @Override
    public Integer call() throws Exception {
        LoadProfile profile = ProfileDatabase.open(dbUrl, dbUser, dbPassword)
                .findById(profileId)
                .orElseThrow(() -> new IllegalArgumentException("профиль не найден: id=" + profileId));
        JmeterPlan plan = JmeterPlans.from(profile, host, port);
        var files = new PlanFiles().write(plan, out);
        System.out.printf("Записано файлов: %d, план: %s%n", files.size(),
                files.get(0).toAbsolutePath());
        return 0;
    }
}
