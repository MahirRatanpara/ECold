package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    @DocumentId
    private String id;

    // userId is implicit in Firestore path: /users/{userId}/templates/{templateId}
    // But we can store it for convenience in queries
    private String userId;

    private String name;
    private String subject;
    private String body;
    private String category; // Stored as String
    private String status; // Stored as String
    private Long usageCount = 0L;
    private Long emailsSent = 0L;
    private Double responseRate = 0.0;
    private String tags; // JSON string of tags array
    private Boolean isDefault = false;
    private Timestamp lastUsed;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Store followUpTemplateId instead of the full object
    private String followUpTemplateId;

    // Note: Assignments are handled in a separate subcollection
    // /users/{userId}/assignments/

    public enum Category {
        OUTREACH, FOLLOW_UP, REFERRAL, INTERVIEW, THANK_YOU
    }

    public enum Status {
        ACTIVE, DRAFT, ARCHIVED
    }

    // Helper methods for enum conversion
    public void setCategoryEnum(Category category) {
        this.category = category != null ? category.name() : null;
    }

    public Category getCategoryEnum() {
        return this.category != null ? Category.valueOf(this.category) : null;
    }

    public void setStatusEnum(Status status) {
        this.status = status != null ? status.name() : null;
    }

    public Status getStatusEnum() {
        return this.status != null ? Status.valueOf(this.status) : null;
    }
}