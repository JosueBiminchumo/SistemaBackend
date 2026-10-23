package com.mediturno.backend.statistics.service;

import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.dto.AppointmentSeriesResponse;
import com.mediturno.backend.statistics.mapper.StatisticsMapper;
import com.mediturno.backend.statistics.repository.StatisticsRepository;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
public class StatisticsService {

    private final StatisticsRepository repository;
    private final StatisticsMapper mapper;
    private final Clock clock;

    public StatisticsService(StatisticsRepository repository, StatisticsMapper mapper, Clock clock) {
        this.repository = repository;
        this.mapper = mapper;
        this.clock = clock;
    }

    public DashboardStatisticsResponse dashboard(LocalDate from, LocalDate to) {
        StatisticsRange range = StatisticsRange.resolve(from, to, clock);
        return mapper.dashboard(range, repository.findTotals(range), repository.countByStatus(range));
    }

    public AppointmentSeriesResponse appointments(String groupBy, LocalDate from, LocalDate to) {
        StatisticsGrouping grouping = StatisticsGrouping.parse(groupBy);
        StatisticsRange range = StatisticsRange.resolve(from, to, clock);
        var rows = switch (grouping) {
            case DAY -> repository.countByDay(range);
            case STATUS -> repository.countByStatus(range);
            case SPECIALTY -> repository.countBySpecialty(range);
        };
        return mapper.series(grouping, range, rows);
    }
}
