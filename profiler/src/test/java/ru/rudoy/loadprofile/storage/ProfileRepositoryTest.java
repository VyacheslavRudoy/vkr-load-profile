package ru.rudoy.loadprofile.storage;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.ArrivalRate;
import ru.rudoy.loadprofile.model.IntervalRate;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.Operation;
import ru.rudoy.loadprofile.model.ThinkTimeStats;

@Testcontainers
class ProfileRepositoryTest {

    @Container
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    @Test
    void savesAndReadsProfileBack() {
        ProfileRepository repository = repository();
        LoadProfile profile = profile("stand-run-a");

        LoadProfile saved = repository.save(profile);
        assertThat(saved.id()).isNotNull();

        assertThat(repository.findById(saved.id())).contains(saved);
        assertThat(repository.findAll()).containsExactly(saved);

        LoadProfile second = repository.save(profile("stand-run-b"));
        assertThat(repository.findAll()).containsExactly(saved, second);
    }

    @Test
    void missingProfileIsEmpty() {
        assertThat(repository().findById(Long.MAX_VALUE)).isEmpty();
    }

    @Test
    void rejectsAlreadySavedProfile() {
        ProfileRepository repository = repository();
        LoadProfile saved = repository.save(profile("run-a"));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> repository.save(saved))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already saved");
    }

    private static ProfileRepository repository() {
        Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .load()
                .migrate();
        DriverManagerDataSource dataSource = new DriverManagerDataSource(
                POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
        return new ProfileRepository(JdbcClient.create(dataSource));
    }

    private static LoadProfile profile(String name) {
        Instant from = Instant.parse("2026-10-21T10:00:00Z");
        Instant to = Instant.parse("2026-10-21T11:00:00Z");
        return new LoadProfile(null, name, Instant.parse("2026-10-21T12:00:00Z"),
                new TimeWindow(from, to),
                List.of(new Operation("GET", "/api/products", 12, 0.55),
                        new Operation("POST", "/api/orders", 3, 0.14)),
                List.of(),
                new ArrivalRate(6.0, List.of(new IntervalRate(from, to, 6.0))),
                new ThinkTimeStats(2000, 2800, 1000, 3000),
                List.of());
    }
}
