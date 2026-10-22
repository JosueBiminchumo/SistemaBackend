package com.mediturno.backend.doctor.service;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.exception.DoctorNotFoundException;
import com.mediturno.backend.doctor.mapper.DoctorMapper;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DoctorService {

    private final DoctorRepository doctorRepository;
    private final DoctorMapper doctorMapper;

    public List<DoctorResponse> findAllActive(Long specialtyId) {
        List<Doctor> doctors;

        if (specialtyId == null) {
            doctors = doctorRepository.findAllByActiveTrueOrderByIdAsc();
        } else {
            doctors = doctorRepository
                    .findAllByActiveTrueAndSpecialtyIdOrderByIdAsc(specialtyId);
        }

        return doctors.stream()
                .map(doctorMapper::toResponse)
                .toList();
    }

    public DoctorResponse findActiveById(Long doctorId) {
        return doctorMapper.toResponse(getActiveDoctor(doctorId));
    }

    public Doctor getActiveDoctor(Long doctorId) {
        return doctorRepository.findByIdAndActiveTrue(doctorId)
                .orElseThrow(() -> new DoctorNotFoundException(doctorId));
    }
}