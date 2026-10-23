package com.mediturno.backend.patient.exception;

public class PatientProfileNotFoundException extends RuntimeException {

    public PatientProfileNotFoundException() {
        super("No se encontró el perfil del paciente.");
    }
}
