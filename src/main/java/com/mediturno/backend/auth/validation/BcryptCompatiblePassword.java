package com.mediturno.backend.auth.validation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

@Documented
@Constraint(validatedBy = BcryptCompatiblePasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.ANNOTATION_TYPE, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface BcryptCompatiblePassword {

    String message() default "La contraseña no puede superar 72 bytes en UTF-8.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
