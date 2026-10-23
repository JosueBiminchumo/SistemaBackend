package com.mediturno.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record AppointmentSeriesResponse(
        String groupBy,
        LocalDate from,
        LocalDate to,
        String timezone,
        long total,
        List<SeriesPoint> points) {
}
