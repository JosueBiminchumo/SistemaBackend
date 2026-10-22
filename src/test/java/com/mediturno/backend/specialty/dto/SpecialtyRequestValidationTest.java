package com.mediturno.backend.specialty.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;

class SpecialtyRequestValidationTest {

    private static Validator validator;
    private static ValidatorFactory validatorFactory;

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
    void acceptsValidRequest() {
        SpecialtyRequest request = new SpecialtyRequest("Cardiología", "Atención del corazón", true);
        Set<ConstraintViolation<SpecialtyRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void acceptsValidRequestWithoutDescriptionAndActive() {
        SpecialtyRequest request = new SpecialtyRequest("Pediatría", null, null);
        Set<ConstraintViolation<SpecialtyRequest>> violations = validator.validate(request);
        assertThat(violations).isEmpty();
    }

    @Test
    void rejectsBlankOrNullName() {
        SpecialtyRequest blankName = new SpecialtyRequest("   ", "Descripción", true);
        Set<ConstraintViolation<SpecialtyRequest>> blankViolations = validator.validate(blankName);
        assertThat(blankViolations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");

        SpecialtyRequest nullName = new SpecialtyRequest(null, "Descripción", true);
        Set<ConstraintViolation<SpecialtyRequest>> nullViolations = validator.validate(nullName);
        assertThat(nullViolations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void rejectsNameExceedingMaxLength() {
        String longName = "A".repeat(101);
        SpecialtyRequest request = new SpecialtyRequest(longName, "Descripción", true);
        Set<ConstraintViolation<SpecialtyRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("name");
    }

    @Test
    void rejectsDescriptionExceedingMaxLength() {
        String longDescription = "D".repeat(256);
        SpecialtyRequest request = new SpecialtyRequest("Neurología", longDescription, true);
        Set<ConstraintViolation<SpecialtyRequest>> violations = validator.validate(request);
        assertThat(violations)
                .extracting(v -> v.getPropertyPath().toString())
                .contains("description");
    }
}
