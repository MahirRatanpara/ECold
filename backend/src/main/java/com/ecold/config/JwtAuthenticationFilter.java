package com.ecold.config;

import com.ecold.entity.User;
import com.ecold.repository.firestore.UserFirestoreRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.ExecutionException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserFirestoreRepository userFirestoreRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();

        // Skip JWT authentication for OAuth and public endpoints
        if (shouldSkipFilter(requestURI)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            String jwt = getJwtFromRequest(request);

            if (StringUtils.hasText(jwt) && jwtUtil.validateToken(jwt)) {
                String email = jwtUtil.getEmailFromToken(jwt);

                try {
                    Optional<User> userOptional = userFirestoreRepository.findByEmail(email);
                    if (userOptional.isPresent()) {
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null,
                                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    } else {
                        log.warn("User not found for email: {}", email);
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.error("Error fetching user from Firestore: {}", e.getMessage());
                }
            }
        } catch (Exception ex) {
            log.error("Could not set user authentication: {}", ex.getMessage());
        }

        filterChain.doFilter(request, response);
    }
    
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    private boolean shouldSkipFilter(String requestURI) {
        // Skip JWT authentication for these endpoints
        return requestURI.startsWith("/api/auth/") ||
               requestURI.startsWith("/api/actuator/");
    }
}