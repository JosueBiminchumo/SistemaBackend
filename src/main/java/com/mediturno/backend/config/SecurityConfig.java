package com.mediturno.backend.config;

import com.mediturno.backend.auth.security.JwtAuthenticationFilter;
import com.mediturno.backend.common.security.RestAccessDeniedHandler;
import com.mediturno.backend.common.security.RestAuthenticationEntryPoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter,
            RestAuthenticationEntryPoint authenticationEntryPoint,
            RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> {})
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(
                               "/swagger-ui.html",
                                      "/swagger-ui/**",
                                     "/v3/api-docs/**"
                               ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/auth/login",
                                "/api/v1/auth/register").permitAll()
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Especialidades: lectura publica, escritura solo ADMIN
                        .requestMatchers(HttpMethod.GET, "/api/v1/specialties", "/api/v1/specialties/*").permitAll()
                        .requestMatchers("/api/v1/specialties/**").hasRole("ADMIN")
                        // Medicos: perfil propio, lectura publica, agenda propia (DOCTOR/ADMIN), resto ADMIN
                        .requestMatchers("/api/v1/doctors/me").hasRole("DOCTOR")
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/doctors",
                                "/api/v1/doctors/*",
                                "/api/v1/doctors/*/schedule",
                                "/api/v1/doctors/*/schedule/rules").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/doctors/*/schedule/rules")
                                .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/v1/doctors/**").hasRole("ADMIN")
                        .requestMatchers("/api/v1/patients/**").hasAnyRole("PATIENT", "ADMIN")
                        // Citas: el alcance por dueño se valida ademas en AppointmentService
                        .requestMatchers(HttpMethod.POST, "/api/v1/appointments").hasRole("PATIENT")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/appointments/*").hasAnyRole("PATIENT", "ADMIN")
                        .requestMatchers(HttpMethod.PATCH, "/api/v1/appointments/*/status")
                                .hasAnyRole("DOCTOR", "ADMIN")
                        .requestMatchers("/api/v1/appointments/**").hasAnyRole("PATIENT", "DOCTOR", "ADMIN")
                        .requestMatchers("/api/v1/statistics/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
