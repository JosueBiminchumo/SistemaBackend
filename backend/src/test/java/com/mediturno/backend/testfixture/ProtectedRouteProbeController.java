package com.mediturno.backend.testfixture;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("test")
public class ProtectedRouteProbeController {

    @GetMapping("/api/v1/patients/probe")
    ResponseEntity<Void> patientRoute() {
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/admin/probe")
    ResponseEntity<Void> adminRoute() {
        return ResponseEntity.noContent().build();
    }
}
