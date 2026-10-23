package com.mediturno.backend.appointment.controller;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mediturno.backend.appointment.dto.AvailabilityResponse;
import com.mediturno.backend.appointment.service.AvailabilityService;

/** GET /api/v1/doctors/{id}/schedule?date=YYYY-MM-DD: franjas del dia con su disponibilidad. */
@RestController
@RequestMapping("/api/v1/doctors/{id}/schedule")
public class DoctorAvailabilityController {

    private final AvailabilityService availabilityService;

    public DoctorAvailabilityController(AvailabilityService availabilityService) {
        this.availabilityService = availabilityService;
    }

    @GetMapping
    public AvailabilityResponse schedule(
            @PathVariable Long id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return availabilityService.forDate(id, date);
    }
}
