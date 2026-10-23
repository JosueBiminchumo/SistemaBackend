package com.mediturno.backend.appointment.controller;

import com.mediturno.backend.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

final class AppointmentErrorFactory {

    private AppointmentErrorFactory() {
    }

    static ResponseEntity<ApiError> build(HttpStatus status, String message, HttpServletRequest request) {
        // AJUSTAR al constructor/metodo real de tu ApiError
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                null // Puedes agregar detalles adicionales si es necesario
        );
        return ResponseEntity.status(status).body(body);
    }
}