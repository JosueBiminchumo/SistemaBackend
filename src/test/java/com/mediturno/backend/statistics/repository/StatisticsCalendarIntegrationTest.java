package com.mediturno.backend.statistics.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediturno.backend.statistics.service.StatisticsRange;
import java.time.LocalDate;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
class StatisticsCalendarIntegrationTest {

    @Autowired
    private StatisticsRepository repository;

    @Autowired
    private JdbcTemplate jdbc;

    @ParameterizedTest
    @ValueSource(strings = {"1000-01-01", "1500-02-28", "1582-10-04", "1582-10-10",
            "1582-10-15", "1970-01-01", "2024-02-29", "9999-12-30"})
    void preservesSupportedIsoDatesWithoutCalendarCutover(String label) {
        LocalDate day = LocalDate.parse(label);
        jdbc.update("DELETE FROM citas");
        jdbc.update("""
                INSERT INTO citas (id, patient_id, doctor_id, start_at, end_at, status, reason,
                    active_slot_key, created_at, updated_at, version)
                VALUES (6001, 9002, 101, ?, ?, 'PENDING', 'Calendar boundary',
                    NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0)
                """, day.atTime(12, 0), day.atTime(12, 30));

        StatisticsRange range = new StatisticsRange(day, day);
        assertThat(repository.findTotals(range).appointments()).isEqualTo(1);
        assertThat(repository.countByDay(range)).containsExactly(new StatisticsBucket(label, 1));
    }
}
