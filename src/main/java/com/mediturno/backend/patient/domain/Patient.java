package com.mediturno.backend.patient.domain;

import java.time.Instant;
import java.time.LocalDate;

import com.mediturno.backend.user.domain.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "patients", uniqueConstraints = {
        @UniqueConstraint(name = "uk_patients_document_number", columnNames = "document_number"),
        @UniqueConstraint(name = "uk_patients_user_id", columnNames = "user_id")
})
public class Patient {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "document_number", nullable = false, length = 8)
    private String documentNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 20)
    private String phone;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Patient() {
    }

    private Patient(User user, String documentNumber, LocalDate birthDate, String phone) {
        this.user = user;
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    public static Patient create(User user, String documentNumber, LocalDate birthDate, String phone) {
        return new Patient(user, documentNumber, birthDate, phone);
    }

    public void updateProfile(String documentNumber, LocalDate birthDate, String phone) {
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    @PrePersist
    void setCreationTimestamps() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void updateTimestamp() {
        updatedAt = Instant.now();
    }

    public Long getUserId() {
        return userId;
    }

    public User getUser() {
        return user;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public String getPhone() {
        return phone;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
