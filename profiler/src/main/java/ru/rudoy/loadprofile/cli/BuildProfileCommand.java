package ru.rudoy.loadprofile.cli;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.rudoy.loadprofile.builder.ProfilePipeline;
import ru.rudoy.loadprofile.ingest.JaegerApiReader;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.session.SessionStrategyType;
import ru.rudoy.loadprofile.storage.ProfileDatabase;

@Component
@Command(name = "build-profile",
        description = "Строит профиль нагрузки по трассировкам Jaeger за окно времени и сохраняет в базу.")
class BuildProfileCommand implements Callable<Integer> {

    @Option(names = "--jaeger", defaultValue = "http://localhost:16686",
            description = "Адрес Jaeger. По умолчанию: ${DEFAULT-VALUE}.")
    private String jaeger;

    @Option(names = "--from", required = true, description = "Начало окна, ISO-8601 (2026-10-03T23:40:54Z).")
    private Instant from;

    @Option(names = "--to", required = true, description = "Конец окна, ISO-8601.")
    private Instant to;

    @Option(names = "--name", required = true, description = "Имя профиля.")
    private String name;

    @Option(names = "--strategy", defaultValue = "BY_SESSION_ID",
            description = "Стратегия сессий: BY_SESSION_ID или BY_CLIENT_TIMEOUT. По умолчанию: ${DEFAULT-VALUE}.")
    private SessionStrategyType strategy;

    @Option(names = "--top-sequences", defaultValue = "5",
            description = "Сколько последовательностей оставлять в профиле. По умолчанию: ${DEFAULT-VALUE}.")
    private int topSequences;

    @Option(names = "--db-url", required = true, description = "URL базы PostgreSQL (jdbc:postgresql://...).")
    private String dbUrl;

    @Option(names = "--db-user", defaultValue = "postgres", description = "Пользователь базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbUser;

    @Option(names = "--db-password", defaultValue = "postgres", description = "Пароль базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbPassword;

    private final ProfilePipeline pipeline;

    BuildProfileCommand(ProfilePipeline pipeline) {
        this.pipeline = pipeline;
    }

    @Override
    public Integer call() {
        TimeWindow window = new TimeWindow(from, to);
        List<Span> spans = new JaegerApiReader(jaeger).fetch(window);
        LoadProfile profile = pipeline.build(spans, name, strategy, topSequences, window);
        LoadProfile saved = ProfileDatabase.open(dbUrl, dbUser, dbPassword).save(profile);
        System.out.printf("Профиль сохранён: id=%d, имя=%s, операций=%d, последовательностей=%d%n",
                saved.id(), saved.name(), saved.operations().size(), saved.sequences().size());
        return 0;
    }
}
