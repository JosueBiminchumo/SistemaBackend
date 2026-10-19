package com.mediturno.backend.user.repository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.mediturno.backend.user.domain.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findsUserAndChecksDuplicateEmail() {
        User saved = userRepository.saveAndFlush(
                User.createPatient("Ana", "Pérez", "ana@example.com", "bcrypt-hash"));

        assertThat(userRepository.findByEmail("ana@example.com")).contains(saved);
        assertThat(userRepository.existsByEmail("ana@example.com")).isTrue();
        assertThat(userRepository.existsByEmail("missing@example.com")).isFalse();
    }

    @Test
    void persistsCreationAndUpdateTimestamps() {
        User saved = userRepository.saveAndFlush(
                User.createPatient("Ana", "Pérez", "ana@example.com", "bcrypt-hash"));

        assertThat(saved.getCreatedAt()).isNotNull();
        assertThat(saved.getUpdatedAt()).isEqualTo(saved.getCreatedAt());
    }

    @Test
    void databaseRejectsDuplicateEmail() {
        userRepository.saveAndFlush(User.createPatient("Ana", "Pérez", "ana@example.com", "hash"));

        assertThatThrownBy(() -> userRepository.saveAndFlush(
                User.createPatient("Otra", "Persona", "ana@example.com", "hash")))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
