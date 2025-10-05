package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterContact {
    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/recruiters/{recruiterId}
    private String userId;

    private String email;
    private String recruiterName;
    private String companyName;
    private String jobRole;
    private String linkedinProfile;
    private String notes;
    private String status; // Stored as String
    private Timestamp lastContactedAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    // Note: Email logs are in separate subcollection: /users/{userId}/email_logs/
    // Note: Assignments are in separate subcollection: /users/{userId}/assignments/

    public enum ContactStatus {
        PENDING, CONTACTED, RESPONDED, REJECTED, INTERVIEWED, HIRED
    }

    // Helper methods for enum conversion
    public void setStatusEnum(ContactStatus status) {
        this.status = status != null ? status.name() : null;
    }

    public ContactStatus getStatusEnum() {
        return this.status != null ? ContactStatus.valueOf(this.status) : null;
    }
}