package com.mediturn.specialty.controller;

import com.mediturn.common.ApiResponse;
import com.mediturn.specialty.service.SpecialtyNameAlreadyExistsException;
import com.mediturn.specialty.service.SpecialtyNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.mediturn.specialty")
public class SpecialtyExceptionHandler {

    @ExceptionHandler(SpecialtyNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(SpecialtyNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }

    @ExceptionHandler(SpecialtyNameAlreadyExistsException.class)
    public ResponseEntity<ApiResponse<Void>> handleConflict(SpecialtyNameAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ApiResponse.failure(exception.getMessage(), null));
    }
}