package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.User;
import com.ecold.repository.UserRepository;
import com.ecold.service.EmailService;
import com.ecold.service.GoogleOAuthService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

@Slf4j
@Service("gmailOAuthService")
@RequiredArgsConstructor
public class GmailOAuthServiceImpl implements EmailService {

    private final UserRepository userRepository;
    private final GoogleOAuthService googleOAuthService;

    @Override
    public EmailResponse sendEmail(EmailRequest emailRequest, User user) {
        log.info("Sending email via Gmail OAuth for user: {} to: {}", user.getEmail(), emailRequest.getTo());

        try {
            // Check if user has valid Gmail tokens
            if (!hasValidGmailTokens(user)) {
                log.error("User {} does not have valid Gmail tokens", user.getEmail());
                return EmailResponse.failure("NO_GMAIL_TOKEN", "User is not authenticated with Gmail");
            }

            // Try to refresh token if needed
            if (isTokenExpired(user)) {
                boolean refreshed = googleOAuthService.refreshAccessToken(user.getRefreshToken());
                if (!refreshed) {
                    log.error("Failed to refresh Gmail token for user {}", user.getEmail());
                    return EmailResponse.failure("TOKEN_REFRESH_FAILED", "Failed to refresh Gmail authentication");
                }
                // Reload user with updated token
                user = userRepository.findById(user.getId()).orElse(user);
            }

            // Send email using Gmail API
            Gmail gmailService = createGmailService(user);
            String messageId = sendGmailMessage(gmailService, emailRequest, user);

            log.info("Email sent successfully via Gmail OAuth to {} with messageId: {}", emailRequest.getTo(), messageId);
            return EmailResponse.success(messageId, "Email sent successfully via Gmail");

        } catch (Exception e) {
            log.error("Failed to send email via Gmail OAuth to {}: {}", emailRequest.getTo(), e.getMessage(), e);
            return EmailResponse.failure("GMAIL_SEND_FAILED", "Failed to send email via Gmail: " + e.getMessage());
        }
    }

    @Override
    public EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData) {
        // This will be implemented by the main EmailServiceImpl which can delegate to this service
        throw new UnsupportedOperationException("Template email sending should be handled by EmailServiceImpl");
    }

    @Override
    public boolean testEmailConfiguration(User user) {
        try {
            if (!hasValidGmailTokens(user)) {
                return false;
            }

            EmailRequest testRequest = EmailRequest.builder()
                    .to(user.getEmail())
                    .subject("ECold Gmail OAuth Test")
                    .body("This is a test email to verify your Gmail OAuth configuration is working correctly.\n\nIf you receive this email, your Gmail OAuth settings are properly configured!")
                    .isHtml(false)
                    .priority(EmailRequest.Priority.NORMAL)
                    .build();

            EmailResponse response = sendEmail(testRequest, user);
            return response.isSuccess();

        } catch (Exception e) {
            log.error("Gmail OAuth configuration test failed: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean validateEmailSettings() {
        // For Gmail OAuth, we need to check if the current user has valid tokens
        try {
            String email = SecurityContextHolder.getContext().getAuthentication().getName();
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null && hasValidGmailTokens(user);
        } catch (Exception e) {
            log.error("Error validating Gmail OAuth settings: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public EmailResponse sendTestEmail(String toEmail, User user) {
        EmailRequest testRequest = EmailRequest.builder()
                .to(toEmail)
                .subject("ECold Gmail OAuth Test")
                .body("Hello!\n\nThis is a test email from ECold application using Gmail OAuth.\n\nTime: " + LocalDateTime.now() + "\n\nBest regards,\nECold Team")
                .isHtml(false)
                .priority(EmailRequest.Priority.NORMAL)
                .build();

        return sendEmail(testRequest, user);
    }

    private boolean hasValidGmailTokens(User user) {
        return user.getProvider() == User.Provider.GOOGLE &&
               user.getAccessToken() != null &&
               !user.getAccessToken().isEmpty();
    }

    private boolean isTokenExpired(User user) {
        return user.getTokenExpiresAt() != null &&
               user.getTokenExpiresAt().isBefore(LocalDateTime.now().minusMinutes(5)); // 5 min buffer
    }

    private Gmail createGmailService(User user) throws Exception {
        GoogleCredential credential = new GoogleCredential.Builder()
                .setTransport(GoogleNetHttpTransport.newTrustedTransport())
                .setJsonFactory(GsonFactory.getDefaultInstance())
                .build()
                .setAccessToken(user.getAccessToken());

        return new Gmail.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                credential)
                .setApplicationName("ECold Application")
                .build();
    }

    private String sendGmailMessage(Gmail gmailService, EmailRequest emailRequest, User user) throws Exception {
        MimeMessage mimeMessage = createMimeMessage(emailRequest, user);
        Message message = createGmailMessage(mimeMessage);

        Message sentMessage = gmailService.users().messages().send("me", message).execute();
        return sentMessage.getId();
    }

    private MimeMessage createMimeMessage(EmailRequest emailRequest, User user) throws MessagingException, UnsupportedEncodingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage mimeMessage = new MimeMessage(session);
        mimeMessage.setFrom(new InternetAddress(user.getEmail(), user.getName()));
        mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(emailRequest.getTo()));
        mimeMessage.setSubject(emailRequest.getSubject());

        if (emailRequest.isHtml()) {
            mimeMessage.setContent(emailRequest.getBody(), "text/html; charset=utf-8");
        } else {
            mimeMessage.setText(emailRequest.getBody());
        }

        // Add CC recipients
        if (emailRequest.getCc() != null && !emailRequest.getCc().isEmpty()) {
            for (String cc : emailRequest.getCc()) {
                mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.CC, new InternetAddress(cc));
            }
        }

        // Add BCC recipients
        if (emailRequest.getBcc() != null && !emailRequest.getBcc().isEmpty()) {
            for (String bcc : emailRequest.getBcc()) {
                mimeMessage.addRecipient(jakarta.mail.Message.RecipientType.BCC, new InternetAddress(bcc));
            }
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

        return mimeMessage;
    }

    private Message createGmailMessage(MimeMessage mimeMessage) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        mimeMessage.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
}