package com.mediturno.backend.patient.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.patient.domain.Patient;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByDocumentNumber(String documentNumber);

    boolean existsByDocumentNumberAndUserIdNot(String documentNumber, Long userId);
}
