package com.mediturno.backend.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Set;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class RegisterRequestValidationTest {

    private static Validator validator;
    private static jakarta.validation.ValidatorFactory validatorFactory;

    @BeforeAll
    static void createValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        validatorFactory.close();
    }

    private RegisterRequest validRequest(String email, String password) {
        return new RegisterRequest(
                "Ana",
                "Pérez",
                "12345678",
                LocalDate.of(2000, 1, 1),
                "987654321",
                email,
                password);
    }

    @Test
    void acceptsValidPatientRegistrationData() {
        RegisterRequest request = validRequest("ana@example.com", "clave-segura");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsWeakPasswordAndMalformedEmail() {
        RegisterRequest request = validRequest("no-es-email", "123");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains("email", "password");
    }

    @Test
    void rejectsPasswordWhoseUtf8BytesExceedBcryptLimit() {
        String password = "á".repeat(37);
        RegisterRequest request = validRequest("ana@example.com", password);

        Set<String> invalidFields = validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());

        assertThat(invalidFields).contains("password");
    }
}
