package com.mediturno.backend.statistics.repository;

import com.mediturno.backend.statistics.service.StatisticsRange;
import java.sql.Timestamp;
import java.util.List;
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

    public List<StatisticsBucket> countByStatus(StatisticsRange range) {
        return jdbc.query("""
                SELECT status AS label, COUNT(*) AS total
                FROM citas
                WHERE start_at >= :startAt AND start_at < :endBefore
                GROUP BY status
                ORDER BY status
                """, parameters(range), (row, index) ->
                new StatisticsBucket(row.getString("label"), row.getLong("total")));
    }

    public List<StatisticsBucket> countByDay(StatisticsRange range) {
        return jdbc.query("""
                SELECT CAST(start_at AS DATE) AS bucket_date, COUNT(*) AS total
                FROM citas
                WHERE start_at >= :startAt AND start_at < :endBefore
                GROUP BY CAST(start_at AS DATE)
                ORDER BY bucket_date
                """, parameters(range), (row, index) -> new StatisticsBucket(
                row.getDate("bucket_date").toLocalDate().toString(), row.getLong("total")));
    }

    public List<StatisticsBucket> countBySpecialty(StatisticsRange range) {
        return jdbc.query("""
                SELECT s.name AS label, COUNT(*) AS total
                FROM citas a
                JOIN doctors d ON d.id = a.doctor_id
                JOIN specialties s ON s.id = d.specialty_id
                WHERE a.start_at >= :startAt AND a.start_at < :endBefore
                GROUP BY s.id, s.name
                ORDER BY total DESC, s.name ASC, s.id ASC
                """, parameters(range), (row, index) ->
                new StatisticsBucket(row.getString("label"), row.getLong("total")));
    }

    private MapSqlParameterSource parameters(StatisticsRange range) {
        return new MapSqlParameterSource()
                .addValue("startAt", Timestamp.valueOf(range.startAt()))
                .addValue("endBefore", Timestamp.valueOf(range.endBefore()));
    }
}
