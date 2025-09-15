package com.ecold.controller;

import com.ecold.dto.LoginRequest;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.SignupRequest;
import com.ecold.dto.UserDto;
import com.ecold.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class AuthController {
    
    private final AuthService authService;
    
    @PostMapping("/signup")
    public ResponseEntity<UserDto> signup(@Valid @RequestBody SignupRequest signupRequest) {
        UserDto user = authService.signup(signupRequest);
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        LoginResponse response = authService.login(loginRequest);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/google")
    public ResponseEntity<String> googleLogin() {
        log.info("🚀 Google OAuth login endpoint called");
        try {
            String authUrl = authService.getGoogleAuthUrl();
            log.info("✅ Generated Google auth URL: {}", authUrl);
            return ResponseEntity.ok(authUrl);
        } catch (Exception e) {
            log.error("❌ Error in Google OAuth login: {}", e.getMessage(), e);
            throw e;
        }
    }

    @PostMapping("/google/callback")
    public ResponseEntity<LoginResponse> googleCallback(@RequestBody GoogleCallbackRequest request) {
        log.info("🔄 Google OAuth callback called with code: {}", request.getCode() != null ? "present" : "null");
        try {
            LoginResponse response = authService.processGoogleCallback(request.getCode());
            log.info("✅ Google OAuth callback successful for user: {}", response.getUser().getEmail());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            log.error("❌ Error in Google OAuth callback: {}", e.getMessage(), e);
            throw e;
        }
    }

    // Inner class for request body
    public static class GoogleCallbackRequest {
        private String code;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
    }
    
    @GetMapping("/microsoft")
    public ResponseEntity<String> microsoftLogin() {
        String authUrl = authService.getMicrosoftAuthUrl();
        return ResponseEntity.ok(authUrl);
    }
    
    @PostMapping("/microsoft/callback")
    public ResponseEntity<LoginResponse> microsoftCallback(@RequestParam String code) {
        LoginResponse response = authService.processMicrosoftCallback(code);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/me")
    public ResponseEntity<UserDto> getCurrentUser() {
        UserDto user = authService.getCurrentUser();
        return ResponseEntity.ok(user);
    }
    
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refreshToken(@RequestHeader("Authorization") String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken.replace("Bearer ", ""));
        return ResponseEntity.ok(response);
    }
    
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        authService.logout();
        return ResponseEntity.ok().build();
    }
}