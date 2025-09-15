package com.ecold.service;

import com.ecold.dto.LoginResponse;

public interface GoogleOAuthService {
    String getAuthorizationUrl();
    LoginResponse processCallback(String code);
    boolean refreshAccessToken(String refreshToken);
}