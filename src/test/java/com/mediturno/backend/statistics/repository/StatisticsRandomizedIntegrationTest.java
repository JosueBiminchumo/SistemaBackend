package com.mediturno.backend.statistics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediturno.backend.config.ClockConfiguration;
import com.mediturno.backend.statistics.dto.AppointmentSeriesResponse;
import com.mediturno.backend.statistics.dto.DashboardStatisticsResponse;
import com.mediturno.backend.statistics.mapper.StatisticsMapper;
import com.mediturno.backend.statistics.service.StatisticsRange;
import com.mediturno.backend.statistics.service.StatisticsService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.jdbc.JdbcTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;

@JdbcTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({StatisticsRepository.class, StatisticsMapper.class,
        StatisticsService.class, ClockConfiguration.class})
@ActiveProfiles("test")
@Sql("/statistics-fixture.sql")
class StatisticsRandomizedIntegrationTest {

    private static final int APPOINTMENT_COUNT = 3_000;
    private static final int PATIENT_COUNT = 12;
    private static final int DOCTOR_COUNT = 8;
    private static final int SPECIALTY_COUNT = 6;
    private static final LocalDate FIRST_DATE = LocalDate.of(2026, 9, 1);
    private static final LocalDate LAST_DATE = FIRST_DATE.plusDays(59);
    private static final List<String> STATUSES =
            List.of("PENDING", "CONFIRMED", "CANCELLED", "COMPLETED");
    private static final Comparator<StatisticsBucket> SPECIALTY_ORDER =
            Comparator.comparingLong(StatisticsBucket::value).reversed()
                    .thenComparing(StatisticsBucket::label);

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private StatisticsRepository repository;

    @Autowired
    private StatisticsService service;

    private final List<GeneratedAppointment> appointments = new ArrayList<>();

    @BeforeEach
    void generateDeterministicAppointmentsUsingParameterizedBatchInserts() {
        jdbc.update("DELETE FROM citas");
        LocalDateTime registeredAt = LocalDateTime.of(2025, 1, 1, 12, 0);
        List<Long> patientIds = new ArrayList<>(List.of(9002L, 9003L));
        for (int index = 0; index < 10; index++) {
            long userId = 10_000L + index;
            insertUser(userId, "PATIENT", true, registeredAt);
            jdbc.update("""
                    INSERT INTO patients
                        (user_id, document_number, birth_date, phone, created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, userId, Integer.toString(70_000_000 + index),
                    LocalDate.of(1990, 1, 1).plusDays(index), "999100" + index,
                    registeredAt, registeredAt);
            patientIds.add(userId);
        }

        insertSpecialty(41, "Dermatologia", true, registeredAt);
        insertSpecialty(42, "Traumatologia", false, registeredAt);
        insertSpecialty(43, "Oftalmologia", true, registeredAt);

        List<DoctorSeed> doctors = new ArrayList<>(List.of(
                new DoctorSeed(101, "Cardiologia", true),
                new DoctorSeed(202, "Neurologia", false)));
        long[] specialtyIds = {41, 42, 43, 32, 31, 41};
        String[] specialtyNames = {"Dermatologia", "Traumatologia", "Oftalmologia",
                "Cardiologia", "Neurologia", "Dermatologia"};
        for (int index = 0; index < 6; index++) {
            long userId = 20_000L + index;
            long doctorId = 400L + index;
            boolean active = index % 2 == 0;
            insertUser(userId, "DOCTOR", active, registeredAt);
            jdbc.update("""
                    INSERT INTO doctors
                        (id, user_id, specialty_id, license_number, biography, active)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """, doctorId, userId, specialtyIds[index],
                    "STRESS-LICENSE-" + index, "private-doctor-biography-" + index, active);
            doctors.add(new DoctorSeed(doctorId, specialtyNames[index], active));
        }

        appointments.clear();
        List<Object[]> rows = new ArrayList<>();
        Random random = new Random(0x5EED2026L);
        for (int index = 0; index < APPOINTMENT_COUNT; index++) {
            // Only alternate days receive random rows, preserving genuine empty days.
            LocalDateTime start = FIRST_DATE.plusDays(random.nextInt(30) * 2L)
                    .atStartOfDay().plusSeconds(random.nextInt(86_400));
            if (index == 0) {
                start = FIRST_DATE.atStartOfDay();
            } else if (index == 1) {
                start = LAST_DATE.atTime(23, 59, 59);
            }
            DoctorSeed doctor = index == 2 ? doctors.get(1)
                    : doctors.get(random.nextInt(doctors.size()));
            long patientId = patientIds.get(random.nextInt(patientIds.size()));
            String status = STATUSES.get(random.nextInt(STATUSES.size()));
            appointments.add(new GeneratedAppointment(patientId, doctor.id(),
                    doctor.specialty(), doctor.active(), start, status));
            rows.add(new Object[]{100_000L + index, patientId, doctor.id(), start,
                    start.plusMinutes(15), status, "private-appointment-reason-" + index,
                    registeredAt, registeredAt});
        }

        jdbc.batchUpdate("""
                INSERT INTO citas
                    (id, patient_id, doctor_id, start_at, end_at, status, reason,
                     active_slot_key, created_at, updated_at, version)
                VALUES (?, ?, ?, ?, ?, ?, ?, NULL, ?, ?, 0)
                """, rows);
    }

    @Test
    void matchesAnIndependentJavaOracleAcrossFixedAndRandomPeriods() {
        List<StatisticsRange> ranges = new ArrayList<>(List.of(
                new StatisticsRange(FIRST_DATE, LAST_DATE),
                new StatisticsRange(FIRST_DATE, FIRST_DATE),
                new StatisticsRange(LAST_DATE, LAST_DATE),
                new StatisticsRange(FIRST_DATE.plusDays(1), FIRST_DATE.plusDays(1)),
                new StatisticsRange(FIRST_DATE.minusDays(30), FIRST_DATE.minusDays(1)),
                new StatisticsRange(LAST_DATE.plusDays(1), LAST_DATE.plusDays(30)),
                new StatisticsRange(LocalDate.of(2026, 9, 15), LocalDate.of(2026, 10, 10)),
                new StatisticsRange(LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 23))));
        Random random = new Random(0xDA7E2026L);
        for (int index = 0; index < 20; index++) {
            int firstOffset = random.nextInt(80) - 10;
            int secondOffset = random.nextInt(80) - 10;
            ranges.add(new StatisticsRange(FIRST_DATE.plusDays(Math.min(firstOffset, secondOffset)),
                    FIRST_DATE.plusDays(Math.max(firstOffset, secondOffset))));
        }

        for (StatisticsRange range : ranges) {
            assertAggregationsMatchOracle(range);
        }
    }

    @Test
    void countsInactiveHistoricalRowsWithoutExposingPersonalSeedData() throws Exception {
        StatisticsRange range = new StatisticsRange(FIRST_DATE, LAST_DATE);
        long inactiveDoctorAppointments = appointments.stream()
                .filter(appointment -> !appointment.activeDoctor()).count();
        long inactiveSpecialtyAppointments = appointments.stream()
                .filter(appointment -> List.of("Neurologia", "Traumatologia")
                        .contains(appointment.specialty())).count();

        assertThat(inactiveDoctorAppointments).isPositive();
        assertThat(inactiveSpecialtyAppointments).isPositive();
        DashboardStatisticsResponse dashboard = service.dashboard(range.from(), range.to());
        assertThat(dashboard.totalAppointments()).isEqualTo(APPOINTMENT_COUNT);
        assertThat(dashboard.totalPatients()).isEqualTo(PATIENT_COUNT);
        assertThat(dashboard.totalDoctors()).isEqualTo(DOCTOR_COUNT);
        assertThat(dashboard.totalSpecialties()).isEqualTo(SPECIALTY_COUNT);
        assertThat(repository.countBySpecialty(range).stream()
                .filter(bucket -> List.of("Neurologia", "Traumatologia").contains(bucket.label()))
                .mapToLong(StatisticsBucket::value).sum()).isEqualTo(inactiveSpecialtyAppointments);
        assertThat(service.appointments("specialty", range.from(), range.to()).labels())
                .doesNotContain("Pediatria");

        assertOnlyAggregateFields(dashboard, List.of("from", "to", "timeZone",
                "totalAppointments", "totalPatients", "totalDoctors", "totalSpecialties",
                "appointmentsByStatus"));
        for (String grouping : List.of("day", "status", "specialty")) {
            assertOnlyAggregateFields(service.appointments(grouping, range.from(), range.to()),
                    List.of("groupBy", "from", "to", "timeZone", "labels", "values"));
        }
    }

    private void assertAggregationsMatchOracle(StatisticsRange range) {
        // The oracle reads the generated Java records, never SQL query results.
        List<GeneratedAppointment> selected = appointments.stream()
                .filter(appointment -> !appointment.startAt().toLocalDate().isBefore(range.from())
                        && !appointment.startAt().toLocalDate().isAfter(range.to()))
                .toList();
        Map<String, Long> byStatus = count(selected, GeneratedAppointment::status);
        Map<String, Long> byDay = count(selected,
                appointment -> appointment.startAt().toLocalDate().toString());
        Map<String, Long> bySpecialty = count(selected, GeneratedAppointment::specialty);
        List<StatisticsBucket> expectedStatuses = alphabeticBuckets(byStatus);
        List<StatisticsBucket> expectedDays = alphabeticBuckets(byDay);
        List<StatisticsBucket> expectedSpecialties = bySpecialty.entrySet().stream()
                .map(entry -> new StatisticsBucket(entry.getKey(), entry.getValue()))
                .sorted(SPECIALTY_ORDER).toList();

        assertThat(repository.findTotals(range)).as("totals for %s", range)
                .isEqualTo(new StatisticsTotals(selected.size(), PATIENT_COUNT,
                        DOCTOR_COUNT, SPECIALTY_COUNT));
        assertThat(repository.countByStatus(range)).as("statuses for %s", range)
                .containsExactlyElementsOf(expectedStatuses);
        assertThat(repository.countByDay(range)).as("days for %s", range)
                .containsExactlyElementsOf(expectedDays);
        assertThat(repository.countBySpecialty(range)).as("specialties for %s", range)
                .containsExactlyElementsOf(expectedSpecialties);

        Map<String, Long> expectedStatusResponse = new LinkedHashMap<>();
        STATUSES.forEach(status -> expectedStatusResponse.put(status, byStatus.getOrDefault(status, 0L)));
        DashboardStatisticsResponse dashboard = service.dashboard(range.from(), range.to());
        assertThat(dashboard.from()).isEqualTo(range.from());
        assertThat(dashboard.to()).isEqualTo(range.to());
        assertThat(dashboard.timeZone()).isEqualTo("UTC");
        assertThat(dashboard.totalAppointments()).isEqualTo(selected.size());
        assertThat(dashboard.totalPatients()).isEqualTo(PATIENT_COUNT);
        assertThat(dashboard.totalDoctors()).isEqualTo(DOCTOR_COUNT);
        assertThat(dashboard.totalSpecialties()).isEqualTo(SPECIALTY_COUNT);
        assertThat(dashboard.appointmentsByStatus()).containsExactlyEntriesOf(expectedStatusResponse);
        assertThat(dashboard.appointmentsByStatus().values().stream().mapToLong(Long::longValue).sum())
                .isEqualTo(selected.size());

        List<String> expectedDayLabels = range.from().datesUntil(range.to().plusDays(1))
                .map(LocalDate::toString).toList();
        assertSeries(service.appointments("day", range.from(), range.to()), "day", range,
                expectedDayLabels,
                expectedDayLabels.stream().map(label -> byDay.getOrDefault(label, 0L)).toList(),
                selected.size());
        assertSeries(service.appointments("status", range.from(), range.to()), "status", range,
                STATUSES, STATUSES.stream().map(status -> byStatus.getOrDefault(status, 0L)).toList(),
                selected.size());
        assertSeries(service.appointments("specialty", range.from(), range.to()), "specialty", range,
                expectedSpecialties.stream().map(StatisticsBucket::label).toList(),
                expectedSpecialties.stream().map(StatisticsBucket::value).toList(), selected.size());
    }

    private void assertSeries(AppointmentSeriesResponse response, String grouping,
            StatisticsRange range, List<String> labels, List<Long> values, long total) {
        assertThat(response.groupBy()).isEqualTo(grouping);
        assertThat(response.from()).isEqualTo(range.from());
        assertThat(response.to()).isEqualTo(range.to());
        assertThat(response.timeZone()).isEqualTo("UTC");
        assertThat(response.labels()).containsExactlyElementsOf(labels);
        assertThat(response.values()).containsExactlyElementsOf(values);
        assertThat(response.values()).allMatch(value -> value >= 0);
        assertThat(response.values().stream().mapToLong(Long::longValue).sum()).isEqualTo(total);
    }

    private void assertOnlyAggregateFields(Object response, List<String> expectedFields) throws Exception {
        ObjectMapper mapper = new ObjectMapper().findAndRegisterModules();
        String serialized = mapper.writeValueAsString(response);
        JsonNode json = mapper.readTree(serialized);
        List<String> fields = new ArrayList<>();
        json.fieldNames().forEachRemaining(fields::add);
        assertThat(fields).containsExactlyElementsOf(expectedFields);
        assertThat(serialized).doesNotContain("private-person-", "@stress.example.test",
                "statistics.admin@example.com", "statistics.patient.", "statistics.doctor.",
                "private-appointment-reason-", "private-doctor-biography-", "STRESS-LICENSE-",
                "70000000", "password", "patientId", "doctorId", "licenseNumber", "reason");
    }

    private void insertUser(long id, String role, boolean active, LocalDateTime registeredAt) {
        jdbc.update("""
                INSERT INTO users
                    (id, first_name, last_name, email, password_hash, role, active, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """, id, "private-person-" + id, "private-person-last-name",
                "user-" + id + "@stress.example.test", "unused-private-hash", role,
                active, registeredAt, registeredAt);
    }

    private void insertSpecialty(long id, String name, boolean active, LocalDateTime registeredAt) {
        jdbc.update("""
                INSERT INTO specialties (id, name, description, active, created_at)
                VALUES (?, ?, ?, ?, ?)
                """, id, name, "Statistics generated specialty", active, registeredAt);
    }

    private static Map<String, Long> count(List<GeneratedAppointment> selected,
            Function<GeneratedAppointment, String> key) {
        return selected.stream().collect(Collectors.groupingBy(key, Collectors.counting()));
    }

    private static List<StatisticsBucket> alphabeticBuckets(Map<String, Long> counts) {
        return new TreeMap<>(counts).entrySet().stream()
                .map(entry -> new StatisticsBucket(entry.getKey(), entry.getValue())).toList();
    }

    private record DoctorSeed(long id, String specialty, boolean active) {
    }

    private record GeneratedAppointment(long patientId, long doctorId, String specialty,
            boolean activeDoctor, LocalDateTime startAt, String status) {
    }
}
