package com.ecold.service.impl;

import com.ecold.service.EmailSendService;
import com.ecold.service.UserService;
import com.ecold.service.GmailApiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSendServiceImpl implements EmailSendService {

    private final UserService userService;
    private final GmailApiService gmailApiService;

    @Override
    public void sendEmail(String to, String subject, String body, byte[] resumeAttachment, boolean useScheduledSend, String scheduleTime) {
        try {
            if (useScheduledSend && scheduleTime != null && !scheduleTime.isEmpty()) {
                sendEmailWithGmailSchedule(to, subject, body, resumeAttachment, scheduleTime);
            } else {
                sendImmediateEmail(to, subject, body, resumeAttachment);
            }
            log.info("Email sent successfully to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", to, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }


    @Override
    public void sendEmailWithGmailSchedule(String to, String subject, String body, byte[] resumeAttachment, String scheduleTime) {
        log.info("Scheduling email via Gmail API:");
        log.info("To: {}, Subject: {}, Schedule Time: {}", to, subject, scheduleTime);

        try {
            // Parse the schedule time
            LocalDateTime scheduledDateTime = LocalDateTime.parse(scheduleTime, DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            // Use Gmail API service to schedule the email
            gmailApiService.scheduleEmail(to, subject, body, resumeAttachment, "resume.pdf", scheduleTime);

            log.info("Email scheduled successfully for {}", scheduledDateTime);
        } catch (Exception e) {
            log.error("Failed to schedule email: {}", e.getMessage());
            throw new RuntimeException("Failed to schedule email: " + e.getMessage());
        }
    }

    @Override
    public void sendImmediateEmail(String to, String subject, String body, byte[] resumeAttachment) {
        log.info("Sending immediate email:");
        log.info("To: {}", to);
        log.info("Subject: {}", subject);
        log.info("Body length: {} characters", body.length());
        log.info("Has resume attachment: {}", resumeAttachment != null && resumeAttachment.length > 0);

        try {
            // Use Gmail API service to send the email
            gmailApiService.sendEmail(to, subject, body, resumeAttachment, "resume.pdf");
            log.info("Email sent successfully to {}", to);
        } catch (Exception e) {
            log.error("Failed to send immediate email: {}", e.getMessage());
            throw new RuntimeException("Email sending failed: " + e.getMessage());
        }
    }

    @Override
    public List<String> getScheduledEmails() {
        return gmailApiService.getScheduledEmails();
    }

    @Override
    public void cancelScheduledEmail(String emailId) {
        gmailApiService.cancelScheduledEmail(emailId);
    }

}