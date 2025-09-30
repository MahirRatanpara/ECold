package com.ecold.repository;

import com.ecold.entity.RecruiterTemplateAssignment;
import com.ecold.entity.EmailTemplate;
import com.ecold.entity.RecruiterContact;
import com.ecold.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecruiterTemplateAssignmentRepository extends JpaRepository<RecruiterTemplateAssignment, Long> {

    Page<RecruiterTemplateAssignment> findByUserAndEmailTemplateAndAssignmentStatusOrderByCreatedAtDesc(
            User user, EmailTemplate template, RecruiterTemplateAssignment.AssignmentStatus status, Pageable pageable);

    Page<RecruiterTemplateAssignment> findByUserAndEmailTemplateAndWeekAssignedAndYearAssignedAndAssignmentStatusOrderByCreatedAtDesc(
            User user, EmailTemplate template, Integer weekAssigned, Integer yearAssigned,
            RecruiterTemplateAssignment.AssignmentStatus status, Pageable pageable);

    List<RecruiterTemplateAssignment> findByUserAndEmailTemplateAndWeekAssignedAndYearAssignedAndAssignmentStatus(
            User user, EmailTemplate template, Integer weekAssigned, Integer yearAssigned,
            RecruiterTemplateAssignment.AssignmentStatus status);

    @Query("SELECT DISTINCT rta.weekAssigned FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.user = :user AND rta.emailTemplate = :template " +
           "AND rta.yearAssigned = :year AND rta.assignmentStatus = :status " +
           "ORDER BY rta.weekAssigned ASC")
    List<Integer> findDistinctWeeksByUserAndTemplateAndYearAndStatus(
            @Param("user") User user,
            @Param("template") EmailTemplate template,
            @Param("year") Integer year,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    @Query("SELECT COUNT(rta) FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.user = :user AND rta.emailTemplate = :template " +
           "AND rta.weekAssigned = :week AND rta.yearAssigned = :year " +
           "AND rta.assignmentStatus = :status")
    Long countByUserAndTemplateAndWeekAndYearAndStatus(
            @Param("user") User user,
            @Param("template") EmailTemplate template,
            @Param("week") Integer week,
            @Param("year") Integer year,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    List<RecruiterTemplateAssignment> findByUserAndAssignmentStatus(User user, RecruiterTemplateAssignment.AssignmentStatus status);

    @Query("SELECT rta FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.user = :user AND rta.emailTemplate = :template " +
           "AND rta.assignmentStatus = 'ACTIVE' " +
           "ORDER BY rta.yearAssigned DESC, rta.weekAssigned DESC, rta.createdAt DESC")
    Page<RecruiterTemplateAssignment> findActiveAssignmentsByUserAndTemplate(
            @Param("user") User user,
            @Param("template") EmailTemplate template,
            Pageable pageable);

    @Query("SELECT rta FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.emailTemplate.id = :templateId AND rta.assignmentStatus = :status " +
           "ORDER BY rta.createdAt DESC")
    Page<RecruiterTemplateAssignment> findByTemplateIdAndStatus(
            @Param("templateId") Long templateId,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status,
            Pageable pageable);

    // New methods based on createdAt/updatedAt dates - using date ranges instead of week numbers
    @Query(value = "SELECT DATE_TRUNC('week', rta.created_at) as week_start, " +
           "COUNT(*) as recruiter_count " +
           "FROM recruiter_template_assignments rta " +
           "WHERE rta.template_id = :#{#template.id} " +
           "AND rta.assignment_status = :#{#status.name()} " +
           "GROUP BY week_start " +
           "ORDER BY week_start ASC", nativeQuery = true)
    List<Object[]> findWeeklyGroupsByUserAndTemplateAndStatusBasedOnDate(
            @Param("template") EmailTemplate template,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    @Query(value = "SELECT COUNT(*) FROM recruiter_template_assignments rta " +
           "WHERE rta.template_id = :#{#template.id} " +
           "AND rta.created_at >= :startDate " +
           "AND rta.created_at < :endDate " +
           "AND rta.assignment_status = :#{#status.name()}", nativeQuery = true)
    Long countByUserAndTemplateAndDateRangeAndStatusBasedOnDate(
            @Param("template") EmailTemplate template,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    @Query("SELECT rta FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.user = :user AND rta.emailTemplate = :template " +
           "AND rta.assignmentStatus = :status " +
           "ORDER BY rta.createdAt DESC")
    List<RecruiterTemplateAssignment> findByUserAndTemplateAndStatusBasedOnDate(
            @Param("user") User user,
            @Param("template") EmailTemplate template,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    @Query(value = "SELECT * FROM recruiter_template_assignments rta " +
           "WHERE rta.template_id = :#{#template.id} " +
           "AND rta.created_at >= :startDate " +
           "AND rta.created_at < :endDate " +
           "AND rta.assignment_status = :#{#status.name()} " +
           "ORDER BY rta.created_at DESC", nativeQuery = true)
    List<RecruiterTemplateAssignment> findByUserAndTemplateAndDateRangeAndStatusBasedOnDate(
            @Param("template") EmailTemplate template,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status);

    @Query(value = "SELECT * FROM recruiter_template_assignments rta " +
           "WHERE rta.template_id = :#{#template.id} " +
           "AND rta.created_at >= :startDate " +
           "AND rta.created_at < :endDate " +
           "AND rta.assignment_status = :#{#status.name()} " +
           "ORDER BY rta.created_at DESC", nativeQuery = true)
    Page<RecruiterTemplateAssignment> findByUserAndTemplateAndDateRangeAndStatusBasedOnDateWithPaging(
            @Param("template") EmailTemplate template,
            @Param("startDate") java.time.LocalDateTime startDate,
            @Param("endDate") java.time.LocalDateTime endDate,
            @Param("status") RecruiterTemplateAssignment.AssignmentStatus status,
            Pageable pageable);

    // Method to find assignment by recruiter, template and status
    RecruiterTemplateAssignment findByRecruiterContactAndEmailTemplateAndAssignmentStatus(
            RecruiterContact recruiterContact,
            EmailTemplate emailTemplate,
            RecruiterTemplateAssignment.AssignmentStatus status);

    // Method to find assignment by template ID, recruiter ID and user ID
    @Query("SELECT rta FROM RecruiterTemplateAssignment rta " +
           "WHERE rta.emailTemplate.id = :templateId " +
           "AND rta.recruiterContact.id = :recruiterId " +
           "AND rta.user.id = :userId " +
           "AND rta.assignmentStatus = 'ACTIVE'")
    java.util.Optional<RecruiterTemplateAssignment> findByTemplateIdAndRecruiterIdAndUserId(
            @Param("templateId") Long templateId,
            @Param("recruiterId") Long recruiterId,
            @Param("userId") Long userId);
}