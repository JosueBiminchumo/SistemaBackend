package com.mediturn.patient.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Unique userId enforces the one-to-one ownership rule until User is integrated.
    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "document_number", nullable = false, unique = true, length = 8)
    private String documentNumber;

    @Column(name = "birth_date", nullable = false)
    private LocalDate birthDate;

    @Column(nullable = false, length = 9)
    private String phone;

    protected Patient() {
    }

    public Patient(Long userId, String documentNumber, LocalDate birthDate, String phone) {
        this.userId = userId;
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    public void updateProfile(String documentNumber, LocalDate birthDate, String phone) {
        this.documentNumber = documentNumber;
        this.birthDate = birthDate;
        this.phone = phone;
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
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
}
