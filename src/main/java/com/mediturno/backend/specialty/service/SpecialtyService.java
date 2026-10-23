package com.mediturno.backend.specialty.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.exception.ConflictException;
import com.mediturno.backend.common.exception.NotFoundException;
import com.mediturno.backend.specialty.domain.Specialty;
import com.mediturno.backend.specialty.dto.SpecialtyRequest;
import com.mediturno.backend.specialty.dto.SpecialtyResponse;
import com.mediturno.backend.specialty.repository.SpecialtyRepository;

@Service
public class SpecialtyService {

    private final SpecialtyRepository repository;

    public SpecialtyService(SpecialtyRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<SpecialtyResponse> list(boolean includeInactive) {
        List<Specialty> items = includeInactive
                ? repository.findAllByOrderByNameAsc()
                : repository.findAllByActiveTrueOrderByNameAsc();
        return items.stream().map(SpecialtyResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public SpecialtyResponse get(Long id) {
        return SpecialtyResponse.from(find(id));
    }

    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        String name = normalize(request.name());
        if (repository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("Ya existe una especialidad con ese nombre.");
        }
        Specialty saved = repository.saveAndFlush(new Specialty(
                name,
                clean(request.description()),
                request.active() == null || request.active()));
        return SpecialtyResponse.from(saved);
    }

    @Transactional
    public SpecialtyResponse update(Long id, SpecialtyRequest request) {
        Specialty specialty = find(id);
        String name = normalize(request.name());
        if (repository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new ConflictException("Ya existe una especialidad con ese nombre.");
        }
        specialty.update(
                name,
                clean(request.description()),
                request.active() == null ? specialty.isActive() : request.active());
        return SpecialtyResponse.from(repository.saveAndFlush(specialty));
    }

    /** Baja logica: nunca se borra fisicamente para no romper medicos ni citas. */
    @Transactional
    public void deactivate(Long id) {
        Specialty specialty = find(id);
        specialty.deactivate();
        repository.saveAndFlush(specialty);
    }

    public Specialty findActive(Long id) {
        Specialty specialty = find(id);
        if (!specialty.isActive()) {
            throw new ConflictException("La especialidad está desactivada.");
        }
        return specialty;
    }

    private Specialty find(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("La especialidad no existe."));
    }

    private static String normalize(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
