package com.mediturno.backend.doctor.service;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.dto.DoctorAvailabilityResponse;
import com.mediturno.backend.doctor.exception.DoctorNotFoundException;
import com.mediturno.backend.doctor.exception.ScheduleNotAvailableException;
import com.mediturno.backend.doctor.mapper.DoctorMapper;
import com.mediturno.backend.doctor.repository.ScheduleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ScheduleServiceTest {

    private static final Long DOCTOR_ID = 1L;
    private static final LocalDate MONDAY_DATE =
            LocalDate.of(2026, 10, 19);

    @Mock
    private ScheduleRepository scheduleRepository;

    @Mock
    private DoctorService doctorService;

    @Mock
    private DoctorMapper doctorMapper;

    @InjectMocks
    private ScheduleService scheduleService;

    private Doctor doctor;

    @BeforeEach
    void setUp() {
        doctor = Doctor.builder()
                .id(DOCTOR_ID)
                .userId(10L)
                .specialtyId(20L)
                .licenseNumber("CMP-45821")
                .biography("Médico especialista")
                .active(true)
                .build();
    }

    @Test
    void shouldThrowExceptionWhenDoctorDoesNotExist() {
        when(doctorService.getActiveDoctor(99L))
                .thenThrow(new DoctorNotFoundException(99L));

        assertThrows(
                DoctorNotFoundException.class,
                () -> scheduleService.findAvailability(
                        99L,
                        MONDAY_DATE
                )
        );

        verify(doctorService).getActiveDoctor(99L);
    }

    @Test
    void shouldThrowExceptionWhenDayHasNoSchedule() {
        when(doctorService.getActiveDoctor(DOCTOR_ID))
                .thenReturn(doctor);

        when(scheduleRepository
                .findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        DOCTOR_ID,
                        DayOfWeek.MONDAY
                ))
                .thenReturn(List.of());

        assertThrows(
                ScheduleNotAvailableException.class,
                () -> scheduleService.findAvailability(
                        DOCTOR_ID,
                        MONDAY_DATE
                )
        );
    }

    @Test
    void shouldGenerateValidAvailableSlots() {
        DoctorSchedule schedule = DoctorSchedule.builder()
                .id(1L)
                .doctor(doctor)
                .dayOfWeek(DayOfWeek.MONDAY)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(10, 0))
                .slotMinutes(30)
                .active(true)
                .build();

        when(doctorService.getActiveDoctor(DOCTOR_ID))
                .thenReturn(doctor);

        when(scheduleRepository
                .findAllByDoctorIdAndDayOfWeekAndActiveTrueOrderByStartTimeAsc(
                        DOCTOR_ID,
                        DayOfWeek.MONDAY
                ))
                .thenReturn(List.of(schedule));

        DoctorAvailabilityResponse response =
                scheduleService.findAvailability(
                        DOCTOR_ID,
                        MONDAY_DATE
                );

        assertEquals(DOCTOR_ID, response.doctorId());
        assertEquals(MONDAY_DATE, response.date());
        assertTrue(response.available());
        assertFalse(response.slots().isEmpty());

        assertEquals(
                List.of(
                        LocalTime.of(9, 0),
                        LocalTime.of(9, 30)
                ),
                response.slots()
        );
    }
}