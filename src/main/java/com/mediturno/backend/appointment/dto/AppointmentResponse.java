package com.mediturno.backend.appointment.dto;

import java.time.LocalDateTime;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.domain.AppointmentStatus;

public record AppointmentResponse(
        Long id,
        Long patientId,
        String patientName,
        Long doctorId,
        String doctorName,
        Long specialtyId,
        String specialtyName,
        LocalDateTime startAt,
        LocalDateTime endAt,
        AppointmentStatus status,
        String reason,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static AppointmentResponse from(Appointment a) {
        var patientUser = a.getPatient().getUser();
        var doctor = a.getDoctor();
        return new AppointmentResponse(
                a.getId(),
                a.getPatient().getUserId(),
                patientUser.getFirstName() + " " + patientUser.getLastName(),
                doctor.getId(),
                doctor.getUser().getFirstName() + " " + doctor.getUser().getLastName(),
                doctor.getSpecialty().getId(),
                doctor.getSpecialty().getName(),
                a.getStartAt(),
                a.getEndAt(),
                a.getStatus(),
                a.getReason(),
                a.getCreatedAt(),
                a.getUpdatedAt());
    }
}
