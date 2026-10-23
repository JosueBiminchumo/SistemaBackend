package com.mediturno.backend.appointment.controller;

import com.mediturno.backend.appointment.dto.AppointmentStatusRequest;
import com.mediturno.backend.appointment.dto.AppointmentUpdateRequest;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PutMapping;
import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.appointment.dto.AppointmentPage;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.dto.AppointmentSearchCriteria;
import com.mediturno.backend.appointment.service.AppointmentService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    private final AppointmentService appointmentService;

    public AppointmentController(AppointmentService appointmentService) {
        this.appointmentService = appointmentService;
    }

    @PostMapping
    public ResponseEntity<AppointmentResponse> create(@Valid @RequestBody AppointmentRequest request) {
        AppointmentResponse created = appointmentService.create(request);
        return ResponseEntity.created(URI.create("/api/v1/appointments/" + created.id())).body(created);
    }

    @GetMapping
    public AppointmentPage list(
            @RequestParam(name = "doctorId", required = false) Long doctorId,
            @RequestParam(name = "patientId", required = false) Long patientId,
            @RequestParam(name = "status", required = false) AppointmentStatus status,
            @RequestParam(name = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(name = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(name = "page", defaultValue = "0") int page,
            @RequestParam(name = "size", defaultValue = "20") int size) {
        return appointmentService.list(
                new AppointmentSearchCriteria(doctorId, patientId, status, from, to), page, size);
    }

    @GetMapping("/{id}")
    public AppointmentResponse getById(@PathVariable("id") Long id) {
        return appointmentService.getById(id);
    }

        @PutMapping("/{id}")
    public AppointmentResponse reschedule(@PathVariable("id") Long id,
                                          @Valid @RequestBody AppointmentUpdateRequest request) {
        return appointmentService.reschedule(id, request);
    }

    @PatchMapping("/{id}/cancel")
    public AppointmentResponse cancel(@PathVariable("id") Long id) {
        return appointmentService.cancel(id);
    }

    @PatchMapping("/{id}/status")
    public AppointmentResponse changeStatus(@PathVariable("id") Long id,
                                            @Valid @RequestBody AppointmentStatusRequest request) {
        return appointmentService.changeStatus(id, request);
    }
}