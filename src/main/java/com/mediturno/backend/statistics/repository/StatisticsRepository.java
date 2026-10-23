package com.mediturno.backend.statistics.repository;

import com.mediturno.backend.statistics.service.StatisticsRange;
import java.sql.Timestamp;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class StatisticsRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public StatisticsRepository(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public StatisticsTotals findTotals(StatisticsRange range) {
        return jdbc.queryForObject("""
                SELECT
                    (SELECT COUNT(*) FROM citas
                     WHERE start_at >= :startAt AND start_at < :endBefore) AS appointments,
                    (SELECT COUNT(*) FROM patients) AS patients,
                    (SELECT COUNT(*) FROM doctors) AS doctors,
                    (SELECT COUNT(*) FROM specialties) AS specialties
                """, parameters(range), (row, index) -> new StatisticsTotals(
                row.getLong("appointments"), row.getLong("patients"),
                row.getLong("doctors"), row.getLong("specialties")));
    }

    private MapSqlParameterSource parameters(StatisticsRange range) {
        return new MapSqlParameterSource()
                .addValue("startAt", Timestamp.valueOf(range.startAt()))
                .addValue("endBefore", Timestamp.valueOf(range.endBefore()));
    }
}
