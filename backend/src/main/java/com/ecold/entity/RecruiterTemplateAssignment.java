package com.ecold.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "recruiter_template_assignments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecruiterTemplateAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recruiter_id", nullable = false)
    private RecruiterContact recruiterContact;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "template_id", nullable = false)
    private EmailTemplate emailTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "week_assigned", nullable = false)
    private Integer weekAssigned;

    @Column(name = "year_assigned", nullable = false)
    private Integer yearAssigned;

    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_status", nullable = false)
    private AssignmentStatus assignmentStatus = AssignmentStatus.ACTIVE;

    @Column(name = "emails_sent")
    private Integer emailsSent = 0;

    @Column(name = "last_email_sent_at")
    private LocalDateTime lastEmailSentAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum AssignmentStatus {
        ACTIVE, COMPLETED, MOVED_TO_FOLLOWUP, ARCHIVED
    }
}