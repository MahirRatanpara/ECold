package com.ecold.service.impl;

import com.ecold.dto.RecruiterTemplateAssignmentDto;
import com.ecold.dto.TemplateWeekSummaryDto;
import com.ecold.dto.RecruiterContactDto;
import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.*;
import com.ecold.repository.RecruiterTemplateAssignmentRepository;
import com.ecold.repository.RecruiterContactRepository;
import com.ecold.repository.EmailTemplateRepository;
import com.ecold.service.RecruiterTemplateAssignmentService;
import com.ecold.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.WeekFields;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class RecruiterTemplateAssignmentServiceImpl implements RecruiterTemplateAssignmentService {

    private final RecruiterTemplateAssignmentRepository assignmentRepository;
    private final RecruiterContactRepository recruiterRepository;
    private final EmailTemplateRepository templateRepository;
    private final UserService userService;

    @Override
    public Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplate(Long templateId, Pageable pageable) {
        User currentUser = userService.getCurrentUser();

        // Try to find the template, but don't fail if it doesn't exist
        Optional<EmailTemplate> templateOpt = templateRepository.findById(templateId);

        Page<RecruiterTemplateAssignment> assignments;

        if (templateOpt.isPresent()) {
            // Normal case - template exists, but don't filter by user for now to avoid inconsistencies
            assignments = assignmentRepository.findByTemplateIdAndStatus(
                    templateId, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE, pageable);
        } else {
            // Handle case where template doesn't exist but assignments might reference it
            // Use a direct query by templateId instead of template object
            assignments = assignmentRepository.findByTemplateIdAndStatus(
                    templateId, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE, pageable);
        }

        return assignments.map(this::convertToDto);
    }

    @Override
    public Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplateAndDateRange(Long templateId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        User currentUser = userService.getCurrentUser();
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Convert LocalDate to LocalDateTime for database query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // Include the entire end date

        // Use the native SQL method that filters by date range directly in the database
        Page<RecruiterTemplateAssignment> assignments = assignmentRepository
                .findByUserAndTemplateAndDateRangeAndStatusBasedOnDateWithPaging(
                        template, startDateTime, endDateTime,
                        RecruiterTemplateAssignment.AssignmentStatus.ACTIVE, pageable);

        return assignments.map(this::convertToDto);
    }

    @Override
    public List<TemplateWeekSummaryDto> getDateRangeSummariesForTemplate(Long templateId) {
        User currentUser = userService.getCurrentUser();
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Use the native SQL method that groups by week start dates
        List<Object[]> weeklyGroups = assignmentRepository.findWeeklyGroupsByUserAndTemplateAndStatusBasedOnDate(
                template, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);


        return weeklyGroups.stream()
                .map(row -> {
                    java.sql.Timestamp weekStartTimestamp = (java.sql.Timestamp) row[0];
                    Long count = ((Number) row[1]).longValue();

                    LocalDate weekStart = weekStartTimestamp.toLocalDateTime().toLocalDate();
                    LocalDate weekEnd = weekStart.plusDays(6); // End of week (Sunday to Saturday)

                    TemplateWeekSummaryDto summary = new TemplateWeekSummaryDto();
                    summary.setStartDate(weekStart);
                    summary.setEndDate(weekEnd);
                    summary.setRecruitersCount(count);

                    // Create a nice label like "Sep 16-22, 2024"
                    String label = formatDateRange(weekStart, weekEnd);
                    summary.setDateRangeLabel(label);

                    return summary;
                })
                .collect(Collectors.toList());
    }

    private String formatDateRange(LocalDate start, LocalDate end) {
        java.time.format.DateTimeFormatter monthDay = java.time.format.DateTimeFormatter.ofPattern("MMM d");
        java.time.format.DateTimeFormatter year = java.time.format.DateTimeFormatter.ofPattern("yyyy");

        if (start.getMonth() == end.getMonth()) {
            // Same month: "Sep 16-22, 2024"
            return start.format(monthDay) + "-" + end.getDayOfMonth() + ", " + start.format(year);
        } else {
            // Different months: "Sep 30-Oct 6, 2024"
            return start.format(monthDay) + "-" + end.format(monthDay) + ", " + start.format(year);
        }
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> getRecruitersForDateRange(Long templateId, LocalDate startDate, LocalDate endDate) {
        User currentUser = userService.getCurrentUser();
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Convert LocalDate to LocalDateTime for database query
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.plusDays(1).atStartOfDay(); // Include the entire end date

        // Use the native SQL method that filters by date range directly in the database
        List<RecruiterTemplateAssignment> assignments = assignmentRepository
                .findByUserAndTemplateAndDateRangeAndStatusBasedOnDate(
                        template, startDateTime, endDateTime,
                        RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

        return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Override
    public RecruiterTemplateAssignmentDto assignRecruiterToTemplate(Long recruiterId, Long templateId) {
        User currentUser = userService.getCurrentUser();

        RecruiterContact recruiter = recruiterRepository.findById(recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Calculate current week and year
        LocalDateTime now = LocalDateTime.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
        int currentYear = now.get(weekFields.weekBasedYear());

        RecruiterTemplateAssignment assignment = new RecruiterTemplateAssignment();
        assignment.setRecruiterContact(recruiter);
        assignment.setEmailTemplate(template);
        assignment.setUser(currentUser);
        assignment.setWeekAssigned(currentWeek);
        assignment.setYearAssigned(currentYear);
        assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
        assignment.setEmailsSent(0); // Explicitly set to 0 for new assignments

        assignment = assignmentRepository.save(assignment);
        return convertToDto(assignment);
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> bulkAssignRecruitersToTemplate(List<Long> recruiterIds, Long templateId) {
        User currentUser = userService.getCurrentUser();
        EmailTemplate template = templateRepository.findById(templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

        // Calculate current week and year
        LocalDateTime now = LocalDateTime.now();
        WeekFields weekFields = WeekFields.of(Locale.getDefault());
        int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
        int currentYear = now.get(weekFields.weekBasedYear());

        List<RecruiterContact> recruiters = recruiterRepository.findAllById(recruiterIds);

        List<RecruiterTemplateAssignment> assignments = recruiters.stream().map(recruiter -> {
            RecruiterTemplateAssignment assignment = new RecruiterTemplateAssignment();
            assignment.setRecruiterContact(recruiter);
            assignment.setEmailTemplate(template);
            assignment.setUser(currentUser);
            assignment.setWeekAssigned(currentWeek);
            assignment.setYearAssigned(currentYear);
            assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
            assignment.setEmailsSent(0); // Explicitly set to 0 for new assignments
            return assignment;
        }).collect(Collectors.toList());

        assignments = assignmentRepository.saveAll(assignments);
        return assignments.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public void moveRecruiterToFollowupTemplate(Long assignmentId) {
        RecruiterTemplateAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        EmailTemplate currentTemplate = assignment.getEmailTemplate();
        EmailTemplate followUpTemplate = null;

        // First check if current template has a specific follow-up template
        if (currentTemplate.getFollowUpTemplate() != null) {
            followUpTemplate = currentTemplate.getFollowUpTemplate();
        } else {
            // If no specific follow-up template, find the active FOLLOW_UP category template for this user
            List<EmailTemplate> followUpTemplates = templateRepository.findByUserAndCategoryAndStatus(
                assignment.getUser(),
                EmailTemplate.Category.FOLLOW_UP,
                EmailTemplate.Status.ACTIVE
            );

            if (!followUpTemplates.isEmpty()) {
                // Use the first active follow-up template
                followUpTemplate = followUpTemplates.get(0);
            }
        }

        if (followUpTemplate != null) {
            // Create new assignment with follow-up template
            RecruiterTemplateAssignment followUpAssignment = new RecruiterTemplateAssignment();
            followUpAssignment.setRecruiterContact(assignment.getRecruiterContact());
            followUpAssignment.setEmailTemplate(followUpTemplate);
            followUpAssignment.setUser(assignment.getUser());
            followUpAssignment.setWeekAssigned(assignment.getWeekAssigned());
            followUpAssignment.setYearAssigned(assignment.getYearAssigned());
            followUpAssignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

            // Preserve email count and last email sent date from original assignment
            followUpAssignment.setEmailsSent(assignment.getEmailsSent());
            followUpAssignment.setLastEmailSentAt(assignment.getLastEmailSentAt());

            assignmentRepository.save(followUpAssignment);

            // Mark current assignment as moved to follow-up
            assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.MOVED_TO_FOLLOWUP);
            assignmentRepository.save(assignment);
        } else {
            // No follow-up template available, just mark assignment as completed/inactive
            assignment.setAssignmentStatus(RecruiterTemplateAssignment.AssignmentStatus.MOVED_TO_FOLLOWUP);
            assignmentRepository.save(assignment);
        }
    }


    @Override
    public void markEmailSent(Long assignmentId) {
        RecruiterTemplateAssignment assignment = assignmentRepository.findById(assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

        assignment.setEmailsSent(assignment.getEmailsSent() + 1);
        assignment.setLastEmailSentAt(LocalDateTime.now());
        assignmentRepository.save(assignment);
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> getActiveAssignments() {
        User currentUser = userService.getCurrentUser();
        List<RecruiterTemplateAssignment> assignments = assignmentRepository
                .findByUserAndAssignmentStatus(currentUser, RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);

        return assignments.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    @Override
    public void deleteAssignment(Long assignmentId) {
        assignmentRepository.deleteById(assignmentId);
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> bulkDeleteAssignments(List<Long> assignmentIds) {
        List<RecruiterTemplateAssignment> assignments = assignmentRepository.findAllById(assignmentIds);
        assignmentRepository.deleteAll(assignments);
        return assignments.stream().map(this::convertToDto).collect(Collectors.toList());
    }

    private RecruiterTemplateAssignmentDto convertToDto(RecruiterTemplateAssignment assignment) {
        RecruiterTemplateAssignmentDto dto = new RecruiterTemplateAssignmentDto();
        dto.setId(assignment.getId());
        dto.setRecruiterId(assignment.getRecruiterContact().getId());
        dto.setTemplateId(assignment.getEmailTemplate().getId());
        dto.setTemplateName(assignment.getEmailTemplate().getName());
        dto.setWeekAssigned(assignment.getWeekAssigned());
        dto.setYearAssigned(assignment.getYearAssigned());
        dto.setAssignmentStatus(assignment.getAssignmentStatus());
        dto.setEmailsSent(assignment.getEmailsSent());
        dto.setLastEmailSentAt(assignment.getLastEmailSentAt());
        dto.setCreatedAt(assignment.getCreatedAt());
        dto.setUpdatedAt(assignment.getUpdatedAt());

        // Convert recruiter contact information
        if (assignment.getRecruiterContact() != null) {
            RecruiterContactDto recruiterDto = new RecruiterContactDto();
            recruiterDto.setId(assignment.getRecruiterContact().getId());
            recruiterDto.setEmail(assignment.getRecruiterContact().getEmail());
            recruiterDto.setRecruiterName(assignment.getRecruiterContact().getRecruiterName());
            recruiterDto.setCompanyName(assignment.getRecruiterContact().getCompanyName());
            recruiterDto.setJobRole(assignment.getRecruiterContact().getJobRole());
            recruiterDto.setLinkedinProfile(assignment.getRecruiterContact().getLinkedinProfile());
            recruiterDto.setStatus(assignment.getRecruiterContact().getStatus());
            recruiterDto.setLastContactedAt(assignment.getRecruiterContact().getLastContactedAt());
            recruiterDto.setCreatedAt(assignment.getRecruiterContact().getCreatedAt());
            recruiterDto.setUpdatedAt(assignment.getRecruiterContact().getUpdatedAt());
            dto.setRecruiterContact(recruiterDto);
        }

        // Convert email template information
        if (assignment.getEmailTemplate() != null) {
            EmailTemplateDto templateDto = new EmailTemplateDto();
            templateDto.setId(assignment.getEmailTemplate().getId());
            templateDto.setName(assignment.getEmailTemplate().getName());
            templateDto.setSubject(assignment.getEmailTemplate().getSubject());
            templateDto.setBody(assignment.getEmailTemplate().getBody());
            templateDto.setCategory(assignment.getEmailTemplate().getCategory());
            templateDto.setStatus(assignment.getEmailTemplate().getStatus());
            templateDto.setUsageCount(assignment.getEmailTemplate().getUsageCount());
            templateDto.setEmailsSent(assignment.getEmailTemplate().getEmailsSent());
            templateDto.setResponseRate(assignment.getEmailTemplate().getResponseRate());
            templateDto.setIsDefault(assignment.getEmailTemplate().getIsDefault());
            templateDto.setLastUsed(assignment.getEmailTemplate().getLastUsed());
            templateDto.setCreatedAt(assignment.getEmailTemplate().getCreatedAt());
            templateDto.setUpdatedAt(assignment.getEmailTemplate().getUpdatedAt());
            dto.setEmailTemplate(templateDto);
        }

        return dto;
    }
}