package com.mediturno.backend.appointment.port;

import java.util.Optional;

/** Contrato con el modulo de medicos (Integrante 3). */
public interface DoctorDirectory {

    /** Devuelve el medico aunque este inactivo, para mostrar citas antiguas. */
    Optional<DoctorRef> findById(Long doctorId);

    Optional<Long> findIdByUserId(Long userId);

    record DoctorRef(Long id, String fullName, String specialtyName, boolean active) {
    }
}