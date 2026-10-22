package com.mediturno.backend.doctor.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(
        name = "doctor_schedules",
        indexes = {
                @Index(
                        name = "idx_schedule_doctor_day_active",
                        columnList = "doctor_id, day_of_week, active"
                )
        }
)
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DoctorSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "doctor_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_schedule_doctor")
    )
    private Doctor doctor;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "day_of_week", nullable = false, length = 15)
    private DayOfWeek dayOfWeek;

    @NotNull
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;

    @NotNull
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;

    @Min(5)
    @Max(240)
    @Column(name = "slot_minutes", nullable = false)
    private Integer slotMinutes;

    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;

    @AssertTrue(message = "La hora de inicio debe ser anterior a la hora de fin")
    public boolean isValidTimeRange() {
        if (startTime == null || endTime == null) {
            return true;
        }

        return startTime.isBefore(endTime);
    }
}