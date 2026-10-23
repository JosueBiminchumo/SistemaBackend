package com.mediturno.backend.patient.exception;

public class DocumentNumberAlreadyRegisteredException extends RuntimeException {

    public DocumentNumberAlreadyRegisteredException() {
        super("El DNI ya está registrado.");
    }
}
