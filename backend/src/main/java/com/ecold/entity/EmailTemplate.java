package com.ecold.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "email_templates")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailTemplate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String name;
    
    @Column(nullable = false)
    private String subject;
    
    @Column(columnDefinition = "TEXT", nullable = false)
    private String body;
    
    @Enumerated(EnumType.STRING)
    private Category category = Category.OUTREACH;
    
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;
    
    private Long usageCount = 0L;
    private Long emailsSent = 0L;
    private Double responseRate = 0.0;
    
    private String tags; // JSON string of tags array


    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;
    
    private LocalDateTime lastUsed;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "emailTemplate", cascade = CascadeType.ALL)
    private List<RecruiterTemplateAssignment> recruiterAssignments;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_template_id")
    private EmailTemplate followUpTemplate;

    @OneToMany(mappedBy = "followUpTemplate", cascade = CascadeType.ALL)
    private List<EmailTemplate> parentTemplates;

    public enum Category {
        OUTREACH, FOLLOW_UP, REFERRAL, INTERVIEW, THANK_YOU
    }
    
    public enum Status {
        ACTIVE, DRAFT, ARCHIVED
    }
}