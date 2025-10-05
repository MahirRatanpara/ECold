package com.ecold.controller;

import com.ecold.dto.RecruiterTemplateAssignmentDto;
import com.ecold.dto.TemplateWeekSummaryDto;
import com.ecold.service.RecruiterTemplateAssignmentService;
import com.ecold.service.EmailSendService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/template-assignments")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:4200", "http://localhost:4201", "http://localhost:3000"})
public class RecruiterTemplateAssignmentController {

    private final RecruiterTemplateAssignmentService assignmentService;
    private final EmailSendService emailSendService;

    @GetMapping("/template/{templateId}")
    public ResponseEntity<Page<RecruiterTemplateAssignmentDto>> getRecruitersForTemplate(
            @PathVariable String templateId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<RecruiterTemplateAssignmentDto> assignments = assignmentService.getRecruitersForTemplate(templateId, pageable);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/template/{templateId}/date-range")
    public ResponseEntity<Page<RecruiterTemplateAssignmentDto>> getRecruitersForTemplateAndDateRange(
            @PathVariable String templateId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        Pageable pageable = PageRequest.of(page, size);
        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        Page<RecruiterTemplateAssignmentDto> assignments = assignmentService.getRecruitersForTemplateAndDateRange(
                templateId, start, end, pageable);
        return ResponseEntity.ok(assignments);
    }

    @GetMapping("/template/{templateId}/date-ranges")
    public ResponseEntity<List<TemplateWeekSummaryDto>> getDateRangeSummariesForTemplate(
            @PathVariable String templateId) {

        List<TemplateWeekSummaryDto> summaries = assignmentService.getDateRangeSummariesForTemplate(templateId);
        return ResponseEntity.ok(summaries);
    }

    @GetMapping("/template/{templateId}/date-range/all")
    public ResponseEntity<List<RecruiterTemplateAssignmentDto>> getAllRecruitersForDateRange(
            @PathVariable String templateId,
            @RequestParam String startDate,
            @RequestParam String endDate) {

        LocalDate start = LocalDate.parse(startDate);
        LocalDate end = LocalDate.parse(endDate);

        List<RecruiterTemplateAssignmentDto> assignments = assignmentService.getRecruitersForDateRange(templateId, start, end);
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/assign")
    public ResponseEntity<RecruiterTemplateAssignmentDto> assignRecruiterToTemplate(
            @RequestBody AssignmentRequest request) {

        RecruiterTemplateAssignmentDto assignment = assignmentService.assignRecruiterToTemplate(
                request.getRecruiterId(), request.getTemplateId());
        return ResponseEntity.ok(assignment);
    }

    @PostMapping("/bulk-assign")
    public ResponseEntity<List<RecruiterTemplateAssignmentDto>> bulkAssignRecruitersToTemplate(
            @RequestBody BulkAssignmentRequest request) {

        List<RecruiterTemplateAssignmentDto> assignments = assignmentService.bulkAssignRecruitersToTemplate(
                request.getRecruiterIds(), request.getTemplateId());
        return ResponseEntity.ok(assignments);
    }

    @PostMapping("/template/{templateId}/date-range/send-bulk-email")
    public ResponseEntity<Void> sendBulkEmailToDateRange(
            @PathVariable String templateId,
            @RequestParam String startDate,
            @RequestParam String endDate,
            @RequestBody BulkEmailRequest request) {

        try {
            LocalDate start = LocalDate.parse(startDate);
            LocalDate end = LocalDate.parse(endDate);

            // Get all recruiters for the specified date range
            List<RecruiterTemplateAssignmentDto> assignments = assignmentService.getRecruitersForDateRange(templateId, start, end);

            // Send emails to each recruiter
            for (RecruiterTemplateAssignmentDto assignment : assignments) {
                try {
                    // Create personalized email content
                    String personalizedSubject = personalizeContent(request.getSubject(), assignment);
                    String personalizedBody = personalizeContent(request.getBody(), assignment);

                    String recipientEmail = assignment.getRecruiterContact() != null ?
                        assignment.getRecruiterContact().getEmail() : null;

                    if (recipientEmail == null) {
                        continue;
                    }

                    byte[] resumeAttachment = null;
                    emailSendService.sendEmail(recipientEmail, personalizedSubject, personalizedBody,
                            resumeAttachment, request.isUseScheduledSend(), request.getScheduleTime());

                    assignmentService.markEmailSent(assignment.getId());

                } catch (Exception e) {
                    // Continue with other emails
                }
            }

            return ResponseEntity.ok().build();
        } catch (Exception e) {
            throw new RuntimeException("Failed to send bulk emails: " + e.getMessage());
        }
    }

    private String personalizeContent(String content, RecruiterTemplateAssignmentDto assignment) {
        if (content == null) return "";

        String personalized = content;

        // Replace placeholders with actual recruiter data
        if (assignment.getRecruiterContact() != null) {
            personalized = personalized.replace("{recruiterName}",
                assignment.getRecruiterContact().getRecruiterName() != null ?
                assignment.getRecruiterContact().getRecruiterName() : "Recruiter");
            personalized = personalized.replace("{companyName}",
                assignment.getRecruiterContact().getCompanyName() != null ?
                assignment.getRecruiterContact().getCompanyName() : "Company");
            personalized = personalized.replace("{jobTitle}",
                assignment.getRecruiterContact().getJobRole() != null ?
                assignment.getRecruiterContact().getJobRole() : "Position");
        }

        // TODO: Get sender name from user context
        personalized = personalized.replace("{senderName}", "Your Name");

        return personalized;
    }

    @PutMapping("/{assignmentId}/mark-email-sent")
    public ResponseEntity<Void> markEmailSent(@PathVariable String assignmentId) {
        assignmentService.markEmailSent(assignmentId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{assignmentId}/move-to-followup")
    public ResponseEntity<Void> moveToFollowup(@PathVariable String assignmentId) {
        assignmentService.moveRecruiterToFollowupTemplate(assignmentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{assignmentId}")
    public ResponseEntity<Void> deleteAssignment(@PathVariable String assignmentId) {
        assignmentService.deleteAssignment(assignmentId);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bulk")
    public ResponseEntity<List<RecruiterTemplateAssignmentDto>> bulkDeleteAssignments(
            @RequestBody List<String> assignmentIds) {

        List<RecruiterTemplateAssignmentDto> deleted = assignmentService.bulkDeleteAssignments(assignmentIds);
        return ResponseEntity.ok(deleted);
    }

    @GetMapping("/active")
    public ResponseEntity<List<RecruiterTemplateAssignmentDto>> getActiveAssignments() {
        List<RecruiterTemplateAssignmentDto> assignments = assignmentService.getActiveAssignments();
        return ResponseEntity.ok(assignments);
    }

    // Request DTOs
    public static class AssignmentRequest {
        private String recruiterId;
        private String templateId;

        public String getRecruiterId() { return recruiterId; }
        public void setRecruiterId(String recruiterId) { this.recruiterId = recruiterId; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
    }

    public static class BulkAssignmentRequest {
        private List<String> recruiterIds;
        private String templateId;

        public List<String> getRecruiterIds() { return recruiterIds; }
        public void setRecruiterIds(List<String> recruiterIds) { this.recruiterIds = recruiterIds; }
        public String getTemplateId() { return templateId; }
        public void setTemplateId(String templateId) { this.templateId = templateId; }
    }

    public static class BulkEmailRequest {
        private String subject;
        private String body;
        private boolean useScheduledSend;
        private String scheduleTime;

        public String getSubject() { return subject; }
        public void setSubject(String subject) { this.subject = subject; }
        public String getBody() { return body; }
        public void setBody(String body) { this.body = body; }
        public boolean isUseScheduledSend() { return useScheduledSend; }
        public void setUseScheduledSend(boolean useScheduledSend) { this.useScheduledSend = useScheduledSend; }
        public String getScheduleTime() { return scheduleTime; }
        public void setScheduleTime(String scheduleTime) { this.scheduleTime = scheduleTime; }
    }
}
