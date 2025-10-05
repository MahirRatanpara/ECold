package com.ecold.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecold.entity.RecruiterTemplateAssignment;
import com.google.cloud.Timestamp;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterTemplateAssignmentDto {
    private String id;
    private String recruiterId;
    private String templateId;
    private String templateName;
    private Integer weekAssigned;
    private Integer yearAssigned;
    private RecruiterTemplateAssignment.AssignmentStatus assignmentStatus;
    private Integer emailsSent;
    private Timestamp lastEmailSentAt;
    private Timestamp createdAt;
    private Timestamp updatedAt;
    private RecruiterContactDto recruiterContact;
    private EmailTemplateDto emailTemplate;
}
