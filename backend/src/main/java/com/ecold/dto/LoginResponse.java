package com.ecold.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponse {
    private String token;
    private String accessToken;
    private String refreshToken;
    private UserDto user;
    private long expiresIn;
    
    public void setAccessToken(String accessToken) {
        this.token = accessToken;
        this.accessToken = accessToken;
    }
}