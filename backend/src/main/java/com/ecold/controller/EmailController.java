package com.ecold.controller;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.User;
import com.ecold.service.EmailService;
import com.ecold.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/emails")
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:3000"})
public class EmailController {

    private final EmailService emailService;
    private final UserRepository userRepository;

    public EmailController(@Qualifier("emailServiceImpl") EmailService emailService,
                          UserRepository userRepository) {
        this.emailService = emailService;
        this.userRepository = userRepository;
    }

    @PostMapping("/send")
    public ResponseEntity<EmailResponse> sendEmail(
            @Valid @RequestBody EmailRequest emailRequest,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            EmailResponse response = emailService.sendEmail(emailRequest, currentUser);

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error sending email: {}", e.getMessage(), e);
            EmailResponse errorResponse = EmailResponse.failure("INTERNAL_ERROR", "Internal server error occurred");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/send-template")
    public ResponseEntity<EmailResponse> sendTemplateEmail(
            @RequestParam Long templateId,
            @RequestParam Long recruiterId,
            @RequestParam(required = false) String scheduleTime,
            @RequestBody(required = false) Map<String, String> additionalData,
            Authentication authentication) {

        try {
            User currentUser = getCurrentUser(authentication);
            EmailResponse response;

            if (scheduleTime != null && !scheduleTime.trim().isEmpty()) {
                java.time.Instant instant = java.time.Instant.parse(scheduleTime);
                LocalDateTime scheduledDateTime = LocalDateTime.ofInstant(instant, java.time.ZoneId.systemDefault());
                response = emailService.sendTemplateEmail(templateId, recruiterId, currentUser, additionalData, scheduledDateTime);
            } else {
                response = emailService.sendTemplateEmail(templateId, recruiterId, currentUser, additionalData);
            }

            if (response.isSuccess()) {
                return ResponseEntity.ok(response);
            } else {
                return ResponseEntity.badRequest().body(response);
            }

        } catch (Exception e) {
            log.error("Error sending template email: {}", e.getMessage(), e);
            EmailResponse errorResponse = EmailResponse.failure("INTERNAL_ERROR", "Internal server error occurred");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @PostMapping("/test")
    public ResponseEntity<EmailResponse> sendTestEmail(
            @RequestParam String toEmail,
            Authentication authentication) {
        
        try {
            User currentUser = getCurrentUser(authentication);
            EmailResponse response = emailService.sendTestEmail(toEmail, currentUser);
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error sending test email: {}", e.getMessage(), e);
            EmailResponse errorResponse = EmailResponse.failure("INTERNAL_ERROR", "Internal server error occurred");
            return ResponseEntity.status(500).body(errorResponse);
        }
    }

    @GetMapping("/config/test")
    public ResponseEntity<Map<String, Object>> testEmailConfiguration(Authentication authentication) {
        try {
            User currentUser = getCurrentUser(authentication);
            boolean isConfigValid = emailService.validateEmailSettings();
            boolean testPassed = false;
            
            if (isConfigValid) {
                testPassed = emailService.testEmailConfiguration(currentUser);
            }
            
            Map<String, Object> result = new HashMap<>();
            result.put("configValid", isConfigValid);
            result.put("testPassed", testPassed);
            result.put("message", isConfigValid ? 
                (testPassed ? "Email configuration is working correctly" : "Email configuration has issues") :
                "Email configuration is not set up");
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Error testing email configuration: {}", e.getMessage(), e);
            Map<String, Object> result = new HashMap<>();
            result.put("configValid", false);
            result.put("testPassed", false);
            result.put("message", "Error testing configuration: " + e.getMessage());
            return ResponseEntity.status(500).body(result);
        }
    }

    @GetMapping("/config/status")
    public ResponseEntity<Map<String, Object>> getEmailConfigurationStatus() {
        Map<String, Object> status = new HashMap<>();

        boolean isValid = emailService.validateEmailSettings();
        status.put("configured", isValid);
        status.put("message", isValid ? "Email is configured" : "Email configuration missing");

        return ResponseEntity.ok(status);
    }


    private User getCurrentUser(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new RuntimeException("User not authenticated");
        }
        
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found: " + email));
    }
}
