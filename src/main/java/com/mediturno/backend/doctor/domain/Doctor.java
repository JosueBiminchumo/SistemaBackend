package com.mediturno.backend.doctor.domain;

import com.mediturno.backend.specialty.domain.Specialty;
import com.mediturno.backend.user.domain.User;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "doctors")
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "specialty_id", nullable = false)
    private Specialty specialty;

    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Column(length = 2000)
    private String biography;

    @Column(nullable = false)
    private boolean active;

    protected Doctor() {
    }

    public Doctor(User user, Specialty specialty, String licenseNumber, String biography) {
        this.user = user;
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;
        this.biography = biography;
        this.active = true;
    }

    public void updateByAdmin(Specialty specialty, String licenseNumber, String biography, boolean active) {
        this.specialty = specialty;
        this.licenseNumber = licenseNumber;
        this.biography = biography;
        this.active = active;
    }

    public void updateBiography(String biography) {
        this.biography = biography;
    }

    public void deactivate() {
        this.active = false;
    }

    public Long getId() {
        return id;
    }

    public User getUser() {
        return user;
    }

    public Specialty getSpecialty() {
        return specialty;
    }

    public String getLicenseNumber() {
        return licenseNumber;
    }

    public String getBiography() {
        return biography;
    }

    public boolean isActive() {
        return active;
    }
}
