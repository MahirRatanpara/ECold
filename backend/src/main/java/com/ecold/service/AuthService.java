package com.ecold.service;

import com.ecold.dto.LoginRequest;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.SignupRequest;
import com.ecold.dto.UserDto;

public interface AuthService {
    UserDto signup(SignupRequest signupRequest);
    LoginResponse login(LoginRequest loginRequest);
    String getGoogleAuthUrl();
    String getMicrosoftAuthUrl();
    LoginResponse processGoogleCallback(String code);
    LoginResponse processMicrosoftCallback(String code);
    UserDto getCurrentUser();
    LoginResponse refreshToken(String refreshToken);
    void logout();
}