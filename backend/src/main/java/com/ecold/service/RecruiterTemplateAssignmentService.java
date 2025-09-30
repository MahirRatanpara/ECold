package com.ecold.service;

import com.ecold.dto.RecruiterTemplateAssignmentDto;
import com.ecold.dto.TemplateWeekSummaryDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface RecruiterTemplateAssignmentService {

    Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplate(Long templateId, Pageable pageable);

    Page<RecruiterTemplateAssignmentDto> getRecruitersForTemplateAndDateRange(Long templateId, LocalDate startDate, LocalDate endDate, Pageable pageable);

    List<TemplateWeekSummaryDto> getDateRangeSummariesForTemplate(Long templateId);

    List<RecruiterTemplateAssignmentDto> getRecruitersForDateRange(Long templateId, LocalDate startDate, LocalDate endDate);

    RecruiterTemplateAssignmentDto assignRecruiterToTemplate(Long recruiterId, Long templateId);

    List<RecruiterTemplateAssignmentDto> bulkAssignRecruitersToTemplate(List<Long> recruiterIds, Long templateId);

    void moveRecruiterToFollowupTemplate(Long assignmentId);


    void markEmailSent(Long assignmentId);

    List<RecruiterTemplateAssignmentDto> getActiveAssignments();

    void deleteAssignment(Long assignmentId);

    List<RecruiterTemplateAssignmentDto> bulkDeleteAssignments(List<Long> assignmentIds);
}