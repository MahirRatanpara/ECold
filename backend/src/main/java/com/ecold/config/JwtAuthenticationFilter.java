package com.ecold.config;

import com.ecold.entity.User;
import com.ecold.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;
    
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                  FilterChain filterChain) throws ServletException, IOException {

        String requestURI = request.getRequestURI();
        System.out.println("Request URI: " + requestURI);

        // Skip JWT authentication for OAuth endpoints
        if (shouldSkipFilter(requestURI)) {
            System.out.println("✅ Skipping JWT filter for endpoint: " + requestURI);
            filterChain.doFilter(request, response);
            return;
        }

        System.out.println("🔍 Processing JWT filter for endpoint: " + requestURI);

        try {
            String jwt = getJwtFromRequest(request);
            System.out.println("JWT from request: " + (jwt != null ? "Present (length: " + jwt.length() + ")" : "Not present"));
            
            if (StringUtils.hasText(jwt)) {
                boolean isValid = jwtUtil.validateToken(jwt);
                System.out.println("JWT token valid: " + isValid);
                
                if (isValid) {
                    String email = jwtUtil.getEmailFromToken(jwt);
                    System.out.println("Email from JWT: " + email);
                    
                    Optional<User> userOptional = userRepository.findByEmail(email);
                    if (userOptional.isPresent()) {
                        User user = userOptional.get();
                        System.out.println("User found: " + user.getEmail());
                        
                        UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(email, null,
                                java.util.Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER")));
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                        System.out.println("Authentication set successfully");
                    } else {
                        System.err.println("User not found for email: " + email);
                    }
                } else {
                    System.err.println("Invalid JWT token");
                }
            } else {
                System.out.println("No JWT token found in request");
            }
        } catch (Exception ex) {
            System.err.println("Could not set user authentication in security context: " + ex.getMessage());
            ex.printStackTrace();
        }

        System.out.println("=== BEFORE doFilter - About to continue filter chain ===");
        filterChain.doFilter(request, response);
        System.out.println("=== AFTER doFilter - Returned from filter chain ===");
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
               requestURI.startsWith("/api/actuator/") ||
               requestURI.startsWith("/api/email-templates/");
    }
}