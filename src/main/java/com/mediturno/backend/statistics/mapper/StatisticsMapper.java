package com.mediturno.backend.statistics.mapper;

import com.mediturno.backend.statistics.dto.AppointmentSeriesResponse;
import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.repository.StatisticsBucket;
import com.mediturno.backend.statistics.repository.StatisticsTotals;
import com.mediturno.backend.statistics.service.StatisticsGrouping;
import com.mediturno.backend.statistics.service.StatisticsRange;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class StatisticsMapper {

    public DashboardStatisticsResponse dashboard(
            StatisticsRange range, StatisticsTotals totals, List<StatisticsBucket> statuses) {
        return new DashboardStatisticsResponse(
                range.from(), range.to(), "UTC", totals.appointments(), totals.patients(),
                totals.doctors(), totals.specialties(), statusCounts(statuses));
    }

    public AppointmentSeriesResponse series(
            StatisticsGrouping grouping, StatisticsRange range, List<StatisticsBucket> rows) {
        Map<String, Long> counts = switch (grouping) {
            case DAY -> dailyCounts(range, rows);
            case STATUS -> statusCounts(rows);
            case SPECIALTY -> orderedCounts(rows);
        };
        return new AppointmentSeriesResponse(grouping.parameter(), range.from(), range.to(), "UTC",
                new ArrayList<>(counts.keySet()), new ArrayList<>(counts.values()));
    }

    private Map<String, Long> dailyCounts(StatisticsRange range, List<StatisticsBucket> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (LocalDate date = range.from(); !date.isAfter(range.to()); date = date.plusDays(1)) {
            counts.put(date.toString(), 0L);
        }
        rows.forEach(row -> counts.merge(row.label(), row.value(), Long::sum));
        return counts;
    }

    private Map<String, Long> statusCounts(List<StatisticsBucket> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        // Contrato compartido con AppointmentStatus del modulo de citas.
        for (String status : List.of("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED")) {
            counts.put(status, 0L);
        }
        rows.forEach(row -> counts.merge(row.label(), row.value(), Long::sum));
        return counts;
    }

    private Map<String, Long> orderedCounts(List<StatisticsBucket> rows) {
        Map<String, Long> counts = new LinkedHashMap<>();
        rows.forEach(row -> counts.merge(row.label(), row.value(), Long::sum));
        return counts;
    }
}
