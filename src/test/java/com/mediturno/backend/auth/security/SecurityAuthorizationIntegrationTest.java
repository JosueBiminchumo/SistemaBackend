package com.mediturno.backend.auth.security;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class SecurityAuthorizationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void clearUsers() {
        userRepository.deleteAll();
    }

    @Test
    void rejectsProtectedRouteWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/patients/probe"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.path").value("/api/v1/patients/probe"))
                .andExpect(jsonPath("$.message").value("Se requiere autenticación válida."));
    }

    @Test
    void acceptsPatientTokenOnPatientRoute() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/v1/patients/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsPatientTokenOnAdminRoute() throws Exception {
        String token = registerAndGetToken();

        mockMvc.perform(get("/api/v1/admin/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void allowsAdministratorOnAdminRoute() throws Exception {
        User admin = userRepository.saveAndFlush(User.create(
                "Alex", "Admin", "admin@example.com", passwordEncoder.encode("clave-segura"),
                UserRole.ADMIN, true));
        String token = jwtService.generateToken(admin);

        mockMvc.perform(get("/api/v1/admin/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNoContent());
    }

    @Test
    void rejectsMalformedBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/patients/probe")
                        .header("Authorization", "Bearer no-es-un-jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsExpiredBearerToken() throws Exception {
        mockMvc.perform(get("/api/v1/patients/probe")
                        .header("Authorization", "Bearer "
                                + SignedJwtTestTokens.expiredTokenFor("ana@example.com")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsTokenWithTamperedSignature() throws Exception {
        String token = SignedJwtTestTokens.tamperSignature(registerAndGetToken());

        mockMvc.perform(get("/api/v1/patients/probe")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnauthorized());
    }

    private String registerAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pérez","email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }
}
