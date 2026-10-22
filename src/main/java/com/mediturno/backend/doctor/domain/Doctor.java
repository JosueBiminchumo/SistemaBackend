package com.mediturno.backend.doctor.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Entity
@Table(
        name = "doctors",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_doctor_user",
                        columnNames = "user_id"
                ),
                @UniqueConstraint(
                        name = "uk_doctor_license",
                        columnNames = "license_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_doctor_specialty_active",
                        columnList = "specialty_id, active"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Doctor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Identificador del usuario asociado.
     * Durante la integración se relacionará con la entidad User.
     */
    @NotNull
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /*
     * Identificador de la especialidad.
     * Durante la integración se relacionará con Specialty.
     */
    @NotNull
    @Column(name = "specialty_id", nullable = false)
    private Long specialtyId;

    @NotBlank
    @Size(max = 50)
    @Column(name = "license_number", nullable = false, length = 50)
    private String licenseNumber;

    @Size(max = 2000)
    @Column(name = "biography", length = 2000)
    private String biography;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
}