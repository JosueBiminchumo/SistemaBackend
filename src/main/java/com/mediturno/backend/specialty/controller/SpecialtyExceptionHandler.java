package com.mediturno.backend.specialty.controller;

import com.mediturno.backend.common.dto.ApiError;
import com.mediturno.backend.specialty.service.SpecialtyNameAlreadyExistsException;
import com.mediturno.backend.specialty.service.SpecialtyNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice(basePackages = "com.mediturno.backend.specialty")
public class SpecialtyExceptionHandler {

    @ExceptionHandler(SpecialtyNotFoundException.class)
    public ResponseEntity<ApiError> handleNotFound(
            SpecialtyNotFoundException exception,
            HttpServletRequest request) {
        return error(HttpStatus.NOT_FOUND, exception.getMessage(), request);
    }

    @ExceptionHandler(SpecialtyNameAlreadyExistsException.class)
    public ResponseEntity<ApiError> handleConflict(
            SpecialtyNameAlreadyExistsException exception,
            HttpServletRequest request) {
        return error(HttpStatus.CONFLICT, exception.getMessage(), request);
    }

    private ResponseEntity<ApiError> error(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        ApiError body = new ApiError(
                Instant.now(),
                status.value(),
                status.getReasonPhrase(),
                message,
                request.getRequestURI(),
                Map.of());
        return ResponseEntity.status(status).body(body);
    }
}