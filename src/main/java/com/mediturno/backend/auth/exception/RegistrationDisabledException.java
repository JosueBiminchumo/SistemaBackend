package com.mediturno.backend.auth.exception;

public class RegistrationDisabledException extends RuntimeException {

    public RegistrationDisabledException() {
        super("El registro público está deshabilitado.");
    }
}
