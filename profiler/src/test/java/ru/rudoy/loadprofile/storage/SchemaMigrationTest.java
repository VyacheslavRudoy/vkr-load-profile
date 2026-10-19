package ru.rudoy.loadprofile.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/** Схема разворачивается миграцией с нуля и пригодна к использованию. */
@Testcontainers
class SchemaMigrationTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void schemaDeploysFromScratchAndHoldsRows() throws Exception {
        Flyway flyway = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load();

        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();

        try (Connection connection = DriverManager.getConnection(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())) {
            try (PreparedStatement insert = connection.prepareStatement(
                    "insert into load_profiles (name, created_at, payload) values (?, ?, ?::jsonb) returning id")) {
                insert.setString(1, "stand-run-a");
                insert.setObject(2, java.time.OffsetDateTime.parse("2026-10-18T12:00:00Z"));
                insert.setString(3, "{\"operations\": []}");
                ResultSet keys = insert.executeQuery();
                assertThat(keys.next()).isTrue();
                assertThat(keys.getLong(1)).isPositive();
            }
            try (Statement statement = connection.createStatement();
                 ResultSet rows = statement.executeQuery("select count(*) from load_profiles")) {
                assertThat(rows.next()).isTrue();
                assertThat(rows.getInt(1)).isEqualTo(1);
            }
        }
    }
}
