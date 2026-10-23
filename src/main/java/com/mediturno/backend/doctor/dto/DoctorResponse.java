package com.mediturno.backend.doctor.dto;

import com.mediturno.backend.doctor.domain.Doctor;

public record DoctorResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        Long specialtyId,
        String specialtyName,
        String licenseNumber,
        String biography,
        boolean active) {

    public static DoctorResponse from(Doctor d) {
        String first = d.getUser().getFirstName();
        String last = d.getUser().getLastName();
        return new DoctorResponse(
                d.getId(),
                first,
                last,
                first + " " + last,
                d.getSpecialty().getId(),
                d.getSpecialty().getName(),
                d.getLicenseNumber(),
                d.getBiography(),
                d.isActive());
    }
}
