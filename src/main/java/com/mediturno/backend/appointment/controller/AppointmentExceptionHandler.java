package com.mediturno.backend.appointment.controller;

import com.mediturno.backend.appointment.exception.AppointmentAccessDeniedException;
import com.mediturno.backend.appointment.exception.AppointmentConflictException;
import com.mediturno.backend.appointment.exception.AppointmentNotFoundException;
import com.mediturno.backend.appointment.exception.AppointmentRuleException;
import com.mediturno.backend.appointment.exception.AppointmentUnauthenticatedException;
import com.mediturno.backend.common.dto.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice(basePackages = "com.mediturno.backend.appointment")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class AppointmentExceptionHandler {

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<ApiError> notFound(AppointmentNotFoundException ex, HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.NOT_FOUND, ex.getMessage(), request);
    }

    @ExceptionHandler(AppointmentConflictException.class)
    public ResponseEntity<ApiError> conflict(AppointmentConflictException ex, HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.CONFLICT, ex.getMessage(), request);
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    public ResponseEntity<ApiError> staleData(ObjectOptimisticLockingFailureException ex,
                                              HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.CONFLICT,
                "La cita fue modificada por otra persona. Recarga e intenta de nuevo", request);
    }

    @ExceptionHandler(AppointmentAccessDeniedException.class)
    public ResponseEntity<ApiError> forbidden(AppointmentAccessDeniedException ex, HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.FORBIDDEN, ex.getMessage(), request);
    }

    @ExceptionHandler(AppointmentUnauthenticatedException.class)
    public ResponseEntity<ApiError> unauthorized(AppointmentUnauthenticatedException ex,
                                                 HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.UNAUTHORIZED, ex.getMessage(), request);
    }

    @ExceptionHandler(AppointmentRuleException.class)
    public ResponseEntity<ApiError> invalidRule(AppointmentRuleException ex, HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiError> invalidParam(MethodArgumentTypeMismatchException ex,
                                                 HttpServletRequest request) {
        return AppointmentErrorFactory.build(HttpStatus.BAD_REQUEST,
                "Valor invalido para el parametro '" + ex.getName() + "'", request);
    }
}