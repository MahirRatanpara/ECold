package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.RecruiterTemplateAssignment;
import com.ecold.entity.ScheduledEmail;
import com.ecold.entity.User;
import com.ecold.repository.RecruiterTemplateAssignmentRepository;
import com.ecold.repository.ScheduledEmailRepository;
import com.ecold.service.ScheduledEmailService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledEmailServiceImpl implements ScheduledEmailService {

    private final ScheduledEmailRepository scheduledEmailRepository;
    private final GmailOAuthServiceImpl gmailOAuthService;
    private final RecruiterTemplateAssignmentRepository assignmentRepository;

    @Override
    @Transactional
    public ScheduledEmail scheduleEmail(EmailRequest emailRequest, User user, LocalDateTime scheduleTime) {
        log.info("=== SCHEDULING EMAIL IN DATABASE === for user {} to {} at {}", user.getEmail(), emailRequest.getTo(), scheduleTime);

        try {
            // Create scheduled email entity
            ScheduledEmail scheduledEmail = ScheduledEmail.builder()
                    .user(user)
                    .recipientEmail(emailRequest.getTo())
                    .subject(emailRequest.getSubject())
                    .body(emailRequest.getBody())
                    .scheduleTime(scheduleTime)
                    .templateId(emailRequest.getTemplateId())
                    .recruiterId(emailRequest.getRecruiterId())
                    .isHtml(emailRequest.isHtml())
                    .priority(emailRequest.getPriority() != null ? emailRequest.getPriority().toString() : "NORMAL")
                    .status(ScheduledEmail.Status.SCHEDULED)
                    .build();

            log.info("=== SCHEDULED EMAIL ENTITY CREATED === about to save");

            // Save to database
            scheduledEmail = scheduledEmailRepository.save(scheduledEmail);

            log.info("=== EMAIL SAVED TO DATABASE === ID: {}, Status: {}, ScheduleTime: {}",
                scheduledEmail.getId(), scheduledEmail.getStatus(), scheduledEmail.getScheduleTime());

            // Verify it was saved by counting
            long count = scheduledEmailRepository.count();
            log.info("=== TOTAL SCHEDULED EMAILS IN DB: {} ===", count);

            return scheduledEmail;
        } catch (Exception e) {
            log.error("=== ERROR SAVING SCHEDULED EMAIL ===", e);
            throw e;
        }
    }

    @Override
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    @Transactional
    public void processScheduledEmails() {
        LocalDateTime now = LocalDateTime.now();
        log.info("=== SCHEDULER RUNNING === Checking for scheduled emails at {}", now);

        // First, check all scheduled emails in the database
        List<ScheduledEmail> allScheduled = scheduledEmailRepository.findAll();
        log.info("=== TOTAL EMAILS IN DATABASE: {} ===", allScheduled.size());

        for (ScheduledEmail email : allScheduled) {
            log.info("  Email ID: {}, Status: {}, ScheduleTime: {}, Recipient: {}",
                email.getId(), email.getStatus(), email.getScheduleTime(), email.getRecipientEmail());
            if (email.getScheduleTime() != null) {
                log.info("    ScheduleTime <= now? {}", email.getScheduleTime().isBefore(now) || email.getScheduleTime().isEqual(now));
            }
        }

        // Find all emails that are scheduled and due to be sent
        log.info("=== QUERYING FOR DUE EMAILS === Status: SCHEDULED, CurrentTime: {}", now);
        List<ScheduledEmail> dueEmails = scheduledEmailRepository.findDueScheduledEmails(
            ScheduledEmail.Status.SCHEDULED, now);

        log.info("=== FOUND {} SCHEDULED EMAILS DUE ===", dueEmails.size());

        if (!dueEmails.isEmpty()) {
            log.info("Found {} emails ready to be sent", dueEmails.size());
        }

        for (ScheduledEmail scheduledEmail : dueEmails) {
            try {
                log.info("=== PROCESSING SCHEDULED EMAIL {} ===", scheduledEmail.getId());
                sendScheduledEmail(scheduledEmail);
            } catch (Exception e) {
                log.error("Error processing scheduled email {}: {}", scheduledEmail.getId(), e.getMessage(), e);
            }
        }
    }

    private void sendScheduledEmail(ScheduledEmail scheduledEmail) {
        log.info("Sending scheduled email {} to {}", scheduledEmail.getId(), scheduledEmail.getRecipientEmail());

        try {
            // Build email request without scheduleTime (send immediately)
            EmailRequest emailRequest = EmailRequest.builder()
                    .to(scheduledEmail.getRecipientEmail())
                    .subject(scheduledEmail.getSubject())
                    .body(scheduledEmail.getBody())
                    .isHtml(scheduledEmail.isHtml())
                    .templateId(scheduledEmail.getTemplateId())
                    .recruiterId(scheduledEmail.getRecruiterId())
                    .priority(EmailRequest.Priority.valueOf(scheduledEmail.getPriority()))
                    .build();

            // Send the email using Gmail OAuth service
            EmailResponse response = gmailOAuthService.sendEmail(emailRequest, scheduledEmail.getUser());

            if (response.isSuccess()) {
                // Mark as sent
                scheduledEmail.setStatus(ScheduledEmail.Status.SENT);
                scheduledEmail.setSentAt(LocalDateTime.now());
                scheduledEmail.setMessageId(response.getMessageId());
                scheduledEmail.setErrorMessage(null);
                log.info("Successfully sent scheduled email {} to {}",
                    scheduledEmail.getId(), scheduledEmail.getRecipientEmail());

                // Update assignment count if this was a template-based email
                if (scheduledEmail.getTemplateId() != null && scheduledEmail.getRecruiterId() != null) {
                    updateAssignmentEmailCount(scheduledEmail.getTemplateId(), scheduledEmail.getRecruiterId(), scheduledEmail.getUser());
                }
            } else {
                // Mark as failed
                scheduledEmail.setStatus(ScheduledEmail.Status.FAILED);
                scheduledEmail.setErrorMessage(response.getMessage());
                log.error("Failed to send scheduled email {} to {}: {}",
                    scheduledEmail.getId(), scheduledEmail.getRecipientEmail(), response.getMessage());
            }

        } catch (Exception e) {
            log.error("Error sending scheduled email {}: {}", scheduledEmail.getId(), e.getMessage(), e);
            scheduledEmail.setStatus(ScheduledEmail.Status.FAILED);
            scheduledEmail.setErrorMessage(e.getMessage());
        }

        // Save the updated scheduled email
        scheduledEmailRepository.save(scheduledEmail);
    }

    private void updateAssignmentEmailCount(Long templateId, Long recruiterId, User user) {
        try {
            RecruiterTemplateAssignment assignment = assignmentRepository
                    .findByTemplateIdAndRecruiterIdAndUserId(templateId, recruiterId, user.getId())
                    .orElse(null);

            if (assignment != null) {
                assignment.setEmailsSent((assignment.getEmailsSent() != null ? assignment.getEmailsSent() : 0) + 1);
                assignment.setLastEmailSentAt(LocalDateTime.now());
                assignmentRepository.save(assignment);
                log.info("Updated email count for assignment: template={}, recruiter={}", templateId, recruiterId);
            } else {
                log.warn("No assignment found to update for template={}, recruiter={}", templateId, recruiterId);
            }
        } catch (Exception e) {
            log.error("Error updating assignment email count: {}", e.getMessage(), e);
        }
    }
}