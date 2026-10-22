package com.mediturno.backend.doctor.service;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.dto.DoctorAvailabilityResponse;
import com.mediturno.backend.doctor.dto.DoctorScheduleResponse;
import com.mediturno.backend.doctor.dto.ScheduleRequest;
import com.mediturno.backend.doctor.exception.ScheduleNotAvailableException;
import com.mediturno.backend.doctor.exception.ScheduleNotFoundException;
import com.mediturno.backend.doctor.exception.ScheduleOverlapException;
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

    public List<DoctorScheduleResponse> findRecurringSchedule(
            Long doctorId
    ) {
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

            while (!currentTime
                    .plusMinutes(schedule.getSlotMinutes())
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
                                && !time
                                .plusMinutes(schedule.getSlotMinutes())
                                .isAfter(schedule.getEndTime())
                );
    }

    @Transactional
    public DoctorScheduleResponse create(
            Long doctorId,
            ScheduleRequest request
    ) {
        Doctor doctor = doctorService.getActiveDoctor(doctorId);

        validateOverlap(doctorId, null, request);

        DoctorSchedule schedule = DoctorSchedule.builder()
                .doctor(doctor)
                .dayOfWeek(request.dayOfWeek())
                .startTime(request.startTime())
                .endTime(request.endTime())
                .slotMinutes(request.slotMinutes())
                .active(true)
                .build();

        DoctorSchedule savedSchedule =
                scheduleRepository.save(schedule);

        return doctorMapper.toScheduleResponse(savedSchedule);
    }

    @Transactional
    public DoctorScheduleResponse update(
            Long doctorId,
            Long scheduleId,
            ScheduleRequest request
    ) {
        doctorService.getActiveDoctor(doctorId);

        DoctorSchedule schedule = scheduleRepository
                .findByIdAndDoctorId(scheduleId, doctorId)
                .orElseThrow(() ->
                        new ScheduleNotFoundException(
                                doctorId,
                                scheduleId
                        )
                );

        validateOverlap(doctorId, scheduleId, request);

        schedule.setDayOfWeek(request.dayOfWeek());
        schedule.setStartTime(request.startTime());
        schedule.setEndTime(request.endTime());
        schedule.setSlotMinutes(request.slotMinutes());
        schedule.setActive(true);

        DoctorSchedule updatedSchedule =
                scheduleRepository.save(schedule);

        return doctorMapper.toScheduleResponse(updatedSchedule);
    }

    @Transactional
    public void deactivate(
            Long doctorId,
            Long scheduleId
    ) {
        doctorService.getActiveDoctor(doctorId);

        DoctorSchedule schedule = scheduleRepository
                .findByIdAndDoctorId(scheduleId, doctorId)
                .orElseThrow(() ->
                        new ScheduleNotFoundException(
                                doctorId,
                                scheduleId
                        )
                );

        schedule.setActive(false);
        scheduleRepository.save(schedule);
    }

    private void validateOverlap(
            Long doctorId,
            Long ignoredScheduleId,
            ScheduleRequest request
    ) {
        boolean overlaps = scheduleRepository
                .findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        doctorId,
                        request.dayOfWeek()
                )
                .stream()
                .filter(schedule ->
                        ignoredScheduleId == null
                                || !schedule.getId()
                                .equals(ignoredScheduleId)
                )
                .anyMatch(schedule ->
                        request.startTime()
                                .isBefore(schedule.getEndTime())
                                && request.endTime()
                                .isAfter(schedule.getStartTime())
                );

        if (overlaps) {
            throw new ScheduleOverlapException();
        }
    }
}