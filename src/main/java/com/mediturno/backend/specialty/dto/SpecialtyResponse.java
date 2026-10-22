package com.mediturno.backend.specialty.dto;

import java.time.Instant;

public record SpecialtyResponse(
        Long id,
        String name,
        String description,
        boolean active,
        Instant createdAt
) {
}