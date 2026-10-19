package com.mediturno.backend.auth.dto;

import static org.assertj.core.api.Assertions.assertThat;

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

    @Test
    void acceptsValidPatientRegistrationData() {
        RegisterRequest request = new RegisterRequest("Ana", "Pérez", "ana@example.com", "clave-segura");

        assertThat(validator.validate(request)).isEmpty();
    }

    @Test
    void rejectsWeakPasswordAndMalformedEmail() {
        RegisterRequest request = new RegisterRequest("Ana", "Pérez", "no-es-email", "123");

        Set<String> invalidFields = validator.validate(request).stream()
                .map(error -> error.getPropertyPath().toString())
                .collect(java.util.stream.Collectors.toSet());
        assertThat(invalidFields).contains("email", "password");
    }

    @Test
    void rejectsPasswordWhoseUtf8BytesExceedBcryptLimit() {
        RegisterRequest request = new RegisterRequest(
                "Ana", "Pérez", "ana@example.com", "🔐".repeat(19));

        assertThat(validator.validate(request))
                .extracting(error -> error.getPropertyPath().toString())
                .contains("password");
    }
}
