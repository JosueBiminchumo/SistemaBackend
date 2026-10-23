package com.mediturno.backend.statistics.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.repository.UserRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
@Sql("/statistics-fixture.sql")
class StatisticsSecurityIntegrationTest {

    private static final String BASE = "/api/v1/statistics/";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository users;

    @Autowired
    private JwtService jwt;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private EntityManager entityManager;

    @ParameterizedTest
    @ValueSource(strings = {"dashboard", "appointments"})
    void rejectsRequestsWithoutToken(String endpoint) throws Exception {
        mockMvc.perform(get(BASE + endpoint))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(BASE + endpoint));
    }

    @ParameterizedTest
    @CsvSource({"dashboard, 9002", "dashboard, 9004", "appointments, 9002", "appointments, 9004"})
    void rejectsValidPatientAndDoctorTokens(String endpoint, long userId) throws Exception {
        mockMvc.perform(get(BASE + endpoint).header("Authorization", "Bearer " + tokenFor(userId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(BASE + endpoint));
    }

    @ParameterizedTest
    @ValueSource(strings = {"dashboard", "appointments"})
    void acceptsValidAdminTokenAndReadsPersistedData(String endpoint) throws Exception {
        mockMvc.perform(get(BASE + endpoint).header("Authorization", "Bearer " + tokenFor(9001))
                        .param("from", "2026-10-01").param("to", "2026-10-23"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.timeZone").value("UTC"));
    }

    @Test
    void refusesTokenWhenAdminAccountHasBeenDeactivated() throws Exception {
        String token = tokenFor(9001);
        jdbc.update("UPDATE users SET active = FALSE WHERE id = 9001");
        entityManager.clear();

        mockMvc.perform(get(BASE + "dashboard").header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refusesMalformedTokenBeforeAccessingStatistics() throws Exception {
        mockMvc.perform(get(BASE + "dashboard").header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private String tokenFor(long id) {
        User user = users.findById(id).orElseThrow();
        return jwt.generateToken(user);
    }
}
