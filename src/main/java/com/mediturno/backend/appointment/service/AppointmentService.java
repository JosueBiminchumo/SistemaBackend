package com.mediturno.backend.appointment.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.repository.AppointmentRepository;
import com.mediturno.backend.common.exception.BadRequestException;
import com.mediturno.backend.common.exception.ConflictException;
import com.mediturno.backend.common.exception.NotFoundException;
import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import com.mediturno.backend.doctor.service.DoctorScheduleService;
import com.mediturno.backend.patient.domain.Patient;
import com.mediturno.backend.patient.repository.PatientRepository;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import com.mediturno.backend.user.repository.UserRepository;

@Service
public class AppointmentService {

    private static final LocalDateTime MIN_DATE = LocalDateTime.of(1970, 1, 1, 0, 0);
    private static final LocalDateTime MAX_DATE = LocalDateTime.of(2200, 1, 1, 0, 0);
    private static final long NO_ID = -1L;

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final DoctorScheduleService scheduleService;
    private final Clock clock;
    private final ZoneId zone;
    private final long minNoticeMinutes;

    public AppointmentService(
            AppointmentRepository appointmentRepository,
            DoctorRepository doctorRepository,
            PatientRepository patientRepository,
            UserRepository userRepository,
            DoctorScheduleService scheduleService,
            Clock clock,
            ZoneId zone,
            @Value("${app.appointments.min-notice-minutes:30}") long minNoticeMinutes) {
        this.appointmentRepository = appointmentRepository;
        this.doctorRepository = doctorRepository;
        this.patientRepository = patientRepository;
        this.userRepository = userRepository;
        this.scheduleService = scheduleService;
        this.clock = clock;
        this.zone = zone;
        this.minNoticeMinutes = minNoticeMinutes;
    }

    // ---------------------------------------------------------------- crear

    @Transactional
    public AppointmentResponse create(String email, AppointmentRequest request) {
        User user = currentUser(email);
        Patient patient = patientRepository.findById(user.getId())
                .orElseThrow(() -> new NotFoundException("No se encontró el perfil del paciente."));

        Slot slot = reserveSlot(request, NO_ID, patient.getUserId());
        Appointment appointment = Appointment.create(
                patient, slot.doctor(), slot.start(), slot.end(), request.reason().trim(), nowLocal());
        return AppointmentResponse.from(save(appointment));
    }

    // ------------------------------------------------------------- consultar

    @Transactional(readOnly = true)
    public List<AppointmentResponse> list(
            String email, AppointmentStatus status, LocalDate from, LocalDate to, Long doctorId) {
        User user = currentUser(email);
        if (from != null && to != null && to.isBefore(from)) {
            throw new BadRequestException("La fecha 'to' no puede ser anterior a 'from'.");
        }
        Long patientFilter = null;
        Long doctorFilter = doctorId;
        if (user.getRole() == UserRole.PATIENT) {
            patientFilter = user.getId();
            doctorFilter = doctorId;
        } else if (user.getRole() == UserRole.DOCTOR) {
            doctorFilter = doctorRepository.findDetailedByUserEmail(email)
                    .orElseThrow(() -> new NotFoundException("No se encontró el perfil del médico.")).getId();
        }
        var statuses = status == null ? EnumSet.allOf(AppointmentStatus.class) : EnumSet.of(status);
        LocalDateTime fromDt = from == null ? MIN_DATE : from.atStartOfDay();
        LocalDateTime toDt = to == null ? MAX_DATE : to.plusDays(1).atStartOfDay();
        return appointmentRepository.search(patientFilter, doctorFilter, statuses, fromDt, toDt)
                .stream().map(AppointmentResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public AppointmentResponse get(Long id, String email) {
        User user = currentUser(email);
        Appointment appointment = find(id);
        assertCanView(user, appointment);
        return AppointmentResponse.from(appointment);
    }

    // ----------------------------------------------------------- reprogramar

    @Transactional
    public AppointmentResponse reschedule(Long id, String email, AppointmentRequest request) {
        User user = currentUser(email);
        Appointment appointment = find(id);
        if (user.getRole() == UserRole.PATIENT && !isOwner(user, appointment)) {
            throw new AccessDeniedException("No puedes modificar la cita de otro paciente.");
        }
        if (!appointment.getStatus().isActive()) {
            throw new ConflictException("Solo se pueden reprogramar citas pendientes o confirmadas.");
        }
        if (!appointment.getStartAt().isAfter(nowLocal())) {
            throw new ConflictException("No se puede reprogramar una cita que ya comenzó.");
        }

        Slot slot = reserveSlot(request, appointment.getId(), appointment.getPatient().getUserId());
        appointment.reschedule(slot.doctor(), slot.start(), slot.end(), request.reason().trim(), nowLocal());
        return AppointmentResponse.from(save(appointment));
    }

    // ------------------------------------------------------- cancelar / estado

    @Transactional
    public AppointmentResponse cancel(Long id, String email) {
        User user = currentUser(email);
        Appointment appointment = find(id);
        assertCanView(user, appointment);
        if (user.getRole() == UserRole.PATIENT && !appointment.getStartAt().isAfter(nowLocal())) {
            throw new ConflictException("No se puede cancelar una cita que ya comenzó.");
        }
        return transition(appointment, AppointmentStatus.CANCELLED);
    }

    @Transactional
    public AppointmentResponse updateStatus(Long id, String email, AppointmentStatus newStatus) {
        User user = currentUser(email);
        Appointment appointment = find(id);
        if (user.getRole() == UserRole.PATIENT) {
            throw new AccessDeniedException("Un paciente solo puede cancelar su cita.");
        }
        assertCanView(user, appointment);
        if (newStatus == AppointmentStatus.COMPLETED && appointment.getStartAt().isAfter(nowLocal())) {
            throw new ConflictException("No se puede completar una cita que aún no comenzó.");
        }
        return transition(appointment, newStatus);
    }

    // ------------------------------------------------------------- internos

    private AppointmentResponse transition(Appointment appointment, AppointmentStatus next) {
        if (!appointment.getStatus().canTransitionTo(next)) {
            throw new ConflictException(
                    "No se puede pasar de " + appointment.getStatus() + " a " + next + ".");
        }
        appointment.changeStatus(next, nowLocal());
        return AppointmentResponse.from(save(appointment));
    }

    private record Slot(Doctor doctor, LocalDateTime start, LocalDateTime end) {
    }

    /** Valida horario/agenda/choques y bloquea al medico para serializar reservas concurrentes. */
    private Slot reserveSlot(AppointmentRequest request, long excludeId, Long patientId) {
        Doctor doctor = doctorRepository.findByIdForUpdate(request.doctorId())
                .orElseThrow(() -> new NotFoundException("El médico no existe."));
        if (!doctor.isActive() || !doctor.getSpecialty().isActive()) {
            throw new ConflictException("El médico no está disponible.");
        }

        LocalDateTime start = request.startAt().withNano(0);
        if (start.getSecond() != 0) {
            throw new BadRequestException("La hora de la cita debe indicarse en minutos exactos.");
        }
        if (!start.isAfter(nowLocal().plusMinutes(minNoticeMinutes))) {
            throw new BadRequestException(
                    "La cita debe reservarse con al menos " + minNoticeMinutes + " minutos de anticipación.");
        }
        int slotMinutes = scheduleService.matchingSlotMinutes(doctor.getId(), start)
                .orElseThrow(() -> new BadRequestException("El horario está fuera de la agenda del médico."));
        LocalDateTime end = start.plusMinutes(slotMinutes);

        if (appointmentRepository.countDoctorOverlap(
                doctor.getId(), AppointmentStatus.ACTIVE, start, end, excludeId) > 0) {
            throw new ConflictException("El horario seleccionado ya está ocupado.");
        }
        if (appointmentRepository.countPatientOverlap(
                patientId, AppointmentStatus.ACTIVE, start, end, excludeId) > 0) {
            throw new ConflictException("Ya tienes otra cita en ese horario.");
        }
        return new Slot(doctor, start, end);
    }

    private Appointment save(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException("El horario seleccionado ya está ocupado.");
        }
    }

    private Appointment find(Long id) {
        return appointmentRepository.findDetailedById(id)
                .orElseThrow(() -> new NotFoundException("La cita no existe."));
    }

    private User currentUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AccessDeniedException("Usuario no válido."));
    }

    private boolean isOwner(User user, Appointment appointment) {
        return appointment.getPatient().getUserId().equals(user.getId());
    }

    private void assertCanView(User user, Appointment appointment) {
        boolean allowed = switch (user.getRole()) {
            case ADMIN -> true;
            case PATIENT -> isOwner(user, appointment);
            case DOCTOR -> appointment.getDoctor().getUser().getId().equals(user.getId());
        };
        if (!allowed) {
            // 404 en lugar de 403 para no revelar la existencia de citas ajenas
            throw new NotFoundException("La cita no existe.");
        }
    }

    private LocalDateTime nowLocal() {
        return LocalDateTime.now(clock.withZone(zone));
    }
}
