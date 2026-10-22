package com.mediturno.backend.appointment.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;

@Entity
@Table(name = "citas")
public class Appointment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "patient_id", nullable = false)
    private Long patientId;

    @Column(name = "doctor_id", nullable = false)
    private Long doctorId;

    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AppointmentStatus status;

    @Column(nullable = false, length = 255)
    private String reason;

    /** "doctorId@startAt" mientras la cita ocupa horario; null si esta cancelada. */
    @Column(name = "active_slot_key", length = 64)
    private String activeSlotKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    protected Appointment() {
    }

    public static Appointment create(Long patientId, Long doctorId, LocalDateTime startAt,
                                     LocalDateTime endAt, String reason, LocalDateTime now) {
        Appointment appointment = new Appointment();
        appointment.patientId = patientId;
        appointment.doctorId = doctorId;
        appointment.startAt = startAt;
        appointment.endAt = endAt;
        appointment.reason = reason;
        appointment.status = AppointmentStatus.PENDING;
        appointment.createdAt = now;
        appointment.updatedAt = now;
        appointment.refreshActiveSlotKey();
        return appointment;
    }

    public void reschedule(LocalDateTime newStartAt, LocalDateTime newEndAt, String newReason, LocalDateTime now) {
        this.startAt = newStartAt;
        this.endAt = newEndAt;
        if (newReason != null && !newReason.isBlank()) {
            this.reason = newReason.trim();
        }
        this.updatedAt = now;
        refreshActiveSlotKey();
    }

    public void changeStatus(AppointmentStatus newStatus, LocalDateTime now) {
        this.status = newStatus;
        this.updatedAt = now;
        refreshActiveSlotKey();
    }

    private void refreshActiveSlotKey() {
        this.activeSlotKey = status.blocksSchedule() ? doctorId + "@" + startAt : null;
    }

    public Long getId() { return id; }
    public Long getPatientId() { return patientId; }
    public Long getDoctorId() { return doctorId; }
    public LocalDateTime getStartAt() { return startAt; }
    public LocalDateTime getEndAt() { return endAt; }
    public AppointmentStatus getStatus() { return status; }
    public String getReason() { return reason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}