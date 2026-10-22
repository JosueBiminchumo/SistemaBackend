package com.mediturno.backend.doctor.dto;

public record DoctorResponse(
        Long id,
        Long userId,
        Long specialtyId,
        String licenseNumber,
        String biography,
        boolean active
) {
}