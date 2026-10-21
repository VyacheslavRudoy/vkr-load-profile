package ru.rudoy.loadprofile.storage;

import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.simple.JdbcClient;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.LoadProfileJson;

/**
 * Сохранение и чтение профилей в PostgreSQL. Не бин Spring: база настраивается
 * при запуске утилиты, и экземпляр создаётся с уже готовым подключением.
 */
public final class ProfileRepository {

    private final JdbcClient jdbc;
    private final LoadProfileJson json = new LoadProfileJson();

    public ProfileRepository(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    /** Сохраняет профиль целиком и возвращает его же с назначенным базой идентификатором. */
    public LoadProfile save(LoadProfile profile) {
        Long id = jdbc.sql("insert into load_profiles (name, created_at, payload)"
                        + " values (:name, :createdAt, :payload::jsonb) returning id")
                .param("name", profile.name())
                .param("createdAt", profile.createdAt().atOffset(ZoneOffset.UTC))
                .param("payload", json.write(profile))
                .query(Long.class)
                .single();
        return withId(profile, id);
    }

    public Optional<LoadProfile> findById(long id) {
        return jdbc.sql("select payload from load_profiles where id = :id")
                .param("id", id)
                .query((rs, rowNum) -> withId(json.read(rs.getString("payload")), id))
                .optional();
    }

    /** Профили от старых к новым. */
    public List<LoadProfile> findAll() {
        return jdbc.sql("select id, payload from load_profiles order by created_at, id")
                .query((rs, rowNum) -> withId(json.read(rs.getString("payload")), rs.getLong("id")))
                .list();
    }

    private static LoadProfile withId(LoadProfile profile, Long id) {
        return new LoadProfile(id, profile.name(), profile.createdAt(), profile.sourceWindow(),
                profile.operations(), profile.sequences(), profile.arrivalRate(),
                profile.thinkTime(), profile.parameters());
    }
}
