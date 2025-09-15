package com.ecold.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "incoming_emails")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class IncomingEmail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String messageId;
    
    @Column(nullable = false)
    private String senderEmail;
    
    private String senderName;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Column(columnDefinition = "TEXT")
    private String htmlBody;
    
    @Enumerated(EnumType.STRING)
    private EmailCategory category;
    
    @Enumerated(EnumType.STRING)
    private EmailPriority priority = EmailPriority.NORMAL;
    
    private boolean isRead = false;
    
    private boolean isProcessed = false;
    
    private LocalDateTime receivedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    private String threadId;
    
    @Column(columnDefinition = "TEXT")
    private String keywords;
    
    private Double confidenceScore;
    
    public enum EmailCategory {
        APPLICATION_UPDATE,
        SHORTLIST_INTERVIEW,
        REJECTION_CLOSED,
        RECRUITER_OUTREACH,
        GENERAL_INQUIRY,
        SPAM,
        UNKNOWN
    }
    
    public enum EmailPriority {
        HIGH, NORMAL, LOW
    }
}