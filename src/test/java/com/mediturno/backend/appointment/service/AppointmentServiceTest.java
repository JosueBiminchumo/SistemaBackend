package com.mediturno.backend.appointment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import com.mediturno.backend.appointment.domain.Appointment;
import com.mediturno.backend.appointment.domain.AppointmentStatus;
import com.mediturno.backend.appointment.dto.AppointmentPage;
import com.mediturno.backend.appointment.dto.AppointmentRequest;
import com.mediturno.backend.appointment.dto.AppointmentResponse;
import com.mediturno.backend.appointment.dto.AppointmentSearchCriteria;
import com.mediturno.backend.appointment.dto.AppointmentStatusRequest;
import com.mediturno.backend.appointment.dto.AppointmentUpdateRequest;
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
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AppointmentServiceTest {

    // "Ahora" = 2026-10-21 10:00 hora de Lima
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-21T15:00:00Z"), ZoneId.of("America/Lima"));
    private final LocalDateTime now = LocalDateTime.of(2026, 10, 21, 10, 0);
    private final LocalDateTime tomorrow9 = LocalDateTime.of(2026, 10, 22, 9, 0);
    private final LocalDateTime tomorrow930 = tomorrow9.plusMinutes(30);

    private final PatientRef patient = new PatientRef(10L, "Maria Lopez");
    private final DoctorRef doctor = new DoctorRef(5L, "Dr. Perez", "Medicina General", true);

    @Mock AppointmentRepository appointmentRepository;
    @Mock PatientDirectory patientDirectory;
    @Mock DoctorDirectory doctorDirectory;
    @Mock DoctorAvailability availability;
    @Mock AppointmentActorProvider actorProvider;

    private AppointmentService service;

    @BeforeEach
    void setUp() {
        service = new AppointmentService(appointmentRepository, new AppointmentMapper(), patientDirectory,
                doctorDirectory, availability, actorProvider, clock, 60);
        lenient().when(patientDirectory.findById(10L)).thenReturn(Optional.of(patient));
        lenient().when(doctorDirectory.findById(5L)).thenReturn(Optional.of(doctor));
    }

    private void actingAs(Long userId, ActorRole role) {
        when(actorProvider.current()).thenReturn(new AppointmentActor(userId, role));
    }

    private Appointment storedAppointment() {
        Appointment appointment = Appointment.create(10L, 5L, tomorrow9, tomorrow930, "Control", now);
        ReflectionTestUtils.setField(appointment, "id", 1L);
        return appointment;
    }

    // ---------- reservar ----------

    @Test
    void patientCreatesAppointmentForHimself() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(availability.slotMinutesAt(5L, tomorrow9)).thenReturn(OptionalInt.of(30));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response = service.create(new AppointmentRequest(5L, tomorrow9, " Control "));

        assertEquals(10L, response.patientId());
        assertEquals(AppointmentStatus.PENDING, response.status());
        assertEquals(tomorrow930, response.endAt());
        assertEquals("Control", response.reason());
    }

    @Test
    void createInThePastIsRejected() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));

        assertThrows(AppointmentRuleException.class, () ->
                service.create(new AppointmentRequest(5L, LocalDateTime.of(2026, 10, 20, 9, 0), "Control")));
    }

    @Test
    void createWithoutMinimumAdvanceIsRejected() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));

        assertThrows(AppointmentRuleException.class, () ->
                service.create(new AppointmentRequest(5L, LocalDateTime.of(2026, 10, 21, 10, 30), "Control")));
    }

    @Test
    void createOutsideDoctorScheduleIsRejected() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(availability.slotMinutesAt(5L, tomorrow9)).thenReturn(OptionalInt.empty());

        assertThrows(AppointmentRuleException.class, () ->
                service.create(new AppointmentRequest(5L, tomorrow9, "Control")));
    }

    @Test
    void createOverlappingDoctorAppointmentIsConflict() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(availability.slotMinutesAt(5L, tomorrow9)).thenReturn(OptionalInt.of(30));
        when(appointmentRepository.existsDoctorOverlap(5L, tomorrow9, tomorrow930)).thenReturn(true);

        assertThrows(AppointmentConflictException.class, () ->
                service.create(new AppointmentRequest(5L, tomorrow9, "Control")));
    }

    @Test
    void createLosingTheRaceOnTheUniqueKeyIsConflict() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(availability.slotMinutesAt(5L, tomorrow9)).thenReturn(OptionalInt.of(30));
        when(appointmentRepository.saveAndFlush(any(Appointment.class)))
                .thenThrow(new DataIntegrityViolationException("uk_citas_active_slot"));

        assertThrows(AppointmentConflictException.class, () ->
                service.create(new AppointmentRequest(5L, tomorrow9, "Control")));
    }

    @Test
    void doctorCannotCreateAppointments() {
        actingAs(200L, ActorRole.DOCTOR);

        assertThrows(AppointmentAccessDeniedException.class, () ->
                service.create(new AppointmentRequest(5L, tomorrow9, "Control")));
    }

    // ---------- reprogramar ----------

    @Test
    void doctorCannotReschedule() {
        actingAs(200L, ActorRole.DOCTOR);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));

        assertThrows(AppointmentAccessDeniedException.class, () ->
                service.reschedule(1L, new AppointmentUpdateRequest(tomorrow9.plusHours(1), null)));
    }

    @Test
    void rescheduleToBusySlotIsConflict() {
        LocalDateTime newStart = tomorrow9.plusHours(1);
        actingAs(100L, ActorRole.PATIENT);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(availability.slotMinutesAt(5L, newStart)).thenReturn(OptionalInt.of(30));
        when(appointmentRepository.existsDoctorOverlapExcluding(5L, newStart, newStart.plusMinutes(30), 1L))
                .thenReturn(true);

        assertThrows(AppointmentConflictException.class, () ->
                service.reschedule(1L, new AppointmentUpdateRequest(newStart, null)));
    }

    // ---------- cancelar y estados ----------

    @Test
    void patientCancelsOwnAppointment() {
        actingAs(100L, ActorRole.PATIENT);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(AppointmentStatus.CANCELLED, service.cancel(1L).status());
    }

    @Test
    void patientCannotCancelSomeoneElsesAppointment() {
        actingAs(101L, ActorRole.PATIENT);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));
        when(patientDirectory.findByUserId(101L)).thenReturn(Optional.of(new PatientRef(11L, "Otra Persona")));

        assertThrows(AppointmentAccessDeniedException.class, () -> service.cancel(1L));
    }

    @Test
    void cancelUnknownAppointmentIsNotFound() {
        when(appointmentRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(AppointmentNotFoundException.class, () -> service.cancel(99L));
    }

    @Test
    void doctorCannotCompleteAFutureAppointment() {
        actingAs(200L, ActorRole.DOCTOR);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));
        when(doctorDirectory.findIdByUserId(200L)).thenReturn(Optional.of(5L));

        assertThrows(AppointmentRuleException.class, () ->
                service.changeStatus(1L, new AppointmentStatusRequest(AppointmentStatus.COMPLETED)));
    }

    @Test
    void doctorConfirmsPendingAppointment() {
        actingAs(200L, ActorRole.DOCTOR);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));
        when(doctorDirectory.findIdByUserId(200L)).thenReturn(Optional.of(5L));
        when(appointmentRepository.saveAndFlush(any(Appointment.class))).thenAnswer(inv -> inv.getArgument(0));

        AppointmentResponse response =
                service.changeStatus(1L, new AppointmentStatusRequest(AppointmentStatus.CONFIRMED));

        assertEquals(AppointmentStatus.CONFIRMED, response.status());
    }

    @Test
    void patientCannotChangeStatusDirectly() {
        actingAs(100L, ActorRole.PATIENT);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(storedAppointment()));

        assertThrows(AppointmentAccessDeniedException.class, () ->
                service.changeStatus(1L, new AppointmentStatusRequest(AppointmentStatus.CONFIRMED)));
    }

    @Test
    void cancelledAppointmentCannotBeCancelledAgain() {
        actingAs(1L, ActorRole.ADMIN);
        Appointment appointment = storedAppointment();
        appointment.changeStatus(AppointmentStatus.CANCELLED, now);
        when(appointmentRepository.findById(1L)).thenReturn(Optional.of(appointment));

        assertThrows(AppointmentConflictException.class, () -> service.cancel(1L));
    }

    // ---------- listar ----------

    @Test
    void patientListIsForcedToOwnAppointments() {
        actingAs(100L, ActorRole.PATIENT);
        when(patientDirectory.findByUserId(100L)).thenReturn(Optional.of(patient));
        when(appointmentRepository.search(eq(10L), isNull(), isNull(), isNull(), isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // aunque pida las de otro paciente (patientId=99), se usa el suyo
        AppointmentPage page = service.list(new AppointmentSearchCriteria(null, 99L, null, null, null), 0, 20);

        assertEquals(0, page.totalElements());
    }
}