package com.ejada.shop.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    public static final String USER_ID = "authUserId";
    public static final String ROLE = "authRole";

    private final JwtService jwt;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            try {
                Claims claims = jwt.parse(header.substring(7));
                if ("ACCESS".equals(claims.get("type", String.class))) {
                    request.setAttribute(USER_ID, Long.valueOf(claims.getSubject()));
                    request.setAttribute(ROLE, claims.get("role", String.class));
                }
            } catch (Exception ignored) {
            }
        }
        chain.doFilter(request, response);
    }
}
