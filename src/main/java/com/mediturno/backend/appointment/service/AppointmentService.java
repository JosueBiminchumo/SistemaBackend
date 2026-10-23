package com.mediturno.backend.appointment.service;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.dto.AppointmentUpdateRequest;
import com.mediturno.backend.appointment.dto.AppointmentPage;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.dto.AppointmentSearchCriteria;
import com.mediturno.backend.appointment.exception.AppointmentAccessDeniedException;
import com.mediturno.backend.appointment.exception.AppointmentConflictException;
import com.mediturno.backend.appointment.exception.AppointmentNotFoundException;
import com.mediturno.backend.appointment.exception.AppointmentRuleException;
import com.mediturno.backend.appointment.mapper.AppointmentMapper;
import com.mediturno.backend.appointment.port.ActorRole;
import com.mediturno.backend.appointment.port.AppointmentActor;
import com.mediturno.backend.appointment.port.AppointmentActorProvider;
import com.mediturno.backend.appointment.port.DoctorAvailability;
import com.mediturno.backend.appointment.port.DoctorDirectory;
import com.mediturno.backend.appointment.port.DoctorDirectory.DoctorRef;
import com.mediturno.backend.appointment.port.PatientDirectory;
import com.mediturno.backend.appointment.port.PatientDirectory.PatientRef;
import com.mediturno.backend.appointment.repository.AppointmentRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

    private static final int MAX_PAGE_SIZE = 100;

    private final AppointmentRepository appointmentRepository;
    private final AppointmentMapper mapper;
    private final PatientDirectory patientDirectory;
    private final DoctorDirectory doctorDirectory;
    private final DoctorAvailability doctorAvailability;
    private final AppointmentActorProvider actorProvider;
    private final Clock clock;
    private final long minAdvanceMinutes;

    public AppointmentService(AppointmentRepository appointmentRepository,
                              AppointmentMapper mapper,
                              PatientDirectory patientDirectory,
                              DoctorDirectory doctorDirectory,
                              DoctorAvailability doctorAvailability,
                              AppointmentActorProvider actorProvider,
                              Clock clock,
                              @Value("${mediturno.appointment.min-advance-minutes:60}") long minAdvanceMinutes) {
        this.appointmentRepository = appointmentRepository;
        this.mapper = mapper;
        this.patientDirectory = patientDirectory;
        this.doctorDirectory = doctorDirectory;
        this.doctorAvailability = doctorAvailability;
        this.actorProvider = actorProvider;
        this.clock = clock;
        this.minAdvanceMinutes = minAdvanceMinutes;
    }

    // ================= CREAR (commits 10 y 11) =================

    @Transactional
    public AppointmentResponse create(AppointmentRequest request) {
        AppointmentActor actor = actorProvider.current();
        if (actor.role() != ActorRole.PATIENT) {
            throw new AppointmentAccessDeniedException("Solo un paciente puede reservar citas");
        }
        PatientRef patient = requirePatient(actor);
        DoctorRef doctor = doctorDirectory.findById(request.doctorId())
                .filter(DoctorRef::active)
                .orElseThrow(() -> new AppointmentRuleException("El medico no existe o no esta activo"));

        LocalDateTime startAt = normalize(request.startAt());
        validateAdvance(startAt);
        LocalDateTime endAt = startAt.plusMinutes(slotMinutes(doctor.id(), startAt));

        assertSlotFree(doctor.id(), patient.id(), startAt, endAt, null);
        Appointment saved = saveOrConflict(Appointment.create(
                patient.id(), doctor.id(), startAt, endAt, request.reason().trim(), LocalDateTime.now(clock)));
        return toResponse(saved);
    }

    // ================= CONSULTAR (commit 12) =================

    @Transactional(readOnly = true)
    public AppointmentPage list(AppointmentSearchCriteria criteria, int page, int size) {
        AppointmentActor actor = actorProvider.current();

        // 1) El alcance lo define el rol, no los parametros del cliente
        Long patientId = criteria.patientId();
        Long doctorId = criteria.doctorId();
        switch (actor.role()) {
            case PATIENT -> patientId = requirePatient(actor).id();
            case DOCTOR -> doctorId = requireDoctorId(actor);
            case ADMIN -> { }
        }

        // 2) Rango de fechas: 'to' es inclusivo (hasta el final de ese dia)
        LocalDate from = criteria.from();
        LocalDate to = criteria.to();
        if (from != null && to != null && to.isBefore(from)) {
            throw new AppointmentRuleException("La fecha 'to' no puede ser anterior a 'from'");
        }
        LocalDateTime fromAt = from == null ? null : from.atStartOfDay();
        LocalDateTime toAt = to == null ? null : to.plusDays(1).atStartOfDay();

        // 3) Paginacion acotada, ordenada por fecha de inicio
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE),
                Sort.by("startAt").ascending());

        Page<Appointment> result = appointmentRepository
                .search(patientId, doctorId, criteria.status(), fromAt, toAt, pageable);

        List<AppointmentResponse> content = result.getContent().stream().map(this::toResponse).toList();
        return new AppointmentPage(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public AppointmentResponse getById(Long id) {
        Appointment appointment = findOrThrow(id);
        assertParticipantOrAdmin(appointment, actorProvider.current());
        return toResponse(appointment);
    }
    // ================= REPROGRAMAR (commit 13) =================

    @Transactional
    public AppointmentResponse reschedule(Long id, AppointmentUpdateRequest request) {
        Appointment appointment = findOrThrow(id);
        AppointmentActor actor = actorProvider.current();

        // El medico no reprograma: puede confirmar o cancelar
        if (actor.role() == ActorRole.DOCTOR) {
            throw new AppointmentAccessDeniedException(
                    "El medico no reprograma citas; puede confirmarlas o cancelarlas");
        }
        assertParticipantOrAdmin(appointment, actor);

        if (!appointment.getStatus().isOpen()) {
            throw new AppointmentConflictException("Solo se pueden reprogramar citas pendientes o confirmadas");
        }

        // Mismas reglas que al reservar
        LocalDateTime startAt = normalize(request.startAt());
        validateAdvance(startAt);
        LocalDateTime endAt = startAt.plusMinutes(slotMinutes(appointment.getDoctorId(), startAt));

        // excludeId = la propia cita, para que no choque consigo misma
        assertSlotFree(appointment.getDoctorId(), appointment.getPatientId(), startAt, endAt, appointment.getId());

        appointment.reschedule(startAt, endAt, request.reason(), LocalDateTime.now(clock));
        return toResponse(saveOrConflict(appointment));
    }
    // ================= HELPERS =================

    private PatientRef requirePatient(AppointmentActor actor) {
        return patientDirectory.findByUserId(actor.userId())
                .orElseThrow(() -> new AppointmentAccessDeniedException("El usuario no tiene perfil de paciente"));
    }

    private Long requireDoctorId(AppointmentActor actor) {
        return doctorDirectory.findIdByUserId(actor.userId())
                .orElseThrow(() -> new AppointmentAccessDeniedException("El usuario no tiene perfil de medico"));
    }

    private Appointment findOrThrow(Long id) {
        return appointmentRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(id));
    }

    /** Paciente y medico solo acceden a sus propias citas; ADMIN a todas. */
    private void assertParticipantOrAdmin(Appointment appointment, AppointmentActor actor) {
        boolean allowed = switch (actor.role()) {
            case ADMIN -> true;
            case PATIENT -> patientDirectory.findByUserId(actor.userId())
                    .map(p -> p.id().equals(appointment.getPatientId())).orElse(false);
            case DOCTOR -> doctorDirectory.findIdByUserId(actor.userId())
                    .map(id -> id.equals(appointment.getDoctorId())).orElse(false);
        };
        if (!allowed) {
            throw new AppointmentAccessDeniedException("No tienes acceso a esta cita");
        }
    }

    private LocalDateTime normalize(LocalDateTime value) {
        return value.truncatedTo(ChronoUnit.MINUTES);
    }

    private void validateAdvance(LocalDateTime startAt) {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!startAt.isAfter(now)) {
            throw new AppointmentRuleException("No se puede reservar una cita en el pasado");
        }
        if (startAt.isBefore(now.plusMinutes(minAdvanceMinutes))) {
            throw new AppointmentRuleException(
                    "La cita debe reservarse con al menos " + minAdvanceMinutes + " minutos de anticipacion");
        }
    }

    private int slotMinutes(Long doctorId, LocalDateTime startAt) {
        return doctorAvailability.slotMinutesAt(doctorId, startAt)
                .orElseThrow(() -> new AppointmentRuleException(
                        "El horario seleccionado no esta disponible en la agenda del medico"));
    }

    private void assertSlotFree(Long doctorId, Long patientId,
                                LocalDateTime startAt, LocalDateTime endAt, Long excludeId) {
        boolean doctorBusy = excludeId == null
                ? appointmentRepository.existsDoctorOverlap(doctorId, startAt, endAt)
                : appointmentRepository.existsDoctorOverlapExcluding(doctorId, startAt, endAt, excludeId);
        if (doctorBusy) {
            throw new AppointmentConflictException("El medico ya tiene una cita en ese horario");
        }
        boolean patientBusy = excludeId == null
                ? appointmentRepository.existsPatientOverlap(patientId, startAt, endAt)
                : appointmentRepository.existsPatientOverlapExcluding(patientId, startAt, endAt, excludeId);
        if (patientBusy) {
            throw new AppointmentConflictException("El paciente ya tiene una cita en ese horario");
        }
    }

    /** La clave unica active_slot_key cubre la carrera entre dos reservas casi simultaneas. */
    private Appointment saveOrConflict(Appointment appointment) {
        try {
            return appointmentRepository.saveAndFlush(appointment);
        } catch (DataIntegrityViolationException ex) {
            throw new AppointmentConflictException("El horario acaba de ser reservado por otra persona");
        }
    }

    private AppointmentResponse toResponse(Appointment appointment) {
        PatientRef patient = patientDirectory.findById(appointment.getPatientId()).orElse(null);
        DoctorRef doctor = doctorDirectory.findById(appointment.getDoctorId()).orElse(null);
        return mapper.toResponse(appointment, patient, doctor);
    }
}