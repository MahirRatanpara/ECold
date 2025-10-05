package com.ecold.service.impl;

import com.ecold.dto.RecruiterTemplateAssignmentDto;
import com.ecold.dto.TemplateWeekSummaryDto;
import com.ecold.dto.RecruiterContactDto;
import com.ecold.dto.EmailTemplateDto;
import com.ecold.entity.*;
import com.ecold.repository.firestore.RecruiterTemplateAssignmentFirestoreRepository;
import com.ecold.repository.firestore.RecruiterContactFirestoreRepository;
import com.ecold.repository.firestore.EmailTemplateFirestoreRepository;
import com.ecold.service.RecruiterTemplateAssignmentService;
import com.ecold.service.UserService;
import com.google.cloud.Timestamp;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.WeekFields;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterTemplateAssignmentServiceImpl implements RecruiterTemplateAssignmentService {

    private final RecruiterTemplateAssignmentFirestoreRepository assignmentFirestoreRepository;
    private final RecruiterContactFirestoreRepository recruiterFirestoreRepository;
    private final EmailTemplateFirestoreRepository templateFirestoreRepository;
    private final UserService userService;

    @Override
    public Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplate(String templateId, Pageable pageable) {
        try {
            User currentUser = userService.getCurrentUser();

            // Get all assignments for this template
            List<RecruiterTemplateAssignment> allAssignments = assignmentFirestoreRepository
                .findByUserAndTemplateAndStatus(currentUser.getId(), templateId,
                    RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());

            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), allAssignments.size());
            List<RecruiterTemplateAssignment> pageContent = allAssignments.subList(start, end);

            List<RecruiterTemplateAssignmentDto> dtos = pageContent.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return new PageImpl<>(dtos, pageable, allAssignments.size());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiters for template", e);
        }
    }

    @Override
    public Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplateAndDateRange(
            String templateId, LocalDate startDate, LocalDate endDate, Pageable pageable) {
        try {
            User currentUser = userService.getCurrentUser();

            // Get all assignments for this template with ACTIVE status
            List<RecruiterTemplateAssignment> allAssignments = assignmentFirestoreRepository
                .findByUserAndTemplateAndStatus(currentUser.getId(), templateId,
                    RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());

            // Filter by date range in memory
            List<RecruiterTemplateAssignment> filteredAssignments = allAssignments.stream()
                .filter(assignment -> {
                    if (assignment.getCreatedAt() == null) return false;
                    LocalDate assignmentDate = assignment.getCreatedAt().toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                    return !assignmentDate.isBefore(startDate) && !assignmentDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

            // Manual pagination
            int start = (int) pageable.getOffset();
            int end = Math.min(start + pageable.getPageSize(), filteredAssignments.size());
            List<RecruiterTemplateAssignment> pageContent = filteredAssignments.subList(start, end);

            List<RecruiterTemplateAssignmentDto> dtos = pageContent.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());

            return new PageImpl<>(dtos, pageable, filteredAssignments.size());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiters for template and date range", e);
        }
    }

    @Override
    public List<TemplateWeekSummaryDto> getDateRangeSummariesForTemplate(String templateId) {
        try {
            User currentUser = userService.getCurrentUser();

            // Get all ACTIVE assignments for this template
            List<RecruiterTemplateAssignment> assignments = assignmentFirestoreRepository
                .findByUserAndTemplateAndStatus(currentUser.getId(), templateId,
                    RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());

            // Group by week start date
            Map<LocalDate, Long> weeklyGroups = new HashMap<>();
            WeekFields weekFields = WeekFields.of(Locale.getDefault());

            for (RecruiterTemplateAssignment assignment : assignments) {
                if (assignment.getCreatedAt() != null) {
                    LocalDate assignmentDate = assignment.getCreatedAt().toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();

                    // Get the start of the week for this date
                    LocalDate weekStart = assignmentDate.with(weekFields.dayOfWeek(), 1);

                    weeklyGroups.merge(weekStart, 1L, Long::sum);
                }
            }

            // Convert to DTOs
            return weeklyGroups.entrySet().stream()
                .map(entry -> {
                    LocalDate weekStart = entry.getKey();
                    Long count = entry.getValue();
                    LocalDate weekEnd = weekStart.plusDays(6);

                    TemplateWeekSummaryDto summary = new TemplateWeekSummaryDto();
                    summary.setStartDate(weekStart);
                    summary.setEndDate(weekEnd);
                    summary.setRecruitersCount(count);
                    summary.setDateRangeLabel(formatDateRange(weekStart, weekEnd));

                    return summary;
                })
                .sorted(Comparator.comparing(TemplateWeekSummaryDto::getStartDate))
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching date range summaries", e);
        }
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
    public List<RecruiterTemplateAssignmentDto> getRecruitersForDateRange(
            String templateId, LocalDate startDate, LocalDate endDate) {
        try {
            User currentUser = userService.getCurrentUser();

            // Get all assignments for this template with ACTIVE status
            List<RecruiterTemplateAssignment> allAssignments = assignmentFirestoreRepository
                .findByUserAndTemplateAndStatus(currentUser.getId(), templateId,
                    RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());

            // Filter by date range
            List<RecruiterTemplateAssignment> filteredAssignments = allAssignments.stream()
                .filter(assignment -> {
                    if (assignment.getCreatedAt() == null) return false;
                    LocalDate assignmentDate = assignment.getCreatedAt().toDate().toInstant()
                        .atZone(ZoneId.systemDefault()).toLocalDate();
                    return !assignmentDate.isBefore(startDate) && !assignmentDate.isAfter(endDate);
                })
                .collect(Collectors.toList());

            return filteredAssignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching recruiters for date range", e);
        }
    }

    @Override
    public RecruiterTemplateAssignmentDto assignRecruiterToTemplate(String recruiterId, String templateId) {
        try {
            User currentUser = userService.getCurrentUser();

            // Verify recruiter and template exist
            RecruiterContact recruiter = recruiterFirestoreRepository.findById(currentUser.getId(), recruiterId)
                .orElseThrow(() -> new RuntimeException("Recruiter not found"));
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

            // Calculate current week and year
            LocalDate now = LocalDate.now();
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
            int currentYear = now.get(weekFields.weekBasedYear());

            RecruiterTemplateAssignment assignment = new RecruiterTemplateAssignment();
            assignment.setRecruiterId(recruiterId);
            assignment.setTemplateId(templateId);
            assignment.setUserId(currentUser.getId());
            assignment.setWeekAssigned(currentWeek);
            assignment.setYearAssigned(currentYear);
            assignment.setAssignmentStatusEnum(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
            assignment.setEmailsSent(0);

            assignment = assignmentFirestoreRepository.save(currentUser.getId(), assignment);
            return convertToDto(assignment);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error assigning recruiter to template", e);
        }
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> bulkAssignRecruitersToTemplate(
            List<String> recruiterIds, String templateId) {
        try {
            User currentUser = userService.getCurrentUser();

            // Verify template exists
            EmailTemplate template = templateFirestoreRepository.findById(currentUser.getId(), templateId)
                .orElseThrow(() -> new RuntimeException("Template not found"));

            // Calculate current week and year
            LocalDate now = LocalDate.now();
            WeekFields weekFields = WeekFields.of(Locale.getDefault());
            int currentWeek = now.get(weekFields.weekOfWeekBasedYear());
            int currentYear = now.get(weekFields.weekBasedYear());

            List<RecruiterTemplateAssignment> assignments = new ArrayList<>();

            for (String recruiterId : recruiterIds) {
                // Verify each recruiter exists
                Optional<RecruiterContact> recruiterOpt = recruiterFirestoreRepository
                    .findById(currentUser.getId(), recruiterId);

                if (recruiterOpt.isPresent()) {
                    RecruiterTemplateAssignment assignment = new RecruiterTemplateAssignment();
                    assignment.setRecruiterId(recruiterId);
                    assignment.setTemplateId(templateId);
                    assignment.setUserId(currentUser.getId());
                    assignment.setWeekAssigned(currentWeek);
                    assignment.setYearAssigned(currentYear);
                    assignment.setAssignmentStatusEnum(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
                    assignment.setEmailsSent(0);

                    assignment = assignmentFirestoreRepository.save(currentUser.getId(), assignment);
                    assignments.add(assignment);
                }
            }

            return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error bulk assigning recruiters to template", e);
        }
    }

    @Override
    public void moveRecruiterToFollowupTemplate(String assignmentId) {
        try {
            User currentUser = userService.getCurrentUser();

            RecruiterTemplateAssignment assignment = assignmentFirestoreRepository
                .findById(currentUser.getId(), assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

            // Get current template
            EmailTemplate currentTemplate = templateFirestoreRepository
                .findById(currentUser.getId(), assignment.getTemplateId())
                .orElse(null);

            EmailTemplate followUpTemplate = null;

            // First check if current template has a specific follow-up template
            if (currentTemplate != null && currentTemplate.getFollowUpTemplateId() != null) {
                followUpTemplate = templateFirestoreRepository
                    .findById(currentUser.getId(), currentTemplate.getFollowUpTemplateId())
                    .orElse(null);
            } else {
                // If no specific follow-up template, find the active FOLLOW_UP category template for this user
                List<EmailTemplate> followUpTemplates = templateFirestoreRepository
                    .findByUserAndCategoryAndStatus(currentUser.getId(),
                        EmailTemplate.Category.FOLLOW_UP.name(),
                        EmailTemplate.Status.ACTIVE.name());

                if (!followUpTemplates.isEmpty()) {
                    followUpTemplate = followUpTemplates.get(0);
                }
            }

            if (followUpTemplate != null) {
                // Create new assignment with follow-up template
                RecruiterTemplateAssignment followUpAssignment = new RecruiterTemplateAssignment();
                followUpAssignment.setRecruiterId(assignment.getRecruiterId());
                followUpAssignment.setTemplateId(followUpTemplate.getId());
                followUpAssignment.setUserId(currentUser.getId());
                followUpAssignment.setWeekAssigned(assignment.getWeekAssigned());
                followUpAssignment.setYearAssigned(assignment.getYearAssigned());
                followUpAssignment.setAssignmentStatusEnum(RecruiterTemplateAssignment.AssignmentStatus.ACTIVE);
                followUpAssignment.setEmailsSent(assignment.getEmailsSent());
                followUpAssignment.setLastEmailSentAt(assignment.getLastEmailSentAt());

                assignmentFirestoreRepository.save(currentUser.getId(), followUpAssignment);
            }

            // Mark current assignment as moved to follow-up
            assignment.setAssignmentStatusEnum(RecruiterTemplateAssignment.AssignmentStatus.MOVED_TO_FOLLOWUP);
            assignmentFirestoreRepository.save(currentUser.getId(), assignment);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error moving recruiter to follow-up template", e);
        }
    }

    @Override
    public void markEmailSent(String assignmentId) {
        try {
            User currentUser = userService.getCurrentUser();

            RecruiterTemplateAssignment assignment = assignmentFirestoreRepository
                .findById(currentUser.getId(), assignmentId)
                .orElseThrow(() -> new RuntimeException("Assignment not found"));

            assignment.setEmailsSent(assignment.getEmailsSent() + 1);
            assignment.setLastEmailSentAt(Timestamp.now());
            assignmentFirestoreRepository.save(currentUser.getId(), assignment);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error marking email sent", e);
        }
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> getActiveAssignments() {
        try {
            User currentUser = userService.getCurrentUser();

            List<RecruiterTemplateAssignment> assignments = assignmentFirestoreRepository
                .findByUserAndStatus(currentUser.getId(),
                    RecruiterTemplateAssignment.AssignmentStatus.ACTIVE.name());

            return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error fetching active assignments", e);
        }
    }

    @Override
    public void deleteAssignment(String assignmentId) {
        try {
            User currentUser = userService.getCurrentUser();
            assignmentFirestoreRepository.delete(currentUser.getId(), assignmentId);
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error deleting assignment", e);
        }
    }

    @Override
    public List<RecruiterTemplateAssignmentDto> bulkDeleteAssignments(List<String> assignmentIds) {
        try {
            User currentUser = userService.getCurrentUser();
            List<RecruiterTemplateAssignment> assignments = new ArrayList<>();

            for (String assignmentId : assignmentIds) {
                Optional<RecruiterTemplateAssignment> assignmentOpt = assignmentFirestoreRepository
                    .findById(currentUser.getId(), assignmentId);

                if (assignmentOpt.isPresent()) {
                    assignments.add(assignmentOpt.get());
                    assignmentFirestoreRepository.delete(currentUser.getId(), assignmentId);
                }
            }

            return assignments.stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error bulk deleting assignments", e);
        }
    }

    private RecruiterTemplateAssignmentDto convertToDto(RecruiterTemplateAssignment assignment) {
        try {
            RecruiterTemplateAssignmentDto dto = new RecruiterTemplateAssignmentDto();
            dto.setId(assignment.getId());
            dto.setRecruiterId(assignment.getRecruiterId());
            dto.setTemplateId(assignment.getTemplateId());
            dto.setWeekAssigned(assignment.getWeekAssigned());
            dto.setYearAssigned(assignment.getYearAssigned());
            dto.setAssignmentStatus(assignment.getAssignmentStatusEnum());
            dto.setEmailsSent(assignment.getEmailsSent());
            dto.setLastEmailSentAt(assignment.getLastEmailSentAt());
            dto.setCreatedAt(assignment.getCreatedAt());
            dto.setUpdatedAt(assignment.getUpdatedAt());

            // Fetch and set recruiter contact information
            if (assignment.getRecruiterId() != null && assignment.getUserId() != null) {
                recruiterFirestoreRepository.findById(assignment.getUserId(), assignment.getRecruiterId())
                    .ifPresent(recruiter -> {
                        RecruiterContactDto recruiterDto = new RecruiterContactDto();
                        recruiterDto.setId(recruiter.getId());
                        recruiterDto.setEmail(recruiter.getEmail());
                        recruiterDto.setRecruiterName(recruiter.getRecruiterName());
                        recruiterDto.setCompanyName(recruiter.getCompanyName());
                        recruiterDto.setJobRole(recruiter.getJobRole());
                        recruiterDto.setLinkedinProfile(recruiter.getLinkedinProfile());
                        recruiterDto.setStatus(recruiter.getStatusEnum());
                        recruiterDto.setLastContactedAt(recruiter.getLastContactedAt());
                        recruiterDto.setCreatedAt(recruiter.getCreatedAt());
                        recruiterDto.setUpdatedAt(recruiter.getUpdatedAt());
                        dto.setRecruiterContact(recruiterDto);
                    });
            }

            // Fetch and set email template information
            if (assignment.getTemplateId() != null && assignment.getUserId() != null) {
                templateFirestoreRepository.findById(assignment.getUserId(), assignment.getTemplateId())
                    .ifPresent(template -> {
                        dto.setTemplateName(template.getName());

                        EmailTemplateDto templateDto = new EmailTemplateDto();
                        templateDto.setId(template.getId());
                        templateDto.setName(template.getName());
                        templateDto.setSubject(template.getSubject());
                        templateDto.setBody(template.getBody());
                        templateDto.setCategory(template.getCategoryEnum());
                        templateDto.setStatus(template.getStatusEnum());
                        templateDto.setUsageCount(template.getUsageCount());
                        templateDto.setEmailsSent(template.getEmailsSent());
                        templateDto.setResponseRate(template.getResponseRate());
                        templateDto.setIsDefault(template.getIsDefault());
                        templateDto.setLastUsed(convertToLocalDateTime(template.getLastUsed()));
                        templateDto.setCreatedAt(convertToLocalDateTime(template.getCreatedAt()));
                        templateDto.setUpdatedAt(convertToLocalDateTime(template.getUpdatedAt()));
                        dto.setEmailTemplate(templateDto);
                    });
            }

            return dto;
        } catch (ExecutionException | InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Error converting assignment to DTO", e);
        }
    }

    /**
     * Helper method to convert Firestore Timestamp to LocalDateTime
     */
    private java.time.LocalDateTime convertToLocalDateTime(com.google.cloud.Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }
        return java.time.LocalDateTime.ofInstant(
                java.time.Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()),
                java.time.ZoneId.systemDefault()
        );
    }
}
