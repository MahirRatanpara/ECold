package com.ecold.dto;

import com.ecold.entity.EmailTemplate;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class EmailTemplateDto {
    private Long id;
    
    @NotBlank(message = "Template name is required")
    private String name;
    
    @NotBlank(message = "Subject is required")
    @ValidPlaceholders
    private String subject;
    
    @NotBlank(message = "Body is required")
    @ValidPlaceholders
    private String body;
    
    @NotNull(message = "Category is required")
    private EmailTemplate.Category category;
    
    @NotNull(message = "Status is required")
    private EmailTemplate.Status status;
    
    private Long usageCount = 0L;
    private Long emailsSent = 0L;
    private Double responseRate = 0.0;
    
    private List<String> tags;


    private Boolean isDefault = false;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime lastUsed;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}