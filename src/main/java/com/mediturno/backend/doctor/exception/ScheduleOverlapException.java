package com.mediturno.backend.doctor.exception;

public class ScheduleOverlapException extends RuntimeException {

    public ScheduleOverlapException() {
        super("El horario se superpone con otro horario activo del médico");
    }
}
