package com.mediturno.backend.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClockConfiguration {

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }

    /**
     * Zona horaria de negocio. Las citas se guardan y se exponen como fecha/hora local
     * de esta zona (por defecto America/Lima), sin desplazamiento UTC.
     */
    @Bean
    ZoneId applicationZone(@Value("${app.timezone:America/Lima}") String zone) {
        return ZoneId.of(zone);
    }
}
