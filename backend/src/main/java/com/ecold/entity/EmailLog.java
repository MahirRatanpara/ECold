package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {
    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/email_logs/{logId}
    private String userId;

    // Store recruiter ID instead of full object
    private String recruiterContactId;

    private String recipientEmail;
    private String subject;
    private String body;
    private String status; // Stored as String
    private String errorMessage;
    private String messageId;
    private Timestamp sentAt;
    private Timestamp deliveredAt;
    private Timestamp openedAt;
    private Timestamp clickedAt;
    private Integer retryCount = 0;
    private Timestamp createdAt;

    public enum EmailStatus {
        PENDING, SENDING, SENT, DELIVERED, OPENED, CLICKED, BOUNCED, FAILED
    }

    // Helper methods for enum conversion
    public void setStatusEnum(EmailStatus status) {
        this.status = status != null ? status.name() : null;
    }

    public EmailStatus getStatusEnum() {
        return this.status != null ? EmailStatus.valueOf(this.status) : null;
    }
}