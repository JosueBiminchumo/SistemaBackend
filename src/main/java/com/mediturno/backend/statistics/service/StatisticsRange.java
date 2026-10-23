package com.mediturno.backend.statistics.service;

import com.mediturno.backend.statistics.exception.InvalidStatisticsQueryException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

public record StatisticsRange(LocalDate from, LocalDate to) {

    public static final int MAX_DAYS = 366;
    private static final LocalDate MIN_DATE = LocalDate.of(1000, 1, 1);
    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 30);

    public StatisticsRange {
        if (from == null || to == null) {
            throw new InvalidStatisticsQueryException("Debe indicar ambas fechas del rango.");
        }
        if (from.isBefore(MIN_DATE) || to.isAfter(MAX_DATE)) {
            throw new InvalidStatisticsQueryException("Las fechas estan fuera del rango admitido.");
        }
        if (from.isAfter(to)) {
            throw new InvalidStatisticsQueryException("La fecha from no puede ser posterior a to.");
        }
        if (ChronoUnit.DAYS.between(from, to) + 1 > MAX_DAYS) {
            throw new InvalidStatisticsQueryException("El rango no puede superar 366 dias.");
        }
    }

    public static StatisticsRange resolve(LocalDate from, LocalDate to, Clock clock) {
        if (to != null && (to.isBefore(MIN_DATE) || to.isAfter(MAX_DATE))) {
            throw new InvalidStatisticsQueryException("Las fechas estan fuera del rango admitido.");
        }
        LocalDate end = to == null ? LocalDate.now(clock.withZone(ZoneOffset.UTC)) : to;
        LocalDate start = from == null ? end.minusDays(29) : from;
        return new StatisticsRange(start, end);
    }

    public LocalDateTime startAt() {
        return from.atStartOfDay();
    }

    public LocalDateTime endBefore() {
        return to.plusDays(1).atStartOfDay();
    }
}
