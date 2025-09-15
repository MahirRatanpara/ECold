package com.ecold.dto;

import com.ecold.entity.RecruiterContact;
import lombok.Data;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;

@Data
public class RecruiterContactDto {
    private Long id;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;
    
    private String recruiterName;
    
    @NotBlank(message = "Company name is required")
    private String companyName;
    
    @NotBlank(message = "Job role is required")
    private String jobRole;
    
    private String linkedinProfile;
    private String notes;
    private RecruiterContact.ContactStatus status;
    private LocalDateTime lastContactedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}