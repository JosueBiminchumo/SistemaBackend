package com.mediturno.backend.appointment.exception;

public class AppointmentAccessDeniedException extends RuntimeException {    // 403
    public AppointmentAccessDeniedException(String message) { super(message); }
}