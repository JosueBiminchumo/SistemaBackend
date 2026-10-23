package com.mediturno.backend.appointment.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * startAt es fecha/hora local de la zona del negocio (America/Lima), ej. 2026-11-03T09:30:00.
 * El paciente se toma siempre del JWT, nunca del body.
 */
public record AppointmentRequest(
        @NotNull Long doctorId,
        @NotNull LocalDateTime startAt,
        @NotBlank @Size(max = 255) String reason) {
}
