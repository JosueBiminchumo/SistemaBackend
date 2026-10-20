package com.mediturno.backend.auth.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import com.mediturno.backend.auth.validation.BcryptCompatiblePassword;

public record RegisterRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Pattern(regexp = "\\d{8}", message = "El DNI debe tener 8 dígitos.") String documentNumber,
        @NotNull @Past(message = "La fecha de nacimiento debe ser anterior a la fecha actual.") LocalDate birthDate,
        @NotBlank @Pattern(regexp = "\\d{9}", message = "El celular debe tener 9 dígitos.") String phone,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @BcryptCompatiblePassword String password) {
}
