package com.ecold.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.ecold.entity.RecruiterTemplateAssignment;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterTemplateAssignmentDto {
    private Long id;
    private Long recruiterId;
    private Long templateId;
    private String templateName;
    private Integer weekAssigned;
    private Integer yearAssigned;
    private RecruiterTemplateAssignment.AssignmentStatus assignmentStatus;
    private Integer emailsSent;
    private LocalDateTime lastEmailSentAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private RecruiterContactDto recruiterContact;
    private EmailTemplateDto emailTemplate;
}