package com.mediturno.backend.doctor.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record ScheduleRequest(

        @NotNull(message = "El día de la semana es obligatorio")
        DayOfWeek dayOfWeek,

        @NotNull(message = "La hora de inicio es obligatoria")
        LocalTime startTime,

        @NotNull(message = "La hora de fin es obligatoria")
        LocalTime endTime,

        @NotNull(message = "La duración del intervalo es obligatoria")
        @Min(value = 5, message = "El intervalo mínimo es de 5 minutos")
        @Max(value = 240, message = "El intervalo máximo es de 240 minutos")
        Integer slotMinutes
) {

    @AssertTrue(message = "La hora de inicio debe ser anterior a la hora de fin")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return true;
        }

        return startTime.isBefore(endTime);
    }
}