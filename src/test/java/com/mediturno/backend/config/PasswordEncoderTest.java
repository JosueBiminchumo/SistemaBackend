package com.mediturno.backend.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class PasswordEncoderTest {

    @Test
    void bcryptStoresAHashAndVerifiesTheOriginalPassword() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        String password = "una-clave-segura";

        String hash = encoder.encode(password);

        assertThat(hash).startsWith("$2");
        assertThat(hash).isNotEqualTo(password);
        assertThat(encoder.matches(password, hash)).isTrue();
        assertThat(encoder.matches("otra-clave", hash)).isFalse();
    }
}
