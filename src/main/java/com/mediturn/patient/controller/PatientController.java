package com.mediturn.patient.controller;

import com.mediturn.common.ApiResponse;
import com.mediturn.patient.dto.PatientResponse;
import com.mediturn.patient.dto.PatientUpdateRequest;
import com.mediturn.patient.service.PatientService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    public ResponseEntity<ApiResponse<PatientResponse>> getMyProfile() {
        PatientResponse profile = patientService.getMyProfile();
        return ResponseEntity.ok(ApiResponse.success("Perfil de paciente obtenido", profile));
    }

    @PutMapping
    public ResponseEntity<ApiResponse<PatientResponse>> updateMyProfile(
            @Valid @RequestBody PatientUpdateRequest request) {
        PatientResponse profile = patientService.updateMyProfile(request);
        return ResponseEntity.status(HttpStatus.OK)
                .body(ApiResponse.success("Perfil de paciente actualizado", profile));
    }
}
