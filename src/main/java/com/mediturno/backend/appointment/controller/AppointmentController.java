package com.mediturno.backend.appointment.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.dto.AppointmentStatusRequest;
import com.mediturno.backend.appointment.service.AppointmentService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService service;

    public AppointmentController(AppointmentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(
            Authentication authentication, @Valid @RequestBody AppointmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(authentication.getName(), request));
    }

    @GetMapping
    public List<AppointmentResponse> list(
            Authentication authentication,
            @RequestParam(required = false) AppointmentStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) Long doctorId) {
        return service.list(authentication.getName(), status, from, to, doctorId);
    }

    @GetMapping("/{id}")
    public AppointmentResponse get(@PathVariable Long id, Authentication authentication) {
        return service.get(id, authentication.getName());
    }

    @PutMapping("/{id}")
    public AppointmentResponse reschedule(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AppointmentRequest request) {
        return service.reschedule(id, authentication.getName(), request);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable Long id, Authentication authentication) {
        return service.cancel(id, authentication.getName());
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse updateStatus(
            @PathVariable Long id,
            Authentication authentication,
            @Valid @RequestBody AppointmentStatusRequest request) {
        return service.updateStatus(id, authentication.getName(), request.status());
    }
}
