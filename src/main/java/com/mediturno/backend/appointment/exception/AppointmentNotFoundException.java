package com.mediturno.backend.appointment.exception;

public class AppointmentNotFoundException extends RuntimeException {        // 404
    public AppointmentNotFoundException(Long id) { super("Cita no encontrada: " + id); }
}