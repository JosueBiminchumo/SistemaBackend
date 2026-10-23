package com.mediturno.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record AppointmentSeriesResponse(
        String groupBy,
        LocalDate from,
        LocalDate to,
        String timeZone,
        List<String> labels,
        List<Long> values) {

    public AppointmentSeriesResponse {
        labels = List.copyOf(labels);
        values = List.copyOf(values);
    }
}
