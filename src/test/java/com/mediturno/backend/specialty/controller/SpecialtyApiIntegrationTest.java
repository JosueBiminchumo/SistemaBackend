package com.mediturno.backend.specialty.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mediturno.backend.auth.security.JwtService;
import com.mediturno.backend.patient.repository.PatientRepository;
import com.mediturno.backend.specialty.entity.Specialty;
import com.mediturno.backend.specialty.repository.SpecialtyRepository;
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SpecialtyApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private SpecialtyRepository specialtyRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    private String adminToken;
    private String patientToken;

    @BeforeEach
    void setUp() {
        specialtyRepository.deleteAll();
        patientRepository.deleteAll();
        userRepository.deleteAll();

        User admin = userRepository.saveAndFlush(User.create(
                "Admin", "System", "admin@mediturno.com", passwordEncoder.encode("password123"),
                UserRole.ADMIN, true));
        adminToken = jwtService.generateToken(admin);

        User patient = userRepository.saveAndFlush(User.create(
                "Carlos", "Paciente", "paciente@mediturno.com", passwordEncoder.encode("password123"),
                UserRole.PATIENT, true));
        patientToken = jwtService.generateToken(patient);
    }

    @Test
    void listSpecialtiesIsPubliclyAccessible() throws Exception {
        specialtyRepository.save(new Specialty("Cardiología", "Corazón", true));
        specialtyRepository.save(new Specialty("Dermatología", "Piel", false));

        mockMvc.perform(get("/api/v1/specialties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Cardiología"))
                .andExpect(jsonPath("$[1].name").value("Dermatología"));

        mockMvc.perform(get("/api/v1/specialties?active=true"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Cardiología"));
    }

    @Test
    void createSpecialtyRequiresAdminRole() throws Exception {
        String body = """
                {"name":"Neurología","description":"Sistema nervioso","active":true}
                """;

        // Sin token -> 401
        mockMvc.perform(post("/api/v1/specialties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isUnauthorized());

        // Con rol PATIENT -> 403
        mockMvc.perform(post("/api/v1/specialties")
                        .header("Authorization", "Bearer " + patientToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
    }

    @Test
    void createSpecialtySuccessWithAdmin() throws Exception {
        mockMvc.perform(post("/api/v1/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"  Cardiología Clínica  ","description":"Atención cardíaca"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.name").value("Cardiología Clínica"))
                .andExpect(jsonPath("$.description").value("Atención cardíaca"))
                .andExpect(jsonPath("$.active").value(true));

        assertThat(specialtyRepository.existsByNameIgnoreCase("Cardiología Clínica")).isTrue();
    }

    @Test
    void createSpecialtyRejectsBlankName() throws Exception {
        mockMvc.perform(post("/api/v1/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"   ","description":"Sin nombre"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.details.name").value("El nombre es obligatorio"));
    }

    @Test
    void createSpecialtyRejectsDuplicateName() throws Exception {
        specialtyRepository.save(new Specialty("Pediatría", "Niños", true));

        mockMvc.perform(post("/api/v1/specialties")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"pediatría","description":"Otra descripción"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una especialidad con ese nombre"));
    }

    @Test
    void updateSpecialtyModifiesFieldsWhenAdmin() throws Exception {
        Specialty saved = specialtyRepository.save(new Specialty("Traumatología", "Huesos", true));

        mockMvc.perform(put("/api/v1/specialties/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Traumatología y Ortopedia","description":"Huesos y articulaciones","active":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.name").value("Traumatología y Ortopedia"))
                .andExpect(jsonPath("$.active").value(false));

        Specialty updated = specialtyRepository.findById(saved.getId()).orElseThrow();
        assertThat(updated.getName()).isEqualTo("Traumatología y Ortopedia");
        assertThat(updated.isActive()).isFalse();
    }

    @Test
    void updateSpecialtyRejectsNonExistentId() throws Exception {
        mockMvc.perform(put("/api/v1/specialties/999999")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Psiquiatría","description":"Salud mental"}
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La especialidad no existe"));
    }

    @Test
    void updateSpecialtyRejectsDuplicateNameOfOtherSpecialty() throws Exception {
        specialtyRepository.save(new Specialty("Cardiología", "Corazón", true));
        Specialty s2 = specialtyRepository.save(new Specialty("Pediatría", "Niños", true));

        mockMvc.perform(put("/api/v1/specialties/" + s2.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"cardiología","description":"Cambio a nombre repetido"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe una especialidad con ese nombre"));
    }

    @Test
    void deactivatePerformsSoftDelete() throws Exception {
        Specialty saved = specialtyRepository.save(new Specialty("Oftalmología", "Ojos", true));

        // Sin token -> 401
        mockMvc.perform(delete("/api/v1/specialties/" + saved.getId()))
                .andExpect(status().isUnauthorized());

        // Con rol PATIENT -> 403
        mockMvc.perform(delete("/api/v1/specialties/" + saved.getId())
                        .header("Authorization", "Bearer " + patientToken))
                .andExpect(status().isForbidden());

        // Con ADMIN -> 200
        mockMvc.perform(delete("/api/v1/specialties/" + saved.getId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(saved.getId()))
                .andExpect(jsonPath("$.active").value(false));

        Specialty deactivated = specialtyRepository.findById(saved.getId()).orElseThrow();
        assertThat(deactivated.isActive()).isFalse();
    }

    @Test
    void deactivateRejectsNonExistentId() throws Exception {
        mockMvc.perform(delete("/api/v1/specialties/999999")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("La especialidad no existe"));
    }
}
