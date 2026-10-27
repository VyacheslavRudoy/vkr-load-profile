package ru.rudoy.loadprofile.storage;

import org.flywaydb.core.Flyway;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

/** Подключение к базе профилей: разворачивает схему миграциями и даёт репозиторий. */
public final class ProfileDatabase {

    private ProfileDatabase() {
    }

    public static ProfileRepository open(String url, String user, String password) {
        Flyway.configure().dataSource(url, user, password).load().migrate();
        return new ProfileRepository(JdbcClient.create(
                new DriverManagerDataSource(url, user, password)));
    }
}
