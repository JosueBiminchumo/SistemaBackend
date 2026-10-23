package com.mediturno.backend.auth.exception;

public class EmailAlreadyRegisteredException extends RuntimeException {

    public EmailAlreadyRegisteredException() {
        super("El correo electrónico ya está registrado.");
    }
}
