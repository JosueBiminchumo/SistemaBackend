package com.mediturno.backend.patient.service;

import com.mediturno.backend.patient.domain.Patient;
import com.mediturno.backend.patient.dto.PatientProfileResponse;
import com.mediturno.backend.patient.dto.UpdatePatientProfileRequest;
import com.mediturno.backend.patient.exception.DocumentNumberAlreadyRegisteredException;
import com.mediturno.backend.patient.exception.PatientProfileNotFoundException;
import com.mediturno.backend.patient.repository.PatientRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientProfileService {

    private final PatientRepository patientRepository;

    public PatientProfileService(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        return PatientProfileResponse.from(findPatient(userId));
    }

    @Transactional
    public PatientProfileResponse updateProfile(Long userId, UpdatePatientProfileRequest request) {
        Patient patient = findPatient(userId);
        if (patientRepository.existsByDocumentNumberAndUserIdNot(
                request.documentNumber().trim(), patient.getUserId())) {
            throw new DocumentNumberAlreadyRegisteredException();
        }

        patient.updateProfile(
                request.documentNumber().trim(),
                request.birthDate(),
                request.phone().trim());
        return PatientProfileResponse.from(patientRepository.saveAndFlush(patient));
    }

    private Patient findPatient(Long userId) {
        return patientRepository.findById(userId)
                .orElseThrow(PatientProfileNotFoundException::new);
    }
}
