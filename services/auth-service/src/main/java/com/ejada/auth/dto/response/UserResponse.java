package com.ejada.auth.dto.response;

import com.ejada.auth.domain.Role;
import com.ejada.auth.entity.User;

public record UserResponse(
        Long id,
        String username,
        String email,
        String fullName,
        Role role,
        Boolean enabled
) {
    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getUsername(), u.getEmail(),
                u.getFullName(), u.getRole(), u.getEnabled());
    }
}
