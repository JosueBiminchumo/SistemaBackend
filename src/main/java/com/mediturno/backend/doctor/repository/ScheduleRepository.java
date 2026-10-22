package com.mediturno.backend.doctor.repository;

import com.mediturno.backend.doctor.domain.DoctorSchedule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface ScheduleRepository
        extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule>
    findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            Long doctorId,
            DayOfWeek dayOfWeek
    );

    List<DoctorSchedule>
    findAllByDoctorIdAndActiveTrueOrderByDayOfWeekAscStartTimeAsc(
            Long doctorId
    );

    Optional<DoctorSchedule> findByIdAndDoctorId(
            Long scheduleId,
            Long doctorId
    );
}