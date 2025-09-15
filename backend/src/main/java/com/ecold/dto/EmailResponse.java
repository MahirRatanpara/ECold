package com.ecold.dto;

import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailResponse {
    
    private boolean success;
    private String message;
    private String messageId;
    private LocalDateTime sentAt;
    private String errorCode;
    private String errorDetail;
    private EmailProvider provider;
    
    public enum EmailProvider {
        SMTP, GMAIL_API, SENDGRID, MAILGUN
    }
    
    public static EmailResponse success(String messageId, String message) {
        return EmailResponse.builder()
                .success(true)
                .message(message)
                .messageId(messageId)
                .sentAt(LocalDateTime.now())
                .build();
    }
    
    public static EmailResponse failure(String errorCode, String errorDetail) {
        return EmailResponse.builder()
                .success(false)
                .errorCode(errorCode)
                .errorDetail(errorDetail)
                .sentAt(LocalDateTime.now())
                .build();
    }
}
