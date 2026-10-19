package com.mediturno.backend.auth.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Base64;

import com.mediturno.backend.user.domain.User;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.Test;

class JwtServiceTest {

    private static final String SECRET = Base64.getEncoder().encodeToString(
            "clave-de-prueba-con-al-menos-treinta-y-dos-bytes".getBytes());

    @Test
    void generatesTokenForUserAndReadsSubject() {
        JwtService jwtService = new JwtService(SECRET, 60_000, new MutableClock(Instant.parse("2026-01-01T00:00:00Z")));
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "hash");

        String token = jwtService.generateToken(user);

        assertThat(jwtService.extractUsername(token)).isEqualTo("ana@example.com");
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void rejectsExpiredTokens() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        JwtService jwtService = new JwtService(SECRET, 60_000, clock);
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "hash");
        String token = jwtService.generateToken(user);
        clock.advanceSeconds(61);

        assertThatThrownBy(() -> jwtService.isTokenValid(token, user))
                .isInstanceOf(ExpiredJwtException.class);
    }

    @Test
    void rejectsTokensSignedWithAnotherSecret() {
        MutableClock clock = new MutableClock(Instant.parse("2026-01-01T00:00:00Z"));
        JwtService issuer = new JwtService(SECRET, 60_000, clock);
        JwtService verifier = new JwtService(
                Base64.getEncoder().encodeToString("otra-clave-de-prueba-con-al-menos-treinta-y-dos-bytes".getBytes()),
                60_000,
                clock);
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "hash");
        String token = issuer.generateToken(user);

        assertThatThrownBy(() -> verifier.extractUsername(token))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void refusesShortSigningKeys() {
        String shortKey = Base64.getEncoder().encodeToString("demasiado-corta".getBytes());

        assertThatThrownBy(() -> new JwtService(
                shortKey, 60_000, new MutableClock(Instant.parse("2026-01-01T00:00:00Z"))))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("256 bits");
    }
}
