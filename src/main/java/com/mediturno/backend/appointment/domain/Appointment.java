package com.mediturno.backend.appointment.domain;

import java.time.LocalDateTime;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.patient.domain.Patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "citas")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(nullable = false, length = 255)
    private String reason;

    /** Unica por medico+inicio mientras la cita esta activa; null si esta cancelada/completada. */
    @Column(name = "active_slot_key", length = 64)
    private String activeSlotKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected Appointment() {
    }

    public static Appointment create(Patient patient, Doctor doctor, LocalDateTime startAt,
            LocalDateTime endAt, String reason, LocalDateTime now) {
        Appointment a = new Appointment();
        a.patient = patient;
        a.doctor = doctor;
        a.startAt = startAt;
        a.endAt = endAt;
        a.reason = reason;
        a.status = AppointmentStatus.PENDING;
        a.createdAt = now;
        a.updatedAt = now;
        a.refreshSlotKey();
        return a;
    }

    public void reschedule(Doctor doctor, LocalDateTime startAt, LocalDateTime endAt,
            String reason, LocalDateTime now) {
        this.doctor = doctor;
        this.startAt = startAt;
        this.endAt = endAt;
        this.reason = reason;
        this.status = AppointmentStatus.PENDING;
        this.updatedAt = now;
        refreshSlotKey();
    }

    public void changeStatus(AppointmentStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.updatedAt = now;
        refreshSlotKey();
    }

    private void refreshSlotKey() {
        this.activeSlotKey = status.isActive() ? doctor.getId() + "-" + startAt : null;
    }

    public Long getId() {
        return id;
    }

    public Patient getPatient() {
        return patient;
    }

    public Doctor getDoctor() {
        return doctor;
    }

    public LocalDateTime getStartAt() {
        return startAt;
    }

    public LocalDateTime getEndAt() {
        return endAt;
    }

    public AppointmentStatus getStatus() {
        return status;
    }

    public String getReason() {
        return reason;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
