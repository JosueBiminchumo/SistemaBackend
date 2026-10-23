package com.mediturno.backend.statistics.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/statistics-fixture.sql")
@WithMockUser(roles = "ADMIN")
@Import(StatisticsApiIntegrationTest.FixedClockConfiguration.class)
class StatisticsApiIntegrationTest {

    private static final String BASE = "/api/v1/statistics/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbc;

    @Test
    void returnsDatabaseDashboardInsideSharedResponseEnvelope() throws Exception {
        mockMvc.perform(get(BASE + "dashboard").param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.timestamp").isNotEmpty())
                .andExpect(jsonPath("$.data.from").value("2026-10-01"))
                .andExpect(jsonPath("$.data.to").value("2026-10-23"))
                .andExpect(jsonPath("$.data.timeZone").value("UTC"))
                .andExpect(jsonPath("$.data.totalAppointments").value(4))
                .andExpect(jsonPath("$.data.totalPatients").value(2))
                .andExpect(jsonPath("$.data.totalDoctors").value(2))
                .andExpect(jsonPath("$.data.totalSpecialties").value(3))
                .andExpect(jsonPath("$.data.appointmentsByStatus.PENDING").value(1))
                .andExpect(jsonPath("$.data.appointmentsByStatus.CONFIRMED").value(1))
                .andExpect(jsonPath("$.data.appointmentsByStatus.CANCELLED").value(1))
                .andExpect(jsonPath("$.data.appointmentsByStatus.COMPLETED").value(1))
                .andExpect(jsonPath("$.data.passwordHash").doesNotExist());
    }

    @Test
    void returnsAlignedDailyValuesWithZeroesBetweenBothInclusiveBoundaries() throws Exception {
        mockMvc.perform(get(BASE + "appointments")
                        .param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("day"))
                .andExpect(jsonPath("$.data.labels.length()").value(23))
                .andExpect(jsonPath("$.data.values.length()").value(23))
                .andExpect(jsonPath("$.data.labels[0]").value("2026-10-01"))
                .andExpect(jsonPath("$.data.values[0]").value(1))
                .andExpect(jsonPath("$.data.values[1]").value(0))
                .andExpect(jsonPath("$.data.labels[14]").value("2026-10-15"))
                .andExpect(jsonPath("$.data.values[14]").value(2))
                .andExpect(jsonPath("$.data.labels[22]").value("2026-10-23"))
                .andExpect(jsonPath("$.data.values[22]").value(1));
    }

    @Test
    void returnsBackendStatusesWithStableChartOrder() throws Exception {
        mockMvc.perform(get(BASE + "appointments").param("groupBy", "STATUS")
                        .param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.groupBy").value("status"))
                .andExpect(jsonPath("$.data.labels[0]").value("PENDING"))
                .andExpect(jsonPath("$.data.labels[1]").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.labels[2]").value("CANCELLED"))
                .andExpect(jsonPath("$.data.labels[3]").value("COMPLETED"))
                .andExpect(jsonPath("$.data.values").value(org.hamcrest.Matchers.contains(1, 1, 1, 1)));
    }

    @Test
    void returnsSpecialtyCountsIncludingInactiveHistoryWithoutConvertingToPercentages() throws Exception {
        mockMvc.perform(get(BASE + "appointments").param("groupBy", "specialty")
                        .param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labels").value(org.hamcrest.Matchers.contains("Cardiologia", "Neurologia")))
                .andExpect(jsonPath("$.data.values").value(org.hamcrest.Matchers.contains(2, 2)));
    }

    @Test
    void defaultWindowUsesUtcTodayEvenWhenApplicationClockZoneIsLima() throws Exception {
        mockMvc.perform(get(BASE + "dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-09-24"))
                .andExpect(jsonPath("$.data.to").value("2026-10-23"))
                .andExpect(jsonPath("$.data.totalAppointments").value(5));
    }

    @Test
    void missingFromUsesThirtyDaysEndingAtRequestedTo() throws Exception {
        mockMvc.perform(get(BASE + "dashboard").param("to", "2026-10-15"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.from").value("2026-09-16"))
                .andExpect(jsonPath("$.data.to").value("2026-10-15"))
                .andExpect(jsonPath("$.data.totalAppointments").value(4));
    }

    @Test
    void missingToUsesUtcToday() throws Exception {
        mockMvc.perform(get(BASE + "dashboard").param("from", "2026-10-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.to").value("2026-10-23"))
                .andExpect(jsonPath("$.data.totalAppointments").value(4));
    }

    @Test
    void noAppointmentsReturnsZeroStatusCountsAndEmptySpecialtySeries() throws Exception {
        jdbc.update("DELETE FROM citas");

        mockMvc.perform(get(BASE + "dashboard").param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalAppointments").value(0))
                .andExpect(jsonPath("$.data.totalPatients").value(2))
                .andExpect(jsonPath("$.data.appointmentsByStatus.PENDING").value(0))
                .andExpect(jsonPath("$.data.appointmentsByStatus.CANCELLED").value(0));
        mockMvc.perform(get(BASE + "appointments").param("groupBy", "specialty")
                        .param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.labels").isEmpty())
                .andExpect(jsonPath("$.data.values").isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"dashboard", "appointments"})
    void rejectsReversedRangeWithUniformApiError(String endpoint) throws Exception {
        mockMvc.perform(get(BASE + endpoint).param("from", "2026-10-23").param("to", "2026-10-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(BASE + endpoint))
                .andExpect(jsonPath("$.message").isNotEmpty())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void rejectsMoreThan366InclusiveDays() throws Exception {
        mockMvc.perform(get(BASE + "dashboard").param("from", "2024-01-01").param("to", "2025-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @ParameterizedTest
    @ValueSource(strings = {"bad-date", "2026-02-30", "-999999999-01-01", "+999999999-12-31"})
    void rejectsInvalidOrExtremeDateWithUniformApiError(String date) throws Exception {
        mockMvc.perform(get(BASE + "dashboard").param("to", date))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(BASE + "dashboard"))
                .andExpect(jsonPath("$.details").isMap());
    }

    @ParameterizedTest
    @ValueSource(strings = {"month", "doctor", "day; DROP TABLE citas"})
    void rejectsUnknownGroupingWithoutExecutingItAsSql(String grouping) throws Exception {
        mockMvc.perform(get(BASE + "appointments").param("groupBy", grouping))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.path").value(BASE + "appointments"));
    }

    @Test
    void publishesBothOperationsInOpenApi() throws Exception {
        mockMvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paths['/api/v1/statistics/dashboard'].get").exists())
                .andExpect(jsonPath("$.paths['/api/v1/statistics/appointments'].get").exists());
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FixedClockConfiguration {

        @Bean
        @Primary
        Clock statisticsTestClock() {
            return Clock.fixed(Instant.parse("2026-10-23T02:30:00Z"), ZoneId.of("America/Lima"));
        }
    }
}
