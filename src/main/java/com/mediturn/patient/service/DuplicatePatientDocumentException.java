package com.mediturn.patient.service;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class DuplicatePatientDocumentException extends RuntimeException {

    public DuplicatePatientDocumentException() {
        super("El DNI ya está registrado");
    }
}
