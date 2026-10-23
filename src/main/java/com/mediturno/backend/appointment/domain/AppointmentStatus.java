package com.mediturno.backend.appointment.domain;

import java.util.EnumSet;
import java.util.Set;

public enum AppointmentStatus {
    PENDING,
    CONFIRMED,
    CANCELLED,
    COMPLETED;

    /** Estados que ocupan el horario del medico. */
    public static final Set<AppointmentStatus> ACTIVE = EnumSet.of(PENDING, CONFIRMED);

    public boolean isActive() {
        return ACTIVE.contains(this);
    }

    public boolean canTransitionTo(AppointmentStatus next) {
        return switch (this) {
            case PENDING -> next == CONFIRMED || next == CANCELLED;
            case CONFIRMED -> next == COMPLETED || next == CANCELLED;
            case CANCELLED, COMPLETED -> false;
        };
    }
}
