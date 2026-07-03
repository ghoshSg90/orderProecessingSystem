package com.test.orderProcessingSystem.logging;

import com.test.orderProcessingSystem.security.UserPrincipal;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Logs one line per incoming request. Ordered after the security chain (default filter order)
 * so the authenticated user is available. Never logs credentials, tokens, or request bodies.
 */
@Slf4j
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserPrincipal principal) {
            log.info("Request received method={} uri={} userId={} role={} ip={}",
                    request.getMethod(), request.getRequestURI(),
                    principal.getUserId(), principal.getUserRoleCategory().name(), clientIp(request));
        } else {
            log.info("Request received method={} uri={} ip={} (unauthenticated)",
                    request.getMethod(), request.getRequestURI(), clientIp(request));
        }
        filterChain.doFilter(request, response);
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Skip actuator/health noise
        return request.getRequestURI().startsWith("/actuator");
    }

    private String clientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
