package com.mediturno.backend.doctor.exception;

import java.time.LocalDate;

public class ScheduleNotAvailableException extends RuntimeException {

    public ScheduleNotAvailableException(Long doctorId, LocalDate date) {
        super(
            "El médico con ID " + doctorId +
            " no tiene horarios disponibles para la fecha " + date
        );
    }
}