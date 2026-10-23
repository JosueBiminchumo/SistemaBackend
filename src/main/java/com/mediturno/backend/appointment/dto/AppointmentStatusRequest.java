package com.mediturno.backend.appointment.dto;

import com.mediturno.backend.appointment.domain.AppointmentStatus;

import jakarta.validation.constraints.NotNull;

public record AppointmentStatusRequest(@NotNull AppointmentStatus status) {
}
