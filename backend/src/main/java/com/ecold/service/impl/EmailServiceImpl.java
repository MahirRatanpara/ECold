package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.EmailTemplate;
import com.ecold.entity.RecruiterContact;
import com.ecold.entity.User;
import com.ecold.repository.EmailTemplateRepository;
import com.ecold.repository.RecruiterContactRepository;
import com.ecold.service.EmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmailTemplateRepository templateRepository;
    private final RecruiterContactRepository recruiterRepository;
    
    @Value("${spring.mail.username:#{null}}")
    private String fromEmail;
    
    @Value("${app.email.from-name:ECold Application}")
    private String fromName;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public EmailResponse sendEmail(EmailRequest emailRequest, User user) {
        log.info("Starting email send process for user: {} to: {}", user.getEmail(), emailRequest.getTo());
        log.info("Email enabled: {}, From email: {}", emailEnabled, fromEmail);

        if (!emailEnabled) {
            log.warn("Email sending is disabled in configuration");
            return EmailResponse.failure("EMAIL_DISABLED", "Email sending is disabled in application configuration");
        }

        boolean settingsValid = validateEmailSettings();
        log.info("Email settings validation result: {}", settingsValid);
        if (!settingsValid) {
            log.error("Email validation failed - fromEmail: {}, mailSender: {}", fromEmail, mailSender != null);
            return EmailResponse.failure("EMAIL_CONFIG_INVALID", "Email configuration is not properly set up");
        }

        try {
            String messageId = generateMessageId();
            
            if (emailRequest.isHtml()) {
                sendHtmlEmail(emailRequest, user, messageId);
            } else {
                sendTextEmail(emailRequest, user, messageId);
            }
            
            log.info("Email sent successfully to {} with messageId: {}", emailRequest.getTo(), messageId);
            return EmailResponse.success(messageId, "Email sent successfully");
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {} - Exception type: {}", emailRequest.getTo(), e.getMessage(), e.getClass().getSimpleName(), e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("forbidden")) {
                log.error("SMTP Forbidden error detected - check Gmail app password and account settings");
                return EmailResponse.failure("SMTP_FORBIDDEN", "SMTP authentication failed - check email credentials");
            }
            return EmailResponse.failure("SEND_FAILED", "Failed to send email: " + e.getMessage());
        }
    }

    @Override
    public EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData) {
        try {
            // Get template
            EmailTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
            
            // Get recruiter
            RecruiterContact recruiter = recruiterRepository.findById(recruiterId)
                    .orElseThrow(() -> new RuntimeException("Recruiter not found: " + recruiterId));
            
            // Verify ownership
            if (!template.getUser().getId().equals(user.getId()) || 
                !recruiter.getUser().getId().equals(user.getId())) {
                return EmailResponse.failure("ACCESS_DENIED", "Access denied to template or recruiter");
            }
            
            // Process template placeholders
            String processedSubject = processPlaceholders(template.getSubject(), recruiter, user, additionalData);
            String processedBody = processPlaceholders(template.getBody(), recruiter, user, additionalData);
            
            // Create email request
            EmailRequest emailRequest = EmailRequest.builder()
                    .to(recruiter.getEmail())
                    .subject(processedSubject)
                    .body(processedBody)
                    .isHtml(false)
                    .templateId(templateId)
                    .recruiterId(recruiterId)
                    .priority(EmailRequest.Priority.NORMAL)
                    .build();
            
            EmailResponse response = sendEmail(emailRequest, user);
            
            if (response.isSuccess()) {
                // Update template usage
                template.setUsageCount((template.getUsageCount() != null ? template.getUsageCount() : 0L) + 1);
                template.setEmailsSent((template.getEmailsSent() != null ? template.getEmailsSent() : 0L) + 1);
                template.setLastUsed(LocalDateTime.now());
                templateRepository.save(template);
                
                // Mark recruiter as contacted
                recruiter.setStatus(RecruiterContact.ContactStatus.CONTACTED);
                recruiter.setLastContactedAt(LocalDateTime.now());
                recruiterRepository.save(recruiter);
            }
            
            return response;
            
        } catch (Exception e) {
            log.error("Failed to send template email: {}", e.getMessage(), e);
            return EmailResponse.failure("TEMPLATE_SEND_FAILED", "Failed to send template email: " + e.getMessage());
        }
    }

    @Override
    public boolean testEmailConfiguration(User user) {
        if (!emailEnabled || !validateEmailSettings()) {
            return false;
        }
        
        try {
            EmailRequest testRequest = EmailRequest.builder()
                    .to(user.getEmail())
                    .subject("ECold Email Configuration Test")
                    .body("This is a test email to verify your ECold email configuration is working correctly.\n\nIf you receive this email, your email settings are properly configured!")
                    .isHtml(false)
                    .priority(EmailRequest.Priority.NORMAL)
                    .build();
            
            EmailResponse response = sendEmail(testRequest, user);
            return response.isSuccess();
            
        } catch (Exception e) {
            log.error("Email configuration test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean validateEmailSettings() {
        return fromEmail != null && !fromEmail.trim().isEmpty() && mailSender != null;
    }

    @Override
    public EmailResponse sendTestEmail(String toEmail, User user) {
        EmailRequest testRequest = EmailRequest.builder()
                .to(toEmail)
                .subject("ECold Test Email")
                .body("Hello!\n\nThis is a test email from ECold application.\n\nTime: " + LocalDateTime.now() + "\n\nBest regards,\nECold Team")
                .isHtml(false)
                .priority(EmailRequest.Priority.NORMAL)
                .build();
        
        return sendEmail(testRequest, user);
    }

    private void sendTextEmail(EmailRequest emailRequest, User user, String messageId) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(emailRequest.getTo());
        message.setSubject(emailRequest.getSubject());
        message.setText(emailRequest.getBody());
        
        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            message.setCc(emailRequest.getCc().toArray(new String[0]));
        }
        
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            message.setBcc(emailRequest.getBcc().toArray(new String[0]));
        }
        
        // Add message ID header (this is simplified - actual implementation depends on mail server)
        mailSender.send(message);
    }

    private void sendHtmlEmail(EmailRequest emailRequest, User user, String messageId) throws MessagingException, java.io.UnsupportedEncodingException {
        MimeMessage mimeMessage = mailSender.createMimeMessage();
        MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
        
        helper.setFrom(fromEmail, fromName);
        helper.setTo(emailRequest.getTo());
        helper.setSubject(emailRequest.getSubject());
        helper.setText(emailRequest.getBody(), true);
        
        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            helper.setCc(emailRequest.getCc().toArray(new String[0]));
        }
        
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            helper.setBcc(emailRequest.getBcc().toArray(new String[0]));
        }
        
        // Set priority
        if (emailRequest.getPriority() != null) {
            switch (emailRequest.getPriority()) {
                case HIGH:
                    mimeMessage.addHeader("X-Priority", "1");
                    mimeMessage.addHeader("X-MSMail-Priority", "High");
                    break;
                case LOW:
                    mimeMessage.addHeader("X-Priority", "5");
                    mimeMessage.addHeader("X-MSMail-Priority", "Low");
                    break;
                default:
                    // Normal priority - no headers needed
                    break;
            }
        }
        
        mailSender.send(mimeMessage);
    }

    private String processPlaceholders(String text, RecruiterContact recruiter, User user, Map<String, String> additionalData) {
        if (text == null) return "";
        
        String processed = text;
        
        // Standard placeholders
        processed = processed.replace("{Company}", recruiter.getCompanyName() != null ? recruiter.getCompanyName() : "");
        processed = processed.replace("{Role}", recruiter.getJobRole() != null ? recruiter.getJobRole() : "");
        processed = processed.replace("{RecruiterName}", recruiter.getRecruiterName() != null ? recruiter.getRecruiterName() : "");
        processed = processed.replace("{MyName}", user.getName() != null ? user.getName() : "");
        
        // Additional data placeholders
        if (additionalData != null) {
            for (Map.Entry<String, String> entry : additionalData.entrySet()) {
                String placeholder = "{" + entry.getKey() + "}";
                processed = processed.replace(placeholder, entry.getValue() != null ? entry.getValue() : "");
            }
        }
        
        return processed;
    }

    private String generateMessageId() {
        return "ecold-" + UUID.randomUUID().toString();
    }
}
