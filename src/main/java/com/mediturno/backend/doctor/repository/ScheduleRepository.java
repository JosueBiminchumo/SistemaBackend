package com.mediturno.backend.doctor.repository;

import java.time.DayOfWeek;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.mediturno.backend.doctor.domain.DoctorSchedule;

public interface ScheduleRepository extends JpaRepository<DoctorSchedule, Long> {

    List<DoctorSchedule> findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(Long doctorId);

    List<DoctorSchedule> findByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
            Long doctorId, DayOfWeek dayOfWeek);

    void deleteByDoctorId(Long doctorId);
}
