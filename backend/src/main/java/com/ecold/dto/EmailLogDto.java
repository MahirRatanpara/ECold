package com.ecold.dto;

import com.ecold.entity.EmailLog;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmailLogDto {
    private String id;
    private Long campaignId;
    private String campaignName;
    private Long recruiterContactId;
    private String recipientEmail;
    private String recruiterName;
    private String companyName;
    private String jobRole;
    private String subject;
    private String body;
    private EmailLog.EmailStatus status;
    private String errorMessage;
    private String messageId;
    private LocalDateTime sentAt;
    private LocalDateTime deliveredAt;
    private LocalDateTime openedAt;
    private LocalDateTime clickedAt;
    private Integer retryCount;
    private LocalDateTime createdAt;
}
