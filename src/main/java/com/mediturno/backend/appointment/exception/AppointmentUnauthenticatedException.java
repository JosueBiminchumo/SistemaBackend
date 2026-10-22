package com.mediturno.backend.appointment.exception;

public class AppointmentUnauthenticatedException extends RuntimeException { // 401
    public AppointmentUnauthenticatedException(String message) { super(message); }
}