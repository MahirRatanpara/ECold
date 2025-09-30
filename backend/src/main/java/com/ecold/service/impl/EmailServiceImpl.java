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
        log.info("Starting email send process for user: {} to: {}", user.getEmail(), emailRequest.getTo());

        // Check if this is a scheduled email
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime scheduleTime = emailRequest.getScheduleTime();

        log.info("=== EMAIL SEND DEBUG ===");
        log.info("  scheduleTime received: {}", scheduleTime);
        log.info("  current time (server): {}", now);
        log.info("  server timezone: {}", java.time.ZoneId.systemDefault());

        if (scheduleTime != null) {
            log.info("  scheduleTime isAfter now: {}", scheduleTime.isAfter(now));
            log.info("  difference in seconds: {}", java.time.Duration.between(now, scheduleTime).getSeconds());
        }

        if (scheduleTime != null && scheduleTime.isAfter(now)) {
            log.info("=== SCHEDULING EMAIL === for user {} to {} at {}", user.getEmail(), emailRequest.getTo(), emailRequest.getScheduleTime());

            // Only Gmail OAuth users can schedule emails
            log.info("=== CHECKING USER AUTH === Provider: {}, hasValidGmailTokens: {}",
                user.getProvider(), hasValidGmailTokens(user));

            if (user.getProvider() != User.Provider.GOOGLE) {
                log.error("User provider is not GOOGLE: {}", user.getProvider());
                return EmailResponse.failure("SCHEDULE_NOT_SUPPORTED", "Scheduled sending requires Gmail authentication. Please sign in with Google.");
            }

            if (!hasValidGmailTokens(user)) {
                log.error("User does not have valid Gmail tokens. AccessToken: {}, TokenExpiresAt: {}",
                    user.getAccessToken() != null ? "present" : "null",
                    user.getTokenExpiresAt());
                return EmailResponse.failure("SCHEDULE_NOT_SUPPORTED", "Scheduled sending requires Gmail authentication. Please sign in with Google.");
            }

            // Schedule the email
            log.info("=== CALLING scheduledEmailService.scheduleEmail ===");
            scheduledEmailService.scheduleEmail(emailRequest, user, emailRequest.getScheduleTime());
            log.info("=== EMAIL SCHEDULED SUCCESSFULLY ===");
            return EmailResponse.success(null, "Email scheduled successfully for " + emailRequest.getScheduleTime());
        }

        log.info("=== NOT SCHEDULING - SENDING IMMEDIATELY ===");

        // If user is authenticated with Google and has valid tokens, use Gmail OAuth
        if (user.getProvider() == User.Provider.GOOGLE && hasValidGmailTokens(user)) {
            log.info("Using Gmail OAuth for user: {}", user.getEmail());

            // Pass the complete emailRequest to Gmail service (without scheduleTime for immediate send)
            EmailResponse response = gmailOAuthService.sendEmail(emailRequest, user);

            // Update assignment count if email was sent successfully and we have template/recruiter info
            if (response.isSuccess() && emailRequest.getTemplateId() != null && emailRequest.getRecruiterId() != null) {
                updateAssignmentEmailCount(emailRequest.getTemplateId(), emailRequest.getRecruiterId(), user);
            }

            return response;
        }

        // Fall back to SMTP configuration
        log.info("Using SMTP configuration for user: {}", user.getEmail());
        log.info("Email enabled: {}, From email: {}", emailEnabled, fromEmail);

        if (!emailEnabled) {
            log.warn("Email sending is disabled in configuration");
            return EmailResponse.failure("EMAIL_DISABLED", "Email sending is disabled in application configuration");
        }

        boolean settingsValid = validateEmailSettings();
        log.info("Email settings validation result: {}", settingsValid);
        if (!settingsValid) {
            log.error("SMTP email configuration not available. User must authenticate with Google OAuth to send emails.");
            return EmailResponse.failure("SMTP_NOT_CONFIGURED", "SMTP email is not configured. Please sign in with Google to send emails from your Gmail account.");
        }

        try {
            String messageId = generateMessageId();

            if (emailRequest.isHtml()) {
                sendHtmlEmail(emailRequest, user, messageId);
            } else {
                sendTextEmail(emailRequest, user, messageId);
            }

            log.info("Email sent successfully to {} with messageId: {}", emailRequest.getTo(), messageId);

            // Update assignment count if we have template/recruiter info
            if (emailRequest.getTemplateId() != null && emailRequest.getRecruiterId() != null) {
                updateAssignmentEmailCount(emailRequest.getTemplateId(), emailRequest.getRecruiterId(), user);
            }

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
        log.info("=== sendTemplateEmail CALLED === Template: {}, Recruiter: {}, ScheduleTime: {}", templateId, recruiterId, scheduleTime);
        try {
            // Get template
            EmailTemplate template = templateRepository.findById(templateId)
                    .orElseThrow(() -> new RuntimeException("Template not found: " + templateId));
            log.info("=== TEMPLATE FOUND === ID: {}, Name: {}, Category: {}", template.getId(), template.getName(), template.getCategory());

            // Get recruiter
            RecruiterContact recruiter = recruiterRepository.findById(recruiterId)
                    .orElseThrow(() -> new RuntimeException("Recruiter not found: " + recruiterId));
            log.info("=== RECRUITER FOUND === ID: {}, Email: {}", recruiter.getId(), recruiter.getEmail());

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

            // Set the schedule time if provided
            if (scheduleTime != null && scheduleTime.isAfter(LocalDateTime.now())) {
                emailRequest.setScheduleTime(scheduleTime);
                log.info("Scheduling template email {} to recruiter {} at {}", templateId, recruiterId, scheduleTime);
            } else {
                log.info("Sending template email {} to recruiter {} immediately", templateId, recruiterId);
            }

            // Send the email (this will handle both immediate and scheduled sending)
            EmailResponse response = sendEmail(emailRequest, user);

            // If email was sent successfully (not scheduled), move to follow-up if applicable
            log.info("=== EMAIL RESPONSE === Success: {}, Scheduled: {}", response.isSuccess(), scheduleTime != null && scheduleTime.isAfter(LocalDateTime.now()));
            if (response.isSuccess() && (scheduleTime == null || !scheduleTime.isAfter(LocalDateTime.now()))) {
                log.info("=== CALLING moveToFollowUpIfApplicable === Template: {}, Recruiter: {}", templateId, recruiterId);
                moveToFollowUpIfApplicable(templateId, recruiterId, user, template);
            } else {
                log.info("=== NOT MOVING TO FOLLOW-UP === Response success: {}, Schedule time: {}", response.isSuccess(), scheduleTime);
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
            log.info("Attempting to update assignment email count for template {} and recruiter {} for user {}",
                templateId, recruiterId, user.getEmail());

            EmailTemplate template = templateRepository.findById(templateId).orElse(null);
            RecruiterContact recruiter = recruiterRepository.findById(recruiterId).orElse(null);

            if (template == null) {
                log.warn("Template {} not found", templateId);
                return;
            }
            if (recruiter == null) {
                log.warn("Recruiter {} not found", recruiterId);
                return;
            }

            log.info("Found template: {} and recruiter: {}", template.getName(), recruiter.getEmail());

            RecruiterTemplateAssignment assignment = assignmentRepository
                .findByRecruiterContactAndEmailTemplateAndAssignmentStatus(
                    recruiter, template, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

            if (assignment != null) {
                int oldCount = assignment.getEmailsSent();
                assignment.setEmailsSent(assignment.getEmailsSent() + 1);
                assignment.setLastEmailSentAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
                log.info("Successfully updated assignment {} email count from {} to {}",
                    assignment.getId(), oldCount, assignment.getEmailsSent());
            } else {
                log.warn("No active assignment found for recruiter {} (email: {}) and template {} (name: {})",
                    recruiterId, recruiter.getEmail(), templateId, template.getName());
            }
        } catch (Exception e) {
            log.error("Failed to update assignment email count for template {} and recruiter {}: {}",
                templateId, recruiterId, e.getMessage(), e);
        }
    }

    private void moveToFollowUpIfApplicable(Long templateId, Long recruiterId, User user, EmailTemplate currentTemplate) {
        try {
            log.info("=== MOVE TO FOLLOW-UP START === Template: {}, Recruiter: {}, User: {}", templateId, recruiterId, user.getId());

            // Find the assignment
            RecruiterTemplateAssignment assignment = assignmentRepository
                    .findByTemplateIdAndRecruiterIdAndUserId(templateId, recruiterId, user.getId())
                    .orElse(null);

            if (assignment == null) {
                log.warn("=== NO ASSIGNMENT FOUND === for template={}, recruiter={}, user={}", templateId, recruiterId, user.getId());
                return;
            }

            log.info("=== ASSIGNMENT FOUND === ID: {}, Status: {}", assignment.getId(), assignment.getAssignmentStatus());

            // Check if we should move to follow-up
            EmailTemplate followUpTemplate = null;

            // First check if current template has a specific follow-up template
            if (currentTemplate.getFollowUpTemplate() != null) {
                followUpTemplate = currentTemplate.getFollowUpTemplate();
                log.info("=== USING LINKED FOLLOW-UP TEMPLATE === ID: {}", followUpTemplate.getId());
            } else {
                log.info("=== NO LINKED FOLLOW-UP, SEARCHING FOR GENERIC FOLLOW_UP ===");
                // If no specific follow-up template, find the active FOLLOW_UP category template for this user
                java.util.List<EmailTemplate> followUpTemplates = templateRepository.findByUserAndCategoryAndStatus(
                        user,
                        EmailTemplate.Category.FOLLOW_UP,
                        EmailTemplate.Status.ACTIVE
                );

                log.info("=== FOUND {} FOLLOW_UP TEMPLATES ===", followUpTemplates.size());

                if (!followUpTemplates.isEmpty()) {
                    // Use the first active follow-up template
                    followUpTemplate = followUpTemplates.get(0);
                    log.info("=== USING GENERIC FOLLOW-UP TEMPLATE === ID: {}, Name: {}", followUpTemplate.getId(), followUpTemplate.getName());
                }
            }

            if (followUpTemplate != null) {
                log.info("=== CREATING FOLLOW-UP ASSIGNMENT ===");
                // Create new assignment with follow-up template
                RecruiterTemplateAssignment followUpAssignment = new RecruiterTemplateAssignment();
                followUpAssignment.setRecruiterContact(assignment.getRecruiterContact());
                followUpAssignment.setEmailTemplate(followUpTemplate);
                followUpAssignment.setUser(assignment.getUser());
                followUpAssignment.setWeekAssigned(assignment.getWeekAssigned());
                followUpAssignment.setYearAssigned(assignment.getYearAssigned());
                followUpAssignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

                // Copy email stats from current assignment
                followUpAssignment.setEmailsSent(assignment.getEmailsSent());
                followUpAssignment.setLastEmailSentAt(assignment.getLastEmailSentAt());

                followUpAssignment = assignmentRepository.save(followUpAssignment);
                log.info("=== FOLLOW-UP ASSIGNMENT CREATED === ID: {}", followUpAssignment.getId());

                // Mark current assignment as moved to follow-up
                assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.MOVED_TO_FOLLOWUP);
                assignmentRepository.save(assignment);
                log.info("=== OLD ASSIGNMENT MARKED AS MOVED_TO_FOLLOWUP === ID: {}", assignment.getId());

                log.info("=== SUCCESS: Moved recruiter {} from template {} to follow-up template {} ===",
                        recruiterId, templateId, followUpTemplate.getId());
            } else {
                log.warn("=== NO FOLLOW-UP TEMPLATE AVAILABLE === for template {}, not moving recruiter {}", templateId, recruiterId);
            }
        } catch (Exception e) {
            log.error("Error moving to follow-up for template={}, recruiter={}: {}", templateId, recruiterId, e.getMessage(), e);
        }
    }
}
