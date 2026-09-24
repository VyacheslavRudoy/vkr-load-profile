package ru.rudoy.loadprofile.demo.traffic;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Исполнитель нагрузки: запускает сессии с заданной интенсивностью.
 * <p>
 * Расписание считает один поток: он начинает новую сессию каждые
 * {@code 60 / sessionsPerMinute} секунд, пока не истечёт длительность
 * прогона. Каждая сессия выполняется в собственном виртуальном потоке,
 * поэтому паузы на время обдумывания просто усыпляют поток и не мешают
 * другим сессиям. Датчик случайных чисел каждой сессии инициализируется
 * из зерна прогона и номера сессии, поэтому состав сессий не зависит от
 * перемешивания потоков и повторяется при том же зерне.
 */
@Component
class LoadDriver {

    private static final Logger log = LoggerFactory.getLogger(LoadDriver.class);

    private final ReferenceProfile profile;

    LoadDriver(ReferenceProfile profile) {
        this.profile = profile;
    }

    /** Параметры одного прогона. */
    record RunOptions(String catalogUrl, String ordersUrl, int sessionsPerMinute,
                      Duration duration, long seed) {

        RunOptions {
            if (sessionsPerMinute < 1 || sessionsPerMinute > 6000) {
                throw new IllegalArgumentException(
                        "sessions per minute must be between 1 and 6000: " + sessionsPerMinute);
            }
            if (duration.isZero() || duration.isNegative()) {
                throw new IllegalArgumentException("duration must be positive: " + duration);
            }
        }
    }

    /** Итог прогона. */
    record RunResult(int started, int completed, int failed) {

        boolean successful() {
            return failed == 0;
        }
    }

    RunResult run(RunOptions options) throws InterruptedException {
        URI catalogBase = URI.create(options.catalogUrl());
        URI ordersBase = URI.create(options.ordersUrl());
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
        ObjectMapper json = new ObjectMapper();
        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
        List<Thread> sessions = new ArrayList<>();
        AtomicInteger completed = new AtomicInteger();
        AtomicInteger failed = new AtomicInteger();
        AtomicInteger nextSessionNumber = new AtomicInteger();

        long periodMs = Math.round(60_000.0 / options.sessionsPerMinute());
        scheduler.scheduleAtFixedRate(() -> {
            int number = nextSessionNumber.incrementAndGet();
            Thread session = Thread.ofVirtual().name("session-" + number)
                    .start(() -> playSession(options, http, json, catalogBase, ordersBase,
                            number, completed, failed));
            sessions.add(session);
        }, 0, periodMs, TimeUnit.MILLISECONDS);

        log.info("generating load for {} s at {} sessions per minute (seed {})",
                options.duration().toSeconds(), options.sessionsPerMinute(), options.seed());
        Thread.sleep(options.duration().toMillis());
        scheduler.shutdown();
        scheduler.awaitTermination(10, TimeUnit.SECONDS);
        for (Thread session : sessions) {
            session.join();
        }

        RunResult result = new RunResult(sessions.size(), completed.get(), failed.get());
        log.info("run finished: {} sessions started, {} completed, {} failed",
                result.started(), result.completed(), result.failed());
        return result;
    }

    private void playSession(RunOptions options, HttpClient http, ObjectMapper json,
                             URI catalogBase, URI ordersBase, int number,
                             AtomicInteger completed, AtomicInteger failed) {
        String sessionId = UUID.randomUUID().toString();
        Random random = new Random(options.seed() * 1_000_003L + number);
        ReferenceProfile.WeightedScenario scenario = profile.chooseScenario(random);
        StandClient stand = new StandClient(http, sessionId, catalogBase, ordersBase,
                json, profile.thinkTime());
        long startedAt = System.nanoTime();
        try {
            scenario.script().play(stand, random);
            log.info("session {} played {} in {} ms", sessionId, scenario.name(),
                    (System.nanoTime() - startedAt) / 1_000_000);
            completed.incrementAndGet();
        } catch (Exception e) {
            log.warn("session {} failed in scenario {}", sessionId, scenario.name(), e);
            failed.incrementAndGet();
        }
    }
}
