package com.mediturno.backend.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import com.mediturno.backend.auth.dto.AuthResponse;
import com.mediturno.backend.auth.dto.LoginRequest;
import com.mediturno.backend.auth.dto.RegisterRequest;
import com.mediturno.backend.auth.exception.EmailAlreadyRegisteredException;
import com.mediturno.backend.auth.exception.RegistrationDisabledException;
import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import com.mediturno.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepository, passwordEncoder, authenticationManager, jwtService, true);
    }

    @Test
    void registersPatientWithNormalizedEmailAndEncodedPassword() {
        RegisterRequest request = new RegisterRequest(" Ana ", " Pérez ", "ANA@EXAMPLE.COM ", "clave-segura");
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtService.generateToken(any(User.class))).thenReturn("jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(3_600_000L);

        AuthResponse response = authService.register(request);

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.expiresIn()).isEqualTo(3600);
        assertThat(response.user().email()).isEqualTo("ana@example.com");
        assertThat(response.user().role()).isEqualTo(UserRole.PATIENT);
        verify(passwordEncoder).encode("clave-segura");
    }

    @Test
    void rejectsAnAlreadyRegisteredEmailBeforeEncodingPassword() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "Pérez", "ANA@example.com", "clave-segura")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
        verify(passwordEncoder, never()).encode(any());
        verify(userRepository, never()).saveAndFlush(any(User.class));
    }

    @Test
    void convertsConcurrentDuplicateEmailInsertIntoConflict() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "unique email",
                        new org.hibernate.exception.ConstraintViolationException(
                                "unique email", null, "uk_users_email")));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "Pérez", "ana@example.com", "clave-segura")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);
    }

    @Test
    void doesNotMisreportUnrelatedDatabaseConstraintFailuresAsDuplicateEmails() {
        when(userRepository.existsByEmail("ana@example.com")).thenReturn(false);
        when(passwordEncoder.encode("clave-segura")).thenReturn("bcrypt-hash");
        when(userRepository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException(
                        "other constraint",
                        new org.hibernate.exception.ConstraintViolationException(
                                "other constraint", null, "chk_users_role")));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "Pérez", "ana@example.com", "clave-segura")))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("other constraint");
    }

    @Test
    void refusesRegistrationWhenPublicRegistrationIsDisabled() {
        AuthService disabledService = new AuthService(
                userRepository, passwordEncoder, authenticationManager, jwtService, false);

        assertThatThrownBy(() -> disabledService.register(
                new RegisterRequest("Ana", "Pérez", "ana@example.com", "clave-segura")))
                .isInstanceOf(RegistrationDisabledException.class);
        verify(userRepository, never()).existsByEmail(any());
    }

    @Test
    void authenticatesCredentialsBeforeReturningToken() {
        User user = User.createPatient("Ana", "Pérez", "ana@example.com", "bcrypt-hash");
        when(userRepository.findByEmail("ana@example.com")).thenReturn(Optional.of(user));
        when(authenticationManager.authenticate(any())).thenReturn(
                org.springframework.security.authentication.UsernamePasswordAuthenticationToken.authenticated(
                        user, null, user.getAuthorities()));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");
        when(jwtService.getExpirationMillis()).thenReturn(3_600_000L);

        AuthResponse response = authService.login(new LoginRequest("ANA@example.com", "clave-segura"));

        assertThat(response.accessToken()).isEqualTo("jwt-token");
        assertThat(response.user().email()).isEqualTo("ana@example.com");
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void doesNotIssueTokenForIncorrectCredentials() {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad credentials"));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("ana@example.com", "incorrecta")))
                .isInstanceOf(BadCredentialsException.class);
        verify(jwtService, never()).generateToken(any());
    }
}
