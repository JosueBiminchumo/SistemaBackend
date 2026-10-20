package com.mediturno.backend.auth.service;

import java.util.Locale;

import com.mediturno.backend.auth.dto.AuthResponse;
import com.mediturno.backend.auth.dto.LoginRequest;
import com.mediturno.backend.auth.dto.RegisterRequest;
import com.mediturno.backend.auth.dto.UserSummary;
import com.mediturno.backend.auth.exception.EmailAlreadyRegisteredException;
import com.mediturno.backend.auth.exception.RegistrationDisabledException;
import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.patient.domain.Patient;
import com.mediturno.backend.patient.exception.DocumentNumberAlreadyRegisteredException;
import com.mediturno.backend.patient.repository.PatientRepository;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.hibernate.exception.ConstraintViolationException;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final boolean registrationEnabled;

    public AuthService(
            UserRepository userRepository,
            PatientRepository patientRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.auth.registration-enabled:true}") boolean registrationEnabled) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.registrationEnabled = registrationEnabled;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (!registrationEnabled) {
            throw new RegistrationDisabledException();
        }

        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }
        String documentNumber = request.documentNumber().trim();
        if (patientRepository.existsByDocumentNumber(documentNumber)) {
            throw new DocumentNumberAlreadyRegisteredException();
        }

        User user = User.createPatient(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        try {
            User savedUser = userRepository.saveAndFlush(user);
            patientRepository.saveAndFlush(Patient.create(
                    savedUser,
                    documentNumber,
                    request.birthDate(),
                    request.phone().trim()));
            return createResponse(savedUser);
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueConstraintViolation(exception)) {
                throw new EmailAlreadyRegisteredException();
            }
            if (isDocumentNumberUniqueConstraintViolation(exception)) {
                throw new DocumentNumberAlreadyRegisteredException();
            }
            throw exception;
        }
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalStateException("La cuenta autenticada ya no existe."));
        return createResponse(user);
    }

    private AuthResponse createResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMillis() / 1000,
                UserSummary.from(user));
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isEmailUniqueConstraintViolation(Throwable exception) {
        return hasConstraintViolation(exception, "uk_users_email");
    }

    private boolean isDocumentNumberUniqueConstraintViolation(Throwable exception) {
        return hasConstraintViolation(exception, "uk_patients_document_number");
    }

    private boolean hasConstraintViolation(Throwable exception, String constraintName) {
        for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
            if (cause instanceof ConstraintViolationException violation) {
                return constraintName.equalsIgnoreCase(violation.getConstraintName());
            }
        }
        return false;
    }
}
