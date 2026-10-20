package com.mediturn.patient.controller;

import com.mediturn.patient.dto.PatientResponse;
import com.mediturn.patient.dto.PatientUpdateRequest;
import com.mediturn.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me")
public class PatientController {

    private final PatientService patientService;

    public PatientController(PatientService patientService) {
        this.patientService = patientService;
    }

    @GetMapping
    public PatientResponse getMyProfile() {
        return patientService.getMyProfile();
    }

    @PutMapping
    public PatientResponse updateMyProfile(@Valid @RequestBody PatientUpdateRequest request) {
        return patientService.updateMyProfile(request);
    }
}
