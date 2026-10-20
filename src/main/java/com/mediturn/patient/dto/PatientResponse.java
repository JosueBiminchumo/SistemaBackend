package com.mediturn.patient.dto;

import java.time.LocalDate;

public record PatientResponse(
        Long id,
        String documentNumber,
        LocalDate birthDate,
        String phone
) {
}
