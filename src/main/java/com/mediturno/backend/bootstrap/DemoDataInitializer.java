package com.mediturno.backend.bootstrap;

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.mediturno.backend.doctor.domain.Doctor;
import com.mediturno.backend.doctor.domain.DoctorSchedule;
import com.mediturno.backend.doctor.repository.DoctorRepository;
import com.mediturno.backend.doctor.repository.ScheduleRepository;
import com.mediturno.backend.specialty.domain.Specialty;
import com.mediturno.backend.specialty.repository.SpecialtyRepository;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import com.mediturno.backend.user.repository.UserRepository;

/**
 * Datos de demostracion SOLO con el perfil "local" (SPRING_PROFILES_ACTIVE=local).
 * Crea especialidades, 3 medicos con agenda lunes-viernes y un admin demo.
 * La clave demo sale de app.demo.password (DEMO_PASSWORD). Si esta vacia no hace nada.
 */
@Component
@Profile("local")
@Order(20)
public class DemoDataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataInitializer.class);

    private final SpecialtyRepository specialtyRepository;
    private final DoctorRepository doctorRepository;
    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    public DemoDataInitializer(
            SpecialtyRepository specialtyRepository,
            DoctorRepository doctorRepository,
            ScheduleRepository scheduleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.demo.password:}") String demoPassword) {
        this.specialtyRepository = specialtyRepository;
        this.doctorRepository = doctorRepository;
        this.scheduleRepository = scheduleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (demoPassword.isBlank() || specialtyRepository.count() > 0) {
            return;
        }
        Specialty cardio = specialtyRepository.save(new Specialty("Cardiología", "Corazón y sistema circulatorio", true));
        Specialty pedia = specialtyRepository.save(new Specialty("Pediatría", "Salud infantil", true));
        Specialty derma = specialtyRepository.save(new Specialty("Dermatología", "Piel, cabello y uñas", true));
        specialtyRepository.save(new Specialty("Medicina General", "Atención primaria", true));

        createDoctor("Carlos", "Mendoza", "carlos.mendoza@mediturn.pe", cardio, "CMP-10001",
                LocalTime.of(8, 0), LocalTime.of(13, 0), 30);
        createDoctor("Ana", "López", "ana.lopez@mediturn.pe", pedia, "CMP-10002",
                LocalTime.of(9, 0), LocalTime.of(14, 0), 30);
        createDoctor("Luis", "Torres", "luis.torres@mediturn.pe", derma, "CMP-10003",
                LocalTime.of(14, 0), LocalTime.of(18, 0), 20);

        if (!userRepository.existsByEmail("admin@mediturn.pe")) {
            userRepository.save(User.create("Admin", "MediTurn", "admin@mediturn.pe",
                    passwordEncoder.encode(demoPassword), UserRole.ADMIN, true));
        }
        log.info("Datos demo creados (perfil local). Usuarios: admin@mediturn.pe y los médicos @mediturn.pe");
    }

    private void createDoctor(String first, String last, String email, Specialty specialty,
            String license, LocalTime start, LocalTime end, int slotMinutes) {
        User user = userRepository.save(User.create(
                first, last, email, passwordEncoder.encode(demoPassword), UserRole.DOCTOR, true));
        Doctor doctor = doctorRepository.save(new Doctor(user, specialty, license, "Médico de " + specialty.getName() + "."));
        List<DoctorSchedule> rules = List.of(
                DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY)
                .stream()
                .map(d -> new DoctorSchedule(doctor, d, start, end, slotMinutes, true))
                .toList();
        scheduleRepository.saveAll(rules);
    }
}
