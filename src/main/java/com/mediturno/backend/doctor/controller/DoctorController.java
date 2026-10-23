package com.mediturno.backend.doctor.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.mediturno.backend.doctor.dto.DoctorCreateRequest;
import com.mediturno.backend.doctor.dto.DoctorProfileUpdateRequest;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.dto.DoctorUpdateRequest;
import com.mediturno.backend.doctor.dto.ScheduleRuleResponse;
import com.mediturno.backend.doctor.dto.ScheduleRulesRequest;
import com.mediturno.backend.doctor.service.DoctorScheduleService;
import com.mediturno.backend.doctor.service.DoctorService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    private final DoctorService doctorService;
    private final DoctorScheduleService scheduleService;

    public DoctorController(DoctorService doctorService, DoctorScheduleService scheduleService) {
        this.doctorService = doctorService;
        this.scheduleService = scheduleService;
    }

    @GetMapping
    public List<DoctorResponse> list(
            @RequestParam(required = false) Long specialtyId,
            @RequestParam(required = false, name = "q") String search,
            @RequestParam(defaultValue = "false") boolean includeInactive,
            Authentication authentication) {
        return doctorService.list(specialtyId, search, isAdmin(authentication), includeInactive);
    }

    @GetMapping("/me")
    public DoctorResponse me(Authentication authentication) {
        return doctorService.getMe(authentication.getName());
    }

    @PutMapping("/me")
    public DoctorResponse updateMe(
            Authentication authentication,
            @Valid @RequestBody DoctorProfileUpdateRequest request) {
        return doctorService.updateMe(authentication.getName(), request);
    }

    @GetMapping("/{id}")
    public DoctorResponse get(@PathVariable Long id, Authentication authentication) {
        return doctorService.get(id, isAdmin(authentication));
    }

    @PostMapping
    public ResponseEntity<DoctorResponse> create(@Valid @RequestBody DoctorCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(doctorService.create(request));
    }

    @PutMapping("/{id}")
    public DoctorResponse update(@PathVariable Long id, @Valid @RequestBody DoctorUpdateRequest request) {
        return doctorService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivate(@PathVariable Long id) {
        doctorService.deactivate(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/schedule/rules")
    public List<ScheduleRuleResponse> rules(@PathVariable Long id) {
        return scheduleService.rules(id);
    }

    @PutMapping("/{id}/schedule/rules")
    public List<ScheduleRuleResponse> replaceRules(
            @PathVariable Long id,
            @Valid @RequestBody ScheduleRulesRequest request,
            Authentication authentication) {
        return scheduleService.replaceRules(id, request, authentication.getName(), isAdmin(authentication));
    }

    private static boolean isAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }
}
