package com.ecold.service;

import com.ecold.entity.IncomingEmail;
import com.ecold.entity.User;
import org.springframework.data.domain.Page;

import java.util.List;

public interface IncomingEmailService {
    void scanIncomingEmails(User user);
    IncomingEmail.EmailCategory categorizeEmail(String subject, String body, String senderEmail);
    Page<IncomingEmail> getIncomingEmails(User user, int page, int size);
    Page<IncomingEmail> getIncomingEmailsByCategory(User user, IncomingEmail.EmailCategory category, int page, int size);
    List<IncomingEmail> getUnreadEmails(User user);
    void markAsRead(Long emailId);
    Long getUnreadCount(User user, IncomingEmail.EmailCategory category);
    void processIncomingEmails();
}