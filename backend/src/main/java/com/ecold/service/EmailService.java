package com.ecold.service;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.User;
import java.time.LocalDateTime;
import java.util.Map;

public interface EmailService {
    
    /**
     * Send email directly from the application
     */
    EmailResponse sendEmail(EmailRequest emailRequest, User user);
    
    /**
     * Send email using template with placeholder replacement
     */
    EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData);

    /**
     * Send email using template with placeholder replacement and scheduling
     */
    EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData, LocalDateTime scheduleTime);
    
    /**
     * Test email configuration
     */
    boolean testEmailConfiguration(User user);
    
    /**
     * Validate email settings
     */
    boolean validateEmailSettings();
    
    /**
     * Send test email
     */
    EmailResponse sendTestEmail(String toEmail, User user);
}
