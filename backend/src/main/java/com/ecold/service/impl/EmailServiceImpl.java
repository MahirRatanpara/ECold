package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.EmailTemplate;
import com.ecold.entity.RecruiterContact;
import com.ecold.entity.RecruiterTemplateAssignment;
import com.ecold.entity.User;
import com.ecold.repository.EmailTemplateRepository;
import com.ecold.repository.RecruiterContactRepository;
import com.ecold.repository.RecruiterTemplateAssignmentRepository;
import com.ecold.repository.UserRepository;
import com.ecold.service.EmailService;
import com.ecold.service.ScheduledEmailService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.security.core.context.SecurityContextHolder;
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
public class EmailServiceImpl implements EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    private final EmailTemplateRepository templateRepository;
    private final RecruiterContactRepository recruiterRepository;
    private final UserRepository userRepository;
    private final RecruiterTemplateAssignmentRepository assignmentRepository;

    @Qualifier("gmailOAuthService")
    private final EmailService gmailOAuthService;

    private final ScheduledEmailService scheduledEmailService;

    public EmailServiceImpl(EmailTemplateRepository templateRepository,
                           RecruiterContactRepository recruiterRepository,
                           UserRepository userRepository,
                           RecruiterTemplateAssignmentRepository assignmentRepository,
                           @Qualifier("gmailOAuthService") EmailService gmailOAuthService,
                           ScheduledEmailService scheduledEmailService) {
        this.templateRepository = templateRepository;
        this.recruiterRepository = recruiterRepository;
        this.userRepository = userRepository;
        this.assignmentRepository = assignmentRepository;
        this.gmailOAuthService = gmailOAuthService;
        this.scheduledEmailService = scheduledEmailService;
    }
    
    @Value("${spring.mail.username:#{null}}")
    private String fromEmail;
    
    @Value("${app.email.from-name:ECold Application}")
    private String fromName;
    
    @Value("${app.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public EmailResponse sendEmail(EmailRequest emailRequest, User user) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleTime = emailRequest.getScheduleTime();

        if (scheduleTime != null && scheduleTime.isAfter(now)) {
            if (user.getProvider() != User.Provider.GOOGLE) {
                return EmailResponse.failure("SCHEDULE_NOT_SUPPORTED", "Scheduled sending requires Gmail authentication. Please sign in with Google.");
            }

            if (!hasValidGmailTokens(user)) {
                return EmailResponse.failure("SCHEDULE_NOT_SUPPORTED", "Scheduled sending requires Gmail authentication. Please sign in with Google.");
            }

            scheduledEmailService.scheduleEmail(emailRequest, user, emailRequest.getScheduleTime());
            return EmailResponse.success(null, "Email scheduled successfully for " + emailRequest.getScheduleTime());
        }

        if (user.getProvider() == User.Provider.GOOGLE && hasValidGmailTokens(user)) {
            EmailResponse response = gmailOAuthService.sendEmail(emailRequest, user);

            if (response.isSuccess() && emailRequest.getTemplateId() != null && emailRequest.getRecruiterId() != null) {
                updateAssignmentEmailCount(emailRequest.getTemplateId(), emailRequest.getRecruiterId(), user);
            }

            return response;
        }

        if (!emailEnabled) {
            return EmailResponse.failure("EMAIL_DISABLED", "Email sending is disabled in application configuration");
        }

        if (!validateEmailSettings()) {
            return EmailResponse.failure("SMTP_NOT_CONFIGURED", "SMTP email is not configured. Please sign in with Google to send emails from your Gmail account.");
        }

        try {
            String messageId = generateMessageId();

            if (emailRequest.isHtml()) {
                sendHtmlEmail(emailRequest, user, messageId);
            } else {
                sendTextEmail(emailRequest, user, messageId);
            }

            if (emailRequest.getTemplateId() != null && emailRequest.getRecruiterId() != null) {
                updateAssignmentEmailCount(emailRequest.getTemplateId(), emailRequest.getRecruiterId(), user);
            }

            return EmailResponse.success(messageId, "Email sent successfully");

        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", emailRequest.getTo(), e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("forbidden")) {
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

            // Create email request without template/recruiter info to avoid duplicate counting
            EmailRequest emailRequestWithoutIds = EmailRequest.builder()
                    .to(emailRequest.getTo())
                    .subject(emailRequest.getSubject())
                    .body(emailRequest.getBody())
                    .isHtml(emailRequest.isHtml())
                    .priority(emailRequest.getPriority())
                    .build();

            EmailResponse response = sendEmail(emailRequestWithoutIds, user);

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

                // Update assignment email count if assignment exists (only here, not in sendEmail)
                updateAssignmentEmailCount(templateId, recruiterId, user);

                // Move to follow-up template if applicable
                moveToFollowUpIfApplicable(templateId, recruiterId, user, template);
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to send template email: {}", e.getMessage(), e);
            return EmailResponse.failure("TEMPLATE_SEND_FAILED", "Failed to send template email: " + e.getMessage());
        }
    }

    @Override
    public EmailResponse sendTemplateEmail(Long templateId, Long recruiterId, User user, Map<String, String> additionalData, LocalDateTime scheduleTime) {
        try {
            EmailTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));

            RecruiterContact recruiter = recruiterRepository.findById(recruiterId)
                    .orElseThrow(() -> new RuntimeException("Recruiter not found: " + recruiterId));

            if (!template.getUser().getId().equals(user.getId()) ||
                !recruiter.getUser().getId().equals(user.getId())) {
                return EmailResponse.failure("ACCESS_DENIED", "Access denied to template or recruiter");
            }

            String processedSubject = processPlaceholders(template.getSubject(), recruiter, user, additionalData);
            String processedBody = processPlaceholders(template.getBody(), recruiter, user, additionalData);

            EmailRequest emailRequest = EmailRequest.builder()
                    .to(recruiter.getEmail())
                    .subject(processedSubject)
                    .body(processedBody)
                    .isHtml(false)
                    .templateId(templateId)
                    .recruiterId(recruiterId)
                    .priority(EmailRequest.Priority.NORMAL)
                    .build();

            if (scheduleTime != null && scheduleTime.isAfter(LocalDateTime.now())) {
                emailRequest.setScheduleTime(scheduleTime);
            }

            EmailResponse response = sendEmail(emailRequest, user);

            if (response.isSuccess() && (scheduleTime == null || !scheduleTime.isAfter(LocalDateTime.now()))) {
                moveToFollowUpIfApplicable(templateId, recruiterId, user, template);
            }

            return response;

        } catch (Exception e) {
            log.error("Failed to send template email {} to recruiter {}: {}", templateId, recruiterId, e.getMessage(), e);
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
        // Check if SMTP is configured
        boolean smtpConfigured = fromEmail != null && !fromEmail.trim().isEmpty() && mailSender != null;

        if (smtpConfigured) {
            return true;
        }

        // If SMTP is not configured, check if we can use Gmail OAuth
        try {
            var authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication == null || authentication.getName() == null) {
                log.debug("No authentication context available for Gmail OAuth validation");
                return false;
            }

            String email = authentication.getName();
            User user = userRepository.findByEmail(email).orElse(null);
            return user != null && hasValidGmailTokens(user);
        } catch (Exception e) {
            log.debug("Could not validate Gmail OAuth settings: {}", e.getMessage());
            return false;
        }
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

    private boolean hasValidGmailTokens(User user) {
        return user.getAccessToken() != null &&
               !user.getAccessToken().isEmpty() &&
               (user.getTokenExpiresAt() == null ||
                user.getTokenExpiresAt().isAfter(LocalDateTime.now()));
    }

    private void updateAssignmentEmailCount(Long templateId, Long recruiterId, User user) {
        try {
            EmailTemplate template = templateRepository.findById(templateId).orElse(null);
            RecruiterContact recruiter = recruiterRepository.findById(recruiterId).orElse(null);

            if (template == null || recruiter == null) {
                return;
            }

            RecruiterTemplateAssignment assignment = assignmentRepository
                .findByRecruiterContactAndEmailTemplateAndAssignmentStatus(
                    recruiter, template, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

            if (assignment != null) {
                assignment.setEmailsSent(assignment.getEmailsSent() + 1);
                assignment.setLastEmailSentAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
            }
        } catch (Exception e) {
            log.error("Failed to update assignment email count: {}", e.getMessage());
        }
    }

    private void moveToFollowUpIfApplicable(Long templateId, Long recruiterId, User user, EmailTemplate currentTemplate) {
        try {
            RecruiterTemplateAssignment assignment = assignmentRepository
                    .findByTemplateIdAndRecruiterIdAndUserId(templateId, recruiterId, user.getId())
                    .orElse(null);

            if (assignment == null) {
                return;
            }

            EmailTemplate followUpTemplate = null;

            if (currentTemplate.getFollowUpTemplate() != null) {
                followUpTemplate = currentTemplate.getFollowUpTemplate();
            } else {
                java.util.List<EmailTemplate> followUpTemplates = templateRepository.findByUserAndCategoryAndStatus(
                        user,
                        EmailTemplate.Category.FOLLOW_UP,
                        EmailTemplate.Status.ACTIVE
                );

                if (!followUpTemplates.isEmpty()) {
                    followUpTemplate = followUpTemplates.get(0);
                }
            }

            if (followUpTemplate != null) {
                RecruiterTemplateAssignment followUpAssignment = new RecruiterTemplateAssignment();
                followUpAssignment.setRecruiterContact(assignment.getRecruiterContact());
                followUpAssignment.setEmailTemplate(followUpTemplate);
                followUpAssignment.setUser(assignment.getUser());
                followUpAssignment.setWeekAssigned(assignment.getWeekAssigned());
                followUpAssignment.setYearAssigned(assignment.getYearAssigned());
                followUpAssignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
                followUpAssignment.setEmailsSent(assignment.getEmailsSent());
                followUpAssignment.setLastEmailSentAt(assignment.getLastEmailSentAt());

                assignmentRepository.save(followUpAssignment);

                assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.MOVED_TO_FOLLOWUP);
                assignmentRepository.save(assignment);
            }
        } catch (Exception e) {
            log.error("Error moving to follow-up: {}", e.getMessage());
        }
    }
}
