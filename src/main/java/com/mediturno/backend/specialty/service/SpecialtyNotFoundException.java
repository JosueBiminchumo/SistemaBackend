package com.mediturno.backend.specialty.service;

public class SpecialtyNotFoundException extends RuntimeException {

    public SpecialtyNotFoundException() {
        super("La especialidad no existe");
    }
}