package com.mediturno.backend.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.appointment.dto.AvailabilityResponse;
import com.mediturno.backend.appointment.repository.AppointmentRepository;
import com.mediturno.backend.appointment.repository.BusyRange;
import com.mediturno.backend.common.exception.NotFoundException;
import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import com.mediturno.backend.doctor.service.DoctorScheduleService;

@Service
public class AvailabilityService {

    private final DoctorRepository doctorRepository;
    private final DoctorScheduleService scheduleService;
    private final AppointmentRepository appointmentRepository;
    private final Clock clock;
    private final ZoneId zone;
    private final long minNoticeMinutes;

    public AvailabilityService(
            DoctorRepository doctorRepository,
            DoctorScheduleService scheduleService,
            AppointmentRepository appointmentRepository,
            Clock clock,
            ZoneId zone,
            @Value("${app.appointments.min-notice-minutes:30}") long minNoticeMinutes) {
        this.doctorRepository = doctorRepository;
        this.scheduleService = scheduleService;
        this.appointmentRepository = appointmentRepository;
        this.clock = clock;
        this.zone = zone;
        this.minNoticeMinutes = minNoticeMinutes;
    }

    @Transactional(readOnly = true)
    public AvailabilityResponse forDate(Long doctorId, LocalDate date) {
        Doctor doctor = doctorRepository.findDetailedById(doctorId)
                .filter(d -> d.isActive() && d.getSpecialty().isActive())
                .orElseThrow(() -> new NotFoundException("El médico no existe."));

        LocalDateTime limit = LocalDateTime.now(clock.withZone(zone)).plusMinutes(minNoticeMinutes);
        List<BusyRange> busy = appointmentRepository.findBusyRanges(
                doctor.getId(), AppointmentStatus.ACTIVE, date.atStartOfDay(), date.plusDays(1).atStartOfDay());

        List<AvailabilityResponse.Slot> slots = scheduleService.slotsFor(doctor.getId(), date).stream()
                .map(w -> {
                    LocalDateTime start = LocalDateTime.of(date, w.start());
                    LocalDateTime end = LocalDateTime.of(date, w.end());
                    boolean taken = busy.stream()
                            .anyMatch(b -> b.startAt().isBefore(end) && b.endAt().isAfter(start));
                    return new AvailabilityResponse.Slot(start, end, !taken && start.isAfter(limit));
                })
                .toList();
        return new AvailabilityResponse(doctor.getId(), date, zone.getId(), slots);
    }
}
