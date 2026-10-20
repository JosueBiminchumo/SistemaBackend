package com.mediturno.backend.auth.validation;

import java.nio.charset.StandardCharsets;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class BcryptCompatiblePasswordValidator
        implements ConstraintValidator<BcryptCompatiblePassword, String> {

    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        return password == null || password.getBytes(StandardCharsets.UTF_8).length <= 72;
    }
}
