package com.mediturno.backend.patient.dto;

import java.time.LocalDate;

import com.mediturno.backend.patient.domain.Patient;

public record PatientProfileResponse(
        Long id,
        String firstName,
        String lastName,
        String email,
        String documentNumber,
        LocalDate birthDate,
        String phone) {

    public static PatientProfileResponse from(Patient patient) {
        return new PatientProfileResponse(
                patient.getUserId(),
                patient.getUser().getFirstName(),
                patient.getUser().getLastName(),
                patient.getUser().getEmail(),
                patient.getDocumentNumber(),
                patient.getBirthDate(),
                patient.getPhone());
    }
}
