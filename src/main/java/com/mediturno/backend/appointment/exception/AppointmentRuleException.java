package com.mediturno.backend.appointment.exception;

public class AppointmentRuleException extends RuntimeException {            // 400
    public AppointmentRuleException(String message) { super(message); }
}