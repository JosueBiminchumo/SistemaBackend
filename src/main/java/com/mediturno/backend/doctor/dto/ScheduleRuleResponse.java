package com.mediturno.backend.doctor.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import com.mediturno.backend.doctor.domain.DoctorSchedule;

public record ScheduleRuleResponse(
        Long id,
        DayOfWeek dayOfWeek,
        LocalTime startTime,
        LocalTime endTime,
        int slotMinutes,
        boolean active) {

    public static ScheduleRuleResponse from(DoctorSchedule s) {
        return new ScheduleRuleResponse(
                s.getId(), s.getDayOfWeek(), s.getStartTime(), s.getEndTime(),
                s.getSlotMinutes(), s.isActive());
    }
}
