package com.mediturno.backend.appointment.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.domain.AppointmentStatus;

public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    String FETCH = """
            join fetch a.patient p
            join fetch p.user
            join fetch a.doctor d
            join fetch d.user
            join fetch d.specialty
            """;

    @Query("select a from Appointment a " + FETCH + " where a.id = :id")
    Optional<Appointment> findDetailedById(@Param("id") Long id);

    @Query("select a from Appointment a " + FETCH + """
            where (:patientId is null or p.userId = :patientId)
              and (:doctorId is null or d.id = :doctorId)
              and a.status in :statuses
              and a.startAt >= :from and a.startAt < :to
            order by a.startAt desc
            """)
    List<Appointment> search(
            @Param("patientId") Long patientId,
            @Param("doctorId") Long doctorId,
            @Param("statuses") Collection<AppointmentStatus> statuses,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    @Query("""
            select count(a) from Appointment a
            where a.doctor.id = :doctorId and a.status in :active
              and a.startAt < :end and a.endAt > :start and a.id <> :excludeId
            """)
    long countDoctorOverlap(
            @Param("doctorId") Long doctorId,
            @Param("active") Collection<AppointmentStatus> active,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);

    @Query("""
            select count(a) from Appointment a
            where a.patient.userId = :patientId and a.status in :active
              and a.startAt < :end and a.endAt > :start and a.id <> :excludeId
            """)
    long countPatientOverlap(
            @Param("patientId") Long patientId,
            @Param("active") Collection<AppointmentStatus> active,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            @Param("excludeId") Long excludeId);

    @Query("""
            select new com.mediturno.backend.appointment.repository.BusyRange(a.startAt, a.endAt)
            from Appointment a
            where a.doctor.id = :doctorId and a.status in :active
              and a.startAt >= :from and a.startAt < :to
            """)
    List<BusyRange> findBusyRanges(
            @Param("doctorId") Long doctorId,
            @Param("active") Collection<AppointmentStatus> active,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
