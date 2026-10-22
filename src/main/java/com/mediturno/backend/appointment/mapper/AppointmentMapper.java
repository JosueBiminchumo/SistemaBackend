package com.mediturno.backend.appointment.mapper;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.port.DoctorDirectory.DoctorRef;
import com.mediturno.backend.appointment.port.PatientDirectory.PatientRef;
import org.springframework.stereotype.Component;

@Component
public class AppointmentMapper {

    public AppointmentResponse toResponse(Appointment appointment, PatientRef patient, DoctorRef doctor) {
        return new AppointmentResponse(
                appointment.getId(),
                appointment.getPatientId(),
                patient == null ? null : patient.fullName(),
                appointment.getDoctorId(),
                doctor == null ? null : doctor.fullName(),
                doctor == null ? null : doctor.specialtyName(),
                appointment.getStartAt(),
                appointment.getEndAt(),
                appointment.getStatus(),
                appointment.getReason(),
                appointment.getCreatedAt());
    }
} 