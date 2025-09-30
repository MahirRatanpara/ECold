package com.ecold.service;

import java.util.List;

public interface GmailApiService {

    void sendEmail(String to, String subject, String body, byte[] attachment, String filename);

    void scheduleEmail(String to, String subject, String body, byte[] attachment, String filename, String scheduleTime);

    List<String> getScheduledEmails();

    void cancelScheduledEmail(String messageId);

    boolean isGmailConfigured();

    String getGmailAuthUrl();

    void handleOAuthCallback(String code);
}