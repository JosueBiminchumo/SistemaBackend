package com.mediturno.backend.statistics.dto;

import java.time.LocalDate;
import java.util.List;

public record DashboardStatisticsResponse(
        LocalDate from,
        LocalDate to,
        String timezone,
        long totalAppointments,
        long pendingAppointments,
        long confirmedAppointments,
        long cancelledAppointments,
        long completedAppointments,
        long totalPatients,
        long totalDoctors,
        long totalSpecialties,
        List<SeriesPoint> appointmentsByStatus) {
}
