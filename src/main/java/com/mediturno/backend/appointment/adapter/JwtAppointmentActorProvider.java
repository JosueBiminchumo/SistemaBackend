package com.mediturno.backend.appointment.adapter;

import com.mediturno.backend.appointment.exception.AppointmentUnauthenticatedException;
import com.mediturno.backend.appointment.port.ActorRole;
import com.mediturno.backend.appointment.port.AppointmentActor;
import com.mediturno.backend.appointment.port.AppointmentActorProvider;
import com.mediturno.backend.user.domain.User;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class JwtAppointmentActorProvider implements AppointmentActorProvider {

    @Override
    public AppointmentActor current() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) {
            throw new AppointmentUnauthenticatedException("Se requiere iniciar sesion");
        }
        // AJUSTAR (punto 1 y 2 de la lista): tipo del principal y getter del id
        if (!(auth.getPrincipal() instanceof User user)) {
            throw new AppointmentUnauthenticatedException("Sesion invalida");
        }
        return new AppointmentActor(user.getId(), resolveRole(auth));
    }

    private ActorRole resolveRole(Authentication auth) {
        for (GrantedAuthority authority : auth.getAuthorities()) {
            String name = authority.getAuthority();
            String normalized = name.startsWith("ROLE_") ? name.substring(5) : name;
            try {
                return ActorRole.valueOf(normalized);
            } catch (IllegalArgumentException ignored) {
                // authority que no es un rol de MediTurno
            }
        }
        throw new AppointmentUnauthenticatedException("El usuario no tiene un rol valido");
    }
}