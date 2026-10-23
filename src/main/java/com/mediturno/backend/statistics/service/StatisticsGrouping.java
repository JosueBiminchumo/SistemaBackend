package com.mediturno.backend.statistics.service;

import com.mediturno.backend.statistics.exception.InvalidStatisticsQueryException;
import java.util.Locale;

public enum StatisticsGrouping {
    DAY, STATUS, SPECIALTY;

    public static StatisticsGrouping parse(String value) {
        if (value != null) {
            for (StatisticsGrouping grouping : values()) {
                if (grouping.name().equalsIgnoreCase(value)) {
                    return grouping;
                }
            }
        }
        throw new InvalidStatisticsQueryException("groupBy debe ser day, status o specialty.");
    }

    public String parameter() {
        return name().toLowerCase(Locale.ROOT);
    }
}
