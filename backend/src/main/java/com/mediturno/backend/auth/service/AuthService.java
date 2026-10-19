package com.mediturno.backend.auth.service;

import java.util.Locale;

import com.mediturno.backend.auth.dto.AuthResponse;
import com.mediturno.backend.auth.dto.LoginRequest;
import com.mediturno.backend.auth.dto.RegisterRequest;
import com.mediturno.backend.auth.dto.UserSummary;
import com.mediturno.backend.auth.exception.EmailAlreadyRegisteredException;
import com.mediturno.backend.auth.exception.RegistrationDisabledException;
import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final boolean registrationEnabled;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            JwtService jwtService,
            @Value("${app.auth.registration-enabled:true}") boolean registrationEnabled) {
        this.userRepository = userRepository;
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

        User patient = User.createPatient(
                request.firstName().trim(),
                request.lastName().trim(),
                email,
                passwordEncoder.encode(request.password()));
        try {
            return createResponse(userRepository.saveAndFlush(patient));
        } catch (DataIntegrityViolationException exception) {
            throw new EmailAlreadyRegisteredException();
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
}
