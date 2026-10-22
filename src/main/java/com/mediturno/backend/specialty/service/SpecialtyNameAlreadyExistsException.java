package com.mediturno.backend.specialty.service;

public class SpecialtyNameAlreadyExistsException extends RuntimeException {

    public SpecialtyNameAlreadyExistsException() {
        super("Ya existe una especialidad con ese nombre");
    }
}
