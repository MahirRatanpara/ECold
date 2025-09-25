package com.ecold.service.impl;

import com.ecold.config.JwtUtil;
import com.ecold.dto.LoginResponse;
import com.ecold.dto.UserDto;
import com.ecold.entity.User;
import com.ecold.repository.UserRepository;
import com.ecold.service.GoogleOAuthService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleOAuthServiceImpl implements GoogleOAuthService {

    private final ClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${spring.security.oauth2.client.registration.google.redirect-uri:http://localhost:4200/auth/google/callback}")
    private String redirectUri;

    @Override
    public String getAuthorizationUrl() {
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");

        // Validate OAuth configuration
        if (googleRegistration.getClientId() == null || googleRegistration.getClientId().isEmpty() ||
            "dummy-client-id".equals(googleRegistration.getClientId())) {
            throw new RuntimeException("Google OAuth Client ID is not configured. Please set GOOGLE_CLIENT_ID environment variable.");
        }
        if (googleRegistration.getClientSecret() == null || googleRegistration.getClientSecret().isEmpty() ||
            "dummy-client-secret".equals(googleRegistration.getClientSecret())) {
            throw new RuntimeException("Google OAuth Client Secret is not configured. Please set GOOGLE_CLIENT_SECRET environment variable.");
        }

        log.info("🔑 Using Client ID: {}", googleRegistration.getClientId());
        log.info("🔗 Using Redirect URI: {}", redirectUri);

        return UriComponentsBuilder
            .fromUriString(googleRegistration.getProviderDetails().getAuthorizationUri())
            .queryParam("client_id", googleRegistration.getClientId())
            .queryParam("redirect_uri", redirectUri)
            .queryParam("scope", String.join(" ", googleRegistration.getScopes()))
            .queryParam("response_type", "code")
            .queryParam("access_type", "offline")
            .queryParam("prompt", "consent")
            .build()
            .toUriString();
    }

    @Override
    public LoginResponse processCallback(String code) {
        try {
            // Exchange code for tokens
            TokenResponse tokenResponse = exchangeCodeForTokens(code);

            // Get user info from Google
            GoogleUserInfo userInfo = getUserInfo(tokenResponse.getAccessToken());

            // Find or create user
            User user = findOrCreateUser(userInfo, tokenResponse);

            // Create login response
            return createLoginResponse(user);

        } catch (Exception e) {
            log.error("Error processing Google OAuth callback", e);
            throw new RuntimeException("OAuth authentication failed: " + e.getMessage());
        }
    }

    @Override
    public boolean refreshAccessToken(String refreshToken) {
        try {
            ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("client_id", googleRegistration.getClientId());
            params.add("client_secret", googleRegistration.getClientSecret());
            params.add("refresh_token", refreshToken);
            params.add("grant_type", "refresh_token");

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                googleRegistration.getProviderDetails().getTokenUri(),
                request,
                String.class
            );

            if (response.getStatusCode() == HttpStatus.OK) {
                JsonNode jsonResponse = objectMapper.readTree(response.getBody());
                String newAccessToken = jsonResponse.get("access_token").asText();

                // Update user's access token in database
                Optional<User> userOpt = userRepository.findByRefreshToken(refreshToken);
                if (userOpt.isPresent()) {
                    User user = userOpt.get();
                    user.setAccessToken(newAccessToken);
                    if (jsonResponse.has("expires_in")) {
                        int expiresIn = jsonResponse.get("expires_in").asInt();
                        user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(expiresIn));
                    }
                    userRepository.save(user);
                    return true;
                }
            }

            return false;
        } catch (Exception e) {
            log.error("Error refreshing access token", e);
            return false;
        }
    }

    private TokenResponse exchangeCodeForTokens(String code) throws Exception {
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");

        log.info("🔄 Exchanging code for tokens...");
        log.info("📍 Token URI: {}", googleRegistration.getProviderDetails().getTokenUri());
        log.info("🔑 Client ID: {}", googleRegistration.getClientId());
        log.info("🔗 Redirect URI: {}", redirectUri);
        log.info("📝 Code length: {}", code != null ? code.length() : "null");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("client_id", googleRegistration.getClientId());
        params.add("client_secret", googleRegistration.getClientSecret());
        params.add("code", code);
        params.add("grant_type", "authorization_code");
        params.add("redirect_uri", redirectUri);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        log.info("🚀 Making token exchange request to Google...");
        ResponseEntity<String> response;
        try {
            response = restTemplate.postForEntity(
                googleRegistration.getProviderDetails().getTokenUri(),
                request,
                String.class
            );

            log.info("✅ Token exchange successful: {}", response.getStatusCode());

            if (response.getStatusCode() != HttpStatus.OK) {
                log.error("❌ Token exchange failed with status: {}", response.getStatusCode());
                throw new RuntimeException("Failed to exchange code for tokens");
            }
        } catch (Exception e) {
            log.error("❌ Token exchange failed with error: {}", e.getMessage());

            // Try to get more details from the error response
            if (e instanceof org.springframework.web.client.HttpClientErrorException) {
                org.springframework.web.client.HttpClientErrorException httpError =
                    (org.springframework.web.client.HttpClientErrorException) e;
                log.error("📄 HTTP Status: {}", httpError.getStatusCode());
                log.error("📄 Response Body: {}", httpError.getResponseBodyAsString());
                log.error("📄 Response Headers: {}", httpError.getResponseHeaders());
            }

            log.error("🔍 Check your Google OAuth configuration:");
            log.error("  - Client ID: {}", googleRegistration.getClientId());
            log.error("  - Redirect URI: {}", redirectUri);
            log.error("  - Make sure these match your Google Console settings");
            log.error("  - Verify that your Client Secret is correct");
            log.error("  - Check that the authorization code hasn't expired (they expire in ~10 minutes)");
            throw e;
        }

        JsonNode jsonResponse = objectMapper.readTree(response.getBody());

        TokenResponse tokenResponse = new TokenResponse();
        tokenResponse.setAccessToken(jsonResponse.get("access_token").asText());
        tokenResponse.setRefreshToken(jsonResponse.has("refresh_token") ?
            jsonResponse.get("refresh_token").asText() : null);
        tokenResponse.setExpiresIn(jsonResponse.has("expires_in") ?
            jsonResponse.get("expires_in").asInt() : 3600);

        return tokenResponse;
    }

    private GoogleUserInfo getUserInfo(String accessToken) throws Exception {
        ClientRegistration googleRegistration = clientRegistrationRepository.findByRegistrationId("google");

        log.info("🔍 Getting user info from Google...");
        log.info("📍 User info URL: {}", googleRegistration.getProviderDetails().getUserInfoEndpoint().getUri());
        log.info("🔑 Access token length: {}", accessToken != null ? accessToken.length() : "null");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);

        HttpEntity<String> request = new HttpEntity<>(headers);

        ResponseEntity<String> response = restTemplate.exchange(
            googleRegistration.getProviderDetails().getUserInfoEndpoint().getUri(),
            HttpMethod.GET,
            request,
            String.class
        );

        log.info("📊 User info response status: {}", response.getStatusCode());
        log.info("📊 User info response body length: {}",
                response.getBody() != null ? response.getBody().length() : "null");

        if (response.getStatusCode() != HttpStatus.OK) {
            log.error("❌ Failed to get user info - Status: {}, Body: {}",
                     response.getStatusCode(), response.getBody());
            throw new RuntimeException("Failed to get user info from Google");
        }

        JsonNode jsonResponse = objectMapper.readTree(response.getBody());

        log.info("📄 Google user info response: {}", jsonResponse.toString());

        GoogleUserInfo userInfo = new GoogleUserInfo();

        // Safely extract user info with null checks
        JsonNode emailNode = jsonResponse.get("email");
        JsonNode nameNode = jsonResponse.get("name");
        JsonNode subNode = jsonResponse.get("sub");
        JsonNode pictureNode = jsonResponse.get("picture");

        if (emailNode == null || emailNode.isNull()) {
            throw new RuntimeException("Email is required but not found in Google user info response");
        }
        if (subNode == null || subNode.isNull()) {
            throw new RuntimeException("User ID (sub) is required but not found in Google user info response");
        }

        userInfo.setEmail(emailNode.asText());
        userInfo.setName(nameNode != null && !nameNode.isNull() ? nameNode.asText() : emailNode.asText());
        userInfo.setPicture(pictureNode != null && !pictureNode.isNull() ? pictureNode.asText() : null);
        userInfo.setGoogleId(subNode.asText());

        log.info("✅ Successfully parsed user info: email={}, name={}, id={}",
                userInfo.getEmail(), userInfo.getName(), userInfo.getGoogleId());

        return userInfo;
    }

    private User findOrCreateUser(GoogleUserInfo userInfo, TokenResponse tokenResponse) {
        Optional<User> existingUser = userRepository.findByEmail(userInfo.getEmail());

        User user;
        if (existingUser.isPresent()) {
            user = existingUser.get();
            // Update OAuth fields
            user.setProvider(User.Provider.GOOGLE);
            user.setProviderId(userInfo.getGoogleId());
        } else {
            // Create new user
            user = new User();
            user.setEmail(userInfo.getEmail());
            user.setName(userInfo.getName());
            user.setProvider(User.Provider.GOOGLE);
            user.setProviderId(userInfo.getGoogleId());
        }

        // Update OAuth tokens
        user.setAccessToken(tokenResponse.getAccessToken());
        user.setRefreshToken(tokenResponse.getRefreshToken());
        user.setTokenExpiresAt(LocalDateTime.now().plusSeconds(tokenResponse.getExpiresIn()));
        user.setProfilePicture(userInfo.getPicture());

        return userRepository.save(user);
    }

    private LoginResponse createLoginResponse(User user) {
        UserDto userDto = new UserDto();
        userDto.setId(user.getId());
        userDto.setEmail(user.getEmail());
        userDto.setName(user.getName());
        userDto.setProvider(user.getProvider());
        userDto.setCreatedAt(user.getCreatedAt());

        String jwtToken = jwtUtil.generateToken(user.getEmail());

        LoginResponse response = new LoginResponse();
        response.setToken(jwtToken);
        response.setRefreshToken("refresh-token-" + user.getId());
        response.setUser(userDto);
        response.setExpiresIn(86400000L); // 24 hours

        return response;
    }

    // Inner classes for data transfer
    private static class TokenResponse {
        private String accessToken;
        private String refreshToken;
        private int expiresIn;

        // Getters and setters
        public String getAccessToken() { return accessToken; }
        public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
        public String getRefreshToken() { return refreshToken; }
        public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
        public int getExpiresIn() { return expiresIn; }
        public void setExpiresIn(int expiresIn) { this.expiresIn = expiresIn; }
    }

    private static class GoogleUserInfo {
        private String email;
        private String name;
        private String picture;
        private String googleId;

        // Getters and setters
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getPicture() { return picture; }
        public void setPicture(String picture) { this.picture = picture; }
        public String getGoogleId() { return googleId; }
        public void setGoogleId(String googleId) { this.googleId = googleId; }
    }
}