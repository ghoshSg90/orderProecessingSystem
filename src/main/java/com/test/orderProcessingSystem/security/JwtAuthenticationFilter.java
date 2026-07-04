package com.test.orderProcessingSystem.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTH_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader(AUTH_HEADER);

        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());

        if (SecurityContextHolder.getContext().getAuthentication() == null && jwtService.isTokenValid(token)) {
            authenticate(request, token);
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Loads the user referenced by the token and populates the security context. If the user no
     * longer exists (e.g. the account was deleted while the token was still within its expiry) or
     * anything else goes wrong, we leave the context unauthenticated and let the request fall through
     * to the authentication entry point (401) — never letting the exception escape as a 500.
     */
    private void authenticate(HttpServletRequest request, String token) {
        try {
            String userName = jwtService.extractUsername(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(userName);

            UsernamePasswordAuthenticationToken authToken =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
            authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authToken);
        } catch (UsernameNotFoundException ex) {
            // WARN: token is valid but its user is gone (deleted account)
            log.warn("Token references non-existent user: {}", ex.getMessage());
        } catch (Exception ex) {
            // Defensive: a bad token must never surface as a 500 from the filter
            log.warn("Could not authenticate request from token: {}", ex.getMessage());
        }
    }
}
