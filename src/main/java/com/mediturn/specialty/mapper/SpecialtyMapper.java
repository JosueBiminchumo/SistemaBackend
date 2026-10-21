package com.mediturn.specialty.mapper;

import com.mediturn.specialty.dto.SpecialtyResponse;
import com.mediturn.specialty.entity.Specialty;

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