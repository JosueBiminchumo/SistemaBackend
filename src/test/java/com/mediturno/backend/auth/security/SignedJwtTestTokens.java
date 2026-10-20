package com.mediturno.backend.auth.security;

import java.time.Instant;
import java.util.Base64;
import java.util.Date;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

final class SignedJwtTestTokens {

    private static final String TEST_SECRET =
            "dGVzdC1zZWNyZXQtdGhhdC1pcy1hdC1sZWFzdC0zMi1ieXRlcy1sb25n";

    private SignedJwtTestTokens() {
    }

    static String expiredTokenFor(String subject) {
        Instant expiredAt = Instant.now().minusSeconds(30);
        return Jwts.builder()
                .subject(subject)
                .issuedAt(Date.from(expiredAt.minusSeconds(30)))
                .expiration(Date.from(expiredAt))
                .signWith(Keys.hmacShaKeyFor(Decoders.BASE64.decode(TEST_SECRET)))
                .compact();
    }

    static String tamperSignature(String token) {
        String[] parts = token.split("\\.");
        String firstCharacter = parts[2].startsWith("A") ? "B" : "A";
        parts[2] = firstCharacter + parts[2].substring(1);
        return String.join(".", parts);
    }
}
