package com.ecold.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BulkEmailSendRequest {
    @NotNull
    private Long templateId;

    @NotNull
    private Integer weekAssigned;

    @NotNull
    private Integer yearAssigned;

    @NotNull
    private String subject;

    @NotNull
    private String body;

    private boolean useScheduledSend = false;

    private LocalDateTime scheduleTime;

    private boolean attachResume = true;

    private List<Long> recipientIds; // Optional: specific recruiters to send to
}