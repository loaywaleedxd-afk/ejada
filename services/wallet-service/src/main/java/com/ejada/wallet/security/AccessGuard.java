package com.ejada.wallet.security;

import com.ejada.wallet.exception.ForbiddenException;
import com.ejada.wallet.exception.UnauthorizedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

@Component
public class AccessGuard {

    private static final String ROLE_ADMIN = "ROLE_ADMIN";

    public void requireSelfOrAdmin(HttpServletRequest request, Long targetUserId) {
        Object userId = request.getAttribute(JwtAuthFilter.USER_ID);
        Object role = request.getAttribute(JwtAuthFilter.ROLE);
        if (userId == null) {
            throw new UnauthorizedException("Authentication required");
        }
        if (!userId.equals(targetUserId) && !ROLE_ADMIN.equals(role)) {
            throw new ForbiddenException("You can only access your own account");
        }
    }
}
