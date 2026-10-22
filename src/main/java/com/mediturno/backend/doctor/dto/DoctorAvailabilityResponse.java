package com.mediturno.backend.doctor.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record DoctorAvailabilityResponse(
        Long doctorId,
        LocalDate date,
        boolean available,
        List<LocalTime> slots
) {
}