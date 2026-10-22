package com.mediturno.backend.appointment.dto;

import com.mediturno.backend.appointment.domain.AppointmentStatus;
import java.time.LocalDate;

public record AppointmentSearchCriteria(
        Long doctorId, Long patientId, AppointmentStatus status, LocalDate from, LocalDate to) {
}