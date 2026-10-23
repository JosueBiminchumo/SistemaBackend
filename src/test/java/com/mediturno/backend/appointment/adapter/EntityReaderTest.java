package com.mediturno.backend.appointment.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class EntityReaderTest {

    public static class UserEnIngles {
        public String getFirstName() { return "Maria"; }
        public String getLastName() { return "Lopez"; }
    }

    public static class UserEnEspanol {
        public String getNombres() { return "Carlos"; }
        public String getApellidos() { return "Ruiz"; }
    }

    public static class UserSoloCorreo {
        public String getEmail() { return "ana@correo.com"; }
    }

    public static class ConId {
        public Long getId() { return 7L; }
    }

    @Test
    void fullNameWorksWithEnglishFields() {
        assertEquals("Maria Lopez", EntityReader.fullName(new UserEnIngles()));
    }

    @Test
    void fullNameWorksWithSpanishFields() {
        assertEquals("Carlos Ruiz", EntityReader.fullName(new UserEnEspanol()));
    }

    @Test
    void fullNameFallsBackToEmail() {
        assertEquals("ana@correo.com", EntityReader.fullName(new UserSoloCorreo()));
    }

    @Test
    void missingGetterReturnsNull() {
        assertNull(EntityReader.call(new UserSoloCorreo(), "getId"));
        assertNull(EntityReader.fullName(null));
    }

    @Test
    void idIsReadAsLong() {
        assertEquals(7L, EntityReader.asLong(EntityReader.call(new ConId(), "getId")));
    }
}