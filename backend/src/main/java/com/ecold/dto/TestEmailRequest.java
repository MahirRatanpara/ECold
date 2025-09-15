package com.ecold.dto;

import lombok.Data;

@Data
public class TestEmailRequest {
    private String recipientEmail;
    private String subject;
    private String body;
}