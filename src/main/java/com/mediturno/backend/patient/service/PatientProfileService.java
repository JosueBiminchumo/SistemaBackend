package com.mediturno.backend.patient.service;

import com.mediturno.backend.patient.domain.Patient;
import com.mediturno.backend.patient.dto.PatientProfileResponse;
import com.mediturno.backend.patient.dto.UpdatePatientProfileRequest;
import com.mediturno.backend.patient.exception.DocumentNumberAlreadyRegisteredException;
import com.mediturno.backend.patient.exception.PatientProfileNotFoundException;
import com.mediturno.backend.patient.repository.PatientRepository;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientProfileService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;

    public PatientProfileService(UserRepository userRepository, PatientRepository patientRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
    }

    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(String email) {
        return PatientProfileResponse.from(findPatient(email));
    }

    @Transactional
    public PatientProfileResponse updateProfile(String email, UpdatePatientProfileRequest request) {
        Patient patient = findPatient(email);
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

    private Patient findPatient(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(PatientProfileNotFoundException::new);
        return patientRepository.findById(user.getId())
                .orElseThrow(PatientProfileNotFoundException::new);
    }
}
