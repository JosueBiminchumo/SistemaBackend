package com.mediturno.backend.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import org.junit.jupiter.api.Test;

class UserSummaryTest {

    @Test
    void exposesOnlyPublicAccountFields() {
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "private-bcrypt-hash");

        UserSummary summary = UserSummary.from(user);

        assertThat(summary)
                .extracting(UserSummary::firstName, UserSummary::lastName, UserSummary::email, UserSummary::role)
                .containsExactly("Ana", "Pérez", "ana@example.com", UserRole.PATIENT);
        assertThat(summary.toString()).doesNotContain("private-bcrypt-hash", "password");
    }
}
