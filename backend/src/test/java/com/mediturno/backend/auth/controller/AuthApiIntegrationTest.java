package com.mediturno.backend.auth.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediturno.backend.patient.repository.PatientRepository;
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
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

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
                                {"firstName":"Ana","lastName":"Pérez","documentNumber":"12345678","birthDate":"1998-05-20","phone":"987654321","email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"))
                .andExpect(jsonPath("$.user.email").value("ana@example.com"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(body.has("passwordHash")).isFalse();
        org.assertj.core.api.Assertions.assertThat(body.toString()).doesNotContain("clave-segura");
        org.assertj.core.api.Assertions.assertThat(patientRepository.findAll())
                .singleElement()
                .satisfies(patient -> {
                    org.assertj.core.api.Assertions.assertThat(patient.getUserId())
                            .isEqualTo(body.get("user").get("id").asLong());
                    org.assertj.core.api.Assertions.assertThat(patient.getDocumentNumber()).isEqualTo("12345678");
                    org.assertj.core.api.Assertions.assertThat(patient.getBirthDate().toString())
                            .isEqualTo("1998-05-20");
                    org.assertj.core.api.Assertions.assertThat(patient.getPhone()).isEqualTo("987654321");
                });
    }

    @Test
    void ignoresRoleSuppliedByPublicRegistrationClient() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pérez","documentNumber":"12345678","birthDate":"1998-05-20","phone":"987654321","email":"ana@example.com","password":"clave-segura","role":"ADMIN"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.user.role").value("PATIENT"));
    }

    @Test
    void readsAndUpdatesPatientProfileWithTheAuthenticatedUser() throws Exception {
        String token = registerAndGetToken("ana@example.com");

        mockMvc.perform(get("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("12345678"))
                .andExpect(jsonPath("$.birthDate").value("1998-05-20"))
                .andExpect(jsonPath("$.phone").value("987654321"));

        mockMvc.perform(put("/api/v1/patients/me")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"documentNumber":"87654321","birthDate":"1997-04-10","phone":"912345678"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.documentNumber").value("87654321"))
                .andExpect(jsonPath("$.birthDate").value("1997-04-10"))
                .andExpect(jsonPath("$.phone").value("912345678"));
    }

    @Test
    void requiresAuthenticationToReadPatientProfile() throws Exception {
        mockMvc.perform(get("/api/v1/patients/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void returnsConflictWhenEmailAlreadyExistsIgnoringCase() throws Exception {
        register("ANA@example.com");

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Otra","lastName":"Persona","documentNumber":"87654321","birthDate":"1998-05-20","phone":"987654321","email":"ana@example.com","password":"clave-segura"}
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
                                new RegistrationPayload(
                                        "Ana",
                                        "Pérez",
                                        "12345678",
                                        "1998-05-20",
                                        "987654321",
                                        email,
                                        "clave-segura"))))
                .andExpect(status().isCreated());
    }

    private String registerAndGetToken(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new RegistrationPayload(
                                        "Ana",
                                        "Pérez",
                                        "12345678",
                                        "1998-05-20",
                                        "987654321",
                                        email,
                                        "clave-segura"))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }

    private record RegistrationPayload(
            String firstName,
            String lastName,
            String documentNumber,
            String birthDate,
            String phone,
            String email,
            String password) {
    }
}
