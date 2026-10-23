package com.mediturno.backend.appointment.adapter;

import com.mediturno.backend.appointment.port.PatientDirectory;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Conecta citas con el modulo de pacientes (Integrante 2) consultando su entidad Patient. */
@Component
public class PatientDirectoryAdapter implements PatientDirectory {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientRef> findByUserId(Long userId) {
        return first("select p from Patient p where p.user.id = :id", userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PatientRef> findById(Long patientId) {
        return first("select p from Patient p where p.id = :id", patientId);
    }

    private Optional<PatientRef> first(String jpql, Long id) {
        List<?> rows = entityManager.createQuery(jpql)
                .setParameter("id", id)
                .setMaxResults(1)
                .getResultList();
        return rows.isEmpty() ? Optional.empty() : Optional.of(toRef(rows.get(0)));
    }

    private PatientRef toRef(Object patient) {
        Long patientId = EntityReader.asLong(EntityReader.call(patient, "getId"));
        String name = EntityReader.fullName(EntityReader.call(patient, "getUser"));
        return new PatientRef(patientId, name != null ? name : "Paciente " + patientId);
    }
}