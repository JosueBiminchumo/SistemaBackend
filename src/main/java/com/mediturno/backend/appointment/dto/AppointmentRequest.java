package com.mediturno.backend.appointment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/** No incluye patientId: el paciente sale del usuario autenticado. */
public record AppointmentRequest(
        @NotNull(message = "El medico es obligatorio") Long doctorId,
        @NotNull(message = "La fecha y hora de inicio son obligatorias") LocalDateTime startAt,
        @NotBlank(message = "El motivo es obligatorio")
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres") String reason) {
}