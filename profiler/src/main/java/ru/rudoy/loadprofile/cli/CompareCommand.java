package ru.rudoy.loadprofile.cli;

import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.storage.ProfileDatabase;

@Component
@Command(name = "compare", description = "Сравнивает два профиля по долям операций.")
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
        var repository = ProfileDatabase.open(dbUrl, dbUser, dbPassword);
        LoadProfile a = repository.findById(first)
                .orElseThrow(() -> new IllegalArgumentException("профиль не найден: id=" + first));
        LoadProfile b = repository.findById(second)
                .orElseThrow(() -> new IllegalArgumentException("профиль не найден: id=" + second));

        Map<String, Double> sharesA = sharesOf(a);
        Map<String, Double> sharesB = sharesOf(b);
        Map<String, Double> deltas = new TreeMap<>();
        for (String ref : sharesA.keySet()) {
            deltas.put(ref, sharesA.get(ref) - sharesB.getOrDefault(ref, 0.0));
        }
        for (String ref : sharesB.keySet()) {
            deltas.putIfAbsent(ref, -sharesB.get(ref));
        }
        deltas.entrySet().stream()
                .sorted((x, y) -> Double.compare(Math.abs(y.getValue()), Math.abs(x.getValue())))
                .forEach(entry -> System.out.printf("%-45s %7.2f%%  %7.2f%%  %+7.2f%%%n",
                        entry.getKey(),
                        100 * sharesA.getOrDefault(entry.getKey(), 0.0),
                        100 * sharesB.getOrDefault(entry.getKey(), 0.0),
                        100 * entry.getValue()));
        return 0;
    }

    private static Map<String, Double> sharesOf(LoadProfile profile) {
        Map<String, Double> shares = new TreeMap<>();
        profile.operations().forEach(op -> shares.put(op.ref(), op.share()));
        return shares;
    }
}
