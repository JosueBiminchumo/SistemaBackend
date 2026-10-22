package com.mediturno.backend.appointment.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AppointmentStatusTest {

    @Test
    void pendingCanBeConfirmedOrCancelledOnly() {
        assertTrue(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.CONFIRMED));
        assertTrue(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.CANCELLED));
        assertFalse(AppointmentStatus.PENDING.canTransitionTo(AppointmentStatus.COMPLETED));
    }

    @Test
    void confirmedCanBeCompletedOrCancelled() {
        assertTrue(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.COMPLETED));
        assertTrue(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.CANCELLED));
        assertFalse(AppointmentStatus.CONFIRMED.canTransitionTo(AppointmentStatus.PENDING));
    }

    @Test
    void finalStatesHaveNoTransitions() {
        assertTrue(AppointmentStatus.CANCELLED.allowedNext().isEmpty());
        assertTrue(AppointmentStatus.COMPLETED.allowedNext().isEmpty());
    }

    @Test
    void onlyCancelledReleasesTheSchedule() {
        assertFalse(AppointmentStatus.CANCELLED.blocksSchedule());
        assertTrue(AppointmentStatus.PENDING.blocksSchedule());
        assertTrue(AppointmentStatus.COMPLETED.blocksSchedule());
    }

    @Test
    void onlyPendingAndConfirmedAreOpen() {
        assertTrue(AppointmentStatus.PENDING.isOpen());
        assertTrue(AppointmentStatus.CONFIRMED.isOpen());
        assertFalse(AppointmentStatus.CANCELLED.isOpen());
        assertFalse(AppointmentStatus.COMPLETED.isOpen());
    }
}