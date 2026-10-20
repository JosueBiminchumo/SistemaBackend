package com.mediturno.backend.patient.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;

public record UpdatePatientProfileRequest(
        @NotBlank @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String documentNumber,
        @NotNull @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual.") LocalDate birthDate,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos.") String phone) {
}
