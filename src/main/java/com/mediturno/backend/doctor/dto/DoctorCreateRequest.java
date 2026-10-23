package com.mediturno.backend.doctor.dto;

import com.mediturno.backend.auth.validation.BcryptCompatiblePassword;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorCreateRequest(
        @NotBlank @Size(max = 80) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 8, max = 72) @BcryptCompatiblePassword String password,
        @NotNull Long specialtyId,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Size(max = 2000) String biography) {
}
