package com.mediturn.specialty.controller;

import com.mediturn.common.ApiResponse;
import com.mediturn.specialty.dto.SpecialtyRequest;
import com.mediturn.specialty.dto.SpecialtyResponse;
import com.mediturn.specialty.service.SpecialtyService;
import jakarta.validation.Valid;
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

import java.util.List;

@RestController
@RequestMapping("/api/v1/specialties")
public class SpecialtyController {

    private final SpecialtyService specialtyService;

    public SpecialtyController(SpecialtyService specialtyService) {
        this.specialtyService = specialtyService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<SpecialtyResponse>>> list(
            @RequestParam(required = false) Boolean active) {
        List<SpecialtyResponse> specialties = specialtyService.list(active);
        return ResponseEntity.ok(ApiResponse.success("Especialidades obtenidas", specialties));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<SpecialtyResponse>> create(
            @Valid @RequestBody SpecialtyRequest request) {
        SpecialtyResponse created = specialtyService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Especialidad creada", created));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialtyRequest request) {
        SpecialtyResponse updated = specialtyService.update(id, request);
        return ResponseEntity.ok(ApiResponse.success("Especialidad actualizada", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<SpecialtyResponse>> deactivate(@PathVariable Long id) {
        SpecialtyResponse deactivated = specialtyService.deactivate(id);
        return ResponseEntity.ok(ApiResponse.success("Especialidad desactivada", deactivated));
    }
}