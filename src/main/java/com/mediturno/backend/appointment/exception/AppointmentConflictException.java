package com.mediturno.backend.appointment.exception;

public class AppointmentConflictException extends RuntimeException {        // 409
    public AppointmentConflictException(String message) { super(message); }
}