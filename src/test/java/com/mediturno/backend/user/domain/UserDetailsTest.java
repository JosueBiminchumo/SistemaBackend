package com.mediturno.backend.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserDetailsTest {

    @Test
    void exposesEmailAndRoleToSpringSecurityWithoutChangingStoredHash() {
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "$2a$10$hash");

        assertThat(user.getUsername()).isEqualTo("ana@example.com");
        assertThat(user.getPassword()).isEqualTo("$2a$10$hash");
        assertThat(user.getAuthorities())
                .extracting("authority")
                .containsExactly("ROLE_PATIENT");
        assertThat(user.isEnabled()).isTrue();
    }

    @Test
    void marksInactiveAccountsDisabled() {
        User user = User.create("Ana", "Pérez", "ana@example.com", "hash", UserRole.PATIENT, false);

        assertThat(user.isEnabled()).isFalse();
    }
}
