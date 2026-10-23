package com.mediturno.backend.specialty.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.specialty.domain.Specialty;

public interface SpecialtyRepository extends JpaRepository<Specialty, Long> {

    List<Specialty> findAllByActiveTrueOrderByNameAsc();

    List<Specialty> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);

    boolean existsByNameIgnoreCaseAndIdNot(String name, Long id);

    long countByActiveTrue();
}
