package com.ecold.service.impl;

import com.ecold.dto.EmailRequest;
import com.ecold.dto.EmailResponse;
import com.ecold.entity.RecruiterTemplateAssignment;
import com.ecold.entity.ScheduledEmail;
import com.ecold.entity.User;
import com.ecold.repository.firestore.RecruiterTemplateAssignmentFirestoreRepository;
import com.ecold.repository.firestore.ScheduledEmailFirestoreRepository;
import com.ecold.repository.firestore.UserFirestoreRepository;
import com.ecold.service.ScheduledEmailService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ScheduledEmailServiceImpl implements ScheduledEmailService {

    private final ScheduledEmailFirestoreRepository scheduledEmailRepository;
    private final GmailOAuthServiceImpl gmailOAuthService;
    private final RecruiterTemplateAssignmentFirestoreRepository assignmentRepository;
    private final UserFirestoreRepository userFirestoreRepository;

    @Value("${scheduling.email.thread-pool-size:5}")
    private int threadPoolSize;

    @Value("${scheduling.email.enabled:true}")
    private boolean schedulingEnabled;

    // ExecutorService for parallel processing of users
    private ExecutorService executorService;

    @Override
    public ScheduledEmail scheduleEmail(EmailRequest emailRequest, User user, LocalDateTime scheduleTime) {
        log.info("=== SCHEDULING EMAIL IN DATABASE === for user {} to {} at {}", user.getEmail(), emailRequest.getTo(), scheduleTime);

        try {
            // Create scheduled email entity
            ScheduledEmail scheduledEmail = ScheduledEmail.builder()
                    .userId(user.getId())
                    .recipientEmail(emailRequest.getTo())
                    .subject(emailRequest.getSubject())
                    .body(emailRequest.getBody())
                    .scheduleTime(convertToTimestamp(scheduleTime))
                    .templateId(emailRequest.getTemplateId())
                    .recruiterId(emailRequest.getRecruiterId())
                    .isHtml(emailRequest.isHtml())
                    .priority(emailRequest.getPriority() != null ? emailRequest.getPriority().toString() : "NORMAL")
                    .status(ScheduledEmail.Status.SCHEDULED.name())
                    .build();

            log.info("=== SCHEDULED EMAIL ENTITY CREATED === about to save");

            // Save to database
            scheduledEmail = scheduledEmailRepository.save(user.getId(), scheduledEmail);

            log.info("=== EMAIL SAVED TO DATABASE === ID: {}, Status: {}, ScheduleTime: {}",
                scheduledEmail.getId(), scheduledEmail.getStatus(), scheduledEmail.getScheduleTime());

            // Verify it was saved by counting
            long count = scheduledEmailRepository.countByUser(user.getId());
            log.info("=== TOTAL SCHEDULED EMAILS IN DB: {} ===", count);

            return scheduledEmail;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("=== ERROR SAVING SCHEDULED EMAIL ===", e);
            throw new RuntimeException("Failed to schedule email", e);
        }
    }

    /**
     * Initialize ExecutorService for parallel processing
     */
    private ExecutorService getExecutorService() {
        if (executorService == null || executorService.isShutdown()) {
            executorService = Executors.newFixedThreadPool(threadPoolSize);
            log.info("ExecutorService initialized with thread pool size: {}", threadPoolSize);
        }
        return executorService;
    }

    /**
     * Scheduled task to process scheduled emails for all users
     * Runs every 30 seconds
     */
    @Override
    @Scheduled(fixedRate = 1800000) // Run every 30 minutes
    public void processScheduledEmails() {
        if (!schedulingEnabled) {
            log.debug("Scheduled email processing is disabled");
            return;
        }

        log.info("=== Starting scheduled email processing ===");
        long startTime = System.currentTimeMillis();

        try {
            // Get all users from Firestore
            List<User> allUsers = userFirestoreRepository.findAll();
            log.info("Found {} users to process for scheduled emails", allUsers.size());

            if (allUsers.isEmpty()) {
                log.info("No users found, skipping scheduled email processing");
                return;
            }

            // Get current timestamp for comparison
            Timestamp now = Timestamp.now();

            // Use ExecutorService to process each user in parallel
            ExecutorService executor = getExecutorService();
            List<CompletableFuture<ProcessingResult>> futures = new ArrayList<>();

            for (User user : allUsers) {
                CompletableFuture<ProcessingResult> future = CompletableFuture.supplyAsync(() ->
                    processUserScheduledEmails(user, now), executor);
                futures.add(future);
            }

            // Wait for all tasks to complete with timeout
            CompletableFuture<Void> allOf = CompletableFuture.allOf(
                futures.toArray(new CompletableFuture[0])
            );

            try {
                allOf.get(5, TimeUnit.MINUTES); // 5 minute timeout
            } catch (TimeoutException e) {
                log.error("Timeout waiting for scheduled email processing to complete", e);
            }

            // Collect results
            int totalProcessed = 0;
            int totalSent = 0;
            int totalFailed = 0;

            for (CompletableFuture<ProcessingResult> future : futures) {
                try {
                    ProcessingResult result = future.get();
                    totalProcessed += result.processed;
                    totalSent += result.sent;
                    totalFailed += result.failed;
                } catch (Exception e) {
                    log.error("Error getting processing result", e);
                }
            }

            long duration = System.currentTimeMillis() - startTime;
            log.info("=== Scheduled email processing completed === " +
                    "Users: {}, Processed: {}, Sent: {}, Failed: {}, Duration: {}ms",
                    allUsers.size(), totalProcessed, totalSent, totalFailed, duration);

        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error processing scheduled emails", e);
        } catch (Exception e) {
            log.error("Unexpected error processing scheduled emails", e);
        }
    }

    /**
     * Process scheduled emails for a specific user
     */
    private ProcessingResult processUserScheduledEmails(User user, Timestamp now) {
        ProcessingResult result = new ProcessingResult();

        try {
            log.debug("Processing scheduled emails for user: {}", user.getEmail());

            // Get all scheduled emails for this user
            List<ScheduledEmail> scheduledEmails = scheduledEmailRepository.findByUser(user.getId());

            // Filter emails that are due (scheduleTime <= now) and status is SCHEDULED
            List<ScheduledEmail> dueEmails = scheduledEmails.stream()
                .filter(email -> "SCHEDULED".equals(email.getStatus()))
                .filter(email -> email.getScheduleTime() != null)
                .filter(email -> email.getScheduleTime().compareTo(now) <= 0)
                .collect(Collectors.toList());

            log.debug("Found {} due emails for user: {}", dueEmails.size(), user.getEmail());

            for (ScheduledEmail scheduledEmail : dueEmails) {
                try {
                    sendScheduledEmail(scheduledEmail, user);
                    result.processed++;

                    // Check if it was successfully sent
                    if ("SENT".equals(scheduledEmail.getStatus())) {
                        result.sent++;
                    } else {
                        result.failed++;
                    }
                } catch (Exception e) {
                    log.error("Error sending scheduled email {} for user {}: {}",
                        scheduledEmail.getId(), user.getEmail(), e.getMessage(), e);
                    result.failed++;
                }
            }

        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error fetching scheduled emails for user {}: {}",
                user.getEmail(), e.getMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error processing user {}: {}",
                user.getEmail(), e.getMessage(), e);
        }

        return result;
    }

    /**
     * Result class for tracking processing statistics
     */
    private static class ProcessingResult {
        int processed = 0;
        int sent = 0;
        int failed = 0;
    }

    /**
     * Cleanup ExecutorService on shutdown
     */
    @PreDestroy
    public void shutdown() {
        if (executorService != null && !executorService.isShutdown()) {
            log.info("Shutting down ExecutorService for scheduled email processing");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                    log.warn("ExecutorService did not terminate gracefully, forced shutdown");
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    private void sendScheduledEmail(ScheduledEmail scheduledEmail, User user) {
        log.info("Sending scheduled email {} to {}", scheduledEmail.getId(), scheduledEmail.getRecipientEmail());

        try {
            // Build email request without scheduleTime (send immediately)
            EmailRequest emailRequest = EmailRequest.builder()
                    .to(scheduledEmail.getRecipientEmail())
                    .subject(scheduledEmail.getSubject())
                    .body(scheduledEmail.getBody())
                    .isHtml(scheduledEmail.getIsHtml() != null ? scheduledEmail.getIsHtml() : false)
                    .templateId(scheduledEmail.getTemplateId())
                    .recruiterId(scheduledEmail.getRecruiterId())
                    .priority(EmailRequest.Priority.valueOf(scheduledEmail.getPriority()))
                    .build();

            // Send the email using Gmail OAuth service
            EmailResponse response = gmailOAuthService.sendEmail(emailRequest, user);

            if (response.isSuccess()) {
                // Mark as sent
                scheduledEmail.setStatusEnum(ScheduledEmail.Status.SENT);
                scheduledEmail.setSentAt(Timestamp.now());
                scheduledEmail.setMessageId(response.getMessageId());
                scheduledEmail.setErrorMessage(null);
                log.info("Successfully sent scheduled email {} to {}",
                    scheduledEmail.getId(), scheduledEmail.getRecipientEmail());

                // Update assignment count if this was a template-based email
                if (scheduledEmail.getTemplateId() != null && scheduledEmail.getRecruiterId() != null) {
                    updateAssignmentEmailCount(scheduledEmail.getTemplateId(), scheduledEmail.getRecruiterId(), user);
                }
            } else {
                // Mark as failed
                scheduledEmail.setStatusEnum(ScheduledEmail.Status.FAILED);
                scheduledEmail.setErrorMessage(response.getMessage());
                log.error("Failed to send scheduled email {} to {}: {}",
                    scheduledEmail.getId(), scheduledEmail.getRecipientEmail(), response.getMessage());
            }

        } catch (Exception e) {
            log.error("Error sending scheduled email {}: {}", scheduledEmail.getId(), e.getMessage(), e);
            scheduledEmail.setStatusEnum(ScheduledEmail.Status.FAILED);
            scheduledEmail.setErrorMessage(e.getMessage());
        }

        try {
            // Save the updated scheduled email
            scheduledEmailRepository.save(user.getId(), scheduledEmail);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Failed to update scheduled email status: {}", e.getMessage(), e);
        }
    }

    private void updateAssignmentEmailCount(String templateId, String recruiterId, User user) {
        try {
            // Find assignments by template and recruiter
            List<RecruiterTemplateAssignment> assignments = assignmentRepository
                    .findByUserAndTemplate(user.getId(), templateId);

            RecruiterTemplateAssignment assignment = assignments.stream()
                    .filter(a -> recruiterId.equals(a.getRecruiterId()))
                    .findFirst()
                    .orElse(null);

            if (assignment != null) {
                assignment.setEmailsSent((assignment.getEmailsSent() != null ? assignment.getEmailsSent() : 0) + 1);
                assignment.setLastEmailSentAt(Timestamp.now());
                assignmentRepository.save(user.getId(), assignment);
                log.info("Updated email count for assignment: template={}, recruiter={}", templateId, recruiterId);
            } else {
                log.warn("No assignment found to update for template={}, recruiter={}", templateId, recruiterId);
            }
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Error updating assignment email count: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to convert LocalDateTime to Firestore Timestamp
     */
    private Timestamp convertToTimestamp(LocalDateTime localDateTime) {
        if (localDateTime == null) {
            return null;
        }
        Instant instant = localDateTime.atZone(ZoneId.systemDefault()).toInstant();
        return Timestamp.ofTimeSecondsAndNanos(instant.getEpochSecond(), instant.getNano());
    }

    /**
     * Helper method to convert Firestore Timestamp to LocalDateTime
     */
    private LocalDateTime convertToLocalDateTime(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return LocalDateTime.ofInstant(
                Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                ZoneId.systemDefault()
        );
    }
}