package com.mediturno.backend.appointment.port;

import java.time.LocalDateTime;
import java.util.OptionalInt;

/** Contrato con la agenda de medicos (Integrante 3). */
public interface DoctorAvailability {

    /**
     * Si startAt es el inicio exacto de un slot dentro de la agenda activa del medico,
     * devuelve la duracion del slot en minutos. Si cae fuera de agenda, en dia no laborable
     * o desalineado, devuelve vacio. No revisa si esta ocupado: eso lo hace el modulo de citas.
     */
    OptionalInt slotMinutesAt(Long doctorId, LocalDateTime startAt);
}