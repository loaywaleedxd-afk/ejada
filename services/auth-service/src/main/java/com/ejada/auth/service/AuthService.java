package com.ejada.auth.service;

import com.ejada.auth.client.WalletClient;
import com.ejada.auth.domain.Role;
import com.ejada.auth.dto.response.AuthResponse;
import com.ejada.auth.dto.request.CreateUserRequest;
import com.ejada.auth.dto.request.LoginRequest;
import com.ejada.auth.dto.request.RegisterRequest;
import com.ejada.auth.dto.response.UserResponse;
import com.ejada.auth.entity.User;
import com.ejada.auth.exception.ConflictException;
import com.ejada.auth.exception.ResourceNotFoundException;
import com.ejada.auth.exception.UnauthorizedException;
import com.ejada.auth.repository.UserRepository;
import com.ejada.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final WalletClient walletClient;
    private final JwtService jwt;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        ensureUnique(req.username(), req.email());
        User user = persist(req.username(), req.email(), req.password(), req.fullName(), Role.ROLE_USER);
        return tokens(user);
    }

    @Transactional
    public User createUser(CreateUserRequest req) {
        ensureUnique(req.username(), req.email());
        return persist(req.username(), req.email(), req.password(), req.fullName(), Role.valueOf(req.role()));
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        User user = users.findByUsername(req.username())
                .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));
        if (!Boolean.TRUE.equals(user.getEnabled())
                || !passwordEncoder.matches(req.password(), user.getPassword())) {
            throw new UnauthorizedException("Invalid credentials");
        }
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse refresh(String refreshToken) {
        Claims claims;
        try {
            claims = jwt.parse(refreshToken);
        } catch (JwtException e) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }
        if (!"REFRESH".equals(claims.get("type", String.class))) {
            throw new UnauthorizedException("Not a refresh token");
        }
        User user = users.findById(Long.valueOf(claims.getSubject()))
                .orElseThrow(() -> new UnauthorizedException("User no longer exists"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new UnauthorizedException("User is disabled");
        }
        return tokens(user);
    }

    @Transactional(readOnly = true)
    public User get(Long id) {
        return users.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User " + id + " not found"));
    }

    private void ensureUnique(String username, String email) {
        if (users.existsByUsername(username)) {
            throw new ConflictException("Username already taken");
        }
        if (users.existsByEmail(email)) {
            throw new ConflictException("Email already registered");
        }
    }

    private User persist(String username, String email, String rawPassword, String fullName, Role role) {
        User user = users.save(User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .fullName(fullName)
                .role(role)
                .enabled(true)
                .build());

        walletClient.createWallet(user.getId());
        return user;
    }

    private AuthResponse tokens(User user) {
        return new AuthResponse(
                jwt.generateAccessToken(user),
                jwt.generateRefreshToken(user),
                "Bearer",
                jwt.accessExpMs(),
                UserResponse.from(user));
    }
}
