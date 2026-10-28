package ru.rudoy.loadprofile.compare;

import java.util.List;

/** Расхождение двух профилей по трём показателям: доли операций, интенсивность, паузы. */
public record ProfileComparison(List<OperationDifference> operations,
                                 double sessionsPerMinuteDifference,
                                 ThinkTimeDifference thinkTime) {

    public ProfileComparison {
        operations = List.copyOf(operations);
    }

    /**
     * Расхождение по одной операции: доли в каждом профиле и их разность.
     * Отсутствующая операция считается долей 0.
     */
    public record OperationDifference(String operation, double firstShare,
                                      double secondShare, double difference) {
    }

    /** Разность статистик пауз, миллисекунды. */
    public record ThinkTimeDifference(long medianMs, long p90Ms) {
    }
}
