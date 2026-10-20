package com.mediturno.backend.auth.controller;


import com.mediturno.backend.patient.repository.PatientRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;
import com.mediturno.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void registersPatientAndNeverReturnsPasswordHash() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pérez","documentNumber":"12345678","birthDate":"2000-01-01","phone":"987654321","email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.has("passwordHash")).isFalse();
        org.assertj.core.api.Assertions.assertThat(body.toString()).doesNotContain("clave-segura");
    }

    @Test
    void ignoresRoleSuppliedByPublicRegistrationClient() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pérez","documentNumber":"12345678","birthDate":"2000-01-01","phone":"987654321","email":"ana@example.com","password":"clave-segura","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void returnsConflictWhenEmailAlreadyExistsIgnoringCase() throws Exception {
        register("ANA@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Otra","lastName":"Persona","documentNumber":"12345678","birthDate":"2000-01-01","phone":"987654321","email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void returnsBadRequestForInvalidRegistrationData() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"","lastName":"Persona","email":"correo-inválido","password":"123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details").isMap());
    }

    @Test
    void returnsStandardErrorForMalformedJsonBody() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("El cuerpo de la solicitud no es válido."))
                .andExpect(jsonPath("$.path").value("/api/v1/auth/login"));
    }

    @Test
    void logsInWithRegisteredCredentials() throws Exception {
        register("ana@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ANA@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(3600))
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void returnsUnauthorizedForIncorrectPassword() throws Exception {
        register("ana@example.com");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"incorrecta"}
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas."));
    }

    @Test
    void refusesLoginForInactiveAccount() throws Exception {
        userRepository.saveAndFlush(User.create(
                "Ana", "Pérez", "ana@example.com", passwordEncoder.encode("clave-segura"),
                UserRole.PATIENT, false));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isUnauthorized());
    }

    private void register(String email) throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationPayload("Ana", "Perez", "12345678", "2000-01-01", "987654321", email, "clave-segura"))))
                .andExpect(status().isCreated());
    }

    private record RegistrationPayload(String firstName, String lastName, String documentNumber, String birthDate, String phone, String email, String password) {
    }
}
