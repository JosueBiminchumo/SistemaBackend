package com.mediturno.backend.doctor.repository;

import com.mediturno.backend.doctor.domain.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    List<Doctor> findAllByActiveTrueOrderByIdAsc();

    List<Doctor> findAllByActiveTrueAndSpecialtyIdOrderByIdAsc(
            Long specialtyId
    );

    Optional<Doctor> findByIdAndActiveTrue(Long id);

    Optional<Doctor> findByUserIdAndActiveTrue(Long userId);

    boolean existsByUserId(Long userId);

    boolean existsByLicenseNumberIgnoreCase(String licenseNumber);
}