package com.mediturno.backend.appointment.dto;

import java.util.List;

public record AppointmentPage(
        List<AppointmentResponse> content, int page, int size, long totalElements, int totalPages) {
}