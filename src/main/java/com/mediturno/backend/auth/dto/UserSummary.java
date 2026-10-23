package com.mediturno.backend.auth.dto;

import com.mediturno.backend.user.domain.User;
import com.mediturno.backend.user.domain.UserRole;

public record UserSummary(
        Long id,
        String firstName,
        String lastName,
        String email,
        UserRole role) {

    public static UserSummary from(User user) {
        return new UserSummary(
                user.getId(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getRole());
    }
}
