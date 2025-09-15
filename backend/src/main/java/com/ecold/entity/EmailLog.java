package com.ecold.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    // Campaign removed - direct email logs only
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_contact_id")
    private RecruiterContact recruiterContact;
    
    @Column(nullable = false)
    private String recipientEmail;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT")
    private String body;
    
    @Enumerated(EnumType.STRING)
    private EmailStatus status;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    private String messageId;
    
    private LocalDateTime sentAt;
    
    private LocalDateTime deliveredAt;
    
    private LocalDateTime openedAt;
    
    private LocalDateTime clickedAt;
    
    private Integer retryCount = 0;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    public enum EmailStatus {
        PENDING, SENDING, SENT, DELIVERED, OPENED, CLICKED, BOUNCED, FAILED
    }
}