package com.mediturno.backend.doctor.service;

import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.dto.DoctorAvailabilityResponse;
import com.mediturno.backend.doctor.dto.DoctorScheduleResponse;
import com.mediturno.backend.doctor.exception.ScheduleNotAvailableException;
import com.mediturno.backend.doctor.mapper.DoctorMapper;
import com.mediturno.backend.doctor.repository.ScheduleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final DoctorService doctorService;
    private final DoctorMapper doctorMapper;

    public List<DoctorScheduleResponse> findRecurringSchedule(Long doctorId) {
        doctorService.getActiveDoctor(doctorId);

        return scheduleRepository
                .findAllByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
                        doctorId
                )
                .stream()
                .map(doctorMapper::toScheduleResponse)
                .toList();
    }

    public DoctorAvailabilityResponse findAvailability(
            Long doctorId,
            LocalDate date
    ) {
        doctorService.getActiveDoctor(doctorId);

        List<DoctorSchedule> schedules = scheduleRepository
                .findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        doctorId,
                        date.getDayOfWeek()
                );

        if (schedules.isEmpty()) {
            throw new ScheduleNotAvailableException(doctorId, date);
        }

        List<LocalTime> slots = new ArrayList<>();

        for (DoctorSchedule schedule : schedules) {
            LocalTime currentTime = schedule.getStartTime();

            while (!currentTime.plusMinutes(schedule.getSlotMinutes())
                    .isAfter(schedule.getEndTime())) {
                slots.add(currentTime);
                currentTime = currentTime.plusMinutes(
                        schedule.getSlotMinutes()
                );
            }
        }

        return new DoctorAvailabilityResponse(
                doctorId,
                date,
                !slots.isEmpty(),
                slots
        );
    }

    public boolean isWithinSchedule(
            Long doctorId,
            LocalDate date,
            LocalTime time
    ) {
        doctorService.getActiveDoctor(doctorId);

        return scheduleRepository
                .findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        doctorId,
                        date.getDayOfWeek()
                )
                .stream()
                .anyMatch(schedule ->
                        !time.isBefore(schedule.getStartTime())
                        && time.plusMinutes(schedule.getSlotMinutes())
                                .compareTo(schedule.getEndTime()) <= 0
                );
    }
}