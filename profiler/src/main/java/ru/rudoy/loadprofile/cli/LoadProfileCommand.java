package ru.rudoy.loadprofile.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.LoadProfileJson;
import ru.rudoy.loadprofile.storage.ProfileDatabase;

@Component
@Command(name = "load-profile", description = "Загружает профиль из JSON-файла в базу.")
class LoadProfileCommand implements Callable<Integer> {

    @Option(names = "--db-url", required = true, description = "URL базы PostgreSQL (jdbc:postgresql://...).")
    private String dbUrl;

    @Option(names = "--db-user", defaultValue = "postgres", description = "Пользователь базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbUser;

    @Option(names = "--db-password", defaultValue = "postgres", description = "Пароль базы. По умолчанию: ${DEFAULT-VALUE}.")
    private String dbPassword;

    @Option(names = "--file", required = true, description = "Файл профиля в JSON.")
    private Path file;

    @Override
    public Integer call() throws Exception {
        LoadProfile profile = new LoadProfileJson().read(Files.readString(file));
        LoadProfile saved = ProfileDatabase.open(dbUrl, dbUser, dbPassword).save(profile);
        System.out.printf("Профиль загружен: id=%d, имя=%s%n", saved.id(), saved.name());
        return 0;
    }
}
