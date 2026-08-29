package com.ejada.auth.security;

import com.ejada.auth.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey key;
    private final long accessExpMs;
    private final long refreshExpMs;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-exp-ms}") long accessExpMs,
            @Value("${app.jwt.refresh-exp-ms}") long refreshExpMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessExpMs = accessExpMs;
        this.refreshExpMs = refreshExpMs;
    }

    public long accessExpMs() {
        return accessExpMs;
    }

    public String generateAccessToken(User user) {
        return build(user, "ACCESS", accessExpMs);
    }

    public String generateRefreshToken(User user) {
        return build(user, "REFRESH", refreshExpMs);
    }

    private String build(User user, String type, long ttlMs) {
        Date now = new Date();
        return Jwts.builder()
                .subject(String.valueOf(user.getId()))
                .claim("username", user.getUsername())
                .claim("role", user.getRole().name())
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + ttlMs))
                .signWith(key)
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }
}
