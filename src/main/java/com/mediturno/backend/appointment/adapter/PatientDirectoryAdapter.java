package com.mediturno.backend.appointment.adapter;

import com.mediturno.backend.appointment.port.PatientDirectory;
import com.mediturno.backend.patient.domain.Patient;
import com.mediturno.backend.patient.repository.PatientRepository;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PatientDirectoryAdapter implements PatientDirectory {

    private final PatientRepository patientRepository;

    public PatientDirectoryAdapter(PatientRepository patientRepository) {
        this.patientRepository = patientRepository;
    }

    @Override
    public Optional<PatientRef> findByUserId(Long userId) {
        return patientRepository.findByUserId(userId).map(this::toRef);   // AJUSTAR (punto 3)
    }

    @Override
    public Optional<PatientRef> findById(Long patientId) {
        return patientRepository.findById(patientId).map(this::toRef);
    }

    private PatientRef toRef(Patient patient) {
        // AJUSTAR (punto 4): de donde sale el nombre completo
        String fullName = patient.getUser().getFirstName() + " " + patient.getUser().getLastName();
        return new PatientRef(patient.getId(), fullName);
    }
}