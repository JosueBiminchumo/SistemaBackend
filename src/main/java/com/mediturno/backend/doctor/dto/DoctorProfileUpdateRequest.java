package com.mediturno.backend.doctor.dto;

import jakarta.validation.constraints.Size;

public record DoctorProfileUpdateRequest(@Size(max = 2000) String biography) {
}
