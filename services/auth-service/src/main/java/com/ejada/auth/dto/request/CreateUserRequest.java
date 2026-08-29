package com.ejada.auth.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateUserRequest(
        @NotBlank(message = "username is required")
        @Pattern(regexp = "^[a-zA-Z0-9._-]{3,50}$",
                message = "username must be 3-50 characters: letters, digits, dot, underscore or hyphen")
        String username,

        @NotBlank(message = "email is required")
        @Email(message = "must be a valid email address")
        @Size(max = 120, message = "email must be at most 120 characters")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, max = 72, message = "password must be 8-72 characters")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).+$",
                message = "password must contain at least one letter and one digit")
        String password,

        @Size(max = 120, message = "full name must be at most 120 characters")
        String fullName,

        @NotBlank(message = "role is required")
        @Pattern(regexp = "ROLE_USER|ROLE_ADMIN", message = "role must be ROLE_USER or ROLE_ADMIN")
        String role
) {
}
