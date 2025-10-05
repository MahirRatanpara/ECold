package com.ecold.service;

import com.ecold.dto.RecruiterTemplateAssignmentDto;
import com.ecold.dto.TemplateWeekSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RecruiterTemplateAssignmentService {

    Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplate(String templateId, Pageable pageable);

    Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplateAndDateRange(String templateId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<TemplateWeekSummaryDto> getDateRangeSummariesForTemplate(String templateId);

    List<RecruiterTemplateAssignmentDto> getRecruitersForDateRange(String templateId, LocalDate startDate, LocalDate endDate);

    RecruiterTemplateAssignmentDto assignRecruiterToTemplate(String recruiterId, String templateId);

    List<RecruiterTemplateAssignmentDto> bulkAssignRecruitersToTemplate(List<String> recruiterIds, String templateId);

    void moveRecruiterToFollowupTemplate(String assignmentId);

    void markEmailSent(String assignmentId);

    List<RecruiterTemplateAssignmentDto> getActiveAssignments();

    void deleteAssignment(String assignmentId);

    List<RecruiterTemplateAssignmentDto> bulkDeleteAssignments(List<String> assignmentIds);
}