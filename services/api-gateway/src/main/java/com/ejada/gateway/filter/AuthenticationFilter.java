package com.ejada.gateway.filter;

import com.ejada.gateway.security.SecurityHeaders;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final SecretKey key;
    private final String internalSecret;

    public AuthenticationFilter(@Value("${app.jwt.secret}") String secret,
                                @Value("${app.internal.secret}") String internalSecret) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.internalSecret = internalSecret;
    }

    @Override
    public int getOrder() {
        return -1;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        if (isPublic(path, method)) {
            ServerHttpRequest passthrough = request.mutate()
                    .headers(h -> h.set(SecurityHeaders.INTERNAL_KEY, internalSecret))
                    .build();
            return chain.filter(exchange.mutate().request(passthrough).build());
        }

        String header = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Missing or malformed Authorization header");
        }

        Claims claims;
        try {
            claims = Jwts.parser().verifyWith(key).build()
                    .parseSignedClaims(header.substring(7)).getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        if (!"ACCESS".equals(claims.get("type", String.class))) {
            return error(exchange, HttpStatus.UNAUTHORIZED, "A refresh token cannot be used to call APIs");
        }

        String role = claims.get("role", String.class);
        if (isAdminRoute(path, method) && !SecurityHeaders.ROLE_ADMIN.equals(role)) {
            return error(exchange, HttpStatus.FORBIDDEN, "Admin role required");
        }

        String username = claims.get("username", String.class);
        ServerHttpRequest mutated = request.mutate()
                .headers(h -> {
                    h.set(SecurityHeaders.USER_ID, claims.getSubject());
                    h.set(SecurityHeaders.USER_ROLE, role == null ? "" : role);
                    h.set(SecurityHeaders.USER_NAME, username == null ? "" : username);
                    h.set(SecurityHeaders.INTERNAL_KEY, internalSecret);
                })
                .build();
        return chain.filter(exchange.mutate().request(mutated).build());
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path.startsWith("/actuator/")) {
            return true;
        }
        if (path.equals("/api/auth/register")
                || path.equals("/api/auth/login")
                || path.equals("/api/auth/refresh")) {
            return true;
        }
        if (HttpMethod.GET.equals(method)
                && (path.startsWith("/api/shop/products")
                || path.startsWith("/api/inventory/products")
                || path.startsWith("/api/shop/payment-methods"))) {
            return true;
        }
        return false;
    }

    private boolean isAdminRoute(String path, HttpMethod method) {
        if (path.startsWith("/api/auth/admin/") || path.startsWith("/api/shop/discounts")) {
            return true;
        }
        boolean mutating = HttpMethod.POST.equals(method) || HttpMethod.PUT.equals(method)
                || HttpMethod.DELETE.equals(method) || HttpMethod.PATCH.equals(method);
        return mutating
                && (path.startsWith("/api/shop/products")
                || path.startsWith("/api/inventory/products")
                || path.startsWith("/api/shop/payment-methods"));
    }

    private Mono<Void> error(ServerWebExchange exchange, HttpStatus status, String message) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"status\":" + status.value() + ",\"error\":\"" + message + "\"}";
        var buffer = exchange.getResponse().bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
