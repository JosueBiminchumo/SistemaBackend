
package com.mediturno.backend.appointment.dev;

import com.mediturno.backend.appointment.port.DoctorAvailability;
import com.mediturno.backend.appointment.port.DoctorDirectory;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Optional;
import java.util.OptionalInt;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * SOLO DESARROLLO Y PRUEBAS. Perfiles: appointment-doctor-stub o test.
 * BORRAR cuando el Integrante 3 implemente DoctorDirectory y DoctorAvailability
 * (si no, habra dos beans del mismo tipo y la app no arrancara).
 * Simula que doctorId = userId del medico y que todos estan activos.
 */
@Configuration
@Profile({"appointment-doctor-stub", "test"})
public class AppointmentDoctorStubConfig {

    @Bean
    DoctorDirectory stubDoctorDirectory() {
        return new DoctorDirectory() {
            @Override
            public Optional<DoctorRef> findById(Long doctorId) {
                return Optional.of(new DoctorRef(doctorId, "Dr. " + doctorId, "Medicina General", true));
            }

            @Override
            public Optional<Long> findIdByUserId(Long userId) {
                return Optional.of(userId);
            }
        };
    }

    /** Lunes a viernes, 08:00-12:00 y 14:00-18:00, slots de 30 minutos. */
    @Bean
    DoctorAvailability stubDoctorAvailability() {
        return (doctorId, startAt) -> {
            DayOfWeek day = startAt.getDayOfWeek();
            if (day == DayOfWeek.SATURDAY || day == DayOfWeek.SUNDAY) {
                return OptionalInt.empty();
            }
            LocalTime t = startAt.toLocalTime();
            boolean morning = !t.isBefore(LocalTime.of(8, 0)) && t.isBefore(LocalTime.of(12, 0));
            boolean afternoon = !t.isBefore(LocalTime.of(14, 0)) && t.isBefore(LocalTime.of(18, 0));
            boolean aligned = t.getMinute() % 30 == 0 && t.getSecond() == 0;
            return (morning || afternoon) && aligned ? OptionalInt.of(30) : OptionalInt.empty();
        };
    }
}