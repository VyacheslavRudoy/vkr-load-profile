package ru.rudoy.loadprofile.builder;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;
import ru.rudoy.loadprofile.ingest.Span;
import ru.rudoy.loadprofile.ingest.TimeWindow;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.ParameterSpec;
import ru.rudoy.loadprofile.session.Session;
import ru.rudoy.loadprofile.session.SessionStrategies;
import ru.rudoy.loadprofile.session.SessionStrategyType;
import ru.rudoy.loadprofile.session.TraceAssembler;

/** Полный конвейер: спаны → сессии → профиль нагрузки. */
@Component
public class ProfilePipeline {

    public LoadProfile build(List<Span> spans, String name, SessionStrategyType strategyType,
                             int topSequences, TimeWindow window) {
        List<ru.rudoy.loadprofile.ingest.Trace> traces = new TraceAssembler().assemble(spans);
        List<Session> sessions = SessionStrategies.create(strategyType).group(traces);
        ParametersBuilder parameters = new ParametersBuilder();
        List<ParameterSpec> specs = new ArrayList<>(parameters.build(sessions));
        specs.addAll(parameters.buildFeeder(sessions));
        return new LoadProfile(null, name, Instant.now(), window,
                new OperationsBuilder().build(traces),
                new SequencesBuilder().build(sessions, topSequences),
                new ArrivalRateBuilder().build(sessions, window, Duration.ofMinutes(1)),
                new ThinkTimeStatsBuilder().build(sessions),
                specs);
    }
}
