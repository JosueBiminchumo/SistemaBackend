package com.mediturn.patient.service;

import com.mediturn.patient.dto.PatientResponse;
import com.mediturn.patient.dto.PatientUpdateRequest;
import com.mediturn.patient.entity.Patient;
import com.mediturn.patient.mapper.PatientMapper;
import com.mediturn.patient.repository.PatientRepository;
import com.mediturn.patient.security.CurrentUserProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class PatientService {

    private final PatientRepository patientRepository;
    private final CurrentUserProvider currentUserProvider;

    public PatientService(PatientRepository patientRepository, CurrentUserProvider currentUserProvider) {
        this.patientRepository = patientRepository;
        this.currentUserProvider = currentUserProvider;
    }

    public PatientResponse getMyProfile() {
        return PatientMapper.toResponse(findCurrentPatient());
    }

    /** Called by the registration flow after its User has been persisted. */
    @Transactional
    public PatientResponse createProfileForUser(Long userId, PatientUpdateRequest request) {
        if (patientRepository.existsByUserId(userId)) {
            throw new PatientProfileAlreadyExistsException();
        }
        if (patientRepository.existsByDocumentNumber(request.documentNumber())) {
            throw new DuplicatePatientDocumentException();
        }

        Patient patient = new Patient(
                userId,
                request.documentNumber(),
                request.birthDate(),
                request.phone()
        );
        return PatientMapper.toResponse(patientRepository.save(patient));
    }

    @Transactional
    public PatientResponse updateMyProfile(PatientUpdateRequest request) {
        Patient patient = findCurrentPatient();

        if (patientRepository.existsByDocumentNumberAndIdNot(request.documentNumber(), patient.getId())) {
            throw new DuplicatePatientDocumentException();
        }

        patient.updateProfile(request.documentNumber(), request.birthDate(), request.phone());
        return PatientMapper.toResponse(patientRepository.save(patient));
    }

    private Patient findCurrentPatient() {
        Long userId = currentUserProvider.getCurrentUserId();
        return patientRepository.findByUserId(userId).orElseThrow(PatientNotFoundException::new);
    }
}
