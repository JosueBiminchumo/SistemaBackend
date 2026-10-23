package com.mediturno.backend.appointment.repository;

import java.time.LocalDateTime;

public record BusyRange(LocalDateTime startAt, LocalDateTime endAt) {
}
