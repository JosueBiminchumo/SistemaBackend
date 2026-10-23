package com.mediturno.backend.appointment.adapter;

import com.mediturno.backend.appointment.exception.AppointmentUnauthenticatedException;
import com.mediturno.backend.appointment.port.ActorRole;
import com.mediturno.backend.appointment.port.AppointmentActor;
import com.mediturno.backend.appointment.port.AppointmentActorProvider;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

/** Entrega el usuario y el rol del JWT ya validado por JwtAuthenticationFilter. */
@Component
public class JwtAppointmentActorProvider implements AppointmentActorProvider {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public AppointmentActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppointmentUnauthenticatedException("Se requiere iniciar sesion");
        }
        return new AppointmentActor(resolveUserId(auth), resolveRole(auth));
    }

    /** 1) Si el principal expone getId() se usa; 2) si no, se busca por el correo (username). */
    private Long resolveUserId(Authentication auth) {
        Object principal = auth.getPrincipal();
        Long id = EntityReader.asLong(EntityReader.call(principal, "getId"));
        if (id != null) {
            return id;
        }
        String username = principal instanceof UserDetails details ? details.getUsername() : auth.getName();
        if (username == null || username.isBlank()) {
            throw new AppointmentUnauthenticatedException("Sesion invalida");
        }
        List<?> rows = entityManager
                .createQuery("select u.id from User u where lower(u.email) = :email")
                .setParameter("email", username.toLowerCase(Locale.ROOT))
                .setMaxResults(1)
                .getResultList();
        Long found = rows.isEmpty() ? null : EntityReader.asLong(rows.get(0));
        if (found == null) {
            throw new AppointmentUnauthenticatedException("No se encontro al usuario de la sesion");
        }
        return found;
    }

    private ActorRole resolveRole(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            ActorRole role = toRole(authority.getAuthority());
            if (role != null) {
                return role;
            }
        }
        throw new AppointmentUnauthenticatedException("El usuario no tiene un rol valido");
    }

    /** Acepta ROLE_ADMIN o ADMIN, y tambien los nombres en espanol. */
    private ActorRole toRole(String authority) {
        if (authority == null) {
            return null;
        }
        String name = authority.toUpperCase(Locale.ROOT);
        if (name.startsWith("ROLE_")) {
            name = name.substring(5);
        }
        return switch (name) {
            case "ADMIN", "ADMINISTRADOR" -> ActorRole.ADMIN;
            case "DOCTOR", "MEDICO" -> ActorRole.DOCTOR;
            case "PATIENT", "PACIENTE" -> ActorRole.PATIENT;
            default -> null;
        };
    }
}