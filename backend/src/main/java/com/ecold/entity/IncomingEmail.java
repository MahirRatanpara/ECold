package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomingEmail {
    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/incoming_emails/{emailId}
    private String userId;

    private String messageId;
    private String senderEmail;
    private String senderName;
    private String subject;
    private String body;
    private String htmlBody;
    private String category; // Stored as String
    private String priority; // Stored as String
    private Boolean isRead = false;
    private Boolean isProcessed = false;
    private Timestamp receivedAt;
    private Timestamp createdAt;
    private String threadId;
    private String keywords;
    private Double confidenceScore;

    public enum EmailCategory {
        APPLICATION_UPDATE,
        SHORTLIST_INTERVIEW,
        REJECTION_CLOSED,
        RECRUITER_OUTREACH,
        GENERAL_INQUIRY,
        SPAM,
        UNKNOWN
    }

    public enum EmailPriority {
        HIGH, NORMAL, LOW
    }

    // Helper methods for enum conversion
    public void setCategoryEnum(EmailCategory category) {
        this.category = category != null ? category.name() : null;
    }

    public EmailCategory getCategoryEnum() {
        return this.category != null ? EmailCategory.valueOf(this.category) : null;
    }

    public void setPriorityEnum(EmailPriority priority) {
        this.priority = priority != null ? priority.name() : null;
    }

    public EmailPriority getPriorityEnum() {
        return this.priority != null ? EmailPriority.valueOf(this.priority) : null;
    }
}