package com.mediturno.backend.doctor.mapper;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.dto.DoctorResponse;
import com.mediturno.backend.doctor.dto.DoctorScheduleResponse;
import org.springframework.stereotype.Component;

@Component
public class DoctorMapper {

    public DoctorResponse toResponse(Doctor doctor) {
        return new DoctorResponse(
                doctor.getId(),
                doctor.getUserId(),
                doctor.getSpecialtyId(),
                doctor.getLicenseNumber(),
                doctor.getBiography(),
                doctor.isActive()
        );
    }

    public DoctorScheduleResponse toScheduleResponse(
            DoctorSchedule schedule
    ) {
        return new DoctorScheduleResponse(
                schedule.getId(),
                schedule.getDoctor().getId(),
                schedule.getDayOfWeek(),
                schedule.getStartTime(),
                schedule.getEndTime(),
                schedule.getSlotMinutes(),
                schedule.isActive()
        );
    }
}