package com.mediturno.backend.specialty.mapper;

import com.mediturno.backend.specialty.dto.SpecialtyResponse;
import com.mediturno.backend.specialty.entity.Specialty;

public final class SpecialtyMapper {

    private SpecialtyMapper() {
    }

    public static SpecialtyResponse toResponse(Specialty specialty) {
        return new SpecialtyResponse(
                specialty.getId(),
                specialty.getName(),
                specialty.getDescription(),
                specialty.isActive(),
                specialty.getCreatedAt()
        );
    }
}