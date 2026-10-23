package com.mediturno.backend.statistics.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Repository;

import jakarta.persistence.EntityManager;

/** Consultas agregadas: la BD agrupa y cuenta; nunca se cargan citas completas a memoria. */
@Repository
public class StatisticsRepository {

    private final EntityManager em;

    public StatisticsRepository(EntityManager em) {
        this.em = em;
    }

    /** filas: [AppointmentStatus, Long] */
    public List<Object[]> countByStatus(LocalDateTime from, LocalDateTime to) {
        return em.createQuery("""
                select a.status, count(a) from Appointment a
                where a.startAt >= :from and a.startAt < :to
                group by a.status
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** filas: [String nombreEspecialidad, Long] ordenadas de mayor a menor */
    public List<Object[]> countBySpecialty(LocalDateTime from, LocalDateTime to) {
        return em.createQuery("""
                select s.name, count(a) from Appointment a
                join a.doctor d join d.specialty s
                where a.startAt >= :from and a.startAt < :to
                group by s.name
                order by count(a) desc, s.name asc
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    /** filas: [Integer year, Integer month, Integer day, Long] ordenadas por fecha */
    public List<Object[]> countByDay(LocalDateTime from, LocalDateTime to) {
        return em.createQuery("""
                select year(a.startAt), month(a.startAt), day(a.startAt), count(a)
                from Appointment a
                where a.startAt >= :from and a.startAt < :to
                group by year(a.startAt), month(a.startAt), day(a.startAt)
                order by year(a.startAt), month(a.startAt), day(a.startAt)
                """, Object[].class)
                .setParameter("from", from)
                .setParameter("to", to)
                .getResultList();
    }

    public long countPatients() {
        return em.createQuery("select count(p) from Patient p", Long.class).getSingleResult();
    }

    public long countActiveDoctors() {
        return em.createQuery("select count(d) from Doctor d where d.active = true", Long.class)
                .getSingleResult();
    }

    public long countActiveSpecialties() {
        return em.createQuery("select count(s) from Specialty s where s.active = true", Long.class)
                .getSingleResult();
    }
}
