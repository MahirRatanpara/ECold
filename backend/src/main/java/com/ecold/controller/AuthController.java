package com.ecold.controller;

import com.ecold.dto.LoginRequest;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.SignupRequest;
import com.ecold.dto.UserDto;
import com.ecold.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

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
        String authUrl = authService.getGoogleAuthUrl();
        return ResponseEntity.ok(authUrl);
    }
    
    @PostMapping("/google/callback")
    public ResponseEntity<LoginResponse> googleCallback(@RequestParam String code) {
        LoginResponse response = authService.processGoogleCallback(code);
        return ResponseEntity.ok(response);
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