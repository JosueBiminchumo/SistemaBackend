package com.mediturno.backend.appointment.domain;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    PENDING, CONFIRMED, CANCELLED, COMPLETED;

    public Set<AppointmentStatus> allowedNext() {
        return switch (this) {
            case PENDING -> EnumSet.of(CONFIRMED, CANCELLED);
            case CONFIRMED -> EnumSet.of(COMPLETED, CANCELLED);
            case CANCELLED, COMPLETED -> EnumSet.noneOf(AppointmentStatus.class);
        };
    }

    public boolean canTransitionTo(AppointmentStatus target) {
        return allowedNext().contains(target);
    }

    /** Una cita cancelada libera el horario; las demas lo ocupan. */
    public boolean blocksSchedule() {
        return this != CANCELLED;
    }

    /** Solo las citas abiertas se pueden reprogramar. */
    public boolean isOpen() {
        return this == PENDING || this == CONFIRMED;
    }
}