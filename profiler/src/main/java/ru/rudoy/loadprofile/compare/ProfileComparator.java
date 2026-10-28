package ru.rudoy.loadprofile.compare;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import ru.rudoy.loadprofile.model.LoadProfile;
import ru.rudoy.loadprofile.model.Operation;

/** Считает расхождение двух профилей по долям операций, интенсивности и паузам. */
public final class ProfileComparator {

    public ProfileComparison compare(LoadProfile first, LoadProfile second) {
        return new ProfileComparison(
                operationDifferences(first, second),
                first.arrivalRate().sessionsPerMinute() - second.arrivalRate().sessionsPerMinute(),
                new ProfileComparison.ThinkTimeDifference(
                        first.thinkTime().medianMs() - second.thinkTime().medianMs(),
                        first.thinkTime().p90Ms() - second.thinkTime().p90Ms()));
    }

    private static List<ProfileComparison.OperationDifference> operationDifferences(
            LoadProfile first, LoadProfile second) {
        Map<String, Double> firstShares = sharesOf(first);
        Map<String, Double> secondShares = sharesOf(second);
        Map<String, Double> differences = new TreeMap<>();
        firstShares.forEach((ref, share) -> differences.merge(ref, share, Double::sum));
        secondShares.forEach((ref, share) -> differences.merge(ref, -share, Double::sum));

        List<ProfileComparison.OperationDifference> rows = new ArrayList<>();
        for (var entry : differences.entrySet()) {
            String ref = entry.getKey();
            rows.add(new ProfileComparison.OperationDifference(ref,
                    firstShares.getOrDefault(ref, 0.0),
                    secondShares.getOrDefault(ref, 0.0),
                    firstShares.getOrDefault(ref, 0.0) - secondShares.getOrDefault(ref, 0.0)));
        }
        // большие расхождения выше; при равных — по алфавиту, чтобы результат был устойчив
        rows.sort(java.util.Comparator
                .comparingDouble((ProfileComparison.OperationDifference row) -> Math.abs(row.difference()))
                .reversed()
                .thenComparing(ProfileComparison.OperationDifference::operation));
        return rows;
    }

    private static Map<String, Double> sharesOf(LoadProfile profile) {
        Map<String, Double> shares = new TreeMap<>();
        for (Operation operation : profile.operations()) {
            shares.put(operation.ref(), operation.share());
        }
        return shares;
    }
}
