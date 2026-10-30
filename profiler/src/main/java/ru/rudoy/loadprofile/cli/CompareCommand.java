package ru.rudoy.loadprofile.cli;

import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.rudoy.loadprofile.compare.ProfileComparator;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.storage.ProfileDatabase;

@Component
@Command(name = "compare", description = "Сравнивает два профиля: доли операций, интенсивность, паузы.")
class CompareCommand implements Callable<Integer> {

    @Option(names = "--db-url", required = true, description = "URL базы PostgreSQL (jdbc:postgresql://...).")
    private String dbUrl;

    @Option(names = "--db-user", defaultValue = "postgres", description = "Пользователь базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbUser;

    @Option(names = "--db-password", defaultValue = "postgres", description = "Пароль базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbPassword;

    @Option(names = "--first", required = true, description = "Идентификатор первого профиля.")
    private long first;

    @Option(names = "--second", required = true, description = "Идентификатор второго профиля.")
    private long second;

    @Override
    public Integer call() {
        System.setProperty("spring.main.banner-mode", "off");
        System.setProperty("spring.main.log-startup-info", "false");
        var repository = ProfileDatabase.open(dbUrl, dbUser, dbPassword);
        LoadProfile a = repository.findById(first)
                .orElseThrow(() -> new IllegalArgumentException("профиль не найден: id=" + first));
        LoadProfile b = repository.findById(second)
                .orElseThrow(() -> new IllegalArgumentException("профиль не найден: id=" + second));

        var comparison = new ProfileComparator().compare(a, b);
        System.out.printf("Профили: %s (id=%d) и %s (id=%d)%n", a.name(), a.id(), b.name(), b.id());
        System.out.println("Доли операций:");
        comparison.operations().forEach(row -> System.out.printf(
                "  %-45s %7.2f%%  %7.2f%%  %+7.2f%%%n", row.operation(),
                100 * row.firstShare(), 100 * row.secondShare(), 100 * row.difference()));
        System.out.printf("Интенсивность: %+.3f сессий/мин (%.3f → %.3f)%n",
                comparison.sessionsPerMinuteDifference(),
                a.arrivalRate().sessionsPerMinute(), b.arrivalRate().sessionsPerMinute());
        System.out.printf("Паузы: медиана %+d мс (%d → %d), p90 %+d мс (%d → %d)%n",
                comparison.thinkTime().medianMs(),
                a.thinkTime().medianMs(), b.thinkTime().medianMs(),
                comparison.thinkTime().p90Ms(),
                a.thinkTime().p90Ms(), b.thinkTime().p90Ms());
        return 0;
    }
}
