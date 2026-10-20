package com.mediturn.common;

import com.mediturn.patient.service.DuplicatePatientDocumentException;
import com.mediturn.patient.service.PatientNotFoundException;
import com.mediturn.patient.service.PatientProfileAlreadyExistsException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handlePatientNotFound(PatientNotFoundException exception) {
        return failure(HttpStatus.NOT_FOUND, exception.getMessage(), null);
    }

    @ExceptionHandler({DuplicatePatientDocumentException.class, PatientProfileAlreadyExistsException.class})
    public ResponseEntity<ApiResponse<Void>> handleConflict(RuntimeException exception) {
        return failure(HttpStatus.CONFLICT, exception.getMessage(), null);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Map<String, String>>> handleValidation(
            MethodArgumentNotValidException exception) {
        Map<String, String> errors = new LinkedHashMap<>();
        for (FieldError fieldError : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(fieldError.getField(), fieldError.getDefaultMessage());
        }
        return failure(HttpStatus.BAD_REQUEST, "La solicitud contiene datos inválidos", errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnreadableRequest(HttpMessageNotReadableException exception) {
        return failure(HttpStatus.BAD_REQUEST, "El cuerpo o formato de la solicitud no es válido", null);
    }

    private <T> ResponseEntity<ApiResponse<T>> failure(HttpStatus status, String message, T data) {
        return ResponseEntity.status(status).body(ApiResponse.failure(message, data));
    }
}
