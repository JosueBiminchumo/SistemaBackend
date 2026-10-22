package com.mediturno.backend.appointment.port;

import java.util.Optional;

/** Contrato con el modulo de pacientes (Integrante 2). */
public interface PatientDirectory {

    Optional<PatientRef> findByUserId(Long userId);

    Optional<PatientRef> findById(Long patientId);

    record PatientRef(Long id, String fullName) {
    }
}
