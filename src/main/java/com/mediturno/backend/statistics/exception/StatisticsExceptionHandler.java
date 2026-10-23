package com.mediturno.backend.statistics.exception;

import com.mediturno.backend.common.dto.ApiError;
import com.mediturno.backend.statistics.controller.StatisticsController;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = StatisticsController.class)
public class StatisticsExceptionHandler {

    @ExceptionHandler(InvalidStatisticsQueryException.class)
    public ResponseEntity<ApiError> handleInvalidQuery(
            InvalidStatisticsQueryException exception, HttpServletRequest request) {
        return ResponseEntity.badRequest().body(new ApiError(
                Instant.now(), HttpStatus.BAD_REQUEST.value(), "Bad Request",
                exception.getMessage(), request.getRequestURI(), Map.of()));
    }
}
