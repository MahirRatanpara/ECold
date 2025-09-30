package com.ecold.service;

import com.ecold.dto.EmailRequest;
import com.ecold.entity.ScheduledEmail;
import com.ecold.entity.User;

import java.time.LocalDateTime;

public interface ScheduledEmailService {

    /**
     * Schedule an email to be sent at a specific time
     */
    ScheduledEmail scheduleEmail(EmailRequest emailRequest, User user, LocalDateTime scheduleTime);

    /**
     * Process and send all due scheduled emails
     * This method is called periodically by the scheduler
     */
    void processScheduledEmails();
}