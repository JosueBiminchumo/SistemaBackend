package com.mediturno.backend.doctor.controller;

import com.mediturno.backend.doctor.dto.DoctorAvailabilityResponse;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.dto.DoctorScheduleResponse;
import com.mediturno.backend.doctor.dto.ScheduleRequest;
import com.mediturno.backend.doctor.service.DoctorService;
import com.mediturno.backend.doctor.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/doctors")
@RequiredArgsConstructor
public class DoctorController {

    private final DoctorService doctorService;
    private final ScheduleService scheduleService;

    @GetMapping
    public ResponseEntity<List<DoctorResponse>> findAll(
            @RequestParam(required = false) Long specialtyId
    ) {
        List<DoctorResponse> doctors =
                doctorService.findAllActive(specialtyId);

        return ResponseEntity.ok(doctors);
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorResponse> findById(
            @PathVariable Long doctorId
    ) {
        DoctorResponse doctor =
                doctorService.findActiveById(doctorId);

        return ResponseEntity.ok(doctor);
    }

    @GetMapping("/{doctorId}/schedule")
    public ResponseEntity<DoctorAvailabilityResponse> findAvailability(
            @PathVariable Long doctorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        DoctorAvailabilityResponse availability =
                scheduleService.findAvailability(doctorId, date);

        return ResponseEntity.ok(availability);
    }

    @GetMapping("/{doctorId}/schedule/recurring")
    public ResponseEntity<List<DoctorScheduleResponse>>
    findRecurringSchedule(
            @PathVariable Long doctorId
    ) {
        List<DoctorScheduleResponse> schedules =
                scheduleService.findRecurringSchedule(doctorId);

        return ResponseEntity.ok(schedules);
    }

    @PostMapping("/{doctorId}/schedule")
    public ResponseEntity<DoctorScheduleResponse> createSchedule(
            @PathVariable Long doctorId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        DoctorScheduleResponse response =
                scheduleService.create(doctorId, request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    @PutMapping("/{doctorId}/schedule/{scheduleId}")
    public ResponseEntity<DoctorScheduleResponse> updateSchedule(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId,
            @Valid @RequestBody ScheduleRequest request
    ) {
        DoctorScheduleResponse response =
                scheduleService.update(
                        doctorId,
                        scheduleId,
                        request
                );

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{doctorId}/schedule/{scheduleId}")
    public ResponseEntity<Void> deactivateSchedule(
            @PathVariable Long doctorId,
            @PathVariable Long scheduleId
    ) {
        scheduleService.deactivate(doctorId, scheduleId);

        return ResponseEntity.noContent().build();
    }
}