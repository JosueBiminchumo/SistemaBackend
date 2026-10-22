package com.mediturno.backend.appointment.port;

/** Contrato con seguridad (Integrante 1): usuario autenticado del JWT. */
public interface AppointmentActorProvider {

    AppointmentActor current();
}