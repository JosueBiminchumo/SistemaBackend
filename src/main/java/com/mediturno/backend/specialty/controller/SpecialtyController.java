package com.mediturno.backend.specialty.controller;

import com.mediturno.backend.specialty.dto.SpecialtyRequest;
import com.mediturno.backend.specialty.dto.SpecialtyResponse;
import com.mediturno.backend.specialty.service.SpecialtyService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
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
    public List<SpecialtyResponse> list(@RequestParam(required = false) Boolean active) {
        return specialtyService.list(active);
    }

    @GetMapping("/{id}")
    public SpecialtyResponse getById(@PathVariable Long id) {
        return specialtyService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SpecialtyResponse create(@Valid @RequestBody SpecialtyRequest request) {
        return specialtyService.create(request);
    }

    @PutMapping("/{id}")
    public SpecialtyResponse update(
            @PathVariable Long id,
            @Valid @RequestBody SpecialtyRequest request) {
        return specialtyService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public SpecialtyResponse deactivate(@PathVariable Long id) {
        return specialtyService.deactivate(id);
    }
}
