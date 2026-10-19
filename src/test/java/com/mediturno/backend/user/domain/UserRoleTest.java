package com.mediturno.backend.user.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class UserRoleTest {

    @Test
    void exposesSharedRolesForAllModules() {
        assertThat(UserRole.values())
                .containsExactly(UserRole.ADMIN, UserRole.DOCTOR, UserRole.PATIENT);
    }
}
