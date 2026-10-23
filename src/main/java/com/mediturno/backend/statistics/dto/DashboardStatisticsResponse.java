package com.mediturno.backend.statistics.dto;

import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record DashboardStatisticsResponse(
        LocalDate from,
        LocalDate to,
        String timeZone,
        long totalAppointments,
        long totalPatients,
        long totalDoctors,
        long totalSpecialties,
        Map<String, Long> appointmentsByStatus) {

    public DashboardStatisticsResponse {
        appointmentsByStatus = Collections.unmodifiableMap(new LinkedHashMap<>(appointmentsByStatus));
    }
}
