package com.ecold.service.impl;

import com.ecold.config.JwtUtil;
import com.ecold.dto.LoginRequest;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.SignupRequest;
import com.ecold.dto.UserDto;
import com.ecold.entity.User;
import com.ecold.exception.AuthenticationException;
import com.ecold.exception.UserAlreadyExistsException;
import com.ecold.repository.firestore.UserFirestoreRepository;
import com.ecold.service.AuthService;
import com.ecold.service.GoogleOAuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {

    private final UserFirestoreRepository userFirestoreRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    @Autowired(required = false)
    private GoogleOAuthService googleOAuthService;

    public AuthServiceImpl(UserFirestoreRepository userFirestoreRepository, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userFirestoreRepository = userFirestoreRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public UserDto signup(SignupRequest signupRequest) {
        try {
            // Check if user already exists
            if (userFirestoreRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
                throw new UserAlreadyExistsException("An account with this email address already exists. Please use a different email or try logging in.");
            }

            // Create new user
            User user = new User();
            user.setEmail(signupRequest.getEmail());
            user.setName(signupRequest.getFirstName() + " " + signupRequest.getLastName());
            user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
            user.setProviderEnum(User.Provider.LOCAL);

            // Save user
            User savedUser = userFirestoreRepository.save(user);

            // Convert to DTO
            UserDto userDto = new UserDto();
            userDto.setId(savedUser.getId());
            userDto.setEmail(savedUser.getEmail());
            userDto.setName(savedUser.getName());
            userDto.setProvider(savedUser.getProviderEnum());
            userDto.setCreatedAt(convertToLocalDateTime(savedUser.getCreatedAt()));

            return userDto;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during signup", e);
        }
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        try {
            // Find user by email
            User user = userFirestoreRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow(() -> new AuthenticationException("Invalid email or password"));

            // Check password
            if (!passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
                throw new AuthenticationException("Invalid email or password");
            }

            // Create user DTO
            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setEmail(user.getEmail());
            userDto.setName(user.getName());
            userDto.setProvider(user.getProviderEnum());
            userDto.setCreatedAt(convertToLocalDateTime(user.getCreatedAt()));

            // Create login response with real JWT token
            String jwtToken = jwtUtil.generateToken(user.getEmail());
            LoginResponse response = new LoginResponse();
            response.setToken(jwtToken);
            response.setRefreshToken("refresh-token-" + user.getId()); // Simple refresh token for now
            response.setUser(userDto);
            response.setExpiresIn(86400000L); // 24 hours

            return response;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error during login", e);
        }
    }

    @Override
    public String getGoogleAuthUrl() {
        if (googleOAuthService != null) {
            return googleOAuthService.getAuthorizationUrl();
        }
        log.warn("Google OAuth service not available - OAuth configuration incomplete");
        throw new AuthenticationException("Google OAuth not configured");
    }

    @Override
    public String getMicrosoftAuthUrl() {
        // TODO: Implement Microsoft OAuth URL generation
        return "https://login.microsoftonline.com/oauth2/v2.0/authorize";
    }

    @Override
    public LoginResponse processGoogleCallback(String code) {
        if (googleOAuthService != null) {
            return googleOAuthService.processCallback(code);
        }
        log.warn("Google OAuth service not available - OAuth configuration incomplete");
        throw new AuthenticationException("Google OAuth not configured");
    }

    @Override
    public LoginResponse processMicrosoftCallback(String code) {
        // TODO: Implement Microsoft OAuth callback processing
        LoginResponse response = new LoginResponse();
        response.setAccessToken("mock-token");
        return response;
    }
    
    @Override
    public UserDto getCurrentUser() {
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userFirestoreRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("User not found"));

            UserDto userDto = new UserDto();
            userDto.setId(user.getId());
            userDto.setEmail(user.getEmail());
            userDto.setName(user.getName());
            userDto.setProvider(user.getProviderEnum());
            userDto.setCreatedAt(convertToLocalDateTime(user.getCreatedAt()));

            return userDto;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching current user", e);
        }
    }
    
    @Override
    public LoginResponse refreshToken(String refreshToken) {
        // TODO: Implement refresh token logic
        return new LoginResponse();
    }
    
    @Override
    public void logout() {
        // TODO: Implement logout logic
    }

    /**
     * Helper method to convert Firestore Timestamp to LocalDateTime
     */
    private java.time.LocalDateTime convertToLocalDateTime(com.google.cloud.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                java.time.ZoneId.systemDefault()
        );
    }
}