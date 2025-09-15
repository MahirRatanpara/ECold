package com.ecold.service.impl;

import com.ecold.config.JwtUtil;
import com.ecold.dto.LoginRequest;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.SignupRequest;
import com.ecold.dto.UserDto;
import com.ecold.entity.User;
import com.ecold.exception.AuthenticationException;
import com.ecold.exception.UserAlreadyExistsException;
import com.ecold.repository.UserRepository;
import com.ecold.service.AuthService;
import com.ecold.service.GoogleOAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final GoogleOAuthService googleOAuthService;

    @Override
    public UserDto signup(SignupRequest signupRequest) {
        // Check if user already exists
        if (userRepository.findByEmail(signupRequest.getEmail()).isPresent()) {
            throw new UserAlreadyExistsException("An account with this email address already exists. Please use a different email or try logging in.");
        }
        
        // Create new user
        User user = new User();
        user.setEmail(signupRequest.getEmail());
        user.setName(signupRequest.getFirstName() + " " + signupRequest.getLastName());
        user.setPassword(passwordEncoder.encode(signupRequest.getPassword()));
        user.setProvider(User.Provider.LOCAL);
        
        // Save user
        User savedUser = userRepository.save(user);
        
        // Convert to DTO
        UserDto userDto = new UserDto();
        userDto.setId(savedUser.getId());
        userDto.setEmail(savedUser.getEmail());
        userDto.setName(savedUser.getName());
        userDto.setProvider(savedUser.getProvider());
        userDto.setCreatedAt(savedUser.getCreatedAt());
        
        return userDto;
    }

    @Override
    public LoginResponse login(LoginRequest loginRequest) {
        // Find user by email
        User user = userRepository.findByEmail(loginRequest.getEmail())
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
        userDto.setProvider(user.getProvider());
        userDto.setCreatedAt(user.getCreatedAt());
        
        // Create login response with real JWT token
        String jwtToken = jwtUtil.generateToken(user.getEmail());
        LoginResponse response = new LoginResponse();
        response.setToken(jwtToken);
        response.setRefreshToken("refresh-token-" + user.getId()); // Simple refresh token for now
        response.setUser(userDto);
        response.setExpiresIn(86400000L); // 24 hours
        
        return response;
    }

    @Override
    public String getGoogleAuthUrl() {
        return googleOAuthService.getAuthorizationUrl();
    }

    @Override
    public String getMicrosoftAuthUrl() {
        // TODO: Implement Microsoft OAuth URL generation
        return "https://login.microsoftonline.com/oauth2/v2.0/authorize";
    }

    @Override
    public LoginResponse processGoogleCallback(String code) {
        return googleOAuthService.processCallback(code);
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
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new AuthenticationException("User not found"));

        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        userDto.setProvider(user.getProvider());
        userDto.setCreatedAt(user.getCreatedAt());

        return userDto;
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
}