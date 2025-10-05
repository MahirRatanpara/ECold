package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledEmail {

    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/scheduled_emails/{emailId}
    private String userId;

    private String recipientEmail;
    private String subject;
    private String body;
    private Timestamp scheduleTime;
    private Timestamp createdAt;
    private String status; // Stored as String

    // Optional fields for template-based emails
    private String templateId;
    private String recruiterId;

    // Error tracking
    private String errorMessage;
    private Timestamp sentAt;
    private String messageId;

    // Additional email options
    private Boolean isHtml;
    private String priority;

    public enum Status {
        SCHEDULED,
        SENT,
        FAILED,
        CANCELLED
    }

    // Helper methods for enum conversion
    public void setStatusEnum(Status status) {
        this.status = status != null ? status.name() : null;
    }

    public Status getStatusEnum() {
        return this.status != null ? Status.valueOf(this.status) : null;
    }
}