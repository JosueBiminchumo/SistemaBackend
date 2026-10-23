package com.mediturno.backend.specialty.dto;

import java.time.Instant;

import com.mediturno.backend.specialty.domain.Specialty;

public record SpecialtyResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt) {

    public static SpecialtyResponse from(Specialty s) {
        return new SpecialtyResponse(s.getId(), s.getName(), s.getDescription(), s.isActive(), s.getCreatedAt());
    }
}
