package com.mediturno.backend.specialty.service;

import com.mediturno.backend.specialty.dto.SpecialtyRequest;
import com.mediturno.backend.specialty.dto.SpecialtyResponse;
import com.mediturno.backend.specialty.entity.Specialty;
import com.mediturno.backend.specialty.mapper.SpecialtyMapper;
import com.mediturno.backend.specialty.repository.SpecialtyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class SpecialtyService {

    private final SpecialtyRepository specialtyRepository;

    public SpecialtyService(SpecialtyRepository specialtyRepository) {
        this.specialtyRepository = specialtyRepository;
    }

    public List<SpecialtyResponse> list(Boolean active) {
        List<Specialty> specialties = (active == null)
                ? specialtyRepository.findAllByOrderByNameAsc()
                : specialtyRepository.findByActiveOrderByNameAsc(active);
        return specialties.stream().map(SpecialtyMapper::toResponse).toList();
    }

    public SpecialtyResponse getById(Long id) {
        return SpecialtyMapper.toResponse(findById(id));
    }

    @Transactional
    public SpecialtyResponse create(SpecialtyRequest request) {
        String name = normalize(request.name());
        if (specialtyRepository.existsByNameIgnoreCase(name)) {
            throw new SpecialtyNameAlreadyExistsException();
        }

        boolean active = request.active() == null || request.active();
        Specialty specialty = new Specialty(name, cleanDescription(request.description()), active);
        return SpecialtyMapper.toResponse(specialtyRepository.save(specialty));
    }

    @Transactional
    public SpecialtyResponse update(Long id, SpecialtyRequest request) {
        Specialty specialty = findById(id);
        String name = normalize(request.name());

        if (specialtyRepository.existsByNameIgnoreCaseAndIdNot(name, id)) {
            throw new SpecialtyNameAlreadyExistsException();
        }

        specialty.update(name, cleanDescription(request.description()));
        if (request.active() != null) {
            if (request.active()) {
                specialty.activate();
            } else {
                specialty.deactivate();
            }
        }
        return SpecialtyMapper.toResponse(specialtyRepository.save(specialty));
    }

    /** Baja lógica: nunca se borra la fila para no romper médicos ni citas. */
    @Transactional
    public SpecialtyResponse deactivate(Long id) {
        Specialty specialty = findById(id);
        specialty.deactivate();
        return SpecialtyMapper.toResponse(specialtyRepository.save(specialty));
    }

    private Specialty findById(Long id) {
        return specialtyRepository.findById(id).orElseThrow(SpecialtyNotFoundException::new);
    }

    private String normalize(String name) {
        return name.trim().replaceAll("\\s+", " ");
    }

    private String cleanDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description.trim();
    }
}
