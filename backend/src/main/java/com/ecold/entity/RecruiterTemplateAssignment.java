package com.ecold.entity;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterTemplateAssignment {
    @DocumentId
    private String id;

    // userId is implicit in path: /users/{userId}/assignments/{assignmentId}
    private String userId;

    // Store IDs instead of full objects
    private String recruiterId;
    private String templateId;

    private Integer weekAssigned;
    private Integer yearAssigned;
    private String assignmentStatus; // Stored as String
    private Integer emailsSent = 0;
    private Timestamp lastEmailSentAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public enum AssignmentStatus {
        ACTIVE, COMPLETED, MOVED_TO_FOLLOWUP, ARCHIVED
    }

    // Helper methods for enum conversion
    public void setAssignmentStatusEnum(AssignmentStatus status) {
        this.assignmentStatus = status != null ? status.name() : null;
    }

    public AssignmentStatus getAssignmentStatusEnum() {
        return this.assignmentStatus != null ? AssignmentStatus.valueOf(this.assignmentStatus) : null;
    }
}