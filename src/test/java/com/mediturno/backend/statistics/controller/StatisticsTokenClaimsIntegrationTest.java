package com.mediturno.backend.statistics.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.repository.UserRepository;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
class StatisticsTokenClaimsIntegrationTest {

    private static final String DASHBOARD = "/api/v1/statistics/dashboard";
    private static final String APPOINTMENTS = "/api/v1/statistics/appointments";

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

    @Autowired
    private Clock clock;

    @Value("${app.jwt.secret}")
    private String testSecret;

    @Test
    void existingAdminTokenLosesAccessImmediatelyAfterDatabaseRoleChanges() throws Exception {
        String token = jwt.generateToken(user(9001));
        mockMvc.perform(get(DASHBOARD).header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        jdbc.update("UPDATE users SET role = 'DOCTOR' WHERE id = 9001");
        entityManager.clear();

        mockMvc.perform(get(DASHBOARD).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(DASHBOARD));
    }

    @Test
    void signedAdminRoleClaimCannotOverridePatientRoleStoredInDatabase() throws Exception {
        Instant now = clock.instant();
        String token = Jwts.builder()
                .subject(user(9002).getUsername())
                .claim("role", "ADMIN")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(300)))
                .signWith(testSigningKey())
                .compact();

        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.path").value(APPOINTMENTS));
    }

    @Test
    void expiredTokenForAnActiveAdminReturnsUniformUnauthorizedError() throws Exception {
        Instant now = clock.instant();
        String expiredToken = Jwts.builder()
                .subject(user(9001).getUsername())
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(testSigningKey())
                .compact();

        mockMvc.perform(get(DASHBOARD).header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(DASHBOARD))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    @Test
    void changedSignatureCannotAuthenticateAnOtherwiseValidAdminToken() throws Exception {
        String token = jwt.generateToken(user(9001));
        int signatureStart = token.lastIndexOf('.') + 1;
        char replacement = token.charAt(signatureStart) == 'A' ? 'B' : 'A';
        String alteredToken = token.substring(0, signatureStart)
                + replacement + token.substring(signatureStart + 1);

        mockMvc.perform(get(APPOINTMENTS).header("Authorization", "Bearer " + alteredToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value(APPOINTMENTS))
                .andExpect(jsonPath("$.details").isMap())
                .andExpect(jsonPath("$.trace").doesNotExist());
    }

    private User user(long id) {
        return users.findById(id).orElseThrow();
    }

    private SecretKey testSigningKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(testSecret));
    }
}
