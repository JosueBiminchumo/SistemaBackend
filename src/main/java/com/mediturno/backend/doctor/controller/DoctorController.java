package com.mediturno.backend.doctor.controller;

import com.mediturno.backend.doctor.dto.DoctorAvailabilityResponse;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.dto.DoctorScheduleResponse;
import com.mediturno.backend.doctor.service.DoctorService;
import com.mediturno.backend.doctor.service.ScheduleService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
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
        return ResponseEntity.ok(
                doctorService.findAllActive(specialtyId)
        );
    }

    @GetMapping("/{doctorId}")
    public ResponseEntity<DoctorResponse> findById(
            @PathVariable Long doctorId
    ) {
        return ResponseEntity.ok(
                doctorService.findActiveById(doctorId)
        );
    }

    @GetMapping("/{doctorId}/schedule")
    public ResponseEntity<DoctorAvailabilityResponse> findAvailability(
            @PathVariable Long doctorId,
            @RequestParam
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate date
    ) {
        return ResponseEntity.ok(
                scheduleService.findAvailability(doctorId, date)
        );
    }

    @GetMapping("/{doctorId}/schedule/recurring")
    public ResponseEntity<List<DoctorScheduleResponse>>
    findRecurringSchedule(@PathVariable Long doctorId) {
        return ResponseEntity.ok(
                scheduleService.findRecurringSchedule(doctorId)
        );
    }
}