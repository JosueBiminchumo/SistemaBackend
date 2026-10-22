package com.mediturno.backend.appointment.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

public record AppointmentUpdateRequest(
        @NotNull(message = "La nueva fecha y hora son obligatorias") LocalDateTime startAt,
        @Size(max = 255, message = "El motivo no puede superar 255 caracteres") String reason) {
}