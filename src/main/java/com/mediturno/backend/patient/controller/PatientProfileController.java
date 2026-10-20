package com.mediturno.backend.patient.controller;

import com.mediturno.backend.patient.dto.PatientProfileResponse;
import com.mediturno.backend.patient.dto.UpdatePatientProfileRequest;
import com.mediturno.backend.patient.service.PatientProfileService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patients/me")
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    public PatientProfileController(PatientProfileService patientProfileService) {
        this.patientProfileService = patientProfileService;
    }

    @GetMapping
    public PatientProfileResponse getProfile(@AuthenticationPrincipal UserDetails user) {
        return patientProfileService.getProfile(user.getUsername());
    }

    @PutMapping
    public PatientProfileResponse updateProfile(
            @AuthenticationPrincipal UserDetails user,
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        return patientProfileService.updateProfile(user.getUsername(), request);
    }
}
