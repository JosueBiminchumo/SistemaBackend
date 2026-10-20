package com.mediturn.patient.mapper;

import com.mediturn.patient.dto.PatientResponse;
import com.mediturn.patient.entity.Patient;

public final class PatientMapper {

    private PatientMapper() {
    }

    public static PatientResponse toResponse(Patient patient) {
        return new PatientResponse(
                patient.getId(),
                patient.getDocumentNumber(),
                patient.getBirthDate(),
                patient.getPhone()
        );
    }
}
