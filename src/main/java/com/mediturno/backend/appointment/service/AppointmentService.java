package com.mediturno.backend.appointment.service;
import com.mediturno.backend.appointment.exception.AppointmentConflictException;
import org.springframework.dao.DataIntegrityViolationException;
import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.exception.AppointmentAccessDeniedException;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AppointmentService {

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

    // ---------- helpers ----------

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

    private PatientRef requirePatient(AppointmentActor actor) {
        return patientDirectory.findByUserId(actor.userId())
                .orElseThrow(() -> new AppointmentAccessDeniedException("El usuario no tiene perfil de paciente"));
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

    private AppointmentResponse toResponse(Appointment appointment) {
        PatientRef patient = patientDirectory.findById(appointment.getPatientId()).orElse(null);
        DoctorRef doctor = doctorDirectory.findById(appointment.getDoctorId()).orElse(null);
        return mapper.toResponse(appointment, patient, doctor);
    }
}