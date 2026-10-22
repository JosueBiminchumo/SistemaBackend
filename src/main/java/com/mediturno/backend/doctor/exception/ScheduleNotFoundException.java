package com.mediturno.backend.doctor.exception;

public class ScheduleNotFoundException extends RuntimeException {

    public ScheduleNotFoundException(Long doctorId, Long scheduleId) {
        super(
                "No existe el horario " + scheduleId
                        + " para el médico con ID " + doctorId
        );
    }
}