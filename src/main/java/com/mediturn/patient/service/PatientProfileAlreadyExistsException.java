package com.mediturn.patient.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class PatientProfileAlreadyExistsException extends RuntimeException {

    public PatientProfileAlreadyExistsException() {
        super("El usuario ya tiene un perfil de paciente");
    }
}
