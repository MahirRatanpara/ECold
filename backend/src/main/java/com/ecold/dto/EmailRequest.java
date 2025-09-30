package com.ecold.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailRequest {
    
    @Email
    @NotBlank
    private String to;
    
    private List<String> cc;
    private List<String> bcc;
    
    @NotBlank
    private String subject;
    
    @NotBlank
    private String body;
    
    private boolean isHtml;
    
    private Long templateId;
    
    private Long recruiterId;
    
    // Additional data for placeholder replacement
    private Map<String, String> placeholderData;
    
    // Email priority
    private Priority priority;
    
    // Track if this is a follow-up email
    private boolean isFollowUp;

    // Schedule time for delayed sending
    private LocalDateTime scheduleTime;

    public enum Priority {
        LOW, NORMAL, HIGH
    }
}
