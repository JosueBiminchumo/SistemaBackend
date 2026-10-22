package com.mediturno.backend.appointment.dto;

import com.mediturno.backend.appointment.domain.AppointmentStatus;
import java.time.LocalDateTime;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        String specialtyName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        AppointmentStatus status,
        String reason,
        LocalDateTime createdAt) {
}