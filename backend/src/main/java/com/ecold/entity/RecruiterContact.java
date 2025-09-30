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
@Table(name = "recruiter_contacts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterContact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Column(nullable = false)
    private String email;
    
    private String recruiterName;
    
    @Column(nullable = false)
    private String companyName;
    
    @Column(nullable = false)
    private String jobRole;
    
    private String linkedinProfile;
    
    private String notes;
    
    @Enumerated(EnumType.STRING)
    private ContactStatus status = ContactStatus.PENDING;
    
    private LocalDateTime lastContactedAt;
    
    @CreationTimestamp
    private LocalDateTime createdAt;
    
    @UpdateTimestamp
    private LocalDateTime updatedAt;
    
    @OneToMany(mappedBy = "recruiterContact", cascade = CascadeType.ALL)
    private List<EmailLog> emailLogs;

    @OneToMany(mappedBy = "recruiterContact", cascade = CascadeType.ALL)
    private List<RecruiterTemplateAssignment> templateAssignments;
    
    public enum ContactStatus {
        PENDING, CONTACTED, RESPONDED, REJECTED, INTERVIEWED, HIRED
    }
}