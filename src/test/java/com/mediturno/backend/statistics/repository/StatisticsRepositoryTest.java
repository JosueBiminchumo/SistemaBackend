package com.mediturno.backend.statistics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediturno.backend.statistics.service.StatisticsRange;
import java.time.LocalDate;
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
@Import(StatisticsRepository.class)
@ActiveProfiles("test")
@Sql("/statistics-fixture.sql")
class StatisticsRepositoryTest {

    private static final StatisticsRange OCTOBER_RANGE =
            range("2026-10-01", "2026-10-23");

    @Autowired
    private StatisticsRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void countsAppointmentStartDatesWithinTheInclusiveRangeAndAllRegisteredEntities() {
        assertThat(repository.findTotals(OCTOBER_RANGE))
                .isEqualTo(new StatisticsTotals(4, 2, 2, 3));
    }

    @Test
    void groupsEveryStatusOnceAndOrdersLabelsDeterministically() {
        assertThat(repository.countByStatus(OCTOBER_RANGE)).containsExactly(
                new StatisticsBucket("CANCELLED", 1),
                new StatisticsBucket("COMPLETED", 1),
                new StatisticsBucket("CONFIRMED", 1),
                new StatisticsBucket("PENDING", 1));
    }

    @Test
    void aggregatesSharedDaysAndOrdersThemChronologically() {
        assertThat(repository.countByDay(OCTOBER_RANGE)).containsExactly(
                new StatisticsBucket("2026-10-01", 1),
                new StatisticsBucket("2026-10-15", 2),
                new StatisticsBucket("2026-10-23", 1));
    }

    @Test
    void joinsSpecialtiesThroughDoctorsAndKeepsInactiveHistoricalRecords() {
        assertThat(repository.countBySpecialty(OCTOBER_RANGE)).containsExactly(
                new StatisticsBucket("Cardiologia", 2),
                new StatisticsBucket("Neurologia", 2));
    }

    @Test
    void includesAnAppointmentAtTheFirstDayMidnight() {
        StatisticsRange firstDay = range("2026-10-01", "2026-10-01");

        assertThat(repository.findTotals(firstDay).appointments()).isEqualTo(1);
        assertThat(repository.countByDay(firstDay))
                .containsExactly(new StatisticsBucket("2026-10-01", 1));
        assertThat(repository.countByStatus(firstDay))
                .containsExactly(new StatisticsBucket("PENDING", 1));
    }

    @Test
    void includesTheLastDayFinalSecondButExcludesTheNextDayMidnight() {
        StatisticsRange lastDay = range("2026-10-23", "2026-10-23");

        assertThat(repository.findTotals(lastDay).appointments()).isEqualTo(1);
        assertThat(repository.countByDay(lastDay))
                .containsExactly(new StatisticsBucket("2026-10-23", 1));
        assertThat(repository.countByStatus(lastDay))
                .containsExactly(new StatisticsBucket("CANCELLED", 1));
    }

    @Test
    void appliesEachRequestedPeriodWithoutChangingGlobalEntityTotals() {
        StatisticsRange firstPeriod = range("2026-10-01", "2026-10-15");
        StatisticsRange secondPeriod = range("2026-10-16", "2026-10-23");

        assertThat(repository.findTotals(firstPeriod))
                .isEqualTo(new StatisticsTotals(3, 2, 2, 3));
        assertThat(repository.findTotals(secondPeriod))
                .isEqualTo(new StatisticsTotals(1, 2, 2, 3));
        assertThat(repository.countBySpecialty(firstPeriod)).containsExactly(
                new StatisticsBucket("Cardiologia", 2),
                new StatisticsBucket("Neurologia", 1));
        assertThat(repository.countBySpecialty(secondPeriod))
                .containsExactly(new StatisticsBucket("Neurologia", 1));
    }

    @Test
    void filtersStartAtRatherThanCreationTimestampOrAppointmentEnd() {
        // All four in-range appointments were created in September. The two
        // out-of-range appointments were created in October; one ends in range.
        assertThat(repository.findTotals(OCTOBER_RANGE).appointments()).isEqualTo(4);
        assertThat(repository.findTotals(range("2026-09-30", "2026-10-24")).appointments())
                .isEqualTo(6);
        assertThat(repository.countByDay(range("2026-09-30", "2026-09-30")))
                .containsExactly(new StatisticsBucket("2026-09-30", 1));
        assertThat(repository.countByDay(range("2026-10-24", "2026-10-24")))
                .containsExactly(new StatisticsBucket("2026-10-24", 1));
    }

    @Test
    void returnsEmptySeriesForAPeriodWithoutAppointmentsButRetainsEntityCounts() {
        StatisticsRange emptyPeriod = range("2026-11-01", "2026-11-30");

        assertThat(repository.findTotals(emptyPeriod))
                .isEqualTo(new StatisticsTotals(0, 2, 2, 3));
        assertThat(repository.countByDay(emptyPeriod)).isEmpty();
        assertThat(repository.countByStatus(emptyPeriod)).isEmpty();
        assertThat(repository.countBySpecialty(emptyPeriod)).isEmpty();
    }

    @Test
    void returnsZeroTotalsAndEmptySeriesWhenAllBusinessTablesAreEmpty() {
        jdbc.update("DELETE FROM citas");
        jdbc.update("DELETE FROM doctor_schedules");
        jdbc.update("DELETE FROM doctors");
        jdbc.update("DELETE FROM patients");
        jdbc.update("DELETE FROM specialties");
        jdbc.update("DELETE FROM users");

        assertThat(repository.findTotals(OCTOBER_RANGE))
                .isEqualTo(new StatisticsTotals(0, 0, 0, 0));
        assertThat(repository.countByDay(OCTOBER_RANGE)).isEmpty();
        assertThat(repository.countByStatus(OCTOBER_RANGE)).isEmpty();
        assertThat(repository.countBySpecialty(OCTOBER_RANGE)).isEmpty();
    }

    private static StatisticsRange range(String from, String to) {
        return new StatisticsRange(LocalDate.parse(from), LocalDate.parse(to));
    }
}
