package com.mediturno.backend.statistics.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.common.exception.BadRequestException;
import com.mediturno.backend.statistics.dto.AppointmentSeriesResponse;
import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.dto.SeriesPoint;
import com.mediturno.backend.statistics.repository.StatisticsRepository;

@Service
public class StatisticsService {

    private static final int MAX_RANGE_DAYS = 366;
    private static final int DEFAULT_RANGE_DAYS = 30;

    private final StatisticsRepository repository;
    private final Clock clock;
    private final ZoneId zone;

    public StatisticsService(StatisticsRepository repository, Clock clock, ZoneId zone) {
        this.repository = repository;
        this.clock = clock;
        this.zone = zone;
    }

    @Transactional(readOnly = true)
    public DashboardStatisticsResponse dashboard(LocalDate from, LocalDate to) {
        LocalDate[] range = resolveRange(from, to);
        Map<AppointmentStatus, Long> byStatus = countByStatus(range[0], range[1]);
        long total = byStatus.values().stream().mapToLong(Long::longValue).sum();

        List<SeriesPoint> points = byStatus.entrySet().stream()
                .map(e -> new SeriesPoint(e.getKey().name(), e.getValue()))
                .toList();

        return new DashboardStatisticsResponse(
                range[0],
                range[1],
                zone.getId(),
                total,
                byStatus.get(AppointmentStatus.PENDING),
                byStatus.get(AppointmentStatus.CONFIRMED),
                byStatus.get(AppointmentStatus.CANCELLED),
                byStatus.get(AppointmentStatus.COMPLETED),
                repository.countPatients(),
                repository.countActiveDoctors(),
                repository.countActiveSpecialties(),
                points);
    }

    @Transactional(readOnly = true)
    public AppointmentSeriesResponse appointments(String groupBy, LocalDate from, LocalDate to) {
        String mode = groupBy == null ? "day" : groupBy.toLowerCase(Locale.ROOT);
        LocalDate[] range = resolveRange(from, to);

        List<SeriesPoint> points = switch (mode) {
            case "status" -> countByStatus(range[0], range[1]).entrySet().stream()
                    .map(e -> new SeriesPoint(e.getKey().name(), e.getValue())).toList();
            case "specialty" -> repository
                    .countBySpecialty(range[0].atStartOfDay(), range[1].plusDays(1).atStartOfDay())
                    .stream()
                    .map(r -> new SeriesPoint((String) r[0], ((Number) r[1]).longValue()))
                    .toList();
            case "day" -> dailySeries(range[0], range[1]);
            default -> throw new BadRequestException("groupBy debe ser day, status o specialty.");
        };
        long total = points.stream().mapToLong(SeriesPoint::value).sum();
        return new AppointmentSeriesResponse(mode, range[0], range[1], zone.getId(), total, points);
    }

    /** Siempre devuelve todos los estados (con 0) para que las tarjetas y graficos sean estables. */
    private Map<AppointmentStatus, Long> countByStatus(LocalDate from, LocalDate to) {
        Map<AppointmentStatus, Long> result = new EnumMap<>(AppointmentStatus.class);
        for (AppointmentStatus s : AppointmentStatus.values()) {
            result.put(s, 0L);
        }
        for (Object[] row : repository.countByStatus(from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            result.put((AppointmentStatus) row[0], ((Number) row[1]).longValue());
        }
        return result;
    }

    /** Un punto por dia del rango, incluyendo los dias sin citas. */
    private List<SeriesPoint> dailySeries(LocalDate from, LocalDate to) {
        Map<LocalDate, Long> counts = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            counts.put(d, 0L);
        }
        for (Object[] row : repository.countByDay(from.atStartOfDay(), to.plusDays(1).atStartOfDay())) {
            LocalDate day = LocalDate.of(
                    ((Number) row[0]).intValue(), ((Number) row[1]).intValue(), ((Number) row[2]).intValue());
            counts.put(day, ((Number) row[3]).longValue());
        }
        List<SeriesPoint> points = new ArrayList<>();
        counts.forEach((day, n) -> points.add(new SeriesPoint(day.toString(), n)));
        return points;
    }

    private LocalDate[] resolveRange(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock.withZone(zone));
        LocalDate end = to != null ? to : today;
        LocalDate start = from != null ? from : end.minusDays(DEFAULT_RANGE_DAYS - 1L);
        if (start.isAfter(end)) {
            throw new BadRequestException("La fecha 'from' no puede ser posterior a 'to'.");
        }
        if (ChronoUnit.DAYS.between(start, end) >= MAX_RANGE_DAYS) {
            throw new BadRequestException("El rango máximo es de " + MAX_RANGE_DAYS + " días.");
        }
        return new LocalDate[] { start, end };
    }
}
