package com.mediturno.backend.auth.controller;


import com.mediturno.backend.patient.repository.PatientRepository;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.mediturno.backend.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "app.auth.registration-enabled=false")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RegistrationDisabledIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private UserRepository userRepository;

    @BeforeEach
    void clearUsers() {
        patientRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void rejectsPublicRegistrationWhenFeatureIsDisabled() throws Exception {
        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Ana","lastName":"Pérez","documentNumber":"12345678","birthDate":"2000-01-01","phone":"987654321","email":"ana@example.com","password":"clave-segura"}
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("El registro público está deshabilitado."));
    }
}
