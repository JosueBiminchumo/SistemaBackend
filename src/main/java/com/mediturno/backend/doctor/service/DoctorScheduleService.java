package com.mediturno.backend.doctor.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.common.exception.BadRequestException;
import com.mediturno.backend.common.exception.NotFoundException;
import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.dto.ScheduleRuleRequest;
import com.mediturno.backend.doctor.dto.ScheduleRuleResponse;
import com.mediturno.backend.doctor.dto.ScheduleRulesRequest;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import com.mediturno.backend.doctor.repository.ScheduleRepository;

@Service
public class DoctorScheduleService {

    /** Franja de atencion generada a partir de una regla semanal. */
    public record SlotWindow(LocalTime start, LocalTime end) {
    }

    private final DoctorRepository doctorRepository;
    private final ScheduleRepository scheduleRepository;

    public DoctorScheduleService(DoctorRepository doctorRepository, ScheduleRepository scheduleRepository) {
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
    }

    @Transactional(readOnly = true)
    public List<ScheduleRuleResponse> rules(Long doctorId) {
        ensureDoctorExists(doctorId);
        return scheduleRepository.findByDoctorIdOrderByDayOfWeekAscStartTimeAsc(doctorId)
                .stream().map(ScheduleRuleResponse::from).toList();
    }

    @Transactional
    public List<ScheduleRuleResponse> replaceRules(
            Long doctorId, ScheduleRulesRequest request, String email, boolean admin) {
        Doctor doctor = doctorRepository.findDetailedById(doctorId)
                .orElseThrow(() -> new NotFoundException("El médico no existe."));
        if (!admin && !doctor.getUser().getEmail().equalsIgnoreCase(email)) {
            throw new AccessDeniedException("No puedes modificar la agenda de otro médico.");
        }
        validate(request.rules());

        scheduleRepository.deleteByDoctorId(doctorId);
        scheduleRepository.flush();
        List<DoctorSchedule> created = request.rules().stream()
                .map(r -> new DoctorSchedule(
                        doctor, r.dayOfWeek(), r.startTime(), r.endTime(), r.slotMinutes(),
                        r.active() == null || r.active()))
                .toList();
        return scheduleRepository.saveAll(created).stream()
                .sorted(Comparator.comparing(DoctorSchedule::getDayOfWeek)
                        .thenComparing(DoctorSchedule::getStartTime))
                .map(ScheduleRuleResponse::from)
                .toList();
    }

    /** Todas las franjas posibles del medico para una fecha (sin descontar citas). */
    @Transactional(readOnly = true)
    public List<SlotWindow> slotsFor(Long doctorId, LocalDate date) {
        List<SlotWindow> slots = new ArrayList<>();
        for (DoctorSchedule rule : scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(doctorId, date.getDayOfWeek())) {
            int cursor = rule.getStartTime().toSecondOfDay() / 60;
            int end = rule.getEndTime().toSecondOfDay() / 60;
            while (cursor + rule.getSlotMinutes() <= end) {
                slots.add(new SlotWindow(
                        LocalTime.ofSecondOfDay(cursor * 60L),
                        LocalTime.ofSecondOfDay((cursor + rule.getSlotMinutes()) * 60L)));
                cursor += rule.getSlotMinutes();
            }
        }
        return slots;
    }

    /**
     * Si la hora de inicio cae exactamente en una franja de la agenda activa del medico,
     * devuelve la duracion de esa franja en minutos.
     */
    @Transactional(readOnly = true)
    public Optional<Integer> matchingSlotMinutes(Long doctorId, LocalDateTime start) {
        LocalTime time = start.toLocalTime();
        for (DoctorSchedule rule : scheduleRepository
                .findByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(doctorId, start.getDayOfWeek())) {
            long offset = ChronoUnit.MINUTES.between(rule.getStartTime(), time);
            boolean aligned = time.getSecond() == 0 && time.getNano() == 0
                    && offset >= 0 && offset % rule.getSlotMinutes() == 0;
            boolean fits = !time.plusMinutes(rule.getSlotMinutes()).isBefore(time) // sin cruce de medianoche
                    && !time.plusMinutes(rule.getSlotMinutes()).isAfter(rule.getEndTime());
            if (aligned && fits) {
                return Optional.of(rule.getSlotMinutes());
            }
        }
        return Optional.empty();
    }

    private void ensureDoctorExists(Long doctorId) {
        if (!doctorRepository.existsById(doctorId)) {
            throw new NotFoundException("El médico no existe.");
        }
    }

    private void validate(List<ScheduleRuleRequest> rules) {
        for (ScheduleRuleRequest r : rules) {
            if (!r.startTime().isBefore(r.endTime())) {
                throw new BadRequestException("La hora de inicio debe ser anterior a la hora de fin.");
            }
            if (r.startTime().getSecond() != 0 || r.endTime().getSecond() != 0) {
                throw new BadRequestException("Las horas deben indicarse en minutos exactos (HH:mm).");
            }
        }
        List<ScheduleRuleRequest> active = rules.stream()
                .filter(r -> r.active() == null || r.active())
                .sorted(Comparator.comparing(ScheduleRuleRequest::dayOfWeek)
                        .thenComparing(ScheduleRuleRequest::startTime))
                .toList();
        for (int i = 1; i < active.size(); i++) {
            ScheduleRuleRequest prev = active.get(i - 1);
            ScheduleRuleRequest cur = active.get(i);
            if (prev.dayOfWeek() == cur.dayOfWeek() && cur.startTime().isBefore(prev.endTime())) {
                throw new BadRequestException("Hay horarios solapados el mismo día.");
            }
        }
    }
}
