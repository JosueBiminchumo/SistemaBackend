package com.mediturno.backend.appointment.repository;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.domain.AppointmentStatus;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctorId = :doctorId
              and a.status <> com.mediturno.backend.appointment.domain.AppointmentStatus.CANCELLED
              and a.startAt < :endAt and a.endAt > :startAt
            """)
    boolean existsDoctorOverlap(@Param("doctorId") Long doctorId,
                                @Param("startAt") LocalDateTime startAt,
                                @Param("endAt") LocalDateTime endAt);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.doctorId = :doctorId and a.id <> :excludeId
              and a.status <> com.mediturno.backend.appointment.domain.AppointmentStatus.CANCELLED
              and a.startAt < :endAt and a.endAt > :startAt
            """)
    boolean existsDoctorOverlapExcluding(@Param("doctorId") Long doctorId,
                                         @Param("startAt") LocalDateTime startAt,
                                         @Param("endAt") LocalDateTime endAt,
                                         @Param("excludeId") Long excludeId);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.patientId = :patientId
              and a.status <> com.mediturno.backend.appointment.domain.AppointmentStatus.CANCELLED
              and a.startAt < :endAt and a.endAt > :startAt
            """)
    boolean existsPatientOverlap(@Param("patientId") Long patientId,
                                 @Param("startAt") LocalDateTime startAt,
                                 @Param("endAt") LocalDateTime endAt);

    @Query("""
            select count(a) > 0 from Appointment a
            where a.patientId = :patientId and a.id <> :excludeId
              and a.status <> com.mediturno.backend.appointment.domain.AppointmentStatus.CANCELLED
              and a.startAt < :endAt and a.endAt > :startAt
            """)
    boolean existsPatientOverlapExcluding(@Param("patientId") Long patientId,
                                          @Param("startAt") LocalDateTime startAt,
                                          @Param("endAt") LocalDateTime endAt,
                                          @Param("excludeId") Long excludeId);

    @Query("""
            select a from Appointment a
            where (:patientId is null or a.patientId = :patientId)
              and (:doctorId is null or a.doctorId = :doctorId)
              and (:status is null or a.status = :status)
              and (:fromAt is null or a.startAt >= :fromAt)
              and (:toAt is null or a.startAt < :toAt)
            """)
    Page<Appointment> search(@Param("patientId") Long patientId,
                             @Param("doctorId") Long doctorId,
                             @Param("status") AppointmentStatus status,
                             @Param("fromAt") LocalDateTime fromAt,
                             @Param("toAt") LocalDateTime toAt,
                             Pageable pageable);
}