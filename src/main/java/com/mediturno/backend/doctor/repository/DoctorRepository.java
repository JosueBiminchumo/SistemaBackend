package com.mediturno.backend.doctor.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.doctor.domain.Doctor;

import jakarta.persistence.LockModeType;

public interface DoctorRepository extends JpaRepository<Doctor, Long> {

    @Query("""
            select d from Doctor d
            join fetch d.user u
            join fetch d.specialty s
            where d.active = true and s.active = true
              and (:specialtyId is null or s.id = :specialtyId)
              and lower(concat(u.firstName, ' ', u.lastName)) like :q
            order by u.lastName, u.firstName
            """)
    List<Doctor> searchActive(@Param("specialtyId") Long specialtyId, @Param("q") String q);

    @Query("""
            select d from Doctor d
            join fetch d.user u
            join fetch d.specialty s
            order by u.lastName, u.firstName
            """)
    List<Doctor> findAllWithDetails();

    @Query("""
            select d from Doctor d
            join fetch d.user join fetch d.specialty
            where d.id = :id
            """)
    Optional<Doctor> findDetailedById(@Param("id") Long id);

    @Query("""
            select d from Doctor d
            join fetch d.user u join fetch d.specialty
            where u.email = :email
            """)
    Optional<Doctor> findDetailedByUserEmail(@Param("email") String email);

    /** Bloquea la fila del medico para serializar reservas concurrentes. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select d from Doctor d where d.id = :id")
    Optional<Doctor> findByIdForUpdate(@Param("id") Long id);

    boolean existsByLicenseNumber(String licenseNumber);

    boolean existsByLicenseNumberAndIdNot(String licenseNumber, Long id);

    long countByActiveTrue();
}
