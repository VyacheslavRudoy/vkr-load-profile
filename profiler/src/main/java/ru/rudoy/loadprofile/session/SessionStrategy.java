package ru.rudoy.loadprofile.session;

import java.util.List;
import ru.rudoy.loadprofile.ingest.Trace;

/** Способ группировки трасс в сессии. */
public interface SessionStrategy {

    List<Session> group(List<Trace> traces);
}
