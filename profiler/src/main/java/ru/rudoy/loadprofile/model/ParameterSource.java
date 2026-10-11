package ru.rudoy.loadprofile.model;

/** Откуда тест берёт значение параметра. */
public enum ParameterSource {
    /** Из ответа более раннего запроса этой же сессии. */
    CORRELATED,
    /** Из заранее подготовленного набора значений. */
    FEEDER,
    /** Фиксированное значение. */
    CONSTANT
}
