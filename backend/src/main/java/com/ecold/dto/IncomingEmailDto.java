package com.ecold.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class IncomingEmailDto {
    private Long id;
    private String messageId;
    private String senderEmail;
    private String senderName;
    private String subject;
    private String body;
    private String category;
    private boolean isRead;
    private LocalDateTime receivedAt;
    private LocalDateTime processedAt;
}