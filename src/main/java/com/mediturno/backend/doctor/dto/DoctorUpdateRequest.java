package com.mediturno.backend.doctor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record DoctorUpdateRequest(
        @NotNull Long specialtyId,
        @NotBlank @Size(max = 50) String licenseNumber,
        @Size(max = 2000) String biography,
        Boolean active) {
}
