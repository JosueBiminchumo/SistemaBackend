package com.mediturno.backend.appointment.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AvailabilityResponse(
        Long doctorId,
        LocalDate date,
        String timezone,
        List<Slot> slots) {

    public record Slot(LocalDateTime startAt, LocalDateTime endAt, boolean available) {
    }
}
