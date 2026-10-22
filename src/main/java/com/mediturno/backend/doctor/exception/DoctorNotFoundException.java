package com.mediturno.backend.doctor.exception;

public class DoctorNotFoundException extends RuntimeException {

    public DoctorNotFoundException(Long doctorId) {
        super("No existe un médico activo con el ID: " + doctorId);
    }
}