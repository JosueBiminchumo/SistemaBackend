package com.mediturno.backend.patient.controller;

import com.mediturno.backend.patient.dto.PatientProfileResponse;
import com.mediturno.backend.patient.dto.UpdatePatientProfileRequest;
import com.mediturno.backend.patient.service.PatientProfileService;
import com.mediturno.backend.user.domain.User;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public PatientProfileResponse getProfile(@AuthenticationPrincipal User user) {
        return patientProfileService.getProfile(user.getId());
    }

    @PutMapping
    public PatientProfileResponse updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UpdatePatientProfileRequest request) {
        return patientProfileService.updateProfile(user.getId(), request);
    }
}
