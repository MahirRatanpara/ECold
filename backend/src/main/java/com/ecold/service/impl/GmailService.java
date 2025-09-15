package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.*;
import com.ecold.repository.EmailLogRepository;
import com.ecold.service.EmailService;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Properties;

// Temporarily disabled to avoid conflicts
// @Service
@RequiredArgsConstructor
@Slf4j
public class GmailService implements EmailService {
    
    private final EmailLogRepository emailLogRepository;

    // Required EmailService interface methods
    @Override
    public EmailResponse sendEmail(EmailRequest emailRequest, User user) {
        // TODO: Implement this method
        return EmailResponse.failure("NOT_IMPLEMENTED", "Gmail service not fully implemented yet");
    }

    @Override
    public EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData) {
        // TODO: Implement this method
        return EmailResponse.failure("NOT_IMPLEMENTED", "Gmail service not fully implemented yet");
    }

    @Override
    public boolean testEmailConfiguration(User user) {
        return validateEmailConnection(user);
    }

    @Override
    public boolean validateEmailSettings() {
        // TODO: Implement this method
        return false;
    }

    @Override
    public EmailResponse sendTestEmail(String toEmail, User user) {
        // TODO: Implement this method
        return EmailResponse.failure("NOT_IMPLEMENTED", "Gmail service not fully implemented yet");
    }

    // Legacy methods - to be refactored later
    public EmailLog sendEmail(User user, RecruiterContact recruiter, String subject, String body, Resume resume, Map<String, String> placeholders) {
        EmailLog emailLog = createEmailLog(recruiter, subject, body);
        
        try {
            Gmail gmail = createGmailService(user);
            String processedSubject = replacePlaceholders(subject, placeholders);
            String processedBody = replacePlaceholders(body, placeholders);
            
            MimeMessage mimeMessage = createMimeMessage(user.getEmail(), recruiter.getEmail(), processedSubject, processedBody, resume);
            Message message = createMessageWithEmail(mimeMessage);
            
            Message sentMessage = gmail.users().messages().send("me", message).execute();
            
            emailLog.setStatus(EmailLog.EmailStatus.SENT);
            emailLog.setMessageId(sentMessage.getId());
            emailLog.setSentAt(LocalDateTime.now());
            
            recruiter.setLastContactedAt(LocalDateTime.now());
            recruiter.setStatus(RecruiterContact.ContactStatus.CONTACTED);
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recruiter.getEmail(), e.getMessage(), e);
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
        }
        
        return emailLogRepository.save(emailLog);
    }
    
    public EmailLog sendTestEmail(User user, String recipientEmail, String subject, String body, Resume resume) {
        EmailLog emailLog = new EmailLog();
        emailLog.setRecipientEmail(recipientEmail);
        emailLog.setSubject(subject);
        emailLog.setBody(body);
        emailLog.setStatus(EmailLog.EmailStatus.PENDING);
        
        try {
            Gmail gmail = createGmailService(user);
            MimeMessage mimeMessage = createMimeMessage(user.getEmail(), recipientEmail, subject, body, resume);
            Message message = createMessageWithEmail(mimeMessage);
            
            Message sentMessage = gmail.users().messages().send("me", message).execute();
            
            emailLog.setStatus(EmailLog.EmailStatus.SENT);
            emailLog.setMessageId(sentMessage.getId());
            emailLog.setSentAt(LocalDateTime.now());
            
        } catch (Exception e) {
            log.error("Failed to send test email to {}: {}", recipientEmail, e.getMessage(), e);
            emailLog.setStatus(EmailLog.EmailStatus.FAILED);
            emailLog.setErrorMessage(e.getMessage());
        }
        
        return emailLogRepository.save(emailLog);
    }
    
    public void sendBulkEmails(User user, List<RecruiterContact> recruiters, String subject, String body, Resume resume) {
        for (RecruiterContact recruiter : recruiters) {
            Map<String, String> placeholders = Map.of(
                "{RecruiterName}", recruiter.getRecruiterName() != null ? recruiter.getRecruiterName() : "Recruiter",
                "{Company}", recruiter.getCompanyName(),
                "{Role}", recruiter.getJobRole(),
                "{MyName}", user.getName()
            );
            sendEmail(user, recruiter, subject, body, resume, placeholders);
            
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    public boolean validateEmailConnection(User user) {
        try {
            Gmail gmail = createGmailService(user);
            gmail.users().getProfile("me").execute();
            return true;
        } catch (Exception e) {
            log.error("Failed to validate Gmail connection for user {}: {}", user.getEmail(), e.getMessage());
            return false;
        }
    }
    
    public List<EmailLog> getEmailHistory(User user, int page, int size) {
        Page<EmailLog> emailLogs = emailLogRepository.findAll(PageRequest.of(page, size));
        return emailLogs.getContent();
    }
    
    public EmailLog retryFailedEmail(Long emailLogId) {
        EmailLog emailLog = emailLogRepository.findById(emailLogId)
            .orElseThrow(() -> new RuntimeException("Email log not found"));
        
        if (emailLog.getStatus() != EmailLog.EmailStatus.FAILED) {
            throw new RuntimeException("Email is not in failed state");
        }
        
        emailLog.setRetryCount(emailLog.getRetryCount() + 1);
        emailLog.setStatus(EmailLog.EmailStatus.PENDING);
        emailLog.setErrorMessage(null);
        
        return emailLogRepository.save(emailLog);
    }
    
    private Gmail createGmailService(User user) throws IOException {
        GoogleCredential credential = new GoogleCredential.Builder()
            .setTransport(new NetHttpTransport())
            .setJsonFactory(GsonFactory.getDefaultInstance())
            .build()
            .setAccessToken(user.getAccessToken())
            .setRefreshToken(user.getRefreshToken());
        
        return new Gmail.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance(), credential)
            .setApplicationName("ECold")
            .build();
    }
    
    private MimeMessage createMimeMessage(String fromEmail, String toEmail, String subject, String body, Resume resume) throws MessagingException, IOException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);
        
        MimeMessage email = new MimeMessage(session);
        email.setFrom(new InternetAddress(fromEmail));
        email.addRecipient(jakarta.mail.Message.RecipientType.TO, new InternetAddress(toEmail));
        email.setSubject(subject);
        
        MimeMultipart multipart = new MimeMultipart();
        
        MimeBodyPart textPart = new MimeBodyPart();
        textPart.setText(body);
        multipart.addBodyPart(textPart);
        
        if (resume != null) {
            MimeBodyPart attachmentPart = new MimeBodyPart();
            attachmentPart.attachFile(new File(resume.getFilePath()));
            attachmentPart.setFileName(resume.getFileName());
            multipart.addBodyPart(attachmentPart);
        }
        
        email.setContent(multipart);
        
        return email;
    }
    
    private Message createMessageWithEmail(MimeMessage emailContent) throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }
    
    private String replacePlaceholders(String template, Map<String, String> placeholders) {
        String result = template;
        for (Map.Entry<String, String> entry : placeholders.entrySet()) {
            result = result.replace(entry.getKey(), entry.getValue());
        }
        return result;
    }
    
    private EmailLog createEmailLog(RecruiterContact recruiter, String subject, String body) {
        EmailLog emailLog = new EmailLog();
        emailLog.setRecruiterContact(recruiter);
        emailLog.setRecipientEmail(recruiter.getEmail());
        emailLog.setSubject(subject);
        emailLog.setBody(body);
        emailLog.setStatus(EmailLog.EmailStatus.PENDING);
        emailLog.setRetryCount(0);
        return emailLog;
    }
}