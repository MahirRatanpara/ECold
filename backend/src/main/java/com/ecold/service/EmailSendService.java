package com.ecold.service;

import java.util.List;

public interface EmailSendService {

    void sendEmail(String to, String subject, String body, byte[] resumeAttachment, boolean useScheduledSend, String scheduleTime);

    void sendEmailWithGmailSchedule(String to, String subject, String body, byte[] resumeAttachment, String scheduleTime);

    void sendImmediateEmail(String to, String subject, String body, byte[] resumeAttachment);

    List<String> getScheduledEmails();

    void cancelScheduledEmail(String emailId);
}